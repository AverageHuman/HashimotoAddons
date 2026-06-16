package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class HaWaypointLabelScreen extends Screen {
    private static final Text TITLE = new LiteralText("Waypoint Label");

    private final Screen parent;
    private final String dimensionKey;
    private final BlockPos pos;
    private final boolean editingExisting;
    private final int initialColorSlot;
    private final String initialLabel;

    private TextFieldWidget labelField;

    public HaWaypointLabelScreen(Screen parent, String dimensionKey, BlockPos pos, String initialLabel, int initialColorSlot, boolean editingExisting) {
        super(TITLE);
        this.parent = parent;
        this.dimensionKey = dimensionKey;
        this.pos = pos;
        this.initialLabel = initialLabel == null ? "" : initialLabel;
        this.initialColorSlot = Math.max(0, Math.min(3, initialColorSlot));
        this.editingExisting = editingExisting;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = 42;

        labelField = new TextFieldWidget(this.textRenderer, centerX - 90, top + 18, 180, 20, new LiteralText("Waypoint Label"));
        labelField.setMaxLength(64);
        labelField.setText(initialLabel);
        children.add(labelField);
        setInitialFocus(labelField);

        addButton(new ButtonWidget(centerX - 105, this.height - 50, 210, 20, new LiteralText("Save"), button -> saveWaypoint()));
        if (editingExisting) {
            addButton(new ButtonWidget(centerX - 105, this.height - 76, 210, 20, new LiteralText("Delete"), button -> deleteWaypoint()));
        }
        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void tick() {
        if (labelField != null) {
            labelField.tick();
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        this.textRenderer.draw(matrices, "Label text is shown in white.", this.width / 2 - 90, 38, 0xA0A0A0);
        this.textRenderer.draw(matrices, "Position: " + HaWaypointManager.formatPosition(pos), this.width / 2 - 90, 54, 0xA0E8FF);
        labelField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return labelField.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (labelField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void saveWaypoint() {
        String label = labelField.getText() == null ? "" : labelField.getText().trim();
        if (label.isEmpty()) {
            label = HaWaypointManager.formatPosition(pos);
        }
        HaWaypointManager.upsertWaypoint(dimensionKey, pos, label, initialColorSlot);
        if (client != null) {
            client.openScreen(resolveReturnScreen());
        }
    }

    private void deleteWaypoint() {
        HaWaypointManager.removeWaypoint(dimensionKey, pos);
        if (client != null) {
            client.openScreen(resolveReturnScreen());
        }
    }

    private Screen resolveReturnScreen() {
        if (parent instanceof HaWaypointListScreen) {
            HaWaypointListScreen listScreen = (HaWaypointListScreen) parent;
            return new HaWaypointListScreen(listScreen.getParentScreen(), listScreen.getPage());
        }
        return parent;
    }
}
