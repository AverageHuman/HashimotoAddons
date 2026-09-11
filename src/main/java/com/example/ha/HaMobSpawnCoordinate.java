package com.example.ha;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Keeps in-memory waypoints for entities whose custom name tag matches the configured name.
 * Spawn packets provide the immutable spawn position; entity metadata can arrive a few ticks later.
 */
public final class HaMobSpawnCoordinate {
    private static final int PENDING_TTL_TICKS = 100;
    private static final double GENERATION_PAUSE_DISTANCE_SQUARED = 100.0D;
    private static final long GENERATION_PAUSE_MILLIS = 5000L;
    private static final Map<Integer, PendingSpawn> PENDING_SPAWNS = new LinkedHashMap<Integer, PendingSpawn>();
    private static final List<HaWaypointManager.WaypointEntry> TEMPORARY_WAYPOINTS = new ArrayList<HaWaypointManager.WaypointEntry>();
    private static boolean lastEnabled;
    private static String lastTargetName = "";
    private static HaWaypointManager.WaypointEntry lastGeneratedWaypoint;
    private static long generationPausedUntilMillis;

    private HaMobSpawnCoordinate() {
    }

    public static void onEntitySpawn(EntitySpawnS2CPacket packet) {
        if (packet != null) {
            registerSpawn(packet.getId(), packet.getUuid(), packet.getX(), packet.getY(), packet.getZ());
        }
    }

    public static void onMobSpawn(MobSpawnS2CPacket packet) {
        if (packet != null) {
            registerSpawn(packet.getId(), packet.getUuid(), packet.getX(), packet.getY(), packet.getZ());
        }
    }

    public static void onEntityTrackerUpdate(EntityTrackerUpdateS2CPacket packet) {
        if (packet != null && tryResolve(packet.id())) {
            PENDING_SPAWNS.remove(Integer.valueOf(packet.id()));
        }
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.world == null) {
            clear();
            return;
        }

        HaMobSpawnCoordinateConfig config = HaConfig.get().mobSpawnCoordinate;
        config.normalize();
        clearWhenConfigurationChanged(config);
        if (!config.isActive()) {
            return;
        }

        updateGenerationPause(client);

