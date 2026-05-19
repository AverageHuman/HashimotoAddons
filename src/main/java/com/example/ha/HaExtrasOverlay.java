package com.example.ha;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public final class HaExtrasOverlay {
    private HaExtrasOverlay() {
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
        if (!config.extrasHudEnabled) {
            return;
        }

        drawPanel(matrices, config.extrasHudX, config.extrasHudY, false);
    }

    public static void drawPreview(MatrixStack matrices, int x, int y, boolean selected) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            return;
        }

        drawPanel(matrices, x, y, true);
        if (selected) {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, "<", x - 8, y + 18, 0xFFFFFF);
        }
    }

    public static int getPanelWidth(MinecraftClient client) {
        int width = 136;
        width = Math.max(width, 42 + client.textRenderer.getWidth(HaGhostWall.getSelectedBlockName()));
        width = Math.max(width, 8 + client.textRenderer.getWidth("World: " + HaGhostWall.getCurrentWorldName(client)));
        return Math.min(240, width);
    }

    public static int getPanelHeight() {
        return 54;
    }

    private static void drawPanel(MatrixStack matrices, int x, int y, boolean preview) {
        MinecraftClient client = MinecraftClient.getInstance();
        int width = getPanelWidth(client);
        int height = getPanelHeight();
        DrawableHelper.fill(matrices, x, y, x + width, y + height, 0x90000000);
        DrawableHelper.fill(matrices, x, y, x + width, y + 1, 0xFFFFD166);
        client.textRenderer.drawWithShadow(matrices, "Extras", x + 5, y + 4, 0xFFFFFF);

        boolean editMode = preview || HaConfig.get().ghostWallEditMode;
        int editColor = editMode ? 0x55FF55 : 0xFF5555;
        client.textRenderer.drawWithShadow(matrices, "Edit Mode: " + (editMode ? "Enable" : "Disable"), x + 5, y + 16, editColor);
        client.textRenderer.drawWithShadow(matrices, "World: " + HaGhostWall.getCurrentWorldName(client), x + 5, y + 28, 0xA0E8FF);

        ItemRenderer renderer = client.getItemRenderer();
        ItemStack stack = new ItemStack(HaGhostWall.getSelectedBlock().asItem());
        renderer.renderInGuiWithOverrides(stack, x + 5, y + 37);
        client.textRenderer.drawWithShadow(matrices, HaGhostWall.getSelectedBlockName(), x + 26, y + 41, 0xFFFFFF);
    }
}
