package com.example.ha;

final class HaMobSpawnCoordinateConfigJsonMapper {
    private HaMobSpawnCoordinateConfigJsonMapper() {
    }

    static SavedMobSpawnCoordinateConfig toSaved(HaMobSpawnCoordinateConfig config) {
        SavedMobSpawnCoordinateConfig result = new SavedMobSpawnCoordinateConfig();
        if (config == null) {
            return result;
        }
        config.normalize();
        result.enabled = config.enabled;
        result.targetName = config.targetName;
        result.colorSlotIndex = config.colorSlotIndex;
        result.renderFullBlocks = config.renderFullBlocks;
        return result;
    }

    static void apply(SavedMobSpawnCoordinateConfig source, HaMobSpawnCoordinateConfig config) {
        if (source == null || config == null) {
            return;
        }
        config.enabled = source.enabled;
        config.targetName = source.targetName;
        config.colorSlotIndex = source.colorSlotIndex;
        config.renderFullBlocks = source.renderFullBlocks;
        config.normalize();
    }
}