        for (Iterator<Map.Entry<Integer, PendingSpawn>> iterator = PENDING_SPAWNS.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<Integer, PendingSpawn> entry = iterator.next();
            PendingSpawn pending = entry.getValue();
            if (tryResolve(entry.getKey().intValue())) {
                iterator.remove();
                continue;
            }
            pending.ttlTicks--;
            if (pending.ttlTicks <= 0) {
                iterator.remove();
            }
        }
    }

    public static void onConfigurationChanged() {
        clear();
    }

    public static void onDisconnected() {
        clear();
    }

    static List<HaWaypointManager.WaypointEntry> getWaypointsForCurrentDimension(MinecraftClient client) {
        String dimensionKey = HaWaypointManager.getCurrentDimensionKey(client);
        List<HaWaypointManager.WaypointEntry> result = new ArrayList<HaWaypointManager.WaypointEntry>();
        if (dimensionKey == null) {
            return result;
        }
        for (HaWaypointManager.WaypointEntry waypoint : TEMPORARY_WAYPOINTS) {
            if (dimensionKey.equals(waypoint.dimensionKey)) {
                result.add(waypoint);
            }
        }
        return result;
    }

    private static void registerSpawn(int id, UUID uuid, double x, double y, double z) {
        HaMobSpawnCoordinateConfig config = HaConfig.get().mobSpawnCoordinate;
        config.normalize();
        clearWhenConfigurationChanged(config);
        if (!config.isActive()) {
            return;
        }

        PendingSpawn pending = new PendingSpawn();
        pending.uuid = uuid;
        pending.pos = new BlockPos(x, y, z);
        pending.ttlTicks = PENDING_TTL_TICKS;
        PENDING_SPAWNS.put(Integer.valueOf(id), pending);
        if (tryResolve(id)) {
            PENDING_SPAWNS.remove(Integer.valueOf(id));
        }
    }

    private static boolean tryResolve(int entityId) {
        PendingSpawn pending = PENDING_SPAWNS.get(Integer.valueOf(entityId));
        if (pending == null) {
            return false;
        }

        HaMobSpawnCoordinateConfig config = HaConfig.get().mobSpawnCoordinate;
        config.normalize();
        if (!config.isActive()) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return false;
        }
        updateGenerationPause(client);
        if (isGenerationPaused(System.currentTimeMillis(), generationPausedUntilMillis)) {
            return true;
        }
        Entity entity = client.world.getEntityById(entityId);
        if (entity == null || (pending.uuid != null && !pending.uuid.equals(entity.getUuid()))) {
            return false;
        }
        if (!matchesTargetName(config.targetName, entity.getCustomName())) {
            return false;
        }

        String dimensionKey = HaWaypointManager.getCurrentDimensionKey(client);
        if (dimensionKey == null) {
            return false;
        }
        HaWaypointManager.WaypointEntry waypoint = new HaWaypointManager.WaypointEntry(
            dimensionKey,
            pending.pos.getX(),
            pending.pos.getY(),
            pending.pos.getZ(),
            config.targetName,
            config.colorSlotIndex
        );
        if (addTemporaryWaypoint(TEMPORARY_WAYPOINTS, waypoint)) {
            lastGeneratedWaypoint = waypoint;
        }
        return true;
    }

    private static void updateGenerationPause(MinecraftClient client) {
        if (lastGeneratedWaypoint == null || client == null || client.player == null) {
            return;
        }
        String currentDimension = HaWaypointManager.getCurrentDimensionKey(client);
        if (!lastGeneratedWaypoint.dimensionKey.equals(currentDimension)) {
            return;
        }
        if (isAtLeastPauseDistanceAway(
            client.player.getX(), client.player.getY(), client.player.getZ(),
            lastGeneratedWaypoint.x + 0.5D, lastGeneratedWaypoint.y + 0.5D, lastGeneratedWaypoint.z + 0.5D
        )) {
            generationPausedUntilMillis = System.currentTimeMillis() + GENERATION_PAUSE_MILLIS;
            lastGeneratedWaypoint = null;
        }
    }

    /** Keeps the first recorded spawn coordinate for each vertical X/Z column. */
    static boolean addTemporaryWaypoint(List<HaWaypointManager.WaypointEntry> waypoints, HaWaypointManager.WaypointEntry candidate) {
        if (waypoints == null || candidate == null) {
            return false;
        }
        for (HaWaypointManager.WaypointEntry waypoint : waypoints) {
            if (candidate.dimensionKey.equals(waypoint.dimensionKey)
                && candidate.x == waypoint.x
                && candidate.z == waypoint.z) {
                return false;
            }
        }
        waypoints.add(candidate);
        return true;
    }

    static boolean isAtLeastPauseDistanceAway(double playerX, double playerY, double playerZ, double waypointX, double waypointY, double waypointZ) {
        double xDistance = playerX - waypointX;
        double yDistance = playerY - waypointY;
        double zDistance = playerZ - waypointZ;
        return xDistance * xDistance + yDistance * yDistance + zDistance * zDistance >= GENERATION_PAUSE_DISTANCE_SQUARED;
    }

    static boolean isGenerationPaused(long currentMillis, long pausedUntilMillis) {
        return currentMillis < pausedUntilMillis;
    }

    private static void clearWhenConfigurationChanged(HaMobSpawnCoordinateConfig config) {
        boolean enabled = config != null && config.enabled;
        String targetName = config == null ? "" : config.targetName;
        if (lastEnabled != enabled || !lastTargetName.equals(targetName)) {
            PENDING_SPAWNS.clear();
            TEMPORARY_WAYPOINTS.clear();
            lastGeneratedWaypoint = null;
            generationPausedUntilMillis = 0L;
        }
        lastEnabled = enabled;
        lastTargetName = targetName;
    }

    private static void clear() {
        PENDING_SPAWNS.clear();
        TEMPORARY_WAYPOINTS.clear();
        lastGeneratedWaypoint = null;
        generationPausedUntilMillis = 0L;
        HaMobSpawnCoordinateConfig config = HaConfig.get().mobSpawnCoordinate;
        config.normalize();
        lastEnabled = config.enabled;
        lastTargetName = config.targetName;
    }

    static String normalizeNameTag(Text text) {
        if (text == null) {
            return "";
        }
        String stripped = Formatting.strip(text.getString());
        return stripped == null ? "" : stripped.trim();
    }

    static boolean matchesTargetName(String targetName, Text nameTag) {
        String normalizedTarget = targetName == null ? "" : targetName.trim();
        return !normalizedTarget.isEmpty() && normalizedTarget.equals(normalizeNameTag(nameTag));
    }

    private static final class PendingSpawn {
        private UUID uuid;
        private BlockPos pos;
        private int ttlTicks;
    }
}
