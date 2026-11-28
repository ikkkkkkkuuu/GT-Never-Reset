package com.xyp.gtneverreset.common.projector;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.common.projector.ProjectorBatch.Layout;
import com.xyp.gtneverreset.common.projector.ProjectorBatch.Layout.Placement;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

final class ProjectorBuildSite {

    private static final int MAX_CELLS = 4_000_000;
    private static final int WORLD_LIMIT = 30_000_000;

    private final ProjectorPlan plan;
    private final ProjectorStructure shape;
    private final Layout layout;
    private final int copies;
    private final int perCopy;
    private final int controller;
    private final Block residualHeat = GameRegistry.findBlock("Railcraft", "residual.heat");
    private final int[] worldOffset = new int[3], structureOffset = new int[3];
    private Placement[] placements;
    private int[] bounds;
    private boolean ready;

    ProjectorBuildSite(ProjectorPlan plan, ProjectorStructure shape, boolean batch) {
        this.plan = plan;
        this.shape = shape;
        copies = batch ? plan.batch.machines() : 1;
        perCopy = shape.size + shape.emptyCells.length;
        controller = shape.indexOf(0, 0, 0);
        layout = batchTooLarge() ? null
            : batch ? new Layout(plan, shape, plan.facing) : new Layout(new ProjectorBatch(), shape, plan.facing);
    }

    boolean advance(long deadline) {
        if (ready) return true;
        if (layout == null) {
            ready = true;
            return true;
        }
        if (!layout.advanceSearch(deadline)) return false;
        ready = true;
        if (!layout.solve()) return true;
        placements = new Placement[copies];
        for (int copy = 0; copy < copies; copy++) placements[copy] = layout.placement(copy);
        bounds = layout.worldBounds();
        long volume = ((long) bounds[3] - bounds[0] + 1) * (bounds[4] - bounds[1] + 1) * (bounds[5] - bounds[2] + 1);
        if (volume > MAX_CELLS || outsideWorld(bounds[0], bounds[3], plan.x)
            || outsideWorld(bounds[2], bounds[5], plan.z)) {
            bounds = null;
            return true;
        }
        bounds[0] += plan.x;
        bounds[1] += plan.y;
        bounds[2] += plan.z;
        bounds[3] += plan.x;
        bounds[4] += plan.y;
        bounds[5] += plan.z;
        return true;
    }

    private static boolean outsideWorld(int min, int max, int origin) {
        return (long) min + origin < -WORLD_LIMIT || (long) max + origin >= WORLD_LIMIT;
    }

    private boolean batchTooLarge() {
        return copies > ProjectorBatch.MAX_COPIES || (long) copies * perCopy > MAX_CELLS;
    }

    boolean valid() {
        return bounds != null;
    }

    int size() {
        return perCopy * copies;
    }

    int[] bounds() {
        return bounds;
    }

    int copies() {
        return copies;
    }

    Placement placement(int copy) {
        return placements[copy];
    }

    double distanceSq(EntityPlayerMP player) {
        double dx = Math.max(0, Math.max(bounds[0] - player.posX, player.posX - bounds[3] - 1));
        double dy = Math.max(0, Math.max(bounds[1] - player.posY, player.posY - bounds[4] - 1));
        double dz = Math.max(0, Math.max(bounds[2] - player.posZ, player.posZ - bounds[5] - 1));
        return dx * dx + dy * dy + dz * dz;
    }

