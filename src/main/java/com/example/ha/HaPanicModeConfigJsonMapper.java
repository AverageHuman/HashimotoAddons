package com.example.ha;

final class HaPanicModeConfigJsonMapper {
    private HaPanicModeConfigJsonMapper() { }

    static SavedPanicModeConfig toSaved(HaPanicModeConfig config) {
        config.normalize();
        SavedPanicModeConfig saved = new SavedPanicModeConfig();
        saved.enabled = config.enabled;
        saved.keyCode = config.keyCode;
        saved.scanCode = config.scanCode;
        saved.keyType = config.keyType;
        return saved;
    }

    static void apply(SavedPanicModeConfig saved, HaPanicModeConfig config) {
        if (saved == null) return;
        config.enabled = saved.enabled;
        config.keyCode = saved.keyCode;
        config.scanCode = saved.scanCode;
        config.keyType = saved.keyType;
        config.normalize();
    }
}

final class SavedPanicModeConfig {
    boolean enabled;
    int keyCode = -1;
    int scanCode = -1;
    String keyType = "keysym";
}
