package com.example.ha;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public final class HaWaypointOverlay {
    private static final float OUTLINE_ALPHA = 0.80F;
    private static final float FILL_ALPHA = 0.80F;
    private static final float FULL_BLOCK_DARKEN_MULTIPLIER = 0.72F;
    private static final double OUTLINE_PIXEL_THICKNESS = 2.0D;
    private static final double OUTLINE_FOV_DEGREES = 70.0D;
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
        boolean fullBlock = HaWaypointManager.isRenderFullBlocks();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);

        boolean renderedAny = false;
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
                drawFilledBox(buffer, x, y, z, x + 1.0D, y + 1.0D, z + 1.0D, red * FULL_BLOCK_DARKEN_MULTIPLIER, green * FULL_BLOCK_DARKEN_MULTIPLIER, blue * FULL_BLOCK_DARKEN_MULTIPLIER, FILL_ALPHA);
            }

            double outlineThickness = getOutlineThickness(client, cameraPos, waypoint);
            drawOutlineBox(buffer, x, y, z, x + 1.0D, y + 1.0D, z + 1.0D, outlineThickness, red, green, blue, OUTLINE_ALPHA);
            renderedAny = true;
        }

        if (!renderedAny) {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.enableTexture();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            return;
        }

        tessellator.draw();

        RenderSystem.enableTexture();
        HaWaypointTextRenderer.render(context, client, waypoints, cameraPos);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawOutlineBox(
        BufferBuilder buffer,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        double thickness,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        double halfThickness = thickness * 0.5D;

        drawFilledBox(buffer, minX, minY - halfThickness, minZ - halfThickness, maxX, minY + halfThickness, minZ + halfThickness, red, green, blue, alpha);
        drawFilledBox(buffer, minX, maxY - halfThickness, minZ - halfThickness, maxX, maxY + halfThickness, minZ + halfThickness, red, green, blue, alpha);
        drawFilledBox(buffer, minX, minY - halfThickness, maxZ - halfThickness, maxX, minY + halfThickness, maxZ + halfThickness, red, green, blue, alpha);
        drawFilledBox(buffer, minX, maxY - halfThickness, maxZ - halfThickness, maxX, maxY + halfThickness, maxZ + halfThickness, red, green, blue, alpha);

        drawFilledBox(buffer, minX - halfThickness, minY, minZ - halfThickness, minX + halfThickness, maxY, minZ + halfThickness, red, green, blue, alpha);
        drawFilledBox(buffer, maxX - halfThickness, minY, minZ - halfThickness, maxX + halfThickness, maxY, minZ + halfThickness, red, green, blue, alpha);
        drawFilledBox(buffer, minX - halfThickness, minY, maxZ - halfThickness, minX + halfThickness, maxY, maxZ + halfThickness, red, green, blue, alpha);
        drawFilledBox(buffer, maxX - halfThickness, minY, maxZ - halfThickness, maxX + halfThickness, maxY, maxZ + halfThickness, red, green, blue, alpha);

        drawFilledBox(buffer, minX - halfThickness, minY - halfThickness, minZ, minX + halfThickness, minY + halfThickness, maxZ, red, green, blue, alpha);
        drawFilledBox(buffer, maxX - halfThickness, minY - halfThickness, minZ, maxX + halfThickness, minY + halfThickness, maxZ, red, green, blue, alpha);
        drawFilledBox(buffer, minX - halfThickness, maxY - halfThickness, minZ, minX + halfThickness, maxY + halfThickness, maxZ, red, green, blue, alpha);
        drawFilledBox(buffer, maxX - halfThickness, maxY - halfThickness, minZ, maxX + halfThickness, maxY + halfThickness, maxZ, red, green, blue, alpha);
    }

    private static void drawFilledBox(
        BufferBuilder buffer,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        addFace(buffer, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        addFace(buffer, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ, red, green, blue, alpha);
        addFace(buffer, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        addFace(buffer, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, alpha);
        addFace(buffer, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        addFace(buffer, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, red, green, blue, alpha);
    }

    private static void addFace(
        BufferBuilder buffer,
        double x1, double y1, double z1,
        double x2, double y2, double z2,
        double x3, double y3, double z3,
        double x4, double y4, double z4,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        addVertex(buffer, x1, y1, z1, red, green, blue, alpha);
        addVertex(buffer, x2, y2, z2, red, green, blue, alpha);
        addVertex(buffer, x3, y3, z3, red, green, blue, alpha);
        addVertex(buffer, x4, y4, z4, red, green, blue, alpha);
    }

    private static void addVertex(BufferBuilder buffer, double x, double y, double z, float red, float green, float blue, float alpha) {
        buffer.vertex(x, y, z).color(red, green, blue, alpha).next();
    }

    private static double getOutlineThickness(MinecraftClient client, Vec3d cameraPos, HaWaypointManager.WaypointEntry waypoint) {
        double screenHeight = Math.max(1.0D, client.getWindow().getScaledHeight());
        double distance = cameraPos.distanceTo(new Vec3d(waypoint.x + 0.5D, waypoint.y + 0.5D, waypoint.z + 0.5D));
        double worldUnitsPerPixel = 2.0D * distance * Math.tan(Math.toRadians(OUTLINE_FOV_DEGREES) * 0.5D) / screenHeight;
        return Math.max(0.01D, worldUnitsPerPixel * OUTLINE_PIXEL_THICKNESS);
    }

    private static int getColorRgb(int slot) {
        Formatting formatting = HaWaypointManager.getColorSlotFormatting(slot);
        Integer value = formatting == null ? null : formatting.getColorValue();
        return value == null ? 0xFFFFFF : value.intValue();
    }
}
