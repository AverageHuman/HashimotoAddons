package com.example.ha;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaDamageTruncation {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d+)?");
    private static final double ONE_MILLION = 1000000.0D;
    private static final double ONE_BILLION = 1000000000.0D;
    private static final double ONE_TRILLION = 1000000000000.0D;
    private static final DecimalFormat SHORT_FORMAT = new DecimalFormat("0.#");

    private HaDamageTruncation() {
    }

    public static Text transformLabel(Text text) {
        if (text == null) {
            return null;
        }

        HaConfig config = HaConfig.get();
        if (!config.damageTruncationEnabled) {
            return text;
        }

        String raw = text.getString();
        if (raw == null || raw.isEmpty() || containsExp(raw)) {
            return text;
        }

        LiteralText rebuilt = new LiteralText("");
        rebuilt.setStyle(text.getStyle());
        boolean changed = rebuildStyledSegments(text, rebuilt);
        if (!changed) {
            return text;
        }
        return rebuilt;
    }

    private static boolean containsExp(String raw) {
        String normalized = Formatting.strip(raw);
        if (normalized == null) {
            normalized = raw;
        }
        return normalized.toLowerCase(Locale.ROOT).contains("exp");
    }

    private static String abbreviate(String token) {
        double value;
        try {
            value = Double.parseDouble(token.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return null;
        }

        if (value <= ONE_MILLION) {
            return null;
        }
        if (value >= ONE_TRILLION) {
            return format(value / ONE_TRILLION) + "t";
        }
        if (value >= ONE_BILLION) {
            return format(value / ONE_BILLION) + "b";
        }
        return format(value / ONE_MILLION) + "m";
    }

    private static String format(double value) {
        synchronized (SHORT_FORMAT) {
            return SHORT_FORMAT.format(value);
        }
    }

    private static boolean rebuildStyledSegments(Text original, LiteralText rebuilt) {
        final boolean[] changed = new boolean[] { false };
        original.visit((Style style, String segment) -> {
            String rewritten = rewriteSegment(segment);
            if (!rewritten.equals(segment)) {
                changed[0] = true;
            }
            rebuilt.append(new LiteralText(rewritten).setStyle(style));
            return Optional.empty();
        }, original.getStyle());
        return changed[0];
    }

    private static String rewriteSegment(String segment) {
        Matcher matcher = NUMBER_PATTERN.matcher(segment);
        StringBuffer rewritten = new StringBuffer();
        boolean changed = false;
        while (matcher.find()) {
            String replacement = abbreviate(matcher.group());
            if (replacement == null) {
                matcher.appendReplacement(rewritten, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            matcher.appendReplacement(rewritten, Matcher.quoteReplacement(replacement));
            changed = true;
        }
        if (!changed) {
            return segment;
        }
        matcher.appendTail(rewritten);
        return rewritten.toString();
    }
}
