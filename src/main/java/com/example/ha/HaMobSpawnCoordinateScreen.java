package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaMobSpawnCoordinateScreen extends Screen {
    private static final Text TITLE = new LiteralText("Mob Spawn Coordinate");

    private final Screen parent;
    private TextFieldWidget nameField;
    private ButtonWidget enabledButton;
    private ButtonWidget colorSlotButton;
    private ButtonWidget renderModeButton;

    public HaMobSpawnCoordinateScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaMobSpawnCoordinateConfig config = HaConfig.get().mobSpawnCoordinate;
        config.normalize();
        int centerX = width / 2;
        int top = 44;

        enabledButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText(""), button -> {
            config.enabled = !config.enabled;
            HaMobSpawnCoordinate.onConfigurationChanged();
            HaConfig.get().save();
            refreshButtons();
        }));
        nameField = new TextFieldWidget(textRenderer, centerX - 105, top + 48, 210, 20, new LiteralText("Name Tag"));
        nameField.setText(config.targetName);
        nameField.setMaxLength(128);
        children.add(nameField);
        colorSlotButton = addButton(new ButtonWidget(centerX - 105, top + 84, 210, 20, new LiteralText(""), button -> {
            config.colorSlotIndex = (config.colorSlotIndex + 1) % 4;
            HaConfig.get().save();
            refreshButtons();
        }));
        renderModeButton = addButton(new ButtonWidget(centerX - 105, top + 108, 210, 20, new LiteralText(""), button -> {
            config.renderFullBlocks = !config.renderFullBlocks;
            HaConfig.get().save();
            refreshButtons();
        }));
        addButton(new ButtonWidget(centerX - 105, height - 50, 210, 20, new LiteralText("Save"), button -> save()));
        addButton(new ButtonWidget(centerX - 105, height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
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
        drawCenteredText(matrices, textRenderer, TITLE, width / 2, 12, 0xFFFFFF);
        int centerX = width / 2;
        int top = 44;
        textRenderer.draw(matrices, "Name Tag:", centerX - 105, top + 36, 0xFFFFFF);
        textRenderer.draw(matrices, "Exact name match. Coordinates clear when disabled or changed.", centerX - 105, top + 136, 0xA0A0A0);
        textRenderer.draw(matrices, "Color uses the selected Waypoint color slot.", centerX - 105, top + 148, 0xA0A0A0);
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

    private void save() {
        HaMobSpawnCoordinateConfig config = HaConfig.get().mobSpawnCoordinate;
        config.targetName = nameField.getText();
        config.normalize();
        HaMobSpawnCoordinate.onConfigurationChanged();
        HaConfig.get().save();
        if (client != null) {
            client.openScreen(new HaMobSpawnCoordinateScreen(parent));
        }
    }

    private void refreshButtons() {
        HaMobSpawnCoordinateConfig config = HaConfig.get().mobSpawnCoordinate;
        enabledButton.setMessage(new LiteralText("Mob Spawn Coordinate: " + (config.enabled ? "\u00a7aEnabled" : "\u00a7cDisabled")));
        colorSlotButton.setMessage(new LiteralText("Waypoint Color Slot: " + (config.colorSlotIndex + 1) + " (" + HaWaypointManager.getColorSlotName(config.colorSlotIndex) + ")").formatted(HaWaypointManager.getColorSlotFormatting(config.colorSlotIndex)));
        renderModeButton.setMessage(new LiteralText("Render Mode: " + (config.renderFullBlocks ? "Full Block" : "Outline Only")));
    }
}
