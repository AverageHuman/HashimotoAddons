package com.example.ha;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public final class HaChestSearchOverlay {
    private static final float OUTLINE_RED = 0.70F;
    private static final float OUTLINE_GREEN = 1.00F;
    private static final float OUTLINE_BLUE = 0.35F;
    private static final float OUTLINE_ALPHA = 0.95F;
    private static final float FILL_RED = 0.70F;
    private static final float FILL_GREEN = 1.00F;
    private static final float FILL_BLUE = 0.35F;
    private static final float FILL_ALPHA = 0.18F;

    private HaChestSearchOverlay() {
    }

    public static void render(WorldRenderContext context) {
        HaConfig config = HaConfig.get();
        if (!config.chestSearchEnabled || config.chestSearchQuery.trim().isEmpty()) {
            return;
        }
        if (context.consumers() == null || context.camera() == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        List<BlockPos> positions = HaChestSearchIndex.get().findMatchingPositions(client, config.chestSearchQuery);
        if (positions.isEmpty()) {
            return;
        }

        Vec3d cameraPos = context.camera().getPos();
        VertexConsumer lineBuffer = context.consumers().getBuffer(RenderLayer.getLines());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder fillBuffer = tessellator.getBuffer();
        fillBuffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);

        for (BlockPos pos : positions) {
            if (client.player != null && client.player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 16384.0D) {
                continue;
            }

            double x = pos.getX() - cameraPos.x;
            double y = pos.getY() - cameraPos.y;
            double z = pos.getZ() - cameraPos.z;

            drawFilledBox(fillBuffer, x + 0.03D, y + 0.03D, z + 0.03D, x + 0.97D, y + 0.97D, z + 0.97D);
        }
        tessellator.draw();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();

        for (BlockPos pos : positions) {
            if (client.player != null && client.player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 16384.0D) {
                continue;
            }

            double x = pos.getX() - cameraPos.x;
            double y = pos.getY() - cameraPos.y;
            double z = pos.getZ() - cameraPos.z;
            net.minecraft.client.render.WorldRenderer.drawBox(context.matrixStack(), lineBuffer, x, y, z, x + 1.0D, y + 1.0D, z + 1.0D, OUTLINE_RED, OUTLINE_GREEN, OUTLINE_BLUE, OUTLINE_ALPHA);
        }
    }

    private static void drawFilledBox(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        addFace(buffer, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ);
        addFace(buffer, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ);
        addFace(buffer, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ);
        addFace(buffer, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
        addFace(buffer, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        addFace(buffer, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ);
    }

    private static void addFace(
        BufferBuilder buffer,
        double x1, double y1, double z1,
        double x2, double y2, double z2,
        double x3, double y3, double z3,
        double x4, double y4, double z4
    ) {
        addVertex(buffer, x1, y1, z1);
        addVertex(buffer, x2, y2, z2);
        addVertex(buffer, x3, y3, z3);
        addVertex(buffer, x4, y4, z4);
    }

    private static void addVertex(BufferBuilder buffer, double x, double y, double z) {
        buffer.vertex(x, y, z).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).next();
    }
}
