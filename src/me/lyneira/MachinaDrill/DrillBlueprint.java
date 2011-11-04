package me.lyneira.MachinaDrill;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.lyneira.MachinaCraft.BlockData;
import me.lyneira.MachinaCraft.BlockLocation;
import me.lyneira.MachinaCraft.BlockRotation;
import me.lyneira.MachinaCraft.BlockVector;
import me.lyneira.MachinaCraft.Machina;
import me.lyneira.MachinaCraft.MachinaBlueprint;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;

/**
 * MachinaBlueprint representing a Drill blueprint
 * 
 * @author Lyneira
 */
final class DrillBlueprint implements MachinaBlueprint {
	public final static DrillBlueprint blueprint = new DrillBlueprint();
	private final static Material coreMaterial = Material.GOLD_BLOCK;
	private final static Material baseMaterial = Material.WOOD;
	private final static Material headMaterial = Material.IRON_BLOCK;
	private final static Material furnaceMaterial = Material.FURNACE;
	private final static Material burningFurnaceMaterial = Material.BURNING_FURNACE;

	final static int blueprintSize;
	final static int differenceMinusSize;
	final static int differencePlusSize;
	final static int drillPatternSize;
	final static int leverIndex;
	final static int furnaceIndex;
	final static int coreIndex;
	private final static int nonSpecialStartIndex;

	private final static DrillBlueprintData south;
	private final static DrillBlueprintData east;
	private final static DrillBlueprintData north;
	private final static DrillBlueprintData west;

