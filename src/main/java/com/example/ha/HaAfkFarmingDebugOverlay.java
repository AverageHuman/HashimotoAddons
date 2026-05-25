package com.example.ha;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

public final class HaAfkFarmingDebugOverlay extends DrawableHelper {
    private HaAfkFarmingDebugOverlay() {
    }

    public static void render(MatrixStack matrices, float tickDelta) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        HaConfig config = HaConfig.get();
        if (client == null
            || client.player == null
            || client.world == null
            || !config.afkFarmingEnabled
            || !config.afkFarmingMobMacroEnabled
            || !config.afkFarmingMobDebugHudEnabled) {
            return;
        }

        HaAfkFarming.MobDebugInfo info = HaAfkFarming.getLastMobDebugInfo();
        if (!info.available) {
            info = HaAfkFarming.getMobDebugInfo(client);
        }
        if (!info.available) {
            return;
        }

        int x = 8;
        int y = 160;
        int width = 230;
        int height = 94;
        fill(matrices, x - 2, y - 2, x + width, y + height, 0x88000000);
        client.textRenderer.drawWithShadow(matrices, "AFK Living Test", x + 3, y + 3, 0xFFFFFF);
        client.textRenderer.drawWithShadow(matrices, "Count: " + info.mobCount + " / " + info.threshold, x + 3, y + 15, info.mobCount >= info.threshold ? 0x55FF55 : 0xFFD166);
        client.textRenderer.drawWithShadow(matrices, "Circle: front " + trim(info.centerDistance) + " r" + trim(info.radius), x + 3, y + 27, 0xA0E8FF);
        client.textRenderer.drawWithShadow(matrices, "CD: " + formatCooldown(info.cooldownMillis), x + 3, y + 39, info.cooldownMillis <= 0L ? 0x55FF55 : 0xFFAA55);
        client.textRenderer.drawWithShadow(matrices, "Inside: total " + info.totalEntities + " living " + info.livingEntities + " mob " + info.mobEntities, x + 3, y + 51, 0xFFFFFF);
        client.textRenderer.drawWithShadow(matrices, "Nearest: " + shorten(info.nearestEntityType, 26), x + 3, y + 63, 0xFFD166);
        client.textRenderer.drawWithShadow(matrices, "Class: " + shorten(info.nearestEntityClass, 27), x + 3, y + 75, 0xA0A0A0);
    }

    private static String formatCooldown(long cooldownMillis) {
        if (cooldownMillis <= 0L) {
            return "Ready";
        }
        return String.format(java.util.Locale.ROOT, "%.1fs", Double.valueOf(cooldownMillis / 1000.0D));
    }

    private static String trim(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", Double.valueOf(value));
    }

    private static String shorten(String value, int maxLength) {
        if (value == null) {
            return "none";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
