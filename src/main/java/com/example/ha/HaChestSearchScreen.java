package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaChestSearchScreen extends Screen {
    private static final Text TITLE = new LiteralText("Chest Search");

    private final Screen parent;
    private TextFieldWidget searchField;
    private ButtonWidget enabledButton;
    private ButtonWidget keyButton;
    private boolean waitingForShortcutKey;

    public HaChestSearchScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 42;
        enabledButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText(""), button -> {
            config.chestSearchEnabled = !config.chestSearchEnabled;
            config.save();
            refreshButtons();
        }));

        keyButton = addButton(new ButtonWidget(centerX - 105, top + 24, 210, 20, new LiteralText(""), button -> {
            waitingForShortcutKey = true;
            keyButton.setMessage(new LiteralText("Press any key or mouse button..."));
        }));

        searchField = new TextFieldWidget(this.textRenderer, centerX - 105, top + 72, 210, 20, new LiteralText("Search"));
        searchField.setText(config.chestSearchQuery);
        searchField.setMaxLength(64);
        children.add(searchField);

        addButton(new ButtonWidget(centerX - 105, top + 106, 100, 20, new LiteralText("Save"), button -> saveQuery()));
        addButton(new ButtonWidget(centerX + 5, top + 106, 100, 20, new LiteralText("Clear Index"), button -> {
            openClearIndexConfirmation();
        }));
        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
        setInitialFocus(searchField);
    }

    @Override
    public void tick() {
        searchField.tick();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        int centerX = this.width / 2;
        int top = 42;
        this.textRenderer.draw(matrices, "Search Item:", centerX - 105, top + 60, 0xFFFFFF);
        this.textRenderer.draw(matrices, "Recorded chests: " + HaChestSearchIndex.get().getRecordCount(), centerX - 105, top + 136, 0xA0A0A0);
        searchField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return waitingForShortcutKey || searchField.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForShortcutKey) {
            if (!HaKeyCaptureHelper.shouldIgnoreKeyCapture(keyCode)) {
                applyBinding(HaKeyCaptureHelper.keyboard(keyCode, scanCode));
            }
            waitingForShortcutKey = false;
            refreshButtons();
            return true;
        }
        return searchField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (waitingForShortcutKey) {
            applyBinding(HaKeyCaptureHelper.mouse(button));
            waitingForShortcutKey = false;
            refreshButtons();
            return true;
        }
        return searchField.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        saveQuery();
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void saveQuery() {
        HaConfig config = HaConfig.get();
        if (searchField != null) {
            config.chestSearchQuery = searchField.getText();
        }
        config.save();
    }

    private void openClearIndexConfirmation() {
        saveQuery();
        if (client != null) {
            client.openScreen(new ConfirmScreen(result -> {
                if (result) {
                    HaChestSearchIndex.get().clear();
                }
                client.openScreen(new HaChestSearchScreen(parent));
            }, new LiteralText("Clear Chest Search Index?"), new LiteralText("Recorded chest contents will be deleted."), new LiteralText("\u00a7cClear Index"), new LiteralText("\u00a7aCancel")));
        }
    }

    private void refreshButtons() {
        enabledButton.setMessage(new LiteralText("Chest Search: " + (HaConfig.get().chestSearchEnabled ? "§aEnabled" : "§cDisabled")));
        keyButton.setMessage(new LiteralText(waitingForShortcutKey ? "Press any key or mouse button..." : "Shortcut Key: " + keyName(HaConfig.get().getChestSearchKey())));
    }

    private static String keyName(InputUtil.Key key) {
        return HaKeyCaptureHelper.keyName(key);
    }

    private static void applyBinding(HaKeyCaptureHelper.InputBinding binding) {
        HaConfig config = HaConfig.get();
        config.chestSearchKeyCode = binding.keyCode;
        config.chestSearchScanCode = binding.scanCode;
        config.chestSearchKeyType = binding.type;
        HaClientMod.updateChestSearchBinding(config.getChestSearchKey());
        config.save();
    }
}
