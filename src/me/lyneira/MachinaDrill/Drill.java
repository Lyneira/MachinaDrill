package me.lyneira.MachinaDrill;

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
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
	 * The amount of energy stored. For the Drill, this is just the number of
	 * server ticks left before the drill needs to consume new fuel.
	 */
	private int currentEnergy = 0;

	/**
	 * The player that activated the drill.
	 */
	private final Player player;

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
				if (patternIndex == DrillBlueprint.drillPatternSize) {
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
	 * Creates a new drill for the given MachinaCraft plugin, with the anchor at
	 * the given BlockLocation and the given Orientation
	 * 
	 * @param plugin
	 *            The MachinaCraft plugin
	 * @param anchor
	 *            The anchor location of the drill
	 * @param orientation
	 *            The orientation of the drill
	 */
	Drill(final DrillBlueprint blueprint, Player player, BlockLocation anchor,
			final BlockRotation yaw) {
		super(blueprint, yaw);

		this.player = player;
		drillPattern = DrillBlueprint.drillPattern.get(yaw);
		// Set furnace to burning state.
		Block furnace = anchor.getRelative(
				blueprint.getByIndex(DrillBlueprint.furnaceIndex, yaw))
				.getBlock();
		Inventory inventory = ((Furnace) furnace.getState()).getInventory();
		setFurnace(furnace, true, inventory);
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
						blueprint.getByIndex(DrillBlueprint.furnaceIndex, yaw)
								.add(BlockFace.UP)).dropItem(item);
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
				DrillBlueprint.centralBaseIndex, yaw).add(BlockFace.DOWN));
		if (!BlockData.isSolid(ground.getTypeId())) {
			return null;
		}

		// Collision detection
		if (blueprint.detectCollision(anchor, face, yaw)) {
			return null;
		}

		// Simulate a block place event to give protection plugins a chance to
		// stop the drill move
		if (!canPlace(newAnchor)) {
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
							blueprint.getByIndex(DrillBlueprint.furnaceIndex,
									yaw)).getBlock().getState());
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
				blueprint.getByIndex(DrillBlueprint.furnaceIndex, yaw))
				.getBlock();
		if (furnace.getType() == Material.BURNING_FURNACE) {
			Inventory inventory = ((Furnace) furnace.getState()).getInventory();
			setFurnace(furnace, false, inventory);
		}
	}

	/**
	 * Sets the given Block to a Furnace with the given burn state, and sets the
	 * furnace contents to the given Inventory.
	 * 
	 * @param block
	 *            The block to set as a Furnace
	 * @param burning
	 *            Whether the furnace is burning
	 * @param inventory
	 *            Inventory to copy over
	 */
	private void setFurnace(final Block furnace, final boolean burning,
			final Inventory inventory) {
		ItemStack[] contents = inventory.getContents();
		inventory.clear();
		BlockState newFurnace = furnace.getState();
		if (burning) {
			newFurnace.setType(Material.BURNING_FURNACE);
		} else {
			newFurnace.setType(Material.FURNACE);
		}
		// Set furnace direction
		newFurnace.setData(new org.bukkit.material.Furnace(yaw.getOpposite()
				.getFacing()));

		newFurnace.update(true);
		Inventory newInventory = ((Furnace) furnace.getState()).getInventory();
		newInventory.setContents(contents);
	}

	/**
	 * Simulates a block place event on behalf of the player who started the
	 * drill. Returns true if the player could build the new drill head block.
	 * 
	 * @param newAnchor
	 *            The new anchor location of the drill.
	 * @return True if the player may place a block at the drill's new head
	 *         location.
	 */
	private boolean canPlace(BlockLocation newAnchor) {
		Block placedBlock = newAnchor.getRelative(
				blueprint.getByIndex(DrillBlueprint.drillHeadIndex, yaw))
				.getBlock();
		BlockState replacedBlockState = placedBlock.getState();
		replacedBlockState.setType(DrillBlueprint.headMaterial);
		Block placedAgainst = placedBlock.getRelative(yaw.getOpposite()
				.getFacing());

		BlockPlaceEvent placeEvent = new BlockPlaceEvent(placedBlock,
				replacedBlockState, placedAgainst, null, player, true);
		MachinaDrill.pluginManager.callEvent(placeEvent);

		if (placeEvent.isCancelled())
			return false;
		return true;
	}
}