package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaSubSkillTimerScreen extends Screen {
    private static final Text TITLE = new LiteralText("Sub Skill Timer");
    private final Screen parent;
    private ButtonWidget displayButton;
    private TextFieldWidget cooldownField;

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

        cooldownField = new TextFieldWidget(this.textRenderer, centerX - 5, top + 52, 72, 20, new LiteralText("Cooldown Seconds"));
        cooldownField.setText(Double.toString(config.subSkillTimerCooldownSeconds));
        cooldownField.setChangedListener(value -> {
            Double parsed = parsePositiveDouble(value);
            if (parsed != null) {
                config.subSkillTimerCooldownSeconds = parsed.doubleValue();
                config.save();
            }
        });
        children.add(cooldownField);

        addButton(new ButtonWidget(centerX - 105, top + 80, 210, 20, new LiteralText("Adjust Overlay Position"), button -> {
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
        int centerX = this.width / 2;
        int top = 48;
        this.textRenderer.draw(matrices, "Manual CD", centerX - 105, top + 58, 0xFFFFFF);
        this.textRenderer.draw(matrices, "seconds", centerX + 72, top + 58, 0xFFFFFF);
        cooldownField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        cooldownField.tick();
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return cooldownField.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return cooldownField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        HaConfig.get().normalize();
        HaConfig.get().save();
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshButtons() {
        displayButton.setMessage(new LiteralText("Display: " + (HaConfig.get().subSkillTimerSlim ? "Slim" : "Full")));
    }

    private static String onOff(boolean value) {
        return value ? "§aEnabled" : "§cDisabled";
    }

    private static Double parsePositiveDouble(String value) {
        try {
            double parsed = Double.parseDouble(value.trim());
            return parsed > 0.0D ? Double.valueOf(parsed) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
