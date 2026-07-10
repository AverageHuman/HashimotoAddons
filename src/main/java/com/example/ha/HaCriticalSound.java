package com.example.ha;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public final class HaCriticalSound {
    private static final long REPLAY_DEBOUNCE_MILLIS = 1500L;
    private static final String CRITICAL_DAGGER = new String(Character.toChars(0x1F5E1));
    private static final String CRITICAL_WAVE = new String(Character.toChars(0x1F30A));
    private static final int CRITICAL_COLOR_RGB = 0xAA0000;
    private static final Map<UUID, TriggerSnapshot> LAST_TRIGGERED_LABELS = new HashMap<UUID, TriggerSnapshot>();

    private HaCriticalSound() {
    }

    public static void onRenderedNameTag(Entity entity, Text text) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED || entity == null || text == null) {
            return;
        }

        HaConfig config = HaConfig.get();
        if (!config.criticalSoundEnabled) {
            LAST_TRIGGERED_LABELS.clear();
            return;
        }

        if (!matchesCriticalPattern(text)) {
            return;
        }

        String label = normalizeLabel(text.getString());
        if (label.isEmpty()) {
            return;
        }

        UUID uuid = entity.getUuid();
        if (uuid == null) {
            return;
        }

        long now = System.currentTimeMillis();
        TriggerSnapshot lastSnapshot = LAST_TRIGGERED_LABELS.get(uuid);
        if (lastSnapshot != null
            && label.equals(lastSnapshot.label)
            && now - lastSnapshot.triggeredAtMillis < REPLAY_DEBOUNCE_MILLIS) {
            return;
        }

        LAST_TRIGGERED_LABELS.put(uuid, new TriggerSnapshot(label, now));
        playCriticalSound(MinecraftClient.getInstance());
    }

    public static void playCriticalSound(MinecraftClient client) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            return;
        }
        if (client == null || client.player == null) {
            return;
        }

        SoundEvent sound = HaSounds.CRITICAL_SOUND;
        if (sound == null) {
            return;
        }

        float volume = HaConfig.get().criticalSoundVolume / 100.0F;
        client.player.playSound(sound, SoundCategory.MASTER, volume, 1.0F);
    }

    public static void onDisconnected() {
        LAST_TRIGGERED_LABELS.clear();
    }

    static boolean matchesCriticalPattern(Text text) {
        if (text == null) {
            return false;
        }

        String fullText = normalizeLabel(text.getString());
        if (fullText.isEmpty() || fullText.indexOf(CRITICAL_WAVE) < 0) {
            return false;
        }

        FirstSegment firstSegment = extractFirstSegment(text);
        return firstSegment != null
            && CRITICAL_DAGGER.equals(firstSegment.text)
            && firstSegment.colorRgb == CRITICAL_COLOR_RGB
            && firstSegment.bold;
    }

    private static FirstSegment extractFirstSegment(Text text) {
        final FirstSegment[] result = new FirstSegment[1];
        try {
            text.visit((style, value) -> {
                if (value == null || value.isEmpty()) {
                    return Optional.empty();
                }
                result[0] = new FirstSegment(value, resolveColor(style), style != null && style.isBold());
                return Optional.of(Boolean.TRUE);
            }, Style.EMPTY);
        } catch (RuntimeException exception) {
            return null;
        }
        return result[0];
    }

    private static int resolveColor(Style style) {
        if (style == null) {
            return -1;
        }

        TextColor color = style.getColor();
        return color == null ? -1 : color.getRgb() & 0xFFFFFF;
    }

    private static String normalizeLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static final class TriggerSnapshot {
        final String label;
        final long triggeredAtMillis;

        TriggerSnapshot(String label, long triggeredAtMillis) {
            this.label = label;
            this.triggeredAtMillis = triggeredAtMillis;
        }
    }

    private static final class FirstSegment {
        final String text;
        final int colorRgb;
        final boolean bold;

        FirstSegment(String text, int colorRgb, boolean bold) {
            this.text = text;
            this.colorRgb = colorRgb;
            this.bold = bold;
        }
    }
}
