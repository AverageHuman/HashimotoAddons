package com.example.ha;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaMobHpDisplayOverlay {
    public static final String POSITION_HUD = "hud";
    public static final String POSITION_CROSSHAIR = "crosshair";
    private static final DecimalFormat HEALTH_FORMAT = new DecimalFormat("0.#");
    private static final DecimalFormat COMPACT_FORMAT = new DecimalFormat("0.#");
    private static final int BAR_HEIGHT = 4;
    private static final Map<Integer, Double> OBSERVED_MAX_HEALTH = new LinkedHashMap<Integer, Double>(50, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Double> eldest) {
            return size() > 50;
        }
    };

    private HaMobHpDisplayOverlay() {
    }

    public static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        HaConfig config = HaConfig.get();
        if (client == null || HaHudVisibility.shouldHideHashimotoHud(client) || !config.mobHpDisplayEnabled) {
            return;
        }

        TargetInfo target = findTarget(client);
        if (target == null) {
            return;
        }

        int x;
        int y;
        if (POSITION_CROSSHAIR.equals(normalizePosition(config.mobHpDisplayPosition))) {
            int width = getPanelWidth(client, target, config.mobHpDisplaySlim);
            x = client.getWindow().getScaledWidth() / 2 - width / 2;
            y = client.getWindow().getScaledHeight() / 2 + 12;
        } else {
            x = config.mobHpDisplayOverlayX;
            y = config.mobHpDisplayOverlayY;
        }
        drawPanel(matrices, x, y, target, config.mobHpDisplaySlim, config.mobHpDisplayShowPercentage, false);
    }

    public static void drawPreview(MatrixStack matrices, int x, int y, boolean selected) {
        TargetInfo target = new TargetInfo("Target Mob", "minecraft:zombie #123", 15.0D, 30.0D);
        HaConfig config = HaConfig.get();
        drawPanel(matrices, x, y, target, config.mobHpDisplaySlim, config.mobHpDisplayShowPercentage, true);
        if (selected) {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, "\u25c0", x - 8, y + 4, 0xFFFFFF);
        }
    }

    public static int getPanelWidth(MinecraftClient client) {
        return getPanelWidth(client, new TargetInfo("Target Mob", "minecraft:zombie #123", 15.0D, 30.0D), HaConfig.get().mobHpDisplaySlim);
    }

    public static int getPanelHeight() {
        return HaConfig.get().mobHpDisplaySlim ? 10 : 62;
    }

    public static String normalizePosition(String value) {
        if (POSITION_CROSSHAIR.equals(value)) {
            return POSITION_CROSSHAIR;
        }
        return POSITION_HUD;
    }

    public static String nextPosition(String value) {
        return POSITION_HUD.equals(normalizePosition(value)) ? POSITION_CROSSHAIR : POSITION_HUD;
    }

    public static String getPositionLabel(String value) {
        return POSITION_CROSSHAIR.equals(normalizePosition(value)) ? "Crosshair" : "HUD";
    }

    private static TargetInfo findTarget(MinecraftClient client) {
        LivingEntity living = HaMobTargeting.findTarget(client);
        return living == null ? null : toTargetInfo(living);
    }

    private static TargetInfo toTargetInfo(LivingEntity living) {
        double health = living.getHealth();
        double reportedMaxHealth = resolveMaxHealth(living);
        if (health <= 0.0D || reportedMaxHealth <= 0.0D) {
            return null;
        }
        double maxHealth = rememberMaxHealth(living, Math.max(health, reportedMaxHealth));
        return new TargetInfo(getName(living), getEntityDebugName(living), health, maxHealth);
    }

    private static int getPanelWidth(MinecraftClient client, TargetInfo target, boolean slim) {
        String hp = buildHpText(target, HaConfig.get().mobHpDisplayShowPercentage);
        if (slim) {
            return client.textRenderer.getWidth(hp);
        }
        int width = Math.max(132, client.textRenderer.getWidth("Mob HP Display") + 10);
        width = Math.max(width, client.textRenderer.getWidth(target.name) + 10);
        width = Math.max(width, client.textRenderer.getWidth("Entity: " + target.entityDebugName) + 10);
        width = Math.max(width, client.textRenderer.getWidth("HP: " + hp) + 10);
        return width;
    }

    private static void drawPanel(MatrixStack matrices, int x, int y, TargetInfo target, boolean slim, boolean showPercentage, boolean preview) {
        MinecraftClient client = MinecraftClient.getInstance();
        int width = getPanelWidth(client, target, slim);
        int height = slim ? 10 : 62;
        int color = hpColor(target.ratio());

        String hp = buildHpText(target, showPercentage);
        if (slim) {
            client.textRenderer.drawWithShadow(matrices, hp, x, y, color);
            return;
        }

        DrawableHelper.fill(matrices, x, y, x + width, y + height, 0x90000000);
        DrawableHelper.fill(matrices, x, y, x + width, y + 1, color | 0xFF000000);
        client.textRenderer.drawWithShadow(matrices, "Mob HP Display", x + 5, y + 4, 0xFFFFFF);
        client.textRenderer.drawWithShadow(matrices, target.name, x + 5, y + 16, 0xA0E8FF);
        client.textRenderer.drawWithShadow(matrices, "Entity: " + target.entityDebugName, x + 5, y + 28, 0xA0A0A0);
        client.textRenderer.drawWithShadow(matrices, "HP: " + hp, x + 5, y + 40, color);

        int barX = x + 5;
        int barY = y + height - BAR_HEIGHT - 5;
        int barWidth = width - 10;
        DrawableHelper.fill(matrices, barX, barY, barX + barWidth, barY + BAR_HEIGHT, 0xAA202020);
        int filled = Math.round(barWidth * target.ratio());
        DrawableHelper.fill(matrices, barX, barY, barX + filled, barY + BAR_HEIGHT, color | 0xFF000000);
    }

    private static String buildHpText(TargetInfo target, boolean showPercentage) {
        String result = formatHealth(target.health) + "/" + formatHealth(target.maxHealth);
        if (showPercentage) {
            result += " (" + Math.round(target.ratio() * 100.0F) + "%)";
        }
        return result;
    }

    private static String formatHealth(double value) {
        double safeValue = Math.max(0.0D, value);
        if (HaConfig.get().mobHpDisplayCompactNumbers) {
            if (safeValue >= 1000000000.0D) {
                return COMPACT_FORMAT.format(safeValue / 1000000000.0D) + "b";
            }
            if (safeValue >= 1000000.0D) {
                return COMPACT_FORMAT.format(safeValue / 1000000.0D) + "m";
            }
            if (safeValue >= 1000.0D) {
                return COMPACT_FORMAT.format(safeValue / 1000.0D) + "k";
            }
        }
        return HEALTH_FORMAT.format(safeValue);
    }

    private static int hpColor(float ratio) {
        if (ratio >= 0.66F) {
            return 0x55FF55;
        }
        if (ratio >= 0.33F) {
            return 0xFFD166;
        }
        return 0xFF5555;
    }

    private static String getName(Entity entity) {
        Text name = entity.getDisplayName();
        String result = name == null ? "" : name.getString();
        result = Formatting.strip(result);
        if (result == null || result.trim().isEmpty()) {
            result = entity.getType().getTranslationKey();
        }
        return result.trim();
    }

    private static double resolveMaxHealth(LivingEntity living) {
        double attributeMax = living.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
        return Math.max(attributeMax, living.getMaxHealth());
    }

    private static double rememberMaxHealth(LivingEntity living, double observedMaxHealth) {
        Integer id = Integer.valueOf(living.getEntityId());
        Double cached = OBSERVED_MAX_HEALTH.get(id);
        double result = cached == null ? observedMaxHealth : Math.max(cached.doubleValue(), observedMaxHealth);
        OBSERVED_MAX_HEALTH.put(id, Double.valueOf(result));
        return result;
    }

    private static String getEntityDebugName(net.minecraft.entity.Entity entity) {
        return EntityType.getId(entity.getType()).toString() + " #" + entity.getEntityId();
    }

    private static final class TargetInfo {
        final String name;
        final String entityDebugName;
        final double health;
        final double maxHealth;

        TargetInfo(String name, String entityDebugName, double health, double maxHealth) {
            this.name = name == null || name.trim().isEmpty() ? "Target Mob" : name;
            this.entityDebugName = entityDebugName == null || entityDebugName.trim().isEmpty() ? "unknown" : entityDebugName;
            this.health = health;
            this.maxHealth = maxHealth;
        }

        float ratio() {
            return maxHealth <= 0.0D ? 0.0F : (float) Math.max(0.0D, Math.min(1.0D, health / maxHealth));
        }
    }
}
