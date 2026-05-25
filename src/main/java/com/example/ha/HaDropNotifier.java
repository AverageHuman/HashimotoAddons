package com.example.ha;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaDropNotifier {
    private static final double DETECTION_DISTANCE_SQUARED = 32.0D * 32.0D;
    private static final int SOUND_REPEAT_DELAY_TICKS = 4;
    private static final int SOUND_PLAY_COUNT = 10;
    private static final Set<Integer> NOTIFIED_ENTITY_IDS = new HashSet<Integer>();

    private static boolean activeSession;
    private static int pendingSoundPlays;
    private static int pendingSoundDelayTicks;

    private HaDropNotifier() {
    }

    public static void tick(MinecraftClient client) {
        HaConfig config = HaConfig.get();
        if (client == null || client.player == null || client.world == null || !config.dropNotifierEnabled) {
            stopSession();
            return;
        }

        if (HaSoulbindProtection.isSoulbound()) {
            activeSession = true;
        }
        if (!isNotificationAllowed(config)) {
            stopSession();
            return;
        }

        tickPendingSounds(client);
        Set<Integer> currentItemEntityIds = getCurrentItemEntityIds(client);
        removeStaleNotifiedEntities(currentItemEntityIds);
        if (config.dropNotifierEntries.isEmpty()) {
            return;
        }

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof ItemEntity)) {
                continue;
            }

            Integer entityId = Integer.valueOf(entity.getEntityId());
            if (NOTIFIED_ENTITY_IDS.contains(entityId) || client.player.squaredDistanceTo(entity) > DETECTION_DISTANCE_SQUARED) {
                continue;
            }

            String displayName = getDisplayName(((ItemEntity) entity).getStack());
            if (matchesAny(config, displayName)) {
                NOTIFIED_ENTITY_IDS.add(entityId);
                notify(client, displayName);
                return;
            }
        }
    }

    public static void onDisconnected() {
        stopSession();
    }

    private static boolean isNotificationAllowed(HaConfig config) {
        return config.dropNotifierEnabled
            && (HaSoulbindProtection.isSoulbound() || (config.dropNotifierContinueAfterStart && activeSession));
    }

    private static boolean matchesAny(HaConfig config, String displayName) {
        String normalizedName = normalizeMatchText(displayName);
        if (normalizedName.isEmpty()) {
            return false;
        }

        for (HaConfig.DropNotifierEntry entry : config.dropNotifierEntries) {
            if (entry.enabled && !entry.matchText.isEmpty() && normalizedName.contains(normalizeMatchText(entry.matchText))) {
                return true;
            }
        }
        return false;
    }

    private static void notify(MinecraftClient client, String displayName) {
        client.inGameHud.setTitles(new LiteralText(displayName), new LiteralText(""), 5, 30, 10);
        playNotificationSound(client);
        pendingSoundPlays = SOUND_PLAY_COUNT - 1;
        pendingSoundDelayTicks = SOUND_REPEAT_DELAY_TICKS;
    }

    private static void tickPendingSounds(MinecraftClient client) {
        if (pendingSoundPlays <= 0) {
            return;
        }
        if (pendingSoundDelayTicks > 0) {
            pendingSoundDelayTicks--;
            return;
        }

        playNotificationSound(client);
        pendingSoundPlays--;
        pendingSoundDelayTicks = SOUND_REPEAT_DELAY_TICKS;
    }

    private static void playNotificationSound(MinecraftClient client) {
        if (client.player != null) {
            client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0F, 1.0F);
        }
    }

    private static Set<Integer> getCurrentItemEntityIds(MinecraftClient client) {
        Set<Integer> currentItemEntityIds = new HashSet<Integer>();
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof ItemEntity) {
                currentItemEntityIds.add(Integer.valueOf(entity.getEntityId()));
            }
        }
        return currentItemEntityIds;
    }

    private static void removeStaleNotifiedEntities(Set<Integer> currentItemEntityIds) {
        Iterator<Integer> iterator = NOTIFIED_ENTITY_IDS.iterator();
        while (iterator.hasNext()) {
            if (!currentItemEntityIds.contains(iterator.next())) {
                iterator.remove();
            }
        }
    }

    private static String getDisplayName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        Text name = stack.getName();
        String value = name == null ? "" : name.getString();
        String stripped = Formatting.strip(value);
        String result = stripped == null ? value : stripped;
        return result.trim();
    }

    private static String normalizeMatchText(String value) {
        if (value == null) {
            return "";
        }
        String stripped = Formatting.strip(value);
        String result = stripped == null ? value : stripped;
        return result.trim().toLowerCase(Locale.ROOT);
    }

    private static void stopSession() {
        activeSession = false;
        pendingSoundPlays = 0;
        pendingSoundDelayTicks = 0;
        NOTIFIED_ENTITY_IDS.clear();
    }
}
