package com.example.ha.mixin;

import com.example.ha.HaWaypointManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
abstract class MinecraftClientWaypointMixin {
    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void ha$placeWaypoint(CallbackInfo ci) {
        if (HaWaypointManager.tryUse((MinecraftClient) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void ha$removeWaypoint(CallbackInfo ci) {
        if (HaWaypointManager.tryAttack((MinecraftClient) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void ha$holdWaypointBreak(boolean breaking, CallbackInfo ci) {
        if (breaking && HaWaypointManager.shouldCancelBlockBreaking((MinecraftClient) (Object) this)) {
            ci.cancel();
        }
    }
}
