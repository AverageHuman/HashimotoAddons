package com.example.ha;

import org.junit.Assert;
import org.junit.Test;

public final class HaEvolutionForgeObservedBoundTest {
    @Test
    public void reportsExpandedBoundsAsChanges() {
        HaObservedStatBoundMerge.State existing = state(10.0D, 10.0D, "%");

        Assert.assertFalse(HaObservedStatBoundMerge.merge(existing, state(10.0D, 10.0D, "%")));
        Assert.assertTrue(HaObservedStatBoundMerge.merge(existing, state(8.0D, 12.0D, "%")));

        Assert.assertEquals(8.0D, existing.min, 0.0D);
        Assert.assertEquals(12.0D, existing.max, 0.0D);
        Assert.assertEquals("+8", existing.displayMin);
        Assert.assertEquals("+12", existing.displayMax);
    }

    @Test
    public void fillsPreviouslyUnknownUnit() {
        HaObservedStatBoundMerge.State existing = state(10.0D, 10.0D, "");

        Assert.assertTrue(HaObservedStatBoundMerge.merge(existing, state(10.0D, 10.0D, "%")));
        Assert.assertEquals("%", existing.unit);
    }

    private static HaObservedStatBoundMerge.State state(double min, double max, String unit) {
        HaObservedStatBoundMerge.State state = new HaObservedStatBoundMerge.State();
        state.min = min;
        state.max = max;
        state.hasMin = true;
        state.hasMax = true;
        state.unit = unit;
        state.displayMin = "+" + (int) min;
        state.displayMax = "+" + (int) max;
        return state;
    }
}
