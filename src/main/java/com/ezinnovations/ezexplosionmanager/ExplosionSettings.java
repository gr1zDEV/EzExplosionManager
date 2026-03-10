package com.ezinnovations.ezexplosionmanager;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Immutable settings view loaded from config.yml.
 */
public final class ExplosionSettings {

    private final boolean debugActive;
    private final double crystalScale;
    private final double tntScale;
    private final double tntCartScale;
    private final double blockBlastScale;
    private final boolean crystalSelfBypass;
    private final Map<String, WorldScaleOverrides> worldOverrides;

    private ExplosionSettings(boolean debugActive,
                              double crystalScale,
                              double tntScale,
                              double tntCartScale,
                              double blockBlastScale,
                              boolean crystalSelfBypass,
                              Map<String, WorldScaleOverrides> worldOverrides) {
        this.debugActive = debugActive;
        this.crystalScale = crystalScale;
        this.tntScale = tntScale;
        this.tntCartScale = tntCartScale;
        this.blockBlastScale = blockBlastScale;
        this.crystalSelfBypass = crystalSelfBypass;
        this.worldOverrides = worldOverrides;
    }

    public static ExplosionSettings loadFromConfig(EzExplosionManagerPlugin plugin) {
        FileConfiguration config = plugin.getConfig();

        boolean debug = config.getBoolean("telemetry.verbose-debug", false);
        double crystal = sanitizeMultiplier(config.getDouble("blast-control.crystal-scale", 1.0));
        double tnt = sanitizeMultiplier(config.getDouble("blast-control.tnt-scale", 1.0));
        double cart = sanitizeMultiplier(config.getDouble("blast-control.minecart-scale", 1.0));
        double bedAnchor = sanitizeMultiplier(config.getDouble("blast-control.bed-anchor-scale", 1.0));
        boolean bypass = config.getBoolean("rules.crystal-owner-vanilla-damage", true);
        Map<String, WorldScaleOverrides> overrides = loadWorldOverrides(config);

        return new ExplosionSettings(debug, crystal, tnt, cart, bedAnchor, bypass, overrides);
    }

    public boolean isDebugActive() {
        return debugActive;
    }

    public boolean isCrystalSelfBypass() {
        return crystalSelfBypass;
    }

    public double multiplierFor(ExplosionSource source) {
        return multiplierFor(source, null);
    }

    public double multiplierFor(ExplosionSource source, String worldName) {
        if (worldName != null) {
            WorldScaleOverrides worldScaleOverrides = worldOverrides.get(worldName.toLowerCase(Locale.ROOT));
            if (worldScaleOverrides != null) {
                return worldScaleOverrides.multiplierFor(source);
            }
        }

        switch (source) {
            case END_CRYSTAL:
                return crystalScale;
            case TNT:
                return tntScale;
            case TNT_MINECART:
                return tntCartScale;
            case BED_OR_ANCHOR:
                return blockBlastScale;
            default:
                return 1.0D;
        }
    }

    private static Map<String, WorldScaleOverrides> loadWorldOverrides(FileConfiguration config) {
        if (!config.isConfigurationSection("world-overrides")) {
            return Collections.emptyMap();
        }

        Map<String, WorldScaleOverrides> overrides = new HashMap<String, WorldScaleOverrides>();
        for (String worldName : config.getConfigurationSection("world-overrides").getKeys(false)) {
            String path = "world-overrides." + worldName;
            WorldScaleOverrides values = new WorldScaleOverrides(
                    sanitizeMultiplier(config.getDouble(path + ".crystal-scale", 1.0)),
                    sanitizeMultiplier(config.getDouble(path + ".tnt-scale", 1.0)),
                    sanitizeMultiplier(config.getDouble(path + ".minecart-scale", 1.0)),
                    sanitizeMultiplier(config.getDouble(path + ".bed-anchor-scale", 1.0))
            );
            overrides.put(worldName.toLowerCase(Locale.ROOT), values);
        }
        return Collections.unmodifiableMap(overrides);
    }

    private static final class WorldScaleOverrides {
        private final double crystalScale;
        private final double tntScale;
        private final double tntCartScale;
        private final double blockBlastScale;

        private WorldScaleOverrides(double crystalScale, double tntScale, double tntCartScale, double blockBlastScale) {
            this.crystalScale = crystalScale;
            this.tntScale = tntScale;
            this.tntCartScale = tntCartScale;
            this.blockBlastScale = blockBlastScale;
        }

        private double multiplierFor(ExplosionSource source) {
            switch (source) {
                case END_CRYSTAL:
                    return crystalScale;
                case TNT:
                    return tntScale;
                case TNT_MINECART:
                    return tntCartScale;
                case BED_OR_ANCHOR:
                    return blockBlastScale;
                default:
                    return 1.0D;
            }
        }
    }

    public static double sanitizeMultiplier(double raw) {
        if (Double.isNaN(raw) || Double.isInfinite(raw) || raw < 0.0D) {
            return 0.0D;
        }
        return raw;
    }
}
