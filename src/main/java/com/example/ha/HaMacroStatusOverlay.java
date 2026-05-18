package com.example.ha;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;

public final class HaMacroStatusOverlay {
    private HaMacroStatusOverlay() {
    }

    public static void render(MatrixStack matrices, float tickDelta) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || HaHudVisibility.shouldHideHashimotoHud(client)) {
            return;
        }

        HaConfig config = HaConfig.get();
        config.normalize();
        if (!config.macroStatusHudEnabled) {
            return;
        }

        drawText(matrices, config.macroStatusHudX, config.macroStatusHudY, config.macroEnabled);
    }

    public static void drawPreview(MatrixStack matrices, int x, int y, boolean enabled, boolean selected) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            return;
        }

        drawText(matrices, x, y, enabled);
        if (selected) {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, "\u25c0", x - 8, y, 0xFFFFFF);
        }
    }

    public static int getPanelWidth(MinecraftClient client) {
        return Math.max(client.textRenderer.getWidth("Macro: Enable"), client.textRenderer.getWidth("Macro: Disable"));
    }

    public static int getPanelHeight() {
        return 10;
    }

    private static void drawText(MatrixStack matrices, int x, int y, boolean enabled) {
        MinecraftClient client = MinecraftClient.getInstance();
        String value = enabled ? "Macro: Enable" : "Macro: Disable";
        int color = enabled ? 0x55FF55 : 0xFF5555;
        client.textRenderer.drawWithShadow(matrices, value, x, y, color);
    }
}
