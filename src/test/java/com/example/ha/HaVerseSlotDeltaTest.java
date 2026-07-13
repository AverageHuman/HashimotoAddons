package com.example.ha;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public final class HaVerseSlotDeltaTest {
    @Test
    public void selectsOnlyTheStackThatReceivedThePickup() {
        Map<Integer, HaVerseSlotDelta.SlotState> before = new HashMap<Integer, HaVerseSlotDelta.SlotState>();
        before.put(Integer.valueOf(5), new HaVerseSlotDelta.SlotState("trash", 3));
        before.put(Integer.valueOf(6), new HaVerseSlotDelta.SlotState("trash", 1));

        Map<Integer, HaVerseSlotDelta.SlotState> after = new HashMap<Integer, HaVerseSlotDelta.SlotState>();
        after.put(Integer.valueOf(5), new HaVerseSlotDelta.SlotState("trash", 4));
        after.put(Integer.valueOf(6), new HaVerseSlotDelta.SlotState("trash", 1));

        List<Integer> changed = HaVerseSlotDelta.findChangedSlots("trash", before, after);
        Assert.assertEquals(Arrays.asList(Integer.valueOf(5)), changed);
    }

    @Test
    public void selectsNewlyCreatedStackAndIgnoresDifferentKeys() {
        Map<Integer, HaVerseSlotDelta.SlotState> before = new HashMap<Integer, HaVerseSlotDelta.SlotState>();
        before.put(Integer.valueOf(5), new HaVerseSlotDelta.SlotState("other", 1));

        Map<Integer, HaVerseSlotDelta.SlotState> after = new HashMap<Integer, HaVerseSlotDelta.SlotState>();
        after.put(Integer.valueOf(5), new HaVerseSlotDelta.SlotState("trash", 1));
        after.put(Integer.valueOf(6), new HaVerseSlotDelta.SlotState("other", 2));

        Assert.assertEquals(Arrays.asList(Integer.valueOf(5)), HaVerseSlotDelta.findChangedSlots("trash", before, after));
    }
}
