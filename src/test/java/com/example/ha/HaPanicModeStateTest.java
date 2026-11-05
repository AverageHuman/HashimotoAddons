package com.example.ha;

import org.junit.Assert;
import org.junit.Test;

public final class HaPanicModeStateTest {
    @Test
    public void togglesOncePerPressAndRestoresOnSecondPress() {
        HaPanicModeState state = new HaPanicModeState();
        Assert.assertTrue(state.update(true, true, true));
        Assert.assertTrue(state.isActive());
        Assert.assertFalse(state.update(true, true, true));
        Assert.assertTrue(state.isActive());
        state.update(true, false, true);
        Assert.assertTrue(state.update(true, true, true));
        Assert.assertFalse(state.isActive());
    }

    @Test
    public void captureAndDisabledPressesCannotLeakIntoGameplay() {
        HaPanicModeState state = new HaPanicModeState();
        state.update(false, true, true);
        Assert.assertFalse(state.update(true, true, true));
        state.update(true, false, false);
        state.update(true, true, false);
        Assert.assertFalse(state.update(true, true, true));
        Assert.assertFalse(state.isActive());
    }

    @Test
    public void worldAbsenceDoesNotResumeButANewProcessStartsInactive() {
        HaPanicModeState state = new HaPanicModeState();
        state.update(true, true, true);
        state.update(true, false, false);
        Assert.assertTrue(state.isActive());
        Assert.assertFalse(new HaPanicModeState().isActive());
        state.resume();
        Assert.assertFalse(state.isActive());
    }
}
