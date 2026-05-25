package com.example.ha;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public final class HaAfkFarmingCircleOverlay {
    private static final int SEGMENTS = 72;

    private HaAfkFarmingCircleOverlay() {
    }

    public static void render(WorldRenderContext context) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED || context.camera() == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        HaConfig config = HaConfig.get();
        if (client == null
            || client.player == null
            || client.world == null
            || !config.afkFarmingEnabled
            || !config.afkFarmingMobMacroEnabled
            || !config.afkFarmingMobCircleVisible) {
            return;
        }

        HaAfkFarming.MobDebugInfo info = HaAfkFarming.getMobDebugInfo(client);
        if (!info.available || info.center == null) {
            return;
        }

        Vec3d cameraPos = context.camera().getPos();
        double centerX = info.center.x - cameraPos.x;
        double centerY = info.center.y + 0.04D - cameraPos.y;
        double centerZ = info.center.z - cameraPos.z;
        double radius = info.radius;
        boolean ready = info.mobCount >= info.threshold && info.threshold > 0 && info.cooldownMillis <= 0L;
        float red = ready ? 0.25F : 1.00F;
        float green = ready ? 1.00F : 0.85F;
        float blue = ready ? 0.25F : 0.10F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = (Math.PI * 2.0D * i) / SEGMENTS;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            buffer.vertex(x, centerY, z).color(red, green, blue, 0.95F).next();
        }
        tessellator.draw();

        buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
        addLine(buffer, centerX - 0.18D, centerY, centerZ, centerX + 0.18D, centerY, centerZ, red, green, blue);
        addLine(buffer, centerX, centerY, centerZ - 0.18D, centerX, centerY, centerZ + 0.18D, red, green, blue);
        tessellator.draw();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private static void addLine(BufferBuilder buffer, double x1, double y1, double z1, double x2, double y2, double z2, float red, float green, float blue) {
        buffer.vertex(x1, y1, z1).color(red, green, blue, 0.95F).next();
        buffer.vertex(x2, y2, z2).color(red, green, blue, 0.95F).next();
    }
}
