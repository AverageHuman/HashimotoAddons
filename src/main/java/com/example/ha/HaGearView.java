package com.example.ha;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class HaGearView {
    private HaGearView() {
    }

    public static void showTargetGear(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }

        HitResult hitResult = client.crosshairTarget;
        if (!(hitResult instanceof EntityHitResult) || hitResult.getType() != HitResult.Type.ENTITY) {
            sendMessage(client, "\u00a7cAim at a player first.");
            return;
        }

        Entity entity = ((EntityHitResult) hitResult).getEntity();
        if (!(entity instanceof PlayerEntity) || entity == client.player) {
            sendMessage(client, "\u00a7cAim at another player first.");
            return;
        }

        PlayerEntity target = (PlayerEntity) entity;
        sendMessage(client, "\u00a7aGear View: \u00a7f" + normalize(target.getDisplayName().getString()));
        sendItemLine(client, "Weapon", target.getMainHandStack());
        sendItemLine(client, "Offhand", target.getOffHandStack());
        sendItemLine(client, "Helmet", target.inventory.armor.get(3));
        sendItemLine(client, "Chestplate", target.inventory.armor.get(2));
        sendItemLine(client, "Leggings", target.inventory.armor.get(1));
        sendItemLine(client, "Boots", target.inventory.armor.get(0));
    }

    private static void sendItemLine(MinecraftClient client, String label, ItemStack stack) {
        BaseText text = new LiteralText(label + ": ");
        if (stack == null || stack.isEmpty()) {
            text.append(new LiteralText("None").formatted(Formatting.DARK_GRAY));
        } else {
            text.append(stack.toHoverableText());
        }
        client.player.sendMessage(text, false);
    }

    private static void sendMessage(MinecraftClient client, String message) {
        client.player.sendMessage(new LiteralText(message), false);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String stripped = Formatting.strip(value);
        return stripped == null ? value.trim() : stripped.trim();
    }
}
