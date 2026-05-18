package com.example.ha.mixin;

import com.example.ha.HaConfig;
import com.example.ha.HaItemLockHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
abstract class HandledScreenMixin {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void ha$handleItemLock(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        HaConfig config = HaConfig.get();
        if (!config.itemLockEnabled) {
            return;
        }

        Slot focusedSlot = ((HandledScreenAccessor) this).ha$getFocusedSlot();
        if (focusedSlot == null) {
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_L) {
            Integer lockKey = HaItemLockHelper.getPlayerLockKey(focusedSlot);
            if (lockKey == null) {
                return;
            }

            boolean lockedNow;
            if (config.lockedSlotIds.contains(lockKey)) {
                config.lockedSlotIds.remove(lockKey);
                lockedNow = false;
            } else {
                config.lockedSlotIds.add(lockKey);
                lockedNow = true;
            }

            config.save();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                float pitch = lockedNow ? 1.35F : 0.85F;
                client.player.playSound(SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.7F, pitch);
            }
            cir.setReturnValue(true);
            return;
        }

        KeyBinding dropKey = MinecraftClient.getInstance().options.keyDrop;
        if (dropKey != null && dropKey.matchesKey(keyCode, scanCode) && HaItemLockHelper.isLockActiveForSlot(MinecraftClient.getInstance(), focusedSlot)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onMouseClick", at = @At("HEAD"), cancellable = true)
    private void ha$preventLockedSlotMovement(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (!HaConfig.get().itemLockEnabled || slot == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (actionType == SlotActionType.PICKUP
            || actionType == SlotActionType.QUICK_MOVE
            || actionType == SlotActionType.THROW
            || actionType == SlotActionType.PICKUP_ALL) {
            if (HaItemLockHelper.isLockActiveForSlot(client, slot)) {
                ci.cancel();
            }
            return;
        }

        if (actionType == SlotActionType.SWAP
            && (HaItemLockHelper.isLockActiveForSlot(client, slot)
                || HaItemLockHelper.isLockActiveForPlayerInventoryIndex(client, button))) {
            ci.cancel();
        }
    }
}
