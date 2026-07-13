package com.example.ha;

public final class HaVerseDetectorConfig {
    public boolean enabled = true;
    public boolean autoThrowTrashVerseEnabled = true;

    public void normalize() {
        // Boolean settings need no additional normalization.
    }

    public void resetDangerousState() {
        autoThrowTrashVerseEnabled = false;
    }
}
