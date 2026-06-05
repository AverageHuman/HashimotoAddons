package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaAfkFarmingScreen extends Screen {
    private static final Text TITLE = new LiteralText("AFK Farming");

    private final Screen parent;
    private ButtonWidget enabledButton;
    private ButtonWidget startButton;
    private ButtonWidget playerAlertButton;
    private ButtonWidget keyAdminAlertButton;
    private ButtonWidget mobMacroButton;
    private ButtonWidget selectedMacroButton;
    private ButtonWidget circleButton;
    private ButtonWidget debugHudButton;
    private ButtonWidget autoMoveButton;
    private ButtonWidget webhookTestButton;
    private TextFieldWidget webhookField;
    private TextFieldWidget reportIntervalField;
    private TextFieldWidget keyAdminNameField;
    private TextFieldWidget mobMinField;
    private TextFieldWidget mobMaxField;
    private TextFieldWidget mobCooldownField;
    private TextFieldWidget autoMoveIntervalField;

    public HaAfkFarmingScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            onClose();
            return;
        }

        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 34;
        int left = centerX - 105;
        int right = centerX + 5;

        enabledButton = addButton(new ButtonWidget(left, top, 100, 20, new LiteralText(""), button -> {
            config.afkFarmingEnabled = !config.afkFarmingEnabled;
            if (!config.afkFarmingEnabled) {
                config.afkFarmingActive = false;
            }
            config.save();
            refreshButtons();
        }));

        startButton = addButton(new ButtonWidget(right, top, 100, 20, new LiteralText(""), button -> {
            if (config.afkFarmingEnabled) {
                config.afkFarmingActive = !config.afkFarmingActive;
                config.save();
            }
            refreshButtons();
        }));

        playerAlertButton = addButton(new ButtonWidget(left, top + 24, 100, 20, new LiteralText(""), button -> {
            config.afkFarmingPlayerAlertsEnabled = !config.afkFarmingPlayerAlertsEnabled;
            config.save();
            refreshButtons();
        }));

        keyAdminAlertButton = addButton(new ButtonWidget(right, top + 24, 100, 20, new LiteralText(""), button -> {
            config.afkFarmingKeyAdminAlertsEnabled = !config.afkFarmingKeyAdminAlertsEnabled;
            config.save();
            refreshButtons();
        }));

        mobMacroButton = addButton(new ButtonWidget(left, top + 48, 100, 20, new LiteralText(""), button -> {
            config.afkFarmingMobMacroEnabled = !config.afkFarmingMobMacroEnabled;
            config.save();
            refreshButtons();
        }));

        selectedMacroButton = addButton(new ButtonWidget(right, top + 48, 100, 20, new LiteralText(""), button -> {
            if (!config.swapEntries.isEmpty()) {
                config.afkFarmingMobMacroIndex = (config.afkFarmingMobMacroIndex + 1) % config.swapEntries.size();
                config.save();
            }
            refreshButtons();
        }));

        circleButton = addButton(new ButtonWidget(left, top + 72, 100, 20, new LiteralText(""), button -> {
            config.afkFarmingMobCircleVisible = !config.afkFarmingMobCircleVisible;
            config.save();
            refreshButtons();
        }));

        debugHudButton = addButton(new ButtonWidget(right, top + 72, 100, 20, new LiteralText(""), button -> {
            config.afkFarmingMobDebugHudEnabled = !config.afkFarmingMobDebugHudEnabled;
            config.save();
            refreshButtons();
        }));

        autoMoveButton = addButton(new ButtonWidget(left, top + 96, 100, 20, new LiteralText(""), button -> {
            config.afkFarmingAutoMoveEnabled = !config.afkFarmingAutoMoveEnabled;
            config.save();
            refreshButtons();
        }));

        webhookTestButton = addButton(new ButtonWidget(right, top + 96, 100, 20, new LiteralText("Webhook Test"), button -> {
            if (client != null) {
                HaAfkFarming.sendWebhookTest(client);
            }
        }));

        webhookField = new TextFieldWidget(this.textRenderer, left, top + 126, 210, 20, new LiteralText("Discord Webhook"));
        webhookField.setMaxLength(512);
        webhookField.setText(config.afkFarmingWebhookUrl);
        webhookField.setChangedListener(value -> {
            config.afkFarmingWebhookUrl = value == null ? "" : value.trim();
            config.save();
        });
        children.add(webhookField);

        reportIntervalField = new TextFieldWidget(this.textRenderer, left, top + 160, 96, 20, new LiteralText("Report Minutes"));
        reportIntervalField.setText(Double.toString(config.afkFarmingReportIntervalMinutes));
        reportIntervalField.setChangedListener(value -> {
            Double parsed = parsePositiveDouble(value);
            if (parsed != null) {
                config.afkFarmingReportIntervalMinutes = parsed.doubleValue();
                config.save();
            }
        });
        children.add(reportIntervalField);

        keyAdminNameField = new TextFieldWidget(this.textRenderer, right, top + 160, 100, 20, new LiteralText("KeyAdmin Name"));
        keyAdminNameField.setText(config.afkFarmingKeyAdminName);
        keyAdminNameField.setChangedListener(value -> {
            config.afkFarmingKeyAdminName = value == null ? "" : value.trim();
            config.save();
        });
        children.add(keyAdminNameField);

        autoMoveIntervalField = new TextFieldWidget(this.textRenderer, left, top + 196, 96, 20, new LiteralText("Auto Move Seconds"));
        autoMoveIntervalField.setText(Double.toString(config.afkFarmingAutoMoveIntervalSeconds));
        autoMoveIntervalField.setChangedListener(value -> {
            Double parsed = parsePositiveDouble(value);
            if (parsed != null) {
                config.afkFarmingAutoMoveIntervalSeconds = parsed.doubleValue();
                config.save();
            }
        });
        children.add(autoMoveIntervalField);

        mobMinField = new TextFieldWidget(this.textRenderer, left, top + 232, 44, 20, new LiteralText("Living Min"));
        mobMinField.setText(Integer.toString(config.afkFarmingMobMinCount));
        mobMinField.setChangedListener(value -> {
            Integer parsed = parsePositiveInt(value);
            if (parsed != null) {
                config.afkFarmingMobMinCount = parsed.intValue();
                config.normalize();
                config.save();
            }
        });
        children.add(mobMinField);

        mobMaxField = new TextFieldWidget(this.textRenderer, left + 52, top + 232, 44, 20, new LiteralText("Living Max"));
        mobMaxField.setText(Integer.toString(config.afkFarmingMobMaxCount));
        mobMaxField.setChangedListener(value -> {
            Integer parsed = parsePositiveInt(value);
            if (parsed != null) {
                config.afkFarmingMobMaxCount = parsed.intValue();
                config.normalize();
                config.save();
            }
        });
        children.add(mobMaxField);

        mobCooldownField = new TextFieldWidget(this.textRenderer, right, top + 232, 100, 20, new LiteralText("Mob Cooldown"));
        mobCooldownField.setText(Double.toString(config.afkFarmingMobMacroCooldownSeconds));
        mobCooldownField.setChangedListener(value -> {
            Double parsed = parsePositiveDouble(value);
            if (parsed != null) {
                config.afkFarmingMobMacroCooldownSeconds = parsed.doubleValue();
                config.save();
            }
        });
        children.add(mobCooldownField);

        addButton(new ButtonWidget(10, 10, 78, 20, new LiteralText("Go Back"), button -> onClose()));
        refreshButtons();
        setInitialFocus(webhookField);
    }

    @Override
    public void tick() {
        webhookField.tick();
        reportIntervalField.tick();
        keyAdminNameField.tick();
        mobMinField.tick();
        mobMaxField.tick();
        mobCooldownField.tick();
        autoMoveIntervalField.tick();
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return webhookField.charTyped(chr, modifiers)
            || reportIntervalField.charTyped(chr, modifiers)
            || keyAdminNameField.charTyped(chr, modifiers)
            || mobMinField.charTyped(chr, modifiers)
            || mobMaxField.charTyped(chr, modifiers)
            || mobCooldownField.charTyped(chr, modifiers)
            || autoMoveIntervalField.charTyped(chr, modifiers)
            || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return webhookField.keyPressed(keyCode, scanCode, modifiers)
            || reportIntervalField.keyPressed(keyCode, scanCode, modifiers)
            || keyAdminNameField.keyPressed(keyCode, scanCode, modifiers)
            || mobMinField.keyPressed(keyCode, scanCode, modifiers)
            || mobMaxField.keyPressed(keyCode, scanCode, modifiers)
            || mobCooldownField.keyPressed(keyCode, scanCode, modifiers)
            || autoMoveIntervalField.keyPressed(keyCode, scanCode, modifiers)
            || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 16, 0xFFFFFF);

        int centerX = this.width / 2;
        int top = 34;
        int left = centerX - 105;
        int right = centerX + 5;
        this.textRenderer.draw(matrices, "Discord Webhook URL:", left, top + 116, 0xA0A0A0);
        this.textRenderer.draw(matrices, "\u5b9a\u671f\u901a\u77e5(\u5206):", left, top + 150, 0xA0A0A0);
        this.textRenderer.draw(matrices, "Admin\u540d:", right, top + 150, 0xA0A0A0);
        this.textRenderer.draw(matrices, "Auto Move\u9593\u9694(\u79d2):", left, top + 186, 0xA0A0A0);
        this.textRenderer.draw(matrices, "Living\u6570(min-max):", left, top + 222, 0xA0A0A0);
        this.textRenderer.draw(matrices, "\u518d\u767a\u52d5CD(\u79d2):", right, top + 222, 0xA0A0A0);

        webhookField.render(matrices, mouseX, mouseY, delta);
        reportIntervalField.render(matrices, mouseX, mouseY, delta);
        keyAdminNameField.render(matrices, mouseX, mouseY, delta);
        mobMinField.render(matrices, mouseX, mouseY, delta);
        mobMaxField.render(matrices, mouseX, mouseY, delta);
        mobCooldownField.render(matrices, mouseX, mouseY, delta);
        autoMoveIntervalField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        HaConfig.get().normalize();
        HaConfig.get().save();
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void refreshButtons() {
        HaConfig config = HaConfig.get();
        config.normalize();
        enabledButton.setMessage(new LiteralText("\u6a5f\u80fd: " + onOff(config.afkFarmingEnabled)));
        startButton.setMessage(new LiteralText(config.afkFarmingActive ? "\u76e3\u8996\u505c\u6b62" : "\u76e3\u8996\u958b\u59cb"));
        playerAlertButton.setMessage(new LiteralText("\u4ed6\u30d7\u30ec\u30a4\u30e4\u30fc: " + onOff(config.afkFarmingPlayerAlertsEnabled)));
        keyAdminAlertButton.setMessage(new LiteralText("Admin\u691c\u77e5: " + onOff(config.afkFarmingKeyAdminAlertsEnabled)));
        mobMacroButton.setMessage(new LiteralText("Living\u767a\u52d5: " + onOff(config.afkFarmingMobMacroEnabled)));
        selectedMacroButton.setMessage(new LiteralText("\u767a\u52d5Macro: " + macroName(config)));
        circleButton.setMessage(new LiteralText("\u5224\u5b9a\u30b5\u30fc\u30af\u30eb: " + onOff(config.afkFarmingMobCircleVisible)));
        debugHudButton.setMessage(new LiteralText("\u30c6\u30b9\u30c8HUD: " + onOff(config.afkFarmingMobDebugHudEnabled)));
        autoMoveButton.setMessage(new LiteralText("Auto Move: " + onOff(config.afkFarmingAutoMoveEnabled)));
        webhookTestButton.setMessage(new LiteralText("Webhook Test"));
    }

    private static String macroName(HaConfig config) {
        if (config.swapEntries.isEmpty()) {
            return "None";
        }
        int index = Math.max(0, Math.min(config.afkFarmingMobMacroIndex, config.swapEntries.size() - 1));
        String name = config.swapEntries.get(index).name;
        if (name.length() > 10) {
            return name.substring(0, 10);
        }
        return name;
    }

    private static String onOff(boolean value) {
        return value ? "§aEnabled" : "§cDisabled";
    }

    private static Double parsePositiveDouble(String value) {
        try {
            double parsed = Double.parseDouble(value.trim());
            return parsed > 0.0D ? Double.valueOf(parsed) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? Integer.valueOf(parsed) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
