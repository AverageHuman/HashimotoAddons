package com.example.ha.mixin;

import com.example.ha.HaSoulbindProtection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
abstract class MinecraftClientDisconnectMixin {
    @Inject(method = "disconnect()V", at = @At("HEAD"), cancellable = true)
    private void ha$confirmDisconnectWithoutScreen(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (HaSoulbindProtection.interceptDisconnect(client, null)) {
            ci.cancel();
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"), cancellable = true)
    private void ha$confirmDisconnectWithScreen(Screen screen, CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (HaSoulbindProtection.interceptDisconnect(client, screen)) {
            ci.cancel();
        }
    }
}
