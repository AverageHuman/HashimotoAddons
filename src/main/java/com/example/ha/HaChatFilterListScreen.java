package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaChatFilterListScreen extends Screen {
    private static final Text TITLE = new LiteralText("Chat Filter");

    private final Screen parent;
    private ButtonWidget enabledButton;

    public HaChatFilterListScreen(Screen parent, int page) {
        super(TITLE);
        this.parent = parent;
    }

    public Screen getParentScreen() {
        return parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 48;
        enabledButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText(""), button -> {
            config.chatFilterEnabled = !config.chatFilterEnabled;
            config.save();
            refreshButtons();
        }));

        addButton(new ButtonWidget(centerX - 105, top + 32, 210, 20, new LiteralText("Edit Filters"), button -> {
            if (client != null) {
                client.openScreen(new HaChatFilterManageScreen(this, 0));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Open Edit Filters to add or change hidden chat text."), this.width / 2, 28, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        HaConfig.get().normalize();
        HaConfig.get().save();
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshButtons() {
        enabledButton.setMessage(new LiteralText("Chat Filter Status: " + (HaConfig.get().chatFilterEnabled ? "§aEnabled" : "§cDisabled")));
    }
}
