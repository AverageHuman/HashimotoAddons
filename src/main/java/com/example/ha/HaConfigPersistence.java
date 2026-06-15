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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.util.InputUtil;

public final class HaConfigPersistence {
    private static final long DEFERRED_SAVE_INTERVAL_MILLIS = 10_000L;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("HashimotoAddons");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");
    private static boolean deferredSavePending;
    private static long deferredSaveStartedMillis;

    private HaConfigPersistence() {
    }

    public static void load(HaConfig config) {
        if (!Files.exists(CONFIG_FILE)) {
            config.normalizeBeforeLoad();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            SavedConfig saved = GSON.fromJson(reader, SavedConfig.class);
            apply(config, saved);
        } catch (IOException exception) {
            reportLoadFailure(exception);
        } catch (RuntimeException exception) {
            reportLoadFailure(exception);
        }

        config.normalizeBeforeLoad();
    }

    public static void save(HaConfig config) {
        config.normalize();
        deferredSavePending = false;
        deferredSaveStartedMillis = 0L;
        final JsonObject snapshot = toJson(config);
        HaAsyncFileWriter.submit(CONFIG_FILE, new HaAsyncFileWriter.WriteOperation() {
            @Override
            public void write() throws IOException {
                Files.createDirectories(CONFIG_DIR);
                try (Writer writer = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                    GSON.toJson(snapshot, writer);
                }
            }
        });
    }

    public static void markDirty() {
        if (!deferredSavePending) {
            deferredSavePending = true;
            deferredSaveStartedMillis = System.currentTimeMillis();
        }
    }

