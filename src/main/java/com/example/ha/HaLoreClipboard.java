package com.example.ha;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaLoreClipboard {
    private HaLoreClipboard() {
    }

    public static boolean copySlotTooltip(Slot slot) {
        if (slot == null || slot.getStack().isEmpty()) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.keyboard == null) {
            return false;
        }

        ItemStack stack = slot.getStack();
        List<Text> tooltip = HaEvolutionForgeHelper.getUnmodifiedTooltip(client, stack);
        client.keyboard.setClipboard(formatTooltip(stack, tooltip));
        if (client.player != null) {
            client.player.sendMessage(new LiteralText("[\u00a7l\u00a7bHashimotoAddons\u00a7r]:Copied item lore to clipboard."), false);
        }
        return true;
    }

    private static String formatTooltip(ItemStack stack, List<Text> tooltip) {
        StringBuilder result = new StringBuilder();
        result.append("Item: ").append(clean(stack.getName().getString())).append("\r\n");
        result.append("Tooltip lines:\r\n");
        for (int i = 0; i < tooltip.size(); i++) {
            String line = clean(tooltip.get(i) == null ? "" : tooltip.get(i).getString());
            result.append('[').append(i).append("] ").append(line).append("\r\n");
            result.append("    codepoints: ").append(codepoints(line)).append("\r\n");
        }
        return result.toString();
    }

    private static String clean(String value) {
        String stripped = Formatting.strip(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static String codepoints(String value) {
        if (value == null || value.isEmpty()) {
            return "(empty)";
        }

        StringBuilder result = new StringBuilder();
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append("U+").append(hex(codePoint)).append("('").append(printable(codePoint)).append("')");
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }

    private static String hex(int codePoint) {
        String value = Integer.toHexString(codePoint).toUpperCase();
        StringBuilder result = new StringBuilder();
        for (int i = value.length(); i < 4; i++) {
            result.append('0');
        }
        return result.append(value).toString();
    }

    private static String printable(int codePoint) {
        if (Character.isWhitespace(codePoint)) {
            return " ";
        }
        return new String(Character.toChars(codePoint));
    }
}
