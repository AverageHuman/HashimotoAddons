package com.example.ha;

public final class HaVerseDetectorConfig {
    public boolean enabled = true;
    public boolean autoThrowTrashVerseEnabled = true;
    public int autoThrowTrashVerseDelayTicks = 0;

    public void normalize() {
        autoThrowTrashVerseDelayTicks = clamp(autoThrowTrashVerseDelayTicks, 0, 72000);
    }

    public void resetDangerousState() {
        autoThrowTrashVerseEnabled = false;
        autoThrowTrashVerseDelayTicks = 0;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
