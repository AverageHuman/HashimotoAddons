package com.example.ha;

import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;

public final class HaMobSpawnCoordinateOverlay {
    private HaMobSpawnCoordinateOverlay() {
    }

    public static void render(WorldRenderContext context) {
        HaMobSpawnCoordinateConfig config = HaConfig.get().mobSpawnCoordinate;
        config.normalize();
        if (!config.isActive()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        List<HaWaypointManager.WaypointEntry> waypoints = HaMobSpawnCoordinate.getWaypointsForCurrentDimension(client);
        HaWaypointOverlay.renderWaypoints(context, client, waypoints, config.renderFullBlocks);
    }
}
