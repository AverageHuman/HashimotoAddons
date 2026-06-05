package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaChatFilterEditScreen extends Screen {
    private static final Text TITLE = new LiteralText("Edit Chat Filter");

    private final Screen parent;
    private final int filterIndex;
    private final int returnPage;

    private TextFieldWidget matchField;
    private ButtonWidget enabledButton;
    private boolean enabled = true;

    public HaChatFilterEditScreen(Screen parent, int filterIndex, int returnPage) {
        super(TITLE);
        this.parent = parent;
        this.filterIndex = filterIndex;
        this.returnPage = returnPage;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        String matchText = "";
        if (filterIndex >= 0 && filterIndex < config.chatFilterEntries.size()) {
            HaConfig.ChatFilterEntry entry = config.chatFilterEntries.get(filterIndex);
            enabled = entry.enabled;
            matchText = entry.matchText;
        }

        int centerX = this.width / 2;
        int top = 40;

        enabledButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText(""), button -> {
            enabled = !enabled;
            refreshButtons();
        }));

        matchField = new TextFieldWidget(this.textRenderer, centerX - 105, top + 48, 210, 20, new LiteralText("Match Text"));
        matchField.setText(matchText);
        matchField.setMaxLength(128);
        children.add(matchField);

        addButton(new ButtonWidget(centerX - 105, this.height - 50, 210, 20, new LiteralText("Save"), button -> saveFilter()));
        if (filterIndex >= 0 && filterIndex < config.chatFilterEntries.size()) {
            addButton(new ButtonWidget(centerX - 105, this.height - 76, 210, 20, new LiteralText("\u00a74Delete"), button -> deleteFilter()));
        }
        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
        setInitialFocus(matchField);
    }

    @Override
    public void tick() {
        matchField.tick();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        int centerX = this.width / 2;
        int top = 40;
        this.textRenderer.draw(matrices, "Hide messages containing:", centerX - 105, top + 36, 0xFFFFFF);
        this.textRenderer.draw(matrices, "Partial match only. Empty text is not saved.", centerX - 105, top + 74, 0xA0A0A0);
        matchField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return matchField.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return matchField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshButtons() {
        enabledButton.setMessage(new LiteralText("Enabled: " + (enabled ? "§aEnabled" : "§cDisabled")));
    }

    private void saveFilter() {
        String matchText = matchField.getText() == null ? "" : matchField.getText().trim();
        HaConfig config = HaConfig.get();
        config.normalize();
        if (matchText.isEmpty()) {
            if (filterIndex >= 0 && filterIndex < config.chatFilterEntries.size()) {
                config.removeChatFilterEntry(filterIndex);
            }
        } else {
            HaConfig.ChatFilterEntry entry;
            if (filterIndex >= 0 && filterIndex < config.chatFilterEntries.size()) {
                entry = config.chatFilterEntries.get(filterIndex);
            } else {
                entry = config.addChatFilterEntry();
            }
            entry.enabled = enabled;
            entry.matchText = matchText;
            entry.normalize();
        }
        config.save();
        openListScreen(filterIndex < 0);
    }

    private void deleteFilter() {
        HaConfig config = HaConfig.get();
        config.removeChatFilterEntry(filterIndex);
        config.save();
        openListScreen(false);
    }

    private void openListScreen(boolean afterAdd) {
        if (client != null) {
            HaConfig config = HaConfig.get();
            int maxPage = Math.max(0, (config.chatFilterEntries.size() - 1) / 6);
            Screen listParent = parent instanceof HaChatFilterManageScreen ? ((HaChatFilterManageScreen) parent).getParentScreen() : parent;
            client.openScreen(new HaChatFilterManageScreen(listParent, afterAdd ? maxPage : Math.min(returnPage, maxPage)));
        }
    }
}
