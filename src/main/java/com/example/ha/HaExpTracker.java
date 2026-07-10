package com.example.ha;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.registry.Registry;

public final class HaExpTracker {
    private static final double TRACKING_DISTANCE_SQUARED = 40.0D * 40.0D;
    private static final int SEEN_TTL_TICKS = 20 * 30;
    private static final int PENDING_TTL_TICKS = 20 * 2;
    private static final int LABEL_DEBUG_TTL_TICKS = 5;
    private static final int DEBUG_LIMIT = 200;
    private static final Pattern XP_PATTERN = Pattern.compile("\\+\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*(?:XP|EXP)\\s*!?", Pattern.CASE_INSENSITIVE);
    private static final Map<String, Integer> SEEN_EXP_EVENTS = new HashMap<String, Integer>();
    private static final Map<Integer, PendingEntity> PENDING_ENTITIES = new HashMap<Integer, PendingEntity>();
    private static final Map<String, Integer> RECENT_LABEL_EVENTS = new HashMap<String, Integer>();
    private static final ArrayDeque<String> DEBUG_EVENTS = new ArrayDeque<String>();
    private static boolean activeSession;
    private static long cachedExpPerHourTenths;
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
        HaConfig config = HaConfig.get();
        result.append("Exp Tracker Debug Log\r\n");
        result.append("Tracking: ").append(isTrackingAllowed(config)).append("\r\n");
        result.append("Active: ").append(activeSession).append("\r\n");
        result.append("Exp Tracker Enabled: ").append(config.expTrackerEnabled).append("\r\n");
        result.append("Auto Stop: ").append(!config.expTrackerContinueAfterStart).append("\r\n");
        result.append("Soulbind Active: ").append(HaSoulbindProtection.isSoulbound()).append("\r\n");
        result.append("Elapsed Seconds: ").append(config.expTrackerElapsedSeconds).append("\r\n");
        result.append("Total XP: ").append(HaExpTrackerOverlay.formatNumber(config.expTrackerTotalTenths, false)).append("\r\n");
        result.append("Label Tracking: numeric entity name tags are logged for damage analysis.\r\n");
        result.append("Unicode Log: visible text keeps raw Unicode and codepoints stay for exact-copy support.\r\n");
        result.append("Text Snapshot: root style and first rendered segment are logged for comparison.\r\n");
        result.append("Pending: ").append(PENDING_ENTITIES.size()).append("\r\n");
        result.append("Seen: ").append(SEEN_EXP_EVENTS.size()).append("\r\n");
        result.append("\r\n");
        for (String event : DEBUG_EVENTS) {
            result.append(event).append("\r\n");
        }
        client.keyboard.setClipboard(result.toString());
        return true;
    }

    public static void clearDebugLog() {
        DEBUG_EVENTS.clear();
        addDebugLine("===== DEBUG CLEARED =====");
    }

    public static void addDebugMarker(String marker) {
        String trimmed = marker == null ? "" : marker.trim();
        addDebugLine("===== MARK " + (trimmed.isEmpty() ? "(no label)" : escapeLogValue(trimmed)) + " =====");
    }

    public static void clear() {
        HaConfig config = HaConfig.get();
        config.expTrackerTotalTenths = 0L;
        config.expTrackerTotal = 0L;
        config.expTrackerElapsedSeconds = 0L;
        tickAccumulatorMillis = 0L;
        lastTickMillis = activeSession ? System.currentTimeMillis() : 0L;
        cachedExpPerHourTenths = 0L;
        lastRateUpdateMillis = 0L;
        config.save();
        SEEN_EXP_EVENTS.clear();
        PENDING_ENTITIES.clear();
        RECENT_LABEL_EVENTS.clear();
        DEBUG_EVENTS.clear();
    }

    public static void recordRenderedNameTag(Entity entity, Text text) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED || entity == null || text == null) {
            return;
        }

        String raw = text.getString();
        String normalized = normalizeLabel(raw);
        if (normalized.isEmpty() || !looksLikeDamageLabel(normalized)) {
            return;
        }

        String key = entity.getEntityId() + ":" + normalized;
        if (isRecentLabelEvent(key)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        double distance = client == null || client.player == null ? -1.0D : Math.sqrt(entity.squaredDistanceTo(client.player));
        String line = "render_label kind=damage"
            + " id=" + entity.getEntityId()
            + " uuid=" + entity.getUuid()
            + " type=" + getEntityTypeName(entity)
            + " dist=" + (distance < 0.0D ? "unknown" : String.format(java.util.Locale.ROOT, "%.2f", distance))
            + " text=" + describeText(text)
            + " raw=" + escapeLogValue(normalized);
        addDebugLine(line);
        RECENT_LABEL_EVENTS.put(key, Integer.valueOf(LABEL_DEBUG_TTL_TICKS));
    }

    public static boolean isActiveSession() {
        return activeSession;
    }

    public static long getElapsedSeconds() {
        return HaConfig.get().expTrackerElapsedSeconds;
    }

    public static long getExpPerHourTenths() {
        return cachedExpPerHourTenths;
    }

    public static boolean isTrackingAllowed(HaConfig config) {
        if (!config.expTrackerEnabled) {
            return false;
        }
        return HaSoulbindProtection.isSoulbound()
            || (config.expTrackerContinueAfterStart && (activeSession || config.expTrackerElapsedSeconds > 0L));
    }

    static long parseXp(String value) {
        return parseXpDetailed(value).exp;
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
            addDebug(source, entity, 0L, "invalid_entity", null, null);
            return false;
        }
        if (entity.squaredDistanceTo(client.player) > TRACKING_DISTANCE_SQUARED) {
            addDebug(source, entity, 0L, "out_of_range", null, null);
            return false;
        }

        String name = getEntityName(entity);
        ParseResult parseResult = parseXpDetailed(name);
        long exp = parseResult.exp;
        if (exp <= 0L) {
            addDebug(source, entity, 0L, "no_xp", parseResult, null);
            return false;
        }

        String key = expEventKey(entity, exp);
        if (SEEN_EXP_EVENTS.containsKey(key)) {
            SEEN_EXP_EVENTS.put(key, SEEN_TTL_TICKS);
            addDebug(source, entity, exp, "duplicate", parseResult, key);
            return false;
        }

        config.expTrackerTotalTenths += exp;
        config.expTrackerTotal = config.expTrackerTotalTenths / 10L;
        updateHourlyRate(config);
        HaConfigPersistence.markDirty();
        SEEN_EXP_EVENTS.put(key, SEEN_TTL_TICKS);
        addDebug(source, entity, exp, "recorded", parseResult, key);
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
                addPendingDebug(pending, "pending_expired", resolvePendingEntityType(client, pending.id));
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
        addPendingDebug(pending, "pending_registered", "unresolved");
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
        for (Iterator<Map.Entry<String, Integer>> iterator = RECENT_LABEL_EVENTS.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, Integer> entry = iterator.next();
            int nextTtl = entry.getValue().intValue() - 1;
            if (nextTtl <= 0) {
                iterator.remove();
            } else {
                entry.setValue(Integer.valueOf(nextTtl));
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
        RECENT_LABEL_EVENTS.clear();
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
        RECENT_LABEL_EVENTS.clear();
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
            HaConfigPersistence.markDirty();
        }
    }

    private static void updateHourlyRate(HaConfig config) {
        long now = System.currentTimeMillis();
        if (now - lastRateUpdateMillis < 1000L) {
            return;
        }
        lastRateUpdateMillis = now;
        long elapsedSeconds = config.expTrackerElapsedSeconds;
        long gainedTenths = Math.max(0L, config.expTrackerTotalTenths);
        cachedExpPerHourTenths = elapsedSeconds <= 0L ? 0L : Math.round(gainedTenths * 3600.0D / elapsedSeconds);
    }

    private static void addDebug(String source, Entity entity, long exp, String result, ParseResult parseResult, String duplicateKey) {
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
            + " type=" + getEntityTypeName(entity)
            + " dist=" + (distance < 0.0D ? "unknown" : String.format(java.util.Locale.ROOT, "%.2f", distance))
            + " exp=" + exp
            + " name=" + escapeLogValue(getEntityName(entity))
            + " customName=" + describeText(entity.getCustomName())
            + " displayName=" + describeText(entity.getDisplayName())
            + " entityName=" + describeText(entity.getName());
        if (isPacketDebugSource(source)) {
            line += " parseValue=" + (parseResult == null ? exp : parseResult.exp)
                + " parseReason=" + (parseResult == null ? "unknown" : parseResult.reason);
        }
        if ("duplicate".equals(result) && duplicateKey != null) {
            line += " duplicateKey=" + duplicateKey;
        }
        addDebugLine(line);
    }

    private static void addDebug(String source, int entityId, long exp, String result) {
        if (!shouldLogDebug(source, result)) {
            return;
        }
        addDebugLine(source + " result=" + result + " id=" + entityId + " exp=" + exp);
    }

    private static void addPendingDebug(PendingEntity pending, String result, String resolvedType) {
        if (pending == null || !shouldLogDebug(pending.source, result)) {
            return;
        }
        addDebugLine(
            pending.source
                + " result=" + result
                + " id=" + pending.id
                + " uuid=" + pending.uuid
                + " pos=" + String.format(java.util.Locale.ROOT, "%.2f,%.2f,%.2f", pending.x, pending.y, pending.z)
                + " resolvedType=" + resolvedType
                + " ttl=" + pending.ttlTicks
        );
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

    private static ParseResult parseXpDetailed(String value) {
        ParseResult result = new ParseResult();
        if (value == null) {
            result.reason = "value_null";
            return result;
        }

        String stripped = Formatting.strip(value);
        if (stripped == null) {
            stripped = value;
        }
        result.normalized = stripped.trim();
        if (result.normalized.isEmpty()) {
            result.reason = "empty_after_strip";
            return result;
        }

        Matcher matcher = XP_PATTERN.matcher(result.normalized);
        if (!matcher.find()) {
            result.reason = "regex_no_match";
            return result;
        }

        result.matchedToken = matcher.group(1);
        try {
            BigDecimal parsed = new BigDecimal(result.matchedToken.replace(",", ""));
            result.exp = parsed.movePointRight(1).setScale(0, RoundingMode.HALF_UP).longValueExact();
            result.reason = "matched";
        } catch (NumberFormatException ignored) {
            result.reason = "number_format_error";
        } catch (ArithmeticException ignored) {
            result.reason = "number_format_error";
        }
        return result;
    }

    private static boolean isPacketDebugSource(String source) {
        return source != null
            && (source.startsWith("entity_spawn")
                || source.startsWith("mob_spawn")
                || source.startsWith("tracker_update"));
    }

    private static String getEntityTypeName(Entity entity) {
        if (entity == null || entity.getType() == null) {
            return "none";
        }
        return Registry.ENTITY_TYPE.getId(entity.getType()).toString();
    }

    private static String resolvePendingEntityType(MinecraftClient client, int entityId) {
        if (client == null || client.world == null) {
            return "world_unavailable";
        }
        Entity entity = client.world.getEntityById(entityId);
        return entity == null ? "missing" : getEntityTypeName(entity);
    }

    private static String describeText(Text text) {
        if (text == null) {
            return "<null>";
        }
        String plain = text.getString();
        if (plain == null) {
            plain = "<null-string>";
        }

        String json;
        try {
            json = Text.Serializer.toJson(text);
        } catch (RuntimeException exception) {
            json = "<json_error:" + exception.getClass().getSimpleName() + ">";
        }

        return "{plain=\"" + escapeLogValue(plain)
            + "\", json=\"" + escapeLogValue(json)
            + "\", styleColor=\"" + describeStyleColor(text.getStyle())
            + "\", bold=" + isStyleBold(text.getStyle())
            + ", italic=" + isStyleItalic(text.getStyle())
            + ", firstSegment=" + describeFirstSegment(text)
            + ", codepoints=" + describeCodepoints(plain)
            + "}";
    }

    private static String describeFirstSegment(Text text) {
        if (text == null) {
            return "<null>";
        }

        final String[] segmentText = new String[1];
        final Style[] segmentStyle = new Style[1];
        try {
            text.visit((style, value) -> {
                if (value == null || value.isEmpty()) {
                    return Optional.empty();
                }
                segmentText[0] = value;
                segmentStyle[0] = style;
                return Optional.of(Boolean.TRUE);
            }, Style.EMPTY);
        } catch (RuntimeException exception) {
            return "<visit_error:" + exception.getClass().getSimpleName() + ">";
        }

        if (segmentText[0] == null) {
            return "<empty>";
        }

        return "{plain=\"" + escapeLogValue(segmentText[0])
            + "\", styleColor=\"" + describeStyleColor(segmentStyle[0])
            + "\", bold=" + isStyleBold(segmentStyle[0])
            + ", italic=" + isStyleItalic(segmentStyle[0])
            + ", codepoints=" + describeCodepoints(segmentText[0])
            + "}";
    }

    private static String describeStyleColor(Style style) {
        if (style == null) {
            return "<null>";
        }

        TextColor color = style.getColor();
        return color == null ? "inherit" : formatColor(color);
    }

    private static boolean isStyleBold(Style style) {
        return style != null && style.isBold();
    }

    private static boolean isStyleItalic(Style style) {
        return style != null && style.isItalic();
    }

    private static String formatColor(TextColor color) {
        if (color == null) {
            return "inherit";
        }
        return String.format(java.util.Locale.ROOT, "#%06X", Integer.valueOf(color.getRgb() & 0xFFFFFF));
    }

    private static String escapeLogValue(String value) {
        if (value == null) {
            return "<null>";
        }
        StringBuilder result = new StringBuilder(value.length());
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (codePoint == '\\') {
                result.append("\\\\");
            } else if (codePoint == '\"') {
                result.append("\\\"");
            } else if (codePoint == '\r') {
                result.append("\\r");
            } else if (codePoint == '\n') {
                result.append("\\n");
            } else if (codePoint == '\t') {
                result.append("\\t");
            } else {
                result.appendCodePoint(codePoint);
            }
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }

    private static String describeCodepoints(String value) {
        if (value == null || value.isEmpty()) {
            return "(empty)";
        }

        StringBuilder result = new StringBuilder();
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append("U+").append(codePointHex(codePoint));
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }

    private static String codePointHex(int codePoint) {
        String value = Integer.toHexString(codePoint).toUpperCase(java.util.Locale.ROOT);
        StringBuilder result = new StringBuilder();
        for (int i = value.length(); i < 4; i++) {
            result.append('0');
        }
        return result.append(value).toString();
    }

    private static String normalizeLabel(String value) {
        if (value == null) {
            return "";
        }
        String stripped = Formatting.strip(value);
        if (stripped == null) {
            stripped = value;
        }
        return stripped.trim();
    }

    private static boolean containsExp(String raw) {
        String normalized = Formatting.strip(raw);
        if (normalized == null) {
            normalized = raw;
        }
        return normalized != null && normalized.toLowerCase(java.util.Locale.ROOT).contains("exp");
    }

    private static boolean looksLikeDamageLabel(String value) {
        if (value == null || value.isEmpty() || containsExp(value)) {
            return false;
        }

        boolean hasDigit = false;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (Character.isDigit(codePoint)) {
                hasDigit = true;
            } else if (Character.isLetter(codePoint)) {
                return false;
            } else if (!Character.isWhitespace(codePoint) && ".,:+-!*/xX".indexOf(codePoint) < 0) {
                return false;
            }
            offset += Character.charCount(codePoint);
        }
        return hasDigit;
    }

    private static boolean isRecentLabelEvent(String key) {
        Integer ttl = RECENT_LABEL_EVENTS.get(key);
        if (ttl == null) {
            return false;
        }
        RECENT_LABEL_EVENTS.put(key, Integer.valueOf(LABEL_DEBUG_TTL_TICKS));
        return true;
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

    private static final class ParseResult {
        long exp;
        String normalized = "";
        String matchedToken = "";
        String reason = "unknown";
    }
}
