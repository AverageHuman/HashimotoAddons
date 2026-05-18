package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaHpAlertListScreen extends Screen {
    private static final Text TITLE = new LiteralText("HP Alert");
    private static final int ALERTS_PER_PAGE = 6;

    private final Screen parent;
    private int page;

    public HaHpAlertListScreen(Screen parent, int page) {
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

        int maxPage = Math.max(0, (config.hpAlertEntries.size() - 1) / ALERTS_PER_PAGE);
        page = Math.min(page, maxPage);

        int centerX = this.width / 2;
        int top = 32;
        addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText("Add HP Alert"), button -> {
            if (client != null) {
                client.openScreen(new HaHpAlertEditScreen(this, -1, page));
            }
        }));

        int startIndex = page * ALERTS_PER_PAGE;
        int endIndex = Math.min(startIndex + ALERTS_PER_PAGE, config.hpAlertEntries.size());
        int y = top + 30;
        for (int i = startIndex; i < endIndex; i++) {
            final int alertIndex = i;
            HaConfig.HpAlertEntry entry = config.hpAlertEntries.get(i);
            String label = trimForList(entry.titleText) + " / " + entry.healthPercentage + "% / " + HaConfig.TITLE_COLOR_NAMES[entry.colorIndex] + " / " + (entry.enabled ? "ON" : "OFF");
            addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText(label), button -> {
                if (client != null) {
                    client.openScreen(new HaHpAlertEditScreen(this, alertIndex, page));
                }
            }));
            y += 24;
        }

        if (page > 0) {
            addButton(new ButtonWidget(centerX - 105, top + 174, 100, 20, new LiteralText("< Back"), button -> {
                if (client != null) {
                    client.openScreen(new HaHpAlertListScreen(parent, page - 1));
                }
            }));
        }
        if (endIndex < config.hpAlertEntries.size()) {
            addButton(new ButtonWidget(centerX + 5, top + 174, 100, 20, new LiteralText("Next >"), button -> {
                if (client != null) {
                    client.openScreen(new HaHpAlertListScreen(parent, page + 1));
                }
            }));
        }

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private static String trimForList(String value) {
        if (value == null || value.length() <= 10) {
            return value;
        }
        return value.substring(0, 10) + "...";
    }
}
