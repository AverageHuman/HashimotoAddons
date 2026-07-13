package com.example.ha;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Locale;

final class HaVerseClassifier {
    private static final Candidate[] CANDIDATES = new Candidate[] {
        new Candidate("ヴァース(特大)+", Kind.EXTRA_LARGE),
        new Candidate("ヴァース(特大)", Kind.EXTRA_LARGE),
        new Candidate("ヴァース(大)+", Kind.LARGE),
        new Candidate("ヴァース(大)", Kind.LARGE),
        new Candidate("ヴァース(中)+", Kind.MEDIUM),
        new Candidate("ヴァース(中)", Kind.MEDIUM),
        new Candidate("ヴァース(小)+", Kind.SMALL),
        new Candidate("ヴァース(小)", Kind.SMALL)
    };

    private HaVerseClassifier() {
    }

    static Result classify(String rawName) {
        String normalized = normalize(rawName);
        if (normalized.isEmpty()) {
            return null;
        }

        for (Candidate candidate : CANDIDATES) {
            if (normalized.contains(candidate.token)) {
                return new Result(candidate.kind, candidate.token);
            }
        }
        return null;
    }

    static Result classifyBeacon(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != Items.BEACON) {
            return null;
        }
        return classify(stack.getName().getString());
    }

    static String normalize(String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            return "";
        }

        StringBuilder stripped = new StringBuilder(rawName.length());
        for (int i = 0; i < rawName.length(); i++) {
            char current = rawName.charAt(i);
            if (current == '\u00a7' && i + 1 < rawName.length()) {
                i++;
                continue;
            }
            stripped.append(current);
        }
        return stripped.toString().trim().toLowerCase(Locale.ROOT);
    }

    enum Kind {
        SMALL(1),
        MEDIUM(2),
        LARGE(3),
        EXTRA_LARGE(4);

        final int overlayNumber;

        Kind(int overlayNumber) {
            this.overlayNumber = overlayNumber;
        }

        int overlayNumber() {
            return overlayNumber;
        }
    }

    static final class Result {
        final Kind kind;
        final String displayName;

        Result(Kind kind, String displayName) {
            this.kind = kind;
            this.displayName = displayName;
        }

        boolean isTrash() {
            return kind == Kind.SMALL || kind == Kind.MEDIUM;
        }

        boolean isProtectable() {
            return kind == Kind.EXTRA_LARGE;
        }

        int overlayNumber() {
            return kind.overlayNumber();
        }
    }

    private static final class Candidate {
        final String token;
        final Kind kind;

        Candidate(String token, Kind kind) {
            this.token = token;
            this.kind = kind;
        }
    }
}
