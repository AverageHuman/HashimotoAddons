package com.example.ha;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public final class HaMobEspOverlay {
    private static final double MAX_DISTANCE_SQUARED = 65536.0D;
    private static final float ESP_RED = 0.00F;
    private static final float ESP_GREEN = 1.00F;
    private static final float ESP_BLUE = 1.00F;
    private static final float OUTLINE_ALPHA = 0.95F;
    private static final float FILL_ALPHA = 0.28F;
    private static long cachedWorldTime = Long.MIN_VALUE;
    private static int cachedWorldIdentity;
    private static boolean cachedEnabled;
    private static String cachedTargetName = "";
    private static List<HaMobEsp.RenderTarget> cachedTargets = Collections.emptyList();

    private HaMobEspOverlay() {
    }

    public static void render(WorldRenderContext context) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED || context.consumers() == null || context.camera() == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            return;
        }

        List<HaMobEsp.RenderTarget> targets = collectTargets(client);
        if (targets.isEmpty()) {
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
        boolean fillStarted = false;
        try {
            fillBuffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
            fillStarted = true;

            for (HaMobEsp.RenderTarget target : targets) {
                Box box = getRenderBox(target).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                drawFilledBox(fillBuffer, box, ESP_RED, ESP_GREEN, ESP_BLUE);
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

        for (HaMobEsp.RenderTarget target : targets) {
            Box box = getRenderBox(target).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            net.minecraft.client.render.WorldRenderer.drawBox(
                context.matrixStack(),
                lineBuffer,
                box.minX,
                box.minY,
                box.minZ,
                box.maxX,
                box.maxY,
                box.maxZ,
                ESP_RED,
                ESP_GREEN,
                ESP_BLUE,
                OUTLINE_ALPHA
            );
        }
    }

    static List<HaMobEsp.RenderTarget> collectTargets(MinecraftClient client) {
        if (client == null || client.world == null || client.player == null || !HaMobEsp.isEnabled()) {
            cachedEnabled = false;
            cachedWorldTime = Long.MIN_VALUE;
            cachedWorldIdentity = 0;
            cachedTargetName = "";
            cachedTargets = Collections.emptyList();
            return cachedTargets;
        }

        HaConfig config = HaConfig.get();
        long worldTime = client.world.getTime();
        int worldIdentity = System.identityHashCode(client.world);
        String targetName = config.mobEspTargetName;
        boolean enabled = config.mobEspEnabled;

        if (cachedEnabled == enabled
            && cachedWorldTime == worldTime
            && cachedWorldIdentity == worldIdentity
            && cachedTargetName.equals(targetName)) {
            return cachedTargets;
        }

        List<Entity> directCandidates = new ArrayList<Entity>();
        List<Entity> physicalCandidates = new ArrayList<Entity>();
        List<Entity> matchedEntities = new ArrayList<Entity>();
        for (Entity entity : client.world.getEntities()) {
            if (entity == null || entity == client.player || client.player.squaredDistanceTo(entity) > MAX_DISTANCE_SQUARED) {
                continue;
            }

            if (!(entity instanceof net.minecraft.entity.player.PlayerEntity) && !(entity instanceof net.minecraft.entity.decoration.ArmorStandEntity)) {
                physicalCandidates.add(entity);
            }
            if (HaMobEsp.isDirectMobTarget(entity)) {
                directCandidates.add(entity);
            }
            if (HaMobEsp.isNameCarrierMatch(entity, targetName)) {
                matchedEntities.add(entity);
            }
        }

        Set<Integer> renderedEntityIds = new HashSet<Integer>();
        List<HaMobEsp.RenderTarget> targets = new ArrayList<HaMobEsp.RenderTarget>();
        for (Entity entity : matchedEntities) {
            HaMobEsp.RenderTarget target = HaMobEsp.findRenderTarget(client, entity, directCandidates, physicalCandidates);
            if (target == null || target.entity == null) {
                continue;
            }

            Integer entityId = Integer.valueOf(target.entity.getEntityId());
            if (!renderedEntityIds.add(entityId)) {
                continue;
            }
            targets.add(target);
        }

        cachedEnabled = enabled;
        cachedWorldTime = worldTime;
        cachedWorldIdentity = worldIdentity;
        cachedTargetName = targetName;
        cachedTargets = Collections.unmodifiableList(targets);
        return cachedTargets;
    }

    static Box getRenderBox(HaMobEsp.RenderTarget target) {
        double expand = target.resolvedToMob ? 0.65D : 0.45D;
        return target.entity.getBoundingBox().expand(expand, target.resolvedToMob ? 1.25D : 0.75D, expand);
    }

    private static void drawFilledBox(BufferBuilder buffer, Box box, float red, float green, float blue) {
        addFace(buffer, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, box.minX, box.maxY, box.minZ, red, green, blue);
        addFace(buffer, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.minY, box.maxZ, red, green, blue);
        addFace(buffer, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ, box.minX, box.minY, box.maxZ, red, green, blue);
        addFace(buffer, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ, red, green, blue);
        addFace(buffer, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, red, green, blue);
        addFace(buffer, box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ, box.maxX, box.minY, box.minZ, red, green, blue);
    }

    private static void addFace(
        BufferBuilder buffer,
        double x1, double y1, double z1,
        double x2, double y2, double z2,
        double x3, double y3, double z3,
        double x4, double y4, double z4,
        float red, float green, float blue
    ) {
        addVertex(buffer, x1, y1, z1, red, green, blue);
        addVertex(buffer, x2, y2, z2, red, green, blue);
        addVertex(buffer, x3, y3, z3, red, green, blue);
        addVertex(buffer, x4, y4, z4, red, green, blue);
    }

    private static void addVertex(BufferBuilder buffer, double x, double y, double z, float red, float green, float blue) {
        buffer.vertex(x, y, z).color(red, green, blue, FILL_ALPHA).next();
    }

    private static void restoreRenderState() {
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

}
