package com.example.ha.mixin;

import com.example.ha.HaEvolutionForgeHelper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
abstract class ItemStackTooltipMixin {
    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void ha$appendEvolutionForgeMarker(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        List<Text> tooltip = cir.getReturnValue();
        if (!HaEvolutionForgeHelper.shouldMarkTooltip((ItemStack) (Object) this, tooltip)) {
            return;
        }

        List<Text> updatedTooltip = new ArrayList<Text>(tooltip);
        HaEvolutionForgeHelper.appendMarker(updatedTooltip);
        cir.setReturnValue(updatedTooltip);
    }
}
