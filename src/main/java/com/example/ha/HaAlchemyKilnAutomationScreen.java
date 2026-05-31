package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaAlchemyKilnAutomationScreen extends Screen {
    private static final Text TITLE = new LiteralText("Alchemy Kiln Assist");

    private final Screen parent;
    private ButtonWidget enabledButton;
    private ButtonWidget keyButton;
    private TextFieldWidget intervalField;
    private boolean waitingForToggleKey;

    public HaAlchemyKilnAutomationScreen(Screen parent) {
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
        int top = 48;

        enabledButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText(""), button -> {
            config.alchemyKilnAutomationEnabled = !config.alchemyKilnAutomationEnabled;
            config.save();
            refreshButtons();
        }));
        keyButton = addButton(new ButtonWidget(centerX - 105, top + 28, 210, 20, new LiteralText(""), button -> {
            waitingForToggleKey = true;
            refreshButtons();
        }));

        intervalField = new TextFieldWidget(this.textRenderer, centerX - 32, top + 64, 72, 20, new LiteralText("Click Interval Ticks"));
        intervalField.setText(Integer.toString(config.alchemyKilnAutomationClickIntervalTicks));
        intervalField.setChangedListener(value -> {
            Integer parsed = parsePositiveInt(value);
            if (parsed != null) {
                config.alchemyKilnAutomationClickIntervalTicks = parsed.intValue();
                config.save();
            }
        });
        children.add(intervalField);

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
        setInitialFocus(intervalField);
    }

    @Override
    public void tick() {
        intervalField.tick();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 18, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Uses shortcut ticket and fixed GUI slots for 錬金釜 crafting."), this.width / 2, 32, 0xA0A0A0);
        int centerX = this.width / 2;
        int top = 48;
        this.textRenderer.draw(matrices, "Click Interval:", centerX - 105, top + 70, 0xFFFFFF);
        this.textRenderer.draw(matrices, "ticks", centerX + 48, top + 70, 0xFFFFFF);
        intervalField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return waitingForToggleKey
            || intervalField.charTyped(chr, modifiers)
            || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForToggleKey) {
            if (!HaKeyCaptureHelper.shouldIgnoreKeyCapture(keyCode)) {
                applyBinding(HaKeyCaptureHelper.keyboard(keyCode, scanCode));
            }
            waitingForToggleKey = false;
            refreshButtons();
            return true;
        }
        return intervalField.keyPressed(keyCode, scanCode, modifiers)
            || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (waitingForToggleKey) {
            applyBinding(HaKeyCaptureHelper.mouse(button));
            waitingForToggleKey = false;
            refreshButtons();
            return true;
        }
        return intervalField.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
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
        enabledButton.setMessage(new LiteralText("Alchemy Kiln Assist: " + onOff(config.alchemyKilnAutomationEnabled)));
        keyButton.setMessage(new LiteralText(waitingForToggleKey ? "Press any key or mouse button..." : "Toggle Key: " + keyName(config.getAlchemyKilnAutomationKey())));
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static String keyName(InputUtil.Key key) {
        return HaKeyCaptureHelper.keyName(key);
    }

    private static Integer parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? Integer.valueOf(parsed) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void applyBinding(HaKeyCaptureHelper.InputBinding binding) {
        HaConfig config = HaConfig.get();
        config.alchemyKilnAutomationKeyCode = binding.keyCode;
        config.alchemyKilnAutomationScanCode = binding.scanCode;
        config.alchemyKilnAutomationKeyType = binding.type;
        HaClientMod.updateAlchemyKilnAutomationBinding(config.getAlchemyKilnAutomationKey());
        config.save();
    }
}
