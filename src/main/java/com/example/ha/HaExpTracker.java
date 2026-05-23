package com.example.ha;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaExpTracker {
    private static final double TRACKING_DISTANCE_SQUARED = 40.0D * 40.0D;
    private static final int SEEN_TTL_TICKS = 20 * 30;
    private static final int PENDING_TTL_TICKS = 20 * 2;
    private static final int DEBUG_LIMIT = 80;
    private static final Pattern XP_PATTERN = Pattern.compile("\\+\\s*([0-9][0-9,]*)\\s*(?:XP|EXP)\\s*!?", Pattern.CASE_INSENSITIVE);
    private static final Map<String, Integer> SEEN_EXP_EVENTS = new HashMap<String, Integer>();
    private static final Map<Integer, PendingEntity> PENDING_ENTITIES = new HashMap<Integer, PendingEntity>();
    private static final ArrayDeque<String> DEBUG_EVENTS = new ArrayDeque<String>();
    private static boolean activeSession;
    private static long cachedExpPerHour;
    private static long lastRateUpdateMillis;
    private static long tickAccumulatorMillis;
    private static long lastTickMillis;

    private HaExpTracker() {
    }

    public static void tick(MinecraftClient client) {
        tickCaches();
        HaConfig config = HaConfig.get();
        if (client == null || client.player == null || client.world == null || !isTrackingAllowed(config)) {
            stopSession();
            return;
        }

        startSessionIfNeeded(config);
        tickElapsedTime(config);
        updateHourlyRate(config);

        tickPendingEntities(client, config);
        for (Entity entity : client.world.getEntities()) {
            tryRecordExp(client, config, entity, "tick");
        }
    }

    public static void onEntitySpawn(EntitySpawnS2CPacket packet) {
        if (packet == null) {
            return;
        }
        registerPending(packet.getId(), packet.getUuid(), packet.getX(), packet.getY(), packet.getZ(), "entity_spawn");
        tryRecordPacketEntity(packet.getId(), "entity_spawn");
    }

    public static void onMobSpawn(MobSpawnS2CPacket packet) {
        if (packet == null) {
            return;
        }
        registerPending(packet.getId(), packet.getUuid(), packet.getX(), packet.getY(), packet.getZ(), "mob_spawn");
        tryRecordPacketEntity(packet.getId(), "mob_spawn");
    }

    public static void onEntityTrackerUpdate(EntityTrackerUpdateS2CPacket packet) {
        if (packet == null) {
            return;
        }
        tryRecordPacketEntity(packet.id(), "tracker_update");
    }

    public static boolean copyDebugLogToClipboard() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.keyboard == null) {
            return false;
        }

        StringBuilder result = new StringBuilder();
        result.append("Exp Tracker Debug Log\r\n");
        result.append("Tracking: ").append(isTrackingAllowed(HaConfig.get())).append("\r\n");
        result.append("Active: ").append(activeSession).append("\r\n");
        result.append("Total XP: ").append(HaConfig.get().expTrackerTotal).append("\r\n");
        result.append("Pending: ").append(PENDING_ENTITIES.size()).append("\r\n");
        result.append("Seen: ").append(SEEN_EXP_EVENTS.size()).append("\r\n");
        result.append("\r\n");
        for (String event : DEBUG_EVENTS) {
            result.append(event).append("\r\n");
        }
        client.keyboard.setClipboard(result.toString());
        return true;
    }

    public static void clear() {
        HaConfig config = HaConfig.get();
        config.expTrackerTotal = 0L;
        config.expTrackerElapsedSeconds = 0L;
        tickAccumulatorMillis = 0L;
        lastTickMillis = activeSession ? System.currentTimeMillis() : 0L;
        cachedExpPerHour = 0L;
        lastRateUpdateMillis = 0L;
        config.save();
        SEEN_EXP_EVENTS.clear();
        PENDING_ENTITIES.clear();
        DEBUG_EVENTS.clear();
    }

    public static boolean isActiveSession() {
        return activeSession;
    }

    public static long getElapsedSeconds() {
        return HaConfig.get().expTrackerElapsedSeconds;
    }

    public static long getExpPerHour() {
        return cachedExpPerHour;
    }

    public static boolean isTrackingAllowed(HaConfig config) {
        if (!config.expTrackerEnabled) {
            return false;
        }
        return HaSoulbindProtection.isSoulbound()
            || (config.expTrackerContinueAfterStart && (activeSession || config.expTrackerElapsedSeconds > 0L));
    }

    static long parseXp(String value) {
        if (value == null) {
            return 0L;
        }

        String stripped = Formatting.strip(value);
        if (stripped == null) {
            stripped = value;
        }
        Matcher matcher = XP_PATTERN.matcher(stripped.trim());
        if (!matcher.find()) {
            return 0L;
        }

        try {
            return Long.parseLong(matcher.group(1).replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static String getEntityName(Entity entity) {
        Text customName = entity.getCustomName();
        if (customName != null) {
            return customName.getString();
        }

        Text displayName = entity.getDisplayName();
        if (displayName != null) {
            return displayName.getString();
        }

        Text name = entity.getName();
        return name == null ? "" : name.getString();
    }

    private static boolean tryRecordExp(MinecraftClient client, HaConfig config, Entity entity, String source) {
        if (client == null || client.player == null || entity == null || entity == client.player) {
            addDebug(source, entity, 0L, "invalid_entity");
            return false;
        }
        if (entity.squaredDistanceTo(client.player) > TRACKING_DISTANCE_SQUARED) {
            addDebug(source, entity, 0L, "out_of_range");
            return false;
        }

        String name = getEntityName(entity);
        long exp = parseXp(name);
        if (exp <= 0L) {
            addDebug(source, entity, 0L, "no_xp");
            return false;
        }

        String key = expEventKey(entity, exp);
        if (SEEN_EXP_EVENTS.containsKey(key)) {
            SEEN_EXP_EVENTS.put(key, SEEN_TTL_TICKS);
            addDebug(source, entity, exp, "duplicate");
            return false;
        }

        config.expTrackerTotal += exp;
        updateHourlyRate(config);
        config.save();
        SEEN_EXP_EVENTS.put(key, SEEN_TTL_TICKS);
        addDebug(source, entity, exp, "recorded");
        return true;
    }

    private static void tryRecordPacketEntity(int entityId, String source) {
        MinecraftClient client = MinecraftClient.getInstance();
        HaConfig config = HaConfig.get();
        if (client == null || client.player == null || client.world == null || !isTrackingAllowed(config)) {
            addDebug(source, entityId, 0L, "tracking_disabled");
            return;
        }

        Entity entity = client.world.getEntityById(entityId);
        if (entity == null) {
            addDebug(source, entityId, 0L, "pending");
            return;
        }
        tryRecordExp(client, config, entity, source);
    }

    private static void tickPendingEntities(MinecraftClient client, HaConfig config) {
        for (Iterator<Map.Entry<Integer, PendingEntity>> iterator = PENDING_ENTITIES.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<Integer, PendingEntity> entry = iterator.next();
            PendingEntity pending = entry.getValue();
            pending.ttlTicks--;
            Entity entity = client.world.getEntityById(entry.getKey().intValue());
            if (entity != null) {
                boolean recorded = tryRecordExp(client, config, entity, pending.source + "_pending");
                if (recorded || parseXp(getEntityName(entity)) > 0L) {
                    iterator.remove();
                    continue;
                }
            }
            if (pending.ttlTicks <= 0) {
                addDebug(pending.source, pending.id, 0L, "pending_expired");
                iterator.remove();
            }
        }
    }

    private static void registerPending(int id, UUID uuid, double x, double y, double z, String source) {
        HaConfig config = HaConfig.get();
        if (!isTrackingAllowed(config)) {
            addDebug(source, id, 0L, "tracking_disabled");
            return;
        }

        PendingEntity pending = new PendingEntity();
        pending.id = id;
        pending.uuid = uuid;
        pending.x = x;
        pending.y = y;
        pending.z = z;
        pending.source = source;
        pending.ttlTicks = PENDING_TTL_TICKS;
        PENDING_ENTITIES.put(Integer.valueOf(id), pending);
        addDebug(source, id, 0L, "pending_registered");
    }

    private static String expEventKey(Entity entity, long exp) {
        if (entity.getUuid() != null) {
            return entity.getUuid().toString();
        }
        int x = (int) Math.floor(entity.getX());
        int y = (int) Math.floor(entity.getY());
        int z = (int) Math.floor(entity.getZ());
        return entity.getEntityId() + ":" + exp + ":" + x + ":" + y + ":" + z;
    }

    private static void tickCaches() {
        for (Iterator<Map.Entry<String, Integer>> iterator = SEEN_EXP_EVENTS.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, Integer> entry = iterator.next();
            int nextTtl = entry.getValue() - 1;
            if (nextTtl <= 0) {
                iterator.remove();
            } else {
                entry.setValue(nextTtl);
            }
        }
    }

    private static void startSessionIfNeeded(HaConfig config) {
        if (activeSession) {
            return;
        }
        activeSession = true;
        lastTickMillis = System.currentTimeMillis();
        tickAccumulatorMillis = 0L;
        lastRateUpdateMillis = 0L;
        updateHourlyRate(config);
        SEEN_EXP_EVENTS.clear();
        PENDING_ENTITIES.clear();
    }

    private static void stopSession() {
        if (!activeSession) {
            return;
        }
        activeSession = false;
        lastTickMillis = 0L;
        tickAccumulatorMillis = 0L;
        lastRateUpdateMillis = 0L;
        SEEN_EXP_EVENTS.clear();
        PENDING_ENTITIES.clear();
    }

    private static void tickElapsedTime(HaConfig config) {
        long now = System.currentTimeMillis();
        if (lastTickMillis <= 0L) {
            lastTickMillis = now;
            return;
        }

        long elapsedMillis = Math.max(0L, now - lastTickMillis);
        lastTickMillis = now;
        tickAccumulatorMillis += elapsedMillis;
        if (tickAccumulatorMillis >= 1000L) {
            long seconds = tickAccumulatorMillis / 1000L;
            tickAccumulatorMillis %= 1000L;
            config.expTrackerElapsedSeconds += seconds;
            config.save();
        }
    }

    private static void updateHourlyRate(HaConfig config) {
        long now = System.currentTimeMillis();
        if (now - lastRateUpdateMillis < 1000L) {
            return;
        }
        lastRateUpdateMillis = now;
        long elapsedSeconds = config.expTrackerElapsedSeconds;
        long gained = Math.max(0L, config.expTrackerTotal);
        cachedExpPerHour = elapsedSeconds <= 0L ? 0L : Math.round(gained * 3600.0D / elapsedSeconds);
    }

    private static void addDebug(String source, Entity entity, long exp, String result) {
        if (!shouldLogDebug(source, result)) {
            return;
        }
        if (entity == null) {
            addDebug(source, -1, exp, result);
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        double distance = client == null || client.player == null ? -1.0D : Math.sqrt(entity.squaredDistanceTo(client.player));
        String line = source
            + " result=" + result
            + " id=" + entity.getEntityId()
            + " uuid=" + entity.getUuid()
            + " dist=" + (distance < 0.0D ? "unknown" : String.format(java.util.Locale.ROOT, "%.2f", distance))
            + " exp=" + exp
            + " name=" + getEntityName(entity);
        addDebugLine(line);
    }

    private static void addDebug(String source, int entityId, long exp, String result) {
        if (!shouldLogDebug(source, result)) {
            return;
        }
        addDebugLine(source + " result=" + result + " id=" + entityId + " exp=" + exp);
    }

    private static boolean shouldLogDebug(String source, String result) {
        if ("recorded".equals(result)
            || "duplicate".equals(result)
            || "pending".equals(result)
            || "pending_registered".equals(result)
            || "pending_expired".equals(result)
            || "tracking_disabled".equals(result)) {
            return true;
        }
        return source != null && source.indexOf("tick") < 0;
    }

    private static void addDebugLine(String line) {
        DEBUG_EVENTS.addLast(System.currentTimeMillis() + " " + line);
        while (DEBUG_EVENTS.size() > DEBUG_LIMIT) {
            DEBUG_EVENTS.removeFirst();
        }
    }

    private static final class PendingEntity {
        int id;
        UUID uuid;
        double x;
        double y;
        double z;
        String source = "";
        int ttlTicks;
    }
}
