package com.xyp.gtneverreset.common.projector;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldServer;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.packet.ProjectorBuildActionPacket.Action;
import com.xyp.gtneverreset.config.ProjectorConfig;
import com.xyp.gtneverreset.config.ToolsConfig;

import appeng.api.config.SecurityPermissions;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongBidirectionalIterator;

public final class ProjectorBuildActions {

    static final ProjectorBuildActions INSTANCE = new ProjectorBuildActions();
    private static final int REQUEST_COOLDOWN_TICKS = 40;
    private static final int SIMULATION_ROUND_CELLS = 256;
    private final Deque<Removal> queue = new ArrayDeque<>();
    private final Map<UUID, Removal> jobs = new HashMap<>();
    private final Map<UUID, Integer> lastRequests = new HashMap<>();

    private ProjectorBuildActions() {}

    public static void request(EntityPlayerMP player, int location, Action action, boolean batch) {
        if (!ToolsConfig.enableMultiblockProjector) return;
        if (action == Action.CANCEL) {
            INSTANCE.cancel(player);
            return;
        }
        if (!ProjectorConfig.buildEnabled || !player.capabilities.allowEdit) return;
        if (INSTANCE.busy(player) || ProjectorBuilder.isBuilding(player)) {
            player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.busy"));
            return;
        }
        ItemStack stack = ProjectorServer.projectorAt(player, location);
        if (stack == null) return;
        ProjectorPlan plan = ProjectorPlan.read(stack);
        if (!plan.isPlaced() || plan.dimension != player.dimension
            || !ProjectorMachines.isSupported(plan.machine)
            || !ProjectorMachines.validFacings(plan.machine)
                .test(plan.facing))
            return;
        UUID owner = player.getUniqueID();
        int tick = player.mcServer.getTickCounter();
        INSTANCE.lastRequests.values()
            .removeIf(previous -> tick - previous >= REQUEST_COOLDOWN_TICKS);
        if (INSTANCE.lastRequests.putIfAbsent(owner, tick) != null) return;
        ProjectorBuildLedger.Build build = action == Action.UNDO ? ProjectorBuildLedger.latest(player, plan) : null;
        if (action == Action.UNDO && build == null) {
            // #tr gtneverreset.projector.removal.no_undo
            // # There is no recent build to undo at this projection.
            // # zh_CN 此投影位置没有可撤销的搭建记录。
            player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.removal.no_undo"));
            return;
        }
        Removal job = new Removal(player, location, stack, plan, batch, build);
        INSTANCE.jobs.put(owner, job);
        INSTANCE.queue.addLast(job);
    }

    boolean busy(EntityPlayerMP player) {
        return jobs.containsKey(player.getUniqueID());
    }

    boolean hasJobs() {
        return !queue.isEmpty();
    }

    void tick(long deadline) {
        for (int remaining = queue.size(); remaining > 0 && System.nanoTime() < deadline; remaining--) {
            Removal job = queue.removeFirst();
            long now = System.nanoTime();
            long share = Math.max(1, (deadline - now) / remaining);
            boolean done = true;
            try {
                done = job.step(now + share);
            } catch (RuntimeException error) {
                GTNeverReset.LOG.warn("projector removal failed for {}", job.player.getUniqueID(), error);
                job.report();
            }
            if (done) {
                jobs.remove(job.player.getUniqueID());
                job.finish();
            } else queue.addLast(job);
        }
    }

    void clear() {
        for (Removal job : jobs.values()) job.finish();
        jobs.clear();
        queue.clear();
        lastRequests.clear();
    }

    private void cancel(EntityPlayerMP player) {
        Removal job = jobs.remove(player.getUniqueID());
        if (job == null) {
            if (ProjectorBuilder.cancel(player)) return;
            // #tr gtneverreset.projector.removal.no_task
            // # No projector construction task is running.
            // # zh_CN 当前没有进行中的投影施工任务。
            player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.removal.no_task"));
            return;
        }
        queue.remove(job);
        job.finish();
        // #tr gtneverreset.projector.removal.cancelled
        // # Projector construction task cancelled.
        // # zh_CN 投影施工任务已取消。
        player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.removal.cancelled"));
    }

    private static final class Removal {

        private final EntityPlayerMP player;
        private final WorldServer world;
        private final ProjectorPlan plan;
        private final ItemStack projector;
        private final int location;
        private final boolean creative, batch;
        private final ProjectorBuildLedger.Build build;
        private final LongBidirectionalIterator history;
        private final ProjectorBuildSite.Cell cell = new ProjectorBuildSite.Cell();
        private final IntArrayList[] layers;
        private ProjectorSimulation simulation;
        private ProjectorBuildSite site;
        private ProjectorSiteChunks chunks;
        private int scanning, layer, layerCell, removed, skipped;
        private boolean ready, pauseReported;

        private Removal(EntityPlayerMP player, int location, ItemStack projector, ProjectorPlan plan, boolean batch,
            ProjectorBuildLedger.Build build) {
            this.player = player;
            this.location = location;
            this.projector = projector;
            this.plan = plan;
            this.batch = batch;
            this.build = build;
            this.world = (WorldServer) player.worldObj;
            this.creative = player.capabilities.isCreativeMode;
            if (build == null) {
                simulation = new ProjectorSimulation(
                    batch ? plan.batchSettings() : plan.settings(),
                    SIMULATION_ROUND_CELLS);
                layers = new IntArrayList[world.getHeight()];
                layer = layers.length - 1;
                history = null;
            } else {
                history = build.newestFirst();
                layers = null;
            }
        }

