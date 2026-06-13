package com.example.ha;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class HaConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
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
        normalizeEntryCollections();
        normalizeCoreSettings();
        normalizeTrackerSettings();
        normalizeOverlaySettings();
        normalizeDangerousSettings();
        normalizeSharedCollections();
    }

    private void normalizeEntryCollections() {
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
    }

    private void normalizeCoreSettings() {
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
    }

    private void normalizeTrackerSettings() {
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
    }

    private void normalizeOverlaySettings() {
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
    }

    private void normalizeDangerousSettings() {
        if (lockedSlotIds == null) {
            lockedSlotIds = new HashSet<Integer>();
        }
    }

    private void normalizeSharedCollections() {
        // Keep shared collection cleanup separate from field clamping.
    }

    public void load() {
        HaConfigPersistence.load(this);
    }

    public void save() {
        HaConfigPersistence.save(this);
    }

    void normalizeBeforeLoad() {
        normalize();
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

    void resetDangerousState() {
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
