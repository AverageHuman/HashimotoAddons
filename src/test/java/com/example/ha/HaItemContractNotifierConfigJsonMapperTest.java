package com.example.ha;

import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

public final class HaItemContractNotifierConfigJsonMapperTest {
    @Test
    public void persistsDisabledDefaultsAndEnabledStates() {
        HaItemContractNotifierConfig config = new HaItemContractNotifierConfig();
        JsonObject defaults = HaItemContractNotifierConfigJsonMapper.toJson(config);
        Assert.assertFalse(defaults.get("enabled").getAsBoolean());
        Assert.assertFalse(defaults.has("debugHudEnabled"));

        config.enabled = true;
        JsonObject enabled = HaItemContractNotifierConfigJsonMapper.toJson(config);
        Assert.assertTrue(enabled.get("enabled").getAsBoolean());
    }

    @Test
    public void keepsDefaultsForMissingFieldsAndAppliesStoredStates() {
        HaItemContractNotifierConfig config = new HaItemContractNotifierConfig();
        HaItemContractNotifierConfigJsonMapper.apply(new JsonObject(), config);
        Assert.assertFalse(config.enabled);

        JsonObject source = new JsonObject();
        source.addProperty("enabled", true);
        source.addProperty("debugHudEnabled", true);
        HaItemContractNotifierConfigJsonMapper.apply(source, config);
        Assert.assertTrue(config.enabled);
    }
}
