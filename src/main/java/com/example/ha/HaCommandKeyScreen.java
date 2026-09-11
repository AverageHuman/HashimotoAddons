package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

/** Full-only entry screen for Command Key. */
public final class HaCommandKeyScreen extends Screen {
    private static final Text TITLE = new LiteralText("Command Key");

    private final Screen parent;
    private ButtonWidget enabledButton;

    public HaCommandKeyScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            onClose();
            return;
        }
        HaConfig config = HaConfig.get();
        config.normalize();
        int centerX = width / 2;
        int top = 52;
        enabledButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText(""), button -> {
            config.commandKey.enabled = !config.commandKey.enabled;
            config.save();
            refreshButtons();
        }));
        addButton(new ButtonWidget(centerX - 105, top + 28, 210, 20, new LiteralText("Edit Command Key"), button -> {
            if (client != null) {
                client.openScreen(new HaCommandKeyEditScreen(this));
            }
        }));
        addButton(new ButtonWidget(centerX - 105, height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, textRenderer, TITLE, width / 2, 18, 0xFFFFFF);
        drawCenteredText(matrices, textRenderer, new LiteralText("Runs the configured command when its key is pressed."), width / 2, 34, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshButtons() {
        enabledButton.setMessage(new LiteralText("Command Key: " + (HaConfig.get().commandKey.enabled ? "\u00a7aEnabled" : "\u00a7cDisabled")));
    }
}
