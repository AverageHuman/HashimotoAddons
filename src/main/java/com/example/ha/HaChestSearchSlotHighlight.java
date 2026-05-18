package com.example.ha;

import com.example.ha.mixin.HandledScreenAccessor;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;

public final class HaChestSearchSlotHighlight {
    private static final int BORDER_COLOR = 0xD0B8FF59;
    private static final int FILL_COLOR = 0x503A5F12;

    private HaChestSearchSlotHighlight() {
    }

    public static void render(HandledScreen<?> screen, MatrixStack matrices) {
        if (!(screen instanceof GenericContainerScreen)) {
            return;
        }

        HaConfig config = HaConfig.get();
        if (!config.chestSearchEnabled || config.chestSearchQuery.trim().isEmpty()) {
            return;
        }

        GenericContainerScreenHandler handler = (GenericContainerScreenHandler) ((GenericContainerScreen) screen).getScreenHandler();
        int containerSlots = handler.getRows() * 9;
        int limit = Math.min(containerSlots, handler.slots.size());
        int left = ((HandledScreenAccessor) screen).ha$getX();
        int top = ((HandledScreenAccessor) screen).ha$getY();

        for (int i = 0; i < limit; i++) {
            Slot slot = handler.slots.get(i);
            if (slot == null) {
                continue;
            }

            ItemStack stack = slot.getStack();
            if (!HaChestSearchIndex.get().matchesQuery(stack, config.chestSearchQuery)) {
                continue;
            }

            int x = left + slot.x;
            int y = top + slot.y;
            DrawableHelper.fill(matrices, x, y, x + 16, y + 16, FILL_COLOR);
            DrawableHelper.fill(matrices, x, y, x + 16, y + 1, BORDER_COLOR);
            DrawableHelper.fill(matrices, x, y + 15, x + 16, y + 16, BORDER_COLOR);
            DrawableHelper.fill(matrices, x, y, x + 1, y + 16, BORDER_COLOR);
            DrawableHelper.fill(matrices, x + 15, y, x + 16, y + 16, BORDER_COLOR);
        }
    }
}
