package com.ezinnovations.ezexplosionmanager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles explosion damage re-scaling and source attribution.
 */
public final class ExplosionDamageListener implements Listener {

    private static final long BLOCK_BLAST_TRACK_WINDOW_MS = 2500L;
    private static final double BLOCK_BLAST_DETECTION_RADIUS_SQUARED = 144.0D; // 12 blocks.

    private final EzExplosionManagerPlugin plugin;
    private final ExplosionSettings settings;

    private final Map<UUID, UUID> crystalAttackerByCrystal = new ConcurrentHashMap<UUID, UUID>();
    private final Map<LocationMarker, Long> recentBedOrAnchorBlasts = new ConcurrentHashMap<LocationMarker, Long>();

    public ExplosionDamageListener(EzExplosionManagerPlugin plugin, ExplosionSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCrystalSpawn(EntitySpawnEvent event) {
        // Keep attacker mapping clean if a UUID gets re-used across restarts/chunk unloads.
        if (event.getEntityType() == EntityType.ENDER_CRYSTAL) {
            crystalAttackerByCrystal.remove(event.getEntity().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCrystalDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal)) {
            return;
        }

        Player attacker = findResponsiblePlayer(event.getDamager());
        if (attacker != null) {
            crystalAttackerByCrystal.put(event.getEntity().getUniqueId(), attacker.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Material type = event.getBlock().getType();
        if (isBedOrAnchor(type)) {
            recentBedOrAnchorBlasts.put(LocationMarker.from(event.getBlock().getLocation()), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Defensive fallback: block explosions may be represented differently on some forks/versions.
        if (event.getEntityType() == EntityType.PRIMED_TNT || event.getEntityType() == EntityType.MINECART_TNT) {
            return;
        }
        if (event.getLocation().getBlock() != null && isBedOrAnchor(event.getLocation().getBlock().getType())) {
            recentBedOrAnchorBlasts.put(LocationMarker.from(event.getLocation()), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerExplosionDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        ExplosionSource source = resolveSource(event);
        if (source == null) {
            return;
        }

        double originalDamage = event.getDamage();
        boolean bypass = false;

        double multiplier = settings.multiplierFor(source);
        if (source == ExplosionSource.END_CRYSTAL && settings.isCrystalSelfBypass()) {
            if (isCrystalSelfDamage(player, event)) {
                multiplier = 1.0D;
                bypass = true;
            }
        }

        double finalDamage = Math.max(0.0D, originalDamage * multiplier);
        event.setDamage(finalDamage);

        writeDebug(player, source, originalDamage, finalDamage, bypass);
    }

    private ExplosionSource resolveSource(EntityDamageEvent event) {
        if (!(event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) {
            return null;
        }

        if (event instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
            if (damager.getType() == EntityType.ENDER_CRYSTAL) {
                return ExplosionSource.END_CRYSTAL;
            }
            if (damager.getType() == EntityType.MINECART_TNT) {
                return ExplosionSource.TNT_MINECART;
            }
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION && wasNearTrackedBedOrAnchor(event.getEntity().getLocation())) {
            return ExplosionSource.BED_OR_ANCHOR;
        }

        return null;
    }

    private boolean wasNearTrackedBedOrAnchor(Location location) {
        long now = System.currentTimeMillis();
        purgeOldBlasts(now);

        LocationMarker playerPos = LocationMarker.from(location);
        for (Map.Entry<LocationMarker, Long> entry : recentBedOrAnchorBlasts.entrySet()) {
            if (!entry.getKey().isSameWorld(playerPos)) {
                continue;
            }
            if (entry.getKey().distanceSquared(playerPos) <= BLOCK_BLAST_DETECTION_RADIUS_SQUARED) {
                return true;
            }
        }
        return false;
    }

    private void purgeOldBlasts(long now) {
        for (Map.Entry<LocationMarker, Long> entry : recentBedOrAnchorBlasts.entrySet()) {
            if (now - entry.getValue().longValue() > BLOCK_BLAST_TRACK_WINDOW_MS) {
                recentBedOrAnchorBlasts.remove(entry.getKey());
            }
        }
    }

    private boolean isCrystalSelfDamage(Player player, EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent)) {
            return false;
        }
        Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
        if (damager.getType() != EntityType.ENDER_CRYSTAL) {
            return false;
        }

        UUID owner = crystalAttackerByCrystal.get(damager.getUniqueId());
        return owner != null && owner.equals(player.getUniqueId());
    }

    private Player findResponsiblePlayer(Entity rawDamager) {
        if (rawDamager instanceof Player) {
            return (Player) rawDamager;
        }
        if (rawDamager instanceof Projectile) {
            Projectile projectile = (Projectile) rawDamager;
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }
        return null;
    }

    private boolean isBedOrAnchor(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.endsWith("_BED") || "RESPAWN_ANCHOR".equals(name);
    }

    private void writeDebug(Player damagedPlayer,
                            ExplosionSource source,
                            double originalDamage,
                            double finalDamage,
                            boolean bypassUsed) {
        if (!settings.isDebugActive()) {
            return;
        }

        plugin.getLogger().info(String.format(
                "[explosion-debug] target=%s source=%s before=%.3f after=%.3f selfBypass=%s",
                damagedPlayer.getName(),
                source.name(),
                originalDamage,
                finalDamage,
                bypassUsed
        ));
    }

    private static final class LocationMarker {
        private final String worldName;
        private final double x;
        private final double y;
        private final double z;

        private LocationMarker(String worldName, double x, double y, double z) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public static LocationMarker from(Location location) {
            String world = location.getWorld() == null ? "unknown" : location.getWorld().getName();
            return new LocationMarker(world, location.getX(), location.getY(), location.getZ());
        }

        public boolean isSameWorld(LocationMarker other) {
            return worldName.equals(other.worldName);
        }

        public double distanceSquared(LocationMarker other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LocationMarker)) {
                return false;
            }
            LocationMarker that = (LocationMarker) o;
            return Double.compare(that.x, x) == 0
                    && Double.compare(that.y, y) == 0
                    && Double.compare(that.z, z) == 0
                    && worldName.equals(that.worldName);
        }

        @Override
        public int hashCode() {
            int result = worldName.hashCode();
            long temp;
            temp = Double.doubleToLongBits(x);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(y);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(z);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }
}
