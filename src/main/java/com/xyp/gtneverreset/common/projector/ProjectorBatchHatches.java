package com.xyp.gtneverreset.common.projector;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.bsideup.jabel.Desugar;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.HatchSlot;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
    private final Int2ObjectOpenHashMap<Sample> originSamples = new Int2ObjectOpenHashMap<>();
    private final ProjectorSimulationWorld world = new ProjectorSimulationWorld();
    private final ProjectorSimulation.Actor actor = new ProjectorSimulation.Actor(world);
    private final Long2IntOpenHashMap requested, originOverrides;
    private final int origin;
    private int nextSample;
    public boolean incompatible;

    ProjectorBatchHatches(ProjectorStructure shape, ProjectorPlan plan, int origin) {
        this.shape = shape;
        this.plan = plan;
        this.origin = origin;
        requested = new Long2IntOpenHashMap(plan.batchOverrides);
        requested.defaultReturnValue(-1);
        originOverrides = plan.batch.copyHatches ? new Long2IntOpenHashMap() : new Long2IntOpenHashMap(plan.overrides);
        originOverrides.defaultReturnValue(-1);
    }

    Sample sample(long key, int cell, int copy) {
        int wanted = requested.get(key);
        if (wanted < 0) {
            Sample originSample = copy == origin ? originEdit(cell) : null;
            if (originSample != null) cells.put(key, originSample);
            return originSample;
        }
        var slot = cell < shape.size ? shape.slotAt(cell) : null;
        if (!accepts(slot, wanted)) {
            incompatible = true;
            return null;
        }
        ItemStack item = itemOf(wanted, cell, slot);
        if (item == null) {
            incompatible = true;
            return null;
        }
        Sample existing = cells.get(key);
        if (existing != null) {
            if (!existing.item()
                .isItemEqual(item) || !ItemStack.areItemStackTagsEqual(existing.item(), item)) incompatible = true;
            return existing;
        }
        Sample sample = placeSample(wanted, cell, item);
        if (sample != null) cells.put(key, sample);
        return sample;
    }

    Sample originEdit(int cell) {
        if (originOverrides.isEmpty() || cell >= shape.size) return null;
        long key = ProjectorPlan.pack(shape.a[cell], shape.b[cell], shape.c[cell]);
        int wanted = originOverrides.get(key);
        if (wanted < 0 || requested.containsKey(key)) return null;
        Sample existing = originSamples.get(cell);
        if (existing != null) return existing;
        HatchSlot slot = shape.slotAt(cell);
        if (!accepts(slot, wanted)) return null;
        ItemStack item = itemOf(wanted, cell, slot);
        if (item == null) return null;
        var choice = shape.choiceAt(cell);
        Sample sample = wanted == ProjectorPlan.CASING && choice != null
            && choice.air()
            && ProjectorPlan.AIR.equals(plan.cellChoices.get(key)) ? new Sample(null, wanted, Blocks.air, 0, world, -2)
                : placeSample(wanted, cell, item);
        if (sample != null) originSamples.put(cell, sample);
        return sample;
    }

    public Sample get(long key) {
        return cells.get(key);
    }

    private ItemStack itemOf(int wanted, int cell, HatchSlot slot) {
        if (wanted != ProjectorPlan.CASING) return ProjectorMachines.stackOf(wanted);
        var choice = shape.choiceAt(cell);
        if (slot.casing() != null && choice != null) {
            String chosen = plan.cellChoices.getOrDefault(
                ProjectorPlan.pack(shape.a[cell], shape.b[cell], shape.c[cell]),
                plan.chosenBlock(choice));
            ItemStack alternative = choice.find(chosen);
            if (alternative != null) return alternative;
        }
        return slot.casing();
    }

    private Sample placeSample(int wanted, int cell, ItemStack item) {
        if (!(item.getItem() instanceof ItemBlock blockItem)) {
            incompatible = true;
            return null;
        }
        int x = nextSample++ * 2;
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
        return new Sample(
            item,
            wanted,
            world.getBlock(x, SAMPLE_Y, 0),
            world.getBlockMetadata(x, SAMPLE_Y, 0),
            world,
            x);
    }

    private static boolean accepts(HatchSlot slot, int wanted) {
        return slot != null && (wanted == ProjectorPlan.CASING || contains(slot.candidates(), wanted));
    }

    private static boolean contains(int[] candidates, int id) {
        for (int candidate : candidates) if (candidate == id) return true;
        return false;
    }
}