    boolean select(int index, Cell cell) {
        int copy = index / perCopy;
        int local = index % perCopy;
        if (!layout.ownsCell(copy, local)) return false;
        Placement placement = placements[copy];
        cell.index = index;
        cell.copy = copy;
        cell.local = local;
        cell.facing = placement.facing();
        cell.controller = local == controller;
        if (local < shape.size) {
            cell.relative[0] = shape.a[local];
            cell.relative[1] = shape.b[local];
            cell.relative[2] = shape.c[local];
            cell.block = shape.blocks[local];
            cell.item = shape.items[local];
            cell.machine = shape.machines[local];
            cell.meta = shape.metas[local];
            cell.wildcard = cell.meta == ProjectorStructure.ANY_META;
        } else {
            long packed = shape.emptyCells[local - shape.size];
            cell.relative[0] = ProjectorPlan.unpackA(packed);
            cell.relative[1] = ProjectorPlan.unpackB(packed);
            cell.relative[2] = ProjectorPlan.unpackC(packed);
            cell.block = Blocks.air;
            cell.item = null;
            cell.machine = 0;
            cell.meta = 0;
            cell.wildcard = false;
        }
        cell.facing.getWorldOffset(cell.relative, cell.offset);
        cell.x = plan.x + placement.x() + cell.offset[0];
        cell.y = plan.y + placement.y() + cell.offset[1];
        cell.z = plan.z + placement.z() + cell.offset[2];
        cell.front = ForgeDirection.UNKNOWN;
        if (local >= shape.size) return true;
        ProjectorBatchHatches.Sample edit = null;
        if (layout.hatches() != null) {
            cell.offset[0] = cell.x - plan.x;
            cell.offset[1] = cell.y - plan.y;
            cell.offset[2] = cell.z - plan.z;
            plan.facing.getOffsetABC(cell.offset, cell.batchRelative);
            edit = layout.hatches()
                .sample(
                    ProjectorPlan.pack(cell.batchRelative[0], cell.batchRelative[1], cell.batchRelative[2]),
                    local,
                    copy);
        }
        TileEntity source = null;
        if (edit != null) {
            cell.block = edit.block();
            cell.item = edit.item();
            cell.meta = edit.meta();
            cell.machine = edit.machine();
            cell.wildcard = false;
            if (cell.machine > 0) source = edit.tile();
        } else if (cell.wildcard || cell.machine > 0) {
            shape.canonical.getWorldOffset(cell.relative, cell.offset);
            int x = shape.originX + cell.offset[0], y = shape.originY + cell.offset[1],
                z = shape.originZ + cell.offset[2];
            if (cell.wildcard) cell.meta = shape.world.getBlockMetadata(x, y, z);
            if (cell.machine > 0) source = shape.world.getTileEntity(x, y, z);
        }
        if (source instanceof IGregTechTileEntity gt && gt.getFrontFacing() != ForgeDirection.UNKNOWN)
            cell.front = ProjectorOrientation.changeBasis(gt.getFrontFacing(), shape.canonical, cell.facing);
        return true;
    }

    State state(World world, Cell cell) {
        if (!loaded(world, cell.x, cell.y, cell.z)) return State.UNLOADED;
        Block actual = world.getBlock(cell.x, cell.y, cell.z);
        boolean air = actual == residualHeat || actual.isAir(world, cell.x, cell.y, cell.z);
        boolean replaceable = air || actual.isReplaceable(world, cell.x, cell.y, cell.z);
        if (cell.local >= shape.size) return air ? State.MATCHED : State.OBSTACLE;
        if (cell.block == Blocks.air && (replaceable || shape.choiceAt(cell.local) == null)) return State.MATCHED;
        if (matches(world, cell, actual)) return needsTurn(world, cell) ? State.TURN : State.MATCHED;
        return replaceable ? State.OPEN : State.OBSTACLE;
    }

    ForgeDirection hatchFront(World world, Cell cell, IGregTechTileEntity tile) {
        ForgeDirection preferred = cell.front;
        boolean validPreferred = preferred != ForgeDirection.UNKNOWN && tile.isValidFacing(preferred);
        if (validPreferred && facesAir(world, cell, preferred)) return preferred;
        for (ForgeDirection direction : ProjectorOrientation.PREFERRED_FRONTS) {
            if (direction != preferred && tile.isValidFacing(direction) && facesAir(world, cell, direction))
                return direction;
        }
        return validPreferred ? preferred : ForgeDirection.UNKNOWN;
    }

    private boolean facesAir(World world, Cell cell, ForgeDirection direction) {
        int x = cell.x + direction.offsetX, y = cell.y + direction.offsetY, z = cell.z + direction.offsetZ;
        return loaded(world, x, y, z) && world.isAirBlock(x, y, z) && !plansBlock(x, y, z);
    }

