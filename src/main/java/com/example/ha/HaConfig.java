package com.example.ha;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class HaConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("HashimotoAddons");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");
    private static final HaConfig INSTANCE = new HaConfig();

    public final List<SwapEntry> swapEntries = new ArrayList<SwapEntry>();
    public final List<HpAlertEntry> hpAlertEntries = new ArrayList<HpAlertEntry>();
    public final List<ManaAlertEntry> manaAlertEntries = new ArrayList<ManaAlertEntry>();
    public final List<ChatFilterEntry> chatFilterEntries = new ArrayList<ChatFilterEntry>();

    public boolean autoHealEnabled = false;
    public int autoHealHotbarSlot = 1;
    public double autoHealCooldownSeconds = 1.0D;
    // Trigger Auto Heal when (health / maxHealth) is <= this value.
    // Default 0.75 matches "15 hearts out of 20 hearts".
    public float autoHealHealthRatioThreshold = 0.75F;

    public boolean macroEnabled = true;
    public int macroToggleKeyCode = GLFW.GLFW_KEY_H;
    public int macroToggleScanCode = -1;
    public boolean macroStatusHudEnabled = false;
    public int macroStatusHudX = 8;
    public int macroStatusHudY = 8;
    public boolean extrasEnabled = true;
    public boolean ghostWallEditMode = false;
    public boolean extrasHudEnabled = false;
    public int extrasHudX = 8;
    public int extrasHudY = 24;
    public String selectedGhostBlockId = "minecraft:glass";
    public final List<String> favoriteGhostBlockIds = new ArrayList<String>();
    public int cameraToggleKeyCode = GLFW.GLFW_KEY_V;
    public int cameraToggleScanCode = -1;
    public int defaultWeaponHotbarSlot = 0;
    public boolean itemLockEnabled = true;
    public boolean soulbindProtectionEnabled = true;
    public boolean chunkChestCounterEnabled = false;
    public int chunkChestOverlayX = 8;
    public int chunkChestOverlayY = 40;
    public boolean chestSearchEnabled = false;
    public String chestSearchQuery = "";
    public int chestSearchKeyCode = GLFW.GLFW_KEY_UNKNOWN;
    public int chestSearchScanCode = -1;
    public boolean dropTrackerEnabled = false;
    public String dropTrackerMode = HaDropTracker.MODE_ALL;
    public int dropTrackerOverlayX = 8;
    public int dropTrackerOverlayY = 72;
    public boolean expTrackerEnabled = false;
    public long expTrackerTotal = 0L;
    public int expTrackerOverlayX = 8;
    public int expTrackerOverlayY = 104;
    public boolean chatFilterEnabled = false;
    public Set<Integer> lockedSlotIds = new HashSet<Integer>();

    private HaConfig() {
    }

    public static HaConfig get() {
        return INSTANCE;
    }

    public void normalize() {
        for (SwapEntry entry : swapEntries) {
            entry.normalize();
        }
        for (HpAlertEntry entry : hpAlertEntries) {
            entry.normalize();
        }
        for (ManaAlertEntry entry : manaAlertEntries) {
            entry.normalize();
        }
        for (int i = chatFilterEntries.size() - 1; i >= 0; i--) {
            ChatFilterEntry entry = chatFilterEntries.get(i);
            entry.normalize();
            if (entry.matchText.isEmpty()) {
                chatFilterEntries.remove(i);
            }
        }
        normalizeGhostBlockSettings();

        autoHealHotbarSlot = clamp(autoHealHotbarSlot, 0, 8);
        defaultWeaponHotbarSlot = clamp(defaultWeaponHotbarSlot, 0, 8);
        macroStatusHudX = Math.max(0, macroStatusHudX);
        macroStatusHudY = Math.max(0, macroStatusHudY);
        autoHealCooldownSeconds = clamp(autoHealCooldownSeconds, 0.05D, 60.0D);
        autoHealHealthRatioThreshold = (float) clamp(autoHealHealthRatioThreshold, 0.05D, 1.0D);
        chunkChestOverlayX = Math.max(0, chunkChestOverlayX);
        chunkChestOverlayY = Math.max(0, chunkChestOverlayY);
        if (chestSearchQuery == null) {
            chestSearchQuery = "";
        } else {
            chestSearchQuery = chestSearchQuery.trim();
        }
        dropTrackerMode = HaDropTracker.normalizeMode(dropTrackerMode);
        dropTrackerOverlayX = Math.max(0, dropTrackerOverlayX);
        dropTrackerOverlayY = Math.max(0, dropTrackerOverlayY);
        expTrackerTotal = Math.max(0L, expTrackerTotal);
        expTrackerOverlayX = Math.max(0, expTrackerOverlayX);
        expTrackerOverlayY = Math.max(0, expTrackerOverlayY);
        if (lockedSlotIds == null) {
            lockedSlotIds = new HashSet<Integer>();
        }
    }

    public SwapEntry addSwapEntry() {
        SwapEntry entry = new SwapEntry();
        swapEntries.add(entry);
        return entry;
    }

    public void removeSwapEntry(int index) {
        if (index >= 0 && index < swapEntries.size()) {
            swapEntries.remove(index);
        }
    }

    public HpAlertEntry addHpAlertEntry() {
        HpAlertEntry entry = new HpAlertEntry();
        hpAlertEntries.add(entry);
        return entry;
    }

    public void removeHpAlertEntry(int index) {
        if (index >= 0 && index < hpAlertEntries.size()) {
            hpAlertEntries.remove(index);
        }
    }

    public ManaAlertEntry addManaAlertEntry() {
        ManaAlertEntry entry = new ManaAlertEntry();
        manaAlertEntries.add(entry);
        return entry;
    }

    public void removeManaAlertEntry(int index) {
        if (index >= 0 && index < manaAlertEntries.size()) {
            manaAlertEntries.remove(index);
        }
    }

    public ChatFilterEntry addChatFilterEntry() {
        ChatFilterEntry entry = new ChatFilterEntry();
        chatFilterEntries.add(entry);
        return entry;
    }

    public void removeChatFilterEntry(int index) {
        if (index >= 0 && index < chatFilterEntries.size()) {
            chatFilterEntries.remove(index);
        }
    }

    public InputUtil.Key getMacroToggleKey() {
        return InputUtil.fromKeyCode(macroToggleKeyCode, macroToggleScanCode);
    }

    public InputUtil.Key getCameraToggleKey() {
        return InputUtil.fromKeyCode(cameraToggleKeyCode, cameraToggleScanCode);
    }

    public InputUtil.Key getChestSearchKey() {
        return InputUtil.fromKeyCode(chestSearchKeyCode, chestSearchScanCode);
    }

    public void load() {
        if (!Files.exists(CONFIG_FILE)) {
            normalize();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            SavedConfig saved = GSON.fromJson(reader, SavedConfig.class);
            apply(saved);
        } catch (IOException ignored) {
        }

        normalize();
    }

    public void save() {
        normalize();

        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(toJson(), writer);
            }
        } catch (IOException ignored) {
        }
    }

    private void apply(SavedConfig saved) {
        swapEntries.clear();
        hpAlertEntries.clear();
        manaAlertEntries.clear();
        chatFilterEntries.clear();
        if (saved == null) {
            return;
        }

        cameraToggleKeyCode = saved.cameraToggleKeyCode;
        cameraToggleScanCode = saved.cameraToggleScanCode;
        itemLockEnabled = saved.itemLockEnabled;
        soulbindProtectionEnabled = saved.soulbindProtectionEnabled;
        chestSearchEnabled = saved.chestSearchEnabled;
        chestSearchQuery = saved.chestSearchQuery;
        chestSearchKeyCode = saved.chestSearchKeyCode;
        chestSearchScanCode = saved.chestSearchScanCode;
        dropTrackerEnabled = saved.dropTrackerEnabled;
        dropTrackerMode = saved.dropTrackerMode;
        dropTrackerOverlayX = saved.dropTrackerOverlayX;
        dropTrackerOverlayY = saved.dropTrackerOverlayY;
        expTrackerEnabled = saved.expTrackerEnabled;
        expTrackerTotal = saved.expTrackerTotal;
        expTrackerOverlayX = saved.expTrackerOverlayX;
        expTrackerOverlayY = saved.expTrackerOverlayY;
        chatFilterEnabled = saved.chatFilterEnabled;
        lockedSlotIds = saved.lockedSlotIds != null ? new HashSet<Integer>(saved.lockedSlotIds) : new HashSet<Integer>();

        if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            autoHealEnabled = saved.autoHealEnabled;
            autoHealHotbarSlot = saved.autoHealHotbarSlot;
            autoHealCooldownSeconds = saved.autoHealCooldownSeconds > 0.0D ? saved.autoHealCooldownSeconds : 1.0D;
            autoHealHealthRatioThreshold = saved.autoHealHealthRatioThreshold > 0.0F ? saved.autoHealHealthRatioThreshold : 0.75F;
            macroEnabled = saved.macroEnabled;
            macroToggleKeyCode = saved.macroToggleKeyCode;
            macroToggleScanCode = saved.macroToggleScanCode;
            macroStatusHudEnabled = saved.macroStatusHudEnabled;
            macroStatusHudX = saved.macroStatusHudX;
            macroStatusHudY = saved.macroStatusHudY;
            extrasEnabled = saved.extrasEnabled;
            ghostWallEditMode = saved.ghostWallEditMode;
            extrasHudEnabled = saved.extrasHudEnabled;
            extrasHudX = saved.extrasHudX;
            extrasHudY = saved.extrasHudY;
            selectedGhostBlockId = saved.selectedGhostBlockId;
            favoriteGhostBlockIds.clear();
            if (saved.favoriteGhostBlockIds != null) {
                favoriteGhostBlockIds.addAll(saved.favoriteGhostBlockIds);
            }
            defaultWeaponHotbarSlot = saved.defaultWeaponHotbarSlot;
            chunkChestCounterEnabled = saved.chunkChestCounterEnabled;
            chunkChestOverlayX = saved.chunkChestOverlayX;
            chunkChestOverlayY = saved.chunkChestOverlayY;

            if (saved.swapEntries != null) {
                for (SavedSwapEntry savedEntry : saved.swapEntries) {
                    SwapEntry entry = new SwapEntry();
                    if (savedEntry != null) {
                        entry.name = savedEntry.name;
                        entry.hotbarSlot = savedEntry.hotbarSlot;
                        entry.intervalSeconds = savedEntry.intervalSeconds;
                        entry.holdTicks = savedEntry.holdTicks;
                    }
                    entry.normalize();
                    swapEntries.add(entry);
                }
            }
        } else {
            resetDangerousState();
        }

        if (saved.hpAlertEntries != null) {
            for (SavedHpAlertEntry savedEntry : saved.hpAlertEntries) {
                HpAlertEntry entry = new HpAlertEntry();
                if (savedEntry != null) {
                    entry.enabled = savedEntry.enabled;
                    entry.healthPercentage = savedEntry.healthPercentage;
                    entry.colorIndex = savedEntry.colorIndex;
                    entry.titleText = savedEntry.titleText;
                }
                entry.normalize();
                hpAlertEntries.add(entry);
            }
        }

        if (saved.manaAlertEntries != null) {
            for (SavedManaAlertEntry savedEntry : saved.manaAlertEntries) {
                ManaAlertEntry entry = new ManaAlertEntry();
                if (savedEntry != null) {
                    entry.enabled = savedEntry.enabled;
                    entry.manaPercentage = savedEntry.manaPercentage;
                    entry.colorIndex = savedEntry.colorIndex;
                    entry.titleText = savedEntry.titleText;
                }
                entry.normalize();
                manaAlertEntries.add(entry);
            }
        }

        if (saved.chatFilterEntries != null) {
            for (SavedChatFilterEntry savedEntry : saved.chatFilterEntries) {
                ChatFilterEntry entry = new ChatFilterEntry();
                if (savedEntry != null) {
                    entry.enabled = savedEntry.enabled;
                    entry.matchText = savedEntry.matchText;
                }
                entry.normalize();
                if (!entry.matchText.isEmpty()) {
                    chatFilterEntries.add(entry);
                }
            }
        }
    }

    private JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("cameraToggleKeyCode", cameraToggleKeyCode);
        root.addProperty("cameraToggleScanCode", cameraToggleScanCode);
        root.addProperty("itemLockEnabled", itemLockEnabled);
        root.addProperty("soulbindProtectionEnabled", soulbindProtectionEnabled);
        root.addProperty("chestSearchEnabled", chestSearchEnabled);
        root.addProperty("chestSearchQuery", chestSearchQuery);
        root.addProperty("chestSearchKeyCode", chestSearchKeyCode);
        root.addProperty("chestSearchScanCode", chestSearchScanCode);
        root.addProperty("dropTrackerEnabled", dropTrackerEnabled);
        root.addProperty("dropTrackerMode", dropTrackerMode);
        root.addProperty("dropTrackerOverlayX", dropTrackerOverlayX);
        root.addProperty("dropTrackerOverlayY", dropTrackerOverlayY);
        root.addProperty("expTrackerEnabled", expTrackerEnabled);
        root.addProperty("expTrackerTotal", expTrackerTotal);
        root.addProperty("expTrackerOverlayX", expTrackerOverlayX);
        root.addProperty("expTrackerOverlayY", expTrackerOverlayY);
        root.addProperty("chatFilterEnabled", chatFilterEnabled);
        root.add("lockedSlotIds", GSON.toJsonTree(new HashSet<Integer>(lockedSlotIds)));
        root.add("hpAlertEntries", GSON.toJsonTree(toSavedHpAlertEntries()));
        root.add("manaAlertEntries", GSON.toJsonTree(toSavedManaAlertEntries()));
        root.add("chatFilterEntries", GSON.toJsonTree(toSavedChatFilterEntries()));

        if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            root.addProperty("autoHealEnabled", autoHealEnabled);
            root.addProperty("autoHealHotbarSlot", autoHealHotbarSlot);
            root.addProperty("autoHealCooldownSeconds", autoHealCooldownSeconds);
            root.addProperty("autoHealHealthRatioThreshold", autoHealHealthRatioThreshold);
            root.addProperty("macroEnabled", macroEnabled);
            root.addProperty("macroToggleKeyCode", macroToggleKeyCode);
            root.addProperty("macroToggleScanCode", macroToggleScanCode);
            root.addProperty("macroStatusHudEnabled", macroStatusHudEnabled);
            root.addProperty("macroStatusHudX", macroStatusHudX);
            root.addProperty("macroStatusHudY", macroStatusHudY);
            root.addProperty("extrasEnabled", extrasEnabled);
            root.addProperty("ghostWallEditMode", ghostWallEditMode);
            root.addProperty("extrasHudEnabled", extrasHudEnabled);
            root.addProperty("extrasHudX", extrasHudX);
            root.addProperty("extrasHudY", extrasHudY);
            root.addProperty("selectedGhostBlockId", selectedGhostBlockId);
            root.add("favoriteGhostBlockIds", GSON.toJsonTree(new ArrayList<String>(favoriteGhostBlockIds)));
            root.addProperty("defaultWeaponHotbarSlot", defaultWeaponHotbarSlot);
            root.addProperty("chunkChestCounterEnabled", chunkChestCounterEnabled);
            root.addProperty("chunkChestOverlayX", chunkChestOverlayX);
            root.addProperty("chunkChestOverlayY", chunkChestOverlayY);
            root.add("swapEntries", GSON.toJsonTree(toSavedSwapEntries()));
        }

        return root;
    }

    private List<SavedSwapEntry> toSavedSwapEntries() {
        List<SavedSwapEntry> savedEntries = new ArrayList<SavedSwapEntry>();
        for (SwapEntry entry : swapEntries) {
            SavedSwapEntry savedEntry = new SavedSwapEntry();
            savedEntry.name = entry.name;
            savedEntry.hotbarSlot = entry.hotbarSlot;
            savedEntry.intervalSeconds = entry.intervalSeconds;
            savedEntry.holdTicks = entry.holdTicks;
            savedEntries.add(savedEntry);
        }
        return savedEntries;
    }

    private List<SavedHpAlertEntry> toSavedHpAlertEntries() {
        List<SavedHpAlertEntry> savedEntries = new ArrayList<SavedHpAlertEntry>();
        for (HpAlertEntry entry : hpAlertEntries) {
            SavedHpAlertEntry savedEntry = new SavedHpAlertEntry();
            savedEntry.enabled = entry.enabled;
            savedEntry.healthPercentage = entry.healthPercentage;
            savedEntry.colorIndex = entry.colorIndex;
            savedEntry.titleText = entry.titleText;
            savedEntries.add(savedEntry);
        }
        return savedEntries;
    }

    private List<SavedManaAlertEntry> toSavedManaAlertEntries() {
        List<SavedManaAlertEntry> savedEntries = new ArrayList<SavedManaAlertEntry>();
        for (ManaAlertEntry entry : manaAlertEntries) {
            SavedManaAlertEntry savedEntry = new SavedManaAlertEntry();
            savedEntry.enabled = entry.enabled;
            savedEntry.manaPercentage = entry.manaPercentage;
            savedEntry.colorIndex = entry.colorIndex;
            savedEntry.titleText = entry.titleText;
            savedEntries.add(savedEntry);
        }
        return savedEntries;
    }

    private List<SavedChatFilterEntry> toSavedChatFilterEntries() {
        List<SavedChatFilterEntry> savedEntries = new ArrayList<SavedChatFilterEntry>();
        for (ChatFilterEntry entry : chatFilterEntries) {
            if (entry.matchText == null || entry.matchText.trim().isEmpty()) {
                continue;
            }
            SavedChatFilterEntry savedEntry = new SavedChatFilterEntry();
            savedEntry.enabled = entry.enabled;
            savedEntry.matchText = entry.matchText;
            savedEntries.add(savedEntry);
        }
        return savedEntries;
    }

    private void resetDangerousState() {
        swapEntries.clear();
        autoHealEnabled = false;
        autoHealHotbarSlot = 1;
        autoHealCooldownSeconds = 1.0D;
        autoHealHealthRatioThreshold = 0.75F;
        macroEnabled = false;
        macroToggleKeyCode = GLFW.GLFW_KEY_H;
        macroToggleScanCode = -1;
        macroStatusHudEnabled = false;
        macroStatusHudX = 8;
        macroStatusHudY = 8;
        extrasEnabled = false;
        ghostWallEditMode = false;
        extrasHudEnabled = false;
        extrasHudX = 8;
        extrasHudY = 24;
        selectedGhostBlockId = "minecraft:glass";
        favoriteGhostBlockIds.clear();
        defaultWeaponHotbarSlot = 0;
        chunkChestCounterEnabled = false;
        chunkChestOverlayX = 8;
        chunkChestOverlayY = 40;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private void normalizeGhostBlockSettings() {
        if (selectedGhostBlockId == null || selectedGhostBlockId.trim().isEmpty()) {
            selectedGhostBlockId = "minecraft:glass";
        } else {
            selectedGhostBlockId = selectedGhostBlockId.trim();
        }
        extrasHudX = Math.max(0, extrasHudX);
        extrasHudY = Math.max(0, extrasHudY);

        Set<String> seen = new HashSet<String>();
        for (int i = favoriteGhostBlockIds.size() - 1; i >= 0; i--) {
            String id = favoriteGhostBlockIds.get(i);
            if (id == null || id.trim().isEmpty()) {
                favoriteGhostBlockIds.remove(i);
                continue;
            }
            id = id.trim();
            if (seen.contains(id)) {
                favoriteGhostBlockIds.remove(i);
            } else {
                favoriteGhostBlockIds.set(i, id);
                seen.add(id);
            }
        }
    }

    public static final class SwapEntry {
        public String name = "New Macro";
        public double intervalSeconds = 5.0D;
        public int hotbarSlot = 0;
        public int holdTicks = 4;
        public int elapsedTicks = 0;

        public void normalize() {
            if (name == null || name.trim().isEmpty()) {
                name = "New Macro";
            } else {
                name = name.trim();
            }

            intervalSeconds = clamp(intervalSeconds, 0.1D, 3600.0D);
            hotbarSlot = clamp(hotbarSlot, 0, 8);
            holdTicks = clamp(holdTicks, 0, 200);
        }

        public void copyFrom(String newName, int newHotbarSlot, double newIntervalSeconds, int newHoldTicks) {
            name = newName;
            hotbarSlot = newHotbarSlot;
            intervalSeconds = newIntervalSeconds;
            holdTicks = newHoldTicks;
            normalize();
        }
    }

    public static final class HpAlertEntry {
        public boolean enabled = true;
        public int healthPercentage = 50;
        public int colorIndex = 0;
        public String titleText = "HP ALERT";
        public boolean triggered = false;

        public void normalize() {
            healthPercentage = clamp(healthPercentage, 1, 100);
            colorIndex = clamp(colorIndex, 0, TITLE_COLORS.length - 1);
            if (titleText == null || titleText.trim().isEmpty()) {
                titleText = "HP ALERT";
            } else {
                titleText = titleText.trim();
            }
        }
    }

    public static final class ManaAlertEntry {
        public boolean enabled = true;
        public int manaPercentage = 50;
        public int colorIndex = 0;
        public String titleText = "MANA ALERT";
        public boolean triggered = false;

        public void normalize() {
            manaPercentage = clamp(manaPercentage, 1, 100);
            colorIndex = clamp(colorIndex, 0, TITLE_COLORS.length - 1);
            if (titleText == null || titleText.trim().isEmpty()) {
                titleText = "MANA ALERT";
            } else {
                titleText = titleText.trim();
            }
        }
    }

    public static final class ChatFilterEntry {
        public boolean enabled = true;
        public String matchText = "";

        public void normalize() {
            if (matchText == null) {
                matchText = "";
            } else {
                matchText = matchText.trim();
            }
        }
    }

    private static final class SavedConfig {
        boolean autoHealEnabled;
        int autoHealHotbarSlot = 1;
        double autoHealCooldownSeconds = 1.0D;
        // Deprecated (kept only for backward compatibility with earlier config files).
        float autoHealHealthThreshold = 0.0F;
        float autoHealHealthRatioThreshold = 0.75F;
        boolean macroEnabled = true;
        int macroToggleKeyCode = GLFW.GLFW_KEY_H;
        int macroToggleScanCode = -1;
        boolean macroStatusHudEnabled = false;
        int macroStatusHudX = 8;
        int macroStatusHudY = 8;
        boolean extrasEnabled = true;
        boolean ghostWallEditMode = false;
        boolean extrasHudEnabled = false;
        int extrasHudX = 8;
        int extrasHudY = 24;
        String selectedGhostBlockId = "minecraft:glass";
        List<String> favoriteGhostBlockIds = new ArrayList<String>();
        int cameraToggleKeyCode = GLFW.GLFW_KEY_V;
        int cameraToggleScanCode = -1;
        int defaultWeaponHotbarSlot = 0;
        boolean itemLockEnabled = true;
        boolean soulbindProtectionEnabled = true;
        boolean chunkChestCounterEnabled = false;
        int chunkChestOverlayX = 8;
        int chunkChestOverlayY = 40;
        boolean chestSearchEnabled = false;
        String chestSearchQuery = "";
        int chestSearchKeyCode = GLFW.GLFW_KEY_UNKNOWN;
        int chestSearchScanCode = -1;
        boolean dropTrackerEnabled = false;
        String dropTrackerMode = HaDropTracker.MODE_ALL;
        int dropTrackerOverlayX = 8;
        int dropTrackerOverlayY = 72;
        boolean expTrackerEnabled = false;
        long expTrackerTotal = 0L;
        int expTrackerOverlayX = 8;
        int expTrackerOverlayY = 104;
        boolean chatFilterEnabled = false;
        Set<Integer> lockedSlotIds = new HashSet<Integer>();
        List<SavedSwapEntry> swapEntries = new ArrayList<SavedSwapEntry>();
        List<SavedHpAlertEntry> hpAlertEntries = new ArrayList<SavedHpAlertEntry>();
        List<SavedManaAlertEntry> manaAlertEntries = new ArrayList<SavedManaAlertEntry>();
        List<SavedChatFilterEntry> chatFilterEntries = new ArrayList<SavedChatFilterEntry>();
    }

    private static final class SavedSwapEntry {
        String name = "New Macro";
        int hotbarSlot = 0;
        double intervalSeconds = 5.0D;
        int holdTicks = 4;
    }

    private static final class SavedHpAlertEntry {
        boolean enabled = true;
        int healthPercentage = 50;
        int colorIndex = 0;
        String titleText = "HP ALERT";
    }

    private static final class SavedManaAlertEntry {
        boolean enabled = true;
        int manaPercentage = 50;
        int colorIndex = 0;
        String titleText = "MANA ALERT";
    }

    private static final class SavedChatFilterEntry {
        boolean enabled = true;
        String matchText = "";
    }

    public static final String[] TITLE_COLORS = new String[] {
        "\u00a7c",
        "\u00a74",
        "\u00a76",
        "\u00a7e",
        "\u00a7b",
        "\u00a7d",
        "\u00a7f"
    };

    public static final String[] TITLE_COLOR_NAMES = new String[] {
        "Red",
        "Dark Red",
        "Gold",
        "Yellow",
        "Aqua",
        "Light Purple",
        "White"
    };

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
