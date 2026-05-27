package com.example.ha;

import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

public final class HaAfkFarmingAutoMoveOverlay extends DrawableHelper {
    private HaAfkFarmingAutoMoveOverlay() {
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
            || HaHudVisibility.shouldHideHashimotoHud(client)
            || !config.afkFarmingEnabled
            || !config.afkFarmingActive
            || !config.afkFarmingAutoMoveEnabled) {
            return;
        }

        long remainingMillis = HaAfkFarming.getAutoMoveRemainingMillis();
        String status = HaAfkFarming.isAutoMoveRunningStep() ? "Auto Move: Running" : "Auto Move: " + formatRemaining(remainingMillis);
        int color = HaAfkFarming.isAutoMoveRunningStep() ? 0x55FF55 : (remainingMillis <= 3000L ? 0xFFD166 : 0xFFFFFF);
        client.textRenderer.drawWithShadow(matrices, status, 8, 148, color);
    }

    private static String formatRemaining(long remainingMillis) {
        if (remainingMillis <= 0L) {
            return "Ready";
        }
        return String.format(Locale.ROOT, "%.1fs", Double.valueOf(remainingMillis / 1000.0D));
    }
}
