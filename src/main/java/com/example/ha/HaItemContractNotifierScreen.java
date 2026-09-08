package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

/** Groups Item Contract Notifier controls and its warning test action. */
public final class HaItemContractNotifierScreen extends Screen {
    private static final Text TITLE = new LiteralText("Item Contract Notifier");

    private final Screen parent;
    private ButtonWidget notifierButton;

    public HaItemContractNotifierScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        int centerX = this.width / 2;
        int top = 48;

        notifierButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText(""), button -> {
            config.itemContractNotifier.enabled = !config.itemContractNotifier.enabled;
            config.save();
            refreshButtons();
        }));
        addButton(new ButtonWidget(centerX - 105, top + 28, 210, 20, new LiteralText("Test Warning"), button -> HaItemContractNotifier.showTestWarning(client)));
        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 16, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Warns when Fast Travel opens with an unprotected target item."), this.width / 2, 32, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshButtons() {
        HaItemContractNotifierConfig config = HaConfig.get().itemContractNotifier;
        if (notifierButton != null) {
            notifierButton.setMessage(new LiteralText("Contract Notifier: " + onOff(config.enabled)));
        }
    }

    private static String onOff(boolean value) {
        return value ? "\u00a7aEnabled" : "\u00a7cDisabled";
    }
}