    public static void tick(HaConfig config) {
        if (!deferredSavePending) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - deferredSaveStartedMillis >= DEFERRED_SAVE_INTERVAL_MILLIS) {
            save(config);
        }
    }

    public static void flush(HaConfig config) {
        save(config);
        HaAsyncFileWriter.flush(3000L);
    }

    public static void apply(HaConfig config, SavedConfig saved) {
        config.swapEntries.clear();
        config.hpAlertEntries.clear();
        config.manaAlertEntries.clear();
        config.chatFilterEntries.clear();
        config.dropNotifierEntries.clear();
        config.elementTrackerTargets.clear();
        config.elementTrackerObservedCounts.clear();
        if (saved == null) {
            return;
        }

        config.cameraToggleKeyCode = saved.cameraToggleKeyCode;
        config.cameraToggleScanCode = saved.cameraToggleScanCode;
        config.cameraToggleKeyType = saved.cameraToggleKeyType;
        config.itemLockEnabled = saved.itemLockEnabled;
        config.soulbindProtectionEnabled = saved.soulbindProtectionEnabled;
        config.chestSearchEnabled = saved.chestSearchEnabled;
        config.chestSearchQuery = saved.chestSearchQuery;
        config.chestSearchKeyCode = saved.chestSearchKeyCode;
        config.chestSearchScanCode = saved.chestSearchScanCode;
        config.chestSearchKeyType = saved.chestSearchKeyType;
        config.evolutionForgeHelperEnabled = saved.evolutionForgeHelperEnabled;
        config.dropTrackerEnabled = saved.dropTrackerEnabled;
        config.dropTrackerMode = saved.dropTrackerMode;
        config.dropTrackerElapsedSeconds = saved.dropTrackerElapsedSeconds;
        config.dropTrackerShowTimer = saved.dropTrackerShowTimer;
        config.dropTrackerShowHourlyProfit = saved.dropTrackerShowHourlyProfit;
        config.dropTrackerCompactNumbers = saved.dropTrackerCompactNumbers;
        config.dropTrackerContinueAfterStart = saved.dropTrackerContinueAfterStart;
        config.dropTrackerOverlayX = saved.dropTrackerOverlayX;
        config.dropTrackerOverlayY = saved.dropTrackerOverlayY;
        config.dropNotifierEnabled = saved.dropNotifierEnabled;
        config.dropNotifierContinueAfterStart = saved.dropNotifierContinueAfterStart;
        config.expTrackerEnabled = saved.expTrackerEnabled;
        config.expTrackerTotalTenths = saved.expTrackerTotalTenths > 0L ? saved.expTrackerTotalTenths : safeMultiplyByTen(saved.expTrackerTotal);
        config.expTrackerTotal = config.expTrackerTotalTenths / 10L;
        config.expTrackerElapsedSeconds = saved.expTrackerElapsedSeconds;
        config.expTrackerShowTimer = saved.expTrackerShowTimer;
        config.expTrackerShowHourlyRate = saved.expTrackerShowHourlyRate;
        config.expTrackerCompactNumbers = saved.expTrackerCompactNumbers;
        config.expTrackerContinueAfterStart = saved.expTrackerContinueAfterStart;
        config.expTrackerOverlayX = saved.expTrackerOverlayX;
        config.expTrackerOverlayY = saved.expTrackerOverlayY;
        config.elementTrackerEnabled = saved.elementTrackerEnabled;
        config.elementTrackerElapsedSeconds = saved.elementTrackerElapsedSeconds;
        config.elementTrackerShowTimer = saved.elementTrackerShowTimer;
        config.elementTrackerContinueAfterStart = saved.elementTrackerContinueAfterStart;
        config.elementTrackerOverlayX = saved.elementTrackerOverlayX;
        config.elementTrackerOverlayY = saved.elementTrackerOverlayY;
        config.mobHpDisplayEnabled = saved.mobHpDisplayEnabled;
        config.mobHpDisplayPosition = saved.mobHpDisplayPosition;
        config.mobHpDisplaySlim = saved.mobHpDisplaySlim;
        config.mobHpDisplayShowPercentage = saved.mobHpDisplayShowPercentage;
        config.mobHpDisplayCompactNumbers = saved.mobHpDisplayCompactNumbers;
        config.mobHpDisplayOverlayX = saved.mobHpDisplayOverlayX;
        config.mobHpDisplayOverlayY = saved.mobHpDisplayOverlayY;
        config.subSkillTimerEnabled = saved.subSkillTimerEnabled;
        config.subSkillTimerSlim = saved.subSkillTimerSlim;
        config.subSkillTimerCooldownSeconds = saved.subSkillTimerCooldownSeconds > 0.0D ? saved.subSkillTimerCooldownSeconds : 10.0D;
        config.subSkillTimerOverlayX = saved.subSkillTimerOverlayX;
        config.subSkillTimerOverlayY = saved.subSkillTimerOverlayY;
        config.ritualBookTimerEnabled = saved.ritualBookTimerEnabled;
        config.ritualBookTimerSlim = saved.ritualBookTimerSlim;
        config.ritualBookTimerSoundVolume = saved.ritualBookTimerSoundVolume;
        config.ritualBookTimerOverlayX = saved.ritualBookTimerOverlayX;
        config.ritualBookTimerOverlayY = saved.ritualBookTimerOverlayY;
        config.spotifyEnabled = saved.spotifyEnabled;
        config.spotifyChromeDetectionEnabled = saved.spotifyChromeDetectionEnabled;
        config.spotifyOverlayX = saved.spotifyOverlayX;
        config.spotifyOverlayY = saved.spotifyOverlayY;
        config.chatFilterEnabled = saved.chatFilterEnabled;
        config.lockedSlotIds = saved.lockedSlotIds != null ? new HashSet<Integer>(saved.lockedSlotIds) : new HashSet<Integer>();
        if (saved.elementTrackerTargets != null) {
            for (SavedElementTrackerTargetEntry savedEntry : saved.elementTrackerTargets) {
                HaConfig.ElementTrackerTargetEntry entry = new HaConfig.ElementTrackerTargetEntry();
                if (savedEntry != null) {
                    entry.elementKey = savedEntry.elementKey;
                    entry.enabled = savedEntry.enabled;
                    entry.targetRank = savedEntry.targetRank;
                }
                entry.normalize();
                config.elementTrackerTargets.add(entry);
            }
        }
        if (saved.elementTrackerObservedCounts != null) {
            for (SavedElementTrackerObservedCountEntry savedEntry : saved.elementTrackerObservedCounts) {
                HaConfig.ElementTrackerObservedCountEntry entry = new HaConfig.ElementTrackerObservedCountEntry();
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
                config.elementTrackerObservedCounts.add(entry);
            }
        }
        if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            config.autoHealEnabled = saved.autoHealEnabled;
            config.autoHealHotbarSlot = saved.autoHealHotbarSlot;
            config.autoHealCooldownSeconds = saved.autoHealCooldownSeconds > 0.0D ? saved.autoHealCooldownSeconds : 1.0D;
            config.autoHealHealthRatioThreshold = saved.autoHealHealthRatioThreshold > 0.0F ? saved.autoHealHealthRatioThreshold : 0.75F;
            config.macroEnabled = saved.macroEnabled;
            config.macroToggleKeyCode = saved.macroToggleKeyCode;
            config.macroToggleScanCode = saved.macroToggleScanCode;
            config.macroToggleKeyType = saved.macroToggleKeyType;
            config.alchemyKilnAutomationEnabled = saved.alchemyKilnAutomationEnabled;
            config.alchemyKilnAutomationClickIntervalTicks = saved.alchemyKilnAutomationClickIntervalTicks > 0 ? saved.alchemyKilnAutomationClickIntervalTicks : 4;
            config.macroStatusHudEnabled = saved.macroStatusHudEnabled;
            config.macroStatusHudX = saved.macroStatusHudX;
            config.macroStatusHudY = saved.macroStatusHudY;
            config.extrasEnabled = saved.extrasEnabled;
            config.ghostWallEditMode = saved.ghostWallEditMode;
            config.extrasHudEnabled = saved.extrasHudEnabled;
            config.extrasHudX = saved.extrasHudX;
            config.extrasHudY = saved.extrasHudY;
            config.selectedGhostBlockId = saved.selectedGhostBlockId;
            config.favoriteGhostBlockIds.clear();
            if (saved.favoriteGhostBlockIds != null) {
                config.favoriteGhostBlockIds.addAll(saved.favoriteGhostBlockIds);
            }
            config.defaultWeaponHotbarSlot = saved.defaultWeaponHotbarSlot;
            config.chunkChestCounterEnabled = saved.chunkChestCounterEnabled;
            config.chunkChestOverlayX = saved.chunkChestOverlayX;
            config.chunkChestOverlayY = saved.chunkChestOverlayY;
            config.damageTruncationEnabled = saved.damageTruncationEnabled;
            config.elementRarityEnabled = saved.elementRarityEnabled;
            config.gearViewEnabled = saved.gearViewEnabled;
            config.gearViewKeyCode = saved.gearViewKeyCode;
            config.gearViewKeyScanCode = saved.gearViewKeyScanCode;
            config.gearViewKeyType = saved.gearViewKeyType;
            config.alchemyKilnAutomationKeyCode = saved.alchemyKilnAutomationKeyCode;
            config.alchemyKilnAutomationScanCode = saved.alchemyKilnAutomationScanCode;
            config.alchemyKilnAutomationKeyType = saved.alchemyKilnAutomationKeyType;
            config.mobEspEnabled = saved.mobEspEnabled;
            config.mobEspTargetName = saved.mobEspTargetName;
            config.afkFarmingEnabled = saved.afkFarmingEnabled;
            config.afkFarmingActive = saved.afkFarmingActive;
            config.afkFarmingWebhookUrl = saved.afkFarmingWebhookUrl;
            config.afkFarmingReportIntervalMinutes = saved.afkFarmingReportIntervalMinutes > 0.0D ? saved.afkFarmingReportIntervalMinutes : 5.0D;
            config.afkFarmingPlayerAlertsEnabled = saved.afkFarmingPlayerAlertsEnabled;
            config.afkFarmingKeyAdminAlertsEnabled = saved.afkFarmingKeyAdminAlertsEnabled;
            config.afkFarmingKeyAdminName = saved.afkFarmingKeyAdminName;
            config.afkFarmingMobMacroEnabled = saved.afkFarmingMobMacroEnabled;
            config.afkFarmingMobCircleVisible = saved.afkFarmingMobCircleVisible;
            config.afkFarmingMobDebugHudEnabled = saved.afkFarmingMobDebugHudEnabled;
            config.afkFarmingMobMacroIndex = saved.afkFarmingMobMacroIndex;
            config.afkFarmingMobMinCount = saved.afkFarmingMobMinCount > 0 ? saved.afkFarmingMobMinCount : 3;
            config.afkFarmingMobMaxCount = saved.afkFarmingMobMaxCount > 0 ? saved.afkFarmingMobMaxCount : 5;
            config.afkFarmingMobMacroCooldownSeconds = saved.afkFarmingMobMacroCooldownSeconds > 0.0D ? saved.afkFarmingMobMacroCooldownSeconds : 5.0D;
            config.afkFarmingAutoMoveEnabled = saved.afkFarmingAutoMoveEnabled;
            if (saved.afkFarmingAutoMoveIntervalSeconds > 0.0D) {
                config.afkFarmingAutoMoveIntervalSeconds = saved.afkFarmingAutoMoveIntervalSeconds;
            } else if (saved.afkFarmingAutoMoveIntervalMinutes > 0.0D) {
                config.afkFarmingAutoMoveIntervalSeconds = saved.afkFarmingAutoMoveIntervalMinutes * 60.0D;
            } else {
                config.afkFarmingAutoMoveIntervalSeconds = 300.0D;
            }
            config.afkFarmingAutoMoveIntervalMinutes = config.afkFarmingAutoMoveIntervalSeconds / 60.0D;
            config.afkFarmingAutoMoveJitterSeconds = saved.afkFarmingAutoMoveJitterSeconds >= 0.0D ? saved.afkFarmingAutoMoveJitterSeconds : 10.0D;
            if (saved.swapEntries != null) {
                for (SavedSwapEntry savedEntry : saved.swapEntries) {
                    HaConfig.SwapEntry entry = new HaConfig.SwapEntry();
                    if (savedEntry != null) {
                        entry.name = savedEntry.name;
                        entry.hotbarSlot = savedEntry.hotbarSlot;
                        entry.intervalSeconds = savedEntry.intervalSeconds;
                        entry.holdTicks = savedEntry.holdTicks;
                    }
                    entry.normalize();
                    config.swapEntries.add(entry);
                }
            }
        } else {
            config.resetDangerousState();
        }
        if (saved.hpAlertEntries != null) {
            for (SavedHpAlertEntry savedEntry : saved.hpAlertEntries) {
                HaConfig.HpAlertEntry entry = new HaConfig.HpAlertEntry();
                if (savedEntry != null) {
                    entry.enabled = savedEntry.enabled;
                    entry.healthPercentage = savedEntry.healthPercentage;
                    entry.colorIndex = savedEntry.colorIndex;
                    entry.titleText = savedEntry.titleText;
                }
                entry.normalize();
                config.hpAlertEntries.add(entry);
            }
        }
        if (saved.manaAlertEntries != null) {
            for (SavedManaAlertEntry savedEntry : saved.manaAlertEntries) {
                HaConfig.ManaAlertEntry entry = new HaConfig.ManaAlertEntry();
                if (savedEntry != null) {
                    entry.enabled = savedEntry.enabled;
                    entry.manaPercentage = savedEntry.manaPercentage;
                    entry.colorIndex = savedEntry.colorIndex;
                    entry.titleText = savedEntry.titleText;
                }
                entry.normalize();
                config.manaAlertEntries.add(entry);
            }
        }
        if (saved.chatFilterEntries != null) {
            for (SavedChatFilterEntry savedEntry : saved.chatFilterEntries) {
                HaConfig.ChatFilterEntry entry = new HaConfig.ChatFilterEntry();
                if (savedEntry != null) {
                    entry.enabled = savedEntry.enabled;
                    entry.matchText = savedEntry.matchText;
                }
                entry.normalize();
                if (!entry.matchText.isEmpty()) {
                    config.chatFilterEntries.add(entry);
                }
            }
        }
        if (saved.dropNotifierEntries != null) {
            for (SavedDropNotifierEntry savedEntry : saved.dropNotifierEntries) {
                HaConfig.DropNotifierEntry entry = new HaConfig.DropNotifierEntry();
                if (savedEntry != null) {
                    entry.enabled = savedEntry.enabled;
                    entry.matchText = savedEntry.matchText;
                }
                entry.normalize();
                if (!entry.matchText.isEmpty()) {
                    config.dropNotifierEntries.add(entry);
                }
            }
        }
    }

    public static com.google.gson.JsonObject toJson(HaConfig config) {
        JsonObject root = new JsonObject();
        root.addProperty("cameraToggleKeyCode", config.cameraToggleKeyCode);
        root.addProperty("cameraToggleScanCode", config.cameraToggleScanCode);
        root.addProperty("cameraToggleKeyType", config.cameraToggleKeyType);
        root.addProperty("itemLockEnabled", config.itemLockEnabled);
        root.addProperty("soulbindProtectionEnabled", config.soulbindProtectionEnabled);
        root.addProperty("chestSearchEnabled", config.chestSearchEnabled);
        root.addProperty("chestSearchQuery", config.chestSearchQuery);
        root.addProperty("chestSearchKeyCode", config.chestSearchKeyCode);
        root.addProperty("chestSearchScanCode", config.chestSearchScanCode);
        root.addProperty("chestSearchKeyType", config.chestSearchKeyType);
        root.addProperty("evolutionForgeHelperEnabled", config.evolutionForgeHelperEnabled);
        root.addProperty("dropTrackerEnabled", config.dropTrackerEnabled);
        root.addProperty("dropTrackerMode", config.dropTrackerMode);
        root.addProperty("dropTrackerElapsedSeconds", config.dropTrackerElapsedSeconds);
        root.addProperty("dropTrackerShowTimer", config.dropTrackerShowTimer);
        root.addProperty("dropTrackerShowHourlyProfit", config.dropTrackerShowHourlyProfit);
        root.addProperty("dropTrackerCompactNumbers", config.dropTrackerCompactNumbers);
        root.addProperty("dropTrackerContinueAfterStart", config.dropTrackerContinueAfterStart);
        root.addProperty("dropTrackerOverlayX", config.dropTrackerOverlayX);
        root.addProperty("dropTrackerOverlayY", config.dropTrackerOverlayY);
        root.addProperty("dropNotifierEnabled", config.dropNotifierEnabled);
        root.addProperty("dropNotifierContinueAfterStart", config.dropNotifierContinueAfterStart);
        root.addProperty("expTrackerEnabled", config.expTrackerEnabled);
        root.addProperty("expTrackerTotalTenths", config.expTrackerTotalTenths);
        root.addProperty("expTrackerTotal", config.expTrackerTotalTenths / 10L);
        root.addProperty("expTrackerElapsedSeconds", config.expTrackerElapsedSeconds);
        root.addProperty("expTrackerShowTimer", config.expTrackerShowTimer);
        root.addProperty("expTrackerShowHourlyRate", config.expTrackerShowHourlyRate);
        root.addProperty("expTrackerCompactNumbers", config.expTrackerCompactNumbers);
        root.addProperty("expTrackerContinueAfterStart", config.expTrackerContinueAfterStart);
        root.addProperty("expTrackerOverlayX", config.expTrackerOverlayX);
        root.addProperty("expTrackerOverlayY", config.expTrackerOverlayY);
        root.addProperty("elementTrackerEnabled", config.elementTrackerEnabled);
        root.addProperty("elementTrackerElapsedSeconds", config.elementTrackerElapsedSeconds);
        root.addProperty("elementTrackerShowTimer", config.elementTrackerShowTimer);
        root.addProperty("elementTrackerContinueAfterStart", config.elementTrackerContinueAfterStart);
        root.addProperty("elementTrackerOverlayX", config.elementTrackerOverlayX);
        root.addProperty("elementTrackerOverlayY", config.elementTrackerOverlayY);
        root.addProperty("mobHpDisplayEnabled", config.mobHpDisplayEnabled);
        root.addProperty("mobHpDisplayPosition", config.mobHpDisplayPosition);
        root.addProperty("mobHpDisplaySlim", config.mobHpDisplaySlim);
        root.addProperty("mobHpDisplayShowPercentage", config.mobHpDisplayShowPercentage);
        root.addProperty("mobHpDisplayCompactNumbers", config.mobHpDisplayCompactNumbers);
        root.addProperty("mobHpDisplayOverlayX", config.mobHpDisplayOverlayX);
        root.addProperty("mobHpDisplayOverlayY", config.mobHpDisplayOverlayY);
        root.addProperty("subSkillTimerEnabled", config.subSkillTimerEnabled);
        root.addProperty("subSkillTimerSlim", config.subSkillTimerSlim);
        root.addProperty("subSkillTimerCooldownSeconds", config.subSkillTimerCooldownSeconds);
        root.addProperty("subSkillTimerOverlayX", config.subSkillTimerOverlayX);
        root.addProperty("subSkillTimerOverlayY", config.subSkillTimerOverlayY);
        root.addProperty("ritualBookTimerEnabled", config.ritualBookTimerEnabled);
        root.addProperty("ritualBookTimerSlim", config.ritualBookTimerSlim);
        root.addProperty("ritualBookTimerSoundVolume", config.ritualBookTimerSoundVolume);
        root.addProperty("ritualBookTimerOverlayX", config.ritualBookTimerOverlayX);
        root.addProperty("ritualBookTimerOverlayY", config.ritualBookTimerOverlayY);
        root.addProperty("spotifyEnabled", config.spotifyEnabled);
        root.addProperty("spotifyChromeDetectionEnabled", config.spotifyChromeDetectionEnabled);
        root.addProperty("spotifyOverlayX", config.spotifyOverlayX);
        root.addProperty("spotifyOverlayY", config.spotifyOverlayY);
        root.addProperty("chatFilterEnabled", config.chatFilterEnabled);
        root.add("lockedSlotIds", GSON.toJsonTree(new HashSet<Integer>(config.lockedSlotIds)));
        root.add("hpAlertEntries", GSON.toJsonTree(toSavedHpAlertEntries(config)));
        root.add("manaAlertEntries", GSON.toJsonTree(toSavedManaAlertEntries(config)));
        root.add("chatFilterEntries", GSON.toJsonTree(toSavedChatFilterEntries(config)));
        root.add("dropNotifierEntries", GSON.toJsonTree(toSavedDropNotifierEntries(config)));
        root.add("elementTrackerTargets", GSON.toJsonTree(toSavedElementTrackerTargets(config)));
        root.add("elementTrackerObservedCounts", GSON.toJsonTree(toSavedElementTrackerObservedCounts(config)));
        if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            root.addProperty("autoHealEnabled", config.autoHealEnabled);
            root.addProperty("autoHealHotbarSlot", config.autoHealHotbarSlot);
            root.addProperty("autoHealCooldownSeconds", config.autoHealCooldownSeconds);
            root.addProperty("autoHealHealthRatioThreshold", config.autoHealHealthRatioThreshold);
            root.addProperty("macroEnabled", config.macroEnabled);
            root.addProperty("macroToggleKeyCode", config.macroToggleKeyCode);
            root.addProperty("macroToggleScanCode", config.macroToggleScanCode);
            root.addProperty("macroToggleKeyType", config.macroToggleKeyType);
            root.addProperty("alchemyKilnAutomationEnabled", config.alchemyKilnAutomationEnabled);
            root.addProperty("alchemyKilnAutomationClickIntervalTicks", config.alchemyKilnAutomationClickIntervalTicks);
            root.addProperty("macroStatusHudEnabled", config.macroStatusHudEnabled);
            root.addProperty("macroStatusHudX", config.macroStatusHudX);
            root.addProperty("macroStatusHudY", config.macroStatusHudY);
            root.addProperty("extrasEnabled", config.extrasEnabled);
            root.addProperty("ghostWallEditMode", config.ghostWallEditMode);
            root.addProperty("extrasHudEnabled", config.extrasHudEnabled);
            root.addProperty("extrasHudX", config.extrasHudX);
            root.addProperty("extrasHudY", config.extrasHudY);
            root.addProperty("selectedGhostBlockId", config.selectedGhostBlockId);
            root.add("favoriteGhostBlockIds", GSON.toJsonTree(new ArrayList<String>(config.favoriteGhostBlockIds)));
            root.addProperty("defaultWeaponHotbarSlot", config.defaultWeaponHotbarSlot);
            root.addProperty("chunkChestCounterEnabled", config.chunkChestCounterEnabled);
            root.addProperty("chunkChestOverlayX", config.chunkChestOverlayX);
            root.addProperty("chunkChestOverlayY", config.chunkChestOverlayY);
            root.addProperty("damageTruncationEnabled", config.damageTruncationEnabled);
            root.addProperty("elementRarityEnabled", config.elementRarityEnabled);
            root.addProperty("gearViewEnabled", config.gearViewEnabled);
            root.addProperty("gearViewKeyCode", config.gearViewKeyCode);
            root.addProperty("gearViewKeyScanCode", config.gearViewKeyScanCode);
            root.addProperty("gearViewKeyType", config.gearViewKeyType);
            root.addProperty("alchemyKilnAutomationKeyCode", config.alchemyKilnAutomationKeyCode);
            root.addProperty("alchemyKilnAutomationScanCode", config.alchemyKilnAutomationScanCode);
            root.addProperty("alchemyKilnAutomationKeyType", config.alchemyKilnAutomationKeyType);
            root.addProperty("mobEspEnabled", config.mobEspEnabled);
            root.addProperty("mobEspTargetName", config.mobEspTargetName);
            root.addProperty("afkFarmingEnabled", config.afkFarmingEnabled);
            root.addProperty("afkFarmingActive", config.afkFarmingActive);
            root.addProperty("afkFarmingWebhookUrl", config.afkFarmingWebhookUrl);
            root.addProperty("afkFarmingReportIntervalMinutes", config.afkFarmingReportIntervalMinutes);
            root.addProperty("afkFarmingPlayerAlertsEnabled", config.afkFarmingPlayerAlertsEnabled);
            root.addProperty("afkFarmingKeyAdminAlertsEnabled", config.afkFarmingKeyAdminAlertsEnabled);
            root.addProperty("afkFarmingKeyAdminName", config.afkFarmingKeyAdminName);
            root.addProperty("afkFarmingMobMacroEnabled", config.afkFarmingMobMacroEnabled);
            root.addProperty("afkFarmingMobCircleVisible", config.afkFarmingMobCircleVisible);
            root.addProperty("afkFarmingMobDebugHudEnabled", config.afkFarmingMobDebugHudEnabled);
            root.addProperty("afkFarmingMobMacroIndex", config.afkFarmingMobMacroIndex);
            root.addProperty("afkFarmingMobMinCount", config.afkFarmingMobMinCount);
            root.addProperty("afkFarmingMobMaxCount", config.afkFarmingMobMaxCount);
            root.addProperty("afkFarmingMobMacroCooldownSeconds", config.afkFarmingMobMacroCooldownSeconds);
            root.addProperty("afkFarmingAutoMoveEnabled", config.afkFarmingAutoMoveEnabled);
            root.addProperty("afkFarmingAutoMoveIntervalSeconds", config.afkFarmingAutoMoveIntervalSeconds);
            root.addProperty("afkFarmingAutoMoveIntervalMinutes", config.afkFarmingAutoMoveIntervalSeconds / 60.0D);
            root.addProperty("afkFarmingAutoMoveJitterSeconds", config.afkFarmingAutoMoveJitterSeconds);
            root.add("swapEntries", GSON.toJsonTree(toSavedSwapEntries(config)));
        }
        return root;
    }

    private static void reportLoadFailure(Exception exception) {
        System.err.println("[HashimotoAddons] Failed to load config.json: " + exception.getMessage());
        exception.printStackTrace(System.err);
    }

    private static long safeMultiplyByTen(long value) {
        return value > Long.MAX_VALUE / 10L ? Long.MAX_VALUE : value * 10L;
    }

    private static java.util.List<SavedSwapEntry> toSavedSwapEntries(HaConfig config) {
        java.util.List<SavedSwapEntry> savedEntries = new java.util.ArrayList<SavedSwapEntry>();
        for (HaConfig.SwapEntry entry : config.swapEntries) {
            SavedSwapEntry savedEntry = new SavedSwapEntry();
            savedEntry.name = entry.name;
            savedEntry.hotbarSlot = entry.hotbarSlot;
            savedEntry.intervalSeconds = entry.intervalSeconds;
            savedEntry.holdTicks = entry.holdTicks;
            savedEntries.add(savedEntry);
        }
        return savedEntries;
    }

    private static java.util.List<SavedHpAlertEntry> toSavedHpAlertEntries(HaConfig config) { java.util.List<SavedHpAlertEntry> list = new java.util.ArrayList<SavedHpAlertEntry>(); for (HaConfig.HpAlertEntry entry : config.hpAlertEntries) { SavedHpAlertEntry savedEntry = new SavedHpAlertEntry(); savedEntry.enabled = entry.enabled; savedEntry.healthPercentage = entry.healthPercentage; savedEntry.colorIndex = entry.colorIndex; savedEntry.titleText = entry.titleText; list.add(savedEntry);} return list; }
    private static java.util.List<SavedManaAlertEntry> toSavedManaAlertEntries(HaConfig config) { java.util.List<SavedManaAlertEntry> list = new java.util.ArrayList<SavedManaAlertEntry>(); for (HaConfig.ManaAlertEntry entry : config.manaAlertEntries) { SavedManaAlertEntry savedEntry = new SavedManaAlertEntry(); savedEntry.enabled = entry.enabled; savedEntry.manaPercentage = entry.manaPercentage; savedEntry.colorIndex = entry.colorIndex; savedEntry.titleText = entry.titleText; list.add(savedEntry);} return list; }
    private static java.util.List<SavedChatFilterEntry> toSavedChatFilterEntries(HaConfig config) { java.util.List<SavedChatFilterEntry> list = new java.util.ArrayList<SavedChatFilterEntry>(); for (HaConfig.ChatFilterEntry entry : config.chatFilterEntries) { if (entry.matchText == null || entry.matchText.trim().isEmpty()) continue; SavedChatFilterEntry savedEntry = new SavedChatFilterEntry(); savedEntry.enabled = entry.enabled; savedEntry.matchText = entry.matchText; list.add(savedEntry);} return list; }
    private static java.util.List<SavedDropNotifierEntry> toSavedDropNotifierEntries(HaConfig config) { java.util.List<SavedDropNotifierEntry> list = new java.util.ArrayList<SavedDropNotifierEntry>(); for (HaConfig.DropNotifierEntry entry : config.dropNotifierEntries) { if (entry.matchText == null || entry.matchText.trim().isEmpty()) continue; SavedDropNotifierEntry savedEntry = new SavedDropNotifierEntry(); savedEntry.enabled = entry.enabled; savedEntry.matchText = entry.matchText; list.add(savedEntry);} return list; }
    private static java.util.List<SavedElementTrackerTargetEntry> toSavedElementTrackerTargets(HaConfig config) { java.util.List<SavedElementTrackerTargetEntry> list = new java.util.ArrayList<SavedElementTrackerTargetEntry>(); for (HaConfig.ElementTrackerTargetEntry entry : config.elementTrackerTargets) { SavedElementTrackerTargetEntry savedEntry = new SavedElementTrackerTargetEntry(); savedEntry.elementKey = entry.elementKey; savedEntry.enabled = entry.enabled; savedEntry.targetRank = entry.targetRank; list.add(savedEntry);} return list; }
    private static java.util.List<SavedElementTrackerObservedCountEntry> toSavedElementTrackerObservedCounts(HaConfig config) { java.util.List<SavedElementTrackerObservedCountEntry> list = new java.util.ArrayList<SavedElementTrackerObservedCountEntry>(); for (HaConfig.ElementTrackerObservedCountEntry entry : config.elementTrackerObservedCounts) { SavedElementTrackerObservedCountEntry savedEntry = new SavedElementTrackerObservedCountEntry(); savedEntry.elementKey = entry.elementKey; savedEntry.commonCount = entry.commonCount; savedEntry.rareCount = entry.rareCount; savedEntry.superiorCount = entry.superiorCount; savedEntry.epicCount = entry.epicCount; savedEntry.legendaryCount = entry.legendaryCount; savedEntry.transcendentCount = entry.transcendentCount; savedEntry.untouchableCount = entry.untouchableCount; savedEntry.uniqueCount = entry.uniqueCount; list.add(savedEntry);} return list; }
}
