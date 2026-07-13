package com.example.ha;

import com.example.ha.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.slot.Slot;

/** Draws the Verse tier number on beacon stacks in inventory/container screens. */
public final class HaVerseSlotOverlay {
    private HaVerseSlotOverlay() {
    }

    public static void render(HandledScreen<?> screen, MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null || !HaConfig.get().verseDetector.enabled) {
            return;
        }

        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int left = accessor.ha$getX();
        int top = accessor.ha$getY();
        for (Slot slot : screen.getScreenHandler().slots) {
            HaVerseClassifier.Result result = HaVerseClassifier.classifyBeacon(slot.getStack());
            if (result == null) {
                continue;
            }

            String number = Integer.toString(result.overlayNumber());
            int markerX = left + slot.x + 16 - client.textRenderer.getWidth(number);
            int markerY = top + slot.y + 1;
            matrices.push();
            matrices.translate(0.0D, 0.0D, 300.0D);
            client.textRenderer.drawWithShadow(matrices, number, markerX, markerY, 0xFFFFFF);
            matrices.pop();
        }
    }
}
