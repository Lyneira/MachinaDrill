package me.lyneira.MachinaDrill;

import java.util.List;

import me.lyneira.MachinaCraft.BlockData;
import me.lyneira.MachinaCraft.BlockLocation;
import me.lyneira.MachinaCraft.BlockRotation;
import me.lyneira.MachinaCraft.BlockVector;
import me.lyneira.MachinaCraft.EventSimulator;
import me.lyneira.MachinaCraft.Fuel;
import me.lyneira.MachinaCraft.HeartBeatEvent;
import me.lyneira.MachinaCraft.Movable;

import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
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
    private BlockVector[] drillPattern;

    /**
     * The amount of energy stored. This is just the number of server ticks left
     * before needing to consume new fuel.
     */
    private int currentEnergy = 0;

    /**
     * The next target location for the drill.
     */
    private BlockLocation queuedTarget = null;

    /**
     * The typeId of the next target location.
     */
    private int nextTypeId;

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
    Drill(final Blueprint blueprint, final List<Integer> moduleIndices, final BlockRotation yaw, Player player, BlockLocation anchor) {
        super(blueprint, moduleIndices, yaw, player);

        this.player = player;
        drillPattern = Blueprint.drillPattern.get(yaw);
        // Set furnace to burning state.
        setFurnace(anchor, true);
    }

    /**
     * Initiates the current move or drill action in the action queue.
     */
    public HeartBeatEvent heartBeat(final BlockLocation anchor) {
        // Drills will not function for offline players.
        if (!player.isOnline())
            return null;

        BlockLocation target = nextTarget(anchor);
        if (target == null && queuedTarget == null) {
            BlockLocation newAnchor = doMove(anchor);
            if (newAnchor == null) {
                return null;
            }
            return new HeartBeatEvent(queueAction(newAnchor), newAnchor);
        } else if (target != null && target.equals(queuedTarget) && target.getTypeId() == nextTypeId) {
            if (!doDrill(anchor)) {
                return null;
            }
        }
        return new HeartBeatEvent(queueAction(anchor));
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
        if (BlockData.isDrillable(nextTypeId)) {
            Block chest = anchor.getRelative(blueprint.getByIndex(Blueprint.chestIndex, yaw, Blueprint.mainModuleIndex)).getBlock();
            Inventory inventory = ((Chest) chest.getState()).getInventory();
            if (inventory.firstEmpty() < 0)
                return false;

            if (!EventSimulator.blockBreak(queuedTarget, player))
                return false;

            if (!useEnergy(anchor, BlockData.getDrillTime(nextTypeId)))
                return false;

            ItemStack item = BlockData.breakBlock(queuedTarget);
            queuedTarget.setEmpty();
            // Put item in the container
            if (item != null) {
                inventory.addItem(item);
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
        BlockFace face = yaw.getYawFace();
        BlockLocation newAnchor = anchor.getRelative(face);
        BlockLocation ground = newAnchor.getRelative(blueprint.getByIndex(Blueprint.centralBaseIndex, yaw, Blueprint.mainModuleIndex).add(BlockFace.DOWN));
        if (!BlockData.isSolid(ground.getTypeId())) {
            return null;
        }

        // Collision detection
        if (detectCollision(anchor, face)) {
            return null;
        }

        // Simulate a block place event to give protection plugins a chance to
        // stop the drill move
        if (!canMove(newAnchor, Blueprint.drillHeadIndex, Blueprint.headMaterial, Blueprint.mainModuleIndex)) {
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
     * Rotates the drill to the new direction, if this would not cause a
     * collision.
     * 
     * @param anchor
     *            The anchor of the Drill
     * @param newYaw
     *            The new direction
     */
    void doRotate(final BlockLocation anchor, final BlockRotation newYaw) {
        BlockRotation rotateBy = newYaw.subtract(yaw);
        if (rotateBy == BlockRotation.ROTATE_0 || detectCollisionRotate(anchor, rotateBy)) {
            return;
        }
        rotate(anchor, rotateBy);
        // Reinitialize the drill pattern since we rotated.
        drillPattern = Blueprint.drillPattern.get(yaw);
        // Set furnace to correct direction.
        setFurnace(anchor, true);
    }

    /**
     * Determines the next target block for the drill and returns its location.
     * 
     * @param anchor
     *            The anchor of the drill
     * @return The BlockLocation of the next target, or null if no drillable
     *         target was found.
     */
    private BlockLocation nextTarget(final BlockLocation anchor) {
        for (BlockVector i : drillPattern) {
            BlockLocation location = anchor.getRelative(i);
            int typeId = location.getTypeId();
            if (BlockData.isDrillable(typeId)) {
                return location;
            }
        }
        return null;
    }

    /**
     * Determines the delay for the next action.
     * 
     * @param anchor
     *            The anchor of the Drill
     * @return Delay in server ticks for the next action
     */
    private int queueAction(final BlockLocation anchor) {
        queuedTarget = nextTarget(anchor);
        if (queuedTarget == null) {
            return moveDelay;
        } else {
            nextTypeId = queuedTarget.getTypeId();
            return BlockData.getDrillTime(nextTypeId);
        }
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
            int newFuel = Fuel.consume((Furnace) anchor.getRelative(blueprint.getByIndex(Blueprint.furnaceIndex, yaw, Blueprint.mainModuleIndex)).getBlock().getState());
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
     * If the player has permission to deactivate the drill, deactivate it. Or
     * rotate it instead if the player rightclicked with the appropriate item.
     */
    public boolean onLever(final BlockLocation anchor, Player player, ItemStack itemInHand) {
        if ((this.player == player && player.hasPermission("machinadrill.deactivate-own")) || player.hasPermission("machinadrill.deactivate-all")) {
            if (itemInHand != null && itemInHand.getType() == Blueprint.rotateMaterial) {
                doRotate(anchor, BlockRotation.yawFromLocation(player.getLocation()));
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * Returns the burning furnace to its normal state.
     * 
     * @param anchor
     *            The anchor of the Drill being deactivated
     */
    public void onDeActivate(final BlockLocation anchor) {
        setFurnace(anchor, false);
    }

    /**
     * Sets the drill's furnace to the given state and set correct direction.
     * 
     * @param anchor
     *            The drill's anchor
     * @param burning
     *            Whether the furnace should be burning.
     */
    void setFurnace(final BlockLocation anchor, final boolean burning) {
        Block furnace = anchor.getRelative(blueprint.getByIndex(Blueprint.furnaceIndex, yaw, Blueprint.mainModuleIndex)).getBlock();
        Fuel.setFurnace(furnace, yaw.getOpposite().getYawFace(), burning);
    }
}