	static {
		// For the SOUTH blueprint: x+ is forward, y+ is up, z+ is right.
		// Blocks that are handled in a special way during detect() go on the
		// front
		// of the array
		BlockVector[] blueprintSouth = {
				// **** Special blocks ****
				// Is a given when the detect is run
				new BlockVector(0, 1, 0),
				// Normal furnace in the off state
				new BlockVector(-1, -1, 0),
				// Core of the machina
				new BlockVector(0, -1, 0),
				// *** Non-special blocks ****
				// Drill anchor for the lever
				new BlockVector(0, 0, 0),
				// Drill head
				new BlockVector(1, 0, 0),
				// Left tire
				new BlockVector(0, -1, 1),
				// Right tire
				new BlockVector(0, -1, -1), };
		Material[] blueprintType = { Material.LEVER, burningFurnaceMaterial,
				coreMaterial, baseMaterial, headMaterial, baseMaterial,
				baseMaterial };
		blueprintSize = Array.getLength(blueprintSouth);
		// Define indices of interest
		leverIndex = 0;
		furnaceIndex = 1;
		coreIndex = 2;
		nonSpecialStartIndex = 3;

		// Calculate SOUTH differences plus and minus for a move with distance
		// 1.
		List<BlockVector> differenceMinus = new ArrayList<BlockVector>(
				blueprintSize);
		List<BlockVector> differencePlus = new ArrayList<BlockVector>(
				blueprintSize);
		List<BlockVector> originalVectors = Arrays.asList(blueprintSouth);
		List<BlockVector> movedVectors = new ArrayList<BlockVector>(
				blueprintSize);
		for (int i = 0; i < blueprintSize; i++) {
			movedVectors.add(blueprintSouth[i].add(BlockFace.SOUTH));
		}
		differenceMinus.addAll(originalVectors);
		differencePlus.addAll(movedVectors);
		differenceMinus.removeAll(movedVectors);
		differencePlus.removeAll(originalVectors);
		BlockVector[] differenceMinusSouth = differenceMinus
				.toArray(new BlockVector[0]);
		BlockVector[] differencePlusSouth = differencePlus
				.toArray(new BlockVector[0]);
		differenceMinusSize = Array.getLength(differenceMinusSouth);
		differencePlusSize = Array.getLength(differencePlusSouth);

		// Define SOUTH drill pattern: x+ is forward, y+ is up, z+ is right.
		BlockVector[] drillPatternSouth = { new BlockVector(2, 0, 0),
				new BlockVector(2, 0, -1), new BlockVector(2, 1, 0),
				new BlockVector(2, 0, 1), new BlockVector(2, -1, 0),
				new BlockVector(2, -1, -1), new BlockVector(2, 1, -1),
				new BlockVector(2, 1, 1), new BlockVector(2, -1, 1), };
		drillPatternSize = Array.getLength(drillPatternSouth);

		// Define other blueprint arrays.
		BlockVector[] blueprintEast = new BlockVector[blueprintSize];
		BlockVector[] blueprintNorth = new BlockVector[blueprintSize];
		BlockVector[] blueprintWest = new BlockVector[blueprintSize];

		// Define other difference arrays.
		BlockVector[] differenceMinusEast = new BlockVector[differenceMinusSize];
		BlockVector[] differencePlusEast = new BlockVector[differencePlusSize];
		BlockVector[] differenceMinusNorth = new BlockVector[differenceMinusSize];
		BlockVector[] differencePlusNorth = new BlockVector[differencePlusSize];
		BlockVector[] differenceMinusWest = new BlockVector[differenceMinusSize];
		BlockVector[] differencePlusWest = new BlockVector[differencePlusSize];

		// Define other drill patterns.
		BlockVector[] drillPatternEast = new BlockVector[drillPatternSize];
		BlockVector[] drillPatternNorth = new BlockVector[drillPatternSize];
		BlockVector[] drillPatternWest = new BlockVector[drillPatternSize];

		// Initialize the other arrays of blueprints.
		for (int i = 0; i < blueprintSize; i++) {
			blueprintEast[i] = new BlockVector(
					blueprintSouth[i].rotated(BlockRotation.ROTATE_90));
			blueprintNorth[i] = new BlockVector(
					blueprintSouth[i].rotated(BlockRotation.ROTATE_180));
			blueprintWest[i] = new BlockVector(
					blueprintSouth[i].rotated(BlockRotation.ROTATE_270));
		}
		// Initialize the other arrays of differences.
		for (int i = 0; i < differenceMinusSize; i++) {
			differenceMinusEast[i] = new BlockVector(
					differenceMinusSouth[i].rotated(BlockRotation.ROTATE_90));
			differencePlusEast[i] = new BlockVector(
					differencePlusSouth[i].rotated(BlockRotation.ROTATE_90));
			differenceMinusNorth[i] = new BlockVector(
					differenceMinusSouth[i].rotated(BlockRotation.ROTATE_180));
			differencePlusNorth[i] = new BlockVector(
					differencePlusSouth[i].rotated(BlockRotation.ROTATE_180));
			differenceMinusWest[i] = new BlockVector(
					differenceMinusSouth[i].rotated(BlockRotation.ROTATE_270));
			differencePlusWest[i] = new BlockVector(
					differencePlusSouth[i].rotated(BlockRotation.ROTATE_270));
		}
		// Initialize the other 3 drill patterns.
		for (int i = 0; i < drillPatternSize; i++) {
			drillPatternEast[i] = new BlockVector(
					drillPatternSouth[i].rotated(BlockRotation.ROTATE_90));
			drillPatternNorth[i] = new BlockVector(
					drillPatternSouth[i].rotated(BlockRotation.ROTATE_180));
			drillPatternWest[i] = new BlockVector(
					drillPatternSouth[i].rotated(BlockRotation.ROTATE_270));
		}

		// Finally, initialize the blueprint data.
		south = new DrillBlueprintData(blueprintSouth, blueprintType,
				differenceMinusSouth, differencePlusSouth, drillPatternSouth);
		east = new DrillBlueprintData(blueprintEast, blueprintType,
				differenceMinusEast, differencePlusEast, drillPatternEast);
		north = new DrillBlueprintData(blueprintNorth, blueprintType,
				differenceMinusNorth, differencePlusNorth, drillPatternNorth);
		west = new DrillBlueprintData(blueprintWest, blueprintType,
				differenceMinusWest, differencePlusWest, drillPatternWest);
	}

	/**
	 * Returns a DrillBlueprintData for the given BlockFace.
	 * 
	 * @param direction
	 *            The direction to get DrillBlueprintData for. Only SOUTH, WEST,
	 *            NORTH, EAST make sense.
	 * @return The blueprint data for the given direction
	 */
	private static final DrillBlueprintData getBlueprintRotated(
			final BlockFace direction) {
		if (direction == BlockFace.WEST) {
			return west;
		} else if (direction == BlockFace.NORTH) {
			return north;
		} else if (direction == BlockFace.EAST) {
			return east;
		} else {
			return south;
		}
	}

	/**
	 * Calculates an array of BlockLocations that represents the shape of a
	 * drill with the given BlockLocation as anchor and for the given
	 * DrillBlueprintData's direction.
	 * 
	 * @param anchor
	 *            The anchor for which to calculate
	 * @param data
	 *            The blueprint data to use
	 * @return A new array of BlockLocations of length blueprintSize.
	 */
	static final BlockLocation[] calculateBlocks(final BlockLocation anchor,
			final DrillBlueprintData data) {
		BlockLocation[] result = new BlockLocation[blueprintSize];
		for (int i = 0; i < blueprintSize; i++) {
			result[i] = anchor.getRelative(data.blueprint[i]);
		}
		return result;
	}

