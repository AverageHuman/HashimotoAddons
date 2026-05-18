package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaHpAlertEditScreen extends Screen {
    private static final Text TITLE = new LiteralText("Edit HP Alert");

    private final Screen parent;
    private final int alertIndex;
    private final int returnPage;

    private TextFieldWidget titleField;
    private TextFieldWidget percentageField;
    private ButtonWidget enabledButton;
    private ButtonWidget colorButton;
    private boolean enabled = true;
    private int colorIndex = 0;

    public HaHpAlertEditScreen(Screen parent, int alertIndex, int returnPage) {
        super(TITLE);
        this.parent = parent;
        this.alertIndex = alertIndex;
        this.returnPage = returnPage;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        String titleText = "HP ALERT";
        int percentage = 50;
        if (alertIndex >= 0 && alertIndex < config.hpAlertEntries.size()) {
            HaConfig.HpAlertEntry entry = config.hpAlertEntries.get(alertIndex);
            enabled = entry.enabled;
            titleText = entry.titleText;
            percentage = entry.healthPercentage;
            colorIndex = entry.colorIndex;
        }

        int centerX = this.width / 2;
        int top = 40;

        enabledButton = addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText(""), button -> {
            enabled = !enabled;
            refreshButtons();
        }));

        titleField = new TextFieldWidget(this.textRenderer, centerX - 105, top + 34, 210, 20, new LiteralText("Alert Title"));
        titleField.setText(titleText);
        titleField.setMaxLength(64);
        children.add(titleField);

        percentageField = new TextFieldWidget(this.textRenderer, centerX - 18, top + 78, 64, 20, new LiteralText("HP Percentage"));
        percentageField.setText(Integer.toString(percentage));
        children.add(percentageField);

        colorButton = addButton(new ButtonWidget(centerX - 105, top + 112, 210, 20, new LiteralText(""), button -> {
            colorIndex = colorIndex >= HaConfig.TITLE_COLORS.length - 1 ? 0 : colorIndex + 1;
            refreshButtons();
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 50, 210, 20, new LiteralText("Save"), button -> saveAlert()));
        if (alertIndex >= 0 && alertIndex < config.hpAlertEntries.size()) {
            addButton(new ButtonWidget(centerX - 105, this.height - 76, 210, 20, new LiteralText("\u00a74Delete"), button -> deleteAlert()));
        }
        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
        setInitialFocus(titleField);
    }

    @Override
    public void tick() {
        titleField.tick();
        percentageField.tick();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        int centerX = this.width / 2;
        int top = 40;
        this.textRenderer.draw(matrices, "Title:", centerX - 105, top + 24, 0xFFFFFF);
        this.textRenderer.draw(matrices, "HP Percentage:", centerX - 105, top + 84, 0xFFFFFF);
        this.textRenderer.draw(matrices, "%", centerX + 50, top + 84, 0xFFFFFF);
        titleField.render(matrices, mouseX, mouseY, delta);
        percentageField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return titleField.charTyped(chr, modifiers)
            || percentageField.charTyped(chr, modifiers)
            || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return titleField.keyPressed(keyCode, scanCode, modifiers)
            || percentageField.keyPressed(keyCode, scanCode, modifiers)
            || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshButtons() {
        enabledButton.setMessage(new LiteralText("Enabled: " + (enabled ? "ON" : "OFF")));
        colorButton.setMessage(new LiteralText("Title Color: " + HaConfig.TITLE_COLOR_NAMES[colorIndex]));
    }

    private void saveAlert() {
        Integer percentage = parsePercentage(percentageField.getText());
        if (percentage == null) {
            percentage = Integer.valueOf(50);
        }

        HaConfig config = HaConfig.get();
        config.normalize();
        HaConfig.HpAlertEntry entry;
        if (alertIndex >= 0 && alertIndex < config.hpAlertEntries.size()) {
            entry = config.hpAlertEntries.get(alertIndex);
        } else {
            entry = config.addHpAlertEntry();
        }
        entry.enabled = enabled;
        entry.titleText = titleField.getText();
        entry.healthPercentage = percentage.intValue();
        entry.colorIndex = colorIndex;
        entry.triggered = false;
        entry.normalize();
        config.save();

        if (client != null) {
            int nextPage = (config.hpAlertEntries.size() - 1) / 6;
            Screen listParent = parent instanceof HaHpAlertListScreen ? ((HaHpAlertListScreen) parent).getParentScreen() : parent;
            client.openScreen(new HaHpAlertListScreen(listParent, alertIndex >= 0 ? returnPage : nextPage));
        }
    }

    private void deleteAlert() {
        HaConfig config = HaConfig.get();
        config.removeHpAlertEntry(alertIndex);
        config.save();
        if (client != null) {
            int maxPage = Math.max(0, (config.hpAlertEntries.size() - 1) / 6);
            Screen listParent = parent instanceof HaHpAlertListScreen ? ((HaHpAlertListScreen) parent).getParentScreen() : parent;
            client.openScreen(new HaHpAlertListScreen(listParent, Math.min(returnPage, maxPage)));
        }
    }

    private static Integer parsePercentage(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= 1 && parsed <= 100 ? Integer.valueOf(parsed) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
