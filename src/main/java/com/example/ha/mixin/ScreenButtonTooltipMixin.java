package com.example.ha.mixin;

import com.example.ha.HaButtonTooltips;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class ScreenButtonTooltipMixin {
    @Shadow
    protected List<ClickableWidget> buttons;

    @Inject(method = "render", at = @At("TAIL"))
    private void ha$renderButtonTooltip(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        for (ClickableWidget button : buttons) {
            if (!button.visible || !button.isHovered()) {
                continue;
            }

            List<Text> tooltip = HaButtonTooltips.get(screen, button.getMessage().getString());
            if (tooltip != null && !tooltip.isEmpty()) {
                screen.renderTooltip(matrices, tooltip, mouseX, mouseY);
            }
            return;
        }
    }
}
