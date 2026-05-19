package com.example.ha;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.registry.Registry;

public final class HaBlockGalleryScreen extends Screen {
    private static final Text TITLE = new LiteralText("Block Gallery");
    private static final int SLOT_SIZE = 20;
    private static final int COLUMNS = 9;
    private static final int ROWS = 4;

    private final Screen parent;
    private int page;
    private TextFieldWidget searchField;

    public HaBlockGalleryScreen(Screen parent, int page) {
        super(TITLE);
        this.parent = parent;
        this.page = Math.max(0, page);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        searchField = new TextFieldWidget(this.textRenderer, centerX - 105, 32, 210, 20, new LiteralText("Search Blocks"));
        searchField.setMaxLength(64);
        children.add(searchField);
        setInitialFocus(searchField);

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 66, 20, new LiteralText("< Back"), button -> {
            page = Math.max(0, page - 1);
        }));
        addButton(new ButtonWidget(centerX - 33, this.height - 28, 66, 20, new LiteralText("Done"), button -> onClose()));
        addButton(new ButtonWidget(centerX + 39, this.height - 28, 66, 20, new LiteralText("Next >"), button -> {
            page++;
        }));
    }

    @Override
    public void tick() {
        searchField.tick();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        searchField.render(matrices, mouseX, mouseY, delta);

        int gridX = this.width / 2 - (COLUMNS * SLOT_SIZE) / 2;
        drawFavorites(matrices, mouseX, mouseY, gridX, 64);
        drawResults(matrices, mouseX, mouseY, gridX, 112);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchField.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        Block favorite = blockAt((int) mouseX, (int) mouseY, getFavoriteSlots(), this.width / 2 - (COLUMNS * SLOT_SIZE) / 2, 80);
        if (favorite != null) {
            clickBlock(favorite, button);
            return true;
        }

        Block result = blockAt((int) mouseX, (int) mouseY, getPageSlots(), this.width / 2 - (COLUMNS * SLOT_SIZE) / 2, 128);
        if (result != null) {
            clickBlock(result, button);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        page = 0;
        return searchField.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        page = 0;
        return searchField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private void drawFavorites(MatrixStack matrices, int mouseX, int mouseY, int x, int y) {
        this.textRenderer.draw(matrices, "Favorite Blocks (right-click to toggle)", x, y, 0xFFD166);
        List<Block> favorites = getFavoriteSlots();
        if (favorites.isEmpty()) {
            this.textRenderer.draw(matrices, "No favorites yet.", x, y + 18, 0xA0A0A0);
            return;
        }
        drawBlockSlots(matrices, favorites, mouseX, mouseY, x, y + 16);
    }

    private void drawResults(MatrixStack matrices, int mouseX, int mouseY, int x, int y) {
        List<Block> all = HaGhostWall.getSelectableBlocks(searchField.getText());
        int maxPage = Math.max(0, (all.size() - 1) / (COLUMNS * ROWS));
        page = Math.min(page, maxPage);
        this.textRenderer.draw(matrices, "All Blocks - Page " + (page + 1) + "/" + (maxPage + 1), x, y, 0xFFFFFF);
        drawBlockSlots(matrices, getPageSlots(), mouseX, mouseY, x, y + 16);
    }

    private void drawBlockSlots(MatrixStack matrices, List<Block> blocks, int mouseX, int mouseY, int x, int y) {
        ItemRenderer renderer = MinecraftClientHolder.CLIENT.getItemRenderer();
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            int slotX = x + (i % COLUMNS) * SLOT_SIZE;
            int slotY = y + (i / COLUMNS) * SLOT_SIZE;
            boolean selected = Registry.BLOCK.getId(block).toString().equals(HaGhostWall.getSelectedBlockId());
            int border = selected ? 0xFF55FF55 : HaGhostWall.isFavorite(block) ? 0xFFFFD166 : 0xFF555555;
            DrawableHelper.fill(matrices, slotX, slotY, slotX + 18, slotY + 18, 0xAA000000);
            DrawableHelper.fill(matrices, slotX, slotY, slotX + 18, slotY + 1, border);
            DrawableHelper.fill(matrices, slotX, slotY + 17, slotX + 18, slotY + 18, border);
            DrawableHelper.fill(matrices, slotX, slotY, slotX + 1, slotY + 18, border);
            DrawableHelper.fill(matrices, slotX + 17, slotY, slotX + 18, slotY + 18, border);
            renderer.renderInGuiWithOverrides(new ItemStack(block.asItem()), slotX + 1, slotY + 1);
            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                this.renderTooltip(matrices, new ItemStack(block.asItem()), mouseX, mouseY);
            }
        }
    }

    private List<Block> getFavoriteSlots() {
        List<Block> favorites = new ArrayList<Block>(HaGhostWall.getFavoriteBlocks());
        if (favorites.size() > COLUMNS) {
            return favorites.subList(0, COLUMNS);
        }
        return favorites;
    }

    private List<Block> getPageSlots() {
        List<Block> all = HaGhostWall.getSelectableBlocks(searchField == null ? "" : searchField.getText());
        int pageSize = COLUMNS * ROWS;
        int start = Math.min(page * pageSize, all.size());
        int end = Math.min(start + pageSize, all.size());
        return new ArrayList<Block>(all.subList(start, end));
    }

    private Block blockAt(int mouseX, int mouseY, List<Block> blocks, int x, int y) {
        for (int i = 0; i < blocks.size(); i++) {
            int slotX = x + (i % COLUMNS) * SLOT_SIZE;
            int slotY = y + (i / COLUMNS) * SLOT_SIZE;
            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                return blocks.get(i);
            }
        }
        return null;
    }

    private void clickBlock(Block block, int button) {
        if (button == 1) {
            HaGhostWall.toggleFavorite(block);
        } else if (button == 0) {
            HaGhostWall.selectBlock(block);
        }
    }
    private static final class MinecraftClientHolder {
        private static final net.minecraft.client.MinecraftClient CLIENT = net.minecraft.client.MinecraftClient.getInstance();
    }
}
