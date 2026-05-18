package com.example.ha.mixin;

import com.example.ha.HaGhostWall;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
abstract class MinecraftClientGhostWallMixin {
    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void ha$placeGhostWall(CallbackInfo ci) {
        if (HaGhostWall.tryPlaceFromUse((MinecraftClient) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void ha$breakGhostWall(CallbackInfo ci) {
        if (HaGhostWall.tryBreakFromAttack((MinecraftClient) (Object) this)) {
            ci.cancel();
        }
    }
}
