package com.example.ha;

import com.example.ha.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.slot.Slot;

public final class HaItemLockOverlay {
    private HaItemLockOverlay() {
    }

    public static void render(Screen screen, MatrixStack matrices, int mouseX, int mouseY) {
        if (!(screen instanceof HandledScreen)) {
            return;
        }

        HaConfig config = HaConfig.get();
        if (!config.itemLockEnabled) {
            return;
        }

        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        Slot focusedSlot = accessor.ha$getFocusedSlot();
        if (focusedSlot != null && HaItemLockHelper.isLockActiveForSlot(MinecraftClient.getInstance(), focusedSlot)) {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(
                matrices,
                "\u26bf",
                mouseX + 10,
                mouseY - 12,
                0xFF5555
            );
        }

        drawLockedSlotBadges((HandledScreen<?>) screen, matrices, config);
    }

    private static void drawLockedSlotBadges(HandledScreen<?> screen, MatrixStack matrices, HaConfig config) {
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int left = accessor.ha$getX();
        int top = accessor.ha$getY();

        for (Slot slot : screen.getScreenHandler().slots) {
            if (!HaItemLockHelper.isLockActiveForSlot(MinecraftClient.getInstance(), slot)) {
                continue;
            }
            int markerX = left + slot.x + 1;
            int markerY = top + slot.y + 1;
            matrices.push();
            matrices.translate(0.0D, 0.0D, 300.0D);
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, "L", markerX, markerY, 0xFF5555);
            matrices.pop();
        }
    }
}
