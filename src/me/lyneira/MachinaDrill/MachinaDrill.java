package me.lyneira.MachinaDrill;

import java.util.logging.Logger;

import me.lyneira.MachinaCraft.MachinaCraft;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class MachinaDrill extends JavaPlugin {
	final Logger log = Logger.getLogger("Minecraft");

	public final void onEnable() {
		PluginDescriptionFile pdf = getDescription();
		log.info(pdf.getName() + " version " + pdf.getVersion()
				+ " is now enabled.");

		MachinaCraft.instance.registerBlueprint(DrillBlueprint.blueprint);
	}

	public final void onDisable() {
		PluginDescriptionFile pdf = getDescription();
		log.info(pdf.getName() + " is now disabled.");

		MachinaCraft.instance.unRegisterBlueprint(DrillBlueprint.blueprint);
	}

}
