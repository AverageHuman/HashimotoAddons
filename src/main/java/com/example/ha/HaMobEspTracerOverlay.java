package com.example.ha;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public final class HaMobEspTracerOverlay {
    private static final int TRACER_COLOR = 0xE600FFFF;
    private static final double MIN_EDGE_PADDING = 8.0D;
    private static final double LINE_WIDTH = 3.5D;
    private static final double YAW_SCREEN_RANGE = 95.0D;
    private static final double PITCH_SCREEN_RANGE = 70.0D;

    private HaMobEspTracerOverlay() {
    }

    public static void render(MatrixStack matrices, float tickDelta) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null || HaHudVisibility.shouldHideHashimotoHud(client)) {
            return;
        }

        List<HaMobEsp.RenderTarget> targets = HaMobEspOverlay.collectTargets(client);
        if (targets.isEmpty()) {
            return;
        }

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        double startX = width * 0.5D;
        double startY = height - 18.0D;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        boolean fillStarted = false;
        try {
            buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
            fillStarted = true;

            for (HaMobEsp.RenderTarget target : targets) {
                ScreenPoint end = projectToScreen(client, target, tickDelta, width, height);
                drawThickLine(buffer, startX, startY, end.x, end.y, LINE_WIDTH, TRACER_COLOR);
            }
        } finally {
            if (fillStarted) {
                try {
                    tessellator.draw();
                } finally {
                    restoreRenderState();
                }
            } else {
                restoreRenderState();
            }
        }
    }

    private static ScreenPoint projectToScreen(MinecraftClient client, HaMobEsp.RenderTarget target, float tickDelta, int width, int height) {
        Vec3d camera = client.player.getCameraPosVec(tickDelta);
        Box box = HaMobEspOverlay.getRenderBox(target);
        double targetX = (box.minX + box.maxX) * 0.5D;
        double targetY = (box.minY + box.maxY) * 0.5D;
        double targetZ = (box.minZ + box.maxZ) * 0.5D;

        double dx = targetX - camera.x;
        double dy = targetY - camera.y;
        double dz = targetZ - camera.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0D;
        double targetPitch = -Math.toDegrees(Math.atan2(dy, horizontalDistance));
        double yawDelta = wrapDegrees(targetYaw - client.player.getYaw(tickDelta));
        double pitchDelta = targetPitch - client.player.getPitch(tickDelta);

        double x = width * 0.5D + (yawDelta / YAW_SCREEN_RANGE) * (width * 0.5D);
        double y = height * 0.5D + (pitchDelta / PITCH_SCREEN_RANGE) * (height * 0.5D);

        x = clamp(x, MIN_EDGE_PADDING, width - MIN_EDGE_PADDING);
        y = clamp(y, MIN_EDGE_PADDING, height - MIN_EDGE_PADDING);
        return new ScreenPoint(x, y);
    }

    private static void drawThickLine(BufferBuilder buffer, double x1, double y1, double x2, double y2, double width, int color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001D) {
            return;
        }

        double nx = -dy / length * width * 0.5D;
        double ny = dx / length * width * 0.5D;

        addVertex(buffer, x1 + nx, y1 + ny, color);
        addVertex(buffer, x2 + nx, y2 + ny, color);
        addVertex(buffer, x2 - nx, y2 - ny, color);
        addVertex(buffer, x1 - nx, y1 - ny, color);
    }

    private static void addVertex(BufferBuilder buffer, double x, double y, int color) {
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        buffer.vertex(x, y, 0.0D).color(red, green, blue, alpha).next();
    }

    private static double wrapDegrees(double degrees) {
        degrees %= 360.0D;
        if (degrees >= 180.0D) {
            degrees -= 360.0D;
        }
        if (degrees < -180.0D) {
            degrees += 360.0D;
        }
        return degrees;
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static final class ScreenPoint {
        final double x;
        final double y;

        ScreenPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static void restoreRenderState() {
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
}
