package com.example.ha.mixin;

import com.example.ha.HaPanicMode;

import com.example.ha.HaItemProtect;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
abstract class ClientPlayerEntityMixin {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void ha$preventLockedHotbarDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        if (HaPanicMode.isActive()) return;
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        if (HaItemProtect.isProtected(player.getMainHandStack())) {
            HaItemProtect.notifyBlockedProtection();
            cir.setReturnValue(false);
        }
    }
}
