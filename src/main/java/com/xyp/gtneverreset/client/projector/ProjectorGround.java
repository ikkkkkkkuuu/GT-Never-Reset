package com.xyp.gtneverreset.client.projector;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraftforge.common.IPlantable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

@SideOnly(Side.CLIENT)
public class ProjectorGround {

    public static final int UNLOADED = Integer.MIN_VALUE, NONE = -1;
    private static final int MAX_COLUMNS = 1 << 16;

    private final Long2IntOpenHashMap heights = new Long2IntOpenHashMap();
    private World world;

    public ProjectorGround() {
        heights.defaultReturnValue(UNLOADED);
    }

    public void clear() {
        heights.clear();
        world = null;
    }

    public void invalidate(int x, int z) {
        heights.remove(ChunkCoordIntPair.chunkXZ2Int(x, z));
    }

    public void invalidateChunk(int chunkX, int chunkZ) {
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) invalidate(chunkX << 4 | dx, chunkZ << 4 | dz);
        }
    }

    public void fit(int columns) {
        if (heights.size() > Math.max(MAX_COLUMNS, 2 * columns)) heights.clear();
    }

    public int height(World current, int x, int z) {
        if (current != world) {
            heights.clear();
            world = current;
        }
        long key = ChunkCoordIntPair.chunkXZ2Int(x, z);
        int cached = heights.get(key);
        if (cached != UNLOADED) return cached;
        Chunk chunk = current.getChunkFromChunkCoords(x >> 4, z >> 4);
        // The client hands out an empty chunk for every column it has not received.
        if (chunk instanceof EmptyChunk || !chunk.isChunkLoaded) return UNLOADED;
        int height = scan(current, chunk, x, z);
        heights.put(key, height);
        return height;
    }

    private static int scan(World world, Chunk chunk, int x, int z) {
        int top = Math.min(255, chunk.getTopFilledSegment() + 15);
        for (int y = top; y >= 0; y--) {
            if (isGround(world, chunk.getBlock(x & 15, y, z & 15), x, y, z)) return y;
        }
        return NONE;
    }

    public static boolean isGround(World world, Block block, int x, int y, int z) {
        if (block == Blocks.air || block == Blocks.snow_layer) return false;
        Material material = block.getMaterial();
        return material.isSolid() && !material.isLiquid()
            && !block.isReplaceable(world, x, y, z)
            && !block.isLeaves(world, x, y, z)
            && !block.isWood(world, x, y, z)
            && !(block instanceof IPlantable);
    }
}
