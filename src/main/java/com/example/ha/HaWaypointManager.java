package com.example.ha;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public final class HaWaypointManager {
    private static final Formatting[] DEFAULT_COLOR_POOL = new Formatting[] {
        Formatting.RED,
        Formatting.YELLOW,
        Formatting.AQUA,
        Formatting.LIGHT_PURPLE
    };

    private static final Comparator<WaypointEntry> WAYPOINT_COMPARATOR = new Comparator<WaypointEntry>() {
        @Override
        public int compare(WaypointEntry left, WaypointEntry right) {
            int yCompare = Integer.compare(left.y, right.y);
            if (yCompare != 0) {
                return yCompare;
            }
            int xCompare = Integer.compare(left.x, right.x);
            if (xCompare != 0) {
                return xCompare;
            }
            return Integer.compare(left.z, right.z);
        }
    };

    private static boolean loaded;
    private static SavedWaypointState state = new SavedWaypointState();
    private static boolean useActionConsumedUntilRelease;

    private HaWaypointManager() {
    }

    public static void load() {
        if (loaded) {
            return;
        }

        try {
            state = HaWaypointStorage.load();
        } catch (IOException exception) {
            state = new SavedWaypointState();
            System.err.println("[HashimotoAddons] Failed to load waypoints: " + exception.getMessage());
            exception.printStackTrace(System.err);
        }

        normalizeState();
        loaded = true;
    }

    public static void save() {
        ensureLoaded();
        normalizeState();
        try {
            HaWaypointStorage.save(state);
        } catch (IOException exception) {
            System.err.println("[HashimotoAddons] Failed to save waypoints: " + exception.getMessage());
            exception.printStackTrace(System.err);
        }
    }

    public static boolean isEditModeEnabled() {
        ensureLoaded();
        return state.editMode;
    }

    public static boolean toggleEditMode() {
        ensureLoaded();
        state.editMode = !state.editMode;
        save();
        return state.editMode;
    }

    public static boolean isRenderFullBlocks() {
        ensureLoaded();
        return state.renderFullBlocks;
    }

    public static boolean toggleRenderMode() {
        ensureLoaded();
        state.renderFullBlocks = !state.renderFullBlocks;
        save();
        return state.renderFullBlocks;
    }

    public static boolean isThroughWallsEnabled() {
        ensureLoaded();
        return state.renderThroughWalls;
    }

    public static boolean toggleThroughWalls() {
        ensureLoaded();
        state.renderThroughWalls = !state.renderThroughWalls;
        save();
        return state.renderThroughWalls;
    }

    public static int getActiveColorSlot() {
        ensureLoaded();
        return clampSlot(state.activeColorSlot);
    }

    public static int cycleActiveColorSlot() {
        ensureLoaded();
        state.activeColorSlot = (clampSlot(state.activeColorSlot) + 1) % 4;
        save();
        return state.activeColorSlot;
    }

    public static void setActiveColorSlot(int slot) {
        ensureLoaded();
        state.activeColorSlot = clampSlot(slot);
        save();
    }

    public static void setColorSlotFormatting(int slot, Formatting formatting) {
        ensureLoaded();
        if (formatting == null || !formatting.isColor()) {
            return;
        }
        state.colorSlots.set(clampSlot(slot), formatting.name().toLowerCase(Locale.ROOT));
        save();
    }

    public static InputUtil.Key getCycleColorKey() {
        ensureLoaded();
        return getBoundKey(state.cycleKeyType, state.cycleKeyCode, state.cycleKeyScanCode);
    }

    public static void setCycleColorKey(HaKeyCaptureHelper.InputBinding binding) {
        ensureLoaded();
        if (binding == null) {
            state.cycleKeyCode = GLFW.GLFW_KEY_UNKNOWN;
            state.cycleKeyScanCode = -1;
            state.cycleKeyType = "keysym";
        } else if ("mouse".equalsIgnoreCase(binding.type)) {
            state.cycleKeyCode = binding.keyCode;
            state.cycleKeyScanCode = -1;
            state.cycleKeyType = "mouse";
        } else {
            state.cycleKeyCode = binding.keyCode;
            state.cycleKeyScanCode = binding.scanCode;
            state.cycleKeyType = "keysym";
        }
        save();
    }

    public static String getCycleColorKeyName() {
        return HaKeyCaptureHelper.keyName(getCycleColorKey());
    }

    public static Formatting getColorSlotFormatting(int slot) {
        ensureLoaded();
        return resolveFormatting(slot);
    }

    public static String getColorSlotName(int slot) {
        Formatting formatting = getColorSlotFormatting(slot);
        String rawName = formatting == null ? "White" : formatting.getName();
        return toDisplayName(rawName);
    }

    public static String getColorSlotDisplayText(int slot) {
        return "Slot " + (clampSlot(slot) + 1) + ": " + getColorSlotName(slot);
    }

    public static List<Formatting> getSelectableColors() {
        List<Formatting> colors = new ArrayList<Formatting>();
        for (Formatting formatting : Formatting.values()) {
            if (formatting.isColor()) {
                colors.add(formatting);
            }
        }
        return colors;
    }

    public static String getCurrentDimensionKey(MinecraftClient client) {
        if (client == null || client.world == null || client.world.getRegistryKey() == null || client.world.getRegistryKey().getValue() == null) {
            return null;
        }
        return client.world.getRegistryKey().getValue().toString();
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.options == null || !client.options.keyUse.isPressed()) {
            useActionConsumedUntilRelease = false;
        }
    }

    public static int getWaypointCountForCurrentDimension(MinecraftClient client) {
        String dimensionKey = getCurrentDimensionKey(client);
        return dimensionKey == null ? 0 : getWaypointCountForDimension(dimensionKey);
    }

    public static int getWaypointCountForDimension(String dimensionKey) {
        ensureLoaded();
        List<SavedWaypointEntry> savedEntries = state.waypointsByDimension.get(dimensionKey);
        return savedEntries == null ? 0 : savedEntries.size();
    }

    public static List<WaypointEntry> getWaypointsForCurrentDimension(MinecraftClient client) {
        String dimensionKey = getCurrentDimensionKey(client);
        if (dimensionKey == null) {
            return new ArrayList<WaypointEntry>();
        }
        return getWaypointsForDimension(dimensionKey);
    }

    public static List<WaypointEntry> getWaypointsForDimension(String dimensionKey) {
        ensureLoaded();
        List<WaypointEntry> entries = new ArrayList<WaypointEntry>();
        List<SavedWaypointEntry> savedEntries = state.waypointsByDimension.get(dimensionKey);
        if (savedEntries == null) {
            return entries;
        }

        for (SavedWaypointEntry savedEntry : savedEntries) {
            WaypointEntry entry = toEntry(dimensionKey, savedEntry);
            if (entry != null) {
                entries.add(entry);
            }
        }

        Collections.sort(entries, WAYPOINT_COMPARATOR);
        return entries;
    }

    public static WaypointEntry getWaypoint(MinecraftClient client, BlockPos pos) {
        return getWaypoint(getCurrentDimensionKey(client), pos);
    }

    public static WaypointEntry getWaypoint(String dimensionKey, BlockPos pos) {
        ensureLoaded();
        if (dimensionKey == null || pos == null) {
            return null;
        }

        List<SavedWaypointEntry> savedEntries = state.waypointsByDimension.get(dimensionKey);
        if (savedEntries == null) {
            return null;
        }

        for (SavedWaypointEntry savedEntry : savedEntries) {
            if (savedEntry != null && savedEntry.x == pos.getX() && savedEntry.y == pos.getY() && savedEntry.z == pos.getZ()) {
                return toEntry(dimensionKey, savedEntry);
            }
        }
        return null;
    }

    public static boolean upsertCurrentWaypoint(MinecraftClient client, BlockPos pos, String label, int colorSlotIndex) {
        return upsertWaypoint(getCurrentDimensionKey(client), pos, label, colorSlotIndex);
    }

    public static boolean upsertWaypoint(String dimensionKey, BlockPos pos, String label, int colorSlotIndex) {
        ensureLoaded();
        if (dimensionKey == null || pos == null) {
            return false;
        }

        List<SavedWaypointEntry> savedEntries = state.waypointsByDimension.get(dimensionKey);
        if (savedEntries == null) {
            savedEntries = new ArrayList<SavedWaypointEntry>();
            state.waypointsByDimension.put(dimensionKey, savedEntries);
        }

        SavedWaypointEntry existing = null;
        for (SavedWaypointEntry savedEntry : savedEntries) {
            if (savedEntry != null && savedEntry.x == pos.getX() && savedEntry.y == pos.getY() && savedEntry.z == pos.getZ()) {
                existing = savedEntry;
                break;
            }
        }

        if (existing == null) {
            existing = new SavedWaypointEntry();
            existing.x = pos.getX();
            existing.y = pos.getY();
            existing.z = pos.getZ();
            savedEntries.add(existing);
        }

        existing.label = normalizeLabel(label);
        existing.colorSlotIndex = clampSlot(colorSlotIndex);
        normalizeState();
        save();
        return true;
    }

    public static boolean removeCurrentWaypoint(MinecraftClient client, BlockPos pos) {
        return removeWaypoint(getCurrentDimensionKey(client), pos);
    }

    public static boolean removeWaypoint(String dimensionKey, BlockPos pos) {
        ensureLoaded();
        if (dimensionKey == null || pos == null) {
            return false;
        }

        List<SavedWaypointEntry> savedEntries = state.waypointsByDimension.get(dimensionKey);
        if (savedEntries == null) {
            return false;
        }

        boolean removed = false;
        for (int i = savedEntries.size() - 1; i >= 0; i--) {
            SavedWaypointEntry savedEntry = savedEntries.get(i);
            if (savedEntry != null && savedEntry.x == pos.getX() && savedEntry.y == pos.getY() && savedEntry.z == pos.getZ()) {
                savedEntries.remove(i);
                removed = true;
                break;
            }
        }

        if (savedEntries.isEmpty()) {
            state.waypointsByDimension.remove(dimensionKey);
        }

        if (removed) {
            save();
        }
        return removed;
    }

    public static boolean clearCurrentDimension(MinecraftClient client) {
        String dimensionKey = getCurrentDimensionKey(client);
        if (dimensionKey == null) {
            return false;
        }

        ensureLoaded();
        List<SavedWaypointEntry> removed = state.waypointsByDimension.remove(dimensionKey);
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    public static String getDisplayLabel(WaypointEntry entry) {
        if (entry == null) {
            return "";
        }
        if (entry.label != null && !entry.label.trim().isEmpty()) {
            return entry.label.trim();
        }
        return formatPosition(entry.x, entry.y, entry.z);
    }

    public static String formatPosition(BlockPos pos) {
        if (pos == null) {
            return "";
        }
        return formatPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    public static String formatPosition(int x, int y, int z) {
        return x + ", " + y + ", " + z;
    }

    public static boolean tryUse(MinecraftClient client) {
        if (!canEdit(client) || client == null || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        if (useActionConsumedUntilRelease) {
            return true;
        }

        BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
        WaypointEntry existing = getWaypoint(client, pos);
        if (Screen.hasShiftDown()) {
            openLabelScreen(client, pos, existing);
            useActionConsumedUntilRelease = true;
            return true;
        }

        if (existing != null) {
            upsertCurrentWaypoint(client, pos, existing.label, getActiveColorSlot());
            notify(client, "Waypoint color updated.");
            useActionConsumedUntilRelease = true;
            return true;
        }

        upsertCurrentWaypoint(client, pos, "", getActiveColorSlot());
        notify(client, "Waypoint placed.");
        useActionConsumedUntilRelease = true;
        return true;
    }

    public static boolean tryAttack(MinecraftClient client) {
        if (!canEdit(client) || client == null || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
        if (!removeCurrentWaypoint(client, pos)) {
            return false;
        }

        notify(client, "Waypoint removed.");
        return true;
    }

    public static boolean shouldCancelBlockBreaking(MinecraftClient client) {
        return canEdit(client)
            && client != null
            && client.crosshairTarget != null
            && client.crosshairTarget.getType() == HitResult.Type.BLOCK
            && getWaypoint(client, ((BlockHitResult) client.crosshairTarget).getBlockPos()) != null;
    }

    public static String getRenderModeLabel() {
        return isRenderFullBlocks() ? "Full Block" : "Outline Only";
    }

    public static String getThroughWallsLabel() {
        return isThroughWallsEnabled() ? "Enabled" : "Disabled";
    }

    public static String getEditModeLabel() {
        return isEditModeEnabled() ? "Enabled" : "Disabled";
    }

    public static final class WaypointEntry {
        public final String dimensionKey;
        public final int x;
        public final int y;
        public final int z;
        public final String label;
        public final int colorSlotIndex;

        WaypointEntry(String dimensionKey, int x, int y, int z, String label, int colorSlotIndex) {
            this.dimensionKey = dimensionKey;
            this.x = x;
            this.y = y;
            this.z = z;
            this.label = label == null ? "" : label;
            this.colorSlotIndex = clampSlot(colorSlotIndex);
        }

        public BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }
    }

    private static void openLabelScreen(MinecraftClient client, BlockPos pos, WaypointEntry existing) {
        if (client == null) {
            return;
        }

        String dimensionKey = getCurrentDimensionKey(client);
        if (dimensionKey == null) {
            return;
        }

        String label = existing == null ? "" : existing.label;
        int colorSlotIndex = existing == null ? getActiveColorSlot() : existing.colorSlotIndex;
        client.openScreen(new HaWaypointLabelScreen(
            client.currentScreen,
            dimensionKey,
            pos,
            label,
            colorSlotIndex,
            existing != null
        ));
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private static void normalizeState() {
        if (state == null) {
            state = new SavedWaypointState();
        }
        if (state.waypointsByDimension == null) {
            state.waypointsByDimension = new LinkedHashMap<String, List<SavedWaypointEntry>>();
        }
        if (state.colorSlots == null) {
            state.colorSlots = new ArrayList<String>();
        }

        while (state.colorSlots.size() < 4) {
            state.colorSlots.add(DEFAULT_COLOR_POOL[state.colorSlots.size()].name().toLowerCase(Locale.ROOT));
        }
        while (state.colorSlots.size() > 4) {
            state.colorSlots.remove(state.colorSlots.size() - 1);
        }

        for (int i = 0; i < state.colorSlots.size(); i++) {
            Formatting formatting = Formatting.byName(state.colorSlots.get(i));
            if (formatting == null || !formatting.isColor()) {
                state.colorSlots.set(i, DEFAULT_COLOR_POOL[i].name().toLowerCase(Locale.ROOT));
            } else {
                state.colorSlots.set(i, formatting.name().toLowerCase(Locale.ROOT));
            }
        }

        state.activeColorSlot = clampSlot(state.activeColorSlot);
        state.cycleKeyCode = state.cycleKeyCode == 0 ? GLFW.GLFW_KEY_UNKNOWN : state.cycleKeyCode;
        if (state.cycleKeyType == null || state.cycleKeyType.trim().isEmpty()) {
            state.cycleKeyType = "keysym";
        }

        List<String> emptyKeys = new ArrayList<String>();
        for (Map.Entry<String, List<SavedWaypointEntry>> dimensionEntry : state.waypointsByDimension.entrySet()) {
            String dimensionKey = dimensionEntry.getKey();
            List<SavedWaypointEntry> savedEntries = dimensionEntry.getValue();
            if (dimensionKey == null || dimensionKey.trim().isEmpty() || savedEntries == null) {
                emptyKeys.add(dimensionKey);
                continue;
            }

            List<SavedWaypointEntry> normalizedEntries = new ArrayList<SavedWaypointEntry>();
            for (SavedWaypointEntry savedEntry : savedEntries) {
                if (savedEntry == null) {
                    continue;
                }
                savedEntry.label = normalizeLabel(savedEntry.label);
                savedEntry.colorSlotIndex = clampSlot(savedEntry.colorSlotIndex);
                normalizedEntries.add(savedEntry);
            }
            Collections.sort(normalizedEntries, new Comparator<SavedWaypointEntry>() {
                @Override
                public int compare(SavedWaypointEntry left, SavedWaypointEntry right) {
                    int yCompare = Integer.compare(left.y, right.y);
                    if (yCompare != 0) {
                        return yCompare;
                    }
                    int xCompare = Integer.compare(left.x, right.x);
                    if (xCompare != 0) {
                        return xCompare;
                    }
                    return Integer.compare(left.z, right.z);
                }
            });
            dimensionEntry.setValue(normalizedEntries);
        }

        for (String key : emptyKeys) {
            state.waypointsByDimension.remove(key);
        }
    }

    private static WaypointEntry toEntry(String dimensionKey, SavedWaypointEntry savedEntry) {
        if (dimensionKey == null || savedEntry == null) {
            return null;
        }
        return new WaypointEntry(
            dimensionKey,
            savedEntry.x,
            savedEntry.y,
            savedEntry.z,
            normalizeLabel(savedEntry.label),
            clampSlot(savedEntry.colorSlotIndex)
        );
    }

    private static boolean canEdit(MinecraftClient client) {
        return client != null
            && client.world != null
            && client.player != null
            && isEditModeEnabled();
    }

    private static Formatting resolveFormatting(int slot) {
        int normalized = clampSlot(slot);
        String name = state.colorSlots.get(normalized);
        Formatting formatting = Formatting.byName(name);
        if (formatting == null || !formatting.isColor()) {
            formatting = DEFAULT_COLOR_POOL[normalized];
        }
        return formatting;
    }

    private static int clampSlot(int slot) {
        if (slot < 0) {
            return 0;
        }
        if (slot > 3) {
            return 3;
        }
        return slot;
    }

    private static String normalizeLabel(String label) {
        if (label == null) {
            return "";
        }
        String trimmed = label.trim();
        String stripped = Formatting.strip(trimmed);
        return stripped == null ? trimmed : stripped.trim();
    }

    private static String toDisplayName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "White";
        }
        String trimmed = name.trim().toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(trimmed.length());
        boolean upperNext = true;
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch == '_' || ch == '-') {
                result.append(' ');
                upperNext = true;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return result.toString();
    }

    private static void notify(MinecraftClient client, String message) {
        if (client != null && client.player != null) {
            client.player.sendMessage(new LiteralText("[\u00a7l\u00a7bHashimotoAddons\u00a7r]:" + message), false);
        }
    }

    private static InputUtil.Key getBoundKey(String type, int keyCode, int scanCode) {
        if ("mouse".equalsIgnoreCase(type)) {
            return InputUtil.Type.MOUSE.createFromCode(keyCode);
        }
        return InputUtil.fromKeyCode(keyCode, scanCode);
    }
}
