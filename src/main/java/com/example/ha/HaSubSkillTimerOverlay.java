package com.example.ha;

import java.text.DecimalFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

public final class HaSubSkillTimerOverlay {
    private static final DecimalFormat SECONDS_FORMAT = new DecimalFormat("0.#");
    private static final int BAR_HEIGHT = 4;

    private HaSubSkillTimerOverlay() {
    }

    public static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        HaConfig config = HaConfig.get();
        if (client == null || HaHudVisibility.shouldHideHashimotoHud(client) || !config.subSkillTimerEnabled || !HaSubSkillTimer.isActive()) {
            return;
        }
        drawPanel(matrices, config.subSkillTimerOverlayX, config.subSkillTimerOverlayY, HaSubSkillTimer.getRemainingMillis(), HaSubSkillTimer.getRemainingRatio(), config.subSkillTimerSlim);
    }

    public static void drawPreview(MatrixStack matrices, int x, int y, boolean selected) {
        HaConfig config = HaConfig.get();
        drawPanel(matrices, x, y, 10300L, 0.68F, config.subSkillTimerSlim);
        if (selected) {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, "\u25c0", x - 8, y + 4, 0xFFFFFF);
        }
    }

    public static int getPanelWidth(MinecraftClient client) {
        if (HaConfig.get().subSkillTimerSlim) {
            return client.textRenderer.getWidth(getSlimText(10300L));
        }
        return 132;
    }

    public static int getPanelHeight() {
        return HaConfig.get().subSkillTimerSlim ? 10 : 42;
    }

    private static void drawPanel(MatrixStack matrices, int x, int y, long remainingMillis, float ratio, boolean slim) {
        MinecraftClient client = MinecraftClient.getInstance();
        String time = formatSeconds(remainingMillis);
        int color = timerColor(ratio);
        if (slim) {
            client.textRenderer.drawWithShadow(matrices, getSlimText(remainingMillis), x, y, color);
            return;
        }

        int width = 132;
        int height = 42;
        DrawableHelper.fill(matrices, x, y, x + width, y + height, 0x90000000);
        DrawableHelper.fill(matrices, x, y, x + width, y + 1, color | 0xFF000000);
        client.textRenderer.drawWithShadow(matrices, "Sub Skill Timer", x + 5, y + 4, 0xFFFFFF);
        client.textRenderer.drawWithShadow(matrices, "Ready in: " + time + "s", x + 5, y + 17, color);

        int barX = x + 5;
        int barY = y + height - BAR_HEIGHT - 6;
        int barWidth = width - 10;
        DrawableHelper.fill(matrices, barX, barY, barX + barWidth, barY + BAR_HEIGHT, 0xAA202020);
        DrawableHelper.fill(matrices, barX, barY, barX + Math.round(barWidth * ratio), barY + BAR_HEIGHT, color | 0xFF000000);
    }

    private static String getSlimText(long remainingMillis) {
        return "Sub: " + formatSeconds(remainingMillis) + "s";
    }

    private static String formatSeconds(long remainingMillis) {
        return SECONDS_FORMAT.format(Math.max(0.0D, remainingMillis / 1000.0D));
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
