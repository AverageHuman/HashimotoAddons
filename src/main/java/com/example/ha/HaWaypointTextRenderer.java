package com.example.ha;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

final class HaWaypointTextRenderer {
    private static final double LABEL_VERTICAL_OFFSET = 0.60D;
    private static final float LABEL_SCALE = 0.025F;
    private static final double MAX_DISTANCE_SQUARED = 65536.0D;

    private HaWaypointTextRenderer() {
    }

    static void render(
        WorldRenderContext context,
        MinecraftClient client,
        List<HaWaypointManager.WaypointEntry> waypoints,
        Vec3d cameraPos,
        boolean throughWalls,
        boolean fullBlock
    ) {
        if (context == null || context.camera() == null || client == null || client.player == null || waypoints == null || waypoints.isEmpty()) {
            return;
        }

        MatrixStack matrices = new MatrixStack();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Camera camera = context.camera();
        VertexConsumerProvider.Immediate textConsumers = VertexConsumerProvider.immediate(new BufferBuilder(256));
        List<TextRenderTask> tasks = new ArrayList<TextRenderTask>();

        for (HaWaypointManager.WaypointEntry waypoint : waypoints) {
            String label = waypoint.label == null ? "" : waypoint.label.trim();
            if (label.isEmpty()) {
                continue;
            }
            if (client.player.squaredDistanceTo(waypoint.x + 0.5D, waypoint.y + 0.5D, waypoint.z + 0.5D) > MAX_DISTANCE_SQUARED) {
                continue;
            }

            Vec3d renderPos = new Vec3d(waypoint.x + 0.5D, waypoint.y + LABEL_VERTICAL_OFFSET, waypoint.z + 0.5D);
            tasks.add(new TextRenderTask(label, renderPos, client.textRenderer.getWidth(label)));
        }

        for (TextRenderTask task : tasks) {
            renderLabel(client, matrices, camera, task, textConsumers, throughWalls);
        }

        textConsumers.draw();
    }

    private static void renderLabel(
        MinecraftClient client,
        MatrixStack matrices,
        Camera camera,
        TextRenderTask task,
        VertexConsumerProvider.Immediate textConsumers,
        boolean throughWalls
    ) {
        matrices.push();
        matrices.translate(task.renderPos.x, task.renderPos.y, task.renderPos.z);
        matrices.multiply(camera.getRotation());
        matrices.scale(-LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

        float width = task.textWidth / 2.0F;
        Matrix4f matrix = matrices.peek().getModel();
        OrderedText orderedLabel = new LiteralText(task.label).asOrderedText();
        client.textRenderer.draw(orderedLabel, -width, 0.0F, 0xFFFFFFFF, false, matrix, textConsumers, throughWalls, 0, 0xF000F0);
        matrices.pop();
    }

    private static final class TextRenderTask {
        final String label;
        final Vec3d renderPos;
        final int textWidth;

        TextRenderTask(String label, Vec3d renderPos, int textWidth) {
            this.label = label;
            this.renderPos = renderPos;
            this.textWidth = textWidth;
        }
    }
}
