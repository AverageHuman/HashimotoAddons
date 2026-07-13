package com.example.ha.mixin;

import com.example.ha.HaBuildFlags;
import com.example.ha.HaConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {
    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
    private void ha$revealInvisibleMobsAndPlayers(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED
            && HaConfig.get().revealInvisibleMobsEnabled
            && (entity instanceof MobEntity || entity instanceof PlayerEntity)
            && entity.isInvisible()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
    private void ha$forceOpaqueInvisiblePlayerLayer(
        LivingEntity entity,
        boolean showBody,
        boolean translucent,
        boolean showOutline,
        CallbackInfoReturnable<RenderLayer> cir
    ) {
        if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED
            && HaConfig.get().revealInvisibleMobsEnabled
            && entity instanceof AbstractClientPlayerEntity
            && entity.isInvisible()) {
            AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) entity;
            cir.setReturnValue(RenderLayer.getEntitySolid(player.getSkinTexture()));
        }
    }
}
