package com.example.ha;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.text.LiteralText;

public final class HaVerseDetector {
    private static final int TITLE_FADE_IN_TICKS = 5;
    private static final int TITLE_STAY_TICKS = 30;
    private static final int TITLE_FADE_OUT_TICKS = 10;

    private HaVerseDetector() {
    }

    public static void onItemPickup(ItemPickupAnimationS2CPacket packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (packet == null || client == null || client.player == null || client.world == null || !client.isOnThread()) {
            return;
        }
        if (packet.getCollectorEntityId() != client.player.getEntityId()) {
            return;
        }

        Entity entity = client.world.getEntityById(packet.getEntityId());
        if (!(entity instanceof ItemEntity)) {
            return;
        }

        ItemStack stack = ((ItemEntity) entity).getStack();
        if (stack == null || stack.isEmpty() || stack.getItem() != Items.BEACON) {
            return;
        }

        HaVerseClassifier.Result result = HaVerseClassifier.classify(stack.getName().getString());
        if (result == null || !HaConfig.get().verseDetector.enabled) {
            return;
        }

        client.inGameHud.setTitles(
            new LiteralText(result.displayName),
            new LiteralText(""),
            TITLE_FADE_IN_TICKS,
            TITLE_STAY_TICKS,
            TITLE_FADE_OUT_TICKS
        );
        if (result.isProtectable()) {
            HaItemProtect.protectIfAbsent(stack);
        }
    }

    public static void onDisconnected() {
        // The detector has no queued state; the Full-only trash handler owns its queue.
    }
}
