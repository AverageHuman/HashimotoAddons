package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaRitualBookTimerScreen extends Screen {
    private static final Text TITLE = new LiteralText("Ritual Book Timer");

    private final Screen parent;
    private ButtonWidget displayButton;

    public HaRitualBookTimerScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 48;
        addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText("Ritual Book Timer: " + onOff(config.ritualBookTimerEnabled)), button -> {
            config.ritualBookTimerEnabled = !config.ritualBookTimerEnabled;
            config.save();
            button.setMessage(new LiteralText("Ritual Book Timer: " + onOff(config.ritualBookTimerEnabled)));
        }));

        displayButton = addButton(new ButtonWidget(centerX - 105, top + 28, 210, 20, new LiteralText(""), button -> {
            config.ritualBookTimerSlim = !config.ritualBookTimerSlim;
            config.save();
            refreshButtons();
        }));

        addButton(new ButtonWidget(centerX - 105, top + 56, 210, 20, new LiteralText("Adjust Overlay Position"), button -> {
            if (client != null) {
                client.openScreen(new HaRitualBookTimerOverlayScreen(this));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 16, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("\u5100\u5f0f\u66f8\u7269\u8cfc\u5165\u30c1\u30e3\u30c3\u30c8\u3092\u691c\u77e5\u3057\u306610\u5206\u30bf\u30a4\u30de\u30fc\u3092\u8868\u793a\u3057\u307e\u3059\u3002"), this.width / 2, 32, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshButtons() {
        displayButton.setMessage(new LiteralText("Display: " + (HaConfig.get().ritualBookTimerSlim ? "Slim" : "Full")));
    }

    private static String onOff(boolean value) {
        return value ? "\u00a7aEnabled" : "\u00a7cDisabled";
    }
}
