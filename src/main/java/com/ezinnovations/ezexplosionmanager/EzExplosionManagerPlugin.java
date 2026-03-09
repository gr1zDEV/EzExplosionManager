package com.ezinnovations.ezexplosionmanager;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin bootstrap class.
 */
public final class EzExplosionManagerPlugin extends JavaPlugin {

    private ExplosionSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = ExplosionSettings.loadFromConfig(this);

        getServer().getPluginManager().registerEvents(new ExplosionDamageListener(this, settings), this);
        getLogger().info("EzExplosionManager enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("EzExplosionManager disabled.");
    }
}
