package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public final class HaPanicModeScreen extends Screen {
    private final Screen parent;
    private boolean waitingForKey;
    private ButtonWidget enabledButton;
    private ButtonWidget keyButton;

    public HaPanicModeScreen(Screen parent) {
        super(new LiteralText("Panic Mode"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 105;
        enabledButton = addButton(new ButtonWidget(x, 62, 210, 20, new LiteralText(""), button -> {
            HaPanicModeConfig config = HaConfig.get().panicMode;
            config.enabled = !config.enabled;
            if (!config.enabled) HaPanicMode.resume(client);
            HaConfig.get().save();
            refresh();
        }));
        keyButton = addButton(new ButtonWidget(x, 90, 210, 20, new LiteralText(""), button -> {
            waitingForKey = true;
            refresh();
        }));
        addButton(new ButtonWidget(x, height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refresh();
    }

    private void refresh() {
        enabledButton.setMessage(new LiteralText("Panic Mode: " + (HaConfig.get().panicMode.enabled ? "\u00a7aEnabled" : "\u00a7cDisabled")));
        keyButton.setMessage(new LiteralText(waitingForKey ? "Press any key or mouse button..."
            : "Key: " + HaKeyCaptureHelper.keyName(HaConfig.get().panicMode.getKey())));
    }

    private void bind(HaKeyCaptureHelper.InputBinding binding) {
        HaPanicModeConfig config = HaConfig.get().panicMode;
        config.keyCode = binding.keyCode;
        config.scanCode = binding.scanCode;
        config.keyType = binding.type;
        HaConfig.get().save();
        waitingForKey = false;
        refresh();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!waitingForKey) return super.keyPressed(keyCode, scanCode, modifiers);
        if (HaKeyCaptureHelper.shouldIgnoreKeyCapture(keyCode)) {
            waitingForKey = false;
            refresh();
        } else {
            bind(HaKeyCaptureHelper.keyboard(keyCode, scanCode));
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (!waitingForKey) return super.mouseClicked(x, y, button);
        bind(HaKeyCaptureHelper.mouse(button));
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return waitingForKey || super.charTyped(chr, modifiers);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, textRenderer, title, width / 2, 18, 0xFFFFFF);
        drawCenteredText(matrices, textRenderer, "Press in game to suspend; press again to restore.", width / 2, 34, 0xA0A0A0);
        drawCenteredText(matrices, textRenderer, HaPanicMode.isActive() ? "Features suspended (settings preserved)" : "Features running normally", width / 2, 124, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() { if (client != null) client.openScreen(parent); }
}
