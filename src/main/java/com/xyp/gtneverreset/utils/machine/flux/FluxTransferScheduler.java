package com.xyp.gtneverreset.utils.machine.flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityFluxConnector;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * GTNH ledger adapter for Flux Networks' ordered transfer cycle. Plugs settle first, then points
 * consume by surge mode and descending user priority, across loaded dimensions. The ordered list
 * is rebuilt only when nodes load/unload or settings change; no world/chunk scan is performed.
 */
public final class FluxTransferScheduler {

    private static final Set<TileEntityFluxConnector> NODES = Collections.newSetFromMap(new WeakHashMap<>());
    private static List<TileEntityFluxConnector> ordered = Collections.emptyList();
    private static boolean dirty;

    public static void add(TileEntityFluxConnector tile) {
        NODES.add(tile);
        dirty = true;
    }

    public static void remove(TileEntityFluxConnector tile) {
        NODES.remove(tile);
        dirty = true;
    }

    public static void changed() {
        dirty = true;
    }

    public static void clear() {
        NODES.clear();
        ordered = Collections.emptyList();
        dirty = false;
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.world.isRemote) return;
        for (Object value : event.getChunk().chunkTileEntityMap.values()) {
            if (value instanceof TileEntityFluxConnector tile && !tile.isInvalid()) add(tile);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) return;
        NODES.removeIf(tile -> tile.getWorldObj() == event.world);
        ordered = Collections.emptyList();
        dirty = true;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (dirty) {
            ordered = new ArrayList<>(NODES);
            ordered.sort(
                Comparator.comparing(TileEntityFluxConnector::isPlug)
                    .reversed()
                    .thenComparing(
                        Comparator.comparingInt(TileEntityFluxConnector::effectivePriority)
                            .reversed()));
            dirty = false;
        }
        for (TileEntityFluxConnector tile : ordered) {
            if (!tile.isInvalid() && tile.getWorldObj() != null && !tile.getWorldObj().isRemote) tile.updateEntity();
        }
    }
}
