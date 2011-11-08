package me.lyneira.MachinaDrill;

import java.util.logging.Logger;

import me.lyneira.MachinaCraft.MachinaCraft;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MachinaDrill extends JavaPlugin {
	final static Logger log = Logger.getLogger("Minecraft");
	static PluginManager pluginManager;

	public final void onEnable() {
		pluginManager =  this.getServer().getPluginManager();
		PluginDescriptionFile pdf = getDescription();
		log.info(pdf.getName() + " version " + pdf.getVersion()
				+ " is now enabled.");

		MachinaCraft.plugin.registerBlueprint(DrillBlueprint.instance);
	}

	public final void onDisable() {
		PluginDescriptionFile pdf = getDescription();
		log.info(pdf.getName() + " is now disabled.");

		MachinaCraft.plugin.unRegisterBlueprint(DrillBlueprint.instance);
	}

}
