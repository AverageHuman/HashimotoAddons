package com.example.ha;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.MessageType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaRitualBookTimer {
    private static final String PURCHASE_PREFIX = "\u30a4\u30f3\u30bf\u30fc\u30b3\u30a4\u30f3\u30671\u500b\u306e";
    private static final String PURCHASE_SUFFIX = "\u306e\u5100\u5f0f\u66f8\u7269\u3092\u8cfc\u5165\u3057\u307e\u3057\u305f\u3002";
    private static final long TIMER_DURATION_MILLIS = 10L * 60L * 1000L;
    private static final int MAX_VISIBLE_TIMERS = 3;
    private static final long DUPLICATE_MESSAGE_WINDOW_MILLIS = 1500L;
    private static final List<RitualTimerEntry> ACTIVE_TIMERS = new ArrayList<RitualTimerEntry>();
    private static String lastProcessedMessage = "";
    private static long lastProcessedAtMillis;

    private HaRitualBookTimer() {
    }

    public static void onGameMessage(Text message, MessageType location) {
        if (!HaConfig.get().ritualBookTimerEnabled || message == null || location == MessageType.GAME_INFO) {
            return;
        }

        String text = Formatting.strip(message.getString());
        if (text == null || text.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (text.equals(lastProcessedMessage) && now - lastProcessedAtMillis <= DUPLICATE_MESSAGE_WINDOW_MILLIS) {
            return;
        }

        int start = text.indexOf(PURCHASE_PREFIX);
        if (start < 0) {
            return;
        }
        start += PURCHASE_PREFIX.length();

        int end = text.indexOf(PURCHASE_SUFFIX, start);
        if (end < 0 || end <= start) {
            return;
        }

        String noun = text.substring(start, end).trim();
        if (noun.isEmpty()) {
            return;
        }

        lastProcessedMessage = text;
        lastProcessedAtMillis = now;
        ACTIVE_TIMERS.add(0, new RitualTimerEntry(noun, now + TIMER_DURATION_MILLIS));
    }

    public static void tick(MinecraftClient client, HaConfig config) {
        long now = System.currentTimeMillis();
        Iterator<RitualTimerEntry> iterator = ACTIVE_TIMERS.iterator();
        while (iterator.hasNext()) {
            RitualTimerEntry entry = iterator.next();
            if (entry.expiresAtMillis > now) {
                continue;
            }
            if (config != null && config.ritualBookTimerEnabled && !entry.notified) {
                notifyTimerFinished(client, entry.noun);
                entry.notified = true;
            }
            iterator.remove();
        }
    }

    public static void onDisconnected() {
        ACTIVE_TIMERS.clear();
        lastProcessedMessage = "";
        lastProcessedAtMillis = 0L;
    }

    public static boolean hasActiveTimers() {
        return !ACTIVE_TIMERS.isEmpty();
    }

    public static List<RitualTimerView> getVisibleTimers() {
        if (ACTIVE_TIMERS.isEmpty()) {
            return Collections.emptyList();
        }

        long now = System.currentTimeMillis();
        List<RitualTimerView> result = new ArrayList<RitualTimerView>();
        int limit = Math.min(MAX_VISIBLE_TIMERS, ACTIVE_TIMERS.size());
        for (int i = 0; i < limit; i++) {
            RitualTimerEntry entry = ACTIVE_TIMERS.get(i);
            result.add(new RitualTimerView(entry.noun, Math.max(0L, entry.expiresAtMillis - now)));
        }
        return result;
    }

    public static List<RitualTimerView> getPreviewTimers() {
        List<RitualTimerView> result = new ArrayList<RitualTimerView>();
        result.add(new RitualTimerView("\u708e", 10L * 60L * 1000L));
        result.add(new RitualTimerView("\u6c34", 7L * 60L * 1000L + 35_000L));
        result.add(new RitualTimerView("\u98a8", 95_000L));
        return result;
    }

    private static void notifyTimerFinished(MinecraftClient client, String noun) {
        if (client == null || client.inGameHud == null) {
            return;
        }
        String label = getDisplayLabel(noun);
        client.inGameHud.setTitles(new LiteralText(label), new LiteralText("\u00a7e\u6642\u9593\u306b\u306a\u308a\u307e\u3057\u305f"), 5, 30, 10);
        if (client.player != null) {
            client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0F, 1.0F);
        }
    }

    public static String getDisplayLabel(String noun) {
        return noun + "\u306e\u5100\u5f0f\u66f8\u7269";
    }

    public static final class RitualTimerView {
        public final String noun;
        public final long remainingMillis;

        RitualTimerView(String noun, long remainingMillis) {
            this.noun = noun;
            this.remainingMillis = remainingMillis;
        }

        public String getDisplayText() {
            return getDisplayLabel(noun) + " " + HaExpTrackerOverlay.formatDuration(Math.max(0L, remainingMillis / 1000L));
        }

        public float getRemainingRatio() {
            return Math.max(0.0F, Math.min(1.0F, remainingMillis / (float) TIMER_DURATION_MILLIS));
        }
    }

    private static final class RitualTimerEntry {
        final String noun;
        final long expiresAtMillis;
        boolean notified;

        RitualTimerEntry(String noun, long expiresAtMillis) {
            this.noun = noun;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
