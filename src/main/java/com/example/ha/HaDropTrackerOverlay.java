package com.example.ha;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;

public final class HaDropTrackerOverlay {
    private static final int MAX_VISIBLE_ITEMS = 5;
    private static final DecimalFormat COMPACT_FORMAT = new DecimalFormat("0.#");

    private HaDropTrackerOverlay() {
    }

    public static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || HaHudVisibility.shouldHideHashimotoHud(client) || !HaConfig.get().dropTrackerEnabled) {
            return;
        }
        drawPanel(matrices, HaConfig.get().dropTrackerOverlayX, HaConfig.get().dropTrackerOverlayY, HaDropTracker.getEntries(), false);
    }

    public static void drawPreview(MatrixStack matrices, int x, int y, boolean selected) {
        List<HaDropTracker.DropEntry> entries = new java.util.ArrayList<HaDropTracker.DropEntry>();
        entries.add(new HaDropTracker.DropEntry("minecraft:diamond", new ItemStack(Items.DIAMOND), new LiteralText("\u00a7b\u9280\u8ca8"), "\u9280\u8ca8", 12));
        entries.add(new HaDropTracker.DropEntry("minecraft:emerald", new ItemStack(Items.EMERALD), new LiteralText("\u00a7e\u91d1\u8ca8"), "\u91d1\u8ca8", 4));
        drawPanel(matrices, x, y, entries, true);
        if (selected) {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, "\u25c0", x - 8, y + 4, 0xFFFFFF);
        }
    }

    public static int getPanelWidth(MinecraftClient client) {
        int width = 132;
        HaConfig config = HaConfig.get();
        width = Math.max(width, 16 + client.textRenderer.getWidth("Status: " + statusText()));
        for (HaDropTracker.DropEntry entry : HaDropTracker.getEntries()) {
            width = Math.max(width, 48 + client.textRenderer.getWidth(entry.displayName) + client.textRenderer.getWidth("x" + entry.count));
        }
        width = Math.max(width, 16 + client.textRenderer.getWidth("Est.Profit: " + formatProfit(HaDropTracker.getEstimatedProfit(), config.dropTrackerCompactNumbers) + " Intercoins."));
        if (config.dropTrackerShowTimer) {
            width = Math.max(width, 16 + client.textRenderer.getWidth("Timer: " + HaExpTrackerOverlay.formatDuration(HaDropTracker.getElapsedSeconds())));
        }
        if (config.dropTrackerShowHourlyProfit) {
            width = Math.max(width, 16 + client.textRenderer.getWidth("Profit/hour: " + formatProfit(HaDropTracker.getProfitPerHour(), config.dropTrackerCompactNumbers)));
        }
        return Math.min(240, width);
    }

    public static int getPanelHeight() {
        int rows = Math.max(1, Math.min(MAX_VISIBLE_ITEMS, Math.max(1, HaDropTracker.getEntries().size())));
        HaConfig config = HaConfig.get();
        int footerRows = 2;
        if (config.dropTrackerShowTimer) {
            footerRows++;
        }
        if (config.dropTrackerShowHourlyProfit) {
            footerRows++;
        }
        return 14 + rows * 20 + 4 + footerRows * 12;
    }

    private static void drawPanel(MatrixStack matrices, int x, int y, List<HaDropTracker.DropEntry> entries, boolean preview) {
        MinecraftClient client = MinecraftClient.getInstance();
        int rows = Math.max(1, Math.min(MAX_VISIBLE_ITEMS, entries.size()));
        int width = getPanelWidth(client);
        int height = getPanelHeight();
        int footerTop = y + 16 + rows * 20 + 4;
        DrawableHelper.fill(matrices, x, y, x + width, y + height, 0x90000000);
        DrawableHelper.fill(matrices, x, y, x + width, y + 1, 0xFF70E000);
        client.textRenderer.drawWithShadow(matrices, "Drop Tracker", x + 5, y + 4, 0xFFFFFF);
        int statusColor = HaDropTracker.isActiveSession() ? 0x55FF55 : 0xFF5555;
        DrawableHelper.fill(matrices, x + 4, footerTop - 3, x + width - 4, footerTop - 2, 0x55333333);
        client.textRenderer.drawWithShadow(matrices, "Status: " + statusText(), x + 5, footerTop, statusColor);

        if (entries.isEmpty()) {
            client.textRenderer.drawWithShadow(matrices, "No drops", x + 5, y + 21, 0xA0A0A0);
            drawFooter(matrices, x, footerTop + 12, preview);
            return;
        }

        ItemRenderer renderer = client.getItemRenderer();
        for (int i = 0; i < rows; i++) {
            HaDropTracker.DropEntry entry = entries.get(i);
            int rowY = y + 16 + i * 20;
            renderer.renderInGuiWithOverrides(entry.displayStack, x + 5, rowY);
            renderer.renderGuiItemOverlay(client.textRenderer, entry.displayStack, x + 5, rowY);
            client.textRenderer.drawWithShadow(matrices, entry.displayName, x + 26, rowY + 2, preview ? 0xD8FFE0 : 0xFFFFFF);
            client.textRenderer.drawWithShadow(matrices, "x" + entry.count, x + 26, rowY + 11, preview ? 0xD8FFE0 : 0xFFFFFF);
        }
        drawFooter(matrices, x, footerTop + 12, preview);
    }

    private static void drawFooter(MatrixStack matrices, int x, int y, boolean preview) {
        HaConfig config = HaConfig.get();
        long profit = preview ? 41200000L : HaDropTracker.getEstimatedProfit();
        MinecraftClient client = MinecraftClient.getInstance();
        client.textRenderer.drawWithShadow(matrices, "Est.Profit: " + formatProfit(profit, config.dropTrackerCompactNumbers) + " Intercoins.", x + 5, y, 0xFFD166);
        y += 12;
        if (config.dropTrackerShowTimer) {
            long seconds = preview ? 3723L : HaDropTracker.getElapsedSeconds();
            client.textRenderer.drawWithShadow(matrices, "Timer: " + HaExpTrackerOverlay.formatDuration(seconds), x + 5, y, 0xA0E8FF);
            y += 12;
        }
        if (config.dropTrackerShowHourlyProfit) {
            long hourlyProfit = preview ? 43500000L : HaDropTracker.getProfitPerHour();
            client.textRenderer.drawWithShadow(matrices, "Profit/hour: " + formatProfit(hourlyProfit, config.dropTrackerCompactNumbers), x + 5, y, 0x55FF55);
        }
    }

    private static int getFooterHeight() {
        HaConfig config = HaConfig.get();
        int rows = 2;
        if (config.dropTrackerShowTimer) {
            rows++;
        }
        if (config.dropTrackerShowHourlyProfit) {
            rows++;
        }
        return rows * 12;
    }

    private static String statusText() {
        return HaDropTracker.isActiveSession() ? "Tracking" : "Stopped";
    }

    private static String formatProfit(long value, boolean compact) {
        long safeValue = Math.max(0L, value);
        if (!compact) {
            return NumberFormat.getIntegerInstance(Locale.US).format(safeValue);
        }
        if (safeValue >= 1000000000L) {
            return COMPACT_FORMAT.format(safeValue / 1000000000.0D) + "b";
        }
        if (safeValue >= 1000000L) {
            return COMPACT_FORMAT.format(safeValue / 1000000.0D) + "m";
        }
        if (safeValue >= 1000L) {
            return COMPACT_FORMAT.format(safeValue / 1000.0D) + "k";
        }
        return Long.toString(safeValue);
    }
}
