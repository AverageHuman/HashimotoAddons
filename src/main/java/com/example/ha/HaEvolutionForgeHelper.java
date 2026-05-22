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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaEvolutionForgeHelper {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("0.########", DecimalFormatSymbols.getInstance(Locale.US));
    private static final Path STORAGE_FILE = FabricLoader.getInstance().getConfigDir().resolve("HashimotoAddons").resolve("evolution_forge_items.json");
    private static final String FORGE_TITLE = "\u30a8\u30dc\u30ea\u30e5\u30fc\u30b7\u30e7\u30f3\u30d5\u30a9\u30fc\u30b8";
    private static final String ARMOR_FORGE_TITLE = "\u30a8\u30dc\u30ea\u30e5\u30fc\u30b7\u30e7\u30f3\u30a2\u30fc\u30de\u30fc\u30d5\u30a9\u30fc\u30b8";
    private static final String RECIPE_PREVIEW_TITLE = "\u30ec\u30b7\u30d4\u30d7\u30ec\u30d3\u30e5\u30fc";
    private static final String CONSUME_HEADER = "\u88fd\u4f5c\u6642\u6d88\u8cbb\u30a2\u30a4\u30c6\u30e0";
    private static final String LEFT_CLICK = "\u5de6\u30af\u30ea\u30c3\u30af";
    private static final String RIGHT_CLICK = "\u53f3\u30af\u30ea\u30c3\u30af";
    private static final String MARKER = "Evo?: Yes";
    private static final Pattern LEADING_MARKERS = Pattern.compile("^[\\s\\u2715\\u2716\\u00d7xX*\\-:\\uFF1A\\u30FB]+");
    private static final Pattern LEADING_COUNT = Pattern.compile("^[0-9]+\\s+");
    private static final Pattern RANGE_LINE_PATTERN = Pattern.compile("^(.*?)([+\\-]?[0-9]+(?:\\.[0-9]+)?)(\\s*[~\\u2393\\uFF5E\\u301C\\-\\u2212\\u2013\\u2014]\\s*)([+\\-]?[0-9]+(?:\\.[0-9]+)?)(%?)(.*)$");
    private static final Pattern CURRENT_VALUE_PATTERN = Pattern.compile("^(.*?)([+\\-]?[0-9]+(?:\\.[0-9]+)?)(%?)(.*)$");
    private static final Map<String, EvolutionForgeData> DATA_BY_SERVER = new LinkedHashMap<String, EvolutionForgeData>();
    private static boolean loaded;
    private static boolean scanningForgeTooltips;
    private static int lastSyncId = -1;
    private static String lastSignature = "";

    private HaEvolutionForgeHelper() {
    }

    public static void tick(MinecraftClient client) {
        if (!HaConfig.get().evolutionForgeHelperEnabled || !isEvolutionForgeScreen(client)) {
            lastSyncId = -1;
            lastSignature = "";
            return;
        }

        GenericContainerScreen screen = (GenericContainerScreen) client.currentScreen;
        GenericContainerScreenHandler handler = (GenericContainerScreenHandler) screen.getScreenHandler();
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
        if (isEvolutionForgeScreen(client)) {
            learnDisplayedTooltip(client, stack, tooltip);
        }

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

    public static void clearCurrentServerItems() {
        load();
        DATA_BY_SERVER.remove(getServerKey(MinecraftClient.getInstance()));
        save();
    }

    private static void scanVisiblePage(MinecraftClient client, GenericContainerScreenHandler handler) {
        load();
        EvolutionForgeData data = getOrCreateData(getServerKey(client));
        int beforeItems = data.items.size();
        int beforeRanges = getStatRangeCount(data);

        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                continue;
            }
            List<Text> tooltip = getRawTooltip(client, stack);
            addConsumedItems(data.items, tooltip);
            addStatRanges(data, stack.getName().getString(), tooltip);
        }

        if (data.items.size() != beforeItems || getStatRangeCount(data) != beforeRanges) {
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

        addConsumedItems(data.items, tooltip);
        addStatRanges(data, stack.getName().getString(), tooltip);

        if (data.items.size() != beforeItems || getStatRangeCount(data) != beforeRanges) {
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
        String normalizedItemName = normalizeItemName(itemName);
        if (normalizedItemName.isEmpty()) {
            return;
        }

        List<StatRange> ranges = data.statRangesByItem.get(normalizedItemName);
        for (Text text : tooltip) {
            StatRange range = parseRangeLine(text == null ? "" : text.getString());
            if (range == null) {
                continue;
            }
            if (ranges == null) {
                ranges = new ArrayList<StatRange>();
                data.statRangesByItem.put(normalizedItemName, ranges);
            }
            putRange(ranges, range);
        }
    }

    private static void addCandidate(Set<String> items, String rawLine) {
        String normalized = normalizeItemName(rawLine);
        if (!normalized.isEmpty()) {
            items.add(normalized);
        }
    }

    private static List<Text> appendStatRangeAnnotations(ItemStack stack, List<Text> tooltip) {
        List<StatRange> ranges = getRangesForStack(stack, tooltip);
        if (ranges.isEmpty()) {
            return tooltip;
        }

        List<Text> updatedTooltip = null;
        for (int i = 0; i < tooltip.size(); i++) {
            Text text = tooltip.get(i);
            String originalLine = text == null ? "" : text.getString();
            String annotatedLine = annotateStatLine(originalLine, ranges);
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

    private static String annotateStatLine(String rawLine, List<StatRange> ranges) {
        String line = normalizeDisplay(rawLine);
        if (line.isEmpty() || line.indexOf("||") >= 0 || parseRangeLine(line) != null) {
            return null;
        }

        Matcher matcher = CURRENT_VALUE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }

        String prefix = matcher.group(1);
        String valueText = matcher.group(2);
        String unit = matcher.group(3);
        String suffix = matcher.group(4);
        String statName = extractStatName(prefix, suffix);
        if (statName.isEmpty()) {
            return null;
        }

        StatRange range = findRange(ranges, statName, unit);
        if (range == null) {
            return null;
        }

        double value;
        try {
            value = Double.parseDouble(valueText);
        } catch (NumberFormatException ignored) {
            return null;
        }

        int percentage = calculateRangePercentage(value, range);
        return prefix
            + valueText
            + "("
            + range.displayMin
            + range.separator
            + range.displayMax
            + " || ("
            + percentage
            + "%))"
            + unit
            + suffix;
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
        String unit = matcher.group(5);
        String statName = extractStatName(matcher.group(1), matcher.group(6));
        if (statName.isEmpty()) {
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
        String title = client.currentScreen.getTitle() == null ? "" : client.currentScreen.getTitle().getString();
        String normalizedTitle = normalizeDisplay(title);
        return normalizedTitle.contains(FORGE_TITLE) || normalizedTitle.contains(ARMOR_FORGE_TITLE) || normalizedTitle.contains(RECIPE_PREVIEW_TITLE);
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

        String stackName = normalizeItemName(stack.getName().getString());
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
        String result = normalizeDisplay(value);
        result = LEADING_MARKERS.matcher(result).replaceFirst("");
        result = toAsciiDigits(result);
        result = LEADING_COUNT.matcher(result).replaceFirst("");
        return result.trim();
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

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
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
                    if (!data.items.isEmpty() || !data.statRangesByItem.isEmpty()) {
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
            SavedEvolutionForgeItems saved = new SavedEvolutionForgeItems();
            for (Map.Entry<String, EvolutionForgeData> entry : DATA_BY_SERVER.entrySet()) {
                EvolutionForgeData data = entry.getValue();
                if (data.items.isEmpty() && data.statRangesByItem.isEmpty()) {
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
                saved.servers.add(server);
            }
            try (Writer writer = Files.newBufferedWriter(STORAGE_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(saved, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static final class EvolutionForgeData {
        final Set<String> items = new LinkedHashSet<String>();
        final Map<String, List<StatRange>> statRangesByItem = new LinkedHashMap<String, List<StatRange>>();
    }

    private static final class SavedEvolutionForgeItems {
        List<ServerItems> servers = new ArrayList<ServerItems>();
    }

    private static final class ServerItems {
        String serverKey = "";
        List<String> items = new ArrayList<String>();
        List<ItemStatRanges> statRanges = new ArrayList<ItemStatRanges>();
    }

    private static final class ItemStatRanges {
        String itemName = "";
        List<StatRange> ranges = new ArrayList<StatRange>();
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
}
