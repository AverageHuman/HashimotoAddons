package com.example.ha;

import com.google.gson.JsonObject;

final class HaItemContractNotifierConfigJsonMapper {
    private HaItemContractNotifierConfigJsonMapper() {
    }

    static JsonObject toJson(HaItemContractNotifierConfig config) {
        JsonObject result = new JsonObject();
        result.addProperty("enabled", config != null && config.enabled);
        return result;
    }

    static void apply(JsonObject source, HaItemContractNotifierConfig config) {
        if (source == null || config == null) {
            return;
        }
        if (source.has("enabled")) {
            config.enabled = source.get("enabled").getAsBoolean();
        }
    }
}
