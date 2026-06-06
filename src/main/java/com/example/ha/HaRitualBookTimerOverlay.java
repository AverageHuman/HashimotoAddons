package com.example.ha;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

public final class HaRitualBookTimerOverlay {
    private HaRitualBookTimerOverlay() {
    }

    public static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        HaConfig config = HaConfig.get();
        if (client == null || HaHudVisibility.shouldHideHashimotoHud(client) || !config.ritualBookTimerEnabled || !HaRitualBookTimer.hasActiveTimers()) {
            return;
        }
        drawPanel(matrices, config.ritualBookTimerOverlayX, config.ritualBookTimerOverlayY, HaRitualBookTimer.getVisibleTimers(), config.ritualBookTimerSlim, false);
    }

    public static void drawPreview(MatrixStack matrices, int x, int y, boolean selected) {
        HaConfig config = HaConfig.get();
        drawPanel(matrices, x, y, HaRitualBookTimer.getPreviewTimers(), config.ritualBookTimerSlim, true);
        if (selected) {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, "\u25c0", x - 8, y + 4, 0xFFFFFF);
        }
    }

    public static int getPanelWidth(MinecraftClient client) {
        List<HaRitualBookTimer.RitualTimerView> entries = HaRitualBookTimer.hasActiveTimers()
            ? HaRitualBookTimer.getVisibleTimers()
            : HaRitualBookTimer.getPreviewTimers();
        if (HaConfig.get().ritualBookTimerSlim) {
            int width = 0;
            for (HaRitualBookTimer.RitualTimerView entry : entries) {
                width = Math.max(width, client.textRenderer.getWidth(entry.getDisplayText()));
            }
            return Math.max(width, client.textRenderer.getWidth("\u708e\u306e\u5100\u5f0f\u66f8\u7269 10:00"));
        }

        int width = 148;
        for (HaRitualBookTimer.RitualTimerView entry : entries) {
            width = Math.max(width, 16 + client.textRenderer.getWidth(entry.getDisplayText()));
        }
        return width;
    }

    public static int getPanelHeight() {
        int rows = Math.max(1, HaRitualBookTimer.hasActiveTimers() ? HaRitualBookTimer.getVisibleTimers().size() : HaRitualBookTimer.getPreviewTimers().size());
        if (HaConfig.get().ritualBookTimerSlim) {
            return rows * 10;
        }
        return 10 + rows * 14;
    }

    private static void drawPanel(MatrixStack matrices, int x, int y, List<HaRitualBookTimer.RitualTimerView> entries, boolean slim, boolean preview) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (slim) {
            int rowY = y;
            for (HaRitualBookTimer.RitualTimerView entry : entries) {
                client.textRenderer.drawWithShadow(matrices, entry.getDisplayText(), x, rowY, timerColor(entry.getRemainingRatio()));
                rowY += 10;
            }
            return;
        }

        int width = preview ? Math.max(196, getPanelWidth(client)) : getPanelWidth(client);
        int height = getPanelHeight();
        DrawableHelper.fill(matrices, x, y, x + width, y + height, 0x90000000);
        DrawableHelper.fill(matrices, x, y, x + width, y + 1, 0xFFB084F5);
        int rowY = y + 4;
        for (HaRitualBookTimer.RitualTimerView entry : entries) {
            client.textRenderer.drawWithShadow(matrices, entry.getDisplayText(), x + 5, rowY, timerColor(entry.getRemainingRatio()));
            rowY += 14;
        }
    }

    private static int timerColor(float ratio) {
        if (ratio > 0.66F) {
            return 0x55FFFF;
        }
        if (ratio > 0.33F) {
            return 0xFFD166;
        }
        return 0x55FF55;
    }
}
