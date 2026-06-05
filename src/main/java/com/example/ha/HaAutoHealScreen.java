package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaAutoHealScreen extends Screen {
    private static final Text TITLE = new LiteralText("Auto Heal");
    private static final int FIELD_WIDTH = 72;

    private final Screen parent;
    private TextFieldWidget healRatioField;
    private TextFieldWidget healCooldownField;
    private ButtonWidget healToggleButton;
    private ButtonWidget healSlotButton;

    public HaAutoHealScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int y = this.height / 4;

        healToggleButton = addButton(new ButtonWidget(centerX - 105, y, 100, 20, new LiteralText(""), button -> {
            config.autoHealEnabled = !config.autoHealEnabled;
            config.save();
            refreshButtons();
        }));

        healSlotButton = addButton(new ButtonWidget(centerX + 5, y, 100, 20, new LiteralText(""), button -> {
            config.autoHealHotbarSlot = nextSlot(config.autoHealHotbarSlot);
            config.save();
            refreshButtons();
        }));

        healCooldownField = new TextFieldWidget(this.textRenderer, centerX - 22, y + 36, FIELD_WIDTH, 20, new LiteralText("Cooldown Seconds"));
        healCooldownField.setText(Double.toString(config.autoHealCooldownSeconds));
        healCooldownField.setChangedListener(value -> {
            Double parsed = parsePositiveDouble(value);
            if (parsed != null) {
                config.autoHealCooldownSeconds = parsed.doubleValue();
                config.save();
            }
        });
        children.add(healCooldownField);

        healRatioField = new TextFieldWidget(this.textRenderer, centerX + 8, y + 62, FIELD_WIDTH, 20, new LiteralText("HP Percentage"));
        healRatioField.setText(Integer.toString(Math.round(config.autoHealHealthRatioThreshold * 100.0F)));
        healRatioField.setChangedListener(value -> {
            Float parsed = parsePercent(value);
            if (parsed != null) {
                config.autoHealHealthRatioThreshold = parsed.floatValue();
                config.save();
            }
        });
        children.add(healRatioField);

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void tick() {
        healRatioField.tick();
        healCooldownField.tick();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 18, 0xFFFFFF);
        int centerX = this.width / 2;
        int y = this.height / 4;
        this.textRenderer.draw(matrices, "CD", centerX - 105, y + 42, 0xFFFFFF);
        this.textRenderer.draw(matrices, "seconds", centerX + 56, y + 42, 0xFFFFFF);
        this.textRenderer.draw(matrices, "HP Percentage:", centerX - 105, y + 68, 0xFFFFFF);
        this.textRenderer.draw(matrices, "%", centerX + 86, y + 68, 0xFFFFFF);

        healCooldownField.render(matrices, mouseX, mouseY, delta);
        healRatioField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return healRatioField.charTyped(chr, modifiers)
            || healCooldownField.charTyped(chr, modifiers)
            || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return healRatioField.keyPressed(keyCode, scanCode, modifiers)
            || healCooldownField.keyPressed(keyCode, scanCode, modifiers)
            || super.keyPressed(keyCode, scanCode, modifiers);
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
        HaConfig config = HaConfig.get();
        healToggleButton.setMessage(new LiteralText("Auto Heal: " + (config.autoHealEnabled ? "§aEnabled" : "§cDisabled")));
        healSlotButton.setMessage(new LiteralText("Heal Key: " + (config.autoHealHotbarSlot + 1)));
    }

    private static int nextSlot(int current) {
        return current >= 8 ? 0 : current + 1;
    }

    private static Float parsePercent(String value) {
        try {
            float parsed = Float.parseFloat(value.trim());
            return parsed > 0.0F && parsed <= 100.0F ? Float.valueOf(parsed / 100.0F) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
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
