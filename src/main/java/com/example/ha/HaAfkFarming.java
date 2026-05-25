package com.example.ha;

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
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Vec3d;

public final class HaAfkFarming {
    private static final double MOB_CENTER_DISTANCE = 3.0D;
    private static final double MOB_RADIUS_SQUARED = 3.0D * 3.0D;
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
    private static int currentMobThreshold = -1;

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
        }

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

    private static void tickPlayerAlerts(MinecraftClient client, HaConfig config, long now) {
        if (!config.afkFarmingPlayerAlertsEnabled) {
            return;
        }

        for (AbstractClientPlayerEntity other : client.world.getPlayers()) {
            if (other == null || other == client.player) {
                continue;
            }
            String name = other.getGameProfile() == null ? other.getEntityName() : other.getGameProfile().getName();
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
            String name = entry.getProfile().getName();
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

    private static void tickMobMacro(MinecraftClient client, HaTickHandler tickHandler, HaConfig config, long now) {
        if (!config.afkFarmingMobMacroEnabled || tickHandler == null || client.currentScreen != null) {
            return;
        }
        if (now < nextMobMacroMillis || config.swapEntries.isEmpty()) {
            return;
        }

        int macroIndex = Math.max(0, Math.min(config.afkFarmingMobMacroIndex, config.swapEntries.size() - 1));
        HaConfig.SwapEntry entry = config.swapEntries.get(macroIndex);
        ensureMobThreshold(config);
        int mobCount = countMobsInFront(client);
        if (mobCount < currentMobThreshold) {
            return;
        }

        tickHandler.triggerSwapEntry(client, entry);
        nextMobMacroMillis = now + Math.max(1L, Math.round(config.afkFarmingMobMacroCooldownSeconds * 1000.0D));
        currentMobThreshold = rollMobThreshold(config);
    }

    private static int countMobsInFront(MinecraftClient client) {
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        Vec3d look = client.player.getRotationVec(1.0F);
        Vec3d center = eye.add(look.multiply(MOB_CENTER_DISTANCE));
        int count = 0;
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof MobEntity) || !entity.isAlive()) {
                continue;
            }
            if (entity.squaredDistanceTo(center) <= MOB_RADIUS_SQUARED) {
                count++;
            }
        }
        return count;
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
        message.append("AFK Farming Status\n");
        message.append("Exp: ").append(config.expTrackerTotal).append('\n');
        message.append("Exp/hour: ").append(HaExpTracker.getExpPerHour()).append('\n');
        message.append("Exp Timer: ").append(formatDuration(HaExpTracker.getElapsedSeconds())).append('\n');
        message.append("Profit: ").append(HaDropTracker.getEstimatedProfit()).append(" Intercoins\n");
        message.append("Profit/hour: ").append(HaDropTracker.getProfitPerHour()).append('\n');
        message.append("Drop Timer: ").append(formatDuration(HaDropTracker.getElapsedSeconds()));
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
        String normalizedUrl = webhookUrl == null ? "" : webhookUrl.trim();
        if (normalizedUrl.isEmpty() || content == null || content.trim().isEmpty()) {
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
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] body = ("{\"content\":\"" + escapeJson(content) + "\"}").getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(body.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body);
                }
                connection.getResponseCode();
            } catch (Exception ignored) {
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
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
        wasRunning = false;
        lastReportMillis = 0L;
        nextMobMacroMillis = 0L;
        currentMobThreshold = -1;
        if (clearAlerts) {
            LAST_ALERT_MILLIS.clear();
        }
    }
}
