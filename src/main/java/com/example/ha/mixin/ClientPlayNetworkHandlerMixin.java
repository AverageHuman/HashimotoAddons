package com.example.ha.mixin;

import com.example.ha.HaSoulbindProtection;
import com.example.ha.HaChatFilter;
import com.example.ha.HaDropTracker;
import com.example.ha.HaChestSearchIndex;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.network.ClientPlayNetworkHandler.class)
abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
    private void ha$trackSoulbindMessages(GameMessageS2CPacket packet, CallbackInfo ci) {
        HaSoulbindProtection.onGameMessage(packet.getMessage());
        if (HaChatFilter.shouldHide(packet.getMessage())) {
            ci.cancel();
        }
    }

    @Inject(method = "onItemPickupAnimation", at = @At("HEAD"))
    private void ha$trackPickedUpItems(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
        HaDropTracker.onItemPickup(packet);
    }

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void ha$clearSoulbindProtection(Text reason, CallbackInfo ci) {
        HaSoulbindProtection.onDisconnected();
    }

    @Inject(method = "onOpenScreen", at = @At("HEAD"))
    private void ha$captureChestSearchTarget(OpenScreenS2CPacket packet, CallbackInfo ci) {
        HaChestSearchIndex.get().onContainerScreenOpen(MinecraftClient.getInstance());
    }
}
