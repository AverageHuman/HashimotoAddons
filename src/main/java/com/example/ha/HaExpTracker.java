package com.example.ha;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaExpTracker {
    private static final double TRACKING_DISTANCE_SQUARED = 20.0D * 20.0D;
    private static final int SEEN_TTL_TICKS = 20 * 30;
    private static final Pattern XP_PATTERN = Pattern.compile("\\+\\s*([0-9][0-9,]*)\\s*(?:XP|EXP)\\s*!?", Pattern.CASE_INSENSITIVE);
    private static final Map<String, Integer> SEEN_ENTITIES = new HashMap<String, Integer>();
    private static boolean activeSession;
    private static long sessionStartMillis;
    private static long sessionStartTotal;
    private static long cachedExpPerHour;
    private static long lastRateUpdateMillis;

    private HaExpTracker() {
    }

    public static void tick(MinecraftClient client) {
        tickSeenCache();
        HaConfig config = HaConfig.get();
        if (client == null || client.player == null || client.world == null || !config.expTrackerEnabled || !HaSoulbindProtection.isSoulbound()) {
            stopSession();
            return;
        }

        startSessionIfNeeded(config);
        updateHourlyRate(config);

        for (Entity entity : client.world.getEntities()) {
            if (entity == null || entity == client.player || entity.squaredDistanceTo(client.player) > TRACKING_DISTANCE_SQUARED) {
                continue;
            }

            String key = entityKey(entity);
            if (SEEN_ENTITIES.containsKey(key)) {
                SEEN_ENTITIES.put(key, SEEN_TTL_TICKS);
                continue;
            }

            long exp = parseXp(getEntityName(entity));
            if (exp <= 0L) {
                continue;
            }

            config.expTrackerTotal += exp;
            updateHourlyRate(config);
            config.save();
            SEEN_ENTITIES.put(key, SEEN_TTL_TICKS);
        }
    }

    public static void clear() {
        HaConfig config = HaConfig.get();
        config.expTrackerTotal = 0L;
        if (activeSession) {
            sessionStartTotal = 0L;
            sessionStartMillis = System.currentTimeMillis();
            cachedExpPerHour = 0L;
            lastRateUpdateMillis = 0L;
        }
        config.save();
        SEEN_ENTITIES.clear();
    }

    public static boolean isActiveSession() {
        return activeSession;
    }

    public static long getElapsedSeconds() {
        if (!activeSession || sessionStartMillis <= 0L) {
            return 0L;
        }
        return Math.max(0L, (System.currentTimeMillis() - sessionStartMillis) / 1000L);
    }

    public static long getExpPerHour() {
        return cachedExpPerHour;
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

    private static String entityKey(Entity entity) {
        if (entity.getUuid() != null) {
            return entity.getUuid().toString();
        }
        return Integer.toString(entity.getEntityId());
    }

    private static void tickSeenCache() {
        for (Iterator<Map.Entry<String, Integer>> iterator = SEEN_ENTITIES.entrySet().iterator(); iterator.hasNext();) {
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
        sessionStartMillis = System.currentTimeMillis();
        sessionStartTotal = config.expTrackerTotal;
        cachedExpPerHour = 0L;
        lastRateUpdateMillis = 0L;
        SEEN_ENTITIES.clear();
    }

    private static void stopSession() {
        if (!activeSession) {
            return;
        }
        activeSession = false;
        sessionStartMillis = 0L;
        sessionStartTotal = 0L;
        cachedExpPerHour = 0L;
        lastRateUpdateMillis = 0L;
        SEEN_ENTITIES.clear();
    }

    private static void updateHourlyRate(HaConfig config) {
        long now = System.currentTimeMillis();
        if (now - lastRateUpdateMillis < 1000L) {
            return;
        }
        lastRateUpdateMillis = now;
        long elapsedSeconds = getElapsedSeconds();
        long gained = Math.max(0L, config.expTrackerTotal - sessionStartTotal);
        cachedExpPerHour = elapsedSeconds <= 0L ? 0L : Math.round(gained * 3600.0D / elapsedSeconds);
    }
}
