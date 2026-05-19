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
    private static final Pattern XP_PATTERN = Pattern.compile("\\+\\s*([0-9][0-9,]*)\\s*XP\\b", Pattern.CASE_INSENSITIVE);
    private static final Map<String, Integer> SEEN_ENTITIES = new HashMap<String, Integer>();

    private HaExpTracker() {
    }

    public static void tick(MinecraftClient client) {
        tickSeenCache();
        HaConfig config = HaConfig.get();
        if (client == null || client.player == null || client.world == null || !config.expTrackerEnabled) {
            return;
        }

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
            config.save();
            SEEN_ENTITIES.put(key, SEEN_TTL_TICKS);
        }
    }

    public static void clear() {
        HaConfig config = HaConfig.get();
        config.expTrackerTotal = 0L;
        config.save();
        SEEN_ENTITIES.clear();
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
}
