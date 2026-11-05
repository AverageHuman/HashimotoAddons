package com.example.ha;

/** No configuration snapshot is needed: suspension never changes feature settings. */
final class HaPanicModeState {
    private volatile boolean active;
    private boolean wasDown;

    boolean isActive() { return active; }

    boolean update(boolean enabled, boolean down, boolean allowed) {
        boolean trigger = enabled && allowed && down && !wasDown;
        wasDown = down;
        if (trigger) active = !active;
        return trigger;
    }

    void resume() { active = false; }
}