	/**
	 * Calculates an array of BlockLocations that represents the negative
	 * difference of a moving drill with the given BlockLocation as anchor and
	 * for the given DrillBlueprintData's direction. The negative difference is
	 * the set of blocks that become empty when the drill has been moved to its
	 * new location in the world.
	 * 
	 * @param anchor
	 *            The anchor for which to calculate. This must be the anchor
	 *            before the move.
	 * @param data
	 *            The blueprint data to use
	 * @return A new array of BlockLocations of length differenceMinusSize
	 */
	static final BlockLocation[] calculateDifferenceMinus(
			final BlockLocation anchor, final DrillBlueprintData data) {
		BlockLocation[] result = new BlockLocation[differenceMinusSize];
		for (int i = 0; i < differenceMinusSize; i++) {
			result[i] = anchor.getRelative(data.differenceMinus[i]);
		}
		return result;
	}

	/**
	 * Calculates an array of BlockLocations that represents the positive
	 * difference of a moving drill with the given BlockLocation as anchor and
	 * for the given DrillBlueprintData's direction. The positive difference is
	 * the set of blocks that become part of the drill when it has been moved to
	 * its new location in the world. These blocks can then be tested for
	 * emptiness to avoid collisions.
	 * 
	 * @param anchor
	 *            The anchor for which to calculate. This must be the anchor
	 *            before the move.
	 * @param data
	 *            The blueprint data to use
	 * @return A new array of BlockLocations of length differencePlusSize
	 */
	static final BlockLocation[] calculateDifferencePlus(
			final BlockLocation anchor, final DrillBlueprintData data) {
		BlockLocation[] result = new BlockLocation[differencePlusSize];
		for (int i = 0; i < differencePlusSize; i++) {
			result[i] = anchor.getRelative(data.differencePlus[i]);
		}
		return result;
	}

	/**
	 * Verifies whether the given Drill is present at the given BlockLocation
	 * 
	 * @param anchor
	 * @param data
	 * @return
	 */
	static final boolean verify(final BlockLocation anchor,
			final DrillBlueprintData data) {
		BlockVector[] blueprint = data.blueprint;
		Material[] types = data.blueprintType;
		BlockLocation block;
		for (int i = 0; i < blueprintSize; i++) {
			block = anchor.getRelative(blueprint[i]);
			if (!block.checkType(types[i])) {
				return false;
			}
		}
		return true;
	}

	private DrillBlueprint() {
		// This is a singleton.
	}

	/**
	 * Detects whether a drill is present at the given BlockLocation
	 */
	public Machina detect(final BlockLocation anchor, final BlockFace leverFace) {
		if (leverFace != BlockFace.UP) {
			return null;
		}
		// Check if the drill is on solid ground.
		if (!BlockData.isSolid(anchor.getRelative(BlockFace.DOWN, 2)
				.getTypeId())) {
			return null;
		}
		BlockLocation core = anchor.getRelative(BlockFace.DOWN);
		if (core.checkType(coreMaterial)) {
			// Search for a furnace around the core.
			BlockFace direction = null;
			if (core.getRelative(BlockFace.NORTH).checkType(furnaceMaterial)) {
				direction = BlockFace.SOUTH;
			} else if (core.getRelative(BlockFace.EAST).checkType(
					furnaceMaterial)) {
				direction = BlockFace.WEST;
			} else if (core.getRelative(BlockFace.SOUTH).checkType(
					furnaceMaterial)) {
				direction = BlockFace.NORTH;
			} else if (core.getRelative(BlockFace.WEST).checkType(
					furnaceMaterial)) {
				direction = BlockFace.EAST;
			}
			if (direction != null) {
				DrillBlueprintData data = getBlueprintRotated(direction);
				BlockVector[] blueprint = data.blueprint;
				Material[] types = data.blueprintType;
				BlockLocation block;
				// The 'special' blocks have already been verified, so start
				// checking at the first non-special block.
				for (int i = nonSpecialStartIndex; i < blueprintSize; i++) {
					block = anchor.getRelative(blueprint[i]);
					if (!block.checkType(types[i])) {
						return null;
					}
				}
				return new Drill(anchor, direction, data);
			}
		}
		return null;
	}
}
