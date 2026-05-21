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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaEvolutionForgeHelper {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STORAGE_FILE = FabricLoader.getInstance().getConfigDir().resolve("HashimotoAddons").resolve("evolution_forge_items.json");
    private static final String FORGE_TITLE = "\u30a8\u30dc\u30ea\u30e5\u30fc\u30b7\u30e7\u30f3\u30d5\u30a9\u30fc\u30b8";
    private static final String CONSUME_HEADER = "\u88fd\u4f5c\u6642\u6d88\u8cbb\u30a2\u30a4\u30c6\u30e0";
    private static final String LEFT_CLICK = "\u5de6\u30af\u30ea\u30c3\u30af";
    private static final String RIGHT_CLICK = "\u53f3\u30af\u30ea\u30c3\u30af";
    private static final String MARKER = "Evo?: Yes";
    private static final Pattern LEADING_MARKERS = Pattern.compile("^[\\s\\u2715\\u2716\\u00d7xX*\\-:：・]+");
    private static final Pattern LEADING_COUNT = Pattern.compile("^[0-9]+\\s+");
    private static final Map<String, Set<String>> ITEMS_BY_SERVER = new LinkedHashMap<String, Set<String>>();
    private static boolean loaded;
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

    public static boolean shouldMarkTooltip(ItemStack stack, List<Text> tooltip) {
        if (!HaConfig.get().evolutionForgeHelperEnabled || stack == null || stack.isEmpty()) {
            return false;
        }

        Set<String> targets = getItemsForCurrentServer();
        if (targets.isEmpty()) {
            return false;
        }

        if (matchesAnyTarget(stack.getName().getString(), targets)) {
            return true;
        }
        if (tooltip != null) {
            for (Text line : tooltip) {
                if (line != null && matchesAnyTarget(line.getString(), targets)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void appendMarker(List<Text> tooltip) {
        if (tooltip == null || hasMarker(tooltip)) {
            return;
        }
        tooltip.add(new LiteralText(MARKER).formatted(Formatting.GREEN));
    }

    public static int getCurrentServerItemCount() {
        return getItemsForCurrentServer().size();
    }

    public static void clearCurrentServerItems() {
        load();
        ITEMS_BY_SERVER.remove(getServerKey(MinecraftClient.getInstance()));
        save();
    }

    private static void scanVisiblePage(MinecraftClient client, GenericContainerScreenHandler handler) {
        load();
        Set<String> items = getOrCreateItems(getServerKey(client));
        int before = items.size();

        int containerSlots = handler.getRows() * 9;
        int limit = Math.min(containerSlots, handler.slots.size());
        for (int i = 0; i < limit; i++) {
            Slot slot = handler.slots.get(i);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                continue;
            }
            addConsumedItems(items, stack.getTooltip(client.player, TooltipContext.Default.NORMAL));
        }

        if (items.size() != before) {
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

    private static void addCandidate(Set<String> items, String rawLine) {
        String normalized = normalizeItemName(rawLine);
        if (!normalized.isEmpty()) {
            items.add(normalized);
        }
    }

    private static boolean isEvolutionForgeScreen(MinecraftClient client) {
        if (client == null || !(client.currentScreen instanceof GenericContainerScreen)) {
            return false;
        }
        String title = client.currentScreen.getTitle() == null ? "" : client.currentScreen.getTitle().getString();
        return normalizeDisplay(title).contains(FORGE_TITLE);
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

    private static Set<String> getItemsForCurrentServer() {
        load();
        Set<String> items = ITEMS_BY_SERVER.get(getServerKey(MinecraftClient.getInstance()));
        return items == null ? new LinkedHashSet<String>() : items;
    }

    private static Set<String> getOrCreateItems(String serverKey) {
        Set<String> items = ITEMS_BY_SERVER.get(serverKey);
        if (items == null) {
            items = new LinkedHashSet<String>();
            ITEMS_BY_SERVER.put(serverKey, items);
        }
        return items;
    }

    private static String createSignature(GenericContainerScreenHandler handler) {
        StringBuilder result = new StringBuilder();
        int containerSlots = handler.getRows() * 9;
        int limit = Math.min(containerSlots, handler.slots.size());
        for (int i = 0; i < limit; i++) {
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
                ITEMS_BY_SERVER.clear();
                for (ServerItems server : saved.servers) {
                    if (server == null || server.serverKey == null || server.serverKey.trim().isEmpty() || server.items == null) {
                        continue;
                    }
                    Set<String> items = new LinkedHashSet<String>();
                    for (String item : server.items) {
                        String normalized = normalizeItemName(item);
                        if (!normalized.isEmpty()) {
                            items.add(normalized);
                        }
                    }
                    if (!items.isEmpty()) {
                        ITEMS_BY_SERVER.put(server.serverKey, items);
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
            for (Map.Entry<String, Set<String>> entry : ITEMS_BY_SERVER.entrySet()) {
                ServerItems server = new ServerItems();
                server.serverKey = entry.getKey();
                server.items = new ArrayList<String>(entry.getValue());
                saved.servers.add(server);
            }
            try (Writer writer = Files.newBufferedWriter(STORAGE_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(saved, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static final class SavedEvolutionForgeItems {
        List<ServerItems> servers = new ArrayList<ServerItems>();
    }

    private static final class ServerItems {
        String serverKey = "";
        List<String> items = new ArrayList<String>();
    }
}
