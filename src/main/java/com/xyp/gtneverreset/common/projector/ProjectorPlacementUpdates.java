package com.xyp.gtneverreset.common.projector;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public final class ProjectorPlacementUpdates {

    private static final int RELEASE_TICKS = 4;
    private static final Map<WorldServer, WorldHolds> HELD_BY_WORLD = new IdentityHashMap<>();
    private static volatile boolean anyHeld;

    private final WorldServer world;
    private final LongOpenHashSet positions = new LongOpenHashSet();
    private final Int2ObjectAVLTreeMap<Batch> waiting = new Int2ObjectAVLTreeMap<>();
    private final ArrayDeque<Batch> revealing = new ArrayDeque<>();
    private int revealedThrough = -1;

    ProjectorPlacementUpdates(WorldServer world) {
        this.world = world;
    }

    public static boolean isHeld(World world, int x, int y, int z) {
        if (!anyHeld) return false;
        synchronized (HELD_BY_WORLD) {
            WorldHolds holds = HELD_BY_WORLD.get(world);
            return holds != null && holds.positions.containsKey(CoordinatePacker.pack(x, y, z));
        }
    }

    public static boolean hasHeldBlocksInChunk(WorldServer world, int chunkX, int chunkZ) {
        if (!anyHeld) return false;
        synchronized (HELD_BY_WORLD) {
            WorldHolds holds = HELD_BY_WORLD.get(world);
            return holds != null && holds.chunks.containsKey(ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ));
        }
    }

    void hold(int band, int x, int y, int z) {
        if (band <= revealedThrough) return;
        long position = CoordinatePacker.pack(x, y, z);
        if (!positions.add(position)) return;
        long chunk = ChunkCoordIntPair.chunkXZ2Int(x >> 4, z >> 4);
        waiting.computeIfAbsent(band, ignored -> new Batch())
            .add(chunk, position);
        synchronized (HELD_BY_WORLD) {
            WorldHolds holds = HELD_BY_WORLD.computeIfAbsent(world, ignored -> new WorldHolds());
            if (holds.positions.addTo(position, 1) == 0) holds.chunks.addTo(chunk, 1);
            anyHeld = true;
        }
    }

    void revealThrough(int band) {
        if (band <= revealedThrough) return;
        revealedThrough = band;
        while (!waiting.isEmpty() && waiting.firstIntKey() <= band) {
            Batch batch = waiting.remove(waiting.firstIntKey());
            batch.perTick = (batch.remaining + RELEASE_TICKS - 1) / RELEASE_TICKS;
            revealing.addLast(batch);
        }
    }

    boolean hasPendingRelease() {
        return !revealing.isEmpty();
    }

    void tick() {
        revealing.removeIf(batch -> {
            batch.publish(batch.perTick);
            return batch.remaining == 0;
        });
    }

    void finish() {
        revealThrough(Integer.MAX_VALUE);
        while (!revealing.isEmpty()) {
            Batch batch = revealing.removeFirst();
            batch.publish(batch.remaining);
        }
    }

    private void publish(long position) {
        positions.remove(position);
        int x = CoordinatePacker.unpackX(position), y = CoordinatePacker.unpackY(position),
            z = CoordinatePacker.unpackZ(position);
        synchronized (HELD_BY_WORLD) {
            WorldHolds holds = HELD_BY_WORLD.get(world);
            if (holds.positions.addTo(position, -1) > 1) return;
            holds.positions.remove(position);
            long chunk = ChunkCoordIntPair.chunkXZ2Int(x >> 4, z >> 4);
            if (holds.chunks.addTo(chunk, -1) == 1) holds.chunks.remove(chunk);
            if (holds.positions.isEmpty()) {
                HELD_BY_WORLD.remove(world);
                anyHeld = !HELD_BY_WORLD.isEmpty();
            }
        }
        world.getPlayerManager()
            .markBlockForUpdate(x, y, z);
    }

    private static final class WorldHolds {

        private final Long2IntOpenHashMap positions = new Long2IntOpenHashMap();
        private final Long2IntOpenHashMap chunks = new Long2IntOpenHashMap();
    }

    private final class Batch {

        private final Long2ObjectLinkedOpenHashMap<LongArrayList> chunks = new Long2ObjectLinkedOpenHashMap<>();
        private LongArrayList currentChunk;
        private int index, remaining, perTick;

        private void add(long chunk, long position) {
            chunks.computeIfAbsent(chunk, ignored -> new LongArrayList())
                .add(position);
            remaining++;
        }

        private void publish(int count) {
            int budget = Math.min(count, remaining);
            while (budget-- > 0) {
                if (currentChunk == null || index >= currentChunk.size()) {
                    currentChunk = chunks.removeFirst();
                    index = 0;
                }
                remaining--;
                ProjectorPlacementUpdates.this.publish(currentChunk.getLong(index++));
            }
        }
    }
}
