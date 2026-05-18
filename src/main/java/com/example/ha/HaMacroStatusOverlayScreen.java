package com.example.ha;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaMacroStatusOverlayScreen extends Screen {
    private static final Text TITLE = new LiteralText("Adjust Macro Status HUD");

    private final Screen parent;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public HaMacroStatusOverlayScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        addButton(new ButtonWidget(this.width / 2 - 105, this.height - 28, 210, 20, new LiteralText("Done"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Drag the HUD panel to move it."), this.width / 2, 28, 0xA0A0A0);

        HaConfig config = HaConfig.get();
        config.normalize();
        HaMacroStatusOverlay.drawPreview(matrices, config.macroStatusHudX, config.macroStatusHudY, config.macroEnabled, dragging);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInside((int) mouseX, (int) mouseY)) {
            HaConfig config = HaConfig.get();
            dragging = true;
            dragOffsetX = (int) mouseX - config.macroStatusHudX;
            dragOffsetY = (int) mouseY - config.macroStatusHudY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && dragging) {
            HaConfig config = HaConfig.get();
            config.macroStatusHudX = clampX((int) mouseX - dragOffsetX);
            config.macroStatusHudY = clampY((int) mouseY - dragOffsetY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            HaConfig.get().save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        HaConfig.get().save();
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private boolean isInside(int mouseX, int mouseY) {
        HaConfig config = HaConfig.get();
        int x = config.macroStatusHudX;
        int y = config.macroStatusHudY;
        int width = HaMacroStatusOverlay.getPanelWidth(MinecraftClient.getInstance());
        int height = HaMacroStatusOverlay.getPanelHeight();
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int clampX(int x) {
        int width = HaMacroStatusOverlay.getPanelWidth(MinecraftClient.getInstance());
        return Math.max(0, Math.min(x, this.width - width));
    }

    private int clampY(int y) {
        return Math.max(0, Math.min(y, this.height - HaMacroStatusOverlay.getPanelHeight()));
    }
}
