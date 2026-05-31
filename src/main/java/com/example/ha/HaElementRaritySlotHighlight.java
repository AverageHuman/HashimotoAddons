package com.example.ha;

import com.example.ha.mixin.HandledScreenAccessor;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public final class HaElementRaritySlotHighlight {
    private static final String[] ELEMENT_NAMES = new String[] {
        "\u30d5\u30a1\u30a4\u30a2\u30a8\u30ec\u30e1\u30f3\u30c8",
        "\u30a2\u30a4\u30b9\u30a8\u30ec\u30e1\u30f3\u30c8",
        "\u30b5\u30f3\u30c0\u30fc\u30a8\u30ec\u30e1\u30f3\u30c8",
        "\u30a2\u30fc\u30b9\u30a8\u30ec\u30e1\u30f3\u30c8",
        "\u30c0\u30fc\u30af\u30a8\u30ec\u30e1\u30f3\u30c8",
        "\u30a6\u30a3\u30f3\u30c9\u30a8\u30ec\u30e1\u30f3\u30c8",
        "\u30a6\u30a9\u30fc\u30bf\u30fc\u30a8\u30ec\u30e1\u30f3\u30c8",
        "\u30e9\u30a4\u30c8\u30a8\u30ec\u30e1\u30f3\u30c8",
        "\u30b9\u30da\u30fc\u30b9\u30a8\u30ec\u30e1\u30f3\u30c8"
    };
    private static final int BORDER_ALPHA = 0xD0;
    private static final int FILL_ALPHA = 0x35;

    private HaElementRaritySlotHighlight() {
    }

    public static void render(HandledScreen<?> screen, MatrixStack matrices) {
        if (screen == null) {
            return;
        }

        HaConfig config = HaConfig.get();
        if (!config.elementRarityEnabled) {
            return;
        }

        ScreenHandler handler = screen.getScreenHandler();
        if (handler == null || handler.slots == null || handler.slots.isEmpty()) {
            return;
        }

        int left = ((HandledScreenAccessor) screen).ha$getX();
        int top = ((HandledScreenAccessor) screen).ha$getY();
        for (Slot slot : handler.slots) {
            if (slot == null) {
                continue;
            }

            ItemStack stack = slot.getStack();
            if (!matchesElementName(stack)) {
                continue;
            }

            Integer rgb = extractHighlightColor(stack);
            if (rgb == null) {
                continue;
            }

            int x = left + slot.x;
            int y = top + slot.y;
            int borderColor = withAlpha(rgb.intValue(), BORDER_ALPHA);
            int fillColor = withAlpha(rgb.intValue(), FILL_ALPHA);
            DrawableHelper.fill(matrices, x, y, x + 16, y + 16, fillColor);
            DrawableHelper.fill(matrices, x, y, x + 16, y + 1, borderColor);
            DrawableHelper.fill(matrices, x, y + 15, x + 16, y + 16, borderColor);
            DrawableHelper.fill(matrices, x, y, x + 1, y + 16, borderColor);
            DrawableHelper.fill(matrices, x + 15, y, x + 16, y + 16, borderColor);
        }
    }

    static boolean matchesElementName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String plainName = normalize(stack.getName().getString());
        for (String elementName : ELEMENT_NAMES) {
            if (plainName.contains(elementName)) {
                return true;
            }
        }
        return false;
    }

    static Integer extractHighlightColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return findColor(stack.getName());
    }

    private static Integer findColor(Text text) {
        if (text == null) {
            return null;
        }

        TextColor styleColor = text.getStyle().getColor();
        if (styleColor != null) {
            return Integer.valueOf(styleColor.getRgb());
        }

        for (Text sibling : text.getSiblings()) {
            Integer siblingColor = findColor(sibling);
            if (siblingColor != null) {
                return siblingColor;
            }
        }
        return null;
    }

    private static int withAlpha(int rgb, int alpha) {
        return (alpha & 0xFF) << 24 | (rgb & 0xFFFFFF);
    }

    private static String normalize(String value) {
        String stripped = Formatting.strip(value);
        if (stripped == null) {
            stripped = value;
        }
        return stripped == null ? "" : stripped.trim();
    }
}
