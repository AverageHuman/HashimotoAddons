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

        MatrixStack matrices = new MatrixStack();
        Camera camera = context.camera();
        BufferBuilder textBuffer = new BufferBuilder(256);
        VertexConsumerProvider.Immediate textConsumers = VertexConsumerProvider.immediate(textBuffer);

        for (HaWaypointManager.WaypointEntry waypoint : waypoints) {
            String label = waypoint.label == null ? "" : waypoint.label.trim();
            if (label.isEmpty()) {
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
                    matrices,
                    camera,
                    anchorX - cameraPos.x,
                    anchorY - cameraPos.y,
                    anchorZ - cameraPos.z,
                    labelRenderData,
                    textConsumers
                );
            }
        }

        textConsumers.draw();
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
        matrices.pop();
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
