package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaMacroEditScreen extends Screen {
    private static final Text TITLE = new LiteralText("Add Macro");

    private final Screen parent;
    private final int macroIndex;
    private final int returnPage;

    private TextFieldWidget nameField;
    private TextFieldWidget waitingTimeField;
    private TextFieldWidget holdTicksField;
    private ButtonWidget hotbarButton;
    private int selectedHotbarSlot = 0;
    private int holdTicks = 4;

    public HaMacroEditScreen(Screen parent, int macroIndex, int returnPage) {
        super(TITLE);
        this.parent = parent;
        this.macroIndex = macroIndex;
        this.returnPage = returnPage;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        String initialName = "";
        int initialHotbarSlot = 0;
        double initialInterval = 5.0D;
        int initialHoldTicks = 4;
        if (macroIndex >= 0 && macroIndex < config.swapEntries.size()) {
            HaConfig.SwapEntry entry = config.swapEntries.get(macroIndex);
            initialName = entry.name;
            initialHotbarSlot = entry.hotbarSlot;
            initialInterval = entry.intervalSeconds;
            initialHoldTicks = entry.holdTicks;
        }

        selectedHotbarSlot = initialHotbarSlot;
        holdTicks = initialHoldTicks;
        int centerX = this.width / 2;
        int top = 40;

        nameField = new TextFieldWidget(this.textRenderer, centerX - 105, top + 18, 210, 20, new LiteralText("Name"));
        nameField.setText(initialName);
        children.add(nameField);

        hotbarButton = addButton(new ButtonWidget(centerX - 105, top + 62, 210, 20, hotbarButtonText(), button -> {
            selectedHotbarSlot = selectedHotbarSlot >= 8 ? 0 : selectedHotbarSlot + 1;
            button.setMessage(hotbarButtonText());
        }));

        waitingTimeField = new TextFieldWidget(this.textRenderer, centerX - 105, top + 106, 210, 20, new LiteralText("Waiting time"));
        waitingTimeField.setText(Double.toString(initialInterval));
        children.add(waitingTimeField);

        holdTicksField = new TextFieldWidget(this.textRenderer, centerX - 105, top + 150, 210, 20, new LiteralText("Hold ticks"));
        holdTicksField.setText(Integer.toString(initialHoldTicks));
        children.add(holdTicksField);

        addButton(new ButtonWidget(centerX - 105, this.height - 50, 210, 20, new LiteralText("Add"), button -> saveMacro()));
        if (macroIndex >= 0 && macroIndex < config.swapEntries.size()) {
            addButton(new ButtonWidget(centerX - 105, this.height - 76, 210, 20, new LiteralText("\u00a74Delete"), button -> deleteMacro()));
        }
        addButton(new ButtonWidget(10, this.height - 30, 100, 20, new LiteralText("Go Back"), button -> onClose()));
        setInitialFocus(nameField);
    }

    @Override
    public void tick() {
        nameField.tick();
        waitingTimeField.tick();
        holdTicksField.tick();
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return nameField.charTyped(chr, modifiers)
            || waitingTimeField.charTyped(chr, modifiers)
            || holdTicksField.charTyped(chr, modifiers)
            || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return nameField.keyPressed(keyCode, scanCode, modifiers)
            || waitingTimeField.keyPressed(keyCode, scanCode, modifiers)
            || holdTicksField.keyPressed(keyCode, scanCode, modifiers)
            || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);

        int centerX = this.width / 2;
        int top = 40;
        this.textRenderer.draw(matrices, "Name:", centerX - 105, top + 8, 0xA0A0A0);
        this.textRenderer.draw(matrices, "Button", centerX - 105, top + 52, 0xA0A0A0);
        this.textRenderer.draw(matrices, "Waiting time:", centerX - 105, top + 96, 0xA0A0A0);
        this.textRenderer.draw(matrices, "Hold ticks:", centerX - 105, top + 140, 0xA0A0A0);

        nameField.render(matrices, mouseX, mouseY, delta);
        waitingTimeField.render(matrices, mouseX, mouseY, delta);
        holdTicksField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void saveMacro() {
        Double interval = parsePositiveDouble(waitingTimeField.getText());
        if (interval == null) {
            interval = Double.valueOf(5.0D);
        }

        Integer parsedHoldTicks = parseNonNegativeInt(holdTicksField.getText());
        if (parsedHoldTicks == null) {
            parsedHoldTicks = Integer.valueOf(4);
        }

        String name = nameField.getText();
        if (name == null || name.trim().isEmpty()) {
            name = "New Macro";
        }

        HaConfig config = HaConfig.get();
        config.normalize();
        if (macroIndex >= 0 && macroIndex < config.swapEntries.size()) {
            config.swapEntries.get(macroIndex).copyFrom(name, selectedHotbarSlot, interval.doubleValue(), parsedHoldTicks.intValue());
        } else {
            HaConfig.SwapEntry entry = config.addSwapEntry();
            entry.copyFrom(name, selectedHotbarSlot, interval.doubleValue(), parsedHoldTicks.intValue());
        }
        config.save();

        if (client != null) {
            Screen listParent = parent instanceof HaMacroListScreen ? ((HaMacroListScreen) parent).getParentScreen() : parent;
            int nextPage = (config.swapEntries.size() - 1) / 6;
            client.openScreen(new HaMacroListScreen(listParent, macroIndex >= 0 ? returnPage : nextPage));
        }
    }

    private void deleteMacro() {
        HaConfig config = HaConfig.get();
        config.removeSwapEntry(macroIndex);
        config.save();

        if (client != null) {
            Screen listParent = parent instanceof HaMacroListScreen ? ((HaMacroListScreen) parent).getParentScreen() : parent;
            int maxPage = Math.max(0, (config.swapEntries.size() - 1) / 6);
            client.openScreen(new HaMacroListScreen(listParent, Math.min(returnPage, maxPage)));
        }
    }

    private Text hotbarButtonText() {
        return new LiteralText(Integer.toString(selectedHotbarSlot + 1));
    }

    private static Double parsePositiveDouble(String value) {
        try {
            double parsed = Double.parseDouble(value.trim());
            return parsed > 0.0D ? Double.valueOf(parsed) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer parseNonNegativeInt(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= 0 ? Integer.valueOf(parsed) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
