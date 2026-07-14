package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaVerseDetectorScreen extends Screen {
    private static final Text TITLE = new LiteralText("Verse Detector");
    private final Screen parent;
    private TextFieldWidget delayField;
    private int delayFieldY;

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

            delayFieldY = y;
            delayField = new TextFieldWidget(this.textRenderer, centerX - 32, delayFieldY, 72, 20, new LiteralText("Throw Delay Ticks"));
            delayField.setMaxLength(5);
            delayField.setText(Integer.toString(config.verseDetector.autoThrowTrashVerseDelayTicks));
            delayField.setChangedListener(value -> {
                Integer parsed = parseNonNegativeInt(value);
                if (parsed != null) {
                    config.verseDetector.autoThrowTrashVerseDelayTicks = parsed.intValue();
                }
            });
            children.add(delayField);
            y += 28;
        }

        addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void tick() {
        if (delayField != null) {
            delayField.tick();
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 18, 0xFFFFFF);
        if (delayField != null) {
            int centerX = this.width / 2;
            this.textRenderer.draw(matrices, "Throw Delay:", centerX - 105, delayFieldY + 6, 0xFFFFFF);
            this.textRenderer.draw(matrices, "ticks", centerX + 48, delayFieldY + 6, 0xFFFFFF);
            delayField.render(matrices, mouseX, mouseY, delta);
        }
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return delayField != null && delayField.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return delayField != null && delayField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return delayField != null && delayField.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
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

    private static Integer parseNonNegativeInt(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= 0 ? Integer.valueOf(parsed) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
