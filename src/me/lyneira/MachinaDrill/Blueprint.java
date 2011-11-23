package me.lyneira.MachinaDrill;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import me.lyneira.MachinaCraft.BlockData;
import me.lyneira.MachinaCraft.BlockLocation;
import me.lyneira.MachinaCraft.BlockRotation;
import me.lyneira.MachinaCraft.BlockVector;
import me.lyneira.MachinaCraft.BlueprintFactory;
import me.lyneira.MachinaCraft.Machina;
import me.lyneira.MachinaCraft.MovableBlueprint;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * MachinaBlueprint representing a Drill blueprint
 * 
 * @author Lyneira
 */
final class Blueprint extends MovableBlueprint {
    private static List<BlueprintFactory> blueprints;
    final static int mainModuleIndex;
    final static Map<BlockRotation, BlockVector[]> drillPattern = new EnumMap<BlockRotation, BlockVector[]>(BlockRotation.class);

    final static int drillPatternSize;
    private final static Material anchorMaterial = Material.GOLD_BLOCK;
    private final static Material baseMaterial = Material.WOOD;
    final static Material headMaterial = Material.IRON_BLOCK;
    private final static Material furnaceMaterial = Material.FURNACE;
    private final static Material burningFurnaceMaterial = Material.BURNING_FURNACE;
    final static Material rotateMaterial = Material.STICK;

    final static int leverIndex;
    final static int centralBaseIndex;
    final static int furnaceIndex;
    final static int drillHeadIndex;

    static {
        blueprints = new ArrayList<BlueprintFactory>(1);
        mainModuleIndex = blueprints.size();
        BlueprintFactory mainModule = new BlueprintFactory();
        blueprints.add(mainModule);
        mainModule
                // Add key blocks
                .addKey(new BlockVector(0, 1, 0), Material.LEVER).addKey(new BlockVector(0, -1, 0), baseMaterial).addKey(new BlockVector(-1, -1, 0), burningFurnaceMaterial)
                .addKey(new BlockVector(1, 0, 0), headMaterial)
                // Add non-key blocks
                .add(new BlockVector(0, 0, 0), anchorMaterial).add(new BlockVector(0, -1, 1), baseMaterial).add(new BlockVector(0, -1, -1), baseMaterial);
        // Get handles to key blocks now. This finalizes the blueprint.
        ListIterator<Integer> handles = mainModule.getHandlesFinal().listIterator();
        leverIndex = handles.next();
        centralBaseIndex = handles.next();
        furnaceIndex = handles.next();
        drillHeadIndex = handles.next();

        // Add drill pattern data 3x3
        drillPatternSize = 9;
        BlockVector[] basePattern = new BlockVector[drillPatternSize];
        basePattern[0] = new BlockVector(2, 0, 0);
        basePattern[1] = new BlockVector(2, 1, 0);
        basePattern[2] = new BlockVector(2, 0, 1);
        basePattern[3] = new BlockVector(2, -1, 0);
        basePattern[4] = new BlockVector(2, 0, -1);
        basePattern[5] = new BlockVector(2, 1, -1);
        basePattern[6] = new BlockVector(2, 1, 1);
        basePattern[7] = new BlockVector(2, -1, 1);
        basePattern[8] = new BlockVector(2, -1, -1);
        for (BlockRotation i : BlockRotation.values()) {
            BlockVector[] rotatedPattern = new BlockVector[drillPatternSize];
            for (int j = 0; j < drillPatternSize; j++) {
                rotatedPattern[j] = basePattern[j].rotated(i);
            }
            drillPattern.put(i, rotatedPattern);
        }
    }

    public final static Blueprint instance = new Blueprint();

    private Blueprint() {
        super(blueprints);
        blueprints = null;
    }

    /**
     * Detects whether a drill is present at the given BlockLocation. Key blocks
     * defined above must be detected manually.
     */
    public Machina detect(Player player, final BlockLocation anchor, final BlockFace leverFace, ItemStack itemInHand) {
        if (leverFace != BlockFace.UP)
            return null;

        if (!player.hasPermission("machinadrill.activate"))
            return null;

        // Check if the drill is on solid ground.
        if (!BlockData.isSolid(anchor.getRelative(BlockFace.DOWN, 2).getTypeId()))
            return null;

        BlockLocation centralBase = anchor.getRelative(BlockFace.DOWN);
        if (centralBase.checkType(baseMaterial)) {
            // Search for a furnace around the central base.
            for (BlockRotation i : BlockRotation.values()) {
                if (centralBase.getRelative(i.getYawFace()).checkType(furnaceMaterial)) {
                    BlockRotation yaw = i.getOpposite();
                    if (anchor.getRelative(yaw.getYawFace()).checkType(headMaterial)) {
                        // The key blocks and yaw have been detected, now we can
                        // leave the rest to the MovableBlueprint framework.
                        if (detectOther(anchor, yaw, mainModuleIndex)) {
                            List<Integer> detectedModules = new ArrayList<Integer>(1);
                            detectedModules.add(mainModuleIndex);
                            Drill drill = new Drill(instance, detectedModules, yaw, player, anchor);
                            if (itemInHand != null && itemInHand.getType() == rotateMaterial) {
                                drill.doRotate(anchor, BlockRotation.yawFromLocation(player.getLocation()));
                                drill.onDeActivate(anchor);
                                drill = null;
                            }
                            return drill;
                        } else {
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }
}
