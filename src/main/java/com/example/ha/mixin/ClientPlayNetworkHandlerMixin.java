package com.example.ha.mixin;

import com.example.ha.HaSoulbindProtection;
import com.example.ha.HaChatFilter;
import com.example.ha.HaDropTracker;
import com.example.ha.HaChestSearchIndex;
import com.example.ha.HaEvolutionForgeHelper;
import com.example.ha.HaElementTracker;
import com.example.ha.HaExpTracker;
import com.example.ha.HaSubSkillTimer;
import com.example.ha.HaDropNotifier;
import com.example.ha.HaAfkFarming;
import com.example.ha.HaAlchemyKilnAutomation;
import com.example.ha.HaRitualBookTimer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket;
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
        HaSubSkillTimer.onGameMessage(packet.getMessage());
        HaRitualBookTimer.onGameMessage(packet.getMessage());
        HaAlchemyKilnAutomation.onGameMessage(packet.getMessage());
        if (packet.getLocation() == MessageType.GAME_INFO) {
            HaAlchemyKilnAutomation.onHudMessage(packet.getMessage());
        }
        if (HaChatFilter.shouldHide(packet.getMessage())) {
            ci.cancel();
        }
    }

    @Inject(method = "onItemPickupAnimation", at = @At("HEAD"))
    private void ha$trackPickedUpItems(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
        HaDropTracker.onItemPickup(packet);
        HaElementTracker.onItemPickup(packet);
    }

    @Inject(method = "onEntitySpawn", at = @At("TAIL"))
    private void ha$trackExpEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        HaExpTracker.onEntitySpawn(packet);
    }

    @Inject(method = "onMobSpawn", at = @At("TAIL"))
    private void ha$trackExpMobSpawn(MobSpawnS2CPacket packet, CallbackInfo ci) {
        HaExpTracker.onMobSpawn(packet);
    }

    @Inject(method = "onEntityTrackerUpdate", at = @At("TAIL"))
    private void ha$trackExpEntityMetadata(EntityTrackerUpdateS2CPacket packet, CallbackInfo ci) {
        HaExpTracker.onEntityTrackerUpdate(packet);
    }

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void ha$clearSoulbindProtection(Text reason, CallbackInfo ci) {
        HaSoulbindProtection.onDisconnected();
        HaDropNotifier.onDisconnected();
        HaRitualBookTimer.onDisconnected();
        HaAfkFarming.onDisconnected();
        HaAlchemyKilnAutomation.onDisconnected();
    }

    @Inject(method = "onOpenScreen", at = @At("HEAD"))
    private void ha$captureChestSearchTarget(OpenScreenS2CPacket packet, CallbackInfo ci) {
        HaChestSearchIndex.get().onContainerScreenOpen(MinecraftClient.getInstance());
        HaEvolutionForgeHelper.onOpenScreen(packet.getSyncId(), packet.getName());
    }
}
