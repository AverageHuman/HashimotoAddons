package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaVerseDetectorScreen extends Screen {
    private static final Text TITLE = new LiteralText("Verse Detector");
    private final Screen parent;

    public HaVerseDetectorScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED && HaConfig.get().verseDetector.autoThrowTrashVerseEnabled) {
            HaConfig.get().verseDetector.autoThrowTrashVerseEnabled = false;
        }

        HaConfig config = HaConfig.get();
        config.normalize();
        int centerX = this.width / 2;
        int y = 64;
        addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Verse Detector: " + onOff(config.verseDetector.enabled)), button -> {
            config.verseDetector.enabled = !config.verseDetector.enabled;
            config.save();
            button.setMessage(new LiteralText("Verse Detector: " + onOff(config.verseDetector.enabled)));
        }));
        y += 28;

        if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Auto Throw Trash Verse: " + onOff(config.verseDetector.autoThrowTrashVerseEnabled)), button -> {
                config.verseDetector.autoThrowTrashVerseEnabled = !config.verseDetector.autoThrowTrashVerseEnabled;
                config.save();
                button.setMessage(new LiteralText("Auto Throw Trash Verse: " + onOff(config.verseDetector.autoThrowTrashVerseEnabled)));
            }));
            y += 28;
        }

        addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 18, 0xFFFFFF);
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

    private static String onOff(boolean value) {
        return value ? "\u00a7aEnabled" : "\u00a7cDisabled";
    }
}
