package com.example.ha;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;

public final class HaSpotifyOverlay {
    private static final String PREFIX_TEXT = "Spotify > ";
    private static final String WIDTH_SAMPLE = "Spotify > 123456789012345678901234567890";
    private static final int PREFIX_COLOR = 0x55FF55;
    private static final int ARTIST_COLOR = 0x55FFFF;
    private static final int SEPARATOR_COLOR = 0xAAAAAA;
    private static final int TITLE_COLOR = 0xFFAA00;
    private static final int STATUS_COLOR = 0xFF5555;
    private static final int PANEL_HEIGHT = 10;
    private static final float SCROLL_PIXELS_PER_SECOND = 24.0F;
    private static final long PAUSE_AT_START_MILLIS = 1000L;
    private static final long PAUSE_AT_END_MILLIS = 1000L;

    private HaSpotifyOverlay() {
    }

    public static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        HaConfig config = HaConfig.get();
        if (client == null || HaHudVisibility.shouldHideHashimotoHud(client) || !config.spotifyEnabled || !HaSpotify.hasTrack()) {
            return;
        }
        drawTrack(matrices, config.spotifyOverlayX, config.spotifyOverlayY, HaSpotify.getCurrentTrack());
    }

    public static void drawPreview(MatrixStack matrices, int x, int y, boolean selected) {
        drawTrack(matrices, x, y, HaSpotify.getPreviewTrack());
        if (selected) {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, "\u25c0", x - 8, y + 1, 0xFFFFFF);
        }
    }

    public static int getPanelWidth(MinecraftClient client) {
        HaSpotify.TrackInfo track = HaSpotify.hasTrack() ? HaSpotify.getCurrentTrack() : HaSpotify.getPreviewTrack();
        return Math.min(getMaxTotalWidth(client), client.textRenderer.getWidth(track.getFullText()));
    }

    public static int getPanelHeight() {
        return PANEL_HEIGHT;
    }

    private static void drawTrack(MatrixStack matrices, int x, int y, HaSpotify.TrackInfo track) {
        if (track == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int prefixWidth = client.textRenderer.getWidth(PREFIX_TEXT);
        int maxTotalWidth = getMaxTotalWidth(client);
        int trackWindowWidth = Math.max(0, maxTotalWidth - prefixWidth);
        int trackInfoWidth = getTrackInfoWidth(client, track);
        int overflowWidth = Math.max(0, trackInfoWidth - trackWindowWidth);
        int scrollOffset = overflowWidth > 0 ? getScrollOffsetPixels(overflowWidth, Util.getMeasuringTimeMs()) : 0;

        client.textRenderer.drawWithShadow(matrices, PREFIX_TEXT, x, y, PREFIX_COLOR);
        if (trackWindowWidth <= 0) {
            return;
        }

        if (overflowWidth > 0) {
            enableScissor(client, x + prefixWidth, y, trackWindowWidth, PANEL_HEIGHT);
        }
        drawTrackInfo(matrices, x + prefixWidth - scrollOffset, y, track);
        if (overflowWidth > 0) {
            RenderSystem.disableScissor();
        }
    }

    private static void drawTrackInfo(MatrixStack matrices, int x, int y, HaSpotify.TrackInfo track) {
        MinecraftClient client = MinecraftClient.getInstance();
        String artistSegment = track.getArtistSegment();
        String separatorSegment = track.getSeparatorSegment();

        int drawX = x;
        if (track.isStatusText()) {
            client.textRenderer.drawWithShadow(matrices, track.title, drawX, y, STATUS_COLOR);
            return;
        }
        client.textRenderer.drawWithShadow(matrices, artistSegment, drawX, y, ARTIST_COLOR);
        drawX += client.textRenderer.getWidth(artistSegment);
        client.textRenderer.drawWithShadow(matrices, separatorSegment, drawX, y, SEPARATOR_COLOR);
        drawX += client.textRenderer.getWidth(separatorSegment);
        client.textRenderer.drawWithShadow(matrices, track.title, drawX, y, TITLE_COLOR);
    }

    private static int getTrackInfoWidth(MinecraftClient client, HaSpotify.TrackInfo track) {
        return client.textRenderer.getWidth(track.getTrackInfoText());
    }

    private static int getMaxTotalWidth(MinecraftClient client) {
        return client.textRenderer.getWidth(WIDTH_SAMPLE);
    }

    private static int getScrollOffsetPixels(int overflowWidth, long nowMillis) {
        long travelDurationMillis = Math.max(1L, (long) Math.ceil((overflowWidth / SCROLL_PIXELS_PER_SECOND) * 1000.0D));
        long cycleDurationMillis = PAUSE_AT_START_MILLIS + travelDurationMillis + PAUSE_AT_END_MILLIS;
        long cyclePositionMillis = nowMillis % cycleDurationMillis;
        if (cyclePositionMillis < PAUSE_AT_START_MILLIS) {
            return 0;
        }
        cyclePositionMillis -= PAUSE_AT_START_MILLIS;
        if (cyclePositionMillis >= travelDurationMillis) {
            return overflowWidth;
        }
        return Math.min(overflowWidth, (int) Math.floor((cyclePositionMillis / 1000.0D) * SCROLL_PIXELS_PER_SECOND));
    }

    private static void enableScissor(MinecraftClient client, int x, int y, int width, int height) {
        Window window = client.getWindow();
        double scale = window.getScaleFactor();
        int scissorX = (int) Math.floor(x * scale);
        int scissorY = (int) Math.floor(window.getFramebufferHeight() - (y + height) * scale);
        int scissorWidth = Math.max(0, (int) Math.ceil(width * scale));
        int scissorHeight = Math.max(0, (int) Math.ceil(height * scale));
        RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }
}
