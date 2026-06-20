package com.example.ha;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.util.math.Vec3d;

final class HaWaypointTextRenderer {
    private static final double LABEL_VERTICAL_OFFSET = 0.05D;
    private static final float LABEL_SCALE = 0.025F;
    private static final double MAX_DISTANCE_SQUARED = 65536.0D;
    private static final MatrixStack LABEL_MATRICES = new MatrixStack();
    private static final BufferBuilder TEXT_BUFFER = new BufferBuilder(4096);
    private static final VertexConsumerProvider.Immediate TEXT_CONSUMERS = VertexConsumerProvider.immediate(TEXT_BUFFER);
    private static final Map<String, LabelRenderData> LABEL_RENDER_CACHE = new LinkedHashMap<String, LabelRenderData>(128, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, LabelRenderData> eldest) {
            return size() > 128;
        }
    };

    private HaWaypointTextRenderer() {
    }

    static void render(
        WorldRenderContext context,
        MinecraftClient client,
        List<HaWaypointManager.WaypointEntry> waypoints,
        Vec3d cameraPos
    ) {
        if (context == null || context.camera() == null || client == null || client.player == null || waypoints == null || waypoints.isEmpty()) {
            return;
        }

        Camera camera = context.camera();

        try {
            for (HaWaypointManager.WaypointEntry waypoint : waypoints) {
                String label = waypoint.label;
                if (!hasLabel(label)) {
                    continue;
                }
                if (client.player.squaredDistanceTo(waypoint.x + 0.5D, waypoint.y + 0.5D, waypoint.z + 0.5D) > MAX_DISTANCE_SQUARED) {
                    continue;
                }

                LabelRenderData labelRenderData = getLabelRenderData(client, label);
                if (labelRenderData != null) {
                    double anchorX = waypoint.x + 0.5D;
                    double anchorY = waypoint.y + 0.5D + LABEL_VERTICAL_OFFSET;
                    double anchorZ = waypoint.z + 0.5D;
                    renderLabel(
                        client,
                        LABEL_MATRICES,
                        camera,
                        anchorX - cameraPos.x,
                        anchorY - cameraPos.y,
                        anchorZ - cameraPos.z,
                        labelRenderData,
                        TEXT_CONSUMERS
                    );
                }
            }
        } finally {
            TEXT_CONSUMERS.draw();
        }
    }

    private static void renderLabel(
        MinecraftClient client,
        MatrixStack matrices,
        Camera camera,
        double x,
        double y,
        double z,
        LabelRenderData labelRenderData,
        VertexConsumerProvider textConsumers
    ) {
        matrices.push();
        try {
            matrices.translate(x, y, z);
            matrices.multiply(camera.getRotation());
            matrices.scale(-LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

            float width = labelRenderData.textWidth / 2.0F;
            client.textRenderer.draw(
                labelRenderData.orderedLabel,
                -width,
                0.0F,
                0xFFFFFFFF,
                false,
                matrices.peek().getModel(),
                textConsumers,
                true,
                0x00000000,
                0xF000F0
            );
        } finally {
            matrices.pop();
        }
    }

    static boolean hasLabel(String label) {
        if (label == null || label.isEmpty()) {
            return false;
        }
        for (int i = 0; i < label.length(); i++) {
            if (!Character.isWhitespace(label.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static LabelRenderData getLabelRenderData(MinecraftClient client, String label) {
        if (client == null || client.textRenderer == null || label == null || label.isEmpty()) {
            return null;
        }

        synchronized (LABEL_RENDER_CACHE) {
            LabelRenderData cached = LABEL_RENDER_CACHE.get(label);
            if (cached != null) {
                return cached;
            }

            OrderedText orderedLabel = new LiteralText(label).asOrderedText();
            LabelRenderData renderData = new LabelRenderData(orderedLabel, client.textRenderer.getWidth(label));
            LABEL_RENDER_CACHE.put(label, renderData);
            return renderData;
        }
    }

    private static final class LabelRenderData {
        final OrderedText orderedLabel;
        final int textWidth;

        LabelRenderData(OrderedText orderedLabel, int textWidth) {
            this.orderedLabel = orderedLabel;
            this.textWidth = textWidth;
        }
    }
}
