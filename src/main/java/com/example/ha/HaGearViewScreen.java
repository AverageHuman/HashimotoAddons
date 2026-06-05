package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaGearViewScreen extends Screen {
    private static final Text TITLE = new LiteralText("Gear View");

    private final Screen parent;
    private ButtonWidget enabledButton;
    private ButtonWidget keyButton;
    private boolean waitingForViewKey;

    public HaGearViewScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 60;

        enabledButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText(""), button -> {
            config.gearViewEnabled = !config.gearViewEnabled;
            config.save();
            refreshButtons();
        }));
        keyButton = addButton(new ButtonWidget(centerX - 105, top + 32, 210, 20, new LiteralText(""), button -> {
            waitingForViewKey = true;
            refreshButtons();
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 16, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Aim at a player and press the key to print visible gear into chat."), this.width / 2, 34, 0xA0A0A0);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Hover the chat item to inspect the client-visible tooltip and lore."), this.width / 2, 46, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForViewKey) {
            if (!HaKeyCaptureHelper.shouldIgnoreKeyCapture(keyCode)) {
                applyBinding(HaKeyCaptureHelper.keyboard(keyCode, scanCode));
            }
            waitingForViewKey = false;
            refreshButtons();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (waitingForViewKey) {
            applyBinding(HaKeyCaptureHelper.mouse(button));
            waitingForViewKey = false;
            refreshButtons();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return waitingForViewKey || super.charTyped(chr, modifiers);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshButtons() {
        HaConfig config = HaConfig.get();
        enabledButton.setMessage(new LiteralText("Gear View: " + onOff(config.gearViewEnabled)));
        keyButton.setMessage(new LiteralText(waitingForViewKey ? "Press any key or mouse button..." : "View Key: " + keyName(config.getGearViewKey())));
    }

    private void applyBinding(HaKeyCaptureHelper.InputBinding binding) {
        HaConfig config = HaConfig.get();
        config.gearViewKeyCode = binding.keyCode;
        config.gearViewKeyScanCode = binding.scanCode;
        config.gearViewKeyType = binding.type;
        HaClientMod.updateGearViewBinding(config.getGearViewKey());
        config.save();
    }

    private static String onOff(boolean value) {
        return value ? "§aEnabled" : "§cDisabled";
    }

    private static String keyName(InputUtil.Key key) {
        return HaKeyCaptureHelper.keyName(key);
    }
}
