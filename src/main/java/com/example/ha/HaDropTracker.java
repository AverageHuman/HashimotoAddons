package com.example.ha;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public final class HaDropTracker {
    public static final String MODE_ALL = "all";
    public static final String MODE_BUILTIN_ONLY = "builtin_only";
    public static final String MODE_REGISTERED_ONLY = "registered_only";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STORAGE_FILE = FabricLoader.getInstance().getConfigDir().resolve("HashimotoAddons").resolve("drop_tracker.json");
    private static final String ITEM_KEY_SEPARATOR = "\u001f";
    private static final List<DropEntry> ENTRIES = new ArrayList<DropEntry>();
    private static final List<RegisteredItem> REGISTERED_ITEMS = new ArrayList<RegisteredItem>();
    private static boolean loaded;
    private static boolean activeSession;
    private static long cachedProfitPerHour;
    private static long lastRateUpdateMillis;
    private static long tickAccumulatorMillis;
    private static long lastTickMillis;

    private HaDropTracker() {
    }

    public static void onItemPickup(ItemPickupAnimationS2CPacket packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null || !isTrackingAllowed(HaConfig.get())) {
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

        ItemStack stack = ((ItemEntity) entity).getStack();
        if (stack.isEmpty() || shouldSkipTrackingForCurrentLocation(client) || !shouldTrack(stack)) {
            return;
        }

        int amount = Math.max(1, packet.getStackAmount());
        add(stack, amount);
        updateHourlyProfit(HaConfig.get());
    }

    public static void tick(MinecraftClient client) {
        HaConfig config = HaConfig.get();
        if (client == null || client.player == null || client.world == null || !isTrackingAllowed(config)) {
            stopSession();
            return;
        }

        startSessionIfNeeded(config);
        tickElapsedTime(config);
        updateHourlyProfit(config);
    }

    public static List<DropEntry> getEntries() {
        load();
        return ENTRIES;
    }

    public static List<RegisteredItem> getRegisteredItems() {
        load();
        return REGISTERED_ITEMS;
    }

    public static int getRegisteredItemCount() {
        load();
        return REGISTERED_ITEMS.size();
    }

    public static long getEstimatedProfit() {
        load();
        long total = 0L;
        for (DropEntry entry : ENTRIES) {
            total += getUnitValue(entry) * entry.count;
        }
        return total;
    }

    public static long getProfitPerHour() {
        return cachedProfitPerHour;
    }

    public static long getElapsedSeconds() {
        return HaConfig.get().dropTrackerElapsedSeconds;
    }

    public static boolean isActiveSession() {
        return activeSession;
    }

    public static boolean isTrackingAllowed(HaConfig config) {
        if (!config.dropTrackerEnabled) {
            return false;
        }
        return HaSoulbindProtection.isSoulbound()
            || (config.dropTrackerContinueAfterStart && (activeSession || config.dropTrackerElapsedSeconds > 0L));
    }

    public static void clear() {
        load();
        ENTRIES.clear();
        HaConfig config = HaConfig.get();
        config.dropTrackerElapsedSeconds = 0L;
        tickAccumulatorMillis = 0L;
        lastTickMillis = activeSession ? System.currentTimeMillis() : 0L;
        cachedProfitPerHour = 0L;
        lastRateUpdateMillis = 0L;
        config.save();
        save();
    }

    public static String normalizeMode(String mode) {
        if (MODE_BUILTIN_ONLY.equals(mode)) {
            return MODE_BUILTIN_ONLY;
        }
        if (MODE_REGISTERED_ONLY.equals(mode)) {
            return MODE_REGISTERED_ONLY;
        }
        return MODE_ALL;
    }

    public static String nextMode(String mode) {
        String normalized = normalizeMode(mode);
        if (MODE_ALL.equals(normalized)) {
            return MODE_BUILTIN_ONLY;
        }
        if (MODE_BUILTIN_ONLY.equals(normalized)) {
            return MODE_REGISTERED_ONLY;
        }
        return MODE_ALL;
    }

    public static String getModeLabel(String mode) {
        String normalized = normalizeMode(mode);
        if (MODE_BUILTIN_ONLY.equals(normalized)) {
            return "Built-in Currencies Only";
        }
        if (MODE_REGISTERED_ONLY.equals(normalized)) {
            return "Built-in + Registered Items";
        }
        return "Track Everything";
    }

    public static String getModeDescription(String mode) {
        String normalized = normalizeMode(mode);
        if (MODE_BUILTIN_ONLY.equals(normalized)) {
            return "\u9280\u8ca8\u30fb\u91d1\u8ca8\u306a\u3069\u306e\u5185\u8535\u901a\u8ca8\u3060\u3051\u3092\u8a18\u9332\u3057\u307e\u3059\u3002";
        }
        if (MODE_REGISTERED_ONLY.equals(normalized)) {
            return "\u5185\u8535\u901a\u8ca8\u3068\u767b\u9332\u6e08\u307f\u30a2\u30a4\u30c6\u30e0\u3060\u3051\u3092\u8a18\u9332\u3057\u307e\u3059\u3002";
        }
        return "\u62fe\u3063\u305f\u30a2\u30a4\u30c6\u30e0\u3092\u3059\u3079\u3066\u8a18\u9332\u3057\u307e\u3059\u3002";
    }

    public static RegisteredItem registerHeldItem(long unitPrice) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return null;
        }

        ItemStack stack = client.player.getMainHandStack();
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        load();
        String itemId = getItemKey(stack);
        RegisteredItem existing = findRegisteredItemExact(itemId);
        String displayName = normalizeDropName(stripFormatting(stack.getName()));
        if (existing != null) {
            existing.displayName = displayName;
            existing.unitPrice = Math.max(0L, unitPrice);
            save();
            return existing;
        }

        RegisteredItem created = new RegisteredItem(itemId, displayName, Math.max(0L, unitPrice));
        REGISTERED_ITEMS.add(created);
        save();
        return created;
    }

    public static void updateRegisteredItem(int index, String displayName, long unitPrice) {
        load();
        if (index < 0 || index >= REGISTERED_ITEMS.size()) {
            return;
        }
        RegisteredItem item = REGISTERED_ITEMS.get(index);
        item.displayName = normalizeDisplayName(displayName, item.itemId);
        item.unitPrice = Math.max(0L, unitPrice);
        save();
    }

    public static void removeRegisteredItem(int index) {
        load();
        if (index < 0 || index >= REGISTERED_ITEMS.size()) {
            return;
        }
        REGISTERED_ITEMS.remove(index);
        save();
    }

    private static void add(ItemStack stack, int amount) {
        load();
        String key = getItemKey(stack);
        String plainName = normalizeDropName(stripFormatting(stack.getName()));
        for (int i = 0; i < ENTRIES.size(); i++) {
            DropEntry entry = ENTRIES.get(i);
            if (entry.key.equals(key)) {
                entry.count += amount;
                entry.displayStack = stack.copy();
                entry.displayStack.setCount(1);
                entry.displayName = HaItemNameNormalizer.preserveStyle(stack.getName(), plainName);
                entry.plainName = plainName;
                ENTRIES.remove(i);
                ENTRIES.add(0, entry);
                save();
                return;
            }
        }

        ItemStack displayStack = stack.copy();
        displayStack.setCount(1);
        ENTRIES.add(0, new DropEntry(key, displayStack, HaItemNameNormalizer.preserveStyle(stack.getName(), plainName), plainName, amount));
        save();
    }

    private static boolean shouldTrack(ItemStack stack) {
        load();
        String mode = normalizeMode(HaConfig.get().dropTrackerMode);
        String key = getItemKey(stack);
        String plainName = normalizeDropName(stripFormatting(stack.getName()));
        if (MODE_BUILTIN_ONLY.equals(mode)) {
            return getBuiltinCoinValue(plainName) > 0L;
        }
        if (MODE_REGISTERED_ONLY.equals(mode)) {
            return getBuiltinCoinValue(plainName) > 0L || findRegisteredItem(key) != null;
        }
        return true;
    }

    private static boolean shouldSkipTrackingForCurrentLocation(MinecraftClient client) {
        if (client == null || client.world == null) {
            return false;
        }

        Scoreboard scoreboard = client.world.getScoreboard();
        if (scoreboard == null) {
            return false;
        }

        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(1);
        if (objective == null) {
            return false;
        }

        return hasGreenCurrentLocationLine(getSidebarLines(scoreboard, objective));
    }

    private static boolean hasGreenCurrentLocationLine(List<String> lines) {
        for (String line : lines) {
            if (line == null) {
                continue;
            }

            String normalized = normalizeScoreboardLine(line);
            if (normalized.contains("\u73fe\u5728\u5730")) {
                String trimmed = line.trim();
                if (trimmed.startsWith(Formatting.DARK_GREEN.toString()) || trimmed.startsWith("\u00a72")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static long getUnitValue(DropEntry entry) {
        RegisteredItem registered = findRegisteredItem(entry.key);
        if (registered != null) {
            return registered.unitPrice;
        }
        return getBuiltinCoinValue(entry.plainName);
    }

    private static long getBuiltinCoinValue(String plainName) {
        if ("\u9280\u8ca8".equals(plainName)) {
            return 1L;
        }
        if ("\u91d1\u8ca8".equals(plainName)) {
            return 100L;
        }
        if ("\u9280\u584a".equals(plainName)) {
            return 10000L;
        }
        if ("\u3068\u3053\u3057\u3048\u306e\u91d1\u584a".equals(plainName)) {
            return 100000L;
        }
        return 0L;
    }

    private static List<String> getSidebarLines(Scoreboard scoreboard, ScoreboardObjective objective) {
        Collection<ScoreboardPlayerScore> scores = scoreboard.getAllPlayerScores(objective);
        List<ScoreboardPlayerScore> visibleScores = new ArrayList<ScoreboardPlayerScore>();
        for (ScoreboardPlayerScore score : scores) {
            if (score != null && score.getPlayerName() != null && !score.getPlayerName().startsWith("#")) {
                visibleScores.add(score);
            }
        }

        Collections.sort(visibleScores, new Comparator<ScoreboardPlayerScore>() {
            @Override
            public int compare(ScoreboardPlayerScore left, ScoreboardPlayerScore right) {
                int scoreCompare = Integer.compare(left.getScore(), right.getScore());
                if (scoreCompare != 0) {
                    return scoreCompare;
                }
                return right.getPlayerName().compareToIgnoreCase(left.getPlayerName());
            }
        });

        if (visibleScores.size() > 15) {
            visibleScores = new ArrayList<ScoreboardPlayerScore>(visibleScores.subList(visibleScores.size() - 15, visibleScores.size()));
        }

        List<String> lines = new ArrayList<String>();
        for (int i = visibleScores.size() - 1; i >= 0; i--) {
            ScoreboardPlayerScore score = visibleScores.get(i);
            Team team = scoreboard.getPlayerTeam(score.getPlayerName());
            lines.add(Team.decorateName(team, new LiteralText(score.getPlayerName())).getString());
        }
        return lines;
    }

    private static String normalizeScoreboardLine(String value) {
        if (value == null) {
            return "";
        }

        String stripped = Formatting.strip(toAsciiWidth(value));
        if (stripped == null) {
            stripped = value;
        }
        return stripped.replace('\u3000', ' ').trim().toUpperCase();
    }

    private static String toAsciiWidth(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '\uFF10' && ch <= '\uFF19') {
                result.append((char) ('0' + (ch - '\uFF10')));
            } else if (ch == '\uFF0E') {
                result.append('.');
            } else if (ch == '\uFF0F' || ch == '\u2215') {
                result.append('/');
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private static String stripFormatting(Text name) {
        String value = name == null ? "" : name.getString();
        String stripped = Formatting.strip(value);
        return stripped == null ? value : stripped.trim();
    }

    private static String getItemKey(ItemStack stack) {
        return createItemKey(getBaseItemId(stack), stripFormatting(stack.getName()));
    }

    private static String getBaseItemId(ItemStack stack) {
        Identifier id = Registry.ITEM.getId(stack.getItem());
        return id == null ? stack.getName().getString() : id.toString();
    }

    private static String createItemKey(String itemId, String plainName) {
        String normalizedItemId = itemId == null ? "" : itemId.trim();
        String normalizedName = normalizeDropName(plainName);
        if (normalizedName.isEmpty()) {
            return normalizedItemId;
        }
        return normalizedItemId + ITEM_KEY_SEPARATOR + normalizedName;
    }

    private static String normalizeDropName(String plainName) {
        return HaItemNameNormalizer.normalize(plainName);
    }

    private static String getItemNameFromKey(String key) {
        if (key == null) {
            return "";
        }
        int separator = key.indexOf(ITEM_KEY_SEPARATOR);
        return separator < 0 ? "" : key.substring(separator + ITEM_KEY_SEPARATOR.length());
    }

    private static String getLegacyItemKey(String key) {
        if (key == null) {
            return "";
        }
        int separator = key.indexOf(ITEM_KEY_SEPARATOR);
        return separator < 0 ? key : key.substring(0, separator);
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
            SavedDropTracker saved = GSON.fromJson(reader, SavedDropTracker.class);
            if (saved == null) {
                return;
            }

            boolean migrationNeeded = false;
            ENTRIES.clear();
            if (saved.entries != null) {
                for (SavedDropEntry savedEntry : saved.entries) {
                    DropEntry entry = toDropEntry(savedEntry);
                    if (entry != null) {
                        DropEntry existing = findDropEntryExact(entry.key);
                        if (existing == null) {
                            ENTRIES.add(entry);
                        } else {
                            existing.count = safeAddCounts(existing.count, entry.count);
                            migrationNeeded = true;
                        }
                        migrationNeeded |= isDropEntryMigrationNeeded(savedEntry, entry);
                    }
                }
            }

            REGISTERED_ITEMS.clear();
            List<LoadedRegisteredItem> loadedRegisteredItems = new ArrayList<LoadedRegisteredItem>();
            if (saved.registeredItems != null) {
                for (SavedRegisteredItem savedItem : saved.registeredItems) {
                    LoadedRegisteredItem loadedItem = toRegisteredItem(savedItem);
                    if (loadedItem != null) {
                        int existingIndex = findLoadedRegisteredItemIndex(loadedRegisteredItems, loadedItem.item.itemId);
                        if (existingIndex < 0) {
                            loadedRegisteredItems.add(loadedItem);
                        } else {
                            LoadedRegisteredItem existing = loadedRegisteredItems.get(existingIndex);
                            if (loadedItem.prefixFree && !existing.prefixFree) {
                                loadedRegisteredItems.set(existingIndex, loadedItem);
                            }
                            migrationNeeded = true;
                        }
                        migrationNeeded |= isRegisteredItemMigrationNeeded(savedItem, loadedItem.item);
                    }
                }
            }
            for (LoadedRegisteredItem loadedItem : loadedRegisteredItems) {
                REGISTERED_ITEMS.add(loadedItem.item);
            }
            if (migrationNeeded) {
                save();
            }
        } catch (IOException ignored) {
        }
    }

    private static void save() {
        try {
            Files.createDirectories(STORAGE_FILE.getParent());
            SavedDropTracker saved = new SavedDropTracker();
            for (DropEntry entry : ENTRIES) {
                saved.entries.add(toSavedEntry(entry));
            }
            for (RegisteredItem item : REGISTERED_ITEMS) {
                saved.registeredItems.add(toSavedRegisteredItem(item));
            }
            try (Writer writer = Files.newBufferedWriter(STORAGE_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(saved, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static SavedDropEntry toSavedEntry(DropEntry entry) {
        SavedDropEntry saved = new SavedDropEntry();
        saved.key = entry.key;
        Identifier id = Registry.ITEM.getId(entry.displayStack.getItem());
        saved.itemId = id == null ? getLegacyItemKey(entry.key) : id.toString();
        saved.displayNameJson = entry.displayName == null ? "" : Text.Serializer.toJson(entry.displayName);
        saved.displayName = entry.displayName == null ? entry.plainName : entry.displayName.getString();
        saved.plainName = entry.plainName;
        saved.count = entry.count;
        return saved;
    }

    private static SavedRegisteredItem toSavedRegisteredItem(RegisteredItem item) {
        SavedRegisteredItem saved = new SavedRegisteredItem();
        saved.itemId = item.itemId;
        saved.displayName = item.displayName;
        saved.unitPrice = item.unitPrice;
        return saved;
    }

    private static DropEntry toDropEntry(SavedDropEntry saved) {
        if (saved == null || saved.count <= 0) {
            return null;
        }

        String itemId = getLegacyItemKey(emptyToFallback(saved.itemId, saved.key));
        String displayName = emptyToFallback(saved.displayName, saved.plainName);
        String sourceName = emptyToFallback(getItemNameFromKey(saved.key), emptyToFallback(saved.plainName, displayName));
        String plainName = normalizeDropName(sourceName);
        String key = createItemKey(itemId, plainName);
        ItemStack displayStack = new ItemStack(getItem(itemId));
        displayStack.setCount(1);
        Text name = HaItemNameNormalizer.preserveStyle(parseSavedName(saved.displayNameJson, displayName), plainName);
        return new DropEntry(key, displayStack, name, plainName, saved.count);
    }

    private static LoadedRegisteredItem toRegisteredItem(SavedRegisteredItem saved) {
        if (saved == null || saved.itemId == null || saved.itemId.isEmpty()) {
            return null;
        }
        String baseItemId = getLegacyItemKey(saved.itemId);
        String sourceName = emptyToFallback(getItemNameFromKey(saved.itemId), saved.displayName);
        String displayName = normalizeDropName(normalizeDisplayName(sourceName, baseItemId));
        String itemId = createItemKey(baseItemId, displayName);
        RegisteredItem item = new RegisteredItem(itemId, displayName, Math.max(0L, saved.unitPrice));
        return new LoadedRegisteredItem(item, !HaItemNameNormalizer.hasRemovablePrefix(sourceName));
    }

    private static DropEntry findDropEntryExact(String key) {
        for (DropEntry entry : ENTRIES) {
            if (entry.key.equals(key)) {
                return entry;
            }
        }
        return null;
    }

    private static int findLoadedRegisteredItemIndex(List<LoadedRegisteredItem> items, String itemId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).item.itemId.equals(itemId)) {
                return i;
            }
        }
        return -1;
    }

    private static int safeAddCounts(int left, int right) {
        long total = (long) left + (long) right;
        return total >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private static boolean isDropEntryMigrationNeeded(SavedDropEntry saved, DropEntry entry) {
        if (saved == null) {
            return false;
        }
        return !entry.key.equals(emptyToFallback(saved.key, entry.key))
            || !entry.plainName.equals(emptyToFallback(saved.plainName, entry.plainName))
            || !entry.plainName.equals(emptyToFallback(saved.displayName, entry.plainName));
    }

    private static boolean isRegisteredItemMigrationNeeded(SavedRegisteredItem saved, RegisteredItem item) {
        return saved != null
            && (!item.itemId.equals(saved.itemId) || !item.displayName.equals(saved.displayName));
    }

    private static Text parseSavedName(String displayNameJson, String fallback) {
        if (displayNameJson != null && !displayNameJson.isEmpty()) {
            try {
                Text parsed = Text.Serializer.fromJson(displayNameJson);
                if (parsed != null) {
                    return parsed;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return new LiteralText(fallback);
    }

    private static Item getItem(String itemId) {
        try {
            Item item = Registry.ITEM.get(new Identifier(itemId));
            return item == Items.AIR && !"minecraft:air".equals(itemId) ? Items.PAPER : item;
        } catch (RuntimeException ignored) {
            return Items.PAPER;
        }
    }

    private static RegisteredItem findRegisteredItem(String itemId) {
        RegisteredItem exact = findRegisteredItemExact(itemId);
        if (exact != null) {
            return exact;
        }

        String legacyKey = getLegacyItemKey(itemId);
        for (RegisteredItem item : REGISTERED_ITEMS) {
            if (item.itemId.equals(legacyKey)) {
                return item;
            }
        }
        return null;
    }

    private static RegisteredItem findRegisteredItemExact(String itemId) {
        for (RegisteredItem item : REGISTERED_ITEMS) {
            if (item.itemId.equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    private static String normalizeDisplayName(String displayName, String fallback) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return fallback;
        }
        return displayName.trim();
    }

    private static String emptyToFallback(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static void startSessionIfNeeded(HaConfig config) {
        if (activeSession) {
            return;
        }
        activeSession = true;
        lastTickMillis = System.currentTimeMillis();
        tickAccumulatorMillis = 0L;
        lastRateUpdateMillis = 0L;
        updateHourlyProfit(config);
    }

    private static void stopSession() {
        if (!activeSession) {
            return;
        }
        activeSession = false;
        lastTickMillis = 0L;
        tickAccumulatorMillis = 0L;
        lastRateUpdateMillis = 0L;
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
            config.dropTrackerElapsedSeconds += seconds;
            config.save();
        }
    }

    private static void updateHourlyProfit(HaConfig config) {
        long now = System.currentTimeMillis();
        if (now - lastRateUpdateMillis < 1000L) {
            return;
        }
        lastRateUpdateMillis = now;
        long elapsedSeconds = config.dropTrackerElapsedSeconds;
        long profit = getEstimatedProfit();
        cachedProfitPerHour = elapsedSeconds <= 0L ? 0L : Math.round(profit * 3600.0D / elapsedSeconds);
    }

    private static final class SavedDropTracker {
        List<SavedDropEntry> entries = new ArrayList<SavedDropEntry>();
        List<SavedRegisteredItem> registeredItems = new ArrayList<SavedRegisteredItem>();
    }

    private static final class SavedDropEntry {
        String key = "";
        String itemId = "";
        String displayNameJson = "";
        String displayName = "";
        String plainName = "";
        int count;
    }

    private static final class SavedRegisteredItem {
        String itemId = "";
        String displayName = "";
        long unitPrice;
    }

    private static final class LoadedRegisteredItem {
        final RegisteredItem item;
        final boolean prefixFree;

        LoadedRegisteredItem(RegisteredItem item, boolean prefixFree) {
            this.item = item;
            this.prefixFree = prefixFree;
        }
    }

    public static final class DropEntry {
        final String key;
        ItemStack displayStack;
        Text displayName;
        String plainName;
        int count;

        DropEntry(String key, ItemStack displayStack, Text displayName, String plainName, int count) {
            this.key = key;
            this.displayStack = displayStack;
            this.displayName = displayName;
            this.plainName = plainName;
            this.count = count;
        }
    }

    public static final class RegisteredItem {
        final String itemId;
        String displayName;
        long unitPrice;

        RegisteredItem(String itemId, String displayName, long unitPrice) {
            this.itemId = itemId;
            this.displayName = displayName;
            this.unitPrice = unitPrice;
        }
    }
}
