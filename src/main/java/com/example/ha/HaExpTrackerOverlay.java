package com.example.ha;

import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

public final class HaExpTrackerOverlay {
    private HaExpTrackerOverlay() {
    }

    public static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || HaHudVisibility.shouldHideHashimotoHud(client) || !HaConfig.get().expTrackerEnabled) {
            return;
        }
        drawPanel(matrices, HaConfig.get().expTrackerOverlayX, HaConfig.get().expTrackerOverlayY, HaConfig.get().expTrackerTotal, false);
    }

    public static void drawPreview(MatrixStack matrices, int x, int y, boolean selected) {
        drawPanel(matrices, x, y, 12345L, true);
        if (selected) {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, "\u25c0", x - 8, y + 4, 0xFFFFFF);
        }
    }

    public static int getPanelWidth(MinecraftClient client) {
        return Math.max(116, 16 + client.textRenderer.getWidth("Total XP: " + formatNumber(HaConfig.get().expTrackerTotal)));
    }

    public static int getPanelHeight() {
        return 34;
    }

    public static String formatNumber(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(Math.max(0L, value));
    }

    private static void drawPanel(MatrixStack matrices, int x, int y, long total, boolean preview) {
        MinecraftClient client = MinecraftClient.getInstance();
        int width = preview ? 132 : getPanelWidth(client);
        int height = getPanelHeight();
        DrawableHelper.fill(matrices, x, y, x + width, y + height, 0x90000000);
        DrawableHelper.fill(matrices, x, y, x + width, y + 1, 0xFF70E0FF);
        client.textRenderer.drawWithShadow(matrices, "Exp Tracker", x + 5, y + 4, 0xFFFFFF);
        client.textRenderer.drawWithShadow(matrices, "Total XP: " + formatNumber(total), x + 5, y + 18, 0xFFD166);
    }
}
