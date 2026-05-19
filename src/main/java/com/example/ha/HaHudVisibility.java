package com.example.ha;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class HaHudVisibility {
    private HaHudVisibility() {
    }

    public static boolean shouldHideHashimotoHud(MinecraftClient client) {
        if (client == null) {
            return true;
        }

        Screen screen = client.currentScreen;
        return screen instanceof HaConfigScreen
            || screen instanceof HaDangerousFeaturesScreen
            || screen instanceof HaMacroStatusOverlayScreen
            || screen instanceof HaExtrasScreen
            || screen instanceof HaBlockGalleryScreen
            || screen instanceof HaExtrasOverlayScreen
            || screen instanceof HaAutoHealScreen
            || screen instanceof HaMacroListScreen
            || screen instanceof HaMacroEditScreen
            || screen instanceof HaHpAlertListScreen
            || screen instanceof HaHpAlertEditScreen
            || screen instanceof HaManaAlertListScreen
            || screen instanceof HaManaAlertEditScreen
            || screen instanceof HaChatFilterListScreen
            || screen instanceof HaChatFilterManageScreen
            || screen instanceof HaChatFilterEditScreen
            || screen instanceof HaCameraScreen
            || screen instanceof HaChunkChestScreen
            || screen instanceof HaChunkChestOverlayScreen
            || screen instanceof HaChestSearchScreen
            || screen instanceof HaDropTrackerScreen
            || screen instanceof HaDropTrackerRegisteredListScreen
            || screen instanceof HaDropTrackerRegisteredEditScreen
            || screen instanceof HaDropTrackerOverlayScreen
            || screen instanceof HaExpTrackerScreen
            || screen instanceof HaExpTrackerOverlayScreen;
    }
}
