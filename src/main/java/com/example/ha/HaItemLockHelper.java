package com.example.ha;

import com.example.ha.mixin.SlotAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;

public final class HaItemLockHelper {
    private HaItemLockHelper() {
    }

    public static Integer getPlayerLockKey(Slot slot) {
        if (slot == null || !(slot.inventory instanceof PlayerInventory)) {
            return null;
        }
        return Integer.valueOf(((SlotAccessor) slot).ha$getIndex());
    }

    public static boolean isLockActiveForSlot(MinecraftClient client, Slot slot) {
        Integer lockKey = getPlayerLockKey(slot);
        if (lockKey == null) {
            return false;
        }

        return isLockActiveForPlayerInventoryIndex(client, lockKey.intValue());
    }

    public static boolean isLockActiveForPlayerInventoryIndex(MinecraftClient client, int playerInventoryIndex) {
        if (isArmorLockTemporarilySuspended(client, playerInventoryIndex)) {
            return false;
        }

        return HaConfig.get().lockedSlotIds.contains(Integer.valueOf(playerInventoryIndex));
    }

    public static boolean isArmorLockTemporarilySuspended(MinecraftClient client, int playerInventoryIndex) {
        if (playerInventoryIndex < 36 || playerInventoryIndex > 39) {
            return false;
        }

        return client.currentScreen instanceof HandledScreen
            && !(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen);
    }
}
