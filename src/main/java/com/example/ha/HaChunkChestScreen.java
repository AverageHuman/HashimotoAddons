package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaChunkChestScreen extends Screen {
    private static final Text TITLE = new LiteralText("Chunk Containers");

    private final Screen parent;

    public HaChunkChestScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = this.height / 4;
        addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText("Container Counter: " + onOff(config.chunkChestCounterEnabled)), button -> {
            config.chunkChestCounterEnabled = !config.chunkChestCounterEnabled;
            config.save();
            button.setMessage(new LiteralText("Container Counter: " + onOff(config.chunkChestCounterEnabled)));
        }));

        addButton(new ButtonWidget(centerX - 105, top + 32, 210, 20, new LiteralText("Adjust Overlay Position"), button -> {
            if (client != null) {
                client.openScreen(new HaChunkChestOverlayScreen(this));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 14, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("\u73fe\u5728\u3044\u308b\u30c1\u30e3\u30f3\u30af\u306e\u30c1\u30a7\u30b9\u30c8\u3068\u30bf\u30eb\u306e\u6570\u3092\u8868\u793a\u3057\u307e\u3059"), this.width / 2, 28, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
