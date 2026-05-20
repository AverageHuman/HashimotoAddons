package com.example.ha;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

public final class HaMobEsp {
    private static final int DEFAULT_COLOR = 0xFFFFFF;
    private static final double SEARCH_HORIZONTAL_RANGE = 8.0D;
    private static final double SEARCH_VERTICAL_RANGE = 12.0D;
    private static final double SMALL_BOX_WIDTH = 0.35D;
    private static final double SMALL_BOX_HEIGHT = 0.60D;

    private HaMobEsp() {
    }

    public static boolean shouldGlow(Entity entity) {
        return isNameCarrierMatch(entity);
    }

    public static boolean isNameCarrierMatch(Entity entity) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED || entity == null || entity instanceof PlayerEntity) {
            return false;
        }

        HaConfig config = HaConfig.get();
        config.normalize();
        if (!config.mobEspEnabled || config.mobEspTargetName.isEmpty()) {
            return false;
        }

        return normalizeName(entity.getDisplayName()).contains(config.mobEspTargetName);
    }

    public static RenderTarget findRenderTarget(MinecraftClient client, Entity matchedEntity) {
        int color = getGlowColor(matchedEntity);
        if (client == null || client.world == null || matchedEntity == null) {
            return null;
        }

        if (isDirectMobTarget(matchedEntity)) {
            return new RenderTarget(matchedEntity, color, true);
        }

        Entity nearest = null;
        double nearestScore = Double.MAX_VALUE;
        for (Entity candidate : client.world.getEntities()) {
            if (!isDirectMobTarget(candidate) || candidate == matchedEntity) {
                continue;
            }
            if (!isWithinNameCarrierRange(matchedEntity, candidate)) {
                continue;
            }

            double score = targetScore(matchedEntity, candidate);
            if (score < nearestScore) {
                nearest = candidate;
                nearestScore = score;
            }
        }

        if (nearest == null) {
            nearest = findNearestPhysicalEntity(client, matchedEntity);
        }

        return nearest == null
            ? new RenderTarget(matchedEntity, color, false)
            : new RenderTarget(nearest, color, true);
    }

    public static int getGlowColor(Entity entity) {
        Text displayName = entity.getDisplayName();
        Integer color = findColor(displayName);
        return color == null ? DEFAULT_COLOR : color.intValue();
    }

    public static float red(int color) {
        return ((color >> 16) & 0xFF) / 255.0F;
    }

    public static float green(int color) {
        return ((color >> 8) & 0xFF) / 255.0F;
    }

    public static float blue(int color) {
        return (color & 0xFF) / 255.0F;
    }

    private static boolean isDirectMobTarget(Entity entity) {
        return entity instanceof LivingEntity
            && !(entity instanceof PlayerEntity)
            && !(entity instanceof ArmorStandEntity)
            && !isNameCarrierLike(entity);
    }

    private static boolean isNameCarrierLike(Entity entity) {
        if (entity instanceof ArmorStandEntity && ((ArmorStandEntity) entity).isMarker()) {
            return true;
        }

        Box box = entity.getBoundingBox();
        double width = Math.max(box.maxX - box.minX, box.maxZ - box.minZ);
        double height = box.maxY - box.minY;
        return width < SMALL_BOX_WIDTH || height < SMALL_BOX_HEIGHT;
    }

    private static boolean isWithinNameCarrierRange(Entity nameCarrier, Entity candidate) {
        double dx = Math.abs(nameCarrier.getX() - candidate.getX());
        double dz = Math.abs(nameCarrier.getZ() - candidate.getZ());
        double dy = Math.abs(nameCarrier.getY() - candidate.getY());
        return dx <= SEARCH_HORIZONTAL_RANGE
            && dz <= SEARCH_HORIZONTAL_RANGE
            && dy <= SEARCH_VERTICAL_RANGE;
    }

    private static double targetScore(Entity nameCarrier, Entity candidate) {
        double distance = nameCarrier.squaredDistanceTo(candidate);
        if (candidate.getY() <= nameCarrier.getY()) {
            distance *= 0.50D;
        }
        if (candidate.isInvisible()) {
            distance *= 1.25D;
        }
        return distance;
    }

    private static Entity findNearestPhysicalEntity(MinecraftClient client, Entity matchedEntity) {
        Entity nearest = null;
        double nearestScore = Double.MAX_VALUE;
        for (Entity candidate : client.world.getEntities()) {
            if (candidate == matchedEntity || candidate instanceof PlayerEntity || candidate instanceof ArmorStandEntity) {
                continue;
            }
            if (!isWithinNameCarrierRange(matchedEntity, candidate) || isTiny(candidate)) {
                continue;
            }

            double score = targetScore(matchedEntity, candidate);
            if (score < nearestScore) {
                nearest = candidate;
                nearestScore = score;
            }
        }
        return nearest;
    }

    private static boolean isTiny(Entity entity) {
        Box box = entity.getBoundingBox();
        double width = Math.max(box.maxX - box.minX, box.maxZ - box.minZ);
        double height = box.maxY - box.minY;
        return width < SMALL_BOX_WIDTH || height < SMALL_BOX_HEIGHT;
    }

    private static String normalizeName(Text text) {
        if (text == null) {
            return "";
        }

        String stripped = Formatting.strip(text.getString());
        return stripped == null ? "" : stripped.trim();
    }

    private static Integer findColor(Text text) {
        if (text == null) {
            return null;
        }

        TextColor styleColor = text.getStyle().getColor();
        if (styleColor != null) {
            return Integer.valueOf(styleColor.getRgb());
        }

        for (Text sibling : text.getSiblings()) {
            Integer siblingColor = findColor(sibling);
            if (siblingColor != null) {
                return siblingColor;
            }
        }
        return null;
    }

    public static final class RenderTarget {
        public final Entity entity;
        public final int color;
        public final boolean resolvedToMob;

        private RenderTarget(Entity entity, int color, boolean resolvedToMob) {
            this.entity = entity;
            this.color = color;
            this.resolvedToMob = resolvedToMob;
        }
    }
}
