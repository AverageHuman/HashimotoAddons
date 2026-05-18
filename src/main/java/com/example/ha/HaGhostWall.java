package com.example.ha;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public final class HaGhostWall {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STORAGE_FILE = FabricLoader.getInstance().getConfigDir().resolve("HashimotoAddons").resolve("ghost_walls.json");
    private static final List<GhostBlock> BLOCKS = new ArrayList<GhostBlock>();
    private static boolean loaded;
    private static int applyCooldownTicks;

    private HaGhostWall() {
    }

    public static void tick(MinecraftClient client) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED || client == null || client.world == null) {
            return;
        }

        load();
        if (applyCooldownTicks > 0) {
            applyCooldownTicks--;
            return;
        }
        applyCooldownTicks = 10;

        String worldKey = getWorldKey(client);
        String dimensionKey = getDimensionKey(client);
        for (GhostBlock block : BLOCKS) {
            if (block.matches(worldKey, dimensionKey)) {
                applyGhostBlock(client, block.toBlockPos());
            }
        }
    }

    public static boolean tryPlaceFromUse(MinecraftClient client) {
        if (!canEdit(client) || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        if (client.player == null || !client.player.isSneaking()) {
            return false;
        }

        BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
        BlockPos placePos = hit.getBlockPos().offset(hit.getSide());
        return add(client, placePos);
    }

    public static boolean tryBreakFromAttack(MinecraftClient client) {
        if (!canEdit(client) || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
        return remove(client, pos);
    }

    private static boolean canEdit(MinecraftClient client) {
        return HaBuildFlags.DANGEROUS_FEATURES_ENABLED
            && client != null
            && client.world != null
            && client.player != null
            && HaConfig.get().ghostWallEditMode;
    }

    private static boolean add(MinecraftClient client, BlockPos pos) {
        load();
        if (client.world == null || pos == null || contains(client, pos)) {
            return false;
        }
        if (!canPlaceAt(client, pos)) {
            return false;
        }

        BLOCKS.add(new GhostBlock(getWorldKey(client), getDimensionKey(client), pos.getX(), pos.getY(), pos.getZ()));
        applyGhostBlock(client, pos);
        save();
        return true;
    }

    private static boolean remove(MinecraftClient client, BlockPos pos) {
        load();
        if (client.world == null || pos == null) {
            return false;
        }

        String worldKey = getWorldKey(client);
        String dimensionKey = getDimensionKey(client);
        boolean removed = false;
        for (Iterator<GhostBlock> iterator = BLOCKS.iterator(); iterator.hasNext();) {
            GhostBlock block = iterator.next();
            if (block.matches(worldKey, dimensionKey) && block.x == pos.getX() && block.y == pos.getY() && block.z == pos.getZ()) {
                iterator.remove();
                removed = true;
            }
        }
        if (!removed) {
            return false;
        }

        client.world.setBlockState(pos, Blocks.AIR.getDefaultState(), 18);
        save();
        return true;
    }

    private static boolean contains(MinecraftClient client, BlockPos pos) {
        String worldKey = getWorldKey(client);
        String dimensionKey = getDimensionKey(client);
        for (GhostBlock block : BLOCKS) {
            if (block.matches(worldKey, dimensionKey) && block.x == pos.getX() && block.y == pos.getY() && block.z == pos.getZ()) {
                return true;
            }
        }
        return false;
    }

    private static boolean canPlaceAt(MinecraftClient client, BlockPos pos) {
        BlockState state = client.world.getBlockState(pos);
        return state == null || state.isAir();
    }

    private static void applyGhostBlock(MinecraftClient client, BlockPos pos) {
        if (client.world == null || pos == null) {
            return;
        }

        BlockState current = client.world.getBlockState(pos);
        if (current != null && !current.isAir() && current.getBlock() != Blocks.GLASS) {
            return;
        }
        client.world.setBlockState(pos, Blocks.GLASS.getDefaultState(), 18);
    }

    private static String getWorldKey(MinecraftClient client) {
        ServerInfo server = client.getCurrentServerEntry();
        if (server != null && server.address != null && !server.address.trim().isEmpty()) {
            return "server:" + server.address.trim();
        }
        if (client.getServer() != null && client.getServer().getSaveProperties() != null) {
            return "singleplayer:" + client.getServer().getSaveProperties().getLevelName();
        }
        return "unknown";
    }

    private static String getDimensionKey(MinecraftClient client) {
        if (client.world == null || client.world.getRegistryKey() == null || client.world.getRegistryKey().getValue() == null) {
            return "unknown";
        }
        return client.world.getRegistryKey().getValue().toString();
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.exists(STORAGE_FILE)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(STORAGE_FILE, StandardCharsets.UTF_8)) {
            SavedGhostWalls saved = GSON.fromJson(reader, SavedGhostWalls.class);
            if (saved != null && saved.blocks != null) {
                BLOCKS.clear();
                for (GhostBlock block : saved.blocks) {
                    if (block != null && block.isValid()) {
                        BLOCKS.add(block);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void save() {
        try {
            Files.createDirectories(STORAGE_FILE.getParent());
            SavedGhostWalls saved = new SavedGhostWalls();
            saved.blocks.addAll(BLOCKS);
            try (Writer writer = Files.newBufferedWriter(STORAGE_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(saved, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static final class SavedGhostWalls {
        List<GhostBlock> blocks = new ArrayList<GhostBlock>();
    }

    private static final class GhostBlock {
        String worldKey = "";
        String dimensionKey = "";
        int x;
        int y;
        int z;

        GhostBlock(String worldKey, String dimensionKey, int x, int y, int z) {
            this.worldKey = worldKey;
            this.dimensionKey = dimensionKey;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        boolean matches(String worldKey, String dimensionKey) {
            return this.worldKey.equals(worldKey) && this.dimensionKey.equals(dimensionKey);
        }

        boolean isValid() {
            return worldKey != null && !worldKey.isEmpty() && dimensionKey != null && !dimensionKey.isEmpty();
        }

        BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }
    }
}
