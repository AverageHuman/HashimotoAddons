package com.example.ha;

import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaWaypointListScreen extends Screen {
    private static final Text TITLE = new LiteralText("Waypoint List");
    private static final int ITEMS_PER_PAGE = 6;

    private final Screen parent;
    private int page;

    public HaWaypointListScreen(Screen parent, int page) {
        super(TITLE);
        this.parent = parent;
        this.page = Math.max(0, page);
    }

    public Screen getParentScreen() {
        return parent;
    }

    public int getPage() {
        return page;
    }

    @Override
    protected void init() {
        String dimensionKey = HaWaypointManager.getCurrentDimensionKey(client);
        List<HaWaypointManager.WaypointEntry> waypoints = dimensionKey == null
            ? java.util.Collections.<HaWaypointManager.WaypointEntry>emptyList()
            : HaWaypointManager.getWaypointsForDimension(dimensionKey);
        int maxPage = Math.max(0, (waypoints.size() - 1) / ITEMS_PER_PAGE);
        page = Math.min(page, maxPage);

        int centerX = this.width / 2;
        int top = 38;
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, waypoints.size());
        int y = top + 20;
        for (int i = startIndex; i < endIndex; i++) {
            final int waypointIndex = i;
            HaWaypointManager.WaypointEntry entry = waypoints.get(i);
            String label = trim(HaWaypointManager.getDisplayLabel(entry));
            String colorName = HaWaypointManager.getColorSlotName(entry.colorSlotIndex);
            String renderModeName = entry.renderFullBlocks ? "Full Block" : "Outline Only";
            String entryLabel = label + " / " + HaWaypointManager.formatPosition(entry.x, entry.y, entry.z) + " / " + colorName + " / " + renderModeName;

            addButton(new ButtonWidget(centerX - 105, y, 168, 20, new LiteralText(entryLabel).formatted(HaWaypointManager.getColorSlotFormatting(entry.colorSlotIndex)), button -> {
                if (client != null) {
                    client.openScreen(new HaWaypointLabelScreen(this, entry.dimensionKey, entry.toBlockPos(), entry.label, entry.colorSlotIndex, entry.renderFullBlocks, true));
                }
            }));
            addButton(new ButtonWidget(centerX + 67, y, 38, 20, new LiteralText("Del"), button -> {
                HaWaypointManager.removeWaypoint(entry.dimensionKey, entry.toBlockPos());
                if (client != null) {
                    client.openScreen(new HaWaypointListScreen(parent, Math.min(page, Math.max(0, (HaWaypointManager.getWaypointCountForDimension(entry.dimensionKey) - 1) / ITEMS_PER_PAGE))));
                }
            }));
            y += 24;
        }

        if (page > 0) {
            addButton(new ButtonWidget(centerX - 105, this.height - 54, 100, 20, new LiteralText("< Back"), button -> {
                if (client != null) {
                    client.openScreen(new HaWaypointListScreen(parent, page - 1));
                }
            }));
        }
        if (endIndex < waypoints.size()) {
            addButton(new ButtonWidget(centerX + 5, this.height - 54, 100, 20, new LiteralText("Next >"), button -> {
                if (client != null) {
                    client.openScreen(new HaWaypointListScreen(parent, page + 1));
                }
            }));
        }

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Dimension: " + safeDimensionKey()), this.width / 2, 26, 0xA0E8FF);
        if (HaWaypointManager.getWaypointCountForCurrentDimension(client) == 0) {
            drawCenteredText(matrices, this.textRenderer, new LiteralText("No waypoints in this dimension yet."), this.width / 2, 82, 0xA0A0A0);
        }
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private String safeDimensionKey() {
        String dimensionKey = HaWaypointManager.getCurrentDimensionKey(client);
        return dimensionKey == null ? "unknown" : dimensionKey;
    }

    private static String trim(String value) {
        if (value == null || value.length() <= 18) {
            return value;
        }
        return value.substring(0, 18) + "...";
    }
}
