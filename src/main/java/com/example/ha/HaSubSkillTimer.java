package com.example.ha;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaSubSkillTimer {
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*秒.*再使用");
    private static long cooldownStartedAtMillis;
    private static long cooldownDurationMillis;

    private HaSubSkillTimer() {
    }

    public static void onGameMessage(Text message) {
        if (message == null) {
            return;
        }

        String text = Formatting.strip(message.getString());
        if (text == null || text.isEmpty()) {
            return;
        }

        Matcher matcher = COOLDOWN_PATTERN.matcher(toAsciiDigits(text));
        if (!matcher.find()) {
            return;
        }

        try {
            double seconds = Double.parseDouble(matcher.group(1));
            if (seconds > 0.0D) {
                cooldownStartedAtMillis = System.currentTimeMillis();
                cooldownDurationMillis = Math.max(1L, Math.round(seconds * 1000.0D));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    public static boolean isActive() {
        return getRemainingMillis() > 0L;
    }

    public static long getRemainingMillis() {
        if (cooldownDurationMillis <= 0L) {
            return 0L;
        }
        long elapsed = System.currentTimeMillis() - cooldownStartedAtMillis;
        return Math.max(0L, cooldownDurationMillis - elapsed);
    }

    public static long getDurationMillis() {
        return cooldownDurationMillis;
    }

    public static float getRemainingRatio() {
        if (cooldownDurationMillis <= 0L) {
            return 0.0F;
        }
        return (float) Math.max(0.0D, Math.min(1.0D, getRemainingMillis() / (double) cooldownDurationMillis));
    }

    private static String toAsciiDigits(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '\uff10' && ch <= '\uff19') {
                result.append((char) ('0' + (ch - '\uff10')));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
