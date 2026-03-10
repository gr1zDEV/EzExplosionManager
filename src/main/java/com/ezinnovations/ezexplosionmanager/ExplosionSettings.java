package com.ezinnovations.ezexplosionmanager;

import org.bukkit.configuration.file.FileConfiguration;

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

    private ExplosionSettings(boolean debugActive,
                              double crystalScale,
                              double tntScale,
                              double tntCartScale,
                              double blockBlastScale,
                              boolean crystalSelfBypass) {
        this.debugActive = debugActive;
        this.crystalScale = crystalScale;
        this.tntScale = tntScale;
        this.tntCartScale = tntCartScale;
        this.blockBlastScale = blockBlastScale;
        this.crystalSelfBypass = crystalSelfBypass;
    }

    public static ExplosionSettings loadFromConfig(EzExplosionManagerPlugin plugin) {
        FileConfiguration config = plugin.getConfig();

        boolean debug = config.getBoolean("telemetry.verbose-debug", false);
        double crystal = sanitizeMultiplier(config.getDouble("blast-control.crystal-scale", 1.0));
        double tnt = sanitizeMultiplier(config.getDouble("blast-control.tnt-scale", 1.0));
        double cart = sanitizeMultiplier(config.getDouble("blast-control.minecart-scale", 1.0));
        double bedAnchor = sanitizeMultiplier(config.getDouble("blast-control.bed-anchor-scale", 1.0));
        boolean bypass = config.getBoolean("rules.crystal-owner-vanilla-damage", true);

        return new ExplosionSettings(debug, crystal, tnt, cart, bedAnchor, bypass);
    }

    public boolean isDebugActive() {
        return debugActive;
    }

    public boolean isCrystalSelfBypass() {
        return crystalSelfBypass;
    }

    public double multiplierFor(ExplosionSource source) {
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

    public static double sanitizeMultiplier(double raw) {
        if (Double.isNaN(raw) || Double.isInfinite(raw) || raw < 0.0D) {
            return 0.0D;
        }
        return raw;
    }
}
