package com.example.ha;

import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaExtrasScreen extends Screen {
    private static final Text TITLE = new LiteralText("Extras");

    private final Screen parent;
    private ButtonWidget extrasButton;
    private ButtonWidget editModeButton;
    private ButtonWidget blockGalleryButton;
    private ButtonWidget hudButton;

    public HaExtrasScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            onClose();
            return;
        }

        int centerX = this.width / 2;
        int top = 42;

        extrasButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText(""), button -> {
            HaGhostWall.setExtrasEnabled(!HaConfig.get().extrasEnabled);
            refreshButtons();
        }));

        editModeButton = addButton(new ButtonWidget(centerX - 105, top + 24, 210, 20, new LiteralText(""), button -> {
            HaConfig config = HaConfig.get();
            config.ghostWallEditMode = !config.ghostWallEditMode;
            config.save();
            refreshButtons();
        }));

        blockGalleryButton = addButton(new ButtonWidget(centerX - 105, top + 48, 210, 20, new LiteralText(""), button -> {
            if (client != null) {
                client.openScreen(new HaBlockGalleryScreen(this, 0));
            }
        }));

        hudButton = addButton(new ButtonWidget(centerX - 105, top + 72, 210, 20, new LiteralText(""), button -> {
            HaConfig config = HaConfig.get();
            config.extrasHudEnabled = !config.extrasHudEnabled;
            config.save();
            refreshButtons();
        }));

        addButton(new ButtonWidget(centerX - 105, top + 96, 210, 20, new LiteralText("Adjust Extras HUD"), button -> {
            if (client != null) {
                client.openScreen(new HaExtrasOverlayScreen(this));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, top + 120, 210, 20, new LiteralText("\u00a7cClear All Ghost Blocks"), button -> openClearAllConfirmation()));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("/ha extras toggles Extras, /ha em toggles Edit Mode."), this.width / 2, 28, 0xA0A0A0);
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
        extrasButton.setMessage(new LiteralText("Extras Visibility: " + enabledLabel(config.extrasEnabled)));
        editModeButton.setMessage(new LiteralText("Edit Mode: " + enabledLabel(config.ghostWallEditMode)));
        blockGalleryButton.setMessage(new LiteralText("Select Ghost Block: " + HaGhostWall.getSelectedBlockName()));
        hudButton.setMessage(new LiteralText("Extras HUD: " + enabledLabel(config.extrasHudEnabled)));
    }

    private void openClearAllConfirmation() {
        if (client != null) {
            client.openScreen(new ConfirmScreen(result -> {
                if (result) {
                    HaGhostWall.clearAll();
                }
                client.openScreen(new HaExtrasScreen(parent));
            }, new LiteralText("Clear All Ghost Blocks?"), new LiteralText("All saved ghost blocks will be restored and deleted."), new LiteralText("\u00a7cClear All"), new LiteralText("\u00a7aCancel")));
        }
    }

    static String enabledLabel(boolean value) {
        return value ? "Enabled" : "Disabled";
    }
}
