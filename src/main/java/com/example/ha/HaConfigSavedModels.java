package com.example.ha;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lwjgl.glfw.GLFW;

final class SavedConfig {
    boolean autoHealEnabled;
    int autoHealHotbarSlot = 1;
    double autoHealCooldownSeconds = 1.0D;
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
    boolean triggerBotEnabled = false;
    int triggerBotMacroIndex = 0;
    double triggerBotCooldownSeconds = 1.0D;
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
    Set<String> protectedItemIds = new HashSet<String>();
    List<SavedElementTrackerTargetEntry> elementTrackerTargets = new ArrayList<SavedElementTrackerTargetEntry>();
    List<SavedElementTrackerObservedCountEntry> elementTrackerObservedCounts = new ArrayList<SavedElementTrackerObservedCountEntry>();
    List<SavedSwapEntry> swapEntries = new ArrayList<SavedSwapEntry>();
    List<SavedHpAlertEntry> hpAlertEntries = new ArrayList<SavedHpAlertEntry>();
    List<SavedManaAlertEntry> manaAlertEntries = new ArrayList<SavedManaAlertEntry>();
    List<SavedChatFilterEntry> chatFilterEntries = new ArrayList<SavedChatFilterEntry>();
    List<SavedDropNotifierEntry> dropNotifierEntries = new ArrayList<SavedDropNotifierEntry>();
}

final class SavedSwapEntry {
    String name = "New Macro";
    int hotbarSlot = 0;
    double intervalSeconds = 5.0D;
    int holdTicks = 4;
    boolean enabled = true;
}

final class SavedHpAlertEntry {
    boolean enabled = true;
    int healthPercentage = 50;
    int colorIndex = 0;
    String titleText = "HP ALERT";
}

final class SavedManaAlertEntry {
    boolean enabled = true;
    int manaPercentage = 50;
    int colorIndex = 0;
    String titleText = "MANA ALERT";
}

final class SavedChatFilterEntry {
    boolean enabled = true;
    String matchText = "";
}

final class SavedDropNotifierEntry {
    boolean enabled = true;
    String matchText = "";
}

final class SavedElementTrackerTargetEntry {
    String elementKey = "";
    boolean enabled = false;
    String targetRank = HaElementTracker.ElementRank.LEGENDARY.getKey();
}

final class SavedElementTrackerObservedCountEntry {
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
