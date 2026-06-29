package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaMacroListScreen extends Screen {
    private static final Text TITLE = new LiteralText("Config List");
    private static final int MACROS_PER_PAGE = 6;

    private final Screen parent;
    private int page;

    public HaMacroListScreen(Screen parent, int page) {
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

        int maxPage = Math.max(0, (config.swapEntries.size() - 1) / MACROS_PER_PAGE);
        page = Math.min(page, maxPage);

        int centerX = this.width / 2;
        int top = 32;
        addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText("Add macro"), button ->
            client.openScreen(new HaMacroEditScreen(this, -1, page))
        ));

        int startIndex = page * MACROS_PER_PAGE;
        int endIndex = Math.min(startIndex + MACROS_PER_PAGE, config.swapEntries.size());
        int y = top + 30;
        for (int i = startIndex; i < endIndex; i++) {
            final int macroIndex = i;
            HaConfig.SwapEntry entry = config.swapEntries.get(i);
            String label = entry.name + " / " + (entry.enabled ? "\u00a7aEnabled" : "\u00a7cDisabled");
            addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText(label), button ->
                client.openScreen(new HaMacroEditScreen(this, macroIndex, page))
            ));
            y += 24;
        }

        if (page > 0) {
            addButton(new ButtonWidget(centerX - 105, top + 174, 100, 20, new LiteralText("Back <"), button ->
                client.openScreen(new HaMacroListScreen(parent, page - 1))
            ));
        }

        if (endIndex < config.swapEntries.size()) {
            addButton(new ButtonWidget(centerX + 5, top + 174, 100, 20, new LiteralText("Next >"), button ->
                client.openScreen(new HaMacroListScreen(parent, page + 1))
            ));
        }

        addButton(new ButtonWidget(10, this.height - 30, 100, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Page " + (page + 1)), this.width / 2, 20, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }
}
