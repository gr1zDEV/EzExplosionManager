package com.ezinnovations.ezexplosionmanager;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin bootstrap class.
 */
public final class EzExplosionManagerPlugin extends JavaPlugin {

    private ExplosionSettings settings;
    private ExplosionDamageListener damageListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = ExplosionSettings.loadFromConfig(this);

        damageListener = new ExplosionDamageListener(this, settings);
        getServer().getPluginManager().registerEvents(damageListener, this);
        getLogger().info("EzExplosionManager enabled.");
    }

    @Override
    public void onDisable() {
        if (damageListener != null) {
            damageListener.clearTrackingData();
            damageListener = null;
        }
        getLogger().info("EzExplosionManager disabled.");
    }
}
