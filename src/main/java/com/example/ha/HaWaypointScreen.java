package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaWaypointScreen extends Screen {
    private static final Text TITLE = new LiteralText("Waypoint");

    private final Screen parent;
    private boolean waitingForCycleKey;
    private ButtonWidget editModeButton;
    private ButtonWidget renderModeButton;
    private ButtonWidget throughWallsButton;
    private ButtonWidget activeSlotButton;
    private ButtonWidget cycleKeyButton;
    private ButtonWidget[] slotButtons;

    public HaWaypointScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaWaypointManager.load();

        int centerX = this.width / 2;
        int top = 36;
        int spacing = 24;

        editModeButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText(""), button -> {
            boolean enabled = HaWaypointManager.toggleEditMode();
            refreshButtons();
            notifyStatus("Waypoint edit mode " + (enabled ? "Enabled" : "Disabled"));
        }));

        renderModeButton = addButton(new ButtonWidget(centerX - 105, top + spacing, 210, 20, new LiteralText(""), button -> {
            boolean fullBlock = HaWaypointManager.toggleRenderMode();
            refreshButtons();
            notifyStatus("Waypoint render mode " + (fullBlock ? "Full Block" : "Outline Only"));
        }));

        throughWallsButton = addButton(new ButtonWidget(centerX - 105, top + spacing * 2, 210, 20, new LiteralText(""), button -> {
            boolean enabled = HaWaypointManager.toggleThroughWalls();
            refreshButtons();
            notifyStatus("Through-wall rendering " + (enabled ? "Enabled" : "Disabled"));
        }));

        activeSlotButton = addButton(new ButtonWidget(centerX - 105, top + spacing * 3, 210, 20, new LiteralText(""), button -> {
            int slot = HaWaypointManager.cycleActiveColorSlot();
            refreshButtons();
            notifyStatus("Active waypoint color slot: " + (slot + 1));
        }));

        cycleKeyButton = addButton(new ButtonWidget(centerX - 105, top + spacing * 4, 210, 20, new LiteralText(""), button -> {
            waitingForCycleKey = true;
            refreshButtons();
        }));

        slotButtons = new ButtonWidget[4];
        for (int i = 0; i < 4; i++) {
            final int slotIndex = i;
            int rowY = top + spacing * (5 + i);
            slotButtons[i] = addButton(new ButtonWidget(centerX - 105, rowY, 210, 20, new LiteralText(""), button -> {
                if (client != null) {
                    client.openScreen(new HaWaypointColorSelectScreen(this, slotIndex));
                }
            }));
        }

        addButton(new ButtonWidget(centerX - 105, top + spacing * 9, 210, 20, new LiteralText("Manage Current Dimension Waypoints"), button -> {
            if (client != null) {
                client.openScreen(new HaWaypointListScreen(this, 0));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void tick() {
        // No text fields to advance.
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Shift+Right Click places a labeled waypoint. Left Click removes it."), this.width / 2, 26, 0xA0A0A0);
        this.textRenderer.draw(matrices, "Dimension: " + safeDimensionKey(), this.width / 2 - 105, 210, 0xA0E8FF);
        this.textRenderer.draw(matrices, "Active Slot: " + (HaWaypointManager.getActiveColorSlot() + 1), this.width / 2 - 105, 222, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForCycleKey) {
            if (!HaKeyCaptureHelper.shouldIgnoreKeyCapture(keyCode)) {
                HaWaypointManager.setCycleColorKey(HaKeyCaptureHelper.keyboard(keyCode, scanCode));
                notifyStatus("Cycle key set to " + HaWaypointManager.getCycleColorKeyName());
            }
            waitingForCycleKey = false;
            refreshButtons();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (waitingForCycleKey) {
            HaWaypointManager.setCycleColorKey(HaKeyCaptureHelper.mouse(button));
            waitingForCycleKey = false;
            notifyStatus("Cycle key set to " + HaWaypointManager.getCycleColorKeyName());
            refreshButtons();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return waitingForCycleKey || super.charTyped(chr, modifiers);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshButtons() {
        boolean waiting = waitingForCycleKey;
        editModeButton.setMessage(new LiteralText("Waypoint Edit Mode: " + (HaWaypointManager.isEditModeEnabled() ? "\u00a7aEnabled" : "\u00a7cDisabled")));
        renderModeButton.setMessage(new LiteralText("Render Mode: " + HaWaypointManager.getRenderModeLabel()));
        throughWallsButton.setMessage(new LiteralText("Through Walls: " + (HaWaypointManager.isThroughWallsEnabled() ? "\u00a7aEnabled" : "\u00a7cDisabled")));
        activeSlotButton.setMessage(new LiteralText("Active Color Slot: " + (HaWaypointManager.getActiveColorSlot() + 1)));
        cycleKeyButton.setMessage(new LiteralText(waiting ? "Press any key or mouse button..." : "Cycle Color Key: " + HaWaypointManager.getCycleColorKeyName()));

        for (int i = 0; i < slotButtons.length; i++) {
            final int slotIndex = i;
            slotButtons[i].setMessage(new LiteralText(HaWaypointManager.getColorSlotDisplayText(slotIndex)).formatted(HaWaypointManager.getColorSlotFormatting(slotIndex)));
        }
    }

    private void notifyStatus(String message) {
        if (client != null && client.player != null) {
            client.player.sendMessage(new LiteralText("[\u00a7l\u00a7bHashimotoAddons\u00a7r]:" + message), false);
        }
    }

    private String safeDimensionKey() {
        String dimensionKey = HaWaypointManager.getCurrentDimensionKey(client);
        return dimensionKey == null ? "unknown" : dimensionKey;
    }
}
