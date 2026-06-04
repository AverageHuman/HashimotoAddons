package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaElementTrackerTargetScreen extends Screen {
    private static final Text TITLE = new LiteralText("Select Target Element");

    private final Screen parent;

    public HaElementTrackerTargetScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 28;
        int spacing = 20;
        int rarityX = centerX - 135;
        int rarityWidth = 104;
        int toggleX = centerX - 23;
        int toggleWidth = 158;

        int row = 0;
        for (HaElementTracker.ElementType type : HaElementTracker.ElementType.values()) {
            final HaElementTracker.ElementType elementType = type;
            int y = top + row * spacing;

            addButton(new ButtonWidget(rarityX, y, rarityWidth, 20, new LiteralText(""), button -> {
                HaConfig.ElementTrackerTargetEntry target = HaConfig.get().getOrCreateElementTrackerTarget(elementType.getKey());
                HaElementTracker.ElementRank currentRank = HaElementTracker.ElementRank.fromKey(target.targetRank);
                target.targetRank = (currentRank == null ? HaElementTracker.ElementRank.LEGENDARY : currentRank).nextTarget().getKey();
                HaConfig.get().save();
                refreshButtons();
            }));

            addButton(new ButtonWidget(toggleX, y, toggleWidth, 20, new LiteralText(""), button -> {
                HaConfig.ElementTrackerTargetEntry target = HaConfig.get().getOrCreateElementTrackerTarget(elementType.getKey());
                target.enabled = !target.enabled;
                HaConfig.get().save();
                refreshButtons();
            }));
            row++;
        }

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Set a target rarity and enable tracking for each element."), this.width / 2, 24, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshButtons() {
        HaConfig config = HaConfig.get();
        int index = 0;
        for (HaElementTracker.ElementType type : HaElementTracker.ElementType.values()) {
            HaConfig.ElementTrackerTargetEntry target = config.getOrCreateElementTrackerTarget(type.getKey());
            HaElementTracker.ElementRank rank = HaElementTracker.ElementRank.fromKey(target.targetRank);
            if (index * 2 + 1 >= this.buttons.size()) {
                break;
            }
            this.buttons.get(index * 2).setMessage(new LiteralText("Target: " + (rank == null ? "Legendary" : rank.getLabel())));
            this.buttons.get(index * 2 + 1).setMessage(new LiteralText(type.getDisplayName() + ": " + onOff(target.enabled)));
            index++;
        }
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
