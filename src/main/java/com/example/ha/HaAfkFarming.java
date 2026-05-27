package com.example.ha;

import com.example.ha.mixin.KeyBindingAccessor;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

public final class HaAfkFarming {
    private static final double MOB_CENTER_DISTANCE = 3.0D;
    private static final double MOB_RADIUS_SQUARED = 3.0D * 3.0D;
    private static final double START_POSITION_ALERT_DISTANCE_SQUARED = 2.0D * 2.0D;
    private static final int START_POSITION_ALERT_SOUND_COUNT = 10;
    private static final int START_POSITION_ALERT_SOUND_DELAY_TICKS = 4;
    private static final long AUTO_MOVE_LEFT_MILLIS = 200L;
    private static final long AUTO_MOVE_BACK_MILLIS = 500L;
    private static final long AUTO_MOVE_LOOK_MILLIS = 150L;
    private static final int AUTO_MOVE_IDLE_STEP = -1;
    private static final int AUTO_MOVE_LOOK_OFFSET_STEP = 4;
    private static final int AUTO_MOVE_LOOK_RESTORE_STEP = 5;
    private static final float AUTO_MOVE_LOOK_YAW_DELTA = 3.0F;
    private static final float AUTO_MOVE_LOOK_PITCH_DELTA = 1.0F;
    private static final Random RANDOM = new Random();
    private static final Map<String, Long> LAST_ALERT_MILLIS = new HashMap<String, Long>();
    private static final ExecutorService WEBHOOK_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "HashimotoAddons-AFK-Farming-Webhook");
        thread.setDaemon(true);
        return thread;
    });

    private static boolean wasRunning;
    private static long lastReportMillis;
    private static long nextMobMacroMillis;
    private static long nextMobMacroTriggerMillis;
    private static int currentMobThreshold = -1;
    private static MobDebugInfo lastMobDebugInfo = MobDebugInfo.empty();
    private static volatile String lastWebhookStatus = "Not sent";
    private static boolean mobMacroDraining;
    private static Vec3d monitoringStartPosition;
    private static boolean monitoringStartPositionAlerted;
    private static int pendingStartPositionAlertSounds;
    private static int pendingStartPositionAlertSoundDelayTicks;
    private static long nextAutoMoveMillis;
    private static int autoMoveStep = AUTO_MOVE_IDLE_STEP;
    private static long autoMoveStepStartMillis;
    private static long autoMoveStepEndMillis;
    private static InputUtil.Key autoMovePressedKey = InputUtil.UNKNOWN_KEY;
    private static float autoMoveStartYaw;
    private static float autoMoveStartPitch;
    private static float autoMoveLookYawDelta;
    private static float autoMoveLookPitchDelta;
    private static float autoMoveLookFromYaw;
    private static float autoMoveLookFromPitch;
    private static float autoMoveLookToYaw;
    private static float autoMoveLookToPitch;
    private static boolean autoMoveViewCaptured;
    private static boolean autoMoveViewAdjusted;

    private HaAfkFarming() {
    }

    public static void tick(MinecraftClient client, HaTickHandler tickHandler) {
        HaConfig config = HaConfig.get();
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED
            || client == null
            || client.player == null
            || client.world == null
            || !config.afkFarmingEnabled
            || !config.afkFarmingActive) {
            resetRuntime(false);
            return;
        }

        long now = System.currentTimeMillis();
        if (!wasRunning) {
            wasRunning = true;
            lastReportMillis = now;
            LAST_ALERT_MILLIS.clear();
            currentMobThreshold = -1;
            monitoringStartPosition = client.player.getPos();
            monitoringStartPositionAlerted = false;
            pendingStartPositionAlertSounds = 0;
            pendingStartPositionAlertSoundDelayTicks = 0;
            resetAutoMove(client, autoMoveStep != AUTO_MOVE_IDLE_STEP || autoMovePressedKey != InputUtil.UNKNOWN_KEY);
            scheduleNextAutoMove(config, now);
            sendWebhook(config.afkFarmingWebhookUrl, "HashimotoAddons: Monitoring started!");
        }

        tickStartPositionAlert(client, config);
        tickAutoMove(client, config, now);
        tickPlayerAlerts(client, config, now);
        tickKeyAdminAlert(client, config, now);
        tickStatusReport(config, now);
        tickMobMacro(client, tickHandler, config, now);
    }

    public static void onDisconnected() {
        HaConfig config = HaConfig.get();
        if (config.afkFarmingActive) {
            config.afkFarmingActive = false;
            config.save();
        }
        resetRuntime(true);
    }

    public static String getLastWebhookStatus() {
        return lastWebhookStatus;
    }

    public static void sendWebhookTest(MinecraftClient client) {
        HaConfig config = HaConfig.get();
        sendWebhook(config.afkFarmingWebhookUrl, "HashimotoAddons: Webhook test from mod.", true);
        sendClientMessage(client, "Webhook test queued.");
    }

    private static void tickPlayerAlerts(MinecraftClient client, HaConfig config, long now) {
        if (!config.afkFarmingPlayerAlertsEnabled) {
            return;
        }

        for (AbstractClientPlayerEntity other : client.world.getPlayers()) {
            if (other == null || other == client.player) {
                continue;
            }
            String rawName = other.getGameProfile() == null ? other.getEntityName() : other.getGameProfile().getName();
            if (containsFormattingCode(rawName)) {
                continue;
            }
            String name = rawName;
            name = stripFormatting(name);
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            maybeAlert(client, config, "player:" + name.toLowerCase(Locale.ROOT), "Player Nearby", name, now);
        }
    }

    private static void tickKeyAdminAlert(MinecraftClient client, HaConfig config, long now) {
        if (!config.afkFarmingKeyAdminAlertsEnabled || client.getNetworkHandler() == null) {
            return;
        }

        String target = config.afkFarmingKeyAdminName == null ? "KeyAdmin" : config.afkFarmingKeyAdminName.trim();
        if (target.isEmpty()) {
            target = "KeyAdmin";
        }

        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            if (entry == null || entry.getProfile() == null || entry.getProfile().getName() == null) {
                continue;
            }
            String rawName = entry.getProfile().getName();
            if (containsFormattingCode(rawName)) {
                continue;
            }
            String name = stripFormatting(rawName);
            if (name.equalsIgnoreCase(target)) {
                maybeAlert(client, config, "keyadmin:" + target.toLowerCase(Locale.ROOT), "KeyAdmin Online", name, now);
                return;
            }
        }
    }

    private static void tickStatusReport(HaConfig config, long now) {
        long intervalMillis = Math.max(1L, Math.round(config.afkFarmingReportIntervalMinutes * 60_000.0D));
        if (now - lastReportMillis < intervalMillis) {
            return;
        }
        lastReportMillis = now;
        sendWebhook(config.afkFarmingWebhookUrl, buildStatusMessage(config));
    }

    private static void tickStartPositionAlert(MinecraftClient client, HaConfig config) {
        tickPendingStartPositionAlertSounds(client);
        if (monitoringStartPosition == null || client.player == null) {
            return;
        }

        Vec3d currentPosition = client.player.getPos();
        boolean awayFromStart = currentPosition.squaredDistanceTo(monitoringStartPosition) >= START_POSITION_ALERT_DISTANCE_SQUARED;
        if (!awayFromStart) {
            monitoringStartPositionAlerted = false;
            return;
        }
        if (monitoringStartPositionAlerted) {
            return;
        }

        monitoringStartPositionAlerted = true;
        client.inGameHud.setTitles(new LiteralText("\u00a7cAFK Position Alert"), new LiteralText("\u00a7eMoved 2+ blocks from start"), 5, 50, 10);
        sendWebhook(
            config.afkFarmingWebhookUrl,
            "HashimotoAddons: AFK position alert!\n"
                + "Start: " + formatPosition(monitoringStartPosition) + "\n"
                + "Current: " + formatPosition(currentPosition)
        );
        playStartPositionAlertSound(client);
        pendingStartPositionAlertSounds = START_POSITION_ALERT_SOUND_COUNT - 1;
        pendingStartPositionAlertSoundDelayTicks = START_POSITION_ALERT_SOUND_DELAY_TICKS;
    }

    private static void tickPendingStartPositionAlertSounds(MinecraftClient client) {
        if (pendingStartPositionAlertSounds <= 0) {
            return;
        }
        if (pendingStartPositionAlertSoundDelayTicks > 0) {
            pendingStartPositionAlertSoundDelayTicks--;
            return;
        }

        playStartPositionAlertSound(client);
        pendingStartPositionAlertSounds--;
        pendingStartPositionAlertSoundDelayTicks = START_POSITION_ALERT_SOUND_DELAY_TICKS;
    }

    private static void playStartPositionAlertSound(MinecraftClient client) {
        if (client != null && client.player != null) {
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0F, 1.4F);
        }
    }

    private static String formatPosition(Vec3d position) {
        return String.format(Locale.ROOT, "X %.1f / Y %.1f / Z %.1f", Double.valueOf(position.x), Double.valueOf(position.y), Double.valueOf(position.z));
    }

    private static void tickAutoMove(MinecraftClient client, HaConfig config, long now) {
        if (!config.afkFarmingAutoMoveEnabled) {
            if (autoMoveStep != AUTO_MOVE_IDLE_STEP || autoMovePressedKey != InputUtil.UNKNOWN_KEY) {
                resetAutoMove(client, true);
            }
            nextAutoMoveMillis = 0L;
            return;
        }
        if (client.currentScreen != null) {
            if (autoMoveStep != AUTO_MOVE_IDLE_STEP) {
                resetAutoMove(client, true);
                scheduleNextAutoMove(config, now);
            }
            return;
        }
        if (autoMoveStep != AUTO_MOVE_IDLE_STEP) {
            tickAutoMoveSequence(client, config, now);
            return;
        }
        if (nextAutoMoveMillis <= 0L) {
            scheduleNextAutoMove(config, now);
            return;
        }
        if (now < nextAutoMoveMillis) {
            return;
        }
        if (monitoringStartPosition != null
            && client.player != null
            && client.player.getPos().squaredDistanceTo(monitoringStartPosition) >= START_POSITION_ALERT_DISTANCE_SQUARED) {
            scheduleNextAutoMove(config, now);
            return;
        }

        captureAutoMoveView(client);
        autoMoveStep = 0;
        tickAutoMoveSequence(client, config, now);
    }

    private static void tickAutoMoveSequence(MinecraftClient client, HaConfig config, long now) {
        if (autoMoveStepEndMillis > 0L && now < autoMoveStepEndMillis) {
            tickAutoMoveLookInterpolation(client, now);
            return;
        }
        if (autoMoveStepEndMillis > 0L) {
            tickAutoMoveLookInterpolation(client, autoMoveStepEndMillis);
        }

        releaseAutoMoveKeys(client);
        long durationMillis = getAutoMoveStepDurationMillis(autoMoveStep);
        if (durationMillis <= 0L) {
            resetAutoMove(client, true);
            scheduleNextAutoMove(config, now);
            return;
        }

        autoMoveStepStartMillis = now;
        if (isAutoMoveLookStep(autoMoveStep)) {
            applyAutoMoveLookStep(client, autoMoveStep);
        } else {
            KeyBinding nextKey = getAutoMoveStepKey(client, autoMoveStep);
            if (nextKey == null) {
                resetAutoMove(client, true);
                scheduleNextAutoMove(config, now);
                return;
            }
            pressSingleAutoMoveKey(client, nextKey);
        }
        autoMoveStepEndMillis = now + durationMillis;
        autoMoveStep++;
    }

    private static KeyBinding getAutoMoveStepKey(MinecraftClient client, int step) {
        if (client == null || client.options == null) {
            return null;
        }
        if (step == 0) {
            return client.options.keyLeft;
        }
        if (step == 1) {
            return client.options.keyBack;
        }
        if (step == 2) {
            return client.options.keyRight;
        }
        if (step == 3) {
            return client.options.keyBack;
        }
        return null;
    }

    private static long getAutoMoveStepDurationMillis(int step) {
        if (step == 0 || step == 2) {
            return AUTO_MOVE_LEFT_MILLIS;
        }
        if (step == 1 || step == 3) {
            return AUTO_MOVE_BACK_MILLIS;
        }
        if (step == AUTO_MOVE_LOOK_OFFSET_STEP || step == AUTO_MOVE_LOOK_RESTORE_STEP) {
            return AUTO_MOVE_LOOK_MILLIS;
        }
        return 0L;
    }

    private static boolean isAutoMoveLookStep(int step) {
        return step == AUTO_MOVE_LOOK_OFFSET_STEP || step == AUTO_MOVE_LOOK_RESTORE_STEP;
    }

    private static void captureAutoMoveView(MinecraftClient client) {
        if (client == null || client.player == null) {
            autoMoveViewCaptured = false;
            autoMoveViewAdjusted = false;
            return;
        }
        autoMoveStartYaw = client.player.getYaw(1.0F);
        autoMoveStartPitch = client.player.getPitch(1.0F);
        autoMoveLookYawDelta = randomSignedOffset(AUTO_MOVE_LOOK_YAW_DELTA);
        autoMoveLookPitchDelta = randomSignedOffset(AUTO_MOVE_LOOK_PITCH_DELTA);
        autoMoveViewCaptured = true;
        autoMoveViewAdjusted = false;
    }

    private static void applyAutoMoveLookStep(MinecraftClient client, int step) {
        if (client == null || client.player == null || !autoMoveViewCaptured) {
            return;
        }
        if (step == AUTO_MOVE_LOOK_OFFSET_STEP) {
            autoMoveLookFromYaw = client.player.getYaw(1.0F);
            autoMoveLookFromPitch = client.player.getPitch(1.0F);
            autoMoveLookToYaw = autoMoveStartYaw + autoMoveLookYawDelta;
            autoMoveLookToPitch = autoMoveStartPitch + autoMoveLookPitchDelta;
            autoMoveViewAdjusted = true;
        } else if (step == AUTO_MOVE_LOOK_RESTORE_STEP) {
            autoMoveLookFromYaw = client.player.getYaw(1.0F);
            autoMoveLookFromPitch = client.player.getPitch(1.0F);
            autoMoveLookToYaw = autoMoveStartYaw;
            autoMoveLookToPitch = autoMoveStartPitch;
        }
        tickAutoMoveLookInterpolation(client, autoMoveStepStartMillis);
    }

    private static float randomSignedOffset(float magnitude) {
        if (magnitude <= 0.0F) {
            return 0.0F;
        }
        return RANDOM.nextBoolean() ? magnitude : -magnitude;
    }

    private static void restoreAutoMoveView(MinecraftClient client) {
        if (client == null || client.player == null || !autoMoveViewCaptured) {
            autoMoveViewCaptured = false;
            autoMoveViewAdjusted = false;
            return;
        }
        setPlayerView(client, autoMoveStartYaw, autoMoveStartPitch);
        autoMoveViewAdjusted = false;
    }

    private static void tickAutoMoveLookInterpolation(MinecraftClient client, long now) {
        int activeStep = autoMoveStep - 1;
        if (!isAutoMoveLookStep(activeStep) || autoMoveStepEndMillis <= autoMoveStepStartMillis) {
            return;
        }
        double progress = (double) (now - autoMoveStepStartMillis) / (double) (autoMoveStepEndMillis - autoMoveStepStartMillis);
        float eased = (float) Math.max(0.0D, Math.min(1.0D, progress));
        float yaw = lerpAngle(autoMoveLookFromYaw, autoMoveLookToYaw, eased);
        float pitch = lerp(autoMoveLookFromPitch, autoMoveLookToPitch, eased);
        setPlayerView(client, yaw, pitch);
        if (activeStep == AUTO_MOVE_LOOK_RESTORE_STEP && eased >= 1.0F) {
            autoMoveViewAdjusted = false;
        }
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    private static float lerpAngle(float from, float to, float progress) {
        return from + wrapDegrees(to - from) * progress;
    }

    private static float wrapDegrees(float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    private static void setPlayerView(MinecraftClient client, float yaw, float pitch) {
        if (client == null || client.player == null) {
            return;
        }
        float clampedPitch = Math.max(-90.0F, Math.min(90.0F, pitch));
        client.player.yaw = yaw;
        client.player.prevYaw = yaw;
        client.player.headYaw = yaw;
        client.player.prevHeadYaw = yaw;
        client.player.bodyYaw = yaw;
        client.player.prevBodyYaw = yaw;
        client.player.pitch = clampedPitch;
        client.player.prevPitch = clampedPitch;
    }

    private static void pressSingleAutoMoveKey(MinecraftClient client, KeyBinding keyBinding) {
        releaseAutoMoveKeys(client);
        InputUtil.Key key = getBoundKey(keyBinding);
        if (key == InputUtil.UNKNOWN_KEY) {
            return;
        }
        autoMovePressedKey = key;
        KeyBinding.setKeyPressed(key, true);
        KeyBinding.onKeyPressed(key);
    }

    private static void releaseAutoMoveKeys(MinecraftClient client) {
        if (client != null && client.options != null) {
            releaseKeyBinding(client.options.keyForward);
            releaseKeyBinding(client.options.keyLeft);
            releaseKeyBinding(client.options.keyBack);
            releaseKeyBinding(client.options.keyRight);
        }
        if (autoMovePressedKey != InputUtil.UNKNOWN_KEY) {
            KeyBinding.setKeyPressed(autoMovePressedKey, false);
            autoMovePressedKey = InputUtil.UNKNOWN_KEY;
        }
    }

    private static void releaseKeyBinding(KeyBinding keyBinding) {
        InputUtil.Key key = getBoundKey(keyBinding);
        if (key != InputUtil.UNKNOWN_KEY) {
            KeyBinding.setKeyPressed(key, false);
        }
    }

    private static InputUtil.Key getBoundKey(KeyBinding keyBinding) {
        if (keyBinding == null) {
            return InputUtil.UNKNOWN_KEY;
        }
        return ((KeyBindingAccessor) keyBinding).ha$getBoundKey();
    }

    private static void scheduleNextAutoMove(HaConfig config, long now) {
        double jitterSeconds = Math.max(0.0D, config.afkFarmingAutoMoveJitterSeconds);
        double jitterMillis = jitterSeconds <= 0.0D ? 0.0D : (RANDOM.nextDouble() * jitterSeconds * 2.0D - jitterSeconds) * 1000.0D;
        long intervalMillis = Math.max(1000L, Math.round(config.afkFarmingAutoMoveIntervalSeconds * 1000.0D + jitterMillis));
        nextAutoMoveMillis = now + intervalMillis;
    }

    private static void resetAutoMove(MinecraftClient client, boolean releaseKeys) {
        if (releaseKeys) {
            releaseAutoMoveKeys(client);
        }
        if (autoMoveViewAdjusted) {
            restoreAutoMoveView(client);
        }
        autoMoveStep = AUTO_MOVE_IDLE_STEP;
        autoMoveStepStartMillis = 0L;
        autoMoveStepEndMillis = 0L;
        autoMoveLookYawDelta = 0.0F;
        autoMoveLookPitchDelta = 0.0F;
        autoMoveLookFromYaw = 0.0F;
        autoMoveLookFromPitch = 0.0F;
        autoMoveLookToYaw = 0.0F;
        autoMoveLookToPitch = 0.0F;
        autoMoveViewCaptured = false;
        autoMoveViewAdjusted = false;
    }

    private static void tickMobMacro(MinecraftClient client, HaTickHandler tickHandler, HaConfig config, long now) {
        if (!config.afkFarmingMobMacroEnabled || tickHandler == null || client.currentScreen != null) {
            mobMacroDraining = false;
            return;
        }
        if (config.swapEntries.isEmpty()) {
            mobMacroDraining = false;
            return;
        }
        if (!mobMacroDraining && now < nextMobMacroMillis) {
            return;
        }

        int macroIndex = Math.max(0, Math.min(config.afkFarmingMobMacroIndex, config.swapEntries.size() - 1));
        HaConfig.SwapEntry entry = config.swapEntries.get(macroIndex);
        ensureMobThreshold(config);
        MobDebugInfo info = collectMobDebugInfo(client, config);
        lastMobDebugInfo = info;
        int mobCount = info.mobCount;

        if (mobMacroDraining && mobCount <= 0) {
            mobMacroDraining = false;
            nextMobMacroMillis = now + Math.max(1L, Math.round(config.afkFarmingMobMacroCooldownSeconds * 1000.0D));
            nextMobMacroTriggerMillis = 0L;
            currentMobThreshold = rollMobThreshold(config);
            return;
        }

        if (!mobMacroDraining && mobCount < currentMobThreshold) {
            return;
        }

        mobMacroDraining = true;
        if (now < nextMobMacroTriggerMillis) {
            return;
        }
        if (tickHandler.triggerSwapEntry(client, entry)) {
            nextMobMacroTriggerMillis = now + Math.max(1L, Math.round(entry.intervalSeconds * 1000.0D));
        }
    }

    public static MobDebugInfo getMobDebugInfo(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || !HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            return MobDebugInfo.empty();
        }
        HaConfig config = HaConfig.get();
        if (!config.afkFarmingEnabled || !config.afkFarmingMobMacroEnabled) {
            return MobDebugInfo.empty();
        }
        MobDebugInfo info = collectMobDebugInfo(client, config);
        lastMobDebugInfo = info;
        return info;
    }

    public static MobDebugInfo getLastMobDebugInfo() {
        return lastMobDebugInfo;
    }

    public static boolean isRunning(HaConfig config) {
        return HaBuildFlags.DANGEROUS_FEATURES_ENABLED
            && config.afkFarmingEnabled
            && config.afkFarmingActive;
    }

    public static long getAutoMoveRemainingMillis() {
        long now = System.currentTimeMillis();
        if (autoMoveStep != AUTO_MOVE_IDLE_STEP && autoMoveStepEndMillis > now) {
            return autoMoveStepEndMillis - now;
        }
        if (nextAutoMoveMillis <= 0L) {
            return 0L;
        }
        return Math.max(0L, nextAutoMoveMillis - now);
    }

    public static boolean isAutoMoveRunningStep() {
        return autoMoveStep != AUTO_MOVE_IDLE_STEP;
    }

    private static MobDebugInfo collectMobDebugInfo(MinecraftClient client, HaConfig config) {
        Vec3d center = getMobCircleCenter(client);
        int threshold = currentMobThreshold;
        if (threshold < config.afkFarmingMobMinCount || threshold > config.afkFarmingMobMaxCount) {
            threshold = config.afkFarmingMobMinCount;
        }

        if (center == null) {
            return MobDebugInfo.empty();
        }

        int count = 0;
        int totalEntities = 0;
        int livingEntities = 0;
        int mobEntities = 0;
        Entity nearestEntity = null;
        double nearestDistanceSquared = Double.MAX_VALUE;
        for (Entity entity : client.world.getEntities()) {
            if (entity == null || entity == client.player || !isInsideMobCircle(entity, center)) {
                continue;
            }
            totalEntities++;
            if (entity instanceof LivingEntity) {
                livingEntities++;
            }
            if (entity instanceof MobEntity) {
                mobEntities++;
            }

            double distanceSquared = horizontalDistanceSquared(entity, center);
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearestEntity = entity;
            }

            if (entity instanceof LivingEntity) {
                count++;
            }
        }

        long now = System.currentTimeMillis();
        long cooldownMillis = mobMacroDraining
            ? Math.max(0L, nextMobMacroTriggerMillis - now)
            : Math.max(0L, nextMobMacroMillis - now);
        return new MobDebugInfo(
            true,
            center,
            MOB_CENTER_DISTANCE,
            Math.sqrt(MOB_RADIUS_SQUARED),
            count,
            threshold,
            cooldownMillis,
            mobMacroDraining,
            totalEntities,
            livingEntities,
            mobEntities,
            getEntityTypeName(nearestEntity),
            getEntityClassName(nearestEntity),
            nearestEntity == null ? -1.0D : Math.sqrt(nearestDistanceSquared)
        );
    }

    private static Vec3d getMobCircleCenter(MinecraftClient client) {
        if (client == null || client.player == null) {
            return null;
        }
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        Vec3d look = client.player.getRotationVec(1.0F);
        Vec3d projected = eye.add(look.multiply(MOB_CENTER_DISTANCE));
        return new Vec3d(projected.x, client.player.getY(), projected.z);
    }

    private static boolean isInsideMobCircle(Entity entity, Vec3d center) {
        return horizontalDistanceSquared(entity, center) <= MOB_RADIUS_SQUARED;
    }

    private static double horizontalDistanceSquared(Entity entity, Vec3d center) {
        double dx = entity.getX() - center.x;
        double dz = entity.getZ() - center.z;
        return dx * dx + dz * dz;
    }

    private static String getEntityTypeName(Entity entity) {
        if (entity == null || entity.getType() == null) {
            return "none";
        }
        return net.minecraft.entity.EntityType.getId(entity.getType()).toString();
    }

    private static String getEntityClassName(Entity entity) {
        return entity == null ? "none" : entity.getClass().getSimpleName();
    }

    private static String stripFormatting(String value) {
        if (value == null) {
            return "";
        }
        String stripped = Formatting.strip(value);
        return stripped == null ? value.trim() : stripped.trim();
    }

    private static boolean containsFormattingCode(String value) {
        return value != null && value.indexOf('\u00a7') >= 0;
    }

    private static void maybeAlert(MinecraftClient client, HaConfig config, String key, String title, String detail, long now) {
        long cooldownMillis = 60_000L;
        Long last = LAST_ALERT_MILLIS.get(key);
        if (last != null && now - last.longValue() < cooldownMillis) {
            return;
        }

        LAST_ALERT_MILLIS.put(key, Long.valueOf(now));
        client.inGameHud.setTitles(new LiteralText("\u00a7c" + title), new LiteralText("\u00a7e" + detail), 5, 40, 10);
        client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0F, 0.7F);
        sendWebhook(config.afkFarmingWebhookUrl, title + ": " + detail);
    }

    private static String buildStatusMessage(HaConfig config) {
        StringBuilder message = new StringBuilder();
        message.append("========== HashimotoAddons: AFK Farming Report ==========\n");
        message.append("Current EXP: ").append(config.expTrackerTotal).append('\n');
        message.append("Estimated EXP/hour: ").append(HaExpTracker.getExpPerHour()).append('\n');
        message.append("Exp Timer: ").append(formatDuration(HaExpTracker.getElapsedSeconds())).append('\n');
        message.append("Profit: ").append(HaDropTracker.getEstimatedProfit()).append(" Intercoins\n");
        message.append("Estimated Profit/hour: ").append(HaDropTracker.getProfitPerHour()).append(" Intercoins\n");
        message.append("Drop Timer: ").append(formatDuration(HaDropTracker.getElapsedSeconds())).append('\n');
        message.append("=========================================================");
        return message.toString();
    }

    private static String formatDuration(long seconds) {
        long safeSeconds = Math.max(0L, seconds);
        long hours = safeSeconds / 3600L;
        long minutes = (safeSeconds % 3600L) / 60L;
        long rest = safeSeconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", Long.valueOf(hours), Long.valueOf(minutes), Long.valueOf(rest));
        }
        return String.format(Locale.ROOT, "%d:%02d", Long.valueOf(minutes), Long.valueOf(rest));
    }

    private static void sendWebhook(String webhookUrl, String content) {
        sendWebhook(webhookUrl, content, false);
    }

    private static void sendWebhook(String webhookUrl, String content, boolean notifyPlayer) {
        String normalizedUrl = webhookUrl == null ? "" : webhookUrl.trim();
        if (normalizedUrl.isEmpty() || content == null || content.trim().isEmpty()) {
            lastWebhookStatus = "Skipped: empty webhook URL or content";
            if (notifyPlayer) {
                sendClientMessage(MinecraftClient.getInstance(), "\u00a7cWebhook URL is empty.");
            }
            return;
        }

        WEBHOOK_EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(normalizedUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setDoOutput(true);
                connection.setRequestProperty("User-Agent", "HashimotoAddons/1.1.4");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] body = ("{\"content\":\"" + escapeJson(content) + "\"}").getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(body.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body);
                }
                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    lastWebhookStatus = "OK: HTTP " + responseCode;
                    if (notifyPlayer) {
                        sendClientMessage(MinecraftClient.getInstance(), "\u00a7aWebhook sent. HTTP " + responseCode);
                    }
                } else {
                    lastWebhookStatus = "Failed: HTTP " + responseCode;
                    if (notifyPlayer) {
                        sendClientMessage(MinecraftClient.getInstance(), "\u00a7cWebhook failed. HTTP " + responseCode);
                    }
                }
            } catch (Exception exception) {
                lastWebhookStatus = "Error: " + exception.getClass().getSimpleName() + " " + exception.getMessage();
                if (notifyPlayer) {
                    sendClientMessage(MinecraftClient.getInstance(), "\u00a7cWebhook error: " + exception.getClass().getSimpleName());
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private static void sendClientMessage(MinecraftClient client, String message) {
        if (client == null) {
            return;
        }
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(new LiteralText("[\u00a7l\u00a7bHashimotoAddons\u00a7r]:" + message), false);
            }
        });
    }

    private static String escapeJson(String value) {
        StringBuilder result = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\' || ch == '"') {
                result.append('\\').append(ch);
            } else if (ch == '\n') {
                result.append("\\n");
            } else if (ch == '\r') {
                result.append("\\r");
            } else if (ch == '\t') {
                result.append("\\t");
            } else if (ch < 0x20) {
                result.append(' ');
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private static void ensureMobThreshold(HaConfig config) {
        if (currentMobThreshold < config.afkFarmingMobMinCount || currentMobThreshold > config.afkFarmingMobMaxCount) {
            currentMobThreshold = rollMobThreshold(config);
        }
    }

    private static int rollMobThreshold(HaConfig config) {
        int min = Math.max(1, config.afkFarmingMobMinCount);
        int max = Math.max(min, config.afkFarmingMobMaxCount);
        return min + RANDOM.nextInt(max - min + 1);
    }

    private static void resetRuntime(boolean clearAlerts) {
        resetAutoMove(MinecraftClient.getInstance(), autoMoveStep != AUTO_MOVE_IDLE_STEP || autoMovePressedKey != InputUtil.UNKNOWN_KEY);
        wasRunning = false;
        lastReportMillis = 0L;
        nextMobMacroMillis = 0L;
        nextMobMacroTriggerMillis = 0L;
        nextAutoMoveMillis = 0L;
        currentMobThreshold = -1;
        lastMobDebugInfo = MobDebugInfo.empty();
        mobMacroDraining = false;
        monitoringStartPosition = null;
        monitoringStartPositionAlerted = false;
        pendingStartPositionAlertSounds = 0;
        pendingStartPositionAlertSoundDelayTicks = 0;
        if (clearAlerts) {
            LAST_ALERT_MILLIS.clear();
        }
    }

    public static final class MobDebugInfo {
        public final boolean available;
        public final Vec3d center;
        public final double centerDistance;
        public final double radius;
        public final int mobCount;
        public final int threshold;
        public final long cooldownMillis;
        public final boolean draining;
        public final int totalEntities;
        public final int livingEntities;
        public final int mobEntities;
        public final String nearestEntityType;
        public final String nearestEntityClass;
        public final double nearestEntityDistance;

        private MobDebugInfo(
            boolean available,
            Vec3d center,
            double centerDistance,
            double radius,
            int mobCount,
            int threshold,
            long cooldownMillis,
            boolean draining,
            int totalEntities,
            int livingEntities,
            int mobEntities,
            String nearestEntityType,
            String nearestEntityClass,
            double nearestEntityDistance
        ) {
            this.available = available;
            this.center = center;
            this.centerDistance = centerDistance;
            this.radius = radius;
            this.mobCount = mobCount;
            this.threshold = threshold;
            this.cooldownMillis = cooldownMillis;
            this.draining = draining;
            this.totalEntities = totalEntities;
            this.livingEntities = livingEntities;
            this.mobEntities = mobEntities;
            this.nearestEntityType = nearestEntityType;
            this.nearestEntityClass = nearestEntityClass;
            this.nearestEntityDistance = nearestEntityDistance;
        }

        static MobDebugInfo empty() {
            return new MobDebugInfo(false, null, MOB_CENTER_DISTANCE, Math.sqrt(MOB_RADIUS_SQUARED), 0, 0, 0L, false, 0, 0, 0, "none", "none", -1.0D);
        }
    }
}
