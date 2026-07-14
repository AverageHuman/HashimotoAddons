package com.example.ha;

/** Resolves the canonical item name used by Verse observed-stat data. */
final class HaVerseStatsNameResolver {
    private static final String[] CANDIDATES = new String[] {
        "ヴァース(極小)+",
        "ヴァース(極小)",
        "ヴァース(特大)+",
        "ヴァース(特大)",
        "ヴァース(大)+",
        "ヴァース(大)",
        "ヴァース(中)+",
        "ヴァース(中)",
        "ヴァース(小)+",
        "ヴァース(小)"
    };

    private HaVerseStatsNameResolver() {
    }

    static String resolve(String rawName) {
        String normalized = stripFormatting(rawName).trim();
        if (normalized.isEmpty()) {
            return "";
        }

        for (String candidate : CANDIDATES) {
            if (normalized.contains(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    private static String stripFormatting(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\u00a7' && i + 1 < value.length()) {
                i++;
                continue;
            }
            result.append(current);
        }
        return result.toString();
    }
}
