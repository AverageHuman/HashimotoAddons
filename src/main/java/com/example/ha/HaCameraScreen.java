package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaCameraScreen extends Screen {
    private static final Text TITLE = new LiteralText("Camera");

    private final Screen parent;
    private boolean waitingForCameraToggleKey;

    public HaCameraScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 4;

        addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText(waitingForCameraToggleKey ? "Press any key or mouse button..." : "Change Camera Toggle Key"), button -> {
            waitingForCameraToggleKey = true;
            button.setMessage(new LiteralText("Press any key or mouse button..."));
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 18, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Toggle key: " + keyName(HaConfig.get().getCameraToggleKey())), this.width / 2, 30, 0xA0A0A0);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("1\u4eba\u79f0 \u3068 3\u4eba\u79f0\u5f8c\u8996\u70b9 \u306e\u307f\u3092\u5207\u308a\u66ff\u3048\u307e\u3059"), this.width / 2, 46, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForCameraToggleKey) {
            if (!HaKeyCaptureHelper.shouldIgnoreKeyCapture(keyCode)) {
                applyBinding(HaKeyCaptureHelper.keyboard(keyCode, scanCode));
            }
            waitingForCameraToggleKey = false;
            if (client != null) {
                client.openScreen(new HaCameraScreen(parent));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return waitingForCameraToggleKey || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (waitingForCameraToggleKey) {
            applyBinding(HaKeyCaptureHelper.mouse(button));
            waitingForCameraToggleKey = false;
            if (client != null) {
                client.openScreen(new HaCameraScreen(parent));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private static String keyName(InputUtil.Key key) {
        return HaKeyCaptureHelper.keyName(key);
    }

    private static void applyBinding(HaKeyCaptureHelper.InputBinding binding) {
        HaConfig config = HaConfig.get();
        config.cameraToggleKeyCode = binding.keyCode;
        config.cameraToggleScanCode = binding.scanCode;
        config.cameraToggleKeyType = binding.type;
        HaClientMod.updateCameraToggleBinding(config.getCameraToggleKey());
        config.save();
    }
}
