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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

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

        if (HaConfig.get().extrasEnabled) {
            applyCurrentWorld(client);
        } else {
            restoreCurrentWorld(client);
        }
    }

    public static boolean tryUse(MinecraftClient client) {
        if (!canEdit(client) || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
        BlockPos pos = hit.getBlockPos();
        GhostBlock existing = find(client, pos);
        if (existing != null) {
            if (client.player != null && client.player.isSneaking() && addAirPlacement(client, pos.offset(hit.getSide()))) {
                return true;
            }

            existing.ghostStateRawId = Block.getRawIdFromState(getSelectedBlock().getDefaultState());
            applyGhostBlock(client, existing, false);
            save();
            return true;
        }

        if (client.player == null || !client.player.isSneaking()) {
            return false;
        }
        return addAirPlacement(client, pos.offset(hit.getSide()));
    }

    public static boolean tryAttack(MinecraftClient client) {
        if (!canEdit(client) || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
        GhostBlock existing = find(client, pos);
        if (existing != null) {
            remove(client, pos);
            return true;
        }

        if (client.player == null || !client.player.isSneaking()) {
            return false;
        }
        return addBarrierReplacement(client, pos);
    }

    public static boolean shouldCancelBlockBreaking(MinecraftClient client) {
        if (!canEdit(client) || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
        if (find(client, pos) != null) {
            return true;
        }

        if (client.player == null || !client.player.isSneaking() || client.world == null) {
            return false;
        }

        BlockState state = client.world.getBlockState(pos);
        return state != null && !state.isAir() && !isProtectedTarget(client, pos, state);
    }

    public static void setExtrasEnabled(boolean enabled) {
        HaConfig config = HaConfig.get();
        config.extrasEnabled = enabled;
        config.save();

        MinecraftClient client = MinecraftClient.getInstance();
        if (enabled) {
            applyCurrentWorld(client);
        } else {
            restoreCurrentWorld(client);
        }
    }

    public static Block getSelectedBlock() {
        HaConfig config = HaConfig.get();
        config.normalize();
        Block block = getBlock(config.selectedGhostBlockId);
        return isSelectableBlock(block) ? block : Blocks.GLASS;
    }

    public static String getSelectedBlockId() {
        return Registry.BLOCK.getId(getSelectedBlock()).toString();
    }

    public static String getSelectedBlockName() {
        return getSelectedBlock().getName().getString();
    }

    public static String getCurrentWorldName(MinecraftClient client) {
        if (client == null) {
            return "Unknown";
        }

        ServerInfo server = client.getCurrentServerEntry();
        if (server != null) {
            if (server.name != null && !server.name.trim().isEmpty()) {
                return server.name.trim();
            }
            if (server.address != null && !server.address.trim().isEmpty()) {
                return server.address.trim();
            }
        }
        if (client.getServer() != null && client.getServer().getSaveProperties() != null) {
            return client.getServer().getSaveProperties().getLevelName();
        }
        return "Unknown";
    }

    public static List<Block> getSelectableBlocks(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        List<Block> blocks = new ArrayList<Block>();
        for (Identifier id : Registry.BLOCK.getIds()) {
            Block block = Registry.BLOCK.get(id);
            if (!isSelectableBlock(block)) {
                continue;
            }

            String blockId = id.toString().toLowerCase();
            String blockName = block.getName().getString().toLowerCase();
            if (normalizedQuery.isEmpty() || blockId.contains(normalizedQuery) || blockName.contains(normalizedQuery)) {
                blocks.add(block);
            }
        }
        return blocks;
    }

    public static List<Block> getFavoriteBlocks() {
        HaConfig config = HaConfig.get();
        config.normalize();
        List<Block> result = new ArrayList<Block>();
        for (String id : config.favoriteGhostBlockIds) {
            Block block = getBlock(id);
            if (isSelectableBlock(block)) {
                result.add(block);
            }
        }
        return result;
    }

    public static boolean isFavorite(Block block) {
        String id = Registry.BLOCK.getId(block).toString();
        return HaConfig.get().favoriteGhostBlockIds.contains(id);
    }

    public static void toggleFavorite(Block block) {
        if (!isSelectableBlock(block)) {
            return;
        }

        HaConfig config = HaConfig.get();
        String id = Registry.BLOCK.getId(block).toString();
        if (config.favoriteGhostBlockIds.contains(id)) {
            config.favoriteGhostBlockIds.remove(id);
        } else {
            config.favoriteGhostBlockIds.add(id);
        }
        config.save();
    }

    public static void selectBlock(Block block) {
        if (!isSelectableBlock(block)) {
            return;
        }

        HaConfig config = HaConfig.get();
        config.selectedGhostBlockId = Registry.BLOCK.getId(block).toString();
        config.save();
    }

    public static boolean isSelectableBlock(Block block) {
        if (block == null || block == Blocks.AIR || block == Blocks.BARRIER || block == Blocks.STRUCTURE_VOID) {
            return false;
        }
        if (block instanceof BlockWithEntity) {
            return false;
        }

        Item item = block.asItem();
        if (item == null || item == Items.AIR) {
            return false;
        }

        String path = Registry.BLOCK.getId(block).getPath();
        return !path.contains("command_block")
            && !path.contains("structure_block")
            && !path.contains("portal")
            && !path.contains("fire");
    }

    private static boolean canEdit(MinecraftClient client) {
        return HaBuildFlags.DANGEROUS_FEATURES_ENABLED
            && client != null
            && client.world != null
            && client.player != null
            && HaConfig.get().extrasEnabled
            && HaConfig.get().ghostWallEditMode;
    }

    private static boolean addBarrierReplacement(MinecraftClient client, BlockPos pos) {
        load();
        if (client.world == null || pos == null || find(client, pos) != null) {
            return false;
        }

        BlockState originalState = client.world.getBlockState(pos);
        if (originalState == null || originalState.isAir() || isProtectedTarget(client, pos, originalState)) {
            return false;
        }

        GhostBlock block = new GhostBlock(
            getWorldKey(client),
            getDimensionKey(client),
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            Block.getRawIdFromState(originalState),
            Block.getRawIdFromState(Blocks.BARRIER.getDefaultState())
        );
        BLOCKS.add(block);
        applyGhostBlock(client, block, false);
        save();
        return true;
    }

    private static boolean addAirPlacement(MinecraftClient client, BlockPos pos) {
        load();
        if (client.world == null || pos == null || find(client, pos) != null) {
            return false;
        }

        BlockState originalState = client.world.getBlockState(pos);
        if (originalState == null || !originalState.isAir()) {
            return false;
        }

        GhostBlock block = new GhostBlock(
            getWorldKey(client),
            getDimensionKey(client),
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            Block.getRawIdFromState(Blocks.AIR.getDefaultState()),
            Block.getRawIdFromState(getSelectedBlock().getDefaultState())
        );
        BLOCKS.add(block);
        applyGhostBlock(client, block, false);
        save();
        return true;
    }

    private static boolean remove(MinecraftClient client, BlockPos pos) {
        load();
        if (client.world == null || pos == null) {
            return false;
        }

        GhostBlock removedBlock = null;
        for (Iterator<GhostBlock> iterator = BLOCKS.iterator(); iterator.hasNext();) {
            GhostBlock block = iterator.next();
            if (block.matches(client, pos)) {
                removedBlock = block;
                iterator.remove();
                break;
            }
        }
        if (removedBlock == null) {
            return false;
        }

        restoreOriginalBlock(client, removedBlock);
        save();
        return true;
    }

    private static GhostBlock find(MinecraftClient client, BlockPos pos) {
        load();
        if (client == null || pos == null) {
            return null;
        }
        for (GhostBlock block : BLOCKS) {
            if (block.matches(client, pos)) {
                return block;
            }
        }
        return null;
    }

    private static boolean isProtectedTarget(MinecraftClient client, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        Identifier id = Registry.BLOCK.getId(block);
        String path = id == null ? "" : id.getPath();
        return path.contains("chest")
            || path.contains("barrel")
            || path.contains("sign");
    }

    private static void applyCurrentWorld(MinecraftClient client) {
        if (client == null || client.world == null) {
            return;
        }

        load();
        for (GhostBlock block : BLOCKS) {
            if (block.matchesWorld(client)) {
                applyGhostBlock(client, block, true);
            }
        }
    }

    private static void restoreCurrentWorld(MinecraftClient client) {
        if (client == null || client.world == null) {
            return;
        }

        load();
        for (GhostBlock block : BLOCKS) {
            if (block.matchesWorld(client)) {
                restoreOriginalBlock(client, block);
            }
        }
    }

    private static void applyGhostBlock(MinecraftClient client, GhostBlock block, boolean captureCurrentAsOriginal) {
        if (client.world == null || block == null) {
            return;
        }

        BlockPos pos = block.toBlockPos();
        BlockState current = client.world.getBlockState(pos);
        BlockState original = Block.getStateFromRawId(block.originalStateRawId);
        BlockState ghost = Block.getStateFromRawId(block.ghostStateRawId);
        if (ghost == null || ghost.isAir()) {
            ghost = Blocks.BARRIER.getDefaultState();
        }
        if (captureCurrentAsOriginal && current != null && !isManagedVisibleState(block, current) && current != original) {
            block.originalStateRawId = Block.getRawIdFromState(current);
        }
        client.world.setBlockState(pos, ghost, 18);
    }

    private static void restoreOriginalBlock(MinecraftClient client, GhostBlock block) {
        if (client.world == null || block == null) {
            return;
        }

        BlockState original = Block.getStateFromRawId(block.originalStateRawId);
        if (original == null) {
            original = Blocks.AIR.getDefaultState();
        }
        client.world.setBlockState(block.toBlockPos(), original, 18);
    }

    private static boolean isManagedVisibleState(GhostBlock block, BlockState state) {
        return Block.getRawIdFromState(state) == block.ghostStateRawId;
    }

    private static Block getBlock(String id) {
        try {
            return Registry.BLOCK.get(new Identifier(id));
        } catch (RuntimeException ignored) {
            return Blocks.AIR;
        }
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
                        block.normalizeStateIds();
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
        int originalStateRawId = Block.getRawIdFromState(Blocks.AIR.getDefaultState());
        int ghostStateRawId = Block.getRawIdFromState(Blocks.GLASS.getDefaultState());

        GhostBlock(String worldKey, String dimensionKey, int x, int y, int z, int originalStateRawId, int ghostStateRawId) {
            this.worldKey = worldKey;
            this.dimensionKey = dimensionKey;
            this.x = x;
            this.y = y;
            this.z = z;
            this.originalStateRawId = originalStateRawId;
            this.ghostStateRawId = ghostStateRawId;
        }

        boolean matches(MinecraftClient client, BlockPos pos) {
            return matchesWorld(client) && x == pos.getX() && y == pos.getY() && z == pos.getZ();
        }

        boolean matchesWorld(MinecraftClient client) {
            return worldKey.equals(getWorldKey(client)) && dimensionKey.equals(getDimensionKey(client));
        }

        boolean isValid() {
            return worldKey != null && !worldKey.isEmpty() && dimensionKey != null && !dimensionKey.isEmpty();
        }

        void normalizeStateIds() {
            if (Block.getStateFromRawId(ghostStateRawId) == null || Block.getStateFromRawId(ghostStateRawId).isAir()) {
                ghostStateRawId = Block.getRawIdFromState(Blocks.GLASS.getDefaultState());
            }
        }

        BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }
    }
}
