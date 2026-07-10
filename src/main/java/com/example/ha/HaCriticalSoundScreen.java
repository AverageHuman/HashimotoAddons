package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaCriticalSoundScreen extends Screen {
    private static final Text TITLE = new LiteralText("Critical Sound");

    private final Screen parent;
    private ButtonWidget toggleButton;

    public HaCriticalSoundScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 48;
        toggleButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText(""), button -> {
            config.criticalSoundEnabled = !config.criticalSoundEnabled;
            config.save();
            refreshButtons();
        }));

        addButton(new SoundVolumeSlider(centerX - 105, top + 28, 210, 20, config));

        addButton(new ButtonWidget(centerX - 105, top + 56, 210, 20, new LiteralText("Test Critical Sound"), button -> {
            HaCriticalSound.playCriticalSound(client);
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 16, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("クリティカル判定のネームタグに一致したら、同梱音を再生します。"), this.width / 2, 32, 0xA0A0A0);
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
        if (toggleButton != null) {
            toggleButton.setMessage(new LiteralText("Critical Sound: " + onOff(config.criticalSoundEnabled)));
        }
    }

    private static String onOff(boolean value) {
        return value ? "\u00a7aEnabled" : "\u00a7cDisabled";
    }

    private static final class SoundVolumeSlider extends SliderWidget {
        private final HaConfig config;

        SoundVolumeSlider(int x, int y, int width, int height, HaConfig config) {
            super(x, y, width, height, new LiteralText(""), config.criticalSoundVolume / 100.0D);
            this.config = config;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(new LiteralText("Critical Sound Volume: " + config.criticalSoundVolume + "%"));
        }

        @Override
        protected void applyValue() {
            config.criticalSoundVolume = (int) Math.round(value * 100.0D);
            config.normalize();
            config.save();
        }
    }
}
