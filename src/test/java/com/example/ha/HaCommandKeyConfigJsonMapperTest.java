package com.example.ha;

import org.junit.Assert;
import org.junit.Test;

public final class HaCommandKeyConfigJsonMapperTest {
    @Test
    public void persistsConfiguredCommandAndBindingWithoutAddingASlash() {
        HaCommandKeyConfig config = new HaCommandKeyConfig();
        config.enabled = true;
        config.command = "/bp";
        config.keyCode = 66;
        config.scanCode = -1;
        config.keyType = "keysym";

        SavedCommandKeyConfig saved = HaCommandKeyConfigJsonMapper.toSaved(config);

        Assert.assertTrue(saved.enabled);
        Assert.assertEquals("/bp", saved.command);
        Assert.assertEquals(66, saved.keyCode);
    }

    @Test
    public void keepsMissingCommandDisabledAndDoesNotTreatItAsExecutable() {
        HaCommandKeyConfig config = new HaCommandKeyConfig();
        HaCommandKeyConfigJsonMapper.apply(new SavedCommandKeyConfig(), config);

        Assert.assertFalse(config.enabled);
        Assert.assertEquals("", config.command);
        Assert.assertFalse(config.hasExecutableCommand());
    }

    @Test
    public void requiresTheUserToProvideTheLeadingSlash() {
        HaCommandKeyConfig config = new HaCommandKeyConfig();
        config.command = "bp";
        config.normalize();

        Assert.assertEquals("bp", config.command);
        Assert.assertFalse(config.hasExecutableCommand());
    }
}
