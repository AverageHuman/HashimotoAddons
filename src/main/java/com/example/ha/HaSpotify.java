package com.example.ha;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
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
    private static final long COMMAND_TIMEOUT_MILLIS = 3000L;
    private static final String PROCESS_NAME = "Spotify";
    private static final String CHROME_AUMID_TOKEN = "chrome";
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
    private static volatile List<String> lastChromeDebugLines = new ArrayList<String>();
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
        TrackInfo spotifyTrack = detectSpotifyTrack();
        if (spotifyTrack.isPlaying() || !HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            return spotifyTrack;
        }

        TrackInfo chromeTrack = detectChromeTrack();
        if (chromeTrack != null) {
            return chromeTrack;
        }
        return spotifyTrack;
    }

    private static TrackInfo detectSpotifyTrack() {
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

    private static TrackInfo detectChromeTrack() {
        List<String> lines = runCommand(StandardCharsets.UTF_8, "powershell.exe", "-NoProfile", "-Command", buildChromeMediaScript());
        lastChromeDebugLines = new ArrayList<String>(lines);
        for (String line : lines) {
            TrackInfo info = parseChromeMediaLine(line);
            if (info != null) {
                return info;
            }
        }
        return null;
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

    private static TrackInfo parseChromeMediaLine(String line) {
        if (line == null || !line.startsWith("__HA_CHROME__\t")) {
            return null;
        }

        String[] fields = line.split("\t", 3);
        if (fields.length < 3) {
            return null;
        }

        String artist = normalizeWindowTitle(fields[1]);
        String title = normalizeWindowTitle(fields[2]);
        if (artist.isEmpty() || title.isEmpty()) {
            return null;
        }
        return new TrackInfo(artist, title, TrackState.PLAYING, TrackSource.CHROME);
    }

    private static String buildChromeMediaScript() {
        return "$ErrorActionPreference = 'SilentlyContinue'; "
            + "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; "
            + "try { "
            + "Add-Type -AssemblyName System.Runtime.WindowsRuntime; "
            + "[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime] | Out-Null; "
            + "[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionPlaybackStatus, Windows.Media.Control, ContentType=WindowsRuntime] | Out-Null; "
            + "function haAwait($op, [Type]$resultType) { "
            + "$method = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.IsGenericMethodDefinition -and $_.GetParameters().Length -eq 1 } | Select-Object -First 1; "
            + "if ($null -eq $method) { Write-Output '__HA_CHROME_ERROR__`tAS_TASK_MISSING'; return $null }; "
            + "$generic = $method.MakeGenericMethod($resultType); "
            + "$task = $generic.Invoke($null, @($op)); "
            + "return $task.GetAwaiter().GetResult(); "
            + "}; "
            + "$manager = haAwait ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]); "
            + "if ($null -eq $manager) { Write-Output '__HA_CHROME_ERROR__`tNO_MANAGER'; exit }; "
            + "foreach ($session in $manager.GetSessions()) { "
            + "$source = $session.SourceAppUserModelId; "
            + "$playbackInfo = $session.GetPlaybackInfo(); "
            + "$status = if ($null -eq $playbackInfo) { 'Unknown' } else { $playbackInfo.PlaybackStatus.ToString() }; "
            + "$op = $session.TryGetMediaPropertiesAsync(); "
            + "$resultType = $op.GetType().GenericTypeArguments[0]; "
            + "$props = haAwait $op $resultType; "
            + "$artist = [string]$props.Artist; "
            + "$title = [string]$props.Title; "
            + "$artist = $artist.Replace(\"`t\", ' ').Replace(\"`r\", ' ').Replace(\"`n\", ' '); "
            + "$title = $title.Replace(\"`t\", ' ').Replace(\"`r\", ' ').Replace(\"`n\", ' '); "
            + "Write-Output ('__HA_CHROME_SESSION__' + \"`t\" + $source + \"`t\" + $status + \"`t\" + $artist + \"`t\" + $title); "
            + "if ([string]::IsNullOrWhiteSpace($source) -or $source.ToLowerInvariant().IndexOf('" + CHROME_AUMID_TOKEN + "') -lt 0) { continue }; "
            + "if ($null -eq $playbackInfo -or $playbackInfo.PlaybackStatus -ne [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionPlaybackStatus]::Playing) { continue }; "
            + "if ([string]::IsNullOrWhiteSpace($artist) -or [string]::IsNullOrWhiteSpace($title)) { continue }; "
            + "Write-Output ('__HA_CHROME__' + \"`t\" + $artist + \"`t\" + $title); "
            + "} "
            + "} catch { Write-Output ('__HA_CHROME_ERROR__`t' + $_.Exception.Message.Replace(\"`t\", ' ').Replace(\"`r\", ' ').Replace(\"`n\", ' ')) }";
    }

    public static String getDebugSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append("Current track: ");
        builder.append(currentTrack == null ? "null" : currentTrack.getFullText());
        builder.append('\n');
        builder.append("Variant: ");
        builder.append(HaBuildFlags.VARIANT);
        builder.append('\n');
        builder.append("Chrome debug lines:");
        List<String> lines = lastChromeDebugLines;
        if (lines == null || lines.isEmpty()) {
            builder.append("\n(none)");
        } else {
            for (String line : lines) {
                builder.append('\n').append(line);
            }
        }
        return builder.toString();
    }

    public static String getChatDebugSummary() {
        int sessionCount = 0;
        int matchedCount = 0;
        int errorCount = 0;
        List<String> lines = lastChromeDebugLines;
        if (lines != null) {
            for (String line : lines) {
                if (line.startsWith("__HA_CHROME_SESSION__")) {
                    sessionCount++;
                } else if (line.startsWith("__HA_CHROME__")) {
                    matchedCount++;
                } else if (line.startsWith("__HA_CHROME_ERROR__")) {
                    errorCount++;
                }
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("track=");
        builder.append(currentTrack == null ? "null" : summarizeTrack(currentTrack));
        builder.append(" variant=");
        builder.append(HaBuildFlags.VARIANT);
        builder.append(" sessions=");
        builder.append(sessionCount);
        builder.append(" matched=");
        builder.append(matchedCount);
        builder.append(" errors=");
        builder.append(errorCount);
        if (errorCount > 0 && lines != null) {
            for (String line : lines) {
                if (line.startsWith("__HA_CHROME_ERROR__")) {
                    builder.append(" firstError=");
                    builder.append(sanitizeForChat(line.substring("__HA_CHROME_ERROR__".length())));
                    break;
                }
            }
        }
        return builder.toString();
    }

    public static boolean copyDebugSummaryToClipboard() {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(getDebugSummary()), null);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String summarizeTrack(TrackInfo track) {
        if (track == null) {
            return "null";
        }
        if (!track.isPlaying()) {
            return sanitizeForChat(track.title);
        }
        return sanitizeForChat(track.getPrefixText() + track.artist + " - " + track.title);
    }

    private static String sanitizeForChat(String value) {
        String normalized = normalizeWindowTitle(value);
        if (normalized.isEmpty()) {
            return "(empty)";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch >= 32 && ch <= 126) {
                builder.append(ch);
            } else {
                builder.append('?');
            }
        }
        String text = builder.toString();
        if (text.length() > 120) {
            return text.substring(0, 120) + "...";
        }
        return text;
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
        final TrackSource source;

        TrackInfo(String artist, String title) {
            this(artist, title, TrackState.PLAYING, TrackSource.SPOTIFY);
        }

        private TrackInfo(String artist, String title, TrackState state, TrackSource source) {
            this.artist = sanitize(artist);
            this.title = sanitize(title);
            this.state = state == null ? TrackState.PLAYING : state;
            this.source = source == null ? TrackSource.SPOTIFY : source;
        }

        String getArtistSegment() {
            if (state != TrackState.PLAYING) {
                return "";
            }
            if (source == TrackSource.CHROME) {
                return artist + " ";
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
            return getPrefixText() + getTrackInfoText();
        }

        boolean isStatusText() {
            return state != TrackState.PLAYING;
        }

        boolean isPlaying() {
            return state == TrackState.PLAYING;
        }

        String getPrefixText() {
            return source == TrackSource.CHROME ? "Google Chrome > " : "Spotify > ";
        }

        static TrackInfo special(String text, TrackState state) {
            return new TrackInfo("", text, state, TrackSource.SPOTIFY);
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

    enum TrackSource {
        SPOTIFY,
        CHROME
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
