package com.example.ha;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

final class HaWaypointStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STORAGE_FILE = FabricLoader.getInstance().getConfigDir().resolve("HashimotoAddons").resolve("waypoints.json");

    private HaWaypointStorage() {
    }

    static SavedWaypointState load() throws IOException {
        if (!Files.exists(STORAGE_FILE)) {
            return new SavedWaypointState();
        }

        try (Reader reader = Files.newBufferedReader(STORAGE_FILE, StandardCharsets.UTF_8)) {
            SavedWaypointState saved = GSON.fromJson(reader, SavedWaypointState.class);
            return saved == null ? new SavedWaypointState() : saved;
        }
    }

    static void save(SavedWaypointState state) throws IOException {
        Files.createDirectories(STORAGE_FILE.getParent());
        try (Writer writer = Files.newBufferedWriter(STORAGE_FILE, StandardCharsets.UTF_8)) {
            GSON.toJson(state, writer);
        }
    }
}
