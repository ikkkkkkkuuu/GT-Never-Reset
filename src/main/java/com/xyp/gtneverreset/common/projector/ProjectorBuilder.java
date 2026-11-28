package com.xyp.gtneverreset.common.projector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;

import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.ProjectorDeploymentPacket;
import com.xyp.gtneverreset.common.packet.ProjectorDeploymentPacket.Phase;
import com.xyp.gtneverreset.common.projector.ProjectorBuildSite.Cell;
import com.xyp.gtneverreset.common.projector.ProjectorBuildSite.State;
import com.xyp.gtneverreset.config.ProjectorConfig;
import com.xyp.gtneverreset.config.ToolsConfig;
import com.xyp.gtnrlib.animation.StructureDeployment;
import com.xyp.gtnrlib.animation.StructureDeployment.Launch;
import com.xyp.gtnrlib.animation.StructureDeploymentStyle;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.threads.RunnableMachineUpdate;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public final class ProjectorBuilder {

    public static final ProjectorBuilder INSTANCE = new ProjectorBuilder();
    private static final int SIMULATION_ROUND_CELLS = 256;
    private static final int REQUEST_COOLDOWN_TICKS = 40;
    private static final int DENIED_LIMIT = 256;
    private static final int CELL_INDEX_BITS = 22;
    private static final int CELL_INDEX_MASK = (1 << CELL_INDEX_BITS) - 1;
    private static final int ANIMATION_RANGE = 128, SPECTATOR_REFRESH_TICKS = 20;
    private static final int LAUNCH_SPECTATOR_REFRESH_TICKS = 4;
    private static final double LAUNCH_FORWARD = .55, LAUNCH_LATERAL = .44, LAUNCH_BELOW_EYES = .36;
    private final Deque<Build> pending = new ArrayDeque<>();
    private final Deque<Build> active = new ArrayDeque<>();
    private final Map<UUID, Build> jobs = new HashMap<>();
    private final Map<UUID, Long> requests = new HashMap<>();
    private long nextDeploymentId, serverTick;
    private Build preparing;

    private ProjectorBuilder() {}

    public static void request(EntityPlayerMP player, int location, boolean batch) {
        if (!ToolsConfig.enableMultiblockProjector || !ProjectorConfig.buildEnabled
            || player.isDead
            || !player.capabilities.allowEdit) return;
        ProjectorBuilder builder = INSTANCE;
        UUID owner = player.getUniqueID();
        Long previous = builder.requests.get(owner);
        if (builder.building(player) || ProjectorBuildActions.INSTANCE.busy(player)
            || previous != null && builder.serverTick - previous < REQUEST_COOLDOWN_TICKS) {
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
            || player.dimension != plan.dimension
            || !ProjectorMachines.validFacings(plan.machine)
                .test(plan.facing)
            || batch && plan.batch.machines() < 2) return;

        var build = new Build(player, plan, batch);
        builder.requests.put(owner, builder.serverTick);
        builder.jobs.put(owner, build);
        builder.pending.addLast(build);
        // #tr gtneverreset.projector.build.started
        // # Checking the build site…
        // # zh_CN 正在检查生成位置……
        player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.started"));
    }

    static boolean isBuilding(EntityPlayerMP player) {
        return INSTANCE.building(player);
    }

    static boolean cancel(EntityPlayerMP player) {
        ProjectorBuilder builder = INSTANCE;
        Build build = builder.jobs.get(player.getUniqueID());
        if (build == null) return false;
        builder.pending.remove(build);
        builder.active.remove(build);
        builder.finish(build);
        // #tr gtneverreset.projector.build.cancelled
        // # Generation cancelled. Kept %s placed blocks.
        // # zh_CN 已取消生成，保留已放置的 %s 个方块。
        player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.cancelled", build.placed));
        return true;
    }

    public void clear() {
        for (Build build : active) finish(build);
        active.clear();
        pending.clear();
        jobs.clear();
        requests.clear();
        preparing = null;
        ProjectorBuildActions.INSTANCE.clear();
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
        ProjectorBuildActions actions = ProjectorBuildActions.INSTANCE;
        long budget = ProjectorConfig.buildBudgetMillis * 1_000_000L;
        long deadline = System.nanoTime() + budget;
        long actionBudget = actions.hasJobs() ? budget / (active.size() + 1) : 0;
        long buildDeadline = deadline - actionBudget;
        for (int remaining = active.size(); remaining > 0 && System.nanoTime() < buildDeadline; remaining--) {
            Build build = active.removeFirst();
            boolean done = true;
            try {
                long now = System.nanoTime();
                done = build.step(now + Math.max(1, (buildDeadline - now) / remaining));
            } catch (RuntimeException e) {
                GTNeverReset.LOG.error("could not generate projector structure {}", build.plan.machine, e);
                build.stopped();
            }
            if (preparing == build && (done || build.started)) preparing = null;
            if (done) finish(build);
            else active.addLast(build);
        }
        if (actions.hasJobs()) actions.tick(deadline);
    }

    boolean building(EntityPlayerMP player) {
        return jobs.containsKey(player.getUniqueID());
    }

    private void finish(Build build) {
        jobs.remove(build.player.getUniqueID());
        if (preparing == build) preparing = null;
        try {
            build.finish();
        } catch (RuntimeException e) {
            GTNeverReset.LOG.error("could not finish projector structure {}", build.plan.machine, e);
        }
    }

    private static final class Build {

        private final EntityPlayerMP player;
        private final WorldServer world;
        private final ProjectorPlan plan;
        private final boolean batch;
        private final ProjectorSimulation simulation;
        private final ProjectorBuildMaterials materials;
        private final Cell cell = new Cell();
        private final IntArrayList turns = new IntArrayList(), clears = new IntArrayList();
        private final IntArrayList retry = new IntArrayList(), controllers = new IntArrayList();
        private final int[] bounds = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE,
            Integer.MIN_VALUE, Integer.MIN_VALUE };
        private final int[] outcomes = new int[Outcome.values().length];
        private ProjectorBuildSite site;
        private ProjectorSiteChunks chunks;
        private LongArrayList candidates = new LongArrayList();
        private long[] order;
        private int scanned, turned, checkedClears, next, retried, controllersPlaced, placed, cleared, oriented, denied;
        private boolean loaded, started, completed;
        private ProjectorBuildLedger.Build record;
        private ProjectorPlacementUpdates updates;
        private StructureDeployment deployment;
        private int bands, visibleBands;
        private long nextBandTick, nextSpectatorRefresh;
        private long deploymentId;
        private ProjectorDeploymentPacket deploymentStart;
        private ProjectorDeploymentPacket deploymentImpact;
        private List<EntityPlayerMP> spectators;

        private Build(EntityPlayerMP player, ProjectorPlan plan, boolean batch) {
            this.player = player;
            this.world = (WorldServer) player.worldObj;
            this.plan = plan;
            this.batch = batch;
            materials = player.capabilities.isCreativeMode ? null : new ProjectorBuildMaterials(player);
            simulation = new ProjectorSimulation(
                batch ? plan.batchSettings() : plan.settings(),
                SIMULATION_ROUND_CELLS);
        }

        private boolean step(long deadline) {
            if (player.isDead || player.worldObj != world || !player.playerNetServerHandler.netManager.isChannelOpen())
                return true;
            if (!ProjectorConfig.buildEnabled || materials == null && !player.capabilities.isCreativeMode)
                return stopped();
            if (!started) return prepare(deadline);

            long now = world.getTotalWorldTime();
            if (deploymentStart != null) {
                if (now >= nextSpectatorRefresh) refreshSpectators(now);
                reveal(now);
                updates.tick();
            }

            boolean machineUpdates = RunnableMachineUpdate.isCurrentThreadEnabled();
            RunnableMachineUpdate.setCurrentThreadEnabled(false);
            try {
                while (System.nanoTime() < deadline) {
                    if (turned < turns.size()) {
                        if (site.select(turns.getInt(turned++), cell)) attemptTurn();
                    } else if (checkedClears < clears.size()) {
                        if (site.select(clears.getInt(checkedClears++), cell)
                            && site.state(world, cell) == State.OBSTACLE) clear();
                    } else if (next < order.length) {
                        int index = (int) order[next++] & CELL_INDEX_MASK;
                        if (!site.select(index, cell)) continue;
                        if (cell.controller) controllers.add(index);
                        else attempt(index, true);
                    } else if (visibleBands < bands) {
                        return false;
                    } else if (retried < retry.size()) {
                        if (site.select(retry.getInt(retried++), cell)) attempt(cell.index, false);
                    } else if (controllersPlaced < controllers.size()) {
                        if (site.select(controllers.getInt(controllersPlaced++), cell)) attempt(cell.index, false);
                    } else {
                        if (updates != null && updates.hasPendingRelease()) return false;
                        completed = true;
                        return true;
                    }
                    if (denied >= DENIED_LIMIT) return stopped();
                }
            } finally {
                RunnableMachineUpdate.setCurrentThreadEnabled(machineUpdates);
            }
            return false;
        }

        private boolean prepare(long deadline) {
            if (site == null) {
                if (!simulation.step(Math.max(1, deadline - System.nanoTime()))) return false;
                ProjectorStructure shape = simulation.result();
                if (shape == null || shape.isEmpty() || !shape.failure.isEmpty() || !shape.limit.isEmpty()) {
                    // #tr gtneverreset.projector.build.incomplete
                    // # Generation stopped: the server could not resolve the complete structure.
                    // # zh_CN 无法生成：服务端未能推算出完整结构。
                    player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.incomplete"));
                    return true;
                }
                site = new ProjectorBuildSite(plan, shape, batch);
            }
            if (chunks == null) {
                if (!site.advance(deadline)) return false;
                if (!site.valid()) {
                    // #tr gtneverreset.projector.build.batch_limit
                    // # This projection is too large to generate.
                    // # zh_CN 投影范围过大，无法生成。
                    player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.batch_limit"));
                    return true;
                }
                int range = ProjectorConfig.buildRange;
                if (range > 0 && site.distanceSq(player) > (double) range * range) {
                    // #tr gtneverreset.projector.build.range
                    // # Move closer to the projected structure.
                    // # zh_CN 请靠近投影结构。
                    player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.range"));
                    return true;
                }
                chunks = new ProjectorSiteChunks(world, site.bounds());
            }
            if (!loaded) {
                loaded = chunks.step(deadline);
                if (!loaded) return false;
            }
            while (scanned < site.size() && System.nanoTime() < deadline) {
                if (site.select(scanned++, cell)) scan();
            }
            if (scanned < site.size()) return false;
            if (materials != null && !materials.reserve()) return true;
            order = candidates.toLongArray();
            Arrays.sort(order);
            candidates = null;
            record = ProjectorBuildLedger.begin(player, plan);
            started = true;
            if (order.length > 0 && plan.buildStyle != StructureDeploymentStyle.NONE)
                startDeployment(world.getTotalWorldTime());
            return false;
        }

        private void scan() {
            State state = site.state(world, cell);
            switch (state) {
                case UNLOADED -> count(Outcome.UNLOADED);
                case MATCHED -> {
                    if (cell.requiresBlock()) count(Outcome.EXISTING);
                }
                case TURN -> turns.add(cell.index);
                case OPEN, OBSTACLE -> {
                    if (!cell.requiresBlock()) {
                        if (state == State.OBSTACLE) clears.add(cell.index);
                        return;
                    }
                    if (!ProjectorBlockRemoval.canEdit(player, cell.x, cell.y, cell.z, cell.item)) {
                        count(Outcome.DENIED);
                        return;
                    }
                    if (ProjectorBlockRemoval.protectedTile(world.getTileEntity(cell.x, cell.y, cell.z))
                        || state == State.OBSTACLE && !ProjectorBlockRemoval.canClear(world, cell.x, cell.y, cell.z)) {
                        count(Outcome.OCCUPIED);
                        return;
                    }
                    if (materials != null && !hasItem()) {
                        count(Outcome.FAILED);
                        return;
                    }
                    if (materials != null) materials.require(cell.x, cell.y, cell.z, cell.item);
                    int[] area = site.bounds();
                    int width = area[3] - area[0] + 1;
                    int depth = area[5] - area[2] + 1;
                    long position = ((long) (cell.y - area[1]) * depth + cell.z - area[2]) * width + cell.x - area[0];
                    candidates.add((position << CELL_INDEX_BITS) | cell.index);
                    bounds[0] = Math.min(bounds[0], cell.x);
                    bounds[1] = Math.min(bounds[1], cell.y);
                    bounds[2] = Math.min(bounds[2], cell.z);
                    bounds[3] = Math.max(bounds[3], cell.x + 1);
                    bounds[4] = Math.max(bounds[4], cell.y + 1);
                    bounds[5] = Math.max(bounds[5], cell.z + 1);
                }
            }
        }

        private void attempt(int index, boolean retryPlacement) {
            Outcome outcome;
            try {
                outcome = place();
            } catch (RuntimeException e) {
                GTNeverReset.LOG.warn("could not place projector block at {}, {}, {}", cell.x, cell.y, cell.z, e);
                outcome = Outcome.FAILED;
            }
            if (outcome == Outcome.OBSTRUCTED && retryPlacement) {
                retry.add(index);
                return;
            }
            count(outcome);
            if (outcome == Outcome.PLACED) denied = 0;
            else if (outcome == Outcome.DENIED) denied++;
        }

        private Outcome place() {
            State state = site.state(world, cell);
            if (state == State.MATCHED) return Outcome.EXISTING;
            if (state == State.UNLOADED) return Outcome.UNLOADED;
            if (state == State.TURN) return turn() ? Outcome.EXISTING : Outcome.DENIED;
            if (!ProjectorBlockRemoval.canEdit(player, cell.x, cell.y, cell.z, cell.item)) return Outcome.DENIED;
            if (materials != null && !player.capabilities.isCreativeMode
                && !materials.canPlace(cell.x, cell.y, cell.z, cell.item)) return Outcome.FAILED;
            if (updates != null) updates.hold(deployment.bandAt(cell.x, cell.z, bands), cell.x, cell.y, cell.z);
            if (ProjectorBlockRemoval.protectedTile(world.getTileEntity(cell.x, cell.y, cell.z)))
                return Outcome.OCCUPIED;
            if (state == State.OBSTACLE) {
                var removal = removeObstacle();
                if (removal != ProjectorBlockRemoval.Result.REMOVED) return switch (removal) {
                    case OCCUPIED -> Outcome.OCCUPIED;
                    case DENIED -> Outcome.DENIED;
                    default -> Outcome.FAILED;
                };
            }
            if (!world.canPlaceEntityOnSide(
                cell.block,
                cell.x,
                cell.y,
                cell.z,
                false,
                ForgeDirection.UP.ordinal(),
                null,
                cell.item)) return Outcome.OBSTRUCTED;
            return placeWithSnapshot();
        }

        private Outcome placeWithSnapshot() {
            if (world.captureBlockSnapshots || world.restoringBlockSnapshots) return Outcome.FAILED;
            int start = world.capturedBlockSnapshots.size();
            List<BlockSnapshot> changes = new ArrayList<>();
            boolean accepted = false;
            try {
                boolean success;
                world.captureBlockSnapshots = true;
                try {
                    success = putBlock();
                } finally {
                    world.captureBlockSnapshots = false;
                    List<BlockSnapshot> captured = world.capturedBlockSnapshots
                        .subList(start, world.capturedBlockSnapshots.size());
                    changes.addAll(captured);
                    captured.clear();
                }
                if (!success || changes.isEmpty()) return Outcome.OBSTRUCTED;
                boolean cancelled = changes.size()
                    == 1 ? ForgeEventFactory.onPlayerBlockPlace(player, changes.get(0), ForgeDirection.UP)
                        .isCanceled()
                        : ForgeEventFactory.onPlayerMultiBlockPlace(player, changes, ForgeDirection.UP)
                            .isCanceled();
                if (cancelled) return Outcome.DENIED;
                accepted = true;
                if (materials != null && !player.capabilities.isCreativeMode)
                    materials.placed(cell.x, cell.y, cell.z, cell.item);
                record.recordPlaced(world, cell.x, cell.y, cell.z, cell.item);
                placed++;
                for (BlockSnapshot change : changes) {
                    Block current = world.getBlock(change.x, change.y, change.z);
                    int meta = world.getBlockMetadata(change.x, change.y, change.z);
                    if (!current.hasTileEntity(meta)) current.onBlockAdded(world, change.x, change.y, change.z);
                    world.markAndNotifyBlock(
                        change.x,
                        change.y,
                        change.z,
                        null,
                        change.getReplacedBlock(),
                        current,
                        change.flag);
                }
                return Outcome.PLACED;
            } finally {
                if (!accepted && !changes.isEmpty()) {
                    world.restoringBlockSnapshots = true;
                    try {
                        for (int i = changes.size() - 1; i >= 0; i--) changes.get(i)
                            .restore(true, false);
                    } finally {
                        world.restoringBlockSnapshots = false;
                        world.markBlockForUpdate(cell.x, cell.y, cell.z);
                    }
                }
            }
        }

        private boolean hasItem() {
            return cell.item != null && cell.item.getItem() instanceof ItemBlock blockItem
                && Block.getBlockFromItem(blockItem) == cell.block;
        }

        private boolean putBlock() {
            if (hasItem()) {
                ItemBlock item = (ItemBlock) cell.item.getItem();
                if (!item.placeBlockAt(
                    cell.item.copy(),
                    player,
                    world,
                    cell.x,
                    cell.y,
                    cell.z,
                    ForgeDirection.UP.ordinal(),
                    .5f,
                    .5f,
                    .5f,
                    cell.meta)) return false;
            } else if (cell.machine != 0 || !world.setBlock(cell.x, cell.y, cell.z, cell.block, cell.meta, 3))
                return false;
            if (world.getBlock(cell.x, cell.y, cell.z) != cell.block
                || world.getBlockMetadata(cell.x, cell.y, cell.z) != cell.meta) return false;
            if (cell.machine == 0) return true;
            TileEntity entity = world.getTileEntity(cell.x, cell.y, cell.z);
            if (!(entity instanceof IGregTechTileEntity tile) || tile.getMetaTileID() != cell.machine) return false;
            if (cell.controller) {
                face(tile);
            } else {
                ForgeDirection front = ProjectorMachines.isHatch(cell.machine) ? site.hatchFront(world, cell, tile)
                    : cell.front;
                if (front != ForgeDirection.UNKNOWN) tile.setFrontFacing(front);
            }
            return true;
        }

        private void attemptTurn() {
            try {
                if (!turn() && site.state(world, cell) == State.TURN) count(Outcome.DENIED);
            } catch (RuntimeException error) {
                GTNeverReset.LOG
                    .warn("could not turn projector controller at {}, {}, {}", cell.x, cell.y, cell.z, error);
                count(Outcome.FAILED);
            }
        }

        private boolean turn() {
            if (site.state(world, cell) != State.TURN
                || !ProjectorBlockRemoval.canEdit(player, cell.x, cell.y, cell.z, cell.item)
                || !(world.getTileEntity(cell.x, cell.y, cell.z) instanceof IGregTechTileEntity tile)) return false;
            face(tile);
            oriented++;
            return true;
        }

        private void face(IGregTechTileEntity tile) {
            tile.setFrontFacing(cell.facing.getDirection());
            if (tile.getMetaTileEntity() instanceof IAlignment alignment) alignment.setExtendedFacing(cell.facing);
        }

        private void clear() {
            try {
                switch (removeObstacle()) {
                    case DENIED -> count(Outcome.DENIED);
                    case OCCUPIED -> count(Outcome.OCCUPIED);
                    case FAILED -> count(Outcome.FAILED);
                    default -> {}
                }
            } catch (RuntimeException error) {
                GTNeverReset.LOG.warn("could not clear projector position {}, {}, {}", cell.x, cell.y, cell.z, error);
                count(Outcome.FAILED);
            }
        }

        private ProjectorBlockRemoval.Result removeObstacle() {
            var result = ProjectorBlockRemoval.remove(player, cell.x, cell.y, cell.z, true, null, this::give);
            if (result == ProjectorBlockRemoval.Result.REMOVED) cleared++;
            return result;
        }

        private void give(ItemStack stack) {
            player.inventory.addItemStackToInventory(stack);
            if (stack.stackSize > 0) player.dropPlayerItemWithRandomChoice(stack, false);
        }

        private void count(Outcome outcome) {
            outcomes[outcome.ordinal()]++;
        }

        private void startDeployment(long now) {
            Launch launch = plan.buildStyle == StructureDeploymentStyle.SINGULARITY ? launchOrigin() : null;
            int travelTicks = launch == null ? 0 : StructureDeploymentStyle.SINGULARITY_LAUNCH_TICKS;
            deployment = new StructureDeployment(
                now + StructureDeployment.START_DELAY_TICKS + travelTicks,
                bounds[0],
                bounds[1],
                bounds[2],
                bounds[3],
                bounds[5],
                Math.min(world.getHeight(), bounds[4] + StructureDeployment.PORTAL_CLEARANCE),
                bounds[4]);
            bands = deployment.bandCount(plan.buildStyle);
            nextBandTick = deployment.startTick() + plan.buildStyle.bandReadyTick(0);
            deploymentId = ++INSTANCE.nextDeploymentId;
            spectators = new ArrayList<>();
            updates = new ProjectorPlacementUpdates(world);
            deploymentStart = new ProjectorDeploymentPacket(
                deploymentId,
                plan.dimension,
                deployment,
                plan,
                batch,
                launch);
            refreshSpectators(now);
        }

        private void reveal(long now) {
            if (visibleBands >= bands || now < nextBandTick) return;
            updates.revealThrough(visibleBands++);
            if (plan.buildStyle.segmented())
                sendDeploymentPacket(new ProjectorDeploymentPacket(deploymentId, plan.dimension, visibleBands));
            if (plan.buildStyle == StructureDeploymentStyle.SUMMON) sendDeploymentUpdate(Phase.IMPACT);
            if (plan.buildStyle == StructureDeploymentStyle.SINGULARITY) {
                deploymentImpact = new ProjectorDeploymentPacket(deploymentId, plan.dimension, now);
                sendDeploymentPacket(deploymentImpact);
            }
            if (visibleBands < bands)
                nextBandTick = Math.max(now + 1, deployment.startTick() + plan.buildStyle.bandReadyTick(visibleBands));
        }

        private Launch launchOrigin() {
            var look = player.getLookVec();
            double yaw = Math.toRadians(player.rotationYaw);
            double x = player.posX + look.xCoord * LAUNCH_FORWARD;
            double y = player.posY + player.getEyeHeight() - LAUNCH_BELOW_EYES + look.yCoord * LAUNCH_FORWARD;
            double z = player.posZ + look.zCoord * LAUNCH_FORWARD;
            double lateralX = Math.cos(yaw) * LAUNCH_LATERAL;
            double lateralZ = Math.sin(yaw) * LAUNCH_LATERAL;
            return new Launch(x - lateralX, y, z - lateralZ, x + lateralX, z + lateralZ);
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
                if (visibleBands > 0 && plan.buildStyle.segmented()) NetWorkHandler
                    .sendTo(new ProjectorDeploymentPacket(deploymentId, plan.dimension, visibleBands), observer);
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
            try {
                if (updates != null) updates.finish();
                if (record != null) record.finish();
                sendDeploymentUpdate(completed ? Phase.COMPLETE : Phase.CANCEL);
                if (placed + cleared + oriented > 0) updateMachines();
                if (completed) {
                    // #tr gtneverreset.projector.build.done
                    // # Placed %s, cleared %s, turned %s, kept %s. Blocked %s, denied %s, unloaded %s, failed %s.
                    // # zh_CN 放置 %s，清理 %s，转向 %s，已有 %s。障碍 %s，拒绝 %s，未加载 %s，失败 %s。
                    player.addChatMessage(
                        new ChatComponentTranslation(
                            "gtneverreset.projector.build.done",
                            placed,
                            cleared,
                            oriented,
                            outcomes[Outcome.EXISTING.ordinal()],
                            outcomes[Outcome.OCCUPIED.ordinal()],
                            outcomes[Outcome.DENIED.ordinal()],
                            outcomes[Outcome.UNLOADED.ordinal()],
                            outcomes[Outcome.FAILED.ordinal()] + outcomes[Outcome.OBSTRUCTED.ordinal()]));
                }
            } finally {
                if (chunks != null) chunks.close();
                if (materials != null && !player.isDead
                    && player.playerNetServerHandler.netManager.isChannelOpen()
                    && ProjectorBuildMaterials.refund(player)) {
                    // #tr gtneverreset.projector.build.refund_pending
                    // # Unused materials are saved. They return as inventory space becomes available.
                    // # zh_CN 未用材料已保留，背包有空位时会自动退还。
                    player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.refund_pending"));
                }
            }
        }

        private void updateMachines() {
            for (int copy = 0; copy < site.copies(); copy++) {
                var origin = site.placement(copy);
                int x = plan.x + origin.x(), y = plan.y + origin.y(), z = plan.z + origin.z();
                if (world.blockExists(x, y, z)) GregTechAPI.causeMachineUpdate(world, x, y, z);
            }
        }

        private boolean stopped() {
            // #tr gtneverreset.projector.build.stopped
            // # Generation stopped at %s, %s, %s. Kept %s placed blocks.
            // # zh_CN 生成在 %s, %s, %s 停止，保留已放置的 %s 个方块。
            int x = scanned == 0 ? plan.x : cell.x;
            int y = scanned == 0 ? plan.y : cell.y;
            int z = scanned == 0 ? plan.z : cell.z;
            player
                .addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.stopped", x, y, z, placed));
            return true;
        }
    }

    private enum Outcome {
        PLACED,
        EXISTING,
        OCCUPIED,
        DENIED,
        UNLOADED,
        FAILED,
        OBSTRUCTED
    }
}
