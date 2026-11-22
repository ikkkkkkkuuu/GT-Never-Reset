package com.xyp.gtneverreset.common.projector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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
import com.xyp.gtneverreset.config.ToolsConfig;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public final class ProjectorBuilder {

    public static final ProjectorBuilder INSTANCE = new ProjectorBuilder();
    private static final long MAX_BATCH_CELLS = 4_000_000L;
    private static final int CELLS_PER_TICK = 256;
    private static final long TICK_NANOS = 4_000_000L;
    private final Deque<Build> queue = new ArrayDeque<>();

    private ProjectorBuilder() {}

    public static void request(EntityPlayerMP player, int location, boolean batch) {
        if (!ToolsConfig.enableMultiblockProjector || !player.capabilities.allowEdit) return;
        ItemStack projector = ProjectorServer.projectorAt(player, location);
        if (projector == null) return;
        ProjectorPlan plan = ProjectorPlan.read(projector);
        if (!plan.isPlaced() || !ProjectorMachines.isSupported(plan.machine)
            || !ProjectorMachines.validFacings(plan.machine)
                .test(plan.facing))
            return;
        if (player.dimension != plan.dimension || !inRange(player, plan)) {
            // #tr gtneverreset.projector.build.range
            // # Move within 128 blocks of the projected controller in its dimension.
            // # zh_CN 请在投影所在维度靠近控制器（128 格以内）。
            player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.range"));
            return;
        }
        if (batch && plan.batch.machines() < 2) return;
        for (Build build : INSTANCE.queue) if (build.player == player) return;
        INSTANCE.queue.addLast(new Build(player, location, projector, plan, batch));
        // #tr gtneverreset.projector.build.started
        // # Checking the build site…
        // # zh_CN 正在检查生成位置……
        player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.started"));
    }

    public void clear() {
        for (Build build : queue) build.releaseMaterials();
        queue.clear();
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || queue.isEmpty()) return;
        Build build = queue.removeFirst();
        try {
            if (!build.step()) {
                queue.addLast(build);
            } else build.releaseMaterials();
        } catch (RuntimeException e) {
            GTNeverReset.LOG.error("could not generate projector structure {}", build.plan.machine, e);
            build.releaseMaterials();
            build.stopped();
        }
    }

    boolean building(EntityPlayerMP player) {
        for (Build job : queue) if (job.player == player) return true;
        return false;
    }

    private static boolean inRange(EntityPlayerMP player, ProjectorPlan plan) {
        return player.getDistanceSq(plan.x + 0.5, plan.y + 0.5, plan.z + 0.5)
            <= ProjectorPlan.REACH * ProjectorPlan.REACH;
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
        private int copy, cell, x, y, z, placed;
        private boolean writing;

        private Build(EntityPlayerMP player, int location, ItemStack projector, ProjectorPlan plan, boolean batch) {
            this.player = player;
            this.world = (WorldServer) player.worldObj;
            this.location = location;
            this.projector = projector;
            this.plan = plan;
            materials = player.capabilities.isCreativeMode ? null : new ProjectorBuildMaterials(player);
            copies = batch ? plan.batch.machines() : 1;
            this.simulation = new ProjectorSimulation(plan.settings(), CELLS_PER_TICK);
            x = plan.x;
            y = plan.y;
            z = plan.z;
        }

        private boolean step() {
            if (player.isDead || player.worldObj != world || !player.playerNetServerHandler.netManager.isChannelOpen())
                return true;
            if (!player.capabilities.allowEdit || (materials == null) != player.capabilities.isCreativeMode
                || ProjectorServer.projectorAt(player, location) != projector
                || !inRange(player, plan)) return stopped();
            if (shape == null) {
                if (!simulation.step(TICK_NANOS)) return false;
                shape = simulation.result();
                if (shape == null || shape.isEmpty() || !shape.failure.isEmpty() || !shape.limit.isEmpty()) {
                    // #tr gtneverreset.projector.build.incomplete
                    // # Generation stopped: the server could not resolve the complete structure.
                    // # zh_CN 无法生成：服务端未能推算出完整结构。
                    player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.incomplete"));
                    return true;
                }
                if (copies > 1) {
                    if ((long) copies * (shape.size + shape.emptyCells.length) > MAX_BATCH_CELLS) {
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
            long deadline = System.nanoTime() + TICK_NANOS;
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
                return false;
            }
            for (int n = 0; n < CELLS_PER_TICK && System.nanoTime() < deadline; n++) {
                if (cell == shape.size + shape.emptyCells.length) {
                    if (++copy == copies) {
                        if (writing) {
                            // #tr gtneverreset.projector.build.done
                            // # Structure generated. Placed %s blocks.
                            // # zh_CN 结构生成完成，共放置 %s 个方块。
                            player.addChatMessage(
                                new ChatComponentTranslation("gtneverreset.projector.build.done", placed));
                            return true;
                        }
                        if (materials != null && !materials.reserve()) return true;
                        writing = true;
                        copy = 0;
                    }
                    cell = 0;
                    if (layout != null) placement = layout.placement(copy);
                }
                position();
                if (!visit()) return stopped();
                cell++;
            }
            return false;
        }

        private void releaseMaterials() {
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
            if (cell >= shape.size || shape.blocks[cell] == Blocks.air)
                return actual == residualHeat || world.isAirBlock(x, y, z);
            shape.simulationPosition(local[0], local[1], local[2], simPos);
            Block expected = shape.blocks[cell];
            int meta = shape.world.getBlockMetadata(simPos[0], simPos[1], simPos[2]);
            int machine = shape.machines[cell];
            if (actual == expected && world.getBlockMetadata(x, y, z) == meta) {
                if (machine == 0) return true;
                if (world.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile
                    && tile.getMetaTileID() == machine) {
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
            ItemStack item = shape.items[cell];
            if (materials != null && (item == null || !(item.getItem() instanceof ItemBlock)
                || Block.getBlockFromItem(item.getItem()) != expected)) {
                // #tr gtneverreset.projector.build.no_item
                // # No survival placement item for the block at %s, %s, %s.
                // # zh_CN 位于 %s, %s, %s 的方块没有对应的生存放置物品。
                player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.no_item", x, y, z));
                return false;
            }
            if (!writing) {
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
            if (!success || changes.isEmpty()) return false;
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
            if (materials != null) materials.placed(x, y, z, shape.items[cell]);
            placed++;
            if (!expected.hasTileEntity(meta)) expected.onBlockAdded(world, x, y, z);
            world.markAndNotifyBlock(x, y, z, null, changes.get(0).replacedBlock, expected, 3);
            return true;
        }

        private boolean putBlock(Block expected, int meta, int machine) {
            ItemStack item = shape.items[cell];
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
            if (shape.world.getTileEntity(simPos[0], simPos[1], simPos[2]) instanceof IGregTechTileEntity source) {
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
