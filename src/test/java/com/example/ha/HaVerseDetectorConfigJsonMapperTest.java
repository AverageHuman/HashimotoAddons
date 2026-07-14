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
        config.autoThrowTrashVerseDelayTicks = 40;

        JsonObject safe = HaVerseDetectorConfigJsonMapper.toJson(config, false);
        Assert.assertFalse(safe.get("enabled").getAsBoolean());
        Assert.assertFalse(safe.has("autoThrowTrashVerseEnabled"));

        JsonObject full = HaVerseDetectorConfigJsonMapper.toJson(config, true);
        Assert.assertFalse(full.get("enabled").getAsBoolean());
        Assert.assertFalse(full.get("autoThrowTrashVerseEnabled").getAsBoolean());
        Assert.assertEquals(40, full.get("autoThrowTrashVerseDelayTicks").getAsInt());
        Assert.assertFalse(safe.has("autoThrowTrashVerseDelayTicks"));
    }

    @Test
    public void appliesMissingFieldsWithoutChangingApprovedDefaults() {
        HaVerseDetectorConfig config = new HaVerseDetectorConfig();
        HaVerseDetectorConfigJsonMapper.apply(new JsonObject(), config, true);
        Assert.assertTrue(config.enabled);
        Assert.assertTrue(config.autoThrowTrashVerseEnabled);
        Assert.assertEquals(0, config.autoThrowTrashVerseDelayTicks);
    }

    @Test
    public void appliesFullDelayField() {
        HaVerseDetectorConfig config = new HaVerseDetectorConfig();
        JsonObject source = new JsonObject();
        source.addProperty("autoThrowTrashVerseDelayTicks", 40);

        HaVerseDetectorConfigJsonMapper.apply(source, config, true);

        Assert.assertEquals(40, config.autoThrowTrashVerseDelayTicks);
    }

    @Test
    public void normalizesDelayToApprovedRange() {
        HaVerseDetectorConfig config = new HaVerseDetectorConfig();
        config.autoThrowTrashVerseDelayTicks = -1;
        config.normalize();
        Assert.assertEquals(0, config.autoThrowTrashVerseDelayTicks);

        config.autoThrowTrashVerseDelayTicks = 72001;
        config.normalize();
        Assert.assertEquals(72000, config.autoThrowTrashVerseDelayTicks);
    }
}
