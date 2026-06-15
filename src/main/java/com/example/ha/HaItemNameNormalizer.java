package com.example.ha;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class HaItemNameNormalizer {
    private static final Pattern ENHANCEMENT_SUFFIX = Pattern.compile("\\s*\\(\\+([1-9]|1[0-2])\\)\\s*$");
    private static final Pattern LEADING_MARKERS = Pattern.compile("^[\\s\\u2715\\u2716\\u00d7xX*\\-:\\uFF1A\\u30FB]+");
    private static final Pattern LEADING_COUNT = Pattern.compile("^[0-9]+\\s+");
    private static final List<String> ITEM_PREFIXES = Arrays.asList(
        "\u3044\u3076\u3057\u9280\u306e", "\u3057\u3063\u304b\u308a\u3068\u3057\u305f", "\u3068\u3093\u3067\u3082\u306a\u3044", "\u3069\u3063\u3057\u308a\u3068\u3057\u305f", "\u30ad\u30e9\u30ad\u30e9\u3057\u305f",
        "\u4e08\u592b\u306a", "\u4e0a\u8cea\u306a", "\u511a\u3044", "\u5206\u539a\u3044", "\u5927\u9b54\u738b\u306e", "\u5947\u8de1\u7684\u306a",
        "\u5b8c\u5168\u7121\u6b20\u306e", "\u6539\u9020\u3055\u308c\u305f", "\u666e\u901a\u306e", "\u6975\u81f4\u306e", "\u76e4\u77f3\u306a", "\u77e5\u7684\u306a",
        "\u795e\u3005\u3057\u3044", "\u795e\u79d8\u306e", "\u798d\u3005\u3057\u3044", "\u7acb\u6d3e\u306a", "\u7cbe\u5bc6\u306a", "\u7cbe\u5de7\u306a",
        "\u78e8\u304d\u4e0a\u3052\u3089\u308c\u305f", "\u714c\u3073\u3084\u304b\u306a", "\u7a76\u6975\u306e", "\u7d14\u7136\u305f\u308b", "\u7dba\u9e97\u306a", "\u7f8e\u3057\u3044", "\u898b\u4e8b\u306a", "\u8a08\u308a\u77e5\u308c\u306a\u3044", "\u8efd\u3044",
        "\u8f1d\u304d\u306e", "\u8fc5\u901f\u306a", "\u91cd\u3044", "\u9583\u5149\u306e", "\u9811\u4e08\u306a", "\u9e97\u3057\u3044",
        "\u552f\u4e00\u7121\u4e8c\u306e", "\u5353\u8d8a\u3057\u305f"
    );

    private HaItemNameNormalizer() {
    }

    static String normalize(String value) {
        String result = normalizeBeforePrefixes(value);
        result = stripPrefixes(result);
        return result.trim();
    }

    static boolean hasRemovablePrefix(String value) {
        String normalized = normalizeBeforePrefixes(value);
        for (String prefix : ITEM_PREFIXES) {
            if (startsWithPrefixAndWhitespace(normalized, prefix)) {
                return true;
            }
        }
        return false;
    }

    static Text preserveStyle(Text source, String normalizedName) {
        if (source == null || normalizedName == null || normalizedName.isEmpty()) {
            return new LiteralText(normalizedName == null ? "" : normalizedName);
        }

        String sourceName = source.getString();
        int start = sourceName.lastIndexOf(normalizedName);
        if (start < 0) {
            return new LiteralText(normalizedName).setStyle(source.getStyle());
        }

        final int rangeStart = start;
        final int rangeEnd = start + normalizedName.length();
        final int[] offset = new int[] { 0 };
        final LiteralText rebuilt = new LiteralText("");
        source.visit((Style style, String segment) -> {
            int segmentStart = offset[0];
            int segmentEnd = segmentStart + segment.length();
            int copyStart = Math.max(segmentStart, rangeStart);
            int copyEnd = Math.min(segmentEnd, rangeEnd);
            if (copyStart < copyEnd) {
                String part = segment.substring(copyStart - segmentStart, copyEnd - segmentStart);
                rebuilt.append(new LiteralText(part).setStyle(style));
            }
            offset[0] = segmentEnd;
            return Optional.empty();
        }, source.getStyle());
        return rebuilt.getString().equals(normalizedName)
            ? rebuilt
            : new LiteralText(normalizedName).setStyle(source.getStyle());
    }

    private static String normalizeBeforePrefixes(String value) {
        String result = normalizeDisplay(value);
        result = ENHANCEMENT_SUFFIX.matcher(result).replaceFirst("");
        result = LEADING_MARKERS.matcher(result).replaceFirst("");
        result = toAsciiDigits(result);
        return LEADING_COUNT.matcher(result).replaceFirst("");
    }

    private static String stripPrefixes(String value) {
        String result = value == null ? "" : value.trim();
        boolean changed;
        do {
            changed = false;
            for (String prefix : ITEM_PREFIXES) {
                if (startsWithPrefixAndWhitespace(result, prefix)) {
                    result = result.substring(prefix.length()).trim();
                    changed = true;
                    break;
                }
            }
        } while (changed);
        return result;
    }

    private static boolean startsWithPrefixAndWhitespace(String value, String prefix) {
        return value != null
            && value.startsWith(prefix)
            && value.length() > prefix.length()
            && Character.isWhitespace(value.charAt(prefix.length()));
    }

    private static String normalizeDisplay(String value) {
        if (value == null) {
            return "";
        }
        String stripped = Formatting.strip(value);
        if (stripped == null) {
            stripped = value;
        }
        return stripped.replace('\u3000', ' ').trim();
    }

    private static String toAsciiDigits(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '\uFF10' && ch <= '\uFF19') {
                result.append((char) ('0' + (ch - '\uFF10')));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
