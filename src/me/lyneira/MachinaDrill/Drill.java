package me.lyneira.MachinaDrill;

import java.util.List;

import me.lyneira.MachinaCraft.BlockData;
import me.lyneira.MachinaCraft.BlockLocation;
import me.lyneira.MachinaCraft.BlockRotation;
import me.lyneira.MachinaCraft.BlockVector;
import me.lyneira.MachinaCraft.Fuel;
import me.lyneira.MachinaCraft.HeartBeatEvent;
import me.lyneira.MachinaCraft.Movable;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * A Machina that moves forward, drilling up blocks in its path.
 * 
 * @author Lyneira
 */
final class Drill extends Movable {
	/**
	 * The number of server ticks to wait for a move action.
	 */
	private static final int moveDelay = 20;

	/**
	 * Array of vectors that determines where the drill looks for blocks to
	 * break.
	 */
	private final BlockVector[] drillPattern;

	/**
	 * The amount of energy stored. This is just the number of server ticks left
	 * before needing to consume new fuel.
	 */
	private int currentEnergy = 0;

	/**
	 * Class that determines the next action for the drill.
	 */
	private final class ActionQueue {
		public boolean drill = true;
		public int patternIndex = -1;

		/**
		 * Returns true if the end of the drill pattern has been reached and the
		 * drill should move.
		 * 
		 * @return True if the drill should move
		 */
		public final boolean nextMove() {
			if (drill) {
				patternIndex++;
				if (patternIndex == Blueprint.drillPatternSize) {
					patternIndex = 0;
					drill = false;
					return true;
				}
			} else {
				drill = true;
			}
			return false;
		}
	}

	private final ActionQueue queue = new ActionQueue();

	/**
	 * Creates a new drill.
	 * 
	 * @param plugin
	 *            The MachinaCraft plugin
	 * @param anchor
	 *            The anchor location of the drill
	 * @param yaw
	 *            The direction of the drill
	 * @param moduleIndices
	 *            The active modules for the drill
	 */
	Drill(final Blueprint blueprint, Player player, BlockLocation anchor,
			final BlockRotation yaw, final List<Integer> moduleIndices) {
		super(blueprint, player, yaw, moduleIndices);

		this.player = player;
		drillPattern = Blueprint.drillPattern.get(yaw);
		// Set furnace to burning state.
		Block furnace = anchor.getRelative(
				blueprint.getByIndex(Blueprint.furnaceIndex, yaw,
						Blueprint.mainModuleIndex)).getBlock();
		Inventory inventory = ((Furnace) furnace.getState()).getInventory();
		Fuel.setFurnace(furnace, yaw.getOpposite().getFacing(), true, inventory);
	}

	/**
	 * Initiates the current move or drill action in the action queue.
	 */
	public HeartBeatEvent heartBeat(final BlockLocation anchor) {
		// Drills will not function for offline players.
		if (!player.isOnline())
			return null;

		if (queue.drill) {
			if (queue.patternIndex >= 0) {
				if (!doDrill(anchor)) {
					return null;
				}
			}
			return new HeartBeatEvent(queueAction(anchor));
		} else {
			BlockLocation newAnchor = doMove(anchor);
			if (newAnchor == null) {
				return null;
			}
			return new HeartBeatEvent(queueAction(newAnchor), newAnchor);
		}
	}

	/**
	 * Attempts to drill the next block in the drill pattern, and drop the
	 * resulting item.
	 * 
	 * @param anchor
	 *            The anchor of the machina
	 * @return False if there is no energy/fuel left to complete the drill. True
	 *         if the drill was successful or there was nothing to drill.
	 */
	private boolean doDrill(final BlockLocation anchor) {
		BlockLocation target = anchor
				.getRelative(drillPattern[queue.patternIndex]);
		int typeId = target.getTypeId();
		if (BlockData.isDrillable(typeId)) {
			// Simulate a block break event on behalf of the player who started
			// the drill. Only break if the event wasn't cancelled by whatever
			// protection plugins may exist.
			Block block = target.getBlock();
			BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
			MachinaDrill.pluginManager.callEvent(breakEvent);
			if (breakEvent.isCancelled()) {
				return false;
			}

			if (!useEnergy(anchor, BlockData.getDrillTime(typeId))) {
				return false;
			}

			ItemStack item = BlockData.breakBlock(target);
			target.setType(Material.AIR);
			if (item != null) {
				// Drop item above the furnace
				anchor.getRelative(
						blueprint.getByIndex(Blueprint.furnaceIndex, yaw,
								Blueprint.mainModuleIndex).add(
								BlockFace.UP)).dropItem(item);
			}
		}
		return true;
	}

