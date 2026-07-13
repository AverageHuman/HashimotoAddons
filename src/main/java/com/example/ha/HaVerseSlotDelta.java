package com.example.ha;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class HaVerseSlotDelta {
    private HaVerseSlotDelta() {
    }

    static List<Integer> findChangedSlots(String targetKey, Map<Integer, SlotState> before, Map<Integer, SlotState> after) {
        if (targetKey == null || targetKey.isEmpty() || after == null || after.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> result = new ArrayList<Integer>();
        for (Map.Entry<Integer, SlotState> entry : after.entrySet()) {
            SlotState current = entry.getValue();
            if (current == null || !targetKey.equals(current.key) || current.count <= 0) {
                continue;
            }

            SlotState previous = before == null ? null : before.get(entry.getKey());
            int previousCount = previous != null && targetKey.equals(previous.key) ? previous.count : 0;
            if (current.count > previousCount) {
                result.add(entry.getKey());
            }
        }

        Collections.sort(result, new Comparator<Integer>() {
            @Override
            public int compare(Integer left, Integer right) {
                return left.compareTo(right);
            }
        });
        return result;
    }

    static final class SlotState {
        final String key;
        final int count;

        SlotState(String key, int count) {
            this.key = key;
            this.count = Math.max(0, count);
        }
    }
}
