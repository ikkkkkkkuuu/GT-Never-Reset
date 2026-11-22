package com.xyp.gtneverreset.common.projector;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.bsideup.jabel.Desugar;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public final class ProjectorBatchHatches {

    public static final int SAMPLE_Y = 64;

    @Desugar
    public record Sample(ItemStack item, int machine, Block block, int meta, ProjectorSimulationWorld world, int x) {

        public TileEntity tile() {
            return world.getTileEntity(x, SAMPLE_Y, 0);
        }
    }

    private final ProjectorStructure shape;
    private final ProjectorPlan plan;
    private final Long2ObjectOpenHashMap<Sample> cells = new Long2ObjectOpenHashMap<>();
    private final ProjectorSimulationWorld world = new ProjectorSimulationWorld();
    private final ProjectorSimulation.Actor actor = new ProjectorSimulation.Actor(world);
    private final Long2IntOpenHashMap requested;
    public boolean incompatible;

    public ProjectorBatchHatches(ProjectorStructure shape, ProjectorPlan plan) {
        this.shape = shape;
        this.plan = plan;
        requested = new Long2IntOpenHashMap(plan.batchOverrides);
        requested.defaultReturnValue(-1);
    }

    public Sample sample(long key, int cell) {
        int wanted = requested.get(key);
        if (wanted < 0) return null;
        var slot = cell < shape.size ? shape.slotAt(cell) : null;
        if (slot == null || (wanted != ProjectorPlan.CASING && !contains(slot.candidates(), wanted))) {
            incompatible = true;
            return null;
        }
        ItemStack item;
        if (wanted == ProjectorPlan.CASING) {
            item = slot.casing();
            var choice = shape.choiceAt(cell);
            if (item != null && choice != null) {
                String chosen = plan.cellChoices.getOrDefault(
                    ProjectorPlan.pack(shape.a[cell], shape.b[cell], shape.c[cell]),
                    plan.chosenBlock(choice));
                ItemStack alternative = choice.find(chosen);
                if (alternative != null) item = alternative;
            }
        } else item = ProjectorMachines.stackOf(wanted);
        if (item == null || !(item.getItem() instanceof ItemBlock blockItem)) {
            incompatible = true;
            return null;
        }
        Sample existing = cells.get(key);
        if (existing != null) {
            if (!existing.item()
                .isItemEqual(item) || !ItemStack.areItemStackTagsEqual(existing.item(), item)) incompatible = true;
            return existing;
        }
        int x = cells.size() * 2;
        if (!blockItem.placeBlockAt(
            item.copy(),
            actor,
            world,
            x,
            SAMPLE_Y,
            0,
            1,
            0.5f,
            0.5f,
            0.5f,
            blockItem.getMetadata(item.getItemDamage()))) {
            incompatible = true;
            return null;
        }
        if (wanted != 0) {
            if (!(world.getTileEntity(x, SAMPLE_Y, 0) instanceof IGregTechTileEntity tile)
                || tile.getMetaTileID() != wanted) {
                incompatible = true;
                return null;
            }
            tile.setFrontFacing(ForgeDirection.getOrientation(shape.hatchFront[cell]));
        }
        Sample sample = new Sample(
            item,
            wanted,
            world.getBlock(x, SAMPLE_Y, 0),
            world.getBlockMetadata(x, SAMPLE_Y, 0),
            world,
            x);
        cells.put(key, sample);
        return sample;
    }

    public Sample get(long key) {
        return cells.get(key);
    }

    private static boolean contains(int[] candidates, int id) {
        for (int candidate : candidates) if (candidate == id) return true;
        return false;
    }
}
