package com.xyp.gtneverreset.common.projector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;

import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.ProjectorDeploymentPacket;
import com.xyp.gtneverreset.common.packet.ProjectorDeploymentPacket.Phase;
import com.xyp.gtneverreset.config.ProjectorConfig;
import com.xyp.gtneverreset.config.ToolsConfig;
import com.xyp.gtnrlib.animation.StructureDeployment;
import com.xyp.gtnrlib.animation.StructureDeployment.Launch;
import com.xyp.gtnrlib.animation.StructureDeploymentStyle;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public final class ProjectorBuilder {

    public static final ProjectorBuilder INSTANCE = new ProjectorBuilder();
    private static final long MAX_BATCH_CELLS = 4_000_000L;
    private static final int SIMULATION_ROUND_CELLS = 256;
    private static final int REQUEST_COOLDOWN_TICKS = 40;
    private static final int ANIMATION_RANGE = 128, SPECTATOR_REFRESH_TICKS = 20;
    private static final int LAUNCH_SPECTATOR_REFRESH_TICKS = 4;
    private static final double LAUNCH_FORWARD = .55, LAUNCH_RIGHT = .28, LAUNCH_BELOW_EYES = .36;
    private final Deque<Build> pending = new ArrayDeque<>();
    private final Deque<Build> active = new ArrayDeque<>();
    private final Map<UUID, Build> jobs = new HashMap<>();
    private final Map<UUID, Long> requests = new HashMap<>();
    private long nextDeploymentId, serverTick;
    private Build preparing;

    private ProjectorBuilder() {}

    public static void request(EntityPlayerMP player, int location, boolean batch) {
        if (!ToolsConfig.enableMultiblockProjector || !ProjectorConfig.buildEnabled || !player.capabilities.allowEdit)
            return;
        ProjectorBuilder builder = INSTANCE;
        UUID owner = player.getUniqueID();
        Long previous = builder.requests.get(owner);
        if (builder.building(player) || previous != null && builder.serverTick - previous < REQUEST_COOLDOWN_TICKS) {
            // #tr gtneverreset.projector.build.busy
            // # A generation request is already active or was just submitted.
            // # zh_CN 已有生成任务或刚提交过请求，请稍后再试。
            player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.busy"));
            return;
        }
        ItemStack projector = ProjectorServer.projectorAt(player, location);
        if (projector == null) return;
        ProjectorPlan plan = ProjectorPlan.read(projector);
        if (!plan.isPlaced() || !ProjectorMachines.isSupported(plan.machine)
            || !ProjectorMachines.validFacings(plan.machine)
                .test(plan.facing))
            return;
        if (player.dimension != plan.dimension || !inRange(player, plan)) {
            // #tr gtneverreset.projector.build.range
            // # Go to the dimension of the projected controller and move closer.
            // # zh_CN 请前往投影控制器所在维度并靠近控制器。
            player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.range"));
            return;
        }
        if (batch && plan.batch.machines() < 2) return;
        var build = new Build(player, location, projector, plan, batch);
        builder.requests.put(owner, builder.serverTick);
        builder.jobs.put(owner, build);
        builder.pending.addLast(build);
        // #tr gtneverreset.projector.build.started
        // # Checking the build site…
        // # zh_CN 正在检查生成位置……
        player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.started"));
    }

    public void clear() {
        for (Build build : active) build.finish();
        active.clear();
        pending.clear();
        jobs.clear();
        requests.clear();
        preparing = null;
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        serverTick++;
        if (serverTick % REQUEST_COOLDOWN_TICKS == 0) requests.values()
            .removeIf(tick -> serverTick - tick >= REQUEST_COOLDOWN_TICKS);
        if (preparing == null && !pending.isEmpty()) {
            preparing = pending.removeFirst();
            active.addLast(preparing);
        }
        if (active.isEmpty()) return;
        long deadline = System.nanoTime() + ProjectorConfig.buildBudgetMillis * 1_000_000L;
        for (int remaining = active.size(); remaining > 0 && System.nanoTime() < deadline; remaining--) {
            Build build = active.removeFirst();
            boolean done = true;
            try {
                long now = System.nanoTime();
                long slice = (deadline - now) / remaining;
                done = build.step(now + Math.max(1, slice));
            } catch (RuntimeException e) {
                GTNeverReset.LOG.error("could not generate projector structure {}", build.plan.machine, e);
                build.stopped();
            } finally {
                if (preparing == build && (done || !build.preparing())) preparing = null;
            }
            if (done) {
                jobs.remove(build.player.getUniqueID());
                build.finish();
            } else {
                active.addLast(build);
            }
        }
    }

    boolean building(EntityPlayerMP player) {
        return jobs.containsKey(player.getUniqueID());
    }

    private static boolean inRange(EntityPlayerMP player, ProjectorPlan plan) {
        int range = ProjectorConfig.buildRange;
        return range == 0 || player.getDistanceSq(plan.x + 0.5, plan.y + 0.5, plan.z + 0.5) <= (double) range * range;
    }

    private enum Stage {
        CHECK,
        GROUP,
        PLACE,
        RETRY,
        CONTROLLERS
    }

    private static final class Build {

        private final EntityPlayerMP player;
        private final WorldServer world;
        private final int location;
        private final ItemStack projector;
        private final ProjectorPlan plan;
        private final ProjectorSimulation simulation;
        private final ProjectorBuildMaterials materials;
        private final Block residualHeat = GameRegistry.findBlock("Railcraft", "residual.heat");
        private final int[] offset = new int[3], local = new int[3], simPos = new int[3];
        private ProjectorStructure shape;
        private final int copies;
        private ProjectorBatch.Layout.Placement placement;
        private ProjectorBatch.Layout layout;
        private ProjectorBatchHatches batchHatches;
        private ProjectorBatchHatches.Sample edit;
        private final int[] batchPosition = new int[3];
        private int copy, cell, x, y, z, placed, cellsPerCopy, controllerCell;
        private int band, bandIndex, retryIndex;
        private final IntArrayList retry = new IntArrayList();
        private final int[] bounds = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE,
            Integer.MIN_VALUE, Integer.MIN_VALUE };
        private Stage stage = Stage.CHECK;
        private boolean supplementing, completed, retryable;
        private IntArrayList[] bands;
        private long nextBandTick, nextSpectatorRefresh;
        private StructureDeployment deployment;
        private long deploymentId;
        private ProjectorDeploymentPacket deploymentStart;
        private ProjectorDeploymentPacket deploymentImpact;
        private List<EntityPlayerMP> spectators;

        private Build(EntityPlayerMP player, int location, ItemStack projector, ProjectorPlan plan, boolean batch) {
            this.player = player;
            this.world = (WorldServer) player.worldObj;
            this.location = location;
            this.projector = projector;
            this.plan = plan;
            materials = player.capabilities.isCreativeMode ? null : new ProjectorBuildMaterials(player);
            copies = batch ? plan.batch.machines() : 1;
            this.simulation = new ProjectorSimulation(plan.settings(), SIMULATION_ROUND_CELLS);
            x = plan.x;
            y = plan.y;
            z = plan.z;
        }

        private boolean preparing() {
            return stage == Stage.CHECK || stage == Stage.GROUP;
        }

        private boolean step(long deadline) {
            if (player.isDead || player.worldObj != world || !player.playerNetServerHandler.netManager.isChannelOpen())
                return true;
            if (!ProjectorConfig.buildEnabled || !player.capabilities.allowEdit
                || (materials == null) != player.capabilities.isCreativeMode
                || ProjectorServer.projectorAt(player, location) != projector
                || !inRange(player, plan)) return stopped();
            if (shape == null) {
                if (!simulation.step(Math.max(1, deadline - System.nanoTime()))) return false;
                shape = simulation.result();
                if (shape == null || shape.isEmpty() || !shape.failure.isEmpty() || !shape.limit.isEmpty()) {
                    // #tr gtneverreset.projector.build.incomplete
                    // # Generation stopped: the server could not resolve the complete structure.
                    // # zh_CN 无法生成：服务端未能推算出完整结构。
                    player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.incomplete"));
                    return true;
                }
                cellsPerCopy = shape.size + shape.emptyCells.length;
                controllerCell = shape.indexOf(0, 0, 0);
                if (copies > 1) {
                    if ((long) copies * cellsPerCopy > MAX_BATCH_CELLS) {
                        // #tr gtneverreset.projector.build.batch_limit
                        // # Reduce the batch: a job can check up to %s structure cells.
                        // # zh_CN 请减少台数：单次批量任务最多可检查 %s 个结构格。
                        player.addChatMessage(
                            new ChatComponentTranslation("gtneverreset.projector.build.batch_limit", MAX_BATCH_CELLS));
                        return true;
                    }
                    layout = new ProjectorBatch.Layout(plan.batch, shape, plan.facing);
                    if (!layout.hasDistinctOrigins()) return incompatible();
                    if (!layout.flipSupported()) {
                        // #tr gtneverreset.projector.build.flip_unsupported
                        // # This machine does not support upside-down placement.
                        // # zh_CN 这台机器不支持上下倒置。
                        player.addChatMessage(
                            new ChatComponentTranslation("gtneverreset.projector.build.flip_unsupported"));
                        return true;
                    }

                }
                return false;
            }
            long now = world.getTotalWorldTime();
            if (deploymentStart != null && now >= nextSpectatorRefresh) refreshSpectators(now);
            if (stage == Stage.PLACE && now < nextBandTick) return false;
            if (layout != null && placement == null) {
                if (!layout.advanceSearch(deadline)) return false;
                if (!layout.solve()) {
                    if (layout.searchLimited()) {
                        // #tr gtneverreset.projector.build.search_limit
                        // # Layout search limit reached. Reduce the array or disable sharing on one axis.
                        // # zh_CN 排布搜索达到上限，请缩小阵列或关闭一个方向的共用。
                        player
                            .addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.search_limit"));
                        return true;
                    }
                    return incompatible();
                }
                placement = layout.placement(0);
                if (!plan.batchOverrides.isEmpty()) batchHatches = new ProjectorBatchHatches(shape, plan);
                return false;
            }
            while (System.nanoTime() < deadline) {
                switch (stage) {
                    case CHECK -> {
                        if (copy == copies) {
                            if (!supplementing && bounds[3] != Integer.MIN_VALUE && plan.buildStyle.segmented()) {
                                deployment = deploymentAt(0);
                                bands = new IntArrayList[deployment.bandCount(plan.buildStyle)];
                                for (int i = 0; i < bands.length; i++) bands[i] = new IntArrayList();
                                select(0, 0);
                                stage = Stage.GROUP;
                                continue;
                            }
                            return !beginPlacement(now);
                        }
                        if (!visitSelected())
                            return batchHatches != null && batchHatches.incompatible ? incompatible() : stopped();
                        advance();
                    }
                    case GROUP -> {
                        if (copy == copies) return !beginPlacement(now);
                        if (cell != controllerCell) {
                            position();
                            bands[deployment.bandAt(x, z, bands.length)].add(copy * cellsPerCopy + cell);
                        }
                        advance();
                    }
                    case PLACE -> {
                        if (bands == null) {
                            if (copy == copies) {
                                stage = Stage.RETRY;
                                continue;
                            }
                            if (cell == controllerCell) {
                                advance();
                                continue;
                            }
                        } else {
                            if (band == bands.length) {
                                stage = Stage.RETRY;
                                continue;
                            }
                            IntArrayList cells = bands[band];
                            if (bandIndex == cells.size()) {
                                band++;
                                bandIndex = 0;
                                sendDeploymentPacket(new ProjectorDeploymentPacket(deploymentId, plan.dimension, band));
                                if (band < bands.length) {
                                    nextBandTick = deployment.startTick() + plan.buildStyle.bandReadyTick(band);
                                    if (plan.buildStyle == StructureDeploymentStyle.REVEAL) nextBandTick = Math
                                        .max(nextBandTick, now + StructureDeploymentStyle.REVEAL_BAND_TICKS);
                                    return false;
                                }
                                continue;
                            }
                            select(cells.getInt(bandIndex++));
                        }
                        retryable = false;
                        if (!visitSelected()) {
                            if (!retryable) return stopped();
                            retry.add(copy * cellsPerCopy + cell);
                        }
                        if (bands == null) advance();
                    }
                    case RETRY -> {
                        if (retryIndex == retry.size()) {
                            select(0, controllerCell);
                            stage = Stage.CONTROLLERS;
                            continue;
                        }
                        select(retry.getInt(retryIndex++));
                        if (!visitSelected()) return stopped();
                    }
                    case CONTROLLERS -> {
                        if (controllerCell < 0 || copy == copies) {
                            // #tr gtneverreset.projector.build.done
                            // # Structure generated. Placed %s blocks.
                            // # zh_CN 结构生成完成，共放置 %s 个方块。
                            player.addChatMessage(
                                new ChatComponentTranslation("gtneverreset.projector.build.done", placed));
                            completed = true;
                            return true;
                        }
                        if (!visitSelected()) return stopped();
                        select(copy + 1, controllerCell);
                    }
                }
            }
            return false;
        }

        private boolean visitSelected() {
            position();
            if (batchHatches != null) {
                offset[0] = x - plan.x;
                offset[1] = y - plan.y;
                offset[2] = z - plan.z;
                plan.facing.getOffsetABC(offset, batchPosition);
                edit = batchHatches
                    .sample(ProjectorPlan.pack(batchPosition[0], batchPosition[1], batchPosition[2]), cell);
                if (batchHatches.incompatible) return false;
            }
            return visit();
        }

        private void advance() {
            if (++cell == cellsPerCopy) select(copy + 1, 0);
        }

        private void select(int index) {
            select(index / cellsPerCopy, index % cellsPerCopy);
        }

        private void select(int copy, int cell) {
            if (this.copy != copy && layout != null && copy < copies) placement = layout.placement(copy);
            this.copy = copy;
            this.cell = cell;
        }

        private boolean beginPlacement(long now) {
            if (materials != null && !materials.reserve()) return false;
            select(0, 0);
            stage = Stage.PLACE;
            if (supplementing || bounds[3] == Integer.MIN_VALUE || plan.buildStyle == StructureDeploymentStyle.NONE)
                return true;
            Launch launch = plan.buildStyle == StructureDeploymentStyle.SINGULARITY ? launchOrigin() : null;
            int travelTicks = launch == null ? 0 : StructureDeploymentStyle.SINGULARITY_LAUNCH_TICKS;
            deployment = deploymentAt(now + StructureDeployment.START_DELAY_TICKS + travelTicks);
            nextBandTick = deployment.startTick() + plan.buildStyle.bandReadyTick(0);
            deploymentId = ++INSTANCE.nextDeploymentId;
            spectators = new ArrayList<>();
            deploymentStart = new ProjectorDeploymentPacket(
                deploymentId,
                plan.dimension,
                deployment,
                plan,
                layout != null,
                launch);
            refreshSpectators(now);
            return true;
        }

        private Launch launchOrigin() {
            var look = player.getLookVec();
            double yaw = Math.toRadians(player.rotationYaw);
            return new Launch(
                player.posX + look.xCoord * LAUNCH_FORWARD - Math.cos(yaw) * LAUNCH_RIGHT,
                player.posY + player.getEyeHeight() - LAUNCH_BELOW_EYES + look.yCoord * LAUNCH_FORWARD,
                player.posZ + look.zCoord * LAUNCH_FORWARD - Math.sin(yaw) * LAUNCH_RIGHT);
        }

        private StructureDeployment deploymentAt(long startTick) {
            return new StructureDeployment(
                startTick,
                bounds[0],
                bounds[1],
                bounds[2],
                bounds[3],
                bounds[5],
                Math.min(world.getHeight(), bounds[4] + StructureDeployment.PORTAL_CLEARANCE),
                bounds[4]);
        }

        private void refreshSpectators(long now) {
            int interval = plan.buildStyle == StructureDeploymentStyle.SINGULARITY && now < deployment.startTick()
                ? LAUNCH_SPECTATOR_REFRESH_TICKS
                : SPECTATOR_REFRESH_TICKS;
            nextSpectatorRefresh = now + interval;
            spectators.removeIf(
                observer -> observer.worldObj != world || !observer.playerNetServerHandler.netManager.isChannelOpen());
            for (Object entry : world.playerEntities) {
                EntityPlayerMP observer = (EntityPlayerMP) entry;
                if (spectators.contains(observer) || observer != player
                    && (observer.posX < bounds[0] - ANIMATION_RANGE || observer.posX > bounds[3] + ANIMATION_RANGE
                        || observer.posZ < bounds[2] - ANIMATION_RANGE
                        || observer.posZ > bounds[5] + ANIMATION_RANGE
                        || observer.posY < bounds[1] - ANIMATION_RANGE
                        || observer.posY > deployment.openingY() + ANIMATION_RANGE))
                    continue;
                spectators.add(observer);
                NetWorkHandler.sendTo(deploymentStart, observer);
                if (band > 0)
                    NetWorkHandler.sendTo(new ProjectorDeploymentPacket(deploymentId, plan.dimension, band), observer);
                if (deploymentImpact != null) NetWorkHandler.sendTo(deploymentImpact, observer);
            }
        }

        private void sendDeploymentUpdate(Phase phase) {
            if (spectators != null)
                sendDeploymentPacket(new ProjectorDeploymentPacket(deploymentId, plan.dimension, phase));
        }

        private void sendDeploymentPacket(ProjectorDeploymentPacket packet) {
            for (EntityPlayerMP observer : spectators) {
                if (observer.worldObj == world && observer.playerNetServerHandler.netManager.isChannelOpen())
                    NetWorkHandler.sendTo(packet, observer);
            }
        }

        private void finish() {
            sendDeploymentUpdate(completed ? Phase.COMPLETE : Phase.CANCEL);
            spectators = null;
            deploymentStart = null;
            deploymentImpact = null;
            if (materials != null && !player.isDead
                && player.playerNetServerHandler.netManager.isChannelOpen()
                && ProjectorBuildMaterials.refund(player)) {
                // #tr gtneverreset.projector.build.refund_pending
                // # Unused materials are saved. They return as inventory space becomes available.
                // # zh_CN 未用材料已保留，背包有空位时会自动退还。
                player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.refund_pending"));
            }
        }

        private boolean stopped() {
            // #tr gtneverreset.projector.build.stopped
            // # Generation stopped at %s, %s, %s. Check space, loaded chunks and permissions. Placed %s blocks.
            // # zh_CN 生成在 %s, %s, %s 停止。请检查空间、区块和权限。已放置 %s 个方块。
            player
                .addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.stopped", x, y, z, placed));
            return true;
        }

        private boolean incompatible() {
            // #tr gtneverreset.projector.build.batch_conflict
            // # No compatible shared face. Match the casings and hatch models, or turn off sharing.
            // # zh_CN 共用面不兼容。请检查外壳和仓室型号，或关闭该方向的共用。
            player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.batch_conflict"));
            return true;
        }

        private void position() {
            if (cell < shape.size) {
                local[0] = shape.a[cell];
                local[1] = shape.b[cell];
                local[2] = shape.c[cell];
            } else {
                long packed = shape.emptyCells[cell - shape.size];
                local[0] = ProjectorPlan.unpackA(packed);
                local[1] = ProjectorPlan.unpackB(packed);
                local[2] = ProjectorPlan.unpackC(packed);
            }
            copyFacing().getWorldOffset(local, offset);
            x = plan.x + (placement == null ? 0 : placement.x()) + offset[0];
            y = plan.y + (placement == null ? 0 : placement.y()) + offset[1];
            z = plan.z + (placement == null ? 0 : placement.z()) + offset[2];
        }

        private boolean visit() {
            if (y < 0 || y >= world.getHeight()
                || x < -30000000
                || x >= 30000000
                || z < -30000000
                || z >= 30000000
                || !world.getChunkProvider()
                    .chunkExists(x >> 4, z >> 4))
                return false;
            Block actual = world.getBlock(x, y, z);
            if (cell >= shape.size || (edit == null ? shape.blocks[cell] : edit.block()) == Blocks.air)
                return actual == residualHeat || world.isAirBlock(x, y, z);
            shape.simulationPosition(local[0], local[1], local[2], simPos);
            Block expected = edit == null ? shape.blocks[cell] : edit.block();
            int meta = edit == null ? shape.world.getBlockMetadata(simPos[0], simPos[1], simPos[2]) : edit.meta();
            int machine = edit == null ? shape.machines[cell] : edit.machine();
            if (actual == expected && world.getBlockMetadata(x, y, z) == meta) {
                if (machine == 0) {
                    if (preparing()) supplementing = true;
                    return true;
                }
                if (world.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile
                    && tile.getMetaTileID() == machine) {
                    if (preparing()) supplementing = true;
                    if (!controller()) return true;
                    return tile.getFrontFacing() == copyFacing().getDirection()
                        && (!(tile.getMetaTileEntity() instanceof IAlignment alignment)
                            || alignment.getExtendedFacing() == copyFacing());
                }
            }
            // Railcraft's invisible tracking block owns a tile entity even though the cell is air.
            if (actual != residualHeat && (world.getTileEntity(x, y, z) != null
                || !(actual.isAir(world, x, y, z) || actual.isReplaceable(world, x, y, z)))) return false;
            if (!world.canMineBlock(player, x, y, z) || world.func_73046_m()
                .isBlockProtected(world, x, y, z, player) || !player.canPlayerEdit(x, y, z, 1, projector)) return false;
            ItemStack item = edit == null ? shape.items[cell] : edit.item();
            if (materials != null && (item == null || !(item.getItem() instanceof ItemBlock)
                || Block.getBlockFromItem(item.getItem()) != expected)) {
                // #tr gtneverreset.projector.build.no_item
                // # No survival placement item for the block at %s, %s, %s.
                // # zh_CN 位于 %s, %s, %s 的方块没有对应的生存放置物品。
                player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.no_item", x, y, z));
                return false;
            }
            if (preparing()) {
                if (!supplementing) {
                    bounds[0] = Math.min(bounds[0], x);
                    bounds[1] = Math.min(bounds[1], y);
                    bounds[2] = Math.min(bounds[2], z);
                    bounds[3] = Math.max(bounds[3], x + 1);
                    bounds[4] = Math.max(bounds[4], y + 1);
                    bounds[5] = Math.max(bounds[5], z + 1);
                }
                if (materials != null) materials.require(x, y, z, item);
                return true;
            }
            if (materials != null && !materials.canPlace(x, y, z, item)) return false;
            return place(expected, meta, machine);
        }

        private ExtendedFacing copyFacing() {
            return placement == null ? plan.facing : placement.facing();
        }

        private boolean controller() {
            return local[0] == 0 && local[1] == 0 && local[2] == 0;
        }

        private boolean place(Block expected, int meta, int machine) {
            if (world.captureBlockSnapshots || world.restoringBlockSnapshots) return false;
            int start = world.capturedBlockSnapshots.size();
            world.captureBlockSnapshots = true;
            boolean success = false;
            List<BlockSnapshot> changes;
            try {
                success = putBlock(expected, meta, machine);
            } finally {
                world.captureBlockSnapshots = false;
                List<BlockSnapshot> captured = world.capturedBlockSnapshots
                    .subList(start, world.capturedBlockSnapshots.size());
                changes = new ArrayList<>(captured);
                captured.clear();
                if (!success) restore(changes);
            }
            if (!success || changes.isEmpty()) {
                retryable = true;
                return false;
            }
            boolean accepted = false;
            try {
                for (BlockSnapshot snapshot : changes)
                    if (snapshot.x != x || snapshot.y != y || snapshot.z != z) return false;
                accepted = !ForgeEventFactory.onPlayerBlockPlace(player, changes.get(0), ForgeDirection.UP)
                    .isCanceled();
            } finally {
                if (!accepted) restore(changes);
            }
            if (!accepted) return false;
            if (materials != null) materials.placed(x, y, z, edit == null ? shape.items[cell] : edit.item());
            placed++;
            if (!expected.hasTileEntity(meta)) expected.onBlockAdded(world, x, y, z);
            world.markAndNotifyBlock(x, y, z, null, changes.get(0).replacedBlock, expected, 3);
            if (placed == 1 && deploymentStart != null) {
                if (plan.buildStyle == StructureDeploymentStyle.SUMMON) sendDeploymentUpdate(Phase.IMPACT);
                if (plan.buildStyle == StructureDeploymentStyle.SINGULARITY) {
                    deploymentImpact = new ProjectorDeploymentPacket(
                        deploymentId,
                        plan.dimension,
                        world.getTotalWorldTime());
                    sendDeploymentPacket(deploymentImpact);
                }
            }
            return true;
        }

        private boolean putBlock(Block expected, int meta, int machine) {
            ItemStack item = edit == null ? shape.items[cell] : edit.item();
            if (item != null && item.getItem() instanceof ItemBlock blockItem
                && Block.getBlockFromItem(blockItem) == expected) {
                if (!blockItem.placeBlockAt(
                    item.copy(),
                    player,
                    world,
                    x,
                    y,
                    z,
                    ForgeDirection.UP.ordinal(),
                    0.5f,
                    0.5f,
                    0.5f,
                    meta)) return false;
            } else if (machine != 0 || !world.setBlock(x, y, z, expected, meta, 3)) return false;
            return world.getBlock(x, y, z) == expected && world.getBlockMetadata(x, y, z) == meta
                && (machine == 0 || orientMachine(machine));
        }

        private boolean orientMachine(int id) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof IGregTechTileEntity gt) || gt.getMetaTileID() != id) return false;
            if (controller()) {
                gt.setFrontFacing(copyFacing().getDirection());
                if (gt.getMetaTileEntity() instanceof IAlignment aligned) aligned.setExtendedFacing(copyFacing());
                return true;
            }
            if ((edit == null ? shape.world.getTileEntity(simPos[0], simPos[1], simPos[2])
                : edit.tile()) instanceof IGregTechTileEntity source) {
                ForgeDirection front = source.getFrontFacing();
                if (front != ForgeDirection.UNKNOWN)
                    gt.setFrontFacing(ProjectorOrientation.changeBasis(front, shape.canonical, copyFacing()));
            }
            return true;
        }

        private void restore(List<BlockSnapshot> changes) {
            world.restoringBlockSnapshots = true;
            try {
                for (int i = changes.size() - 1; i >= 0; i--) changes.get(i)
                    .restore(true, false);
            } finally {
                world.restoringBlockSnapshots = false;
            }
        }
    }
}
