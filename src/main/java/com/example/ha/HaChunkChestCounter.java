package com.example.ha;

import java.util.Map;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

public final class HaChunkChestCounter {
    private HaChunkChestCounter() {
    }

    public static int countCurrentChunkChests(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return 0;
        }

        BlockPos playerPos = client.player.getBlockPos();
        WorldChunk chunk = client.world.getWorldChunk(playerPos);
        if (chunk == null) {
            return 0;
        }

        int count = 0;
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockEntity blockEntity = entry.getValue();
            if (blockEntity instanceof ChestBlockEntity
                || blockEntity instanceof TrappedChestBlockEntity
                || blockEntity instanceof BarrelBlockEntity) {
                count++;
            }
        }
        return count;
    }
}
