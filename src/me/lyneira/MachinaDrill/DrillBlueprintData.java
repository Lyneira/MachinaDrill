package me.lyneira.MachinaDrill;

import me.lyneira.MachinaCraft.BlockVector;

import org.bukkit.Material;

/**
 * Represents a drill's blueprint, drill pattern and difference data for a
 * single compass direction
 * 
 * @author Lyneira
 */
final class DrillBlueprintData {
	final BlockVector[] blueprint;
	final Material[] blueprintType;
	final BlockVector[] differenceMinus;
	final BlockVector[] differencePlus;
	final BlockVector[] drillPattern;

	DrillBlueprintData(final BlockVector[] blueprint,
			final Material[] blueprintType,
			final BlockVector[] differenceMinus,
			final BlockVector[] differencePlus, final BlockVector[] drillPattern) {
		this.blueprint = blueprint;
		this.blueprintType = blueprintType;
		this.differenceMinus = differenceMinus;
		this.differencePlus = differencePlus;
		this.drillPattern = drillPattern;
	}
}
