package com.example.ha;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class HaVerseDetectorOverlay {
    private static final int DISPLAY_TICKS = 45;
    private static final int PANEL_SIZE = 18;
    private static final int PANEL_PADDING = 6;
    private static DisplayState currentState;

    private HaVerseDetectorOverlay() {
    }

    public static void show(ItemStack stack, HaVerseClassifier.Result result, World world) {
        if (stack == null || stack.isEmpty() || result == null || world == null) {
            return;
        }

        ItemStack displayStack = stack.copy();
        displayStack.setCount(1);
        currentState = new DisplayState(displayStack, result.overlayNumber(), world, world.getTime() + DISPLAY_TICKS);
    }

    public static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null || HaHudVisibility.shouldHideHashimotoHud(client) || !HaConfig.get().verseDetector.enabled) {
            clear();
            return;
        }

        DisplayState state = currentState;
        if (state == null) {
            return;
        }

        if (state.world != client.world) {
            clear();
            return;
        }

        if (client.world.getTime() > state.expiresAtWorldTime) {
            clear();
            return;
        }

        int x = Math.max(PANEL_PADDING, client.getWindow().getScaledWidth() - PANEL_SIZE - PANEL_PADDING);
        int y = PANEL_PADDING;
        DrawableHelper.fill(matrices, x, y, x + PANEL_SIZE, y + PANEL_SIZE, 0xAA000000);
        DrawableHelper.fill(matrices, x, y, x + PANEL_SIZE, y + 1, 0xFF70E000);

        ItemRenderer renderer = client.getItemRenderer();
        renderer.renderInGuiWithOverrides(state.stack, x + 1, y + 1);
        renderer.renderGuiItemOverlay(client.textRenderer, state.stack, x + 1, y + 1);

        String overlay = Integer.toString(state.overlayNumber);
        int overlayX = x + PANEL_SIZE - client.textRenderer.getWidth(overlay) - 1;
        client.textRenderer.drawWithShadow(matrices, overlay, overlayX, y + 1, 0xFFFFFF);
    }

    public static void onDisconnected() {
        clear();
    }

    private static void clear() {
        currentState = null;
    }

    private static final class DisplayState {
        final ItemStack stack;
        final int overlayNumber;
        final World world;
        final long expiresAtWorldTime;

        DisplayState(ItemStack stack, int overlayNumber, World world, long expiresAtWorldTime) {
            this.stack = stack;
            this.overlayNumber = overlayNumber;
            this.world = world;
            this.expiresAtWorldTime = expiresAtWorldTime;
        }
    }
}
