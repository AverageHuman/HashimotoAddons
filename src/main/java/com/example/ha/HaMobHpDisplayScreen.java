package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaMobHpDisplayScreen extends Screen {
    private static final Text TITLE = new LiteralText("Mob HP Display");

    private final Screen parent;
    private ButtonWidget positionButton;
    private ButtonWidget displayButton;
    private ButtonWidget percentageButton;
    private ButtonWidget compactButton;

    public HaMobHpDisplayScreen(Screen parent) {
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
        addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText("Mob HP Display: " + onOff(config.mobHpDisplayEnabled)), button -> {
            config.mobHpDisplayEnabled = !config.mobHpDisplayEnabled;
            config.save();
            button.setMessage(new LiteralText("Mob HP Display: " + onOff(config.mobHpDisplayEnabled)));
        }));

        positionButton = addButton(new ButtonWidget(centerX - 105, top + spacing, 210, 20, new LiteralText(""), button -> {
            config.mobHpDisplayPosition = HaMobHpDisplayOverlay.nextPosition(config.mobHpDisplayPosition);
            config.save();
            refreshButtons();
        }));

        displayButton = addButton(new ButtonWidget(centerX - 105, top + spacing * 2, 210, 20, new LiteralText(""), button -> {
            config.mobHpDisplaySlim = !config.mobHpDisplaySlim;
            config.save();
            refreshButtons();
        }));

        percentageButton = addButton(new ButtonWidget(centerX - 105, top + spacing * 3, 210, 20, new LiteralText(""), button -> {
            config.mobHpDisplayShowPercentage = !config.mobHpDisplayShowPercentage;
            config.save();
            refreshButtons();
        }));

        compactButton = addButton(new ButtonWidget(centerX - 105, top + spacing * 4, 210, 20, new LiteralText(""), button -> {
            config.mobHpDisplayCompactNumbers = !config.mobHpDisplayCompactNumbers;
            config.save();
            refreshButtons();
        }));

        addButton(new ButtonWidget(centerX - 105, top + spacing * 5, 210, 20, new LiteralText("Adjust Overlay Position"), button -> {
            if (client != null) {
                client.openScreen(new HaMobHpDisplayOverlayScreen(this));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 14, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Shows the HP of the non-player mob under your crosshair."), this.width / 2, 30, 0xA0A0A0);
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
        positionButton.setMessage(new LiteralText("Position: " + HaMobHpDisplayOverlay.getPositionLabel(config.mobHpDisplayPosition)));
        displayButton.setMessage(new LiteralText("Display: " + (config.mobHpDisplaySlim ? "Slim" : "Full")));
        percentageButton.setMessage(new LiteralText("Show Percentage: " + onOff(config.mobHpDisplayShowPercentage)));
        compactButton.setMessage(new LiteralText("Compact HP: " + onOff(config.mobHpDisplayCompactNumbers)));
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
