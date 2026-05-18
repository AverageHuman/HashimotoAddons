package com.example.ha;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;

public final class HaDropTrackerOverlay {
    private static final int MAX_VISIBLE_ITEMS = 5;

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
        for (HaDropTracker.DropEntry entry : HaDropTracker.getEntries()) {
            width = Math.max(width, 48 + client.textRenderer.getWidth(entry.displayName) + client.textRenderer.getWidth("x" + entry.count));
        }
        width = Math.max(width, 16 + client.textRenderer.getWidth("Est.Profit: " + HaDropTracker.getEstimatedProfit() + " Intercoins."));
        return Math.min(240, width);
    }

    public static int getPanelHeight() {
        int rows = Math.max(1, Math.min(MAX_VISIBLE_ITEMS, Math.max(1, HaDropTracker.getEntries().size())));
        return 14 + rows * 20 + 18;
    }

    private static void drawPanel(MatrixStack matrices, int x, int y, List<HaDropTracker.DropEntry> entries, boolean preview) {
        MinecraftClient client = MinecraftClient.getInstance();
        int rows = Math.max(1, Math.min(MAX_VISIBLE_ITEMS, entries.size()));
        int width = getPanelWidth(client);
        int height = 14 + rows * 20 + 18;
        DrawableHelper.fill(matrices, x, y, x + width, y + height, 0x90000000);
        DrawableHelper.fill(matrices, x, y, x + width, y + 1, 0xFF70E000);
        client.textRenderer.drawWithShadow(matrices, "Drop Tracker", x + 5, y + 4, 0xFFFFFF);

        if (entries.isEmpty()) {
            client.textRenderer.drawWithShadow(matrices, "No drops", x + 5, y + 21, 0xA0A0A0);
            drawProfit(matrices, x, y + height - 12, preview ? 412L : HaDropTracker.getEstimatedProfit());
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
        drawProfit(matrices, x, y + height - 12, preview ? 412L : HaDropTracker.getEstimatedProfit());
    }

    private static void drawProfit(MatrixStack matrices, int x, int y, long profit) {
        MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, "Est.Profit: " + profit + " Intercoins.", x + 5, y, 0xFFD166);
    }
}
