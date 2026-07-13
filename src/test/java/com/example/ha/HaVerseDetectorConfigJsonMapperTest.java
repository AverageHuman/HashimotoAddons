package com.example.ha;

import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

public final class HaVerseDetectorConfigJsonMapperTest {
    @Test
    public void writesSharedAndVariantSpecificSettings() {
        HaVerseDetectorConfig config = new HaVerseDetectorConfig();
        config.enabled = false;
        config.autoThrowTrashVerseEnabled = false;

        JsonObject safe = HaVerseDetectorConfigJsonMapper.toJson(config, false);
        Assert.assertFalse(safe.get("enabled").getAsBoolean());
        Assert.assertFalse(safe.has("autoThrowTrashVerseEnabled"));

        JsonObject full = HaVerseDetectorConfigJsonMapper.toJson(config, true);
        Assert.assertFalse(full.get("enabled").getAsBoolean());
        Assert.assertFalse(full.get("autoThrowTrashVerseEnabled").getAsBoolean());
    }

    @Test
    public void appliesMissingFieldsWithoutChangingApprovedDefaults() {
        HaVerseDetectorConfig config = new HaVerseDetectorConfig();
        HaVerseDetectorConfigJsonMapper.apply(new JsonObject(), config, true);
        Assert.assertTrue(config.enabled);
        Assert.assertTrue(config.autoThrowTrashVerseEnabled);
    }
}
