package com.example.ha;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;

public final class HaTriggerBot {
    private static final double HEALTH_THRESHOLD = 50000.0D;
    private static long nextTriggerMillis;

    private HaTriggerBot() {
    }

    public static void tick(MinecraftClient client, HaTickHandler tickHandler) {
        HaConfig config = HaConfig.get();
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED
            || client == null
            || client.player == null
            || client.world == null
            || !config.triggerBotEnabled) {
            resetRuntime();
            return;
        }

        if (client.currentScreen != null) {
            return;
        }

        if (config.swapEntries.isEmpty()) {
            resetRuntime();
            return;
        }

        LivingEntity target = HaMobTargeting.findTarget(client);
        if (target == null) {
            resetRuntime();
            return;
        }

        double health = target.getHealth();
        if (health <= HEALTH_THRESHOLD) {
            resetRuntime();
            return;
        }

        long now = System.currentTimeMillis();
        if (now < nextTriggerMillis) {
            return;
        }

        int macroIndex = Math.max(0, Math.min(config.triggerBotMacroIndex, config.swapEntries.size() - 1));
        HaConfig.SwapEntry entry = config.swapEntries.get(macroIndex);
        if (tickHandler.triggerSwapEntry(client, entry)) {
            nextTriggerMillis = now + Math.max(1L, Math.round(config.triggerBotCooldownSeconds * 1000.0D));
        }
    }

    public static void onDisconnected() {
        resetRuntime();
    }

    private static void resetRuntime() {
        nextTriggerMillis = 0L;
    }
}
