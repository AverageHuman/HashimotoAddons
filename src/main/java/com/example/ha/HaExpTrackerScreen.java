package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaExpTrackerScreen extends Screen {
    private static final Text TITLE = new LiteralText("Exp Tracker");

    private final Screen parent;

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

        addButton(new ButtonWidget(centerX - 105, top + 64, 210, 20, new LiteralText("Reset Total"), button -> HaExpTracker.clear()));
        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 14, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Tracks +XP name tags within 20 blocks."), this.width / 2, 30, 0xA0A0A0);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Total XP: " + HaExpTrackerOverlay.formatNumber(HaConfig.get().expTrackerTotal)), this.width / 2, 150, 0xFFD166);
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
