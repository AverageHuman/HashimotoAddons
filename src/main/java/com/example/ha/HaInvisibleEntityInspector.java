package com.example.ha;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

public final class HaInvisibleEntityInspector {
    private static final double MAX_TARGET_DISTANCE = 32.0D;
    private static final double TARGET_BOX_EXPAND = 0.75D;
    private static final double NEARBY_RADIUS = 3.0D;
    private static final int MAX_NEARBY_ENTITIES = 6;

    private HaInvisibleEntityInspector() {
    }

    public static void inspect(MinecraftClient client) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED
            || client == null
            || client.player == null
            || client.world == null
            || !HaConfig.get().invisibleEntityInspectorEnabled) {
            return;
        }

        Entity target = findTarget(client);
        if (target == null) {
            sendMessage(client, "\u00a7cNo entity found on the view ray.");
            return;
        }

        sendMessage(client, "\u00a7aEntity Inspector: \u00a7f" + normalize(target.getDisplayName().getString()));
        sendMessage(client, "\u00a77Type: \u00a7f" + typeId(target) + " \u00a78| Java: \u00a7f" + target.getClass().getName());
        sendMessage(client, "\u00a77Renderer: \u00a7f" + rendererClass(client, target));
        sendMessage(client, "\u00a77Entity ID: \u00a7f" + target.getEntityId() + " \u00a78| UUID: \u00a7f" + target.getUuidAsString());
        sendMessage(client, String.format(Locale.ROOT,
            "\u00a77Position: \u00a7f%.2f, %.2f, %.2f \u00a78| Distance: \u00a7f%.2f",
            target.getX(), target.getY(), target.getZ(), client.player.distanceTo(target)));
        sendMessage(client, "\u00a77Invisible: \u00a7f" + target.isInvisible()
            + " \u00a78| Invisible to you: \u00a7f" + target.isInvisibleTo(client.player)
            + " \u00a78| Glowing: \u00a7f" + target.isGlowing());
        sendMessage(client, "\u00a77Alive: \u00a7f" + target.isAlive()
            + " \u00a78| Silent: \u00a7f" + target.isSilent()
            + " \u00a78| No gravity: \u00a7f" + target.hasNoGravity()
            + " \u00a78| Pose: \u00a7f" + target.getPose());
        sendMessage(client, "\u00a77Custom name: \u00a7f" + customName(target)
            + " \u00a78| Name visible: \u00a7f" + target.isCustomNameVisible());

        Box box = target.getBoundingBox();
        Vec3d velocity = target.getVelocity();
        sendMessage(client, String.format(Locale.ROOT,
            "\u00a77Box W/H/D: \u00a7f%.3f / %.3f / %.3f \u00a78| Velocity: \u00a7f%.3f, %.3f, %.3f",
            box.getXLength(), box.getYLength(), box.getZLength(), velocity.x, velocity.y, velocity.z));
        sendMessage(client, "\u00a77Vehicle: \u00a7f" + (target.hasVehicle() ? typeId(target.getVehicle()) : "None")
            + " \u00a78| Passengers: \u00a7f" + target.getPassengerList().size());
        sendMessage(client, "\u00a77Tags: \u00a7f" + scoreboardTags(target.getScoreboardTags()));

        if (target instanceof LivingEntity) {
            inspectLiving(client, (LivingEntity) target);
        }
        if (target instanceof ArmorStandEntity) {
            inspectArmorStand(client, (ArmorStandEntity) target);
        }
        inspectNearby(client, target);
    }

    private static Entity findTarget(MinecraftClient client) {
        if (client.crosshairTarget instanceof EntityHitResult
            && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity direct = ((EntityHitResult) client.crosshairTarget).getEntity();
            if (direct != null && direct != client.player) {
                return direct;
            }
        }

        Vec3d start = client.player.getCameraPosVec(1.0F);
        Vec3d end = start.add(client.player.getRotationVec(1.0F).multiply(MAX_TARGET_DISTANCE));
        Entity nearestInvisible = null;
        Entity nearestAny = null;
        double nearestInvisibleDistance = Double.MAX_VALUE;
        double nearestAnyDistance = Double.MAX_VALUE;

        for (Entity entity : client.world.getEntities()) {
            if (entity == null || entity == client.player) {
                continue;
            }
            Optional<Vec3d> hit = entity.getBoundingBox().expand(TARGET_BOX_EXPAND).raycast(start, end);
            if (!hit.isPresent()) {
                continue;
            }
            double distance = start.squaredDistanceTo(hit.get());
            if (distance < nearestAnyDistance) {
                nearestAnyDistance = distance;
                nearestAny = entity;
            }
            if ((entity.isInvisible() || entity.isInvisibleTo(client.player)) && distance < nearestInvisibleDistance) {
                nearestInvisibleDistance = distance;
                nearestInvisible = entity;
            }
        }
        return nearestInvisible != null ? nearestInvisible : nearestAny;
    }

    private static void inspectLiving(MinecraftClient client, LivingEntity living) {
        sendMessage(client, String.format(Locale.ROOT,
            "\u00a77Health: \u00a7f%.2f / %.2f \u00a78| Effects: \u00a7f%s",
            living.getHealth(), living.getMaxHealth(), statusEffects(living.getStatusEffects())));
        sendItemLine(client, "Main hand", living.getEquippedStack(EquipmentSlot.MAINHAND));
        sendItemLine(client, "Offhand", living.getEquippedStack(EquipmentSlot.OFFHAND));
        sendItemLine(client, "Head", living.getEquippedStack(EquipmentSlot.HEAD));
        sendItemLine(client, "Chest", living.getEquippedStack(EquipmentSlot.CHEST));
        sendItemLine(client, "Legs", living.getEquippedStack(EquipmentSlot.LEGS));
        sendItemLine(client, "Feet", living.getEquippedStack(EquipmentSlot.FEET));
    }

    private static void inspectArmorStand(MinecraftClient client, ArmorStandEntity stand) {
        sendMessage(client, "\u00a76Armor stand: \u00a7fmarker=" + stand.isMarker()
            + ", small=" + stand.isSmall()
            + ", arms=" + stand.shouldShowArms()
            + ", hideBasePlate=" + stand.shouldHideBasePlate());
    }

    private static void inspectNearby(MinecraftClient client, Entity target) {
        List<Entity> nearby = new ArrayList<>();
        for (Entity entity : client.world.getEntities()) {
            if (entity != null
                && entity != target
                && entity != client.player
                && target.squaredDistanceTo(entity) <= NEARBY_RADIUS * NEARBY_RADIUS) {
                nearby.add(entity);
            }
        }
        nearby.sort(Comparator.comparingDouble(target::squaredDistanceTo));

        if (nearby.isEmpty()) {
            sendMessage(client, "\u00a77Nearby entities (3 blocks): \u00a78None");
            return;
        }

        sendMessage(client, "\u00a77Nearby entities (3 blocks): \u00a7f" + nearby.size());
        for (int i = 0; i < Math.min(nearby.size(), MAX_NEARBY_ENTITIES); i++) {
            Entity entity = nearby.get(i);
            sendMessage(client, String.format(Locale.ROOT,
                "\u00a78- \u00a7f%s \u00a77(%s, %s, invisible=%s, %.2fm)",
                normalize(entity.getDisplayName().getString()), typeId(entity), entity.getClass().getSimpleName(),
                entity.isInvisible() || entity.isInvisibleTo(client.player), target.distanceTo(entity)));
        }
    }

    private static String rendererClass(MinecraftClient client, Entity entity) {
        try {
            EntityRenderer<?> renderer = client.getEntityRenderDispatcher().getRenderer(entity);
            return renderer == null ? "None" : renderer.getClass().getName();
        } catch (RuntimeException exception) {
            return "Error: " + exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
    }

    private static String customName(Entity entity) {
        return entity.hasCustomName() && entity.getCustomName() != null
            ? normalize(entity.getCustomName().getString())
            : "None";
    }

    private static String typeId(Entity entity) {
        Identifier id = EntityType.getId(entity.getType());
        return id == null ? String.valueOf(entity.getType()) : id.toString();
    }

    private static String scoreboardTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "None";
        }
        return tags.stream().sorted().collect(Collectors.joining(", "));
    }

    private static String statusEffects(Collection<StatusEffectInstance> effects) {
        if (effects == null || effects.isEmpty()) {
            return "None";
        }
        return effects.stream()
            .map(effect -> {
                Identifier id = Registry.STATUS_EFFECT.getId(effect.getEffectType());
                return (id == null ? effect.getEffectType().toString() : id.toString())
                    + " x" + (effect.getAmplifier() + 1)
                    + " (" + effect.getDuration() + "t)";
            })
            .sorted()
            .collect(Collectors.joining(", "));
    }

    private static void sendItemLine(MinecraftClient client, String label, ItemStack stack) {
        BaseText text = new LiteralText(label + ": ");
        if (stack == null || stack.isEmpty()) {
            text.append(new LiteralText("None").formatted(Formatting.DARK_GRAY));
        } else {
            text.append(stack.toHoverableText());
        }
        client.player.sendMessage(text, false);
    }

    private static void sendMessage(MinecraftClient client, String message) {
        client.player.sendMessage(new LiteralText(message), false);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String stripped = Formatting.strip(value);
        return stripped == null ? value.trim() : stripped.trim();
    }
}
