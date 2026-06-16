package com.example.ha;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaElementTracker {
    private static final double[] RANK_VALUES = new double[] {
        1.0D,
        5.0D,
        25.0D,
        125.0D,
        625.0D,
        3125.0D,
        15625.0D,
        78125.0D
    };

    private static boolean activeSession;
    private static long tickAccumulatorMillis;
    private static long lastTickMillis;

    private HaElementTracker() {
    }

    public static void tick(MinecraftClient client) {
        HaConfig config = HaConfig.get();
        if (client == null || client.player == null || client.world == null || !isTrackingAllowed(config)) {
            stopSession();
            return;
        }

        startSessionIfNeeded();
        tickElapsedTime(config);
    }

    public static void clear() {
        HaConfig config = HaConfig.get();
        config.elementTrackerElapsedSeconds = 0L;
        for (String elementKey : getElementKeys()) {
            HaConfig.ElementTrackerObservedCountEntry counts = config.getOrCreateElementTrackerObservedCounts(elementKey);
            counts.commonCount = 0L;
            counts.rareCount = 0L;
            counts.superiorCount = 0L;
            counts.epicCount = 0L;
            counts.legendaryCount = 0L;
            counts.transcendentCount = 0L;
            counts.untouchableCount = 0L;
            counts.uniqueCount = 0L;
        }
        config.save();
        stopSession();
    }

    public static boolean isActiveSession() {
        return activeSession;
    }

    public static long getElapsedSeconds() {
        return HaConfig.get().elementTrackerElapsedSeconds;
    }

    public static List<String> getElementKeys() {
        List<String> keys = new ArrayList<String>();
        for (ElementType type : ElementType.values()) {
            keys.add(type.getKey());
        }
        return keys;
    }

    public static List<ElementHudEntry> getEnabledEntries() {
        HaConfig config = HaConfig.get();
        List<ElementHudEntry> result = new ArrayList<ElementHudEntry>();
        for (ElementType type : ElementType.values()) {
            HaConfig.ElementTrackerTargetEntry target = config.getOrCreateElementTrackerTarget(type.getKey());
            if (!target.enabled) {
                continue;
            }
            HaConfig.ElementTrackerObservedCountEntry counts = config.getOrCreateElementTrackerObservedCounts(type.getKey());
            if (getCurrentValue(counts) <= 0.0D) {
                continue;
            }
            result.add(createHudEntry(type, target, counts, config.elementTrackerElapsedSeconds));
        }
        return result;
    }

    public static String getStatusText() {
        return activeSession ? "Tracking" : "Stopped";
    }

    public static boolean isTrackingAllowed(HaConfig config) {
        if (config == null || !config.elementTrackerEnabled) {
            return false;
        }
        return HaSoulbindProtection.isSoulbound()
            || (config.elementTrackerContinueAfterStart && (activeSession || config.elementTrackerElapsedSeconds > 0L));
    }

    public static void onItemPickup(ItemPickupAnimationS2CPacket packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        HaConfig config = HaConfig.get();
        if (packet == null || client == null || client.player == null || client.world == null || !isTrackingAllowed(config)) {
            return;
        }
        if (!client.isOnThread()) {
            return;
        }
        if (packet.getCollectorEntityId() != client.player.getEntityId()) {
            return;
        }

        Entity entity = client.world.getEntityById(packet.getEntityId());
        if (!(entity instanceof ItemEntity)) {
            return;
        }

        ParsedElementStack parsed = parseElementStack(client, ((ItemEntity) entity).getStack());
        if (parsed == null) {
            return;
        }

        startSessionIfNeeded();
        recordObservation(config, parsed.elementType, parsed.rank, Math.max(1, packet.getStackAmount()));
        HaConfigPersistence.markDirty();
    }

    private static ParsedElementStack parseElementStack(MinecraftClient client, ItemStack stack) {
        String displayName = HaElementRaritySlotHighlight.getMatchingElementName(stack);
        if (displayName == null) {
            return null;
        }

        ElementType type = ElementType.fromDisplayName(displayName);
        if (type == null) {
            return null;
        }

        List<Text> tooltip = HaEvolutionForgeHelper.getUnmodifiedTooltip(client, stack);
        ElementRank rank = ElementRank.fromTooltip(tooltip);
        if (rank == null) {
            return null;
        }

        ParsedElementStack parsed = new ParsedElementStack();
        parsed.elementType = type;
        parsed.rank = rank;
        return parsed;
    }

    private static void recordObservation(HaConfig config, ElementType type, ElementRank rank, int delta) {
        if (delta <= 0) {
            return;
        }
        HaConfig.ElementTrackerObservedCountEntry counts = config.getOrCreateElementTrackerObservedCounts(type.getKey());
        switch (rank) {
            case COMMON:
                counts.commonCount += delta;
                break;
            case RARE:
                counts.rareCount += delta;
                break;
            case SUPERIOR:
                counts.superiorCount += delta;
                break;
            case EPIC:
                counts.epicCount += delta;
                break;
            case LEGENDARY:
                counts.legendaryCount += delta;
                break;
            case TRANSCENDENT:
                counts.transcendentCount += delta;
                break;
            case UNTOUCHABLE:
                counts.untouchableCount += delta;
                break;
            case UNIQUE:
                counts.uniqueCount += delta;
                break;
            default:
                break;
        }
    }

    private static ElementHudEntry createHudEntry(ElementType type, HaConfig.ElementTrackerTargetEntry target, HaConfig.ElementTrackerObservedCountEntry counts, long elapsedSeconds) {
        ElementRank targetRank = ElementRank.fromKey(target.targetRank);
        double currentValue = getCurrentValue(counts);
        double targetValue = getTargetValue(targetRank);
        double progressRatio = targetValue <= 0.0D ? 0.0D : Math.min(1.0D, currentValue / targetValue);
        int progressPercent = (int) Math.round(progressRatio * 100.0D);
        String etaText;
        if (currentValue >= targetValue) {
            etaText = "Done";
        } else if (elapsedSeconds <= 0L || currentValue <= 0.0D) {
            etaText = "N/A";
        } else {
            double perSecond = currentValue / elapsedSeconds;
            etaText = perSecond <= 0.0D
                ? "N/A"
                : HaExpTrackerOverlay.formatDuration((long) Math.ceil((targetValue - currentValue) / perSecond));
        }

        return new ElementHudEntry(type, targetRank, progressPercent, etaText);
    }

    private static double getCurrentValue(HaConfig.ElementTrackerObservedCountEntry counts) {
        if (counts == null) {
            return 0.0D;
        }
        return counts.commonCount * RANK_VALUES[ElementRank.COMMON.ordinal()]
            + counts.rareCount * RANK_VALUES[ElementRank.RARE.ordinal()]
            + counts.superiorCount * RANK_VALUES[ElementRank.SUPERIOR.ordinal()]
            + counts.epicCount * RANK_VALUES[ElementRank.EPIC.ordinal()]
            + counts.legendaryCount * RANK_VALUES[ElementRank.LEGENDARY.ordinal()]
            + counts.transcendentCount * RANK_VALUES[ElementRank.TRANSCENDENT.ordinal()]
            + counts.untouchableCount * RANK_VALUES[ElementRank.UNTOUCHABLE.ordinal()]
            + counts.uniqueCount * RANK_VALUES[ElementRank.UNIQUE.ordinal()];
    }

    private static double getTargetValue(ElementRank rank) {
        return rank == null ? RANK_VALUES[ElementRank.LEGENDARY.ordinal()] : RANK_VALUES[rank.ordinal()];
    }

    private static void startSessionIfNeeded() {
        if (activeSession) {
            return;
        }
        activeSession = true;
        tickAccumulatorMillis = 0L;
        lastTickMillis = System.currentTimeMillis();
    }

    private static void stopSession() {
        activeSession = false;
        tickAccumulatorMillis = 0L;
        lastTickMillis = 0L;
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
            config.elementTrackerElapsedSeconds += seconds;
            HaConfigPersistence.markDirty();
        }
    }

    private static String normalize(String value) {
        String stripped = Formatting.strip(value);
        if (stripped == null) {
            stripped = value;
        }
        return stripped == null ? "" : stripped.trim();
    }

    public static final class ElementHudEntry {
        private final ElementType type;
        private final ElementRank targetRank;
        private final int progressPercent;
        private final String etaText;

        ElementHudEntry(ElementType type, ElementRank targetRank, int progressPercent, String etaText) {
            this.type = type;
            this.targetRank = targetRank;
            this.progressPercent = progressPercent;
            this.etaText = etaText;
        }

        public String getDisplayText() {
            return type.getDisplayName() + " " + progressPercent + "% -> " + targetRank.getLabel() + " " + etaText;
        }
    }

    public enum ElementType {
        FIRE("fire", "ファイアエレメント"),
        ICE("ice", "アイスエレメント"),
        THUNDER("thunder", "サンダーエレメント"),
        EARTH("earth", "アースエレメント"),
        DARK("dark", "ダークエレメント"),
        WIND("wind", "ウィンドエレメント"),
        WATER("water", "ウォーターエレメント"),
        LIGHT("light", "ライトエレメント"),
        SPACETIME("spacetime", "スペースタイムエレメント");

        private final String key;
        private final String displayName;

        ElementType(String key, String displayName) {
            this.key = key;
            this.displayName = displayName;
        }

        public String getKey() {
            return key;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static ElementType fromDisplayName(String displayName) {
            String normalized = displayName == null ? "" : displayName.trim();
            for (ElementType type : values()) {
                if (type.displayName.equals(normalized)) {
                    return type;
                }
            }
            return null;
        }

        public static ElementType fromKey(String key) {
            String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
            for (ElementType type : values()) {
                if (type.key.equals(normalized)) {
                    return type;
                }
            }
            return null;
        }
    }

    public enum ElementRank {
        COMMON("COMMON", "Common"),
        RARE("RARE", "Rare"),
        SUPERIOR("SUPERIOR", "Superior"),
        EPIC("EPIC", "Epic"),
        LEGENDARY("LEGENDARY", "Legendary"),
        TRANSCENDENT("TRANSCENDENT", "Transcendent"),
        UNTOUCHABLE("UNTOUCHABLE", "Untouchable"),
        UNIQUE("UNIQUE", "Unique");

        private final String key;
        private final String label;

        ElementRank(String key, String label) {
            this.key = key;
            this.label = label;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }

        public ElementRank nextTarget() {
            switch (this) {
                case LEGENDARY:
                    return TRANSCENDENT;
                case TRANSCENDENT:
                    return UNTOUCHABLE;
                case UNTOUCHABLE:
                    return UNIQUE;
                case UNIQUE:
                    return LEGENDARY;
                default:
                    return LEGENDARY;
            }
        }

        public static ElementRank fromKey(String key) {
            if (key == null) {
                return null;
            }
            String normalized = key.trim().toUpperCase(Locale.ROOT);
            if ("UNTOCHABLE".equals(normalized)) {
                normalized = "UNTOUCHABLE";
            }
            for (ElementRank rank : values()) {
                if (rank.key.equals(normalized)) {
                    return rank;
                }
            }
            return null;
        }

        public static ElementRank fromTooltip(List<Text> tooltip) {
            if (tooltip == null || tooltip.isEmpty()) {
                return null;
            }
            for (int i = tooltip.size() - 1; i >= 0; i--) {
                String line = normalize(tooltip.get(i) == null ? "" : tooltip.get(i).getString()).toUpperCase(Locale.ROOT);
                for (ElementRank rank : values()) {
                    if (line.contains(rank.key)) {
                        return rank;
                    }
                }
                if (line.contains("UNTOCHABLE")) {
                    return UNTOUCHABLE;
                }
            }
            return null;
        }
    }

    private static final class ParsedElementStack {
        ElementType elementType;
        ElementRank rank;
    }
}
