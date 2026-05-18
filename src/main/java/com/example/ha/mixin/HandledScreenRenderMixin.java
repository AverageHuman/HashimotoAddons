package com.example.ha.mixin;

import com.example.ha.HaChestSearchSlotHighlight;
import com.example.ha.HaItemLockOverlay;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
abstract class HandledScreenRenderMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void ha$renderItemLockOverlay(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        HaChestSearchSlotHighlight.render((HandledScreen<?>) (Object) this, matrices);
        HaItemLockOverlay.render((HandledScreen<?>) (Object) this, matrices, mouseX, mouseY);
    }
}
