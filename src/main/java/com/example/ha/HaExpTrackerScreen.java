package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaExpTrackerScreen extends Screen {
    private static final Text TITLE = new LiteralText("Exp Tracker");

    private final Screen parent;
    private ButtonWidget timerButton;
    private ButtonWidget hourlyRateButton;
    private ButtonWidget compactNumbersButton;

    public HaExpTrackerScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 48;
        addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText("Exp Tracker: " + onOff(config.expTrackerEnabled)), button -> {
            config.expTrackerEnabled = !config.expTrackerEnabled;
            config.save();
            button.setMessage(new LiteralText("Exp Tracker: " + onOff(config.expTrackerEnabled)));
        }));

        addButton(new ButtonWidget(centerX - 105, top + 32, 210, 20, new LiteralText("Adjust Overlay Position"), button -> {
            if (client != null) {
                client.openScreen(new HaExpTrackerOverlayScreen(this));
            }
        }));

        timerButton = addButton(new ButtonWidget(centerX - 105, top + 64, 210, 20, new LiteralText(""), button -> {
            config.expTrackerShowTimer = !config.expTrackerShowTimer;
            config.save();
            refreshButtons();
        }));

        hourlyRateButton = addButton(new ButtonWidget(centerX - 105, top + 96, 210, 20, new LiteralText(""), button -> {
            config.expTrackerShowHourlyRate = !config.expTrackerShowHourlyRate;
            config.save();
            refreshButtons();
        }));

        compactNumbersButton = addButton(new ButtonWidget(centerX - 105, top + 128, 210, 20, new LiteralText(""), button -> {
            config.expTrackerCompactNumbers = !config.expTrackerCompactNumbers;
            config.save();
            refreshButtons();
        }));

        addButton(new ButtonWidget(centerX - 105, top + 160, 210, 20, new LiteralText("Reset Total"), button -> HaExpTracker.clear()));
        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 14, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Tracks +EXP name tags only while soulbound."), this.width / 2, 30, 0xA0A0A0);
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

    private void refreshButtons() {
        HaConfig config = HaConfig.get();
        timerButton.setMessage(new LiteralText("Show Timer: " + onOff(config.expTrackerShowTimer)));
        hourlyRateButton.setMessage(new LiteralText("Show EXP/hour: " + onOff(config.expTrackerShowHourlyRate)));
        compactNumbersButton.setMessage(new LiteralText("Compact XP: " + onOff(config.expTrackerCompactNumbers)));
    }
}
