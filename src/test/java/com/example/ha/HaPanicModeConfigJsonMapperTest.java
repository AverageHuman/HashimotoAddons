package com.example.ha;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;

public final class HaPanicModeConfigJsonMapperTest {
    @Test
    public void roundTripsEnabledAndMouseBindingWithoutRuntimeState() {
        HaPanicModeConfig config = new HaPanicModeConfig();
        config.enabled = true;
        config.keyType = "mouse";
        config.keyCode = 4;
        Gson gson = new Gson();
        String json = gson.toJson(HaPanicModeConfigJsonMapper.toSaved(config));
        HaPanicModeConfig restored = new HaPanicModeConfig();
        HaPanicModeConfigJsonMapper.apply(gson.fromJson(json, SavedPanicModeConfig.class), restored);
        Assert.assertTrue(restored.enabled);
        Assert.assertEquals("mouse", restored.keyType);
        Assert.assertEquals(4, restored.keyCode);
        Assert.assertFalse(json.contains("active"));
    }

    @Test
    public void oldConfigDefaultsToDisabledAndUnbound() {
        HaPanicModeConfig config = new HaPanicModeConfig();
        HaPanicModeConfigJsonMapper.apply(null, config);
        Assert.assertFalse(config.enabled);
        Assert.assertEquals(-1, config.keyCode);
    }

    @Test
    public void invalidMouseBindingIsNormalizedBeforePolling() {
        HaPanicModeConfig config = new HaPanicModeConfig();
        SavedPanicModeConfig saved = new SavedPanicModeConfig();
        saved.keyType = "mouse";
        saved.keyCode = 100;
        HaPanicModeConfigJsonMapper.apply(saved, config);
        Assert.assertEquals("keysym", config.keyType);
        Assert.assertEquals(-1, config.keyCode);
    }
}
