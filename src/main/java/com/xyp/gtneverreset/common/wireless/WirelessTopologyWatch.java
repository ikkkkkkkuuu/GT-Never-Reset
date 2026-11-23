package com.xyp.gtneverreset.common.wireless;

import java.util.Collection;
import java.util.Map;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.xyp.gtneverreset.common.wireless.PhysicalClusterTracker.Cluster;
import com.xyp.gtneverreset.common.wireless.WirelessChannelManager.Address;
import com.xyp.gtnrlib.world.IBlockStateListener;
import com.xyp.gtnrlib.world.WorldListener;

import appeng.api.networking.IGridNode;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

public class WirelessTopologyWatch {

    private final Map<World, LongSet> watched = new Reference2ObjectOpenHashMap<>();
    private final Map<World, Long2ObjectMap<TileEntity>> tiles = new Reference2ObjectOpenHashMap<>();
    private final Map<World, Long2ObjectMap<ChunkWatch>> listeners = new Reference2ObjectOpenHashMap<>();

    void update(Collection<Cluster<IGridNode, WirelessFrequency>> clusters, PhysicalMeTopology topology) {
        Map<World, LongSet> next = new Reference2ObjectOpenHashMap<>();
        Map<World, Long2ObjectMap<TileEntity>> nextTiles = new Reference2ObjectOpenHashMap<>();
        for (var cluster : clusters) collect(cluster, topology, next, nextTiles);
        for (World world : tiles.keySet()) {
            if (!nextTiles.containsKey(world)) {
                for (ChunkWatch listener : listeners.remove(world)
                    .values()) WorldListener.INSTANCE.unregisterBlockStateListener(world, listener);
            }
        }
        for (var entry : nextTiles.entrySet()) {
            register(entry.getKey(), entry.getValue(), true);
        }
        watched.clear();
        watched.putAll(next);
        tiles.clear();
        tiles.putAll(nextTiles);
    }

    public void add(Cluster<IGridNode, WirelessFrequency> cluster, PhysicalMeTopology topology) {
        Map<World, Long2ObjectMap<TileEntity>> added = new Reference2ObjectOpenHashMap<>();
        collect(cluster, topology, watched, added);
        for (var entry : added.entrySet()) {
            Long2ObjectMap<TileEntity> current = tiles
                .computeIfAbsent(entry.getKey(), ignored -> new Long2ObjectOpenHashMap<>());
            current.putAll(entry.getValue());
            register(entry.getKey(), entry.getValue(), false);
        }
    }

    private static void collect(Cluster<IGridNode, WirelessFrequency> cluster, PhysicalMeTopology topology,
        Map<World, LongSet> map, Map<World, Long2ObjectMap<TileEntity>> tiles) {
        if (cluster.frequencies()
            .isEmpty()) return;
        for (IGridNode node : cluster.nodes()) add(map, tiles, topology.nodeAddress(node));
        for (WirelessFrequency source : cluster.frequencies()) add(map, tiles, WirelessFrequencies.controller(source));
    }

    private void register(World world, Long2ObjectMap<TileEntity> watchedTiles, boolean replace) {
        Long2ObjectMap<LongSet> positionsByChunk = new Long2ObjectOpenHashMap<>();
        for (long position : watchedTiles.keySet()) {
            long chunk = CoordinatePacker
                .pack(CoordinatePacker.unpackX(position) >> 4, 0, CoordinatePacker.unpackZ(position) >> 4);
            positionsByChunk.computeIfAbsent(chunk, ignored -> new LongOpenHashSet())
                .add(position);
        }
        Long2ObjectMap<ChunkWatch> current = listeners
            .computeIfAbsent(world, ignored -> new Long2ObjectOpenHashMap<>());
        if (replace) {
            for (var iterator = current.long2ObjectEntrySet()
                .iterator(); iterator.hasNext();) {
                var entry = iterator.next();
                if (positionsByChunk.containsKey(entry.getLongKey())) continue;
                WorldListener.INSTANCE.unregisterBlockStateListener(world, entry.getValue());
                iterator.remove();
            }
        }
        for (var entry : positionsByChunk.long2ObjectEntrySet()) {
            ChunkWatch listener = current.computeIfAbsent(entry.getLongKey(), ignored -> new ChunkWatch(world));
            LongSet positions = entry.getValue();
            if (!replace) positions.addAll(listener.positions);
            if (positions.equals(listener.positions)) continue;
            listener.positions = positions;
            register(world, listener);
        }
    }

    private void register(World world, ChunkWatch listener) {
        Iterable<BlockPos> positions = () -> listener.positions.longStream()
            .mapToObj(
                position -> new BlockPos(
                    CoordinatePacker.unpackX(position),
                    CoordinatePacker.unpackY(position),
                    CoordinatePacker.unpackZ(position)))
            .iterator();
        WorldListener.INSTANCE.registerBlockStateListener(world, listener, positions);
    }

    private class ChunkWatch implements IBlockStateListener {

        private final World world;
        private LongSet positions = new LongOpenHashSet();

        private ChunkWatch(World world) {
            this.world = world;
        }

        @Override
        public void onBlockChanged(BlockPos pos) {
            WirelessTopologyWatch.this.onBlockChanged(world, pos);
        }
    }

    private static void add(Map<World, LongSet> map, Map<World, Long2ObjectMap<TileEntity>> tiles, Address address) {
        if (address == null) return;
        var tile = address.tile();
        if (tile == null) return;
        World world = tile.getWorldObj();
        long position = CoordinatePacker.pack(address.x(), address.y(), address.z());
        tiles.computeIfAbsent(world, ignored -> new Long2ObjectOpenHashMap<>())
            .put(position, tile);
        LongSet positions = map.computeIfAbsent(world, ignored -> new LongOpenHashSet());
        positions.add(position);
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            positions.add(
                CoordinatePacker
                    .pack(address.x() + side.offsetX, address.y() + side.offsetY, address.z() + side.offsetZ));
        }
    }

    void nodeChanged(IGridNode node) {
        if (watched.isEmpty()) return;
        var block = node.getGridBlock();
        if (block == null) return;
        var position = block.getLocation();
        if (position == null) return;
        World world = position.getWorld();
        if (world == null || world.isRemote) return;
        LongSet positions = watched.get(world);
        if (positions != null && positions.contains(CoordinatePacker.pack(position.x, position.y, position.z))) {
            WirelessClusterManager.invalidateTopology();
        }
    }

    private void onBlockChanged(World world, BlockPos pos) {
        Long2ObjectMap<TileEntity> expectedTiles = tiles.get(world);
        TileEntity expected = expectedTiles == null ? null
            : expectedTiles.get(CoordinatePacker.pack(pos.x, pos.y, pos.z));
        if (expected == null || expected.isInvalid()
            || !world.blockExists(pos.x, pos.y, pos.z)
            || world.getTileEntity(pos.x, pos.y, pos.z) != expected) WirelessClusterManager.invalidateTopology();
    }

    void clear() {
        for (var entry : listeners.entrySet()) {
            for (ChunkWatch listener : entry.getValue()
                .values()) WorldListener.INSTANCE.unregisterBlockStateListener(entry.getKey(), listener);
        }
        listeners.clear();
        watched.clear();
        tiles.clear();
    }
}
