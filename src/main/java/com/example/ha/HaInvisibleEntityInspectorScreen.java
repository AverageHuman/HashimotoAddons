package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaInvisibleEntityInspectorScreen extends Screen {
    private static final Text TITLE = new LiteralText("Invisible Entity Inspector");

    private final Screen parent;
    private ButtonWidget enabledButton;
    private ButtonWidget keyButton;
    private boolean waitingForInspectorKey;

    public HaInvisibleEntityInspectorScreen(Screen parent) {
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
        int top = 60;

        enabledButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText(""), button -> {
            config.invisibleEntityInspectorEnabled = !config.invisibleEntityInspectorEnabled;
            config.save();
            refreshButtons();
        }));
        keyButton = addButton(new ButtonWidget(centerX - 105, top + 32, 210, 20, new LiteralText(""), button -> {
            waitingForInspectorKey = true;
            refreshButtons();
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 16, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Aim at an entity and press the key to print client-visible details."), this.width / 2, 34, 0xA0A0A0);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("A view-ray scan is used when an invisible target cannot be selected directly."), this.width / 2, 46, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForInspectorKey) {
            if (!HaKeyCaptureHelper.shouldIgnoreKeyCapture(keyCode)) {
                applyBinding(HaKeyCaptureHelper.keyboard(keyCode, scanCode));
            }
            waitingForInspectorKey = false;
            refreshButtons();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (waitingForInspectorKey) {
            applyBinding(HaKeyCaptureHelper.mouse(button));
            waitingForInspectorKey = false;
            refreshButtons();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return waitingForInspectorKey || super.charTyped(chr, modifiers);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshButtons() {
        HaConfig config = HaConfig.get();
        enabledButton.setMessage(new LiteralText("Inspector: " + onOff(config.invisibleEntityInspectorEnabled)));
        keyButton.setMessage(new LiteralText(waitingForInspectorKey
            ? "Press any key or mouse button..."
            : "Inspector Key: " + keyName(config.getInvisibleEntityInspectorKey())));
    }

    private void applyBinding(HaKeyCaptureHelper.InputBinding binding) {
        HaConfig config = HaConfig.get();
        config.invisibleEntityInspectorKeyCode = binding.keyCode;
        config.invisibleEntityInspectorScanCode = binding.scanCode;
        config.invisibleEntityInspectorKeyType = binding.type;
        HaClientMod.updateInvisibleEntityInspectorBinding(config.getInvisibleEntityInspectorKey());
        config.save();
    }

    private static String onOff(boolean value) {
        return value ? "\u00a7aEnabled" : "\u00a7cDisabled";
    }

    private static String keyName(InputUtil.Key key) {
        return HaKeyCaptureHelper.keyName(key);
    }
}
