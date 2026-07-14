package com.example.ha;

/** Pure merge logic for an observed stat's minimum and maximum values. */
final class HaObservedStatBoundMerge {
    private HaObservedStatBoundMerge() {
    }

    static boolean merge(State existing, State incoming) {
        if (existing == null || incoming == null) {
            return false;
        }

        boolean changed = false;
        if (incoming.hasMin && (!existing.hasMin || incoming.min < existing.min)) {
            existing.hasMin = true;
            existing.min = incoming.min;
            existing.displayMin = incoming.displayMin;
            changed = true;
        }
        if (incoming.hasMax && (!existing.hasMax || incoming.max > existing.max)) {
            existing.hasMax = true;
            existing.max = incoming.max;
            existing.displayMax = incoming.displayMax;
            changed = true;
        }
        if ((existing.unit == null || existing.unit.isEmpty()) && incoming.unit != null && !incoming.unit.isEmpty()) {
            existing.unit = incoming.unit;
            changed = true;
        }
        return changed;
    }

    static final class State {
        double min;
        double max;
        boolean hasMin;
        boolean hasMax;
        String unit = "";
        String displayMin = "";
        String displayMax = "";
    }
}
