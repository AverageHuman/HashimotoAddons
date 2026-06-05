package com.example.ha;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public final class HaEvolutionForgeHelper {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("0.########", DecimalFormatSymbols.getInstance(Locale.US));
    private static final Path STORAGE_FILE = FabricLoader.getInstance().getConfigDir().resolve("HashimotoAddons").resolve("evolution_forge_items.json");
    private static final Path PREFIX_TOKEN_CANDIDATES_FILE = FabricLoader.getInstance().getConfigDir().resolve("HashimotoAddons").resolve("prefix_token_candidates.json");
    private static final Path ALLOWED_PREFIX_TOKENS_FILE = FabricLoader.getInstance().getConfigDir().resolve("HashimotoAddons").resolve("allowed_prefix_tokens.json");
    private static final String FORGE_TITLE = "\u30a8\u30dc\u30ea\u30e5\u30fc\u30b7\u30e7\u30f3\u30d5\u30a9\u30fc\u30b8";
    private static final String ARMOR_FORGE_TITLE = "\u30a8\u30dc\u30ea\u30e5\u30fc\u30b7\u30e7\u30f3\u30a2\u30fc\u30de\u30fc\u30d5\u30a9\u30fc\u30b8";
    private static final String RECIPE_PREVIEW_TITLE = "\u30ec\u30b7\u30d4\u30d7\u30ec\u30d3\u30e5\u30fc";
    private static final String CONSUME_HEADER = "\u88fd\u4f5c\u6642\u6d88\u8cbb\u30a2\u30a4\u30c6\u30e0";
    private static final String LEFT_CLICK = "\u5de6\u30af\u30ea\u30c3\u30af";
    private static final String RIGHT_CLICK = "\u53f3\u30af\u30ea\u30c3\u30af";
    private static final String ITEM_RANK_LABEL = "\u30a2\u30a4\u30c6\u30e0\u30e9\u30f3\u30af";
    private static final String MARKER = "Evo?: Yes";
    private static final Pattern LEADING_MARKERS = Pattern.compile("^[\\s\\u2715\\u2716\\u00d7xX*\\-:\\uFF1A\\u30FB]+");
    private static final Pattern LEADING_COUNT = Pattern.compile("^[0-9]+\\s+");
    private static final Pattern ENHANCEMENT_SUFFIX = Pattern.compile("\\s*\\(\\+([0-9]+)\\)\\s*$");
    private static final Pattern ITEM_KEY_ENHANCEMENT_SUFFIX = Pattern.compile("\\s*\\(\\+([1-9]|1[0-2])\\)\\s*$");
    private static final Pattern RANGE_LINE_PATTERN = Pattern.compile("^(.*?)([+\\-]?[0-9]+(?:\\.[0-9]+)?)(\\s*[~\\u2393\\uFF5E\\u301C\\-\\u2212\\u2013\\u2014]\\s*)([+\\-]?[0-9]+(?:\\.[0-9]+)?)([%\\uFF05]?)(.*)$");
    private static final Pattern CURRENT_VALUE_PATTERN = Pattern.compile("^(.*?)([+\\-]?[0-9]+(?:\\.[0-9]+)?)([%\\uFF05]?)(.*)$");
    private static final Pattern HP_BOOSTER_VALUE_PATTERN = Pattern.compile("増強剤(?:\\s*極)?(?:<[^>]+>)?.*?HP\\s*[+＋]([0-9]+(?:\\.[0-9]+)?)");
    private static final String OBSERVED_RANGE_SEPARATOR = "\u2393";
    private static final String MAX_HP_STAT_NAME = "最大HP";
    private static final Map<String, StatBoost> STAT_BOOSTS = createStatBoosts();
    private static final Map<String, Double> HP_BOOSTER_BONUSES = createHpBoosterBonuses();
    private static final double SPECIAL_SUBWEAPON_PERCENT_BOOST = 20.0D;
    private static final String SUBWEAPON_TOOLTIP_LABEL = "\u30b5\u30d6\u30a6\u30a7\u30dd\u30f3";
    private static final String SOUL_PROTECTOR_TOOLTIP_LABEL = "\u30bd\u30a6\u30eb\u30d7\u30ed\u30c6\u30af\u30bf\u30fc";
    private static final String NO_TRACK_ITEM_LABEL = "\u306e\u52a0\u8b77";
    private static final String[] ITEM_NAME_EXCEPTION_PREFIXES = new String[] {
        "\u5b8c\u5168\u7121\u6b20\u306e",
        "\u6975\u81f4\u306e",
        "\u5353\u8d8a\u3057\u305f",
        "\u8a08\u308a\u77e5\u308c\u306a\u3044"
    };
    private static final Set<String> DISABLED_PREFIX_TOKENS = new LinkedHashSet<String>(Arrays.asList(
        "\u7d14\u771f\u306a\u8a18\u61b6"
    ));
    private static final Set<String> TRACKED_STAT_NAMES = new LinkedHashSet<String>(Arrays.asList(
        "HP\u81ea\u7136\u56de\u5fa9",
        "MANA\u81ea\u7136\u56de\u5fa9",
        "SCRI\u5897\u52a0",
        "\u30a2\u30f3\u30c7\u30c3\u30c9\u7279\u653b",
        "\u30a8\u30f3\u30c1\u30e3\u30f3\u30c8\u6210\u529f\u7387",
        "\u30af\u30ea\u30c6\u30a3\u30ab\u30eb\u30c0\u30e1\u30fc\u30b8",
        "\u30af\u30ea\u30c6\u30a3\u30ab\u30eb\u7387",
        "\u30b7\u30e3\u30a4\u30f3\u30d1\u30ef\u30fc",
        "\u30b9\u30ad\u30eb\u30af\u30ea\u30c6\u30a3\u30ab\u30eb\u30c0\u30e1\u30fc\u30b8",
        "\u30b9\u30ad\u30eb\u30af\u30ea\u30c6\u30a3\u30ab\u30eb\u7387",
        "\u30b9\u30ad\u30eb\u30af\u30fc\u30eb\u30c0\u30a6\u30f3\u77ed\u7e2e",
        "\u30b9\u30ad\u30eb\u30c0\u30e1\u30fc\u30b8",
        "\u30c0\u30e1\u30fc\u30b8\u7121\u52b9\u5316\u30af\u30fc\u30eb\u30c0\u30a6\u30f3\u77ed\u7e2e",
        "\u30c0\u30e1\u30fc\u30b8\u7121\u52b9\u5316\u7387",
        "\u30c9\u30ed\u30c3\u30d7\u7387\u5897\u52a0",
        "\u30ce\u30c3\u30af\u30d0\u30c3\u30af\u8010\u6027",
        "\u30d1\u30ea\u30fc\u30af\u30fc\u30eb\u30c0\u30a6\u30f3\u77ed\u7e2e",
        "\u30d1\u30ea\u30fc\u7387",
        "\u5149 \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8",
        "\u5149 \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8\u5897\u5e45",
        "\u5149 \u5c5e\u6027\u9632\u5fa1",
        "\u571f \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8",
        "\u571f \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8\u5897\u5e45",
        "\u571f \u5c5e\u6027\u9632\u5fa1",
        "\u5bfeMOB\u30c0\u30e1\u30fc\u30b8",
        "\u5c04\u7a0b",
        "\u653b\u6483\u529b",
        "\u653b\u6483\u901f\u5ea6",
        "\u6642\u7a7a \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8",
        "\u6642\u7a7a \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8\u5897\u5e45",
        "\u6642\u7a7a \u5c5e\u6027\u9632\u5fa1",
        "\u6700\u5927HP",
        "\u6700\u5927MANA",
        "\u6700\u5927\u30b9\u30bf\u30df\u30ca",
        "\u6b66\u5668\u30c0\u30e1\u30fc\u30b8",
        "\u6c34 \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8",
        "\u6c34 \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8\u5897\u5e45",
        "\u6c34 \u5c5e\u6027\u9632\u5fa1",
        "\u6c37 \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8",
        "\u6c37 \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8\u5897\u5e45",
        "\u6c37 \u5c5e\u6027\u9632\u5fa1",
        "\u706b \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8",
        "\u706b \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8\u5897\u5e45",
        "\u706b \u5c5e\u6027\u9632\u5fa1",
        "\u7269\u7406\u30c0\u30e1\u30fc\u30b8",
        "\u767a\u5c04\u4f53\u30c0\u30e1\u30fc\u30b8",
        "\u7d76\u5bfe\u9632\u5fa1",
        "\u885d\u6483\u7bc4\u56f2",
        "\u88ab\u30c0\u30e1\u30fc\u30b8\u8efd\u6e1b",
        "\u88ab\u7269\u7406\u30c0\u30e1\u30fc\u30b8\u8efd\u6e1b",
        "\u88ab\u767a\u5c04\u4f53\u30c0\u30e1\u30fc\u30b8\u8efd\u6e1b",
        "\u88ab\u843d\u4e0b\u30c0\u30e1\u30fc\u30b8\u8efd\u6e1b",
        "\u88ab\u9b54\u6cd5\u30b9\u30ad\u30eb\u30c0\u30e1\u30fc\u30b8\u8efd\u6e1b",
        "\u8ffd\u52a0\u79fb\u52d5\u901f\u5ea6",
        "\u8ffd\u52a0\u7d4c\u9a13\u5024",
        "\u95c7 \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8",
        "\u95c7 \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8\u5897\u5e45",
        "\u95c7 \u5c5e\u6027\u9632\u5fa1",
        "\u9632\u5177",
        "\u96f7 \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8",
        "\u96f7 \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8\u5897\u5e45",
        "\u96f7 \u5c5e\u6027\u9632\u5fa1",
        "\u9811\u5f37\u3055",
        "\u98a8 \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8",
        "\u98a8 \u5c5e\u6027\u30c0\u30e1\u30fc\u30b8\u5897\u5e45",
        "\u98a8 \u5c5e\u6027\u9632\u5fa1",
        "\u9b54\u6cd5\u30c0\u30e1\u30fc\u30b8"
    ));
    private static final Map<String, EvolutionForgeData> DATA_BY_SERVER = new LinkedHashMap<String, EvolutionForgeData>();
    private static final Map<String, PrefixTokenCandidate> PREFIX_TOKEN_CANDIDATES = new LinkedHashMap<String, PrefixTokenCandidate>();
    private static final Set<String> ALLOWED_PREFIX_TOKENS = new LinkedHashSet<String>();
    private static boolean loaded;
    private static boolean prefixTokensLoaded;
    private static boolean scanningForgeTooltips;
    private static int lastSyncId = -1;
    private static String lastSignature = "";
    private static int trackedScreenSyncId = -1;
    private static String trackedScreenTitle = "";
    private static String previousContainerTitle = "";
    private static boolean recipePreviewCanRegisterMaterials;

    private HaEvolutionForgeHelper() {
    }

    public static void tick(MinecraftClient client) {
        if (!HaConfig.get().evolutionForgeHelperEnabled) {
            lastSyncId = -1;
            lastSignature = "";
            resetScreenTracking();
            return;
        }
        if (client == null || !(client.currentScreen instanceof GenericContainerScreen)) {
            lastSyncId = -1;
            lastSignature = "";
            resetScreenTracking();
            return;
        }

        GenericContainerScreen screen = (GenericContainerScreen) client.currentScreen;
        GenericContainerScreenHandler handler = (GenericContainerScreenHandler) screen.getScreenHandler();
        String normalizedTitle = getNormalizedCurrentScreenTitle(client);
        if (handler.syncId != trackedScreenSyncId || !normalizedTitle.equals(trackedScreenTitle)) {
            recipePreviewCanRegisterMaterials = isRecipePreviewTitle(normalizedTitle) && isForgeRecipeSourceTitle(previousContainerTitle);
            trackedScreenSyncId = handler.syncId;
            trackedScreenTitle = normalizedTitle;
        }
        if (!isRecipePreviewTitle(normalizedTitle)) {
            previousContainerTitle = normalizedTitle;
        }

        String signature = createSignature(handler);
        if (handler.syncId == lastSyncId && signature.equals(lastSignature)) {
            return;
        }

        lastSyncId = handler.syncId;
        lastSignature = signature;
        scanVisiblePage(client, handler);
    }

    public static List<Text> applyTooltipAnnotations(ItemStack stack, List<Text> tooltip) {
        if (scanningForgeTooltips || !HaConfig.get().evolutionForgeHelperEnabled || stack == null || stack.isEmpty() || tooltip == null) {
            return tooltip;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        learnDisplayedTooltip(client, stack, tooltip);

        List<Text> updatedTooltip = appendStatRangeAnnotations(stack, tooltip);
        if (shouldMarkTooltip(stack, updatedTooltip)) {
            if (updatedTooltip == tooltip) {
                updatedTooltip = new ArrayList<Text>(tooltip);
            }
            appendMarker(updatedTooltip);
        }
        return updatedTooltip;
    }

    public static int getCurrentServerItemCount() {
        return getDataForCurrentServer().items.size();
    }

    public static int getCurrentServerStatRangeCount() {
        return getStatRangeCount(getDataForCurrentServer());
    }

    public static int getCurrentServerObservedBoundCount() {
        return getObservedBoundCount(getDataForCurrentServer());
    }

    public static void clearCurrentServerItems() {
        load();
        DATA_BY_SERVER.remove(getServerKey(MinecraftClient.getInstance()));
        save();
    }

    public static void onOpenScreen(int syncId, Text title) {
        String newTitle = normalizeDisplay(title == null ? "" : title.getString());
        String currentTitle = getNormalizedCurrentScreenTitle(MinecraftClient.getInstance());
        if (!currentTitle.isEmpty() && !isRecipePreviewTitle(currentTitle)) {
            previousContainerTitle = currentTitle;
        }

        recipePreviewCanRegisterMaterials = isRecipePreviewTitle(newTitle) && isForgeRecipeSourceTitle(previousContainerTitle);
        trackedScreenSyncId = syncId;
        trackedScreenTitle = newTitle;
        lastSyncId = -1;
        lastSignature = "";

        if (!newTitle.isEmpty() && !isRecipePreviewTitle(newTitle)) {
            previousContainerTitle = newTitle;
        }
    }

    private static void scanVisiblePage(MinecraftClient client, GenericContainerScreenHandler handler) {
        load();
        EvolutionForgeData data = getOrCreateData(getServerKey(client));
        int beforeItems = data.items.size();
        int beforeRanges = getStatRangeCount(data);
        int beforeObservedBounds = getObservedBoundCount(data);
        int beforePrefixTokenCount = PREFIX_TOKEN_CANDIDATES.size();
        boolean shouldScanForgeData = isEvolutionForgeScreen(client);

        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                continue;
            }
            List<Text> tooltip = getRawTooltip(client, stack);
            if (shouldScanForgeData && shouldRegisterConsumedItems(client)) {
                addConsumedItems(data.items, tooltip);
            }
            if (shouldScanForgeData) {
                addStatRanges(data, stack.getName().getString(), tooltip);
            }
            collectPrefixTokens(client, tooltip);
            addObservedBounds(data, stack.getName().getString(), tooltip, getEnhancementLevel(stack, tooltip));
        }

        if (data.items.size() != beforeItems
            || getStatRangeCount(data) != beforeRanges
            || getObservedBoundCount(data) != beforeObservedBounds
            || PREFIX_TOKEN_CANDIDATES.size() != beforePrefixTokenCount) {
            save();
        }
    }

    public static List<Text> getUnmodifiedTooltip(MinecraftClient client, ItemStack stack) {
        return getRawTooltip(client, stack);
    }

    private static List<Text> getRawTooltip(MinecraftClient client, ItemStack stack) {
        scanningForgeTooltips = true;
        try {
            return stack.getTooltip(client.player, TooltipContext.Default.NORMAL);
        } finally {
            scanningForgeTooltips = false;
        }
    }

    private static void learnDisplayedTooltip(MinecraftClient client, ItemStack stack, List<Text> tooltip) {
        load();
        EvolutionForgeData data = getOrCreateData(getServerKey(client));
        int beforeItems = data.items.size();
        int beforeRanges = getStatRangeCount(data);
        int beforeObservedBounds = getObservedBoundCount(data);
        int beforePrefixTokenCount = PREFIX_TOKEN_CANDIDATES.size();

        if (isEvolutionForgeScreen(client)) {
            if (shouldRegisterConsumedItems(client)) {
                addConsumedItems(data.items, tooltip);
            }
            addStatRanges(data, stack.getName().getString(), tooltip);
        }
        collectPrefixTokens(client, tooltip);
        addObservedBounds(data, stack.getName().getString(), tooltip, getEnhancementLevel(stack, tooltip));

        if (data.items.size() != beforeItems
            || getStatRangeCount(data) != beforeRanges
            || getObservedBoundCount(data) != beforeObservedBounds
            || PREFIX_TOKEN_CANDIDATES.size() != beforePrefixTokenCount) {
            save();
        }
    }

    private static void addConsumedItems(Set<String> items, List<Text> tooltip) {
        boolean consuming = false;
        for (Text text : tooltip) {
            String line = normalizeDisplay(text == null ? "" : text.getString());
            if (line.indexOf(CONSUME_HEADER) >= 0) {
                consuming = true;
                int separator = Math.max(line.indexOf(':'), line.indexOf('\uff1a'));
                if (separator >= 0 && separator + 1 < line.length()) {
                    addCandidate(items, line.substring(separator + 1));
                }
                continue;
            }
            if (!consuming) {
                continue;
            }
            if (line.isEmpty() || MARKER.equals(line) || line.indexOf(LEFT_CLICK) >= 0 || line.indexOf(RIGHT_CLICK) >= 0) {
                continue;
            }
            addCandidate(items, line);
        }
    }

    private static void addStatRanges(EvolutionForgeData data, String itemName, List<Text> tooltip) {
        String normalizedItemName = resolveTrackedItemName(itemName, tooltip);
        if (normalizedItemName.isEmpty()) {
            return;
        }

        ItemStatAdjustments adjustments = getItemStatAdjustments(tooltip);
        List<StatRange> ranges = data.statRangesByItem.get(normalizedItemName);
        for (Text text : tooltip) {
            StatRange range = parseRangeLine(text == null ? "" : text.getString());
            if (range == null) {
                continue;
            }
            applyStatRangeAdjustments(range, adjustments);
            if (ranges == null) {
                ranges = new ArrayList<StatRange>();
                data.statRangesByItem.put(normalizedItemName, ranges);
            }
            putRange(ranges, range);
        }
    }

    private static void addObservedBounds(EvolutionForgeData data, String itemName, List<Text> tooltip, int enhancementLevel) {
        String normalizedItemName = resolveTrackedItemName(itemName, tooltip);
        if (normalizedItemName.isEmpty() || tooltip == null || tooltip.isEmpty()) {
            return;
        }

        ItemStatAdjustments adjustments = getItemStatAdjustments(tooltip);
        ItemEnhancementProfile enhancementProfile = getItemEnhancementProfile(tooltip);
        List<ObservedStatBound> bounds = data.observedBoundsByItem.get(normalizedItemName);
        for (Text text : tooltip) {
            ParsedCurrentStat parsed = parseCurrentStat(text == null ? "" : text.getString());
            if (parsed == null) {
                continue;
            }

            double storedValue = applyTrackedStatAdjustments(parsed.statName, parsed.value, adjustments);
            StatBoost boost = STAT_BOOSTS.get(parsed.statName);
            if (enhancementLevel > 0 && boost != null) {
                storedValue = estimateBaseStatValue(storedValue, boost, enhancementLevel, enhancementProfile);
            }

            ObservedStatBound bound = new ObservedStatBound();
            bound.statName = parsed.statName;
            bound.unit = parsed.unit;
            bound.hasMin = true;
            bound.hasMax = true;
            bound.min = storedValue;
            bound.max = storedValue;
            String displayValue = formatSignedValue(storedValue);
            bound.displayMin = displayValue;
            bound.displayMax = displayValue;

            if (bounds == null) {
                bounds = new ArrayList<ObservedStatBound>();
                data.observedBoundsByItem.put(normalizedItemName, bounds);
            }
            putObservedBound(bounds, bound);
        }
    }

    private static void addCandidate(Set<String> items, String rawLine) {
        String normalized = normalizeItemName(rawLine);
        if (!normalized.isEmpty()) {
            items.add(normalized);
        }
    }

    private static void collectPrefixTokens(MinecraftClient client, List<Text> tooltip) {
        loadPrefixTokens();
        if (tooltip == null || tooltip.isEmpty()) {
            return;
        }

        Text nameLine = tooltip.get(0);
        String normalizedName = normalizeDisplay(nameLine == null ? "" : nameLine.getString());
        if (normalizedName.isEmpty() || shouldSkipTrackingName(normalizedName)) {
            return;
        }

        String[] tokens = normalizedName.split("\\s+");
        String serverKey = getServerKey(client);
        for (int i = 0; i < tokens.length; i++) {
            String token = normalizeDisplay(tokens[i]);
            if (token.isEmpty()) {
                continue;
            }
            PrefixTokenCandidate candidate = PREFIX_TOKEN_CANDIDATES.get(token);
            if (candidate == null) {
                candidate = new PrefixTokenCandidate();
                candidate.token = token;
                PREFIX_TOKEN_CANDIDATES.put(token, candidate);
            }
            candidate.count++;
            if (!serverKey.isEmpty()) {
                candidate.servers.add(serverKey);
            }
            candidate.positions.add(Integer.valueOf(i));
            if (candidate.examples.size() < 10) {
                candidate.examples.add(normalizedName);
            }
        }
    }

    private static List<Text> appendStatRangeAnnotations(ItemStack stack, List<Text> tooltip) {
        List<StatRange> ranges = getRangesForStack(stack, tooltip);
        List<ObservedStatBound> observedBounds = getObservedBoundsForStack(stack, tooltip);
        if (ranges.isEmpty() && observedBounds.isEmpty()) {
            return tooltip;
        }

        int enhancementLevel = getEnhancementLevel(stack, tooltip);
        ItemStatAdjustments adjustments = getItemStatAdjustments(tooltip);
        ItemEnhancementProfile enhancementProfile = getItemEnhancementProfile(tooltip);
        List<Text> updatedTooltip = null;
        for (int i = 0; i < tooltip.size(); i++) {
            Text text = tooltip.get(i);
            String originalLine = text == null ? "" : text.getString();
            String annotatedLine = annotateStatLine(originalLine, ranges, observedBounds, enhancementLevel, adjustments, enhancementProfile);
            if (annotatedLine == null) {
                continue;
            }
            if (updatedTooltip == null) {
                updatedTooltip = new ArrayList<Text>(tooltip);
            }
            updatedTooltip.set(i, new LiteralText(annotatedLine));
        }
        return updatedTooltip == null ? tooltip : updatedTooltip;
    }

    private static String annotateStatLine(String rawLine, List<StatRange> ranges, List<ObservedStatBound> observedBounds, int enhancementLevel, ItemStatAdjustments adjustments, ItemEnhancementProfile enhancementProfile) {
        ParsedCurrentStat parsed = parseCurrentStat(rawLine);
        if (parsed == null) {
            return null;
        }

        double adjustedValue = applyTrackedStatAdjustments(parsed.statName, parsed.value, adjustments);
        StatRange range = findRange(ranges, parsed.statName, parsed.unit);
        if (range == null) {
            ObservedStatBound observedBound = findObservedBound(observedBounds, parsed.statName, parsed.unit);
            if (observedBound == null) {
                return null;
            }

            StatBoost observedBoost = STAT_BOOSTS.get(parsed.statName);
            double trueValue = enhancementLevel > 0 && observedBoost != null
                ? estimateBaseStatValue(adjustedValue, observedBoost, enhancementLevel, enhancementProfile)
                : adjustedValue;
            if (shouldSuppressObservedBoundAnnotation(trueValue, observedBound)) {
                return null;
            }
            return formatObservedAnnotation(parsed, observedBound, trueValue);
        }

        StatBoost boost = STAT_BOOSTS.get(parsed.statName);
        double trueValue = enhancementLevel > 0 && boost != null
            ? estimateBaseStatValue(adjustedValue, boost, enhancementLevel, enhancementProfile)
            : adjustedValue;
        if (shouldSuppressRangeAnnotation(trueValue, range)) {
            return null;
        }
        int percentage = calculateRangePercentage(trueValue, range);
        return parsed.prefix
            + parsed.valueText
            + formatTrueValueSuffix(parsed.value, trueValue)
            + "("
            + range.displayMin
            + range.separator
            + range.displayMax
            + " || ("
            + percentage
            + "%))"
            + parsed.unitText
            + parsed.suffix;
    }

    private static StatRange parseRangeLine(String rawLine) {
        String line = normalizeDisplay(rawLine);
        if (line.isEmpty() || line.indexOf("||") >= 0) {
            return null;
        }

        Matcher matcher = RANGE_LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }

        String minText = matcher.group(2);
        String separator = matcher.group(3).trim();
        String maxText = matcher.group(4);
        String unit = normalizeUnit(matcher.group(5));
        String statName = extractStatName(matcher.group(1), matcher.group(6));
        if (statName.isEmpty() || isIgnoredStatName(statName)) {
            return null;
        }

        try {
            double first = Double.parseDouble(minText);
            double second = Double.parseDouble(maxText);
            double min = Math.min(first, second);
            double max = Math.max(first, second);
            StatRange range = new StatRange();
            range.statName = statName;
            range.min = min;
            range.max = max;
            range.unit = unit == null ? "" : unit;
            range.separator = separator.isEmpty() ? "~" : separator;
            range.displayMin = first <= second ? normalizeRangeDisplay(minText, true) : formatRangeValue(min, true);
            range.displayMax = first <= second ? normalizeRangeDisplay(maxText, false) : formatRangeValue(max, false);
            return range;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void putRange(List<StatRange> ranges, StatRange newRange) {
        for (int i = 0; i < ranges.size(); i++) {
            StatRange existing = ranges.get(i);
            if (newRange.statName.equals(existing.statName)) {
                ranges.set(i, newRange);
                return;
            }
        }
        ranges.add(newRange);
    }

    private static void putObservedBound(List<ObservedStatBound> bounds, ObservedStatBound newBound) {
        for (ObservedStatBound existing : bounds) {
            if (!matchesObservedBound(existing, newBound)) {
                continue;
            }
            if (newBound.hasMin && (!existing.hasMin || newBound.min < existing.min)) {
                existing.hasMin = true;
                existing.min = newBound.min;
                existing.displayMin = newBound.displayMin;
            }
            if (newBound.hasMax && (!existing.hasMax || newBound.max > existing.max)) {
                existing.hasMax = true;
                existing.max = newBound.max;
                existing.displayMax = newBound.displayMax;
            }
            if ((existing.unit == null || existing.unit.isEmpty()) && newBound.unit != null && !newBound.unit.isEmpty()) {
                existing.unit = newBound.unit;
            }
            return;
        }
        bounds.add(newBound);
    }

    private static StatRange findRange(List<StatRange> ranges, String statName, String unit) {
        for (StatRange range : ranges) {
            if (range == null || range.statName == null || !range.statName.equals(statName)) {
                continue;
            }
            if (range.unit == null || range.unit.isEmpty() || range.unit.equals(unit)) {
                return range;
            }
        }
        return null;
    }

    private static ObservedStatBound findObservedBound(List<ObservedStatBound> bounds, String statName, String unit) {
        for (ObservedStatBound bound : bounds) {
            if (bound == null || bound.statName == null || !bound.statName.equals(statName)) {
                continue;
            }
            if (bound.unit == null || bound.unit.isEmpty() || bound.unit.equals(unit)) {
                return bound;
            }
        }
        return null;
    }

    private static int calculateRangePercentage(double value, StatRange range) {
        if (range.max <= range.min) {
            return value >= range.max ? 100 : 0;
        }
        int percentage = (int) Math.floor(((value - range.min) / (range.max - range.min)) * 100.0D);
        if (percentage < 0) {
            return 0;
        }
        if (percentage > 100) {
            return 100;
        }
        return percentage;
    }

    private static double estimateBaseStatValue(double value, StatBoost boost, int enhancementLevel, ItemEnhancementProfile enhancementProfile) {
        if (boost.fixedPerLevel) {
            return value - (enhancementLevel * boost.amount);
        }
        double percentPerLevel = boost.amount;
        if (enhancementProfile != null && enhancementProfile.percentBoostOverride > 0.0D) {
            percentPerLevel = enhancementProfile.percentBoostOverride;
        }
        double multiplier = Math.pow(1.0D + (percentPerLevel / 100.0D), enhancementLevel);
        return multiplier <= 0.0D ? value : value / multiplier;
    }

    private static boolean shouldSuppressRangeAnnotation(double value, StatRange range) {
        return range != null
            && Double.compare(range.min, range.max) == 0
            && Double.compare(value, range.min) == 0;
    }

    private static boolean shouldSuppressObservedBoundAnnotation(double value, ObservedStatBound bound) {
        return bound != null
            && bound.hasMin
            && bound.hasMax
            && Double.compare(bound.min, bound.max) == 0
            && Double.compare(value, bound.min) == 0;
    }

    private static String formatObservedAnnotation(ParsedCurrentStat parsed, ObservedStatBound observedBound, double trueValue) {
        if (observedBound.hasMin && observedBound.hasMax) {
            int percentage = calculateObservedBoundPercentage(trueValue, observedBound);
            return parsed.prefix
                + parsed.valueText
                + formatTrueValueSuffix(parsed.value, trueValue)
                + "("
                + observedBound.displayMin
                + OBSERVED_RANGE_SEPARATOR
                + observedBound.displayMax
                + " || ("
                + percentage
                + "%))"
                + parsed.unitText
                + parsed.suffix;
        }
        return parsed.prefix
            + parsed.valueText
            + formatTrueValueSuffix(parsed.value, trueValue)
            + "("
            + formatObservedBound(observedBound)
            + ")"
            + parsed.unitText
            + parsed.suffix;
    }

    private static int calculateObservedBoundPercentage(double value, ObservedStatBound bound) {
        if (bound == null || !bound.hasMin || !bound.hasMax) {
            return 0;
        }
        StatRange syntheticRange = new StatRange();
        syntheticRange.min = bound.min;
        syntheticRange.max = bound.max;
        return calculateRangePercentage(value, syntheticRange);
    }

    private static String formatTrueValueSuffix(double displayedValue, double trueValue) {
        return Double.compare(displayedValue, trueValue) == 0 ? "" : "|" + formatSignedValue(trueValue);
    }

    private static String formatSignedValue(double value) {
        double scaled = value * 1000.0D;
        double truncated = (value >= 0.0D ? Math.floor(scaled) : Math.ceil(scaled)) / 1000.0D;
        String formatted = NUMBER_FORMAT.format(truncated);
        return truncated >= 0.0D ? "+" + formatted : formatted;
    }

    private static int getEnhancementLevel(ItemStack stack, List<Text> tooltip) {
        int level = parseEnhancementLevel(stack == null ? "" : stack.getName().getString());
        if (level > 0 || tooltip == null) {
            return level;
        }
        for (Text line : tooltip) {
            level = parseEnhancementLevel(line == null ? "" : line.getString());
            if (level > 0) {
                return level;
            }
        }
        return 0;
    }

    private static int parseEnhancementLevel(String value) {
        Matcher matcher = ENHANCEMENT_SUFFIX.matcher(normalizeDisplay(value));
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static ItemEnhancementProfile getItemEnhancementProfile(List<Text> tooltip) {
        ItemEnhancementProfile profile = new ItemEnhancementProfile();
        if (tooltip == null) {
            return profile;
        }
        for (Text line : tooltip) {
            String normalized = normalizeDisplay(line == null ? "" : line.getString());
            if (normalized.isEmpty()) {
                continue;
            }
            if (normalized.contains(SUBWEAPON_TOOLTIP_LABEL) || normalized.contains(SOUL_PROTECTOR_TOOLTIP_LABEL)) {
                profile.percentBoostOverride = SPECIAL_SUBWEAPON_PERCENT_BOOST;
                break;
            }
        }
        return profile;
    }

    private static ItemStatAdjustments getItemStatAdjustments(List<Text> tooltip) {
        ItemStatAdjustments adjustments = new ItemStatAdjustments();
        if (tooltip == null) {
            return adjustments;
        }
        for (Text line : tooltip) {
            String normalized = normalizeDisplay(line == null ? "" : line.getString());
            if (normalized.isEmpty()) {
                continue;
            }
            Double bonus = getHpBoosterBonus(normalized);
            if (bonus != null && bonus.doubleValue() > adjustments.maxHpFlatBonus) {
                adjustments.maxHpFlatBonus = bonus.doubleValue();
            }
        }
        return adjustments;
    }

    private static Double getHpBoosterBonus(String normalizedLine) {
        if (normalizedLine == null || normalizedLine.isEmpty() || normalizedLine.indexOf("増強剤") < 0) {
            return null;
        }

        if (normalizedLine.contains("煌めく増強剤 極")) {
            return Double.valueOf(800.0D);
        }
        if (normalizedLine.contains("煌めく増強剤")) {
            return Double.valueOf(500.0D);
        }
        if (normalizedLine.contains("究極の増強剤<参段>")) {
            return Double.valueOf(130.0D);
        }
        if (normalizedLine.contains("究極の増強剤<弐段>")) {
            return Double.valueOf(100.0D);
        }
        if (normalizedLine.contains("究極の増強剤")) {
            return Double.valueOf(70.0D);
        }
        if (normalizedLine.contains("超越の増強剤")) {
            return Double.valueOf(40.0D);
        }
        if (normalizedLine.contains("伝説の増強剤")) {
            return Double.valueOf(25.0D);
        }
        if (normalizedLine.contains("最上級増強剤")) {
            return Double.valueOf(15.0D);
        }
        if (normalizedLine.contains("上級増強剤")) {
            return Double.valueOf(10.0D);
        }
        if (normalizedLine.contains("中級増強剤")) {
            return Double.valueOf(6.0D);
        }
        if (normalizedLine.contains("初級増強剤")) {
            return Double.valueOf(3.0D);
        }

        Matcher matcher = HP_BOOSTER_VALUE_PATTERN.matcher(normalizedLine);
        if (matcher.find()) {
            try {
                return Double.valueOf(Double.parseDouble(matcher.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }

        Double best = null;
        int bestLength = -1;
        for (Map.Entry<String, Double> entry : HP_BOOSTER_BONUSES.entrySet()) {
            if (normalizedLine.contains(entry.getKey()) && entry.getKey().length() > bestLength) {
                best = entry.getValue();
                bestLength = entry.getKey().length();
            }
        }
        return best;
    }

    private static double applyTrackedStatAdjustments(String statName, double value, ItemStatAdjustments adjustments) {
        if (adjustments == null) {
            return value;
        }
        if (MAX_HP_STAT_NAME.equals(statName) && adjustments.maxHpFlatBonus != 0.0D) {
            return value - adjustments.maxHpFlatBonus;
        }
        return value;
    }

    private static void applyStatRangeAdjustments(StatRange range, ItemStatAdjustments adjustments) {
        if (range == null || adjustments == null) {
            return;
        }
        if (MAX_HP_STAT_NAME.equals(range.statName) && adjustments.maxHpFlatBonus != 0.0D) {
            range.min -= adjustments.maxHpFlatBonus;
            range.max -= adjustments.maxHpFlatBonus;
            range.displayMin = formatRangeValue(range.min, true);
            range.displayMax = formatRangeValue(range.max, false);
        }
    }

    private static double applyStatAdjustments(String statName, double value, ItemStatAdjustments adjustments) {
        if (adjustments == null) {
            return value;
        }
        if ("譛螟ｧHP".equals(statName) && adjustments.maxHpFlatBonus != 0.0D) {
            return value - adjustments.maxHpFlatBonus;
        }
        return value;
    }

    private static boolean shouldMarkTooltip(ItemStack stack, List<Text> tooltip) {
        EvolutionForgeData data = getDataForCurrentServer();
        if (data.items.isEmpty()) {
            return false;
        }

        if (matchesAnyTarget(stack.getName().getString(), data.items)) {
            return true;
        }
        if (tooltip != null) {
            for (Text line : tooltip) {
                if (line != null && matchesAnyTarget(line.getString(), data.items)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void appendMarker(List<Text> tooltip) {
        if (tooltip == null || hasMarker(tooltip)) {
            return;
        }
        tooltip.add(new LiteralText(MARKER).formatted(Formatting.GREEN));
    }

    private static boolean isEvolutionForgeScreen(MinecraftClient client) {
        if (client == null || !(client.currentScreen instanceof GenericContainerScreen)) {
            return false;
        }
        String normalizedTitle = getNormalizedCurrentScreenTitle(client);
        return isEvolutionForgeTitle(normalizedTitle) || isRecipePreviewTitle(normalizedTitle);
    }

    private static boolean shouldRegisterConsumedItems(MinecraftClient client) {
        String normalizedTitle = getNormalizedCurrentScreenTitle(client);
        if (isEvolutionForgeTitle(normalizedTitle)) {
            return true;
        }
        return isRecipePreviewTitle(normalizedTitle) && recipePreviewCanRegisterMaterials;
    }

    private static boolean isEvolutionForgeTitle(String normalizedTitle) {
        return normalizedTitle.contains(FORGE_TITLE) || normalizedTitle.contains(ARMOR_FORGE_TITLE);
    }

    private static boolean isForgeRecipeSourceTitle(String normalizedTitle) {
        return isEvolutionForgeTitle(normalizedTitle) && !isRecipePreviewTitle(normalizedTitle);
    }

    private static boolean isRecipePreviewTitle(String normalizedTitle) {
        return normalizedTitle.contains(RECIPE_PREVIEW_TITLE);
    }

    private static String getNormalizedCurrentScreenTitle(MinecraftClient client) {
        if (client == null || client.currentScreen == null || client.currentScreen.getTitle() == null) {
            return "";
        }
        return normalizeDisplay(client.currentScreen.getTitle().getString());
    }

    private static void resetScreenTracking() {
        trackedScreenSyncId = -1;
        trackedScreenTitle = "";
        previousContainerTitle = "";
        recipePreviewCanRegisterMaterials = false;
    }

    private static boolean matchesAnyTarget(String rawValue, Set<String> targets) {
        String normalized = normalizeItemName(rawValue);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String target : targets) {
            if (target.length() >= 2 && (normalized.equals(target) || normalized.contains(target))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMarker(List<Text> tooltip) {
        for (Text line : tooltip) {
            if (line != null && MARKER.equals(Formatting.strip(line.getString()))) {
                return true;
            }
        }
        return false;
    }

    private static List<StatRange> getRangesForStack(ItemStack stack, List<Text> tooltip) {
        EvolutionForgeData data = getDataForCurrentServer();
        if (data.statRangesByItem.isEmpty()) {
            return new ArrayList<StatRange>();
        }

        String stackName = resolveTrackedItemName(stack.getName().getString(), tooltip);
        List<StatRange> ranges = findRangesForItemName(data, stackName);
        if (ranges != null) {
            return ranges;
        }

        if (tooltip != null) {
            for (Text line : tooltip) {
                if (line == null) {
                    continue;
                }
                ranges = findRangesForItemName(data, normalizeItemName(line.getString()));
                if (ranges != null) {
                    return ranges;
                }
            }
        }
        return new ArrayList<StatRange>();
    }

    private static List<ObservedStatBound> getObservedBoundsForStack(ItemStack stack, List<Text> tooltip) {
        EvolutionForgeData data = getDataForCurrentServer();
        if (data.observedBoundsByItem.isEmpty()) {
            return new ArrayList<ObservedStatBound>();
        }

        String stackName = resolveTrackedItemName(stack.getName().getString(), tooltip);
        List<ObservedStatBound> bounds = findObservedBoundsForItemName(data, stackName);
        if (bounds != null) {
            return bounds;
        }

        if (tooltip != null) {
            for (Text line : tooltip) {
                if (line == null) {
                    continue;
                }
                bounds = findObservedBoundsForItemName(data, normalizeItemName(line.getString()));
                if (bounds != null) {
                    return bounds;
                }
            }
        }
        return new ArrayList<ObservedStatBound>();
    }

    private static List<StatRange> findRangesForItemName(EvolutionForgeData data, String itemName) {
        if (data == null || itemName == null || itemName.isEmpty()) {
            return null;
        }

        List<StatRange> exact = data.statRangesByItem.get(itemName);
        if (exact != null) {
            return exact;
        }

        List<StatRange> bestRanges = null;
        int bestLength = 0;
        for (Map.Entry<String, List<StatRange>> entry : data.statRangesByItem.entrySet()) {
            String recipeName = entry.getKey();
            if (recipeName == null || recipeName.length() < 2 || recipeName.length() <= bestLength) {
                continue;
            }
            if (itemName.length() > recipeName.length() && itemName.endsWith(recipeName)) {
                bestLength = recipeName.length();
                bestRanges = entry.getValue();
            }
        }
        return bestRanges;
    }

    private static List<ObservedStatBound> findObservedBoundsForItemName(EvolutionForgeData data, String itemName) {
        if (data == null || itemName == null || itemName.isEmpty()) {
            return null;
        }

        List<ObservedStatBound> exact = data.observedBoundsByItem.get(itemName);
        if (exact != null) {
            return exact;
        }

        List<ObservedStatBound> bestBounds = null;
        int bestLength = 0;
        for (Map.Entry<String, List<ObservedStatBound>> entry : data.observedBoundsByItem.entrySet()) {
            String knownItemName = entry.getKey();
            if (knownItemName == null || knownItemName.length() < 2 || knownItemName.length() <= bestLength) {
                continue;
            }
            if (itemName.length() > knownItemName.length() && itemName.endsWith(knownItemName)) {
                bestLength = knownItemName.length();
                bestBounds = entry.getValue();
            }
        }
        return bestBounds;
    }

    private static EvolutionForgeData getDataForCurrentServer() {
        load();
        EvolutionForgeData data = DATA_BY_SERVER.get(getServerKey(MinecraftClient.getInstance()));
        return data == null ? new EvolutionForgeData() : data;
    }

    private static EvolutionForgeData getOrCreateData(String serverKey) {
        EvolutionForgeData data = DATA_BY_SERVER.get(serverKey);
        if (data == null) {
            data = new EvolutionForgeData();
            DATA_BY_SERVER.put(serverKey, data);
        }
        return data;
    }

    private static int getStatRangeCount(EvolutionForgeData data) {
        int count = 0;
        for (List<StatRange> ranges : data.statRangesByItem.values()) {
            count += ranges.size();
        }
        return count;
    }

    private static int getObservedBoundCount(EvolutionForgeData data) {
        int count = 0;
        for (List<ObservedStatBound> bounds : data.observedBoundsByItem.values()) {
            count += bounds.size();
        }
        return count;
    }

    private static String createSignature(GenericContainerScreenHandler handler) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < handler.slots.size(); i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (!stack.isEmpty()) {
                result.append(i).append(':').append(stack.getItem()).append(':').append(stack.getCount()).append(':').append(stack.getName().getString()).append(';');
            }
        }
        return result.toString();
    }

    private static String getServerKey(MinecraftClient client) {
        if (client == null) {
            return "unknown";
        }
        ServerInfo server = client.getCurrentServerEntry();
        if (server != null && server.address != null && !server.address.trim().isEmpty()) {
            return server.address.trim().toLowerCase(Locale.ROOT);
        }
        if (client.getServer() != null && client.getServer().getSaveProperties() != null) {
            return "singleplayer:" + client.getServer().getSaveProperties().getLevelName();
        }
        return "unknown";
    }

    private static String normalizeItemName(String value) {
        loadPrefixTokens();
        String result = normalizeDisplay(value);
        if (shouldSkipTrackingName(result)) {
            return "";
        }
        result = ITEM_KEY_ENHANCEMENT_SUFFIX.matcher(result).replaceFirst("");
        result = LEADING_MARKERS.matcher(result).replaceFirst("");
        result = toAsciiDigits(result);
        result = LEADING_COUNT.matcher(result).replaceFirst("");
        result = stripItemNameExceptionPrefixes(result);
        result = stripAllowedPrefixTokens(result);
        result = result.trim();
        return result;
    }

    private static String stripItemNameExceptionPrefixes(String value) {
        String result = value == null ? "" : value.trim();
        boolean changed;
        do {
            changed = false;
            for (String prefix : ITEM_NAME_EXCEPTION_PREFIXES) {
                if (prefix != null && !prefix.isEmpty() && result.startsWith(prefix)) {
                    result = result.substring(prefix.length()).trim();
                    changed = true;
                }
            }
        } while (changed);
        return result;
    }

    private static String resolveTrackedItemName(String fallbackName, List<Text> tooltip) {
        String derived = findRankColoredItemName(tooltip);
        if (!derived.isEmpty()) {
            return normalizeItemName(derived);
        }
        return normalizeItemName(fallbackName);
    }

    private static boolean shouldSkipTrackingName(String value) {
        String normalized = normalizeDisplay(value);
        return !normalized.isEmpty() && normalized.contains(NO_TRACK_ITEM_LABEL);
    }

    private static String stripAllowedPrefixTokens(String value) {
        String result = value == null ? "" : value.trim();
        if (result.isEmpty() || ALLOWED_PREFIX_TOKENS.isEmpty()) {
            return result;
        }

        List<String> tokens = new ArrayList<String>(Arrays.asList(result.split("\\s+")));
        while (!tokens.isEmpty()) {
            String first = normalizeDisplay(tokens.get(0));
            if (first.isEmpty() || !ALLOWED_PREFIX_TOKENS.contains(first)) {
                break;
            }
            tokens.remove(0);
        }
        if (tokens.isEmpty()) {
            return "";
        }
        StringBuilder rebuilt = new StringBuilder();
        for (String token : tokens) {
            if (token == null || token.isEmpty()) {
                continue;
            }
            if (rebuilt.length() > 0) {
                rebuilt.append(' ');
            }
            rebuilt.append(token);
        }
        return rebuilt.toString().trim();
    }

    private static String findRankColoredItemName(List<Text> tooltip) {
        if (tooltip == null || tooltip.isEmpty()) {
            return "";
        }

        Integer rankColor = findItemRankColor(tooltip);
        if (rankColor == null) {
            return "";
        }

        Text nameLine = tooltip.get(0);
        if (nameLine == null) {
            return "";
        }

        List<StyledSegment> segments = collectStyledSegments(nameLine);
        String bestMatch = "";
        for (StyledSegment segment : segments) {
            if (segment == null || segment.text == null) {
                continue;
            }
            String candidate = normalizeDisplay(segment.text);
            if (candidate.isEmpty() || !Objects.equals(segment.colorRgb, rankColor)) {
                continue;
            }
            candidate = ITEM_KEY_ENHANCEMENT_SUFFIX.matcher(candidate).replaceFirst("").trim();
            if (candidate.length() > bestMatch.length()) {
                bestMatch = candidate;
            }
        }
        return bestMatch;
    }

    private static Integer findItemRankColor(List<Text> tooltip) {
        if (tooltip == null) {
            return null;
        }
        for (Text line : tooltip) {
            if (line == null) {
                continue;
            }
            Integer color = findItemRankColor(line);
            if (color != null) {
                return color;
            }
        }
        return null;
    }

    private static Integer findItemRankColor(Text line) {
        String plain = normalizeDisplay(line == null ? "" : line.getString());
        if (plain.isEmpty() || plain.indexOf(ITEM_RANK_LABEL) < 0) {
            return null;
        }

        List<StyledSegment> segments = collectStyledSegments(line);
        int consumed = 0;
        boolean afterDelimiter = false;
        for (StyledSegment segment : segments) {
            if (segment == null || segment.text == null || segment.text.isEmpty()) {
                continue;
            }
            String raw = segment.text;
            for (int i = 0; i < raw.length(); i++) {
                char ch = raw.charAt(i);
                if (!afterDelimiter) {
                    if (ch == ':' || ch == '\uff1a') {
                        afterDelimiter = true;
                    }
                    consumed++;
                    continue;
                }
                if (!Character.isWhitespace(ch)) {
                    return segment.colorRgb;
                }
                consumed++;
            }
        }
        return null;
    }

    private static List<StyledSegment> collectStyledSegments(Text text) {
        List<StyledSegment> segments = new ArrayList<StyledSegment>();
        if (text == null) {
            return segments;
        }
        text.visit((Style style, String part) -> {
            if (part != null && !part.isEmpty()) {
                StyledSegment segment = new StyledSegment();
                segment.text = part;
                segment.colorRgb = getStyleColorRgb(style);
                segments.add(segment);
            }
            return Optional.empty();
        }, text.getStyle());
        return segments;
    }

    private static Integer getStyleColorRgb(Style style) {
        if (style == null) {
            return null;
        }
        TextColor color = style.getColor();
        return color == null ? null : color.getRgb();
    }

    private static String normalizeStatName(String value) {
        String result = normalizeDisplay(value);
        result = toAsciiDigits(result);
        while (!result.isEmpty() && !Character.isLetterOrDigit(result.charAt(0))) {
            result = result.substring(1).trim();
        }
        while (!result.isEmpty()) {
            char last = result.charAt(result.length() - 1);
            if (last != ':' && last != '\uff1a') {
                break;
            }
            result = result.substring(0, result.length() - 1).trim();
        }
        return isMeaningfulStatName(result) ? result : "";
    }

    private static String extractStatName(String prefix, String suffix) {
        String fromSuffix = normalizeStatName(suffix);
        if (!fromSuffix.isEmpty()) {
            return fromSuffix;
        }
        return normalizeStatName(prefix);
    }

    private static ParsedCurrentStat parseCurrentStat(String rawLine) {
        String line = normalizeDisplay(rawLine);
        if (line.isEmpty() || line.indexOf("||") >= 0 || line.indexOf("|") >= 0 || parseRangeLine(line) != null) {
            return null;
        }

        Matcher matcher = CURRENT_VALUE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }

        String prefix = matcher.group(1);
        String valueText = matcher.group(2);
        String unitText = matcher.group(3);
        String suffix = matcher.group(4);
        String suffixStatName = normalizeStatName(suffix);
        boolean suffixOnlyStatLine = normalizeStatName(prefix).isEmpty() && !suffixStatName.isEmpty();
        if (!containsStatDelimiter(prefix) && !containsStatDelimiter(suffix) && !suffixOnlyStatLine) {
            return null;
        }

        String statName = !suffixStatName.isEmpty() ? suffixStatName : extractStatName(prefix, suffix);
        if (statName.isEmpty() || isIgnoredStatName(statName)) {
            return null;
        }

        try {
            ParsedCurrentStat parsed = new ParsedCurrentStat();
            parsed.prefix = prefix;
            parsed.valueText = valueText;
            parsed.unitText = unitText;
            parsed.suffix = suffix;
            parsed.unit = normalizeUnit(unitText);
            parsed.statName = statName;
            parsed.value = Double.parseDouble(valueText);
            return parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean containsStatDelimiter(String value) {
        return value != null && (value.indexOf(':') >= 0 || value.indexOf('\uff1a') >= 0);
    }

    private static boolean isMeaningfulStatName(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        boolean hasLetter = false;
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetter(value.charAt(i))) {
                hasLetter = true;
                break;
            }
        }
        return hasLetter;
    }

        private static boolean isIgnoredStatName(String statName) {
        return !TRACKED_STAT_NAMES.contains(statName);
    }

    private static String normalizeUnit(String unit) {
        if ("\uff05".equals(unit)) {
            return "%";
        }
        return unit == null ? "" : unit;
    }

    private static boolean matchesObservedBound(ObservedStatBound left, ObservedStatBound right) {
        if (left == null || right == null || !Objects.equals(left.statName, right.statName)) {
            return false;
        }
        String leftUnit = left.unit == null ? "" : left.unit;
        String rightUnit = right.unit == null ? "" : right.unit;
        return leftUnit.isEmpty() || rightUnit.isEmpty() || leftUnit.equals(rightUnit);
    }

    private static String formatObservedBound(ObservedStatBound bound) {
        if (bound == null) {
            return "";
        }
        if (bound.hasMin && bound.hasMax) {
            if (Double.compare(bound.min, bound.max) == 0) {
                return bound.displayMin;
            }
            return bound.displayMin + "~" + bound.displayMax;
        }
        if (bound.hasMin) {
            return ">=" + bound.displayMin;
        }
        if (bound.hasMax) {
            return "<=" + bound.displayMax;
        }
        return "";
    }

    private static Map<String, StatBoost> createStatBoosts() {
        Map<String, StatBoost> boosts = new LinkedHashMap<String, StatBoost>();
        putPercentBoost(boosts, "\u653b\u6483\u529b", 3.5D);
        putPercentBoost(boosts, "\u6700\u5927HP", 5.0D);
        putPercentBoost(boosts, "\u6700\u5927MANA", 5.0D);
        putPercentBoost(boosts, "HP\u81ea\u7136\u56de\u5fa9", 5.0D);
        putPercentBoost(boosts, "MANA\u81ea\u7136\u56de\u5fa9", 5.0D);
        putPercentBoost(boosts, "\u88ab\u30c0\u30e1\u30fc\u30b8\u8efd\u6e1b", 2.0D);
        putPercentBoost(boosts, "\u88ab\u7269\u7406\u30c0\u30e1\u30fc\u30b8\u8efd\u6e1b", 2.0D);
        putPercentBoost(boosts, "\u88ab\u767a\u5c04\u4f53\u30c0\u30e1\u30fc\u30b8\u8efd\u6e1b", 2.0D);
        putPercentBoost(boosts, "\u88ab\u9b54\u6cd5\u30b9\u30ad\u30eb\u30c0\u30e1\u30fc\u30b8\u8efd\u6e1b", 2.0D);
        putPercentBoost(boosts, "\u88ab\u843d\u4e0b\u30c0\u30e1\u30fc\u30b8\u8efd\u6e1b", 2.0D);
        putPercentBoost(boosts, "\u88ab\u71c3\u713c\u30c0\u30e1\u30fc\u30b8\u8efd\u6e1b", 2.0D);
        putPercentBoost(boosts, "\u7d76\u5bfe\u9632\u5fa1", 5.0D);
        putPercentBoost(boosts, "\u6b66\u5668\u30c0\u30e1\u30fc\u30b8", 5.0D);
        putPercentBoost(boosts, "\u9b54\u6cd5\u30c0\u30e1\u30fc\u30b8", 5.0D);
        putPercentBoost(boosts, "\u7269\u7406\u30c0\u30e1\u30fc\u30b8", 5.0D);
        putPercentBoost(boosts, "\u30b9\u30ad\u30eb\u30c0\u30e1\u30fc\u30b8", 5.0D);
        putPercentBoost(boosts, "\u767a\u5c04\u4f53\u30c0\u30e1\u30fc\u30b8", 5.0D);
        putPercentBoost(boosts, "\u4e0d\u6b7b\u65cf\u7279\u653b", 5.0D);
        putPercentBoost(boosts, "\u30af\u30ea\u30c6\u30a3\u30ab\u30eb\u7387", 2.0D);
        putPercentBoost(boosts, "\u30b9\u30ad\u30eb\u30af\u30ea\u30c6\u30a3\u30ab\u30eb\u7387", 2.0D);
        putPercentBoost(boosts, "\u30d6\u30ed\u30c3\u30af\u7387", 2.5D);
        putPercentBoost(boosts, "\u30d6\u30ed\u30c3\u30af\u6642\u8efd\u6e1b", 2.5D);
        putPercentBoost(boosts, "\u30d1\u30ea\u30fc\u7387", 2.5D);
        putPercentBoost(boosts, "\u30c0\u30e1\u30fc\u30b8\u7121\u52b9\u5316\u7387", 2.5D);
        putPercentBoost(boosts, "\u30b9\u30ad\u30eb\u30af\u30fc\u30eb\u30c0\u30a6\u30f3\u77ed\u7e2e", 2.5D);
        putPercentBoost(boosts, "\u30d6\u30ed\u30c3\u30af\u30af\u30fc\u30eb\u30c0\u30a6\u30f3\u77ed\u7e2e", 3.0D);
        putPercentBoost(boosts, "\u30d1\u30ea\u30fc\u30af\u30fc\u30eb\u30c0\u30a6\u30f3\u77ed\u7e2e", 3.0D);
        putPercentBoost(boosts, "\u30c0\u30e1\u30fc\u30b8\u7121\u52b9\u5316\u30af\u30fc\u30eb\u30c0\u30a6\u30f3\u77ed\u7e2e", 3.0D);
        boosts.put(normalizeStatName("\u5bfeMOB\u30c0\u30e1\u30fc\u30b8"), new StatBoost(50.0D, true));
        return boosts;
    }

    private static Map<String, Double> createHpBoosterBonuses() {
        Map<String, Double> bonuses = new LinkedHashMap<String, Double>();
        bonuses.put("蛻晉ｴ壼｢怜ｼｷ蜑､", 3.0D);
        bonuses.put("荳ｭ邏壼｢怜ｼｷ蜑､", 6.0D);
        bonuses.put("荳顔ｴ壼｢怜ｼｷ蜑､", 10.0D);
        bonuses.put("譛荳顔ｴ壼｢怜ｼｷ蜑､", 15.0D);
        bonuses.put("莨晁ｪｬ縺ｮ蠅怜ｼｷ蜑､", 25.0D);
        bonuses.put("雜・ｶ翫・蠅怜ｼｷ蜑､", 40.0D);
        bonuses.put("遨ｶ讌ｵ縺ｮ蠅怜ｼｷ蜑､<蠑先ｮｵ>", 100.0D);
        bonuses.put("遨ｶ讌ｵ縺ｮ蠅怜ｼｷ蜑､<蜿よｮｵ>", 130.0D);
        bonuses.put("遨ｶ讌ｵ縺ｮ蠅怜ｼｷ蜑､", 70.0D);
        bonuses.put("辣後ａ縺丞｢怜ｼｷ蜑､ 讌ｵ", 800.0D);
        bonuses.put("辣後ａ縺丞｢怜ｼｷ蜑､", 500.0D);
        return bonuses;
    }

    private static void putPercentBoost(Map<String, StatBoost> boosts, String statName, double amount) {
        boosts.put(normalizeStatName(statName), new StatBoost(amount, false));
    }

    private static String normalizeDisplay(String value) {
        if (value == null) {
            return "";
        }
        String stripped = Formatting.strip(value);
        if (stripped == null) {
            stripped = value;
        }
        return stripped.replace('\u3000', ' ').trim();
    }

    private static String toAsciiDigits(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '\uff10' && ch <= '\uff19') {
                result.append((char) ('0' + (ch - '\uff10')));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private static String normalizeRangeDisplay(String value, boolean forcePlus) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (forcePlus && value.charAt(0) != '+' && value.charAt(0) != '-') {
            return "+" + value;
        }
        return value;
    }

    private static String formatRangeValue(double value, boolean forcePlus) {
        String formatted = NUMBER_FORMAT.format(value);
        if (forcePlus && value >= 0.0D) {
            return "+" + formatted;
        }
        return formatted;
    }

    private static List<String> normalizeStringList(List<String> values) {
        List<String> result = new ArrayList<String>();
        if (values == null) {
            return result;
        }
        Set<String> seen = new LinkedHashSet<String>();
        for (String value : values) {
            String normalized = normalizeDisplay(value);
            if (!normalized.isEmpty() && seen.add(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private static List<Integer> normalizeIntegerList(List<Integer> values) {
        List<Integer> result = new ArrayList<Integer>();
        if (values == null) {
            return result;
        }
        Set<Integer> seen = new LinkedHashSet<Integer>();
        for (Integer value : values) {
            if (value != null && value.intValue() >= 0 && seen.add(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        loadPrefixTokens();
        if (!Files.exists(STORAGE_FILE)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(STORAGE_FILE, StandardCharsets.UTF_8)) {
            SavedEvolutionForgeItems saved = GSON.fromJson(reader, SavedEvolutionForgeItems.class);
            if (saved != null && saved.servers != null) {
                DATA_BY_SERVER.clear();
                for (ServerItems server : saved.servers) {
                    if (server == null || server.serverKey == null || server.serverKey.trim().isEmpty()) {
                        continue;
                    }
                    EvolutionForgeData data = new EvolutionForgeData();
                    if (server.items != null) {
                        for (String item : server.items) {
                            String normalized = normalizeItemName(item);
                            if (!normalized.isEmpty()) {
                                data.items.add(normalized);
                            }
                        }
                    }
                    if (server.statRanges != null) {
                        for (ItemStatRanges itemRanges : server.statRanges) {
                            if (itemRanges == null || itemRanges.itemName == null || itemRanges.ranges == null) {
                                continue;
                            }
                            String normalizedItemName = normalizeItemName(itemRanges.itemName);
                            if (normalizedItemName.isEmpty()) {
                                continue;
                            }
                            List<StatRange> ranges = new ArrayList<StatRange>();
                            for (StatRange range : itemRanges.ranges) {
                                if (range != null) {
                                    range.normalize();
                                }
                                if (range != null && range.isValid()) {
                                    putRange(ranges, range);
                                }
                            }
                            if (!ranges.isEmpty()) {
                                data.statRangesByItem.put(normalizedItemName, ranges);
                            }
                        }
                    }
                    if (server.observedBounds != null) {
                        for (ItemObservedBounds itemBounds : server.observedBounds) {
                            if (itemBounds == null || itemBounds.itemName == null || itemBounds.bounds == null) {
                                continue;
                            }
                            String normalizedItemName = normalizeItemName(itemBounds.itemName);
                            if (normalizedItemName.isEmpty()) {
                                continue;
                            }
                            List<ObservedStatBound> bounds = new ArrayList<ObservedStatBound>();
                            for (ObservedStatBound bound : itemBounds.bounds) {
                                if (bound != null) {
                                    bound.normalize();
                                }
                                if (bound != null && bound.isValid()) {
                                    putObservedBound(bounds, bound);
                                }
                            }
                            if (!bounds.isEmpty()) {
                                data.observedBoundsByItem.put(normalizedItemName, bounds);
                            }
                        }
                    }
                    if (!data.items.isEmpty() || !data.statRangesByItem.isEmpty() || !data.observedBoundsByItem.isEmpty()) {
                        DATA_BY_SERVER.put(server.serverKey, data);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void save() {
        try {
            Files.createDirectories(STORAGE_FILE.getParent());
            savePrefixTokens();
            SavedEvolutionForgeItems saved = new SavedEvolutionForgeItems();
            for (Map.Entry<String, EvolutionForgeData> entry : DATA_BY_SERVER.entrySet()) {
                EvolutionForgeData data = entry.getValue();
                if (data.items.isEmpty() && data.statRangesByItem.isEmpty() && data.observedBoundsByItem.isEmpty()) {
                    continue;
                }
                ServerItems server = new ServerItems();
                server.serverKey = entry.getKey();
                server.items = new ArrayList<String>(data.items);
                for (Map.Entry<String, List<StatRange>> rangeEntry : data.statRangesByItem.entrySet()) {
                    ItemStatRanges itemRanges = new ItemStatRanges();
                    itemRanges.itemName = rangeEntry.getKey();
                    itemRanges.ranges = new ArrayList<StatRange>(rangeEntry.getValue());
                    server.statRanges.add(itemRanges);
                }
                for (Map.Entry<String, List<ObservedStatBound>> observedEntry : data.observedBoundsByItem.entrySet()) {
                    ItemObservedBounds itemBounds = new ItemObservedBounds();
                    itemBounds.itemName = observedEntry.getKey();
                    itemBounds.bounds = new ArrayList<ObservedStatBound>(observedEntry.getValue());
                    server.observedBounds.add(itemBounds);
                }
                saved.servers.add(server);
            }
            try (Writer writer = Files.newBufferedWriter(STORAGE_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(saved, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static void loadPrefixTokens() {
        if (prefixTokensLoaded) {
            return;
        }
        prefixTokensLoaded = true;
        ALLOWED_PREFIX_TOKENS.clear();
        PREFIX_TOKEN_CANDIDATES.clear();

        if (Files.exists(ALLOWED_PREFIX_TOKENS_FILE)) {
            try (Reader reader = Files.newBufferedReader(ALLOWED_PREFIX_TOKENS_FILE, StandardCharsets.UTF_8)) {
                AllowedPrefixTokensFile allowed = GSON.fromJson(reader, AllowedPrefixTokensFile.class);
                if (allowed != null && allowed.allowedPrefixTokens != null) {
                    for (String token : allowed.allowedPrefixTokens) {
                        String normalized = normalizeDisplay(token);
                        if (!normalized.isEmpty() && !DISABLED_PREFIX_TOKENS.contains(normalized)) {
                            ALLOWED_PREFIX_TOKENS.add(normalized);
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }

        if (Files.exists(PREFIX_TOKEN_CANDIDATES_FILE)) {
            try (Reader reader = Files.newBufferedReader(PREFIX_TOKEN_CANDIDATES_FILE, StandardCharsets.UTF_8)) {
                PrefixTokenCandidatesFile saved = GSON.fromJson(reader, PrefixTokenCandidatesFile.class);
                if (saved != null && saved.candidates != null) {
                    for (PrefixTokenCandidate candidate : saved.candidates) {
                        if (candidate == null) {
                            continue;
                        }
                        candidate.normalize();
                        if (!candidate.isValid()) {
                            continue;
                        }
                        PREFIX_TOKEN_CANDIDATES.put(candidate.token, candidate);
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    private static void savePrefixTokens() throws IOException {
        Files.createDirectories(PREFIX_TOKEN_CANDIDATES_FILE.getParent());
        PrefixTokenCandidatesFile saved = new PrefixTokenCandidatesFile();
        for (PrefixTokenCandidate candidate : PREFIX_TOKEN_CANDIDATES.values()) {
            if (candidate == null || !candidate.isValid()) {
                continue;
            }
            saved.candidates.add(candidate.copy());
        }
        try (Writer writer = Files.newBufferedWriter(PREFIX_TOKEN_CANDIDATES_FILE, StandardCharsets.UTF_8)) {
            GSON.toJson(saved, writer);
        }
    }

    private static final class EvolutionForgeData {
        final Set<String> items = new LinkedHashSet<String>();
        final Map<String, List<StatRange>> statRangesByItem = new LinkedHashMap<String, List<StatRange>>();
        final Map<String, List<ObservedStatBound>> observedBoundsByItem = new LinkedHashMap<String, List<ObservedStatBound>>();
    }

    private static final class SavedEvolutionForgeItems {
        List<ServerItems> servers = new ArrayList<ServerItems>();
    }

    private static final class PrefixTokenCandidatesFile {
        List<PrefixTokenCandidate> candidates = new ArrayList<PrefixTokenCandidate>();
    }

    private static final class AllowedPrefixTokensFile {
        List<String> allowedPrefixTokens = new ArrayList<String>();
    }

    private static final class PrefixTokenCandidate {
        String token = "";
        int count;
        List<String> examples = new ArrayList<String>();
        List<String> servers = new ArrayList<String>();
        List<Integer> positions = new ArrayList<Integer>();

        void normalize() {
            token = normalizeDisplay(token);
            examples = normalizeStringList(examples);
            servers = normalizeStringList(servers);
            positions = normalizeIntegerList(positions);
            if (count < 0) {
                count = 0;
            }
        }

        boolean isValid() {
            return token != null && !token.isEmpty();
        }

        PrefixTokenCandidate copy() {
            PrefixTokenCandidate copy = new PrefixTokenCandidate();
            copy.token = token;
            copy.count = count;
            copy.examples = new ArrayList<String>(examples);
            copy.servers = new ArrayList<String>(servers);
            copy.positions = new ArrayList<Integer>(positions);
            return copy;
        }
    }

    private static final class ServerItems {
        String serverKey = "";
        List<String> items = new ArrayList<String>();
        List<ItemStatRanges> statRanges = new ArrayList<ItemStatRanges>();
        List<ItemObservedBounds> observedBounds = new ArrayList<ItemObservedBounds>();
    }

    private static final class ItemStatRanges {
        String itemName = "";
        List<StatRange> ranges = new ArrayList<StatRange>();
    }

    private static final class ItemObservedBounds {
        String itemName = "";
        List<ObservedStatBound> bounds = new ArrayList<ObservedStatBound>();
    }

    private static final class StatRange {
        String statName = "";
        double min;
        double max;
        String unit = "";
        String separator = "~";
        String displayMin = "";
        String displayMax = "";

        boolean isValid() {
            return statName != null && !statName.trim().isEmpty() && max >= min;
        }

        void normalize() {
            statName = normalizeStatName(statName);
            if (unit == null) {
                unit = "";
            } else {
                unit = normalizeUnit(unit);
            }
            if (separator == null || separator.trim().isEmpty()) {
                separator = "~";
            } else {
                separator = separator.trim();
            }
            if (max < min) {
                double oldMin = min;
                min = max;
                max = oldMin;
            }
            if (displayMin == null || displayMin.isEmpty()) {
                displayMin = formatRangeValue(min, true);
            }
            if (displayMax == null || displayMax.isEmpty()) {
                displayMax = formatRangeValue(max, false);
            }
        }
    }

    private static final class ObservedStatBound {
        String statName = "";
        double min;
        double max;
        boolean hasMin;
        boolean hasMax;
        String unit = "";
        String displayMin = "";
        String displayMax = "";

        boolean isValid() {
            return statName != null && !statName.trim().isEmpty() && (hasMin || hasMax);
        }

        void normalize() {
            statName = normalizeStatName(statName);
            if (unit == null) {
                unit = "";
            } else {
                unit = normalizeUnit(unit);
            }
            if (hasMin && hasMax && max < min) {
                double oldMin = min;
                min = max;
                max = oldMin;
                String oldDisplayMin = displayMin;
                displayMin = displayMax;
                displayMax = oldDisplayMin;
            }
            if (hasMin && (displayMin == null || displayMin.isEmpty())) {
                displayMin = formatRangeValue(min, true);
            }
            if (hasMax && (displayMax == null || displayMax.isEmpty())) {
                displayMax = formatRangeValue(max, true);
            }
        }
    }

    private static final class ParsedCurrentStat {
        String prefix = "";
        String valueText = "";
        String unitText = "";
        String suffix = "";
        String statName = "";
        String unit = "";
        double value;
    }

    private static final class StyledSegment {
        String text = "";
        Integer colorRgb;
    }

    private static final class ItemStatAdjustments {
        double maxHpFlatBonus;
    }

    private static final class ItemEnhancementProfile {
        double percentBoostOverride;
    }

    private static final class StatBoost {
        final double amount;
        final boolean fixedPerLevel;

        StatBoost(double amount, boolean fixedPerLevel) {
            this.amount = amount;
            this.fixedPerLevel = fixedPerLevel;
        }
    }
}



