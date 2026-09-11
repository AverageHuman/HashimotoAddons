package com.example.ha;

final class HaCommandKeyConfigJsonMapper {
    private HaCommandKeyConfigJsonMapper() {
    }

    static SavedCommandKeyConfig toSaved(HaCommandKeyConfig config) {
        SavedCommandKeyConfig result = new SavedCommandKeyConfig();
        if (config == null) {
            return result;
        }
        config.normalize();
        result.enabled = config.enabled;
        result.command = config.command;
        result.keyCode = config.keyCode;
        result.scanCode = config.scanCode;
        result.keyType = config.keyType;
        return result;
    }

    static void apply(SavedCommandKeyConfig source, HaCommandKeyConfig config) {
        if (source == null || config == null) {
            return;
        }
        config.enabled = source.enabled;
        config.command = source.command;
        config.keyCode = source.keyCode;
        config.scanCode = source.scanCode;
        config.keyType = source.keyType;
        config.normalize();
    }
}
