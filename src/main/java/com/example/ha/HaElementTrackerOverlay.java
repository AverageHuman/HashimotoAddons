package com.example.ha;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

public final class HaElementTrackerOverlay {
    private HaElementTrackerOverlay() {
    }

    public static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || HaHudVisibility.shouldHideHashimotoHud(client) || !HaConfig.get().elementTrackerEnabled) {
            return;
        }
        drawPanel(matrices, HaConfig.get().elementTrackerOverlayX, HaConfig.get().elementTrackerOverlayY, HaElementTracker.getEnabledEntries(), false);
    }

    public static void drawPreview(MatrixStack matrices, int x, int y, boolean selected) {
        List<HaElementTracker.ElementHudEntry> previewEntries = new ArrayList<HaElementTracker.ElementHudEntry>();
        previewEntries.add(new HaElementTracker.ElementHudEntry(HaElementTracker.ElementType.FIRE, HaElementTracker.ElementRank.LEGENDARY, 39, "12:34"));
        previewEntries.add(new HaElementTracker.ElementHudEntry(HaElementTracker.ElementType.WATER, HaElementTracker.ElementRank.UNTOUCHABLE, 82, "4:05"));
        previewEntries.add(new HaElementTracker.ElementHudEntry(HaElementTracker.ElementType.SPACETIME, HaElementTracker.ElementRank.UNIQUE, 100, "Done"));
        drawPanel(matrices, x, y, previewEntries, true);
        if (selected) {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, "\u25c0", x - 8, y + 4, 0xFFFFFF);
        }
    }

    public static int getPanelWidth(MinecraftClient client) {
        List<HaElementTracker.ElementHudEntry> entries = HaElementTracker.getEnabledEntries();
        if (entries.isEmpty()) {
            entries = new ArrayList<HaElementTracker.ElementHudEntry>();
            entries.add(new HaElementTracker.ElementHudEntry(HaElementTracker.ElementType.FIRE, HaElementTracker.ElementRank.LEGENDARY, 39, "12:34"));
        }

        HaConfig config = HaConfig.get();
        int width = Math.max(160, 16 + client.textRenderer.getWidth("Status: " + HaElementTracker.getStatusText()));
        if (config.elementTrackerShowTimer) {
            width = Math.max(width, 16 + client.textRenderer.getWidth("Timer: " + HaExpTrackerOverlay.formatDuration(HaElementTracker.getElapsedSeconds())));
        }
        for (HaElementTracker.ElementHudEntry entry : entries) {
            width = Math.max(width, 16 + client.textRenderer.getWidth(entry.getDisplayText()));
        }
        return width;
    }

    public static int getPanelHeight() {
        int lines = 2 + Math.max(1, HaElementTracker.getEnabledEntries().size());
        if (HaConfig.get().elementTrackerShowTimer) {
            lines++;
        }
        return 10 + lines * 14;
    }

    private static void drawPanel(MatrixStack matrices, int x, int y, List<HaElementTracker.ElementHudEntry> entries, boolean preview) {
        MinecraftClient client = MinecraftClient.getInstance();
        int width = preview ? Math.max(220, getPreviewWidth(client, entries)) : getPanelWidth(client);
        int height = preview ? getPreviewHeight(entries) : getPanelHeight();

        DrawableHelper.fill(matrices, x, y, x + width, y + height, 0x90000000);
        DrawableHelper.fill(matrices, x, y, x + width, y + 1, 0xFF70E0FF);
        client.textRenderer.drawWithShadow(matrices, "Element Tracker", x + 5, y + 4, 0xFFFFFF);

        int rowY = y + 18;
        int statusColor = HaElementTracker.isActiveSession() ? 0x55FF55 : 0xFF5555;
        client.textRenderer.drawWithShadow(matrices, "Status: " + (preview ? "Tracking" : HaElementTracker.getStatusText()), x + 5, rowY, statusColor);
        rowY += 14;

        if (preview ? HaConfig.get().elementTrackerShowTimer : HaConfig.get().elementTrackerShowTimer) {
            long seconds = preview ? 3723L : HaElementTracker.getElapsedSeconds();
            client.textRenderer.drawWithShadow(matrices, "Timer: " + HaExpTrackerOverlay.formatDuration(seconds), x + 5, rowY, 0xA0E8FF);
            rowY += 14;
        }

        if (entries.isEmpty()) {
            client.textRenderer.drawWithShadow(matrices, "No targets enabled", x + 5, rowY, 0xFFD166);
            return;
        }

        for (HaElementTracker.ElementHudEntry entry : entries) {
            client.textRenderer.drawWithShadow(matrices, entry.getDisplayText(), x + 5, rowY, 0xFFD166);
            rowY += 14;
        }
    }

    private static int getPreviewWidth(MinecraftClient client, List<HaElementTracker.ElementHudEntry> entries) {
        int width = Math.max(160, 16 + client.textRenderer.getWidth("Status: Tracking"));
        if (HaConfig.get().elementTrackerShowTimer) {
            width = Math.max(width, 16 + client.textRenderer.getWidth("Timer: 1:02:03"));
        }
        for (HaElementTracker.ElementHudEntry entry : entries) {
            width = Math.max(width, 16 + client.textRenderer.getWidth(entry.getDisplayText()));
        }
        return width;
    }

    private static int getPreviewHeight(List<HaElementTracker.ElementHudEntry> entries) {
        int lines = 2 + Math.max(1, entries.size());
        if (HaConfig.get().elementTrackerShowTimer) {
            lines++;
        }
        return 10 + lines * 14;
    }
}