	/**
	 * Moves the drill forward if there is empty space to move into, and ground
	 * to stand on.
	 * 
	 * @param anchor
	 *            The anchor of the Drill to move
	 * @return The new anchor location of the Drill, or null on failure.
	 */
	private BlockLocation doMove(final BlockLocation anchor) {
		// Check for ground at the new base
		BlockFace face = yaw.getFacing();
		BlockLocation newAnchor = anchor.getRelative(face);
		BlockLocation ground = newAnchor.getRelative(blueprint.getByIndex(
				Blueprint.centralBaseIndex, yaw,
				Blueprint.mainModuleIndex).add(BlockFace.DOWN));
		if (!BlockData.isSolid(ground.getTypeId())) {
			return null;
		}

		// Collision detection
		if (blueprint.detectCollision(anchor, face, yaw,
				Blueprint.mainModuleIndex)) {
			return null;
		}

		// Simulate a block place event to give protection plugins a chance to
		// stop the drill move
		if (!canPlace(newAnchor, Blueprint.drillHeadIndex, Blueprint.headMaterial, Blueprint.mainModuleIndex)) {
			return null;
		}

		// Use energy
		if (!useEnergy(anchor, moveDelay)) {
			return null;
		}

		// Okay to move.
		moveByFace(anchor, face);

		return newAnchor;
	}

	/**
	 * Determines the delay for the next action.
	 * 
	 * @param anchor
	 *            The anchor of the Drill
	 * @return Delay in server ticks for the next action
	 */
	private int queueAction(final BlockLocation anchor) {
		while (!queue.nextMove()) {
			int typeId = anchor.getRelative(drillPattern[queue.patternIndex])
					.getTypeId();
			if (BlockData.isDrillable(typeId)) {
				return BlockData.getDrillTime(typeId);
			}
		}
		return moveDelay;
	}

	/**
	 * Uses the given amount of energy and returns true if successful.
	 * 
	 * @param anchor
	 *            The anchor of the Drill
	 * @param energy
	 *            The amount of energy needed for the next action
	 * @return True if enough energy could be used up
	 */
	private boolean useEnergy(final BlockLocation anchor, final int energy) {
		while (currentEnergy < energy) {
			int newFuel = Fuel.consume((Furnace) anchor
					.getRelative(
							blueprint.getByIndex(Blueprint.furnaceIndex,
									yaw, Blueprint.mainModuleIndex))
					.getBlock().getState());
			if (newFuel > 0) {
				currentEnergy += newFuel;
			} else {
				return false;
			}
		}
		currentEnergy -= energy;
		return true;
	}

	/**
	 * Simply checks the appropriate deactivate permission to determine whether
	 * the player may deactivate the drill.
	 */
	public boolean playerDeActivate(final BlockLocation anchor, Player player) {
		if (this.player == player) {
			if (player.hasPermission("machinadrill.deactivate-own"))
				return true;
		} else {
			if (player.hasPermission("machinadrill.deactivate-all"))
				return true;
		}
		return false;
	}

	/**
	 * Returns the burning furnace to its normal state.
	 * 
	 * @param anchor
	 *            The anchor of the Drill being deactivated
	 */
	public void onDeActivate(final BlockLocation anchor) {
		// Set furnace to off state.
		Block furnace = anchor.getRelative(
				blueprint.getByIndex(Blueprint.furnaceIndex, yaw,
						Blueprint.mainModuleIndex)).getBlock();
		if (furnace.getType() == Material.BURNING_FURNACE) {
			Inventory inventory = ((Furnace) furnace.getState()).getInventory();
			Fuel.setFurnace(furnace, yaw.getOpposite().getFacing(), false,
					inventory);
		}
	}
}