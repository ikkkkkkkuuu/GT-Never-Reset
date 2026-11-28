package com.xyp.gtneverreset.common.projector;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

import com.xyp.gtneverreset.GTNeverReset;

final class ProjectorSiteChunks {

    private static final int LOADS_PER_TICK = 4;
    private static final int TIMEOUT_TICKS = 200;

    private final WorldServer world;
    private final int firstX, lastX, lastZ;
    private final long timeout;
    private final List<Ticket> tickets = new ArrayList<>();
    private int x, z, loading;
    private long loadTick = Long.MIN_VALUE;
    private int started;
    private boolean ticketsUnavailable;

    ProjectorSiteChunks(WorldServer world, int[] bounds) {
        this.world = world;
        x = firstX = bounds[0] >> 4;
        z = bounds[2] >> 4;
        lastX = bounds[3] >> 4;
        lastZ = bounds[5] >> 4;
        timeout = world.getTotalWorldTime() + TIMEOUT_TICKS;
    }

    boolean step(long deadline) {
        long now = world.getTotalWorldTime();
        if (now >= timeout) return true;
        if (loadTick != now) {
            loadTick = now;
            started = 0;
        }
        while (z <= lastZ && started < LOADS_PER_TICK && System.nanoTime() < deadline) {
            int chunkX = x;
            int chunkZ = z;
            if (++x > lastX) {
                x = firstX;
                z++;
            }
            if (world.theChunkProviderServer.chunkExists(chunkX, chunkZ)) continue;
            Ticket ticket = ticket();
            if (ticket != null) ForgeChunkManager.forceChunk(ticket, new ChunkCoordIntPair(chunkX, chunkZ));
            started++;
            loading++;
            world.theChunkProviderServer.loadChunk(chunkX, chunkZ, () -> loading--);
        }
        return z > lastZ && loading == 0;
    }

    void close() {
        for (Ticket ticket : tickets) ForgeChunkManager.releaseTicket(ticket);
        tickets.clear();
    }

    private Ticket ticket() {
        if (!tickets.isEmpty()) {
            Ticket current = tickets.get(tickets.size() - 1);
            if (current.getChunkList()
                .size() < current.getChunkListDepth()) return current;
        }
        if (ticketsUnavailable) return null;
        Ticket ticket = ForgeChunkManager.requestTicket(GTNeverReset.INSTANCE, world, ForgeChunkManager.Type.NORMAL);
        if (ticket == null) ticketsUnavailable = true;
        else tickets.add(ticket);
        return ticket;
    }
}
