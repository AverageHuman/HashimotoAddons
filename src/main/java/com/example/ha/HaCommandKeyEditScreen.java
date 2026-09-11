package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

/** Edits the Full-only Command Key binding and command text. */
public final class HaCommandKeyEditScreen extends Screen {
    private static final Text TITLE = new LiteralText("Edit Command Key");

    private final Screen parent;
    private TextFieldWidget commandField;
    private ButtonWidget keyButton;
    private boolean waitingForKey;

    public HaCommandKeyEditScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            onClose();
            return;
        }
        HaCommandKeyConfig config = HaConfig.get().commandKey;
        config.normalize();
        int centerX = width / 2;
        int top = 52;
        keyButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText(""), button -> {
            waitingForKey = true;
            refreshKeyButton();
        }));
        commandField = new TextFieldWidget(textRenderer, centerX - 105, top + 48, 210, 20, new LiteralText("Command"));
        commandField.setMaxLength(256);
        commandField.setText(config.command);
        children.add(commandField);
        addButton(new ButtonWidget(centerX - 105, height - 50, 210, 20, new LiteralText("Save"), button -> save()));
        addButton(new ButtonWidget(centerX - 105, height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshKeyButton();
        setInitialFocus(commandField);
    }

    @Override
    public void tick() {
        commandField.tick();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, textRenderer, TITLE, width / 2, 18, 0xFFFFFF);
        textRenderer.draw(matrices, "Command:", width / 2 - 105, 88, 0xFFFFFF);
        textRenderer.draw(matrices, "Enter the leading / yourself (example: /bp).", width / 2 - 105, 124, 0xA0A0A0);
        commandField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return waitingForKey || commandField.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForKey) {
            if (!HaKeyCaptureHelper.shouldIgnoreKeyCapture(keyCode)) {
                applyBinding(HaKeyCaptureHelper.keyboard(keyCode, scanCode));
            }
            waitingForKey = false;
            refreshKeyButton();
            return true;
        }
        return commandField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (waitingForKey) {
            applyBinding(HaKeyCaptureHelper.mouse(button));
            waitingForKey = false;
            refreshKeyButton();
            return true;
        }
        return commandField.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshKeyButton() {
        keyButton.setMessage(new LiteralText(waitingForKey
            ? "Press any key or mouse button..."
            : "Command Key: " + HaKeyCaptureHelper.keyName(HaConfig.get().commandKey.getKey())));
    }

    private void applyBinding(HaKeyCaptureHelper.InputBinding binding) {
        HaCommandKeyConfig config = HaConfig.get().commandKey;
        config.keyCode = binding.keyCode;
        config.scanCode = binding.scanCode;
        config.keyType = binding.type;
        HaClientMod.updateCommandKeyBinding(config.getKey());
        HaConfig.get().save();
    }

    private void save() {
        HaConfig config = HaConfig.get();
        config.commandKey.command = commandField.getText();
        config.normalize();
        config.save();
        if (client != null) {
            client.openScreen(new HaCommandKeyScreen(parent));
        }
    }
}
