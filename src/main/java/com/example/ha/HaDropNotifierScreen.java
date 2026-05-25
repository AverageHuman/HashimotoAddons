package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaDropNotifierScreen extends Screen {
    private static final Text TITLE = new LiteralText("Drop Notifier");

    private final Screen parent;
    private ButtonWidget enabledButton;
    private ButtonWidget autoStopButton;

    public HaDropNotifierScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 48;
        enabledButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText(""), button -> {
            config.dropNotifierEnabled = !config.dropNotifierEnabled;
            config.save();
            refreshButtons();
        }));

        autoStopButton = addButton(new ButtonWidget(centerX - 105, top + 28, 210, 20, new LiteralText(""), button -> {
            config.dropNotifierContinueAfterStart = !config.dropNotifierContinueAfterStart;
            config.save();
            refreshButtons();
        }));

        addButton(new ButtonWidget(centerX - 105, top + 60, 210, 20, new LiteralText("Edit Notifiers"), button -> {
            if (client != null) {
                client.openScreen(new HaDropNotifierManageScreen(this, 0));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Notifies when matching dropped items appear nearby."), this.width / 2, 28, 0xA0A0A0);
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
        HaConfig config = HaConfig.get();
        enabledButton.setMessage(new LiteralText("Drop Notifier: " + onOff(config.dropNotifierEnabled)));
        autoStopButton.setMessage(new LiteralText("Auto Stop: " + onOff(!config.dropNotifierContinueAfterStart)));
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
