package com.example.ha;

import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaDropTrackerRegisteredEditScreen extends Screen {
    private static final Text TITLE = new LiteralText("Edit Registered Item");

    private final Screen parent;
    private final int itemIndex;
    private final int returnPage;

    private TextFieldWidget displayNameField;
    private TextFieldWidget priceField;
    private String itemId = "";

    public HaDropTrackerRegisteredEditScreen(Screen parent, int itemIndex, int returnPage) {
        super(TITLE);
        this.parent = parent;
        this.itemIndex = itemIndex;
        this.returnPage = returnPage;
    }

    @Override
    protected void init() {
        List<HaDropTracker.RegisteredItem> items = HaDropTracker.getRegisteredItems();
        if (itemIndex < 0 || itemIndex >= items.size()) {
            if (client != null) {
                client.openScreen(parent);
            }
            return;
        }

        HaDropTracker.RegisteredItem entry = items.get(itemIndex);
        itemId = entry.itemId;

        int centerX = this.width / 2;
        int top = 40;

        displayNameField = new TextFieldWidget(this.textRenderer, centerX - 105, top + 18, 210, 20, new LiteralText("Display Name"));
        displayNameField.setText(entry.displayName);
        displayNameField.setMaxLength(64);
        children.add(displayNameField);

        priceField = new TextFieldWidget(this.textRenderer, centerX - 105, top + 74, 210, 20, new LiteralText("Intercoins"));
        priceField.setText(Long.toString(entry.unitPrice));
        priceField.setMaxLength(20);
        children.add(priceField);

        addButton(new ButtonWidget(centerX - 105, this.height - 50, 210, 20, new LiteralText("Save"), button -> saveItem()));
        addButton(new ButtonWidget(centerX - 105, this.height - 76, 210, 20, new LiteralText("\u00a74Delete"), button -> deleteItem()));
        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
        setInitialFocus(displayNameField);
    }

    @Override
    public void tick() {
        displayNameField.tick();
        priceField.tick();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        int centerX = this.width / 2;
        int top = 40;
        this.textRenderer.draw(matrices, "Display Name:", centerX - 105, top + 8, 0xFFFFFF);
        this.textRenderer.draw(matrices, "Price:", centerX - 105, top + 64, 0xFFFFFF);
        this.textRenderer.draw(matrices, "Item ID: " + itemId, centerX - 105, top + 112, 0xA0A0A0);
        this.textRenderer.draw(matrices, "Intercoins per item", centerX - 105, top + 98, 0xA0A0A0);
        displayNameField.render(matrices, mouseX, mouseY, delta);
        priceField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return displayNameField.charTyped(chr, modifiers)
            || priceField.charTyped(chr, modifiers)
            || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return displayNameField.keyPressed(keyCode, scanCode, modifiers)
            || priceField.keyPressed(keyCode, scanCode, modifiers)
            || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void saveItem() {
        long unitPrice = parsePrice(priceField.getText());
        HaDropTracker.updateRegisteredItem(itemIndex, displayNameField.getText(), unitPrice);
        openListScreen(false);
    }

    private void deleteItem() {
        HaDropTracker.removeRegisteredItem(itemIndex);
        openListScreen(true);
    }

    private void openListScreen(boolean afterDelete) {
        if (client != null) {
            int maxPage = Math.max(0, (HaDropTracker.getRegisteredItems().size() - 1) / 6);
            Screen listParent = parent instanceof HaDropTrackerRegisteredListScreen ? ((HaDropTrackerRegisteredListScreen) parent).getParentScreen() : parent;
            client.openScreen(new HaDropTrackerRegisteredListScreen(listParent, afterDelete ? Math.min(returnPage, maxPage) : returnPage));
        }
    }

    private static long parsePrice(String value) {
        try {
            return Math.max(0L, Long.parseLong(value.trim()));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
