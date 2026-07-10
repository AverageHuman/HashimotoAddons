package com.example.ha.mixin;

import com.example.ha.HaExpTracker;
import com.example.ha.HaDamageTruncation;
import com.example.ha.HaCriticalSound;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"))
    private void ha$trackRenderedLabel(Entity entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        HaCriticalSound.onRenderedNameTag(entity, text);
        HaExpTracker.recordRenderedNameTag(entity, text);
    }

    @ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), argsOnly = true)
    private Text ha$truncateDamageLabel(Text text) {
        return HaDamageTruncation.transformLabel(text);
    }
}
