package com.example.ha;

import com.example.ha.mixin.SlotAccessor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;

public final class HaAutoThrowTrashVerse {
    private static final int RESOLUTION_TIMEOUT_TICKS = 20;
    private static final String MESSAGE_PREFIX = "[\u00a7l\u00a7bHashimotoAddons\u00a7r]: ";
    private static final List<PendingPickup> PENDING_PICKUPS = new ArrayList<PendingPickup>();
    private static final Deque<PendingTarget> PENDING_TARGETS = new ArrayDeque<PendingTarget>();
    private static Object trackedWorld;

    private HaAutoThrowTrashVerse() {
    }

    public static void onItemPickup(ItemPickupAnimationS2CPacket packet) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED || !HaConfig.get().verseDetector.autoThrowTrashVerseEnabled) {
            return;
        }

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
        if (result == null || !result.isTrash()) {
            return;
        }

        PENDING_PICKUPS.add(new PendingPickup(
            HaItemProtect.createProtectionKey(stack),
            captureInventory(client.player.inventory)
        ));
    }

    public static void tick(MinecraftClient client) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED || !HaConfig.get().verseDetector.autoThrowTrashVerseEnabled) {
            clear();
            return;
        }
        if (client == null || client.player == null || client.world == null) {
            clear();
            return;
        }
        if (trackedWorld != client.world) {
            clear();
            trackedWorld = client.world;
        }

        resolvePickups(client);
        processNextTarget(client);
    }

    public static void onDisconnected() {
        clear();
        trackedWorld = null;
    }

    private static void resolvePickups(MinecraftClient client) {
        Map<Integer, HaVerseSlotDelta.SlotState> current = captureInventory(client.player.inventory);
        for (int i = PENDING_PICKUPS.size() - 1; i >= 0; i--) {
            PendingPickup pickup = PENDING_PICKUPS.get(i);
            List<Integer> changedSlots = HaVerseSlotDelta.findChangedSlots(pickup.stackKey, pickup.before, current);
            if (!changedSlots.isEmpty()) {
                for (Integer inventoryIndex : changedSlots) {
                    if (!hasPendingTarget(inventoryIndex.intValue(), pickup.stackKey)) {
                        PENDING_TARGETS.addLast(new PendingTarget(inventoryIndex.intValue(), pickup.stackKey));
                    }
                }
                PENDING_PICKUPS.remove(i);
                continue;
            }

            pickup.ageTicks++;
            if (pickup.ageTicks >= RESOLUTION_TIMEOUT_TICKS) {
                PENDING_PICKUPS.remove(i);
                notify(client, "\u00a7cCould not identify the picked Trash Verse slot; it was left untouched.");
            }
        }
    }

    private static void processNextTarget(MinecraftClient client) {
        if (PENDING_TARGETS.isEmpty() || client.currentScreen != null || client.interactionManager == null) {
            return;
        }

        PendingTarget target = PENDING_TARGETS.peekFirst();
        Slot slot = findInventorySlot(client, target.inventoryIndex, target.stackKey);
        if (slot == null) {
            PENDING_TARGETS.removeFirst();
            notify(client, "\u00a7cThe picked Trash Verse moved before it could be thrown; it was left untouched.");
            return;
        }
        if (HaItemProtect.isProtected(slot.getStack())) {
            PENDING_TARGETS.removeFirst();
            notify(client, "\u00a7eThe picked Trash Verse is protected; it was left untouched.");
            return;
        }

        client.interactionManager.clickSlot(
            client.player.currentScreenHandler.syncId,
            slot.id,
            1,
            SlotActionType.THROW,
            client.player
        );
        PENDING_TARGETS.removeFirst();
    }

    private static Slot findInventorySlot(MinecraftClient client, int inventoryIndex, String stackKey) {
        if (client == null || client.player == null || client.player.currentScreenHandler == null) {
            return null;
        }

        for (Slot slot : client.player.currentScreenHandler.slots) {
            if (slot.inventory != client.player.inventory || ((SlotAccessor) slot).ha$getIndex() != inventoryIndex) {
                continue;
            }
            ItemStack stack = slot.getStack();
            if (stack != null && !stack.isEmpty() && stackKey.equals(HaItemProtect.createProtectionKey(stack))) {
                return slot;
            }
            return null;
        }
        return null;
    }

    private static Map<Integer, HaVerseSlotDelta.SlotState> captureInventory(PlayerInventory inventory) {
        Map<Integer, HaVerseSlotDelta.SlotState> result = new HashMap<Integer, HaVerseSlotDelta.SlotState>();
        if (inventory == null) {
            return result;
        }
        for (int index = 0; index < inventory.size(); index++) {
            ItemStack stack = inventory.getStack(index);
            if (stack == null || stack.isEmpty()) {
                result.put(Integer.valueOf(index), new HaVerseSlotDelta.SlotState("", 0));
            } else {
                result.put(Integer.valueOf(index), new HaVerseSlotDelta.SlotState(
                    HaItemProtect.createProtectionKey(stack),
                    stack.getCount()
                ));
            }
        }
        return result;
    }

    private static boolean hasPendingTarget(int inventoryIndex, String stackKey) {
        for (PendingTarget target : PENDING_TARGETS) {
            if (target.inventoryIndex == inventoryIndex && stackKey.equals(target.stackKey)) {
                return true;
            }
        }
        return false;
    }

    private static void notify(MinecraftClient client, String message) {
        if (client != null && client.player != null) {
            client.player.sendMessage(new LiteralText(MESSAGE_PREFIX + message), false);
        }
    }

    private static void clear() {
        PENDING_PICKUPS.clear();
        PENDING_TARGETS.clear();
    }

    private static final class PendingPickup {
        final String stackKey;
        final Map<Integer, HaVerseSlotDelta.SlotState> before;
        int ageTicks;

        PendingPickup(String stackKey, Map<Integer, HaVerseSlotDelta.SlotState> before) {
            this.stackKey = stackKey;
            this.before = before;
        }
    }

    private static final class PendingTarget {
        final int inventoryIndex;
        final String stackKey;

        PendingTarget(int inventoryIndex, String stackKey) {
            this.inventoryIndex = inventoryIndex;
            this.stackKey = stackKey;
        }
    }
}
