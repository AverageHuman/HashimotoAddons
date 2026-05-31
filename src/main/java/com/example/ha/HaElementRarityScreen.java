package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaElementRarityScreen extends Screen {
    private static final Text TITLE = new LiteralText("Element Rarity");

    private final Screen parent;

    public HaElementRarityScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 52;
        addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText("Element Rarity: " + onOff(config.elementRarityEnabled)), button -> {
            config.elementRarityEnabled = !config.elementRarityEnabled;
            config.save();
            button.setMessage(new LiteralText("Element Rarity: " + onOff(config.elementRarityEnabled)));
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 16, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Highlights element item slots with the same color as the item name."), this.width / 2, 34, 0xA0A0A0);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Works in all inventory-style handled screens."), this.width / 2, 46, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
