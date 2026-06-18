package com.example.ha;

import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaWaypointColorSelectScreen extends Screen {
    private static final Text TITLE = new LiteralText("Select Waypoint Color");

    private final Screen parent;
    private final int slotIndex;

    public HaWaypointColorSelectScreen(Screen parent, int slotIndex) {
        super(TITLE);
        this.parent = parent;
        this.slotIndex = Math.max(0, Math.min(3, slotIndex));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startX = centerX - 110;
        int startY = 38;
        int buttonWidth = 52;
        int buttonHeight = 20;
        int gap = 4;

        List<Formatting> colors = HaWaypointManager.getSelectableColors();
        for (int i = 0; i < colors.size(); i++) {
            final Formatting formatting = colors.get(i);
            int col = i % 4;
            int row = i / 4;
            int x = startX + col * (buttonWidth + gap);
            int y = startY + row * (buttonHeight + gap);
            addButton(new ButtonWidget(x, y, buttonWidth, buttonHeight, new LiteralText(formatting.getName()).formatted(formatting), button -> {
                HaWaypointManager.setColorSlotFormatting(slotIndex, formatting);
                if (client != null) {
                    client.openScreen(parent);
                }
            }));
        }

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Slot " + (slotIndex + 1) + " currently uses " + HaWaypointManager.getColorSlotName(slotIndex) + "."), this.width / 2, 26, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }
}
