package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaMobEspScreen extends Screen {
    private static final Text TITLE = new LiteralText("Mob ESP");

    private final Screen parent;
    private TextFieldWidget nameField;
    private ButtonWidget enabledButton;

    public HaMobEspScreen(Screen parent) {
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
            config.mobEspEnabled = !config.mobEspEnabled;
            config.save();
            refreshButtons();
        }));

        nameField = new TextFieldWidget(this.textRenderer, centerX - 105, top + 48, 210, 20, new LiteralText("Mob Name"));
        nameField.setText(config.mobEspTargetName);
        nameField.setMaxLength(128);
        children.add(nameField);

        addButton(new ButtonWidget(centerX - 105, this.height - 50, 210, 20, new LiteralText("Save"), button -> save()));
        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
        setInitialFocus(nameField);
    }

    @Override
    public void tick() {
        nameField.tick();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        int centerX = this.width / 2;
        int top = 48;
        this.textRenderer.draw(matrices, "Mob Name:", centerX - 105, top + 36, 0xFFFFFF);
        this.textRenderer.draw(matrices, "Partial name match. Empty name disables matching.", centerX - 105, top + 74, 0xA0A0A0);
        nameField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return nameField.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return nameField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshButtons() {
        enabledButton.setMessage(new LiteralText("Mob ESP: " + (HaConfig.get().mobEspEnabled ? "ON" : "OFF")));
    }

    private void save() {
        HaConfig config = HaConfig.get();
        config.mobEspTargetName = nameField.getText() == null ? "" : nameField.getText().trim();
        config.save();
        if (client != null) {
            client.openScreen(new HaMobEspScreen(parent));
        }
    }
}
