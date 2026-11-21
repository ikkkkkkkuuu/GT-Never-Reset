package com.xyp.gtneverreset.common.projector;

import java.io.File;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class ProjectorSimulationWorld extends World {

    private static final WorldSettings SETTINGS = new WorldSettings(
        1L,
        WorldSettings.GameType.SURVIVAL,
        false,
        false,
        WorldType.FLAT);

    public int placed;
    public boolean outOfHeight;
    public final LongOpenHashSet positions = new LongOpenHashSet();
    private boolean changed;

    public ProjectorSimulationWorld() {
        super(new EmptySaveHandler(), "GTNRProjectorSimulation", SETTINGS, new WorldProviderSurface(), new Profiler());
        provider.setDimension(Integer.MAX_VALUE);
        provider.hasNoSky = true;
        chunkProvider = createChunkProvider();
        calculateInitialSkylight();
        calculateInitialWeatherBody();
    }

    public boolean consumeChanged() {
        boolean result = changed;
        changed = false;
        return result;
    }

    @Override
    public boolean blockExists(int x, int y, int z) {
        if (y >= 0 && y < 256) return true;
        outOfHeight = true;
        return false;
    }

    @Override
    public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) {
        if (y < 0 || y >= 256) {
            outOfHeight = true;
            return false;
        }
        boolean wasAir = getBlock(x, y, z) == Blocks.air;
        boolean result = super.setBlock(x, y, z, block, meta, flags);
        if (result) {
            changed = true;
            if (wasAir != (block == Blocks.air)) placed += wasAir ? 1 : -1;
            if (block == Blocks.air) positions.remove(ProjectorPlan.pack(x, y, z));
            else positions.add(ProjectorPlan.pack(x, y, z));
        }
        return result;
    }

    @Override
    public boolean setBlockMetadataWithNotify(int x, int y, int z, int meta, int flags) {
        boolean result = super.setBlockMetadataWithNotify(x, y, z, meta, flags);
        if (result) changed = true;
        return result;
    }

    @Override
    protected IChunkProvider createChunkProvider() {
        return new ChunkStore(this);
    }

    @Override
    protected boolean chunkExists(int x, int z) {
        return true;
    }

    @Override
    public boolean spawnEntityInWorld(Entity entity) {
        return false;
    }

    @Override
    public void updateEntities() {}

    // Only updateEntities drops removed tile entities from the loaded list, and it never runs here.
    public void dropRemovedTileEntities() {
        loadedTileEntityList.removeIf(tile -> ((TileEntity) tile).isInvalid());
    }

    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {}

    @Override
    protected int func_152379_p() {
        return 0;
    }

    @Override
    public Entity getEntityByID(int id) {
        return null;
    }

    @Override
    public boolean updateLightByType(EnumSkyBlock type, int x, int y, int z) {
        return true;
    }

    public static class ChunkStore implements IChunkProvider {

        private final World world;
        private final Long2ObjectOpenHashMap<Chunk> loaded = new Long2ObjectOpenHashMap<>();

        public ChunkStore(World world) {
            this.world = world;
        }

        @Override
        public Chunk loadChunk(int x, int z) {
            return provideChunk(x, z);
        }

        @Override
        public Chunk provideChunk(int x, int z) {
            long key = ChunkCoordIntPair.chunkXZ2Int(x, z);
            Chunk chunk = loaded.get(key);
            if (chunk == null) {
                chunk = new Chunk(world, x, z);
                // Chunk.removeTileEntity does nothing on unloaded chunks, so replaced machines would keep theirs.
                chunk.isChunkLoaded = true;
                loaded.put(key, chunk);
            }
            return chunk;
        }

        @Override
        public boolean chunkExists(int x, int z) {
            return true;
        }

        @Override
        public void populate(IChunkProvider provider, int x, int z) {}

        @Override
        public boolean saveChunks(boolean all, IProgressUpdate progress) {
            return false;
        }

        @Override
        public boolean unloadQueuedChunks() {
            return false;
        }

        @Override
        public boolean canSave() {
            return false;
        }

        @Override
        public String makeString() {
            return "GTNRProjectorSimulation";
        }

        @Override
        public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
            return null;
        }

        @Override
        public ChunkPosition func_147416_a(World world, String structure, int x, int y, int z) {
            return null;
        }

        @Override
        public int getLoadedChunkCount() {
            return loaded.size();
        }

        @Override
        public void recreateStructures(int x, int z) {}

        @Override
        public void saveExtraData() {}
    }

    public static class EmptySaveHandler implements ISaveHandler, IPlayerFileData, IChunkLoader {

        @Override
        public WorldInfo loadWorldInfo() {
            return null;
        }

        @Override
        public void checkSessionLock() {}

        @Override
        public IChunkLoader getChunkLoader(WorldProvider provider) {
            return this;
        }

        @Override
        public IPlayerFileData getSaveHandler() {
            return this;
        }

        @Override
        public void saveWorldInfoWithPlayer(WorldInfo info, NBTTagCompound player) {}

        @Override
        public void saveWorldInfo(WorldInfo info) {}

        @Override
        public File getWorldDirectory() {
            return null;
        }

        @Override
        public File getMapFileFromName(String name) {
            return null;
        }

        @Override
        public String getWorldDirectoryName() {
            return null;
        }

        @Override
        public Chunk loadChunk(World world, int x, int z) {
            return null;
        }

        @Override
        public void saveChunk(World world, Chunk chunk) {}

        @Override
        public void saveExtraChunkData(World world, Chunk chunk) {}

        @Override
        public void chunkTick() {}

        @Override
        public void saveExtraData() {}

        @Override
        public void flush() {}

        @Override
        public void writePlayerData(EntityPlayer player) {}

        @Override
        public NBTTagCompound readPlayerData(EntityPlayer player) {
            return null;
        }

        @Override
        public String[] getAvailablePlayerDat() {
            return new String[0];
        }
    }
}
