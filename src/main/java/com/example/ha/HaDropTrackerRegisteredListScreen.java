package com.example.ha;

import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaDropTrackerRegisteredListScreen extends Screen {
    private static final Text TITLE = new LiteralText("Registered Tracker Items");
    private static final int ITEMS_PER_PAGE = 6;

    private final Screen parent;
    private int page;

    public HaDropTrackerRegisteredListScreen(Screen parent, int page) {
        super(TITLE);
        this.parent = parent;
        this.page = Math.max(0, page);
    }

    public Screen getParentScreen() {
        return parent;
    }

    @Override
    protected void init() {
        List<HaDropTracker.RegisteredItem> items = HaDropTracker.getRegisteredItems();
        int maxPage = Math.max(0, (items.size() - 1) / ITEMS_PER_PAGE);
        page = Math.min(page, maxPage);

        int centerX = this.width / 2;
        int top = 40;
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());
        int y = top;
        for (int i = startIndex; i < endIndex; i++) {
            final int itemIndex = i;
            HaDropTracker.RegisteredItem entry = items.get(i);
            String label = trim(entry.displayName) + " / " + entry.unitPrice + " Intercoins";
            addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText(label), button -> {
                if (client != null) {
                    client.openScreen(new HaDropTrackerRegisteredEditScreen(this, itemIndex, page));
                }
            }));
            y += 24;
        }

        if (page > 0) {
            addButton(new ButtonWidget(centerX - 105, top + 174, 100, 20, new LiteralText("< Back"), button -> {
                if (client != null) {
                    client.openScreen(new HaDropTrackerRegisteredListScreen(parent, page - 1));
                }
            }));
        }
        if (endIndex < items.size()) {
            addButton(new ButtonWidget(centerX + 5, top + 174, 100, 20, new LiteralText("Next >"), button -> {
                if (client != null) {
                    client.openScreen(new HaDropTrackerRegisteredListScreen(parent, page + 1));
                }
            }));
        }

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Hold an item and use /ha tracker add [price] to register it."), this.width / 2, 26, 0xA0A0A0);
        if (HaDropTracker.getRegisteredItems().isEmpty()) {
            drawCenteredText(matrices, this.textRenderer, new LiteralText("No registered items yet."), this.width / 2, 52, 0xA0A0A0);
        }
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private static String trim(String value) {
        if (value == null || value.length() <= 18) {
            return value;
        }
        return value.substring(0, 18) + "...";
    }
}
