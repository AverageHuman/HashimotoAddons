package com.example.ha.mixin;

import com.example.ha.HaSoulbindProtection;
import java.util.Iterator;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void ha$replaceDisconnectButton(CallbackInfo ci) {
        ButtonWidget originalButton = findDisconnectButton();
        if (originalButton == null || client == null) {
            return;
        }

        this.buttons.remove(originalButton);
        removeChild(originalButton);

        final ButtonWidget disconnectButton = originalButton;
        addButton(new ButtonWidget(disconnectButton.x, disconnectButton.y, disconnectButton.getWidth(), disconnectButton.getHeight(), disconnectButton.getMessage(), button -> {
            if (HaSoulbindProtection.interceptPauseMenuDisconnect(client, (GameMenuScreen) (Object) this, new Runnable() {
                @Override
                public void run() {
                    disconnectButton.onPress();
                }
            })) {
                return;
            }

            disconnectButton.onPress();
        }));
    }

    private ButtonWidget findDisconnectButton() {
        for (ClickableWidget widget : this.buttons) {
            if (!(widget instanceof ButtonWidget)) {
                continue;
            }

            ButtonWidget button = (ButtonWidget) widget;
            if (isDisconnectMessage(button.getMessage())) {
                return button;
            }
        }
        return null;
    }

    private void removeChild(ClickableWidget widget) {
        for (Iterator<net.minecraft.client.gui.Element> iterator = this.children.iterator(); iterator.hasNext(); ) {
            if (iterator.next() == widget) {
                iterator.remove();
                return;
            }
        }
    }

    private static boolean isDisconnectMessage(Text text) {
        if (text instanceof TranslatableText) {
            return "menu.disconnect".equals(((TranslatableText) text).getKey())
                || "menu.returnToMenu".equals(((TranslatableText) text).getKey());
        }
        String value = text.getString();
        return "Disconnect".equals(value) || "Return to Menu".equals(value);
    }
}
