package com.example.ha;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

public final class HaChunkChestOverlay {
    private HaChunkChestOverlay() {
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
        if (!config.chunkChestCounterEnabled) {
            return;
        }

        drawPanel(matrices, config.chunkChestOverlayX, config.chunkChestOverlayY, "Containers", Integer.toString(HaChunkChestCounter.countCurrentChunkChests(client)), 0xFFD166);
    }

    public static void drawPreview(MatrixStack matrices, int x, int y, boolean selected) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            return;
        }

        drawPanel(matrices, x, y, "Containers", "12", 0xFFD166);
        if (selected) {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, "\u25c0", x - 8, y + 2, 0xFFFFFF);
        }
    }

    public static int getPanelWidth(MinecraftClient client) {
        int textWidth = Math.max(client.textRenderer.getWidth("Containers"), client.textRenderer.getWidth("12"));
        return textWidth + 10;
    }

    public static int getPanelHeight() {
        return 20;
    }

    private static void drawPanel(MatrixStack matrices, int x, int y, String title, String value, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        int width = Math.max(client.textRenderer.getWidth(title), client.textRenderer.getWidth(value)) + 10;
        int height = getPanelHeight();
        DrawableHelper.fill(matrices, x, y, x + width, y + height, 0x90000000);
        DrawableHelper.fill(matrices, x, y, x + width, y + 1, color | 0xFF000000);
        client.textRenderer.drawWithShadow(matrices, title, x + 4, y + 3, 0xFFFFFF);
        client.textRenderer.drawWithShadow(matrices, value, x + 4, y + 11, color);
    }
}