        private boolean step(long deadline) {
            if (player.isDead || player.worldObj != world || !player.playerNetServerHandler.netManager.isChannelOpen())
                return true;
            if (!ProjectorConfig.buildEnabled || !player.capabilities.allowEdit
                || player.capabilities.isCreativeMode != creative
                || ProjectorServer.projectorAt(player, location) != projector) return report();

            if (build == null && !ready) {
                if (!prepareSite(deadline)) return false;
                if (!ready) return true;
            }

            Collection<ProjectorWirelessAccess.Link> networks = null;
            while (System.nanoTime() < deadline) {
                if (ProjectorRemovedItems.pending(player)) {
                    if (networks == null) networks = ProjectorWirelessAccess.find(player, SecurityPermissions.INJECT)
                        .values();
                    if (ProjectorRemovedItems.deliver(player, networks)) {
                        if (!pauseReported) {
                            // #tr gtneverreset.projector.removal.paused
                            // # Removal is paused until there is room for the recovered blocks.
                            // # zh_CN 回收空间不足，腾出空间后将继续拆除。
                            player
                                .addChatMessage(new ChatComponentTranslation("gtneverreset.projector.removal.paused"));
                            pauseReported = true;
                        }
                        return false;
                    }
                }
                pauseReported = false;
                if (build == null ? !nextProjected() : !nextRecorded()) return report();
            }
            return false;
        }

        private boolean prepareSite(long deadline) {
            if (simulation != null) {
                if (!simulation.step(Math.max(1, deadline - System.nanoTime()))) return false;
                ProjectorStructure shape = simulation.result();
                simulation = null;
                if (shape == null || shape.isEmpty() || !shape.failure.isEmpty() || !shape.limit.isEmpty())
                    return report();
                site = new ProjectorBuildSite(plan, shape, batch);
            }
            if (!site.advance(deadline)) return false;
            int range = ProjectorConfig.buildRange;
            if (!site.valid() || range > 0 && site.distanceSq(player) > (double) range * range) return report();
            if (chunks == null) chunks = new ProjectorSiteChunks(world, site.bounds());
            if (!chunks.step(deadline)) return false;
            while (scanning < site.size() && System.nanoTime() < deadline) {
                if (!site.select(scanning++, cell) || cell.y < 0
                    || cell.y >= layers.length
                    || !site.holdsStructure(world, cell)) continue;
                if (layers[cell.y] == null) layers[cell.y] = new IntArrayList();
                layers[cell.y].add(cell.index);
            }
            ready = scanning >= site.size();
            return ready;
        }

        private boolean nextProjected() {
            while (layer >= 0 && (layers[layer] == null || layerCell >= layers[layer].size())) {
                layer--;
                layerCell = 0;
            }
            if (layer < 0) return false;
            if (!site.select(layers[layer].getInt(layerCell++), cell) || !site.holdsStructure(world, cell)) {
                skipped++;
                return true;
            }
            var block = world.getBlock(cell.x, cell.y, cell.z);
            Item item = Item.getItemFromBlock(block);
            ItemStack drop = item == null ? null
                : new ItemStack(item, 1, block.getDamageValue(world, cell.x, cell.y, cell.z));
            remove(cell.x, cell.y, cell.z, drop);
            return true;
        }

        private boolean nextRecorded() {
            if (!history.hasPrevious()) return false;
            long position = history.previousLong();
            int x = CoordinatePacker.unpackX(position), y = CoordinatePacker.unpackY(position),
                z = CoordinatePacker.unpackZ(position);
            int range = ProjectorConfig.buildRange;
            if (y < 0 || y >= world.getHeight()
                || !world.getChunkProvider()
                    .chunkExists(x >> 4, z >> 4)
                || range > 0 && player.getDistanceSq(x + .5, y + .5, z + .5) > (double) range * range) {
                skipped++;
                return true;
            }
            ProjectorBuildLedger.BlockState state = build.stateAt(position);
            if (!state.matches(world, x, y, z)) {
                skipped++;
                history.remove();
                build.changed();
                return true;
            }
            if (remove(x, y, z, state.expectedDrop())) {
                history.remove();
                build.changed();
            }
            return true;
        }

        private boolean remove(int x, int y, int z, ItemStack drop) {
            if (ProjectorBlockRemoval
                .remove(player, x, y, z, false, drop, stack -> ProjectorRemovedItems.save(player, stack))
                != ProjectorBlockRemoval.Result.REMOVED) {
                skipped++;
                return false;
            }
            removed++;
            return true;
        }

        private boolean report() {
            // #tr gtneverreset.projector.removal.done
            // # Projector removal finished. Removed %s blocks and skipped %s.
            // # zh_CN 投影拆除结束，已拆除 %s 个方块，跳过 %s 个。
            player
                .addChatMessage(new ChatComponentTranslation("gtneverreset.projector.removal.done", removed, skipped));
            return true;
        }

        private void finish() {
            if (chunks != null) chunks.close();
            if (build != null) build.finishUndo();
        }
    }
}
