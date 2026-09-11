package com.example.ha;

/** Persisted settings for the Safe/Full Mob Spawn Coordinate feature. */
public final class HaMobSpawnCoordinateConfig {
    public boolean enabled = false;
    public String targetName = "";
    public int colorSlotIndex = 0;
    public boolean renderFullBlocks = false;

    public void normalize() {
        targetName = targetName == null ? "" : targetName.trim();
        colorSlotIndex = Math.max(0, Math.min(3, colorSlotIndex));
    }

    public boolean isActive() {
        return enabled && !targetName.isEmpty();
    }
}
