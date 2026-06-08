package com.example.ha;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.MinecraftClient;

public final class HaSpotify {
    private static final int POLL_INTERVAL_TICKS = 20;
    private static final long COMMAND_TIMEOUT_MILLIS = 1500L;
    private static final String PROCESS_NAME = "Spotify";
    private static final ExecutorService POLLER = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "ha-spotify-poller");
            thread.setDaemon(true);
            return thread;
        }
    });
    private static final TrackInfo PREVIEW_TRACK = new TrackInfo("Artist Name", "A Very Long Song Title");
    private static final TrackInfo PAUSED_TRACK = TrackInfo.special("Paused", TrackState.PAUSED);
    private static final TrackInfo NOT_OPEN_TRACK = TrackInfo.special("None(maybe not open)", TrackState.NOT_OPEN);
    private static volatile TrackInfo currentTrack = NOT_OPEN_TRACK;
    private static volatile boolean pollInFlight;
    private static int pollCooldownTicks;

    private HaSpotify() {
    }

    public static void tick(MinecraftClient client, HaConfig config) {
        if (client == null || config == null || !config.spotifyEnabled || !isWindows()) {
            currentTrack = NOT_OPEN_TRACK;
            pollCooldownTicks = 0;
            return;
        }

        if (pollCooldownTicks > 0) {
            pollCooldownTicks--;
            return;
        }
        if (pollInFlight) {
            return;
        }

        pollCooldownTicks = POLL_INTERVAL_TICKS;
        pollInFlight = true;
        POLLER.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    currentTrack = detectCurrentTrack();
                } finally {
                    pollInFlight = false;
                }
            }
        });
    }

    public static boolean hasTrack() {
        return currentTrack != null;
    }

    public static TrackInfo getCurrentTrack() {
        return currentTrack;
    }

    public static TrackInfo getPreviewTrack() {
        return PREVIEW_TRACK;
    }

    private static TrackInfo detectCurrentTrack() {
        WindowQueryResult result = queryWindowTitlesWithPowerShell();
        if (!result.hasProcess) {
            result = queryWindowTitlesWithTaskList();
        }
        if (!result.hasProcess) {
            return NOT_OPEN_TRACK;
        }
        for (String title : result.titles) {
            TrackInfo info = parseTrackInfo(title);
            if (info != null) {
                return info;
            }
        }
        return PAUSED_TRACK;
    }

    private static WindowQueryResult queryWindowTitlesWithPowerShell() {
        String script = "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; "
            + "$processes = Get-Process " + PROCESS_NAME + " -ErrorAction SilentlyContinue; "
            + "if (-not $processes) { Write-Output '__HA_NO_PROCESS__'; exit }; "
            + "Write-Output '__HA_PROCESS_FOUND__'; "
            + "$processes | Where-Object { $_.MainWindowTitle -and $_.MainWindowTitle -ne 'N/A' } "
            + "| Select-Object -ExpandProperty MainWindowTitle";
        List<String> lines = runCommand(StandardCharsets.UTF_8, "powershell.exe", "-NoProfile", "-Command", script);
        return WindowQueryResult.fromCommandLines(lines, "__HA_PROCESS_FOUND__", "__HA_NO_PROCESS__");
    }

    private static WindowQueryResult queryWindowTitlesWithTaskList() {
        List<String> lines = runCommand(Charset.defaultCharset(), "tasklist", "/v", "/fo", "csv", "/fi", "imagename eq spotify.exe");
        List<String> titles = new ArrayList<String>();
        boolean hasProcess = false;
        for (String line : lines) {
            List<String> fields = parseCsvLine(line);
            if (fields.size() < 9) {
                continue;
            }
            if (!"spotify.exe".equalsIgnoreCase(fields.get(0).trim())) {
                continue;
            }
            hasProcess = true;
            String title = fields.get(fields.size() - 1).trim();
            if (!title.isEmpty() && !"N/A".equalsIgnoreCase(title)) {
                titles.add(title);
            }
        }
        return new WindowQueryResult(hasProcess, titles);
    }

    private static List<String> runCommand(Charset charset, String... command) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(COMMAND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ArrayList<String>();
            }
            return readLines(process.getInputStream(), charset);
        } catch (IOException ignored) {
            return new ArrayList<String>();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return new ArrayList<String>();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static List<String> readLines(InputStream inputStream, Charset charset) throws IOException {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset));
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private static TrackInfo parseTrackInfo(String rawTitle) {
        String title = normalizeWindowTitle(rawTitle);
        if (title.isEmpty() || isIgnoredWindowTitle(title)) {
            return null;
        }

        int separatorIndex = title.indexOf(" - ");
        if (separatorIndex <= 0 || separatorIndex >= title.length() - 3) {
            return null;
        }

        String artist = title.substring(0, separatorIndex).trim();
        String songTitle = title.substring(separatorIndex + 3).trim();
        if (artist.isEmpty() || songTitle.isEmpty()) {
            return null;
        }
        return new TrackInfo(artist, songTitle);
    }

    private static String normalizeWindowTitle(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.replace('\uFEFF', ' ')
            .replace('\u200E', ' ')
            .replace('\u200F', ' ')
            .replace('\t', ' ')
            .replace('\r', ' ')
            .replace('\n', ' ')
            .trim();
        while (normalized.contains("  ")) {
            normalized = normalized.replace("  ", " ");
        }
        return normalized;
    }

    private static boolean isIgnoredWindowTitle(String title) {
        String normalized = title.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty()
            || "n/a".equals(normalized)
            || "spotify".equals(normalized)
            || "spotify free".equals(normalized)
            || "spotify premium".equals(normalized)
            || "advertisement".equals(normalized)
            || "advertisement - spotify".equals(normalized);
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name", "");
        return osName != null && osName.toLowerCase(Locale.ROOT).contains("win");
    }

    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<String>();
        if (line == null || line.isEmpty()) {
            return fields;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    public static final class TrackInfo {
        final String artist;
        final String title;
        final TrackState state;

        TrackInfo(String artist, String title) {
            this(artist, title, TrackState.PLAYING);
        }

        private TrackInfo(String artist, String title, TrackState state) {
            this.artist = sanitize(artist);
            this.title = sanitize(title);
            this.state = state == null ? TrackState.PLAYING : state;
        }

        String getArtistSegment() {
            if (state != TrackState.PLAYING) {
                return "";
            }
            return "[" + artist + "] ";
        }

        String getSeparatorSegment() {
            if (state != TrackState.PLAYING) {
                return "";
            }
            return "- ";
        }

        String getTrackInfoText() {
            if (state != TrackState.PLAYING) {
                return title;
            }
            return getArtistSegment() + getSeparatorSegment() + title;
        }

        String getFullText() {
            return "Spotify > " + getTrackInfoText();
        }

        boolean isStatusText() {
            return state != TrackState.PLAYING;
        }

        static TrackInfo special(String text, TrackState state) {
            return new TrackInfo("", text, state);
        }

        private static String sanitize(String value) {
            String normalized = normalizeWindowTitle(value);
            return normalized.isEmpty() ? "Unknown" : normalized;
        }
    }

    private enum TrackState {
        PLAYING,
        PAUSED,
        NOT_OPEN
    }

    private static final class WindowQueryResult {
        final boolean hasProcess;
        final List<String> titles;

        WindowQueryResult(boolean hasProcess, List<String> titles) {
            this.hasProcess = hasProcess;
            this.titles = titles == null ? new ArrayList<String>() : titles;
        }

        static WindowQueryResult fromCommandLines(List<String> lines, String foundMarker, String notFoundMarker) {
            boolean hasProcess = false;
            List<String> titles = new ArrayList<String>();
            for (String line : lines) {
                if (foundMarker.equals(line)) {
                    hasProcess = true;
                } else if (notFoundMarker.equals(line)) {
                    hasProcess = false;
                } else if (!line.isEmpty()) {
                    titles.add(line);
                }
            }
            return new WindowQueryResult(hasProcess, titles);
        }
    }
}
