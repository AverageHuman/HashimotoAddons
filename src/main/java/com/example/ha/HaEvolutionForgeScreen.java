package com.example.ha;

import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaEvolutionForgeScreen extends Screen {
    private static final Text TITLE = new LiteralText("Evolution Forge Helper");

    private final Screen parent;
    private ButtonWidget enabledButton;

    public HaEvolutionForgeScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 88;

        enabledButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText("Helper: " + onOff(config.evolutionForgeHelperEnabled)), button -> {
            config.evolutionForgeHelperEnabled = !config.evolutionForgeHelperEnabled;
            config.save();
            updateButtons();
        }));
        addButton(new ButtonWidget(centerX - 105, top + 28, 210, 20, new LiteralText("Clear Learned Data"), button -> openClearConfirmation()));
        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 16, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Scanned Items: " + HaEvolutionForgeHelper.getCurrentServerItemCount()), this.width / 2, 48, 0xA0E8FF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Forge Stat Ranges: " + HaEvolutionForgeHelper.getCurrentServerStatRangeCount()), this.width / 2, 60, 0xA0E8FF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Observed Stat Bounds: " + HaEvolutionForgeHelper.getCurrentServerObservedBoundCount()), this.width / 2, 72, 0xA0E8FF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("\u30d5\u30a9\u30fc\u30b8\u753b\u9762\u3092\u958b\u304f\u3068\u30ec\u30b7\u30d4\u306e\u30b9\u30c6\u30fc\u30bf\u30b9\u7bc4\u56f2\u3092\u8aad\u307f\u53d6\u308a\u307e\u3059\u3002"), this.width / 2, 144, 0xA0A0A0);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("\u901a\u5e38\u30a2\u30a4\u30c6\u30e0\u306e\u30c4\u30fc\u30eb\u30c1\u30c3\u30d7\u3092\u30db\u30d0\u30fc\u3059\u308b\u3068\u6682\u5b9a\u30b9\u30c6\u30fc\u30bf\u30b9\u7bc4\u56f2\u3092\u5b66\u7fd2\u3057\u307e\u3059\u3002"), this.width / 2, 156, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void openClearConfirmation() {
        if (client != null) {
            client.openScreen(new ConfirmScreen(result -> {
                if (result) {
                    HaEvolutionForgeHelper.clearCurrentServerItems();
                }
                client.openScreen(new HaEvolutionForgeScreen(parent));
            }, new LiteralText("Clear Learned Data?"), new LiteralText("Scanned forge data and observed stat bounds for this server will be removed."), new LiteralText("\u00a7cClear"), new LiteralText("\u00a7aCancel")));
        }
    }

    private void updateButtons() {
        if (enabledButton != null) {
            enabledButton.setMessage(new LiteralText("Helper: " + onOff(HaConfig.get().evolutionForgeHelperEnabled)));
        }
    }

    private static String onOff(boolean value) {
        return value ? "\u00a7aEnabled" : "\u00a7cDisabled";
    }
}
