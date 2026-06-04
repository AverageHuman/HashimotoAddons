package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaElementTrackerScreen extends Screen {
    private static final Text TITLE = new LiteralText("Element Tracker");

    private final Screen parent;
    private ButtonWidget timerButton;
    private ButtonWidget continueAfterStartButton;

    public HaElementTrackerScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 36;
        int spacing = 24;
        addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText("Element Tracker: " + onOff(config.elementTrackerEnabled)), button -> {
            config.elementTrackerEnabled = !config.elementTrackerEnabled;
            config.save();
            button.setMessage(new LiteralText("Element Tracker: " + onOff(config.elementTrackerEnabled)));
        }));

        addButton(new ButtonWidget(centerX - 105, top + spacing, 210, 20, new LiteralText("Select Target Element"), button -> {
            if (client != null) {
                client.openScreen(new HaElementTrackerTargetScreen(this));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, top + spacing * 2, 210, 20, new LiteralText("Adjust Overlay Position"), button -> {
            if (client != null) {
                client.openScreen(new HaElementTrackerOverlayScreen(this));
            }
        }));

        timerButton = addButton(new ButtonWidget(centerX - 105, top + spacing * 3, 210, 20, new LiteralText(""), button -> {
            config.elementTrackerShowTimer = !config.elementTrackerShowTimer;
            config.save();
            refreshButtons();
        }));

        continueAfterStartButton = addButton(new ButtonWidget(centerX - 105, top + spacing * 4, 210, 20, new LiteralText(""), button -> {
            config.elementTrackerContinueAfterStart = !config.elementTrackerContinueAfterStart;
            config.save();
            refreshButtons();
        }));

        addButton(new ButtonWidget(centerX - 105, top + spacing * 5, 210, 20, new LiteralText("Reset Data"), button -> HaElementTracker.clear()));
        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 14, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Tracks visible element items and estimates time to your target rarity."), this.width / 2, 30, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshButtons() {
        HaConfig config = HaConfig.get();
        timerButton.setMessage(new LiteralText("Show Timer: " + onOff(config.elementTrackerShowTimer)));
        continueAfterStartButton.setMessage(new LiteralText("Auto Stop: " + onOff(!config.elementTrackerContinueAfterStart)));
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
