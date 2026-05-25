package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaDropNotifierManageScreen extends Screen {
    private static final Text TITLE = new LiteralText("Edit Notifiers");
    private static final int ITEMS_PER_PAGE = 6;

    private final Screen parent;
    private int page;

    public HaDropNotifierManageScreen(Screen parent, int page) {
        super(TITLE);
        this.parent = parent;
        this.page = Math.max(0, page);
    }

    public Screen getParentScreen() {
        return parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int maxPage = Math.max(0, (config.dropNotifierEntries.size() - 1) / ITEMS_PER_PAGE);
        page = Math.min(page, maxPage);

        int centerX = this.width / 2;
        int top = 36;
        addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText("Add New Item"), button -> {
            if (client != null) {
                client.openScreen(new HaDropNotifierEditScreen(this, -1, page));
            }
        }));

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, config.dropNotifierEntries.size());
        int y = top + 30;
        for (int i = startIndex; i < endIndex; i++) {
            final int entryIndex = i;
            HaConfig.DropNotifierEntry entry = config.dropNotifierEntries.get(i);
            String label = "Edit Item: " + trimForList(entry.matchText) + " / " + (entry.enabled ? "Enabled" : "Disabled");
            addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText(label), button -> {
                if (client != null) {
                    client.openScreen(new HaDropNotifierEditScreen(this, entryIndex, page));
                }
            }));
            y += 24;
        }

        if (page > 0) {
            addButton(new ButtonWidget(centerX - 105, top + 174, 100, 20, new LiteralText("< Back"), button -> {
                if (client != null) {
                    client.openScreen(new HaDropNotifierManageScreen(parent, page - 1));
                }
            }));
        }
        if (endIndex < config.dropNotifierEntries.size()) {
            addButton(new ButtonWidget(centerX + 5, top + 174, 100, 20, new LiteralText("Next >"), button -> {
                if (client != null) {
                    client.openScreen(new HaDropNotifierManageScreen(parent, page + 1));
                }
            }));
        }

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        if (HaConfig.get().dropNotifierEntries.isEmpty()) {
            drawCenteredText(matrices, this.textRenderer, new LiteralText("No drop notifiers yet."), this.width / 2, 82, 0xA0A0A0);
        }
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

    private static String trimForList(String value) {
        if (value == null || value.length() <= 18) {
            return value;
        }
        return value.substring(0, 18) + "...";
    }
}
