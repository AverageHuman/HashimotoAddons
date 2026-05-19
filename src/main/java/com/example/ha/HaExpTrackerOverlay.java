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
        if (!HaExpTracker.isActiveSession()) {
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
        HaConfig config = HaConfig.get();
        int width = Math.max(116, 16 + client.textRenderer.getWidth("Total XP: " + formatNumber(config.expTrackerTotal)));
        if (config.expTrackerShowTimer) {
            width = Math.max(width, 16 + client.textRenderer.getWidth("Timer: " + formatDuration(HaExpTracker.getElapsedSeconds())));
        }
        if (config.expTrackerShowHourlyRate) {
            width = Math.max(width, 16 + client.textRenderer.getWidth("EXP/hour: " + formatNumber(HaExpTracker.getExpPerHour())));
        }
        return width;
    }

    public static int getPanelHeight() {
        HaConfig config = HaConfig.get();
        int lines = 2;
        if (config.expTrackerShowTimer) {
            lines++;
        }
        if (config.expTrackerShowHourlyRate) {
            lines++;
        }
        return 10 + lines * 14;
    }

    public static String formatNumber(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(Math.max(0L, value));
    }

    public static String formatDuration(long seconds) {
        long safeSeconds = Math.max(0L, seconds);
        long hours = safeSeconds / 3600L;
        long minutes = (safeSeconds % 3600L) / 60L;
        long remainingSeconds = safeSeconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, remainingSeconds);
        }
        return String.format(Locale.US, "%d:%02d", minutes, remainingSeconds);
    }

    private static void drawPanel(MatrixStack matrices, int x, int y, long total, boolean preview) {
        MinecraftClient client = MinecraftClient.getInstance();
        HaConfig config = HaConfig.get();
        int width = preview ? 132 : getPanelWidth(client);
        int height = getPanelHeight();
        DrawableHelper.fill(matrices, x, y, x + width, y + height, 0x90000000);
        DrawableHelper.fill(matrices, x, y, x + width, y + 1, 0xFF70E0FF);
        client.textRenderer.drawWithShadow(matrices, "Exp Tracker", x + 5, y + 4, 0xFFFFFF);
        int rowY = y + 18;
        client.textRenderer.drawWithShadow(matrices, "Total XP: " + formatNumber(total), x + 5, rowY, 0xFFD166);
        rowY += 14;
        if (config.expTrackerShowTimer) {
            long elapsedSeconds = preview ? 3723L : HaExpTracker.getElapsedSeconds();
            client.textRenderer.drawWithShadow(matrices, "Timer: " + formatDuration(elapsedSeconds), x + 5, rowY, 0xA0E8FF);
            rowY += 14;
        }
        if (config.expTrackerShowHourlyRate) {
            long rate = preview ? 123456L : HaExpTracker.getExpPerHour();
            client.textRenderer.drawWithShadow(matrices, "EXP/hour: " + formatNumber(rate), x + 5, rowY, 0x55FF55);
        }
    }
}
