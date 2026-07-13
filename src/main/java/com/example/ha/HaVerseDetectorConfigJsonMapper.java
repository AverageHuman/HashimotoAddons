package com.example.ha;

import com.google.gson.JsonObject;

final class HaVerseDetectorConfigJsonMapper {
    private HaVerseDetectorConfigJsonMapper() {
    }

    static JsonObject toJson(HaVerseDetectorConfig config, boolean dangerousFeaturesEnabled) {
        JsonObject result = new JsonObject();
        result.addProperty("enabled", config != null && config.enabled);
        if (dangerousFeaturesEnabled) {
            result.addProperty("autoThrowTrashVerseEnabled", config != null && config.autoThrowTrashVerseEnabled);
        }
        return result;
    }

    static void apply(JsonObject source, HaVerseDetectorConfig config, boolean dangerousFeaturesEnabled) {
        if (source == null || config == null) {
            return;
        }
        if (source.has("enabled")) {
            config.enabled = source.get("enabled").getAsBoolean();
        }
        if (dangerousFeaturesEnabled && source.has("autoThrowTrashVerseEnabled")) {
            config.autoThrowTrashVerseEnabled = source.get("autoThrowTrashVerseEnabled").getAsBoolean();
        }
    }
}