    private boolean plansBlock(int x, int y, int z) {
        if (x < bounds[0] || x > bounds[3] || y < bounds[1] || y > bounds[4] || z < bounds[2] || z > bounds[5])
            return false;
        int dx = x - plan.x, dy = y - plan.y, dz = z - plan.z;
        ProjectorBatchHatches hatches = layout.hatches();
        long key = 0;
        if (hatches != null) {
            worldOffset[0] = dx;
            worldOffset[1] = dy;
            worldOffset[2] = dz;
            plan.facing.getOffsetABC(worldOffset, structureOffset);
            key = ProjectorPlan.pack(structureOffset[0], structureOffset[1], structureOffset[2]);
        }
        // A neighbor can belong to another array copy and may not have been placed yet.
        for (int copy = 0; copy < copies; copy++) {
            Placement placement = placements[copy];
            worldOffset[0] = dx - placement.x();
            worldOffset[1] = dy - placement.y();
            worldOffset[2] = dz - placement.z();
            placement.facing()
                .getOffsetABC(worldOffset, structureOffset);
            int local = shape.indexOf(structureOffset[0], structureOffset[1], structureOffset[2]);
            if (local >= 0 && layout.ownsCell(copy, local)) {
                return hatches == null ? shape.blocks[local] != Blocks.air : !hatches.isAir(key, local, copy);
            }
        }
        return false;
    }

    boolean needsTurn(World world, Cell cell) {
        return cell.controller && cell.machine > 0
            && world.getTileEntity(cell.x, cell.y, cell.z) instanceof IGregTechTileEntity tile
            && tile.getMetaTileID() == cell.machine
            && tile.getFrontFacing() != cell.facing.getDirection();
    }

    boolean holdsStructure(World world, Cell cell) {
        if (cell.local >= shape.size || !loaded(world, cell.x, cell.y, cell.z)) return false;
        Block actual = world.getBlock(cell.x, cell.y, cell.z);
        if (actual.isAir(world, cell.x, cell.y, cell.z)) return false;
        if (cell.block == Blocks.air && (actual.isReplaceable(world, cell.x, cell.y, cell.z)
            || shape.choiceAt(cell.local) == null && shape.slotAt(cell.local) == null)) return false;
        return matches(world, cell, actual);
    }

    private boolean matches(World world, Cell cell, Block actual) {
        int meta = world.getBlockMetadata(cell.x, cell.y, cell.z);
        if (cell.machine == 0) {
            if (actual == cell.block && (meta == cell.meta || cell.wildcard)) return true;
            ProjectorStructure.Choice choice = shape.choiceAt(cell.local);
            if (choice != null && choice.channel() == null
                && choice.accepts(
                    Item.getItemFromBlock(actual),
                    ProjectorStructure.damageOf(world, actual, meta, cell.x, cell.y, cell.z)))
                return true;
        }
        TileEntity tile = world.getTileEntity(cell.x, cell.y, cell.z);
        if (!(tile instanceof IGregTechTileEntity gt)) return false;
        if (cell.machine > 0 && gt.getMetaTileID() == cell.machine) return true;
        ProjectorStructure.HatchSlot slot = shape.slotAt(cell.local);
        if (slot == null) return false;
        for (int accepted : slot.accepted()) {
            if (accepted == gt.getMetaTileID()) return true;
        }
        return false;
    }

    private static boolean loaded(World world, int x, int y, int z) {
        int chunkX = x >> 4, chunkZ = z >> 4;
        if (y < 0 || y >= world.getHeight()
            || !world.getChunkProvider()
                .chunkExists(chunkX, chunkZ))
            return false;
        var chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        return !(chunk instanceof EmptyChunk) && chunk.isChunkLoaded;
    }

    enum State {
        MATCHED,
        OPEN,
        OBSTACLE,
        TURN,
        UNLOADED
    }

    static final class Cell {

        private final int[] relative = new int[3];
        private final int[] offset = new int[3];
        private final int[] batchRelative = new int[3];
        int x, y, z, index, copy, local, meta, machine;
        private boolean wildcard;
        boolean controller;
        Block block;
        ItemStack item;
        ExtendedFacing facing;
        ForgeDirection front;

        boolean requiresBlock() {
            return block != Blocks.air;
        }
    }
}
