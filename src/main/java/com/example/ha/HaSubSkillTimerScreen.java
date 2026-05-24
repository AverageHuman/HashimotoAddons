package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaSubSkillTimerScreen extends Screen {
    private static final Text TITLE = new LiteralText("Sub Skill Timer");
    private final Screen parent;
    private ButtonWidget displayButton;

    public HaSubSkillTimerScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 48;
        addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText("Sub Skill Timer: " + onOff(config.subSkillTimerEnabled)), button -> {
            config.subSkillTimerEnabled = !config.subSkillTimerEnabled;
            config.save();
            button.setMessage(new LiteralText("Sub Skill Timer: " + onOff(config.subSkillTimerEnabled)));
        }));

        displayButton = addButton(new ButtonWidget(centerX - 105, top + 24, 210, 20, new LiteralText(""), button -> {
            config.subSkillTimerSlim = !config.subSkillTimerSlim;
            config.save();
            refreshButtons();
        }));

        addButton(new ButtonWidget(centerX - 105, top + 48, 210, 20, new LiteralText("Adjust Overlay Position"), button -> {
            if (client != null) {
                client.openScreen(new HaSubSkillTimerOverlayScreen(this));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 16, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Shows cooldown seconds from sub skill reuse messages."), this.width / 2, 32, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshButtons() {
        displayButton.setMessage(new LiteralText("Display: " + (HaConfig.get().subSkillTimerSlim ? "Slim" : "Full")));
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
