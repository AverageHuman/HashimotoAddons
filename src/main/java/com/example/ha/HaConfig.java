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
    public final List<DropNotifierEntry> dropNotifierEntries = new ArrayList<DropNotifierEntry>();
    public final List<ElementTrackerTargetEntry> elementTrackerTargets = new ArrayList<ElementTrackerTargetEntry>();
    public final List<ElementTrackerObservedCountEntry> elementTrackerObservedCounts = new ArrayList<ElementTrackerObservedCountEntry>();

    public boolean autoHealEnabled = false;
    public int autoHealHotbarSlot = 1;
    public double autoHealCooldownSeconds = 1.0D;
    // Trigger Auto Heal when (health / maxHealth) is <= this value.
    // Default 0.75 matches "15 hearts out of 20 hearts".
    public float autoHealHealthRatioThreshold = 0.75F;

    public boolean macroEnabled = true;
    public int macroToggleKeyCode = GLFW.GLFW_KEY_H;
    public int macroToggleScanCode = -1;
    public String macroToggleKeyType = "keysym";
    public boolean alchemyKilnAutomationEnabled = false;
    public int alchemyKilnAutomationKeyCode = GLFW.GLFW_KEY_UNKNOWN;
    public int alchemyKilnAutomationScanCode = -1;
    public String alchemyKilnAutomationKeyType = "keysym";
    public int alchemyKilnAutomationClickIntervalTicks = 4;
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
    public String cameraToggleKeyType = "keysym";
    public int defaultWeaponHotbarSlot = 0;
    public boolean itemLockEnabled = true;
    public boolean soulbindProtectionEnabled = true;
    public boolean chunkChestCounterEnabled = false;
    public int chunkChestOverlayX = 8;
    public int chunkChestOverlayY = 40;
    public boolean damageTruncationEnabled = false;
    public boolean elementRarityEnabled = true;
    public boolean gearViewEnabled = true;
    public int gearViewKeyCode = GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
    public int gearViewKeyScanCode = -1;
    public String gearViewKeyType = "mouse";
    public boolean mobEspEnabled = false;
    public String mobEspTargetName = "";
    public boolean afkFarmingEnabled = false;
    public boolean afkFarmingActive = false;
    public String afkFarmingWebhookUrl = "";
    public double afkFarmingReportIntervalMinutes = 5.0D;
    public boolean afkFarmingPlayerAlertsEnabled = true;
    public boolean afkFarmingKeyAdminAlertsEnabled = true;
    public String afkFarmingKeyAdminName = "KeyAdmin";
    public boolean afkFarmingMobMacroEnabled = false;
    public boolean afkFarmingMobCircleVisible = true;
    public boolean afkFarmingMobDebugHudEnabled = true;
    public int afkFarmingMobMacroIndex = 0;
    public int afkFarmingMobMinCount = 3;
    public int afkFarmingMobMaxCount = 5;
    public double afkFarmingMobMacroCooldownSeconds = 5.0D;
    public boolean afkFarmingAutoMoveEnabled = false;
    public double afkFarmingAutoMoveIntervalSeconds = 300.0D;
    public double afkFarmingAutoMoveIntervalMinutes = 5.0D;
    public double afkFarmingAutoMoveJitterSeconds = 10.0D;
    public boolean chestSearchEnabled = false;
    public String chestSearchQuery = "";
    public int chestSearchKeyCode = GLFW.GLFW_KEY_UNKNOWN;
    public int chestSearchScanCode = -1;
    public String chestSearchKeyType = "keysym";
    public boolean evolutionForgeHelperEnabled = true;
    public boolean dropTrackerEnabled = false;
    public String dropTrackerMode = HaDropTracker.MODE_ALL;
    public long dropTrackerElapsedSeconds = 0L;
    public boolean dropTrackerShowTimer = false;
    public boolean dropTrackerShowHourlyProfit = false;
    public boolean dropTrackerCompactNumbers = false;
    public boolean dropTrackerContinueAfterStart = false;
    public int dropTrackerOverlayX = 8;
    public int dropTrackerOverlayY = 72;
    public boolean dropNotifierEnabled = false;
    public boolean dropNotifierContinueAfterStart = false;
    public boolean expTrackerEnabled = false;
    public long expTrackerTotalTenths = 0L;
    // Deprecated mirror kept only for backward compatibility with older config files.
    public long expTrackerTotal = 0L;
    public long expTrackerElapsedSeconds = 0L;
    public boolean expTrackerShowTimer = false;
    public boolean expTrackerShowHourlyRate = false;
    public boolean expTrackerCompactNumbers = false;
    public boolean expTrackerContinueAfterStart = false;
    public int expTrackerOverlayX = 8;
    public int expTrackerOverlayY = 104;
    public boolean elementTrackerEnabled = false;
    public long elementTrackerElapsedSeconds = 0L;
    public boolean elementTrackerShowTimer = false;
    public boolean elementTrackerContinueAfterStart = false;
    public int elementTrackerOverlayX = 8;
    public int elementTrackerOverlayY = 168;
    public boolean mobHpDisplayEnabled = false;
    public String mobHpDisplayPosition = "hud";
    public boolean mobHpDisplaySlim = false;
    public boolean mobHpDisplayShowPercentage = true;
    public boolean mobHpDisplayCompactNumbers = false;
    public int mobHpDisplayOverlayX = 8;
    public int mobHpDisplayOverlayY = 136;
    public boolean subSkillTimerEnabled = false;
    public boolean subSkillTimerSlim = false;
    public double subSkillTimerCooldownSeconds = 10.0D;
    public int subSkillTimerOverlayX = 8;
    public int subSkillTimerOverlayY = 200;
    public boolean ritualBookTimerEnabled = false;
    public boolean ritualBookTimerSlim = false;
    public int ritualBookTimerSoundVolume = 100;
    public int ritualBookTimerOverlayX = 8;
    public int ritualBookTimerOverlayY = 232;
    public boolean spotifyEnabled = false;
    public boolean spotifyChromeDetectionEnabled = false;
    public int spotifyOverlayX = 8;
    public int spotifyOverlayY = 264;
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
        for (int i = dropNotifierEntries.size() - 1; i >= 0; i--) {
            DropNotifierEntry entry = dropNotifierEntries.get(i);
            entry.normalize();
            if (entry.matchText.isEmpty()) {
                dropNotifierEntries.remove(i);
            }
        }
        normalizeGhostBlockSettings();

        autoHealHotbarSlot = clamp(autoHealHotbarSlot, 0, 8);
        defaultWeaponHotbarSlot = clamp(defaultWeaponHotbarSlot, 0, 8);
        alchemyKilnAutomationClickIntervalTicks = clamp(alchemyKilnAutomationClickIntervalTicks, 4, 200);
        macroStatusHudX = Math.max(0, macroStatusHudX);
        macroStatusHudY = Math.max(0, macroStatusHudY);
        autoHealCooldownSeconds = clamp(autoHealCooldownSeconds, 0.05D, 60.0D);
        autoHealHealthRatioThreshold = (float) clamp(autoHealHealthRatioThreshold, 0.05D, 1.0D);
        chunkChestOverlayX = Math.max(0, chunkChestOverlayX);
        chunkChestOverlayY = Math.max(0, chunkChestOverlayY);
        if (mobEspTargetName == null) {
            mobEspTargetName = "";
        } else {
            mobEspTargetName = mobEspTargetName.trim();
        }
        if (afkFarmingWebhookUrl == null) {
            afkFarmingWebhookUrl = "";
        } else {
            afkFarmingWebhookUrl = afkFarmingWebhookUrl.trim();
        }
        if (afkFarmingKeyAdminName == null || afkFarmingKeyAdminName.trim().isEmpty()) {
            afkFarmingKeyAdminName = "KeyAdmin";
        } else {
            afkFarmingKeyAdminName = afkFarmingKeyAdminName.trim();
        }
        if (!afkFarmingEnabled) {
            afkFarmingActive = false;
        }
        afkFarmingReportIntervalMinutes = clamp(afkFarmingReportIntervalMinutes, 0.1D, 1440.0D);
        afkFarmingMobMacroIndex = Math.max(0, afkFarmingMobMacroIndex);
        afkFarmingMobMinCount = clamp(afkFarmingMobMinCount, 1, 200);
        afkFarmingMobMaxCount = clamp(afkFarmingMobMaxCount, 1, 200);
        if (afkFarmingMobMaxCount < afkFarmingMobMinCount) {
            afkFarmingMobMaxCount = afkFarmingMobMinCount;
        }
        afkFarmingMobMacroCooldownSeconds = clamp(afkFarmingMobMacroCooldownSeconds, 0.1D, 3600.0D);
        afkFarmingAutoMoveIntervalSeconds = clamp(afkFarmingAutoMoveIntervalSeconds, 1.0D, 86400.0D);
        afkFarmingAutoMoveIntervalMinutes = afkFarmingAutoMoveIntervalSeconds / 60.0D;
        afkFarmingAutoMoveJitterSeconds = clamp(afkFarmingAutoMoveJitterSeconds, 0.0D, 300.0D);
        if (chestSearchQuery == null) {
            chestSearchQuery = "";
        } else {
            chestSearchQuery = chestSearchQuery.trim();
        }
        dropTrackerMode = HaDropTracker.normalizeMode(dropTrackerMode);
        dropTrackerElapsedSeconds = Math.max(0L, dropTrackerElapsedSeconds);
        dropTrackerOverlayX = Math.max(0, dropTrackerOverlayX);
        dropTrackerOverlayY = Math.max(0, dropTrackerOverlayY);
        expTrackerTotalTenths = Math.max(0L, expTrackerTotalTenths);
        expTrackerTotal = Math.max(0L, expTrackerTotal);
        if (expTrackerTotalTenths <= 0L && expTrackerTotal > 0L) {
            expTrackerTotalTenths = safeMultiplyByTen(expTrackerTotal);
        }
        expTrackerTotal = expTrackerTotalTenths / 10L;
        expTrackerElapsedSeconds = Math.max(0L, expTrackerElapsedSeconds);
        expTrackerOverlayX = Math.max(0, expTrackerOverlayX);
        expTrackerOverlayY = Math.max(0, expTrackerOverlayY);
        elementTrackerElapsedSeconds = Math.max(0L, elementTrackerElapsedSeconds);
        elementTrackerOverlayX = Math.max(0, elementTrackerOverlayX);
        elementTrackerOverlayY = Math.max(0, elementTrackerOverlayY);
        normalizeElementTrackerSettings();
        mobHpDisplayPosition = HaMobHpDisplayOverlay.normalizePosition(mobHpDisplayPosition);
        mobHpDisplayOverlayX = Math.max(0, mobHpDisplayOverlayX);
        mobHpDisplayOverlayY = Math.max(0, mobHpDisplayOverlayY);
        subSkillTimerOverlayX = Math.max(0, subSkillTimerOverlayX);
        subSkillTimerOverlayY = Math.max(0, subSkillTimerOverlayY);
        subSkillTimerCooldownSeconds = clamp(subSkillTimerCooldownSeconds, 0.1D, 3600.0D);
        ritualBookTimerSoundVolume = clamp(ritualBookTimerSoundVolume, 0, 100);
        ritualBookTimerOverlayX = Math.max(0, ritualBookTimerOverlayX);
        ritualBookTimerOverlayY = Math.max(0, ritualBookTimerOverlayY);
        spotifyOverlayX = Math.max(0, spotifyOverlayX);
        spotifyOverlayY = Math.max(0, spotifyOverlayY);
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

    public DropNotifierEntry addDropNotifierEntry() {
        DropNotifierEntry entry = new DropNotifierEntry();
        dropNotifierEntries.add(entry);
        return entry;
    }

    public void removeDropNotifierEntry(int index) {
        if (index >= 0 && index < dropNotifierEntries.size()) {
            dropNotifierEntries.remove(index);
        }
    }

    public ElementTrackerTargetEntry getOrCreateElementTrackerTarget(String elementKey) {
        String normalizedKey = normalizeElementTrackerKey(elementKey);
        for (ElementTrackerTargetEntry entry : elementTrackerTargets) {
            if (normalizedKey.equals(entry.elementKey)) {
                entry.normalize();
                return entry;
            }
        }
        ElementTrackerTargetEntry entry = new ElementTrackerTargetEntry();
        entry.elementKey = normalizedKey;
        entry.normalize();
        elementTrackerTargets.add(entry);
        return entry;
    }

    public ElementTrackerObservedCountEntry getOrCreateElementTrackerObservedCounts(String elementKey) {
        String normalizedKey = normalizeElementTrackerKey(elementKey);
        for (ElementTrackerObservedCountEntry entry : elementTrackerObservedCounts) {
            if (normalizedKey.equals(entry.elementKey)) {
                entry.normalize();
                return entry;
            }
        }
        ElementTrackerObservedCountEntry entry = new ElementTrackerObservedCountEntry();
        entry.elementKey = normalizedKey;
        entry.normalize();
        elementTrackerObservedCounts.add(entry);
        return entry;
    }

    public InputUtil.Key getMacroToggleKey() {
        return getBoundKey(macroToggleKeyType, macroToggleKeyCode, macroToggleScanCode);
    }

    public InputUtil.Key getAlchemyKilnAutomationKey() {
        return getBoundKey(alchemyKilnAutomationKeyType, alchemyKilnAutomationKeyCode, alchemyKilnAutomationScanCode);
    }

    public InputUtil.Key getCameraToggleKey() {
        return getBoundKey(cameraToggleKeyType, cameraToggleKeyCode, cameraToggleScanCode);
    }

    public InputUtil.Key getChestSearchKey() {
        return getBoundKey(chestSearchKeyType, chestSearchKeyCode, chestSearchScanCode);
    }

    public InputUtil.Key getGearViewKey() {
        return getBoundKey(gearViewKeyType, gearViewKeyCode, gearViewKeyScanCode);
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
        dropNotifierEntries.clear();
        elementTrackerTargets.clear();
        elementTrackerObservedCounts.clear();
        if (saved == null) {
            return;
        }

        cameraToggleKeyCode = saved.cameraToggleKeyCode;
        cameraToggleScanCode = saved.cameraToggleScanCode;
        cameraToggleKeyType = saved.cameraToggleKeyType;
        itemLockEnabled = saved.itemLockEnabled;
        soulbindProtectionEnabled = saved.soulbindProtectionEnabled;
        chestSearchEnabled = saved.chestSearchEnabled;
        chestSearchQuery = saved.chestSearchQuery;
        chestSearchKeyCode = saved.chestSearchKeyCode;
        chestSearchScanCode = saved.chestSearchScanCode;
        chestSearchKeyType = saved.chestSearchKeyType;
        evolutionForgeHelperEnabled = saved.evolutionForgeHelperEnabled;
        dropTrackerEnabled = saved.dropTrackerEnabled;
        dropTrackerMode = saved.dropTrackerMode;
        dropTrackerElapsedSeconds = saved.dropTrackerElapsedSeconds;
        dropTrackerShowTimer = saved.dropTrackerShowTimer;
        dropTrackerShowHourlyProfit = saved.dropTrackerShowHourlyProfit;
        dropTrackerCompactNumbers = saved.dropTrackerCompactNumbers;
        dropTrackerContinueAfterStart = saved.dropTrackerContinueAfterStart;
        dropTrackerOverlayX = saved.dropTrackerOverlayX;
        dropTrackerOverlayY = saved.dropTrackerOverlayY;
        dropNotifierEnabled = saved.dropNotifierEnabled;
        dropNotifierContinueAfterStart = saved.dropNotifierContinueAfterStart;
        expTrackerEnabled = saved.expTrackerEnabled;
        expTrackerTotalTenths = saved.expTrackerTotalTenths > 0L
            ? saved.expTrackerTotalTenths
            : safeMultiplyByTen(saved.expTrackerTotal);
        expTrackerTotal = expTrackerTotalTenths / 10L;
        expTrackerElapsedSeconds = saved.expTrackerElapsedSeconds;
        expTrackerShowTimer = saved.expTrackerShowTimer;
        expTrackerShowHourlyRate = saved.expTrackerShowHourlyRate;
        expTrackerCompactNumbers = saved.expTrackerCompactNumbers;
        expTrackerContinueAfterStart = saved.expTrackerContinueAfterStart;
        expTrackerOverlayX = saved.expTrackerOverlayX;
        expTrackerOverlayY = saved.expTrackerOverlayY;
        elementTrackerEnabled = saved.elementTrackerEnabled;
        elementTrackerElapsedSeconds = saved.elementTrackerElapsedSeconds;
        elementTrackerShowTimer = saved.elementTrackerShowTimer;
        elementTrackerContinueAfterStart = saved.elementTrackerContinueAfterStart;
        elementTrackerOverlayX = saved.elementTrackerOverlayX;
        elementTrackerOverlayY = saved.elementTrackerOverlayY;
        mobHpDisplayEnabled = saved.mobHpDisplayEnabled;
        mobHpDisplayPosition = saved.mobHpDisplayPosition;
        mobHpDisplaySlim = saved.mobHpDisplaySlim;
        mobHpDisplayShowPercentage = saved.mobHpDisplayShowPercentage;
        mobHpDisplayCompactNumbers = saved.mobHpDisplayCompactNumbers;
        mobHpDisplayOverlayX = saved.mobHpDisplayOverlayX;
        mobHpDisplayOverlayY = saved.mobHpDisplayOverlayY;
        subSkillTimerEnabled = saved.subSkillTimerEnabled;
        subSkillTimerSlim = saved.subSkillTimerSlim;
        subSkillTimerCooldownSeconds = saved.subSkillTimerCooldownSeconds > 0.0D ? saved.subSkillTimerCooldownSeconds : 10.0D;
        subSkillTimerOverlayX = saved.subSkillTimerOverlayX;
        subSkillTimerOverlayY = saved.subSkillTimerOverlayY;
        ritualBookTimerEnabled = saved.ritualBookTimerEnabled;
        ritualBookTimerSlim = saved.ritualBookTimerSlim;
        ritualBookTimerSoundVolume = saved.ritualBookTimerSoundVolume;
        ritualBookTimerOverlayX = saved.ritualBookTimerOverlayX;
        ritualBookTimerOverlayY = saved.ritualBookTimerOverlayY;
        spotifyEnabled = saved.spotifyEnabled;
        spotifyChromeDetectionEnabled = saved.spotifyChromeDetectionEnabled;
        spotifyOverlayX = saved.spotifyOverlayX;
        spotifyOverlayY = saved.spotifyOverlayY;
        chatFilterEnabled = saved.chatFilterEnabled;
        lockedSlotIds = saved.lockedSlotIds != null ? new HashSet<Integer>(saved.lockedSlotIds) : new HashSet<Integer>();
        if (saved.elementTrackerTargets != null) {
            for (SavedElementTrackerTargetEntry savedEntry : saved.elementTrackerTargets) {
                ElementTrackerTargetEntry entry = new ElementTrackerTargetEntry();
                if (savedEntry != null) {
                    entry.elementKey = savedEntry.elementKey;
                    entry.enabled = savedEntry.enabled;
                    entry.targetRank = savedEntry.targetRank;
                }
                entry.normalize();
                elementTrackerTargets.add(entry);
            }
        }
        if (saved.elementTrackerObservedCounts != null) {
            for (SavedElementTrackerObservedCountEntry savedEntry : saved.elementTrackerObservedCounts) {
                ElementTrackerObservedCountEntry entry = new ElementTrackerObservedCountEntry();
                if (savedEntry != null) {
                    entry.elementKey = savedEntry.elementKey;
                    entry.commonCount = savedEntry.commonCount;
                    entry.rareCount = savedEntry.rareCount;
                    entry.superiorCount = savedEntry.superiorCount;
                    entry.epicCount = savedEntry.epicCount;
                    entry.legendaryCount = savedEntry.legendaryCount;
                    entry.transcendentCount = savedEntry.transcendentCount;
                    entry.untouchableCount = savedEntry.untouchableCount;
                    entry.uniqueCount = savedEntry.uniqueCount;
                }
                entry.normalize();
                elementTrackerObservedCounts.add(entry);
            }
        }

        if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            autoHealEnabled = saved.autoHealEnabled;
            autoHealHotbarSlot = saved.autoHealHotbarSlot;
            autoHealCooldownSeconds = saved.autoHealCooldownSeconds > 0.0D ? saved.autoHealCooldownSeconds : 1.0D;
            autoHealHealthRatioThreshold = saved.autoHealHealthRatioThreshold > 0.0F ? saved.autoHealHealthRatioThreshold : 0.75F;
            macroEnabled = saved.macroEnabled;
            macroToggleKeyCode = saved.macroToggleKeyCode;
            macroToggleScanCode = saved.macroToggleScanCode;
            macroToggleKeyType = saved.macroToggleKeyType;
            alchemyKilnAutomationEnabled = saved.alchemyKilnAutomationEnabled;
            alchemyKilnAutomationClickIntervalTicks = saved.alchemyKilnAutomationClickIntervalTicks > 0 ? saved.alchemyKilnAutomationClickIntervalTicks : 4;
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
        damageTruncationEnabled = saved.damageTruncationEnabled;
        elementRarityEnabled = saved.elementRarityEnabled;
        gearViewEnabled = saved.gearViewEnabled;
        gearViewKeyCode = saved.gearViewKeyCode;
        gearViewKeyScanCode = saved.gearViewKeyScanCode;
        gearViewKeyType = saved.gearViewKeyType;
        alchemyKilnAutomationKeyCode = saved.alchemyKilnAutomationKeyCode;
        alchemyKilnAutomationScanCode = saved.alchemyKilnAutomationScanCode;
        alchemyKilnAutomationKeyType = saved.alchemyKilnAutomationKeyType;
        mobEspEnabled = saved.mobEspEnabled;
            mobEspTargetName = saved.mobEspTargetName;
            afkFarmingEnabled = saved.afkFarmingEnabled;
            afkFarmingActive = saved.afkFarmingActive;
            afkFarmingWebhookUrl = saved.afkFarmingWebhookUrl;
            afkFarmingReportIntervalMinutes = saved.afkFarmingReportIntervalMinutes > 0.0D ? saved.afkFarmingReportIntervalMinutes : 5.0D;
            afkFarmingPlayerAlertsEnabled = saved.afkFarmingPlayerAlertsEnabled;
            afkFarmingKeyAdminAlertsEnabled = saved.afkFarmingKeyAdminAlertsEnabled;
            afkFarmingKeyAdminName = saved.afkFarmingKeyAdminName;
            afkFarmingMobMacroEnabled = saved.afkFarmingMobMacroEnabled;
            afkFarmingMobCircleVisible = saved.afkFarmingMobCircleVisible;
            afkFarmingMobDebugHudEnabled = saved.afkFarmingMobDebugHudEnabled;
            afkFarmingMobMacroIndex = saved.afkFarmingMobMacroIndex;
            afkFarmingMobMinCount = saved.afkFarmingMobMinCount > 0 ? saved.afkFarmingMobMinCount : 3;
            afkFarmingMobMaxCount = saved.afkFarmingMobMaxCount > 0 ? saved.afkFarmingMobMaxCount : 5;
            afkFarmingMobMacroCooldownSeconds = saved.afkFarmingMobMacroCooldownSeconds > 0.0D ? saved.afkFarmingMobMacroCooldownSeconds : 5.0D;
            afkFarmingAutoMoveEnabled = saved.afkFarmingAutoMoveEnabled;
            if (saved.afkFarmingAutoMoveIntervalSeconds > 0.0D) {
                afkFarmingAutoMoveIntervalSeconds = saved.afkFarmingAutoMoveIntervalSeconds;
            } else if (saved.afkFarmingAutoMoveIntervalMinutes > 0.0D) {
                afkFarmingAutoMoveIntervalSeconds = saved.afkFarmingAutoMoveIntervalMinutes * 60.0D;
            } else {
                afkFarmingAutoMoveIntervalSeconds = 300.0D;
            }
            afkFarmingAutoMoveIntervalMinutes = afkFarmingAutoMoveIntervalSeconds / 60.0D;
            afkFarmingAutoMoveJitterSeconds = saved.afkFarmingAutoMoveJitterSeconds >= 0.0D ? saved.afkFarmingAutoMoveJitterSeconds : 10.0D;

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

        if (saved.dropNotifierEntries != null) {
            for (SavedDropNotifierEntry savedEntry : saved.dropNotifierEntries) {
                DropNotifierEntry entry = new DropNotifierEntry();
                if (savedEntry != null) {
                    entry.enabled = savedEntry.enabled;
                    entry.matchText = savedEntry.matchText;
                }
                entry.normalize();
                if (!entry.matchText.isEmpty()) {
                    dropNotifierEntries.add(entry);
                }
            }
        }
    }

    private JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("cameraToggleKeyCode", cameraToggleKeyCode);
        root.addProperty("cameraToggleScanCode", cameraToggleScanCode);
        root.addProperty("cameraToggleKeyType", cameraToggleKeyType);
        root.addProperty("itemLockEnabled", itemLockEnabled);
        root.addProperty("soulbindProtectionEnabled", soulbindProtectionEnabled);
        root.addProperty("chestSearchEnabled", chestSearchEnabled);
        root.addProperty("chestSearchQuery", chestSearchQuery);
        root.addProperty("chestSearchKeyCode", chestSearchKeyCode);
        root.addProperty("chestSearchScanCode", chestSearchScanCode);
        root.addProperty("chestSearchKeyType", chestSearchKeyType);
        root.addProperty("evolutionForgeHelperEnabled", evolutionForgeHelperEnabled);
        root.addProperty("dropTrackerEnabled", dropTrackerEnabled);
        root.addProperty("dropTrackerMode", dropTrackerMode);
        root.addProperty("dropTrackerElapsedSeconds", dropTrackerElapsedSeconds);
        root.addProperty("dropTrackerShowTimer", dropTrackerShowTimer);
        root.addProperty("dropTrackerShowHourlyProfit", dropTrackerShowHourlyProfit);
        root.addProperty("dropTrackerCompactNumbers", dropTrackerCompactNumbers);
        root.addProperty("dropTrackerContinueAfterStart", dropTrackerContinueAfterStart);
        root.addProperty("dropTrackerOverlayX", dropTrackerOverlayX);
        root.addProperty("dropTrackerOverlayY", dropTrackerOverlayY);
        root.addProperty("dropNotifierEnabled", dropNotifierEnabled);
        root.addProperty("dropNotifierContinueAfterStart", dropNotifierContinueAfterStart);
        root.addProperty("expTrackerEnabled", expTrackerEnabled);
        root.addProperty("expTrackerTotalTenths", expTrackerTotalTenths);
        root.addProperty("expTrackerTotal", expTrackerTotalTenths / 10L);
        root.addProperty("expTrackerElapsedSeconds", expTrackerElapsedSeconds);
        root.addProperty("expTrackerShowTimer", expTrackerShowTimer);
        root.addProperty("expTrackerShowHourlyRate", expTrackerShowHourlyRate);
        root.addProperty("expTrackerCompactNumbers", expTrackerCompactNumbers);
        root.addProperty("expTrackerContinueAfterStart", expTrackerContinueAfterStart);
        root.addProperty("expTrackerOverlayX", expTrackerOverlayX);
        root.addProperty("expTrackerOverlayY", expTrackerOverlayY);
        root.addProperty("elementTrackerEnabled", elementTrackerEnabled);
        root.addProperty("elementTrackerElapsedSeconds", elementTrackerElapsedSeconds);
        root.addProperty("elementTrackerShowTimer", elementTrackerShowTimer);
        root.addProperty("elementTrackerContinueAfterStart", elementTrackerContinueAfterStart);
        root.addProperty("elementTrackerOverlayX", elementTrackerOverlayX);
        root.addProperty("elementTrackerOverlayY", elementTrackerOverlayY);
        root.addProperty("mobHpDisplayEnabled", mobHpDisplayEnabled);
        root.addProperty("mobHpDisplayPosition", mobHpDisplayPosition);
        root.addProperty("mobHpDisplaySlim", mobHpDisplaySlim);
        root.addProperty("mobHpDisplayShowPercentage", mobHpDisplayShowPercentage);
        root.addProperty("mobHpDisplayCompactNumbers", mobHpDisplayCompactNumbers);
        root.addProperty("mobHpDisplayOverlayX", mobHpDisplayOverlayX);
        root.addProperty("mobHpDisplayOverlayY", mobHpDisplayOverlayY);
        root.addProperty("subSkillTimerEnabled", subSkillTimerEnabled);
        root.addProperty("subSkillTimerSlim", subSkillTimerSlim);
        root.addProperty("subSkillTimerCooldownSeconds", subSkillTimerCooldownSeconds);
        root.addProperty("subSkillTimerOverlayX", subSkillTimerOverlayX);
        root.addProperty("subSkillTimerOverlayY", subSkillTimerOverlayY);
        root.addProperty("ritualBookTimerEnabled", ritualBookTimerEnabled);
        root.addProperty("ritualBookTimerSlim", ritualBookTimerSlim);
        root.addProperty("ritualBookTimerSoundVolume", ritualBookTimerSoundVolume);
        root.addProperty("ritualBookTimerOverlayX", ritualBookTimerOverlayX);
        root.addProperty("ritualBookTimerOverlayY", ritualBookTimerOverlayY);
        root.addProperty("spotifyEnabled", spotifyEnabled);
        root.addProperty("spotifyChromeDetectionEnabled", spotifyChromeDetectionEnabled);
        root.addProperty("spotifyOverlayX", spotifyOverlayX);
        root.addProperty("spotifyOverlayY", spotifyOverlayY);
        root.addProperty("chatFilterEnabled", chatFilterEnabled);
        root.add("lockedSlotIds", GSON.toJsonTree(new HashSet<Integer>(lockedSlotIds)));
        root.add("hpAlertEntries", GSON.toJsonTree(toSavedHpAlertEntries()));
        root.add("manaAlertEntries", GSON.toJsonTree(toSavedManaAlertEntries()));
        root.add("chatFilterEntries", GSON.toJsonTree(toSavedChatFilterEntries()));
        root.add("dropNotifierEntries", GSON.toJsonTree(toSavedDropNotifierEntries()));
        root.add("elementTrackerTargets", GSON.toJsonTree(toSavedElementTrackerTargets()));
        root.add("elementTrackerObservedCounts", GSON.toJsonTree(toSavedElementTrackerObservedCounts()));

        if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            root.addProperty("autoHealEnabled", autoHealEnabled);
            root.addProperty("autoHealHotbarSlot", autoHealHotbarSlot);
            root.addProperty("autoHealCooldownSeconds", autoHealCooldownSeconds);
            root.addProperty("autoHealHealthRatioThreshold", autoHealHealthRatioThreshold);
            root.addProperty("macroEnabled", macroEnabled);
            root.addProperty("macroToggleKeyCode", macroToggleKeyCode);
            root.addProperty("macroToggleScanCode", macroToggleScanCode);
            root.addProperty("macroToggleKeyType", macroToggleKeyType);
            root.addProperty("alchemyKilnAutomationEnabled", alchemyKilnAutomationEnabled);
            root.addProperty("alchemyKilnAutomationClickIntervalTicks", alchemyKilnAutomationClickIntervalTicks);
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
        root.addProperty("damageTruncationEnabled", damageTruncationEnabled);
        root.addProperty("elementRarityEnabled", elementRarityEnabled);
        root.addProperty("gearViewEnabled", gearViewEnabled);
        root.addProperty("gearViewKeyCode", gearViewKeyCode);
        root.addProperty("gearViewKeyScanCode", gearViewKeyScanCode);
        root.addProperty("gearViewKeyType", gearViewKeyType);
        root.addProperty("alchemyKilnAutomationKeyCode", alchemyKilnAutomationKeyCode);
        root.addProperty("alchemyKilnAutomationScanCode", alchemyKilnAutomationScanCode);
        root.addProperty("alchemyKilnAutomationKeyType", alchemyKilnAutomationKeyType);
        root.addProperty("mobEspEnabled", mobEspEnabled);
            root.addProperty("mobEspTargetName", mobEspTargetName);
            root.addProperty("afkFarmingEnabled", afkFarmingEnabled);
            root.addProperty("afkFarmingActive", afkFarmingActive);
            root.addProperty("afkFarmingWebhookUrl", afkFarmingWebhookUrl);
            root.addProperty("afkFarmingReportIntervalMinutes", afkFarmingReportIntervalMinutes);
            root.addProperty("afkFarmingPlayerAlertsEnabled", afkFarmingPlayerAlertsEnabled);
            root.addProperty("afkFarmingKeyAdminAlertsEnabled", afkFarmingKeyAdminAlertsEnabled);
            root.addProperty("afkFarmingKeyAdminName", afkFarmingKeyAdminName);
            root.addProperty("afkFarmingMobMacroEnabled", afkFarmingMobMacroEnabled);
            root.addProperty("afkFarmingMobCircleVisible", afkFarmingMobCircleVisible);
            root.addProperty("afkFarmingMobDebugHudEnabled", afkFarmingMobDebugHudEnabled);
            root.addProperty("afkFarmingMobMacroIndex", afkFarmingMobMacroIndex);
            root.addProperty("afkFarmingMobMinCount", afkFarmingMobMinCount);
            root.addProperty("afkFarmingMobMaxCount", afkFarmingMobMaxCount);
            root.addProperty("afkFarmingMobMacroCooldownSeconds", afkFarmingMobMacroCooldownSeconds);
            root.addProperty("afkFarmingAutoMoveEnabled", afkFarmingAutoMoveEnabled);
            root.addProperty("afkFarmingAutoMoveIntervalSeconds", afkFarmingAutoMoveIntervalSeconds);
            root.addProperty("afkFarmingAutoMoveIntervalMinutes", afkFarmingAutoMoveIntervalSeconds / 60.0D);
            root.addProperty("afkFarmingAutoMoveJitterSeconds", afkFarmingAutoMoveJitterSeconds);
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

    private List<SavedDropNotifierEntry> toSavedDropNotifierEntries() {
        List<SavedDropNotifierEntry> savedEntries = new ArrayList<SavedDropNotifierEntry>();
        for (DropNotifierEntry entry : dropNotifierEntries) {
            if (entry.matchText == null || entry.matchText.trim().isEmpty()) {
                continue;
            }
            SavedDropNotifierEntry savedEntry = new SavedDropNotifierEntry();
            savedEntry.enabled = entry.enabled;
            savedEntry.matchText = entry.matchText;
            savedEntries.add(savedEntry);
        }
        return savedEntries;
    }

    private List<SavedElementTrackerTargetEntry> toSavedElementTrackerTargets() {
        List<SavedElementTrackerTargetEntry> savedEntries = new ArrayList<SavedElementTrackerTargetEntry>();
        for (ElementTrackerTargetEntry entry : elementTrackerTargets) {
            SavedElementTrackerTargetEntry savedEntry = new SavedElementTrackerTargetEntry();
            savedEntry.elementKey = entry.elementKey;
            savedEntry.enabled = entry.enabled;
            savedEntry.targetRank = entry.targetRank;
            savedEntries.add(savedEntry);
        }
        return savedEntries;
    }

    private List<SavedElementTrackerObservedCountEntry> toSavedElementTrackerObservedCounts() {
        List<SavedElementTrackerObservedCountEntry> savedEntries = new ArrayList<SavedElementTrackerObservedCountEntry>();
        for (ElementTrackerObservedCountEntry entry : elementTrackerObservedCounts) {
            SavedElementTrackerObservedCountEntry savedEntry = new SavedElementTrackerObservedCountEntry();
            savedEntry.elementKey = entry.elementKey;
            savedEntry.commonCount = entry.commonCount;
            savedEntry.rareCount = entry.rareCount;
            savedEntry.superiorCount = entry.superiorCount;
            savedEntry.epicCount = entry.epicCount;
            savedEntry.legendaryCount = entry.legendaryCount;
            savedEntry.transcendentCount = entry.transcendentCount;
            savedEntry.untouchableCount = entry.untouchableCount;
            savedEntry.uniqueCount = entry.uniqueCount;
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
        macroToggleKeyType = "keysym";
        alchemyKilnAutomationEnabled = false;
        alchemyKilnAutomationKeyCode = GLFW.GLFW_KEY_UNKNOWN;
        alchemyKilnAutomationScanCode = -1;
        alchemyKilnAutomationKeyType = "keysym";
        alchemyKilnAutomationClickIntervalTicks = 4;
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
        damageTruncationEnabled = false;
        elementRarityEnabled = true;
        gearViewEnabled = true;
        gearViewKeyCode = GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
        gearViewKeyScanCode = -1;
        gearViewKeyType = "mouse";
        mobEspEnabled = false;
        mobEspTargetName = "";
        afkFarmingEnabled = false;
        afkFarmingActive = false;
        afkFarmingWebhookUrl = "";
        afkFarmingReportIntervalMinutes = 5.0D;
        afkFarmingPlayerAlertsEnabled = true;
        afkFarmingKeyAdminAlertsEnabled = true;
        afkFarmingKeyAdminName = "KeyAdmin";
        afkFarmingMobMacroEnabled = false;
        afkFarmingMobCircleVisible = true;
        afkFarmingMobDebugHudEnabled = true;
        afkFarmingMobMacroIndex = 0;
        afkFarmingMobMinCount = 3;
        afkFarmingMobMaxCount = 5;
        afkFarmingMobMacroCooldownSeconds = 5.0D;
        afkFarmingAutoMoveEnabled = false;
        afkFarmingAutoMoveIntervalSeconds = 300.0D;
        afkFarmingAutoMoveIntervalMinutes = 5.0D;
        afkFarmingAutoMoveJitterSeconds = 10.0D;
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

    public static final class DropNotifierEntry {
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

    public static final class ElementTrackerTargetEntry {
        public String elementKey = "";
        public boolean enabled = false;
        public String targetRank = HaElementTracker.ElementRank.LEGENDARY.getKey();

        public void normalize() {
            elementKey = normalizeElementTrackerKey(elementKey);
            targetRank = normalizeElementTrackerRank(targetRank);
        }
    }

    public static final class ElementTrackerObservedCountEntry {
        public String elementKey = "";
        public long commonCount = 0L;
        public long rareCount = 0L;
        public long superiorCount = 0L;
        public long epicCount = 0L;
        public long legendaryCount = 0L;
        public long transcendentCount = 0L;
        public long untouchableCount = 0L;
        public long uniqueCount = 0L;

        public void normalize() {
            elementKey = normalizeElementTrackerKey(elementKey);
            commonCount = Math.max(0L, commonCount);
            rareCount = Math.max(0L, rareCount);
            superiorCount = Math.max(0L, superiorCount);
            epicCount = Math.max(0L, epicCount);
            legendaryCount = Math.max(0L, legendaryCount);
            transcendentCount = Math.max(0L, transcendentCount);
            untouchableCount = Math.max(0L, untouchableCount);
            uniqueCount = Math.max(0L, uniqueCount);
        }
    }

    private void normalizeElementTrackerSettings() {
        for (int i = elementTrackerTargets.size() - 1; i >= 0; i--) {
            ElementTrackerTargetEntry entry = elementTrackerTargets.get(i);
            if (entry == null) {
                elementTrackerTargets.remove(i);
                continue;
            }
            entry.normalize();
        }
        for (int i = elementTrackerObservedCounts.size() - 1; i >= 0; i--) {
            ElementTrackerObservedCountEntry entry = elementTrackerObservedCounts.get(i);
            if (entry == null) {
                elementTrackerObservedCounts.remove(i);
                continue;
            }
            entry.normalize();
        }
        for (String elementKey : HaElementTracker.getElementKeys()) {
            getOrCreateElementTrackerTarget(elementKey);
            getOrCreateElementTrackerObservedCounts(elementKey);
        }
    }

    private static String normalizeElementTrackerKey(String elementKey) {
        if (elementKey == null) {
            return "";
        }
        return elementKey.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String normalizeElementTrackerRank(String targetRank) {
        if (targetRank == null || targetRank.trim().isEmpty()) {
            return HaElementTracker.ElementRank.LEGENDARY.getKey();
        }
        HaElementTracker.ElementRank rank = HaElementTracker.ElementRank.fromKey(targetRank);
        return rank == null ? HaElementTracker.ElementRank.LEGENDARY.getKey() : rank.getKey();
    }

    private static InputUtil.Key getBoundKey(String type, int keyCode, int scanCode) {
        if ("mouse".equalsIgnoreCase(type)) {
            return InputUtil.Type.MOUSE.createFromCode(keyCode);
        }
        return InputUtil.fromKeyCode(keyCode, scanCode);
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
        String macroToggleKeyType = "keysym";
        boolean alchemyKilnAutomationEnabled = false;
        int alchemyKilnAutomationKeyCode = GLFW.GLFW_KEY_UNKNOWN;
        int alchemyKilnAutomationScanCode = -1;
        String alchemyKilnAutomationKeyType = "keysym";
        int alchemyKilnAutomationClickIntervalTicks = 4;
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
        String cameraToggleKeyType = "keysym";
        int defaultWeaponHotbarSlot = 0;
        boolean itemLockEnabled = true;
        boolean soulbindProtectionEnabled = true;
        boolean chunkChestCounterEnabled = false;
        int chunkChestOverlayX = 8;
        int chunkChestOverlayY = 40;
        boolean damageTruncationEnabled = false;
        boolean elementRarityEnabled = true;
        boolean gearViewEnabled = true;
        int gearViewKeyCode = GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
        int gearViewKeyScanCode = -1;
        String gearViewKeyType = "mouse";
        boolean mobEspEnabled = false;
        String mobEspTargetName = "";
        boolean afkFarmingEnabled = false;
        boolean afkFarmingActive = false;
        String afkFarmingWebhookUrl = "";
        double afkFarmingReportIntervalMinutes = 5.0D;
        boolean afkFarmingPlayerAlertsEnabled = true;
        boolean afkFarmingKeyAdminAlertsEnabled = true;
        String afkFarmingKeyAdminName = "KeyAdmin";
        boolean afkFarmingMobMacroEnabled = false;
        boolean afkFarmingMobCircleVisible = true;
        boolean afkFarmingMobDebugHudEnabled = true;
        int afkFarmingMobMacroIndex = 0;
        int afkFarmingMobMinCount = 3;
        int afkFarmingMobMaxCount = 5;
        double afkFarmingMobMacroCooldownSeconds = 5.0D;
        boolean afkFarmingAutoMoveEnabled = false;
        double afkFarmingAutoMoveIntervalSeconds = 300.0D;
        double afkFarmingAutoMoveIntervalMinutes = 5.0D;
        double afkFarmingAutoMoveJitterSeconds = 10.0D;
        boolean chestSearchEnabled = false;
        String chestSearchQuery = "";
        int chestSearchKeyCode = GLFW.GLFW_KEY_UNKNOWN;
        int chestSearchScanCode = -1;
        String chestSearchKeyType = "keysym";
        boolean evolutionForgeHelperEnabled = true;
        boolean dropTrackerEnabled = false;
        String dropTrackerMode = HaDropTracker.MODE_ALL;
        long dropTrackerElapsedSeconds = 0L;
        boolean dropTrackerShowTimer = false;
        boolean dropTrackerShowHourlyProfit = false;
        boolean dropTrackerCompactNumbers = false;
        boolean dropTrackerContinueAfterStart = false;
        int dropTrackerOverlayX = 8;
        int dropTrackerOverlayY = 72;
        boolean dropNotifierEnabled = false;
        boolean dropNotifierContinueAfterStart = false;
        boolean expTrackerEnabled = false;
        long expTrackerTotalTenths = 0L;
        long expTrackerTotal = 0L;
        long expTrackerElapsedSeconds = 0L;
        boolean expTrackerShowTimer = false;
        boolean expTrackerShowHourlyRate = false;
        boolean expTrackerCompactNumbers = false;
        boolean expTrackerContinueAfterStart = false;
        int expTrackerOverlayX = 8;
        int expTrackerOverlayY = 104;
        boolean elementTrackerEnabled = false;
        long elementTrackerElapsedSeconds = 0L;
        boolean elementTrackerShowTimer = false;
        boolean elementTrackerContinueAfterStart = false;
        int elementTrackerOverlayX = 8;
        int elementTrackerOverlayY = 168;
        boolean mobHpDisplayEnabled = false;
        String mobHpDisplayPosition = "hud";
        boolean mobHpDisplaySlim = false;
        boolean mobHpDisplayShowPercentage = true;
        boolean mobHpDisplayCompactNumbers = false;
        int mobHpDisplayOverlayX = 8;
        int mobHpDisplayOverlayY = 136;
        boolean subSkillTimerEnabled = false;
        boolean subSkillTimerSlim = false;
        double subSkillTimerCooldownSeconds = 10.0D;
        int subSkillTimerOverlayX = 8;
        int subSkillTimerOverlayY = 200;
        boolean ritualBookTimerEnabled = false;
        boolean ritualBookTimerSlim = false;
        int ritualBookTimerSoundVolume = 100;
        int ritualBookTimerOverlayX = 8;
        int ritualBookTimerOverlayY = 232;
        boolean spotifyEnabled = false;
        boolean spotifyChromeDetectionEnabled = false;
        int spotifyOverlayX = 8;
        int spotifyOverlayY = 264;
        boolean chatFilterEnabled = false;
        Set<Integer> lockedSlotIds = new HashSet<Integer>();
        List<SavedElementTrackerTargetEntry> elementTrackerTargets = new ArrayList<SavedElementTrackerTargetEntry>();
        List<SavedElementTrackerObservedCountEntry> elementTrackerObservedCounts = new ArrayList<SavedElementTrackerObservedCountEntry>();
        List<SavedSwapEntry> swapEntries = new ArrayList<SavedSwapEntry>();
        List<SavedHpAlertEntry> hpAlertEntries = new ArrayList<SavedHpAlertEntry>();
        List<SavedManaAlertEntry> manaAlertEntries = new ArrayList<SavedManaAlertEntry>();
        List<SavedChatFilterEntry> chatFilterEntries = new ArrayList<SavedChatFilterEntry>();
        List<SavedDropNotifierEntry> dropNotifierEntries = new ArrayList<SavedDropNotifierEntry>();
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

    private static final class SavedDropNotifierEntry {
        boolean enabled = true;
        String matchText = "";
    }

    private static final class SavedElementTrackerTargetEntry {
        String elementKey = "";
        boolean enabled = false;
        String targetRank = HaElementTracker.ElementRank.LEGENDARY.getKey();
    }

    private static final class SavedElementTrackerObservedCountEntry {
        String elementKey = "";
        long commonCount = 0L;
        long rareCount = 0L;
        long superiorCount = 0L;
        long epicCount = 0L;
        long legendaryCount = 0L;
        long transcendentCount = 0L;
        long untouchableCount = 0L;
        long uniqueCount = 0L;
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

    private static long safeMultiplyByTen(long value) {
        if (value <= 0L) {
            return 0L;
        }
        if (value > Long.MAX_VALUE / 10L) {
            return Long.MAX_VALUE;
        }
        return value * 10L;
    }
}
