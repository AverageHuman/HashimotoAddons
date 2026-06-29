package com.example.ha;

import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

final class HaMobTargeting {
    private static final double FALLBACK_TARGET_DISTANCE = 64.0D;
    private static final double FALLBACK_BOX_EXPAND = 0.35D;

    private HaMobTargeting() {
    }

    static LivingEntity findTarget(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return null;
        }

        LivingEntity directTarget = findDirectTarget(client);
        if (directTarget != null) {
            return directTarget;
        }
        return findFallbackRayTarget(client);
    }

    private static LivingEntity findDirectTarget(MinecraftClient client) {
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.ENTITY) {
            return null;
        }

        Entity entity = ((EntityHitResult) client.crosshairTarget).getEntity();
        if (!isValidTargetEntity(client, entity)) {
            return null;
        }

        LivingEntity living = (LivingEntity) entity;
        if (!isUsableLivingTarget(living)) {
            return null;
        }
        return living;
    }

    private static LivingEntity findFallbackRayTarget(MinecraftClient client) {
        Vec3d start = client.player.getCameraPosVec(1.0F);
        Vec3d direction = client.player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(FALLBACK_TARGET_DISTANCE));

        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : client.world.getEntities()) {
            if (!isValidTargetEntity(client, entity) || !entity.isAlive()) {
                continue;
            }

            LivingEntity living = (LivingEntity) entity;
            if (!isUsableLivingTarget(living)) {
                continue;
            }

            Box box = entity.getBoundingBox().expand(FALLBACK_BOX_EXPAND);
            Optional<Vec3d> hit = box.raycast(start, end);
            if (!hit.isPresent()) {
                continue;
            }

            double distance = start.squaredDistanceTo(hit.get());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = living;
            }
        }
        return best;
    }

    private static boolean isValidTargetEntity(MinecraftClient client, Entity entity) {
        return entity instanceof LivingEntity
            && entity != client.player
            && entity.getType() != EntityType.ARMOR_STAND;
    }

    private static boolean isUsableLivingTarget(LivingEntity living) {
        return living.getHealth() > 0.0F && resolveMaxHealth(living) > 0.0D;
    }

    private static double resolveMaxHealth(LivingEntity living) {
        double attributeMax = living.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
        return Math.max(attributeMax, living.getMaxHealth());
    }
}
