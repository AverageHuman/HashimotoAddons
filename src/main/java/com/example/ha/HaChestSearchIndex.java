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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

public final class HaChestSearchIndex {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STORAGE_FILE = FabricLoader.getInstance().getConfigDir().resolve("HashimotoAddons").resolve("chest_search.json");
    private static final HaChestSearchIndex INSTANCE = new HaChestSearchIndex();

    private final List<ChestRecord> records = new ArrayList<ChestRecord>();
    private boolean loaded;
    private int lastSyncId = -1;
    private BlockPos lastPos;
    private String lastSignature = "";
    private BlockPos pendingTargetPos;

    private HaChestSearchIndex() {
    }

    public static HaChestSearchIndex get() {
        return INSTANCE;
    }

    public void onContainerScreenOpen(MinecraftClient client) {
        pendingTargetPos = getTargetContainerPos(client);
    }

    public void tick(MinecraftClient client) {
        if (!HaConfig.get().chestSearchEnabled) {
            return;
        }
        if (!(client.currentScreen instanceof GenericContainerScreen)) {
            pendingTargetPos = getTargetContainerPos(client);
            lastSyncId = -1;
            lastPos = null;
            lastSignature = "";
            return;
        }

        GenericContainerScreenHandler handler = (GenericContainerScreenHandler) ((GenericContainerScreen) client.currentScreen).getScreenHandler();
        if (handler.syncId != lastSyncId) {
            lastSyncId = handler.syncId;
            lastPos = pendingTargetPos != null ? pendingTargetPos : getTargetContainerPos(client);
            lastSignature = "";
        } else if (lastPos == null) {
            lastPos = pendingTargetPos != null ? pendingTargetPos : getTargetContainerPos(client);
        }
        pendingTargetPos = null;

        if (lastPos == null) {
            return;
        }

        String signature = createSignature(handler);
        if (signature.equals(lastSignature)) {
            return;
        }

        lastSignature = signature;
        record(client, handler, lastPos);
    }

    public List<BlockPos> findMatchingPositions(MinecraftClient client, String query) {
        load();
        String normalizedQuery = normalize(query);
        if (client == null || client.world == null || normalizedQuery.isEmpty()) {
            return Collections.emptyList();
        }

        String worldKey = getWorldKey(client);
        List<BlockPos> matches = new ArrayList<BlockPos>();
        Set<String> seen = new HashSet<String>();
        for (ChestRecord record : records) {
            if (!worldKey.equals(record.worldKey) || record.items == null) {
                continue;
            }

            for (ChestItem item : record.items) {
                if (item != null && item.matches(normalizedQuery)) {
                    String posKey = record.x + "," + record.y + "," + record.z;
                    if (seen.add(posKey)) {
                        matches.add(new BlockPos(record.x, record.y, record.z));
                    }
                    break;
                }
            }
        }
        return matches;
    }

    public int getRecordCount() {
        load();
        return records.size();
    }

    public boolean matchesQuery(ItemStack stack, String query) {
        String normalizedQuery = normalize(query);
        if (stack == null || stack.isEmpty() || normalizedQuery.isEmpty()) {
            return false;
        }

        String name = stack.getName().getString();
        Identifier id = Registry.ITEM.getId(stack.getItem());
        String itemId = id == null ? "" : id.toString();
        return normalize(name).contains(normalizedQuery) || normalize(itemId).contains(normalizedQuery);
    }

    public void clear() {
        load();
        records.clear();
        save();
    }

    private void record(MinecraftClient client, GenericContainerScreenHandler handler, BlockPos pos) {
        load();
        ChestRecord record = new ChestRecord();
        record.worldKey = getWorldKey(client);
        record.x = pos.getX();
        record.y = pos.getY();
        record.z = pos.getZ();
        record.items = new ArrayList<ChestItem>();

        int chestSlots = handler.getRows() * 9;
        int limit = Math.min(chestSlots, handler.slots.size());
        for (int i = 0; i < limit; i++) {
            Slot slot = handler.slots.get(i);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                continue;
            }

            ChestItem item = new ChestItem();
            item.name = stack.getName().getString();
            Identifier id = Registry.ITEM.getId(stack.getItem());
            item.id = id == null ? "" : id.toString();
            item.count = stack.getCount();
            record.items.add(item);
        }

        replace(record);
        save();
    }

    private void replace(ChestRecord newRecord) {
        for (int i = 0; i < records.size(); i++) {
            ChestRecord existing = records.get(i);
            if (existing.x == newRecord.x
                && existing.y == newRecord.y
                && existing.z == newRecord.z
                && newRecord.worldKey.equals(existing.worldKey)) {
                records.set(i, newRecord);
                return;
            }
        }
        records.add(newRecord);
    }

    private static BlockPos getTargetContainerPos(MinecraftClient client) {
        if (client == null || client.world == null || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
        Block block = client.world.getBlockState(pos).getBlock();
        if (block instanceof ChestBlock || block instanceof TrappedChestBlock || block instanceof BarrelBlock) {
            return pos.toImmutable();
        }
        return null;
    }

    private static String createSignature(GenericContainerScreenHandler handler) {
        StringBuilder result = new StringBuilder();
        int chestSlots = handler.getRows() * 9;
        int limit = Math.min(chestSlots, handler.slots.size());
        for (int i = 0; i < limit; i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (!stack.isEmpty()) {
                result.append(i).append(':').append(Registry.ITEM.getId(stack.getItem())).append(':').append(stack.getCount()).append(';');
            }
        }
        return result.toString();
    }

    private static String getWorldKey(MinecraftClient client) {
        String server = client.getCurrentServerEntry() == null ? "singleplayer" : client.getCurrentServerEntry().address;
        String dimension = client.world.getRegistryKey().getValue().toString();
        return server + "|" + dimension;
    }

    private void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.exists(STORAGE_FILE)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(STORAGE_FILE, StandardCharsets.UTF_8)) {
            SavedChestSearch saved = GSON.fromJson(reader, SavedChestSearch.class);
            if (saved != null && saved.records != null) {
                records.clear();
                records.addAll(saved.records);
            }
        } catch (IOException exception) {
            reportLoadFailure(exception);
        } catch (RuntimeException exception) {
            reportLoadFailure(exception);
        }
    }

    private void save() {
        final SavedChestSearch saved = new SavedChestSearch();
        saved.records = new ArrayList<ChestRecord>(records);
        HaAsyncFileWriter.submit(STORAGE_FILE, new HaAsyncFileWriter.WriteOperation() {
            @Override
            public void write() throws IOException {
                Files.createDirectories(STORAGE_FILE.getParent());
                try (Writer writer = Files.newBufferedWriter(STORAGE_FILE, StandardCharsets.UTF_8)) {
                    GSON.toJson(saved, writer);
                }
            }
        });
    }

    private static void reportLoadFailure(Exception exception) {
        System.err.println("[HashimotoAddons] Failed to load chest_search.json: " + exception.getMessage());
        exception.printStackTrace(System.err);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class SavedChestSearch {
        List<ChestRecord> records = new ArrayList<ChestRecord>();
    }

    private static final class ChestRecord {
        String worldKey = "";
        int x;
        int y;
        int z;
        List<ChestItem> items = new ArrayList<ChestItem>();
    }

    private static final class ChestItem {
        String name = "";
        String id = "";
        int count;

        boolean matches(String normalizedQuery) {
            return normalize(name).contains(normalizedQuery) || normalize(id).contains(normalizedQuery);
        }
    }
}
