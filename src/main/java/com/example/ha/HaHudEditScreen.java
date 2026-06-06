package com.example.ha;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaHudEditScreen extends Screen {
    private static final Text TITLE = new LiteralText("Edit HUD Layout");

    private final Screen parent;
    private final List<HudPanel> panels = new ArrayList<HudPanel>();
    private HudPanel draggingPanel;
    private int dragOffsetX;
    private int dragOffsetY;

    public HaHudEditScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        panels.clear();
        if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            panels.add(new MacroStatusPanel());
            panels.add(new ExtrasPanel());
            panels.add(new ChunkContainersPanel());
        }
        panels.add(new DropTrackerPanel());
        panels.add(new ExpTrackerPanel());
        panels.add(new ElementTrackerPanel());
        panels.add(new MobHpDisplayPanel());
        panels.add(new SubSkillTimerPanel());
        panels.add(new RitualBookTimerPanel());

        addButton(new ButtonWidget(this.width / 2 - 105, this.height - 28, 210, 20, new LiteralText("Done"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Drag any HUD panel to move it."), this.width / 2, 28, 0xA0A0A0);

        HaConfig.get().normalize();
        for (HudPanel panel : panels) {
            panel.draw(matrices, panel == draggingPanel);
        }
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = panels.size() - 1; i >= 0; i--) {
                HudPanel panel = panels.get(i);
                if (panel.contains((int) mouseX, (int) mouseY)) {
                    draggingPanel = panel;
                    dragOffsetX = (int) mouseX - panel.getX();
                    dragOffsetY = (int) mouseY - panel.getY();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingPanel != null) {
            draggingPanel.setPosition(clampX(draggingPanel, (int) mouseX - dragOffsetX), clampY(draggingPanel, (int) mouseY - dragOffsetY));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingPanel != null) {
            draggingPanel = null;
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

    private int clampX(HudPanel panel, int x) {
        return Math.max(0, Math.min(x, this.width - panel.getWidth()));
    }

    private int clampY(HudPanel panel, int y) {
        return Math.max(0, Math.min(y, this.height - panel.getHeight()));
    }

    private abstract static class HudPanel {
        abstract int getX();

        abstract int getY();

        abstract void setPosition(int x, int y);

        abstract int getWidth();

        abstract int getHeight();

        abstract void draw(MatrixStack matrices, boolean selected);

        final boolean contains(int mouseX, int mouseY) {
            int x = getX();
            int y = getY();
            return mouseX >= x && mouseX < x + getWidth() && mouseY >= y && mouseY < y + getHeight();
        }
    }

    private static final class MacroStatusPanel extends HudPanel {
        @Override
        int getX() {
            return HaConfig.get().macroStatusHudX;
        }

        @Override
        int getY() {
            return HaConfig.get().macroStatusHudY;
        }

        @Override
        void setPosition(int x, int y) {
            HaConfig.get().macroStatusHudX = x;
            HaConfig.get().macroStatusHudY = y;
        }

        @Override
        int getWidth() {
            return HaMacroStatusOverlay.getPanelWidth(MinecraftClient.getInstance());
        }

        @Override
        int getHeight() {
            return HaMacroStatusOverlay.getPanelHeight();
        }

        @Override
        void draw(MatrixStack matrices, boolean selected) {
            HaConfig config = HaConfig.get();
            HaMacroStatusOverlay.drawPreview(matrices, config.macroStatusHudX, config.macroStatusHudY, config.macroEnabled, selected);
        }
    }

    private static final class ExtrasPanel extends HudPanel {
        @Override
        int getX() {
            return HaConfig.get().extrasHudX;
        }

        @Override
        int getY() {
            return HaConfig.get().extrasHudY;
        }

        @Override
        void setPosition(int x, int y) {
            HaConfig.get().extrasHudX = x;
            HaConfig.get().extrasHudY = y;
        }

        @Override
        int getWidth() {
            return HaExtrasOverlay.getPanelWidth(MinecraftClient.getInstance());
        }

        @Override
        int getHeight() {
            return HaExtrasOverlay.getPanelHeight();
        }

        @Override
        void draw(MatrixStack matrices, boolean selected) {
            HaConfig config = HaConfig.get();
            HaExtrasOverlay.drawPreview(matrices, config.extrasHudX, config.extrasHudY, selected);
        }
    }

    private static final class ChunkContainersPanel extends HudPanel {
        @Override
        int getX() {
            return HaConfig.get().chunkChestOverlayX;
        }

        @Override
        int getY() {
            return HaConfig.get().chunkChestOverlayY;
        }

        @Override
        void setPosition(int x, int y) {
            HaConfig.get().chunkChestOverlayX = x;
            HaConfig.get().chunkChestOverlayY = y;
        }

        @Override
        int getWidth() {
            return HaChunkChestOverlay.getPanelWidth(MinecraftClient.getInstance());
        }

        @Override
        int getHeight() {
            return HaChunkChestOverlay.getPanelHeight();
        }

        @Override
        void draw(MatrixStack matrices, boolean selected) {
            HaConfig config = HaConfig.get();
            HaChunkChestOverlay.drawPreview(matrices, config.chunkChestOverlayX, config.chunkChestOverlayY, selected);
        }
    }

    private static final class DropTrackerPanel extends HudPanel {
        @Override
        int getX() {
            return HaConfig.get().dropTrackerOverlayX;
        }

        @Override
        int getY() {
            return HaConfig.get().dropTrackerOverlayY;
        }

        @Override
        void setPosition(int x, int y) {
            HaConfig.get().dropTrackerOverlayX = x;
            HaConfig.get().dropTrackerOverlayY = y;
        }

        @Override
        int getWidth() {
            return HaDropTrackerOverlay.getPanelWidth(MinecraftClient.getInstance());
        }

        @Override
        int getHeight() {
            return HaDropTrackerOverlay.getPanelHeight();
        }

        @Override
        void draw(MatrixStack matrices, boolean selected) {
            HaConfig config = HaConfig.get();
            HaDropTrackerOverlay.drawPreview(matrices, config.dropTrackerOverlayX, config.dropTrackerOverlayY, selected);
        }
    }

    private static final class ExpTrackerPanel extends HudPanel {
        @Override
        int getX() {
            return HaConfig.get().expTrackerOverlayX;
        }

        @Override
        int getY() {
            return HaConfig.get().expTrackerOverlayY;
        }

        @Override
        void setPosition(int x, int y) {
            HaConfig.get().expTrackerOverlayX = x;
            HaConfig.get().expTrackerOverlayY = y;
        }

        @Override
        int getWidth() {
            return HaExpTrackerOverlay.getPanelWidth(MinecraftClient.getInstance());
        }

        @Override
        int getHeight() {
            return HaExpTrackerOverlay.getPanelHeight();
        }

        @Override
        void draw(MatrixStack matrices, boolean selected) {
            HaConfig config = HaConfig.get();
            HaExpTrackerOverlay.drawPreview(matrices, config.expTrackerOverlayX, config.expTrackerOverlayY, selected);
        }
    }

    private static final class MobHpDisplayPanel extends HudPanel {
        @Override
        int getX() {
            return HaConfig.get().mobHpDisplayOverlayX;
        }

        @Override
        int getY() {
            return HaConfig.get().mobHpDisplayOverlayY;
        }

        @Override
        void setPosition(int x, int y) {
            HaConfig.get().mobHpDisplayOverlayX = x;
            HaConfig.get().mobHpDisplayOverlayY = y;
        }

        @Override
        int getWidth() {
            return HaMobHpDisplayOverlay.getPanelWidth(MinecraftClient.getInstance());
        }

        @Override
        int getHeight() {
            return HaMobHpDisplayOverlay.getPanelHeight();
        }

        @Override
        void draw(MatrixStack matrices, boolean selected) {
            HaConfig config = HaConfig.get();
            HaMobHpDisplayOverlay.drawPreview(matrices, config.mobHpDisplayOverlayX, config.mobHpDisplayOverlayY, selected);
        }
    }

    private static final class ElementTrackerPanel extends HudPanel {
        @Override
        int getX() {
            return HaConfig.get().elementTrackerOverlayX;
        }

        @Override
        int getY() {
            return HaConfig.get().elementTrackerOverlayY;
        }

        @Override
        void setPosition(int x, int y) {
            HaConfig.get().elementTrackerOverlayX = x;
            HaConfig.get().elementTrackerOverlayY = y;
        }

        @Override
        int getWidth() {
            return HaElementTrackerOverlay.getPanelWidth(MinecraftClient.getInstance());
        }

        @Override
        int getHeight() {
            return HaElementTrackerOverlay.getPanelHeight();
        }

        @Override
        void draw(MatrixStack matrices, boolean selected) {
            HaConfig config = HaConfig.get();
            HaElementTrackerOverlay.drawPreview(matrices, config.elementTrackerOverlayX, config.elementTrackerOverlayY, selected);
        }
    }

    private static final class SubSkillTimerPanel extends HudPanel {
        @Override
        int getX() {
            return HaConfig.get().subSkillTimerOverlayX;
        }

        @Override
        int getY() {
            return HaConfig.get().subSkillTimerOverlayY;
        }

        @Override
        void setPosition(int x, int y) {
            HaConfig.get().subSkillTimerOverlayX = x;
            HaConfig.get().subSkillTimerOverlayY = y;
        }

        @Override
        int getWidth() {
            return HaSubSkillTimerOverlay.getPanelWidth(MinecraftClient.getInstance());
        }

        @Override
        int getHeight() {
            return HaSubSkillTimerOverlay.getPanelHeight();
        }

        @Override
        void draw(MatrixStack matrices, boolean selected) {
            HaConfig config = HaConfig.get();
            HaSubSkillTimerOverlay.drawPreview(matrices, config.subSkillTimerOverlayX, config.subSkillTimerOverlayY, selected);
        }
    }

    private static final class RitualBookTimerPanel extends HudPanel {
        @Override
        int getX() {
            return HaConfig.get().ritualBookTimerOverlayX;
        }

        @Override
        int getY() {
            return HaConfig.get().ritualBookTimerOverlayY;
        }

        @Override
        void setPosition(int x, int y) {
            HaConfig.get().ritualBookTimerOverlayX = x;
            HaConfig.get().ritualBookTimerOverlayY = y;
        }

        @Override
        int getWidth() {
            return HaRitualBookTimerOverlay.getPanelWidth(MinecraftClient.getInstance());
        }

        @Override
        int getHeight() {
            return HaRitualBookTimerOverlay.getPanelHeight();
        }

        @Override
        void draw(MatrixStack matrices, boolean selected) {
            HaConfig config = HaConfig.get();
            HaRitualBookTimerOverlay.drawPreview(matrices, config.ritualBookTimerOverlayX, config.ritualBookTimerOverlayY, selected);
        }
    }
}
