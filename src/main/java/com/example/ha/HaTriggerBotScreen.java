package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaTriggerBotScreen extends Screen {
    private static final Text TITLE = new LiteralText("TriggerBot");

    private final Screen parent;
    private ButtonWidget enabledButton;
    private ButtonWidget selectedMacroButton;
    private TextFieldWidget cooldownField;

    public HaTriggerBotScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            onClose();
            return;
        }

        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 40;
        int left = centerX - 105;
        int right = centerX + 5;

        enabledButton = addButton(new ButtonWidget(left, top, 100, 20, new LiteralText(""), button -> {
            config.triggerBotEnabled = !config.triggerBotEnabled;
            config.save();
            refreshButtons();
        }));

        selectedMacroButton = addButton(new ButtonWidget(right, top, 100, 20, new LiteralText(""), button -> {
            if (!config.swapEntries.isEmpty()) {
                config.triggerBotMacroIndex = (config.triggerBotMacroIndex + 1) % config.swapEntries.size();
                config.save();
            }
            refreshButtons();
        }));

        cooldownField = new TextFieldWidget(this.textRenderer, left, top + 66, 210, 20, new LiteralText("Cooldown Seconds"));
        cooldownField.setMaxLength(16);
        cooldownField.setText(Double.toString(config.triggerBotCooldownSeconds));
        cooldownField.setChangedListener(value -> {
            Double parsed = parsePositiveDouble(value);
            if (parsed != null) {
                config.triggerBotCooldownSeconds = parsed.doubleValue();
                config.save();
            }
        });
        children.add(cooldownField);

        addButton(new ButtonWidget(10, 10, 78, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
        setInitialFocus(cooldownField);
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
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 16, 0xFFFFFF);

        int centerX = this.width / 2;
        int top = 40;
        int left = centerX - 105;
        this.textRenderer.draw(matrices, "HP Threshold: 50000", left, top + 30, 0xA0A0A0);
        this.textRenderer.draw(matrices, "Cooldown Seconds:", left, top + 56, 0xA0A0A0);

        cooldownField.render(matrices, mouseX, mouseY, delta);
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
        config.normalize();
        enabledButton.setMessage(new LiteralText("TriggerBot: " + onOff(config.triggerBotEnabled)));
        selectedMacroButton.setMessage(new LiteralText("Macro: " + macroName(config)));
        selectedMacroButton.active = !config.swapEntries.isEmpty();
    }

    private static String macroName(HaConfig config) {
        if (config.swapEntries.isEmpty()) {
            return "None";
        }
        int index = Math.max(0, Math.min(config.triggerBotMacroIndex, config.swapEntries.size() - 1));
        String name = config.swapEntries.get(index).name;
        if (name.length() > 10) {
            return name.substring(0, 10);
        }
        return name;
    }

    private static String onOff(boolean value) {
        return value ? "\u00a7aEnabled" : "\u00a7cDisabled";
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
