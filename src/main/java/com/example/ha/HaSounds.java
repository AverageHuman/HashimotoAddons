package com.example.ha;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public final class HaSounds {
    public static final SoundEvent RITUAL_TIMER_READY = register("ritual_timer_ready");

    private HaSounds() {
    }

    public static void register() {
        // Loading this class registers all sound events through their static initializers.
    }

    private static SoundEvent register(String path) {
        Identifier id = new Identifier("ha", path);
        return Registry.register(Registry.SOUND_EVENT, id, new SoundEvent(id));
    }
}
