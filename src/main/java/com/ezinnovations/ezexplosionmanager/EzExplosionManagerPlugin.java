package com.ezinnovations.ezexplosionmanager;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"ezexplosionmanager".equalsIgnoreCase(command.getName())) {
            return false;
        }

        if (args.length == 1 && "reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("ezexplosionmanager.reload")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
                return true;
            }

            reloadConfig();
            settings = ExplosionSettings.loadFromConfig(this);
            if (damageListener != null) {
                damageListener.updateSettings(settings);
            }

            sender.sendMessage(ChatColor.GREEN + "EzExplosionManager config reloaded.");
            getLogger().info("Configuration reloaded by " + sender.getName() + ".");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " reload");
        return true;
    }
}
