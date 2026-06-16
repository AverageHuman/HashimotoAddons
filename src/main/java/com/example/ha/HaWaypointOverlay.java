package com.example.ha;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public final class HaWaypointOverlay {
    private static final float OUTLINE_ALPHA = 0.80F;
    private static final float FILL_ALPHA = 0.80F;
    private static final float FULL_BLOCK_DARKEN_MULTIPLIER = 0.72F;
    private static final double MAX_DISTANCE_SQUARED = 65536.0D;

    private HaWaypointOverlay() {
    }

    public static void render(WorldRenderContext context) {
        if (context == null || context.camera() == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return;
        }

        List<HaWaypointManager.WaypointEntry> waypoints = HaWaypointManager.getWaypointsForCurrentDimension(client);
        if (waypoints.isEmpty()) {
            return;
        }

        Vec3d cameraPos = context.camera().getPos();
        VertexConsumer lineBuffer = context.consumers().getBuffer(RenderLayer.getLines());
        boolean fullBlock = HaWaypointManager.isRenderFullBlocks();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder fillBuffer = tessellator.getBuffer();
        if (fullBlock) {
            fillBuffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
        }

        MatrixStack matrices = context.matrixStack();
        RenderSystem.lineWidth(2.0F);
        try {
            for (HaWaypointManager.WaypointEntry waypoint : waypoints) {
                if (client.player.squaredDistanceTo(waypoint.x + 0.5D, waypoint.y + 0.5D, waypoint.z + 0.5D) > MAX_DISTANCE_SQUARED) {
                    continue;
                }

                double x = waypoint.x - cameraPos.x;
                double y = waypoint.y - cameraPos.y;
                double z = waypoint.z - cameraPos.z;
                int rgb = getColorRgb(waypoint.colorSlotIndex);
                float red = ((rgb >> 16) & 0xFF) / 255.0F;
                float green = ((rgb >> 8) & 0xFF) / 255.0F;
                float blue = (rgb & 0xFF) / 255.0F;

                if (fullBlock) {
                    drawFilledBox(fillBuffer, x, y, z, x + 1.0D, y + 1.0D, z + 1.0D, red * FULL_BLOCK_DARKEN_MULTIPLIER, green * FULL_BLOCK_DARKEN_MULTIPLIER, blue * FULL_BLOCK_DARKEN_MULTIPLIER);
                }

                net.minecraft.client.render.WorldRenderer.drawBox(matrices, lineBuffer, x, y, z, x + 1.0D, y + 1.0D, z + 1.0D, red, green, blue, OUTLINE_ALPHA);
            }
        } finally {
            RenderSystem.lineWidth(1.0F);
        }

        if (fullBlock) {
            tessellator.draw();
        }

        RenderSystem.enableTexture();
        HaWaypointTextRenderer.render(context, client, waypoints, cameraPos);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawFilledBox(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float red, float green, float blue) {
        addFace(buffer, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, red, green, blue);
        addFace(buffer, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ, red, green, blue);
        addFace(buffer, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, red, green, blue);
        addFace(buffer, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue);
        addFace(buffer, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue);
        addFace(buffer, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, red, green, blue);
    }

    private static void addFace(
        BufferBuilder buffer,
        double x1, double y1, double z1,
        double x2, double y2, double z2,
        double x3, double y3, double z3,
        double x4, double y4, double z4,
        float red,
        float green,
        float blue
    ) {
        addVertex(buffer, x1, y1, z1, red, green, blue);
        addVertex(buffer, x2, y2, z2, red, green, blue);
        addVertex(buffer, x3, y3, z3, red, green, blue);
        addVertex(buffer, x4, y4, z4, red, green, blue);
    }

    private static void addVertex(BufferBuilder buffer, double x, double y, double z, float red, float green, float blue) {
        buffer.vertex(x, y, z).color(red, green, blue, FILL_ALPHA).next();
    }

    private static int getColorRgb(int slot) {
        Formatting formatting = HaWaypointManager.getColorSlotFormatting(slot);
        Integer value = formatting == null ? null : formatting.getColorValue();
        return value == null ? 0xFFFFFF : value.intValue();
    }
}
