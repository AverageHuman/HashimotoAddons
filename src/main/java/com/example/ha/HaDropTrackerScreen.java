package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaDropTrackerScreen extends Screen {
    private static final Text TITLE = new LiteralText("Drop Tracker");

    private final Screen parent;
    private ButtonWidget modeButton;

    public HaDropTrackerScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 40;
        addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText("Drop Tracker: " + onOff(config.dropTrackerEnabled)), button -> {
            config.dropTrackerEnabled = !config.dropTrackerEnabled;
            config.save();
            button.setMessage(new LiteralText("Drop Tracker: " + onOff(config.dropTrackerEnabled)));
        }));

        modeButton = addButton(new ButtonWidget(centerX - 105, top + 32, 210, 20, new LiteralText(""), button -> {
            config.dropTrackerMode = HaDropTracker.nextMode(config.dropTrackerMode);
            config.save();
            refreshButtons();
        }));

        addButton(new ButtonWidget(centerX - 105, top + 64, 210, 20, new LiteralText("Adjust HUD Position"), button -> {
            if (client != null) {
                client.openScreen(new HaDropTrackerOverlayScreen(this));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, top + 96, 210, 20, new LiteralText("Edit Registered Items"), button -> {
            if (client != null) {
                client.openScreen(new HaDropTrackerRegisteredListScreen(this, 0));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, top + 188, 210, 20, new LiteralText("Reset Counts"), button -> {
            HaDropTracker.clear();
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 14, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Picked up items are saved to JSON and kept across sessions."), this.width / 2, 28, 0xA0A0A0);
        drawCenteredText(matrices, this.textRenderer, new LiteralText(HaDropTracker.getModeDescription(HaConfig.get().dropTrackerMode)), this.width / 2, 180, 0xA0A0A0);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Registered items: " + HaDropTracker.getRegisteredItemCount()), this.width / 2, 194, 0xA0A0A0);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Add current held item: /ha tracker add [price]"), this.width / 2, 208, 0xA0A0A0);
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
        modeButton.setMessage(new LiteralText("Tracking Mode: " + HaDropTracker.getModeLabel(HaConfig.get().dropTrackerMode)));
    }
}
