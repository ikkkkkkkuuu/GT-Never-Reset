package com.xyp.gtneverreset.common.projector;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElement.BlocksToPlace;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.HatchSlot;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

public class ProjectorHatchProbe {

    private final ProjectorSimulation simulation;
    private final ProjectorSimulationWorld world;

    public ProjectorHatchProbe(ProjectorSimulation simulation) {
        this.simulation = simulation;
        this.world = simulation.world;
    }

    public void findSlots() {
        AutoPlaceEnvironment placement = AutoPlaceEnvironment
            .fromLegacy(simulation.source, simulation.actor, message -> {});
        for (Long2ObjectMap.Entry<IStructureElement<Object>> entry : simulation.elements.long2ObjectEntrySet()) {
            IStructureElement<Object> element = entry.getValue();
            if (simulation.slotOfElement.containsKey(element)) continue;
            long position = entry.getLongKey();
            simulation.slotOfElement.put(
                element,
                slotOf(
                    element,
                    ProjectorPlan.unpackA(position),
                    ProjectorPlan.unpackB(position),
                    ProjectorPlan.unpackC(position),
                    placement));
        }
    }

    private int slotOf(IStructureElement<Object> element, int x, int y, int z, AutoPlaceEnvironment placement) {
        // Hatch filters only accept kinds the machine still lacks, and earlier probes registered hatches with it.
        if (simulation.machine instanceof MTEMultiBlockBase multiblock) multiblock.clearHatches();
        BlocksToPlace blocks;
        try {
            blocks = element.getBlocksToPlace(simulation.machine, world, x, y, z, simulation.trigger, placement);
        } catch (RuntimeException e) {
            return -1;
        }
        IntArrayList candidates = new IntArrayList();
        ItemStack casing = null;
        if (blocks != null) {
            Predicate<ItemStack> predicate = blocks.getPredicate();
            for (int id : ProjectorMachines.hatches()) {
                if (accepts(predicate, ProjectorMachines.stackOf(id))) candidates.add(id);
            }
            if (blocks.getStacks() != null) {
                for (ItemStack stack : blocks.getStacks()) {
                    if (stack != null && !ProjectorMachines.isHatch(ProjectorMachines.idOf(stack))) {
                        casing = stack.copy();
                        break;
                    }
                }
            }
            // Legacy optional hatch adders only offer their casing for placement but still accept hatches.
            if (candidates.isEmpty()) candidates = tryHatchesInPlace(element, x, y, z);
            if (candidates.isEmpty()) return -1;
        } else {
            if (!world.isAirBlock(x, y, z) || simulation.checks(element, x, y, z)) return -1;
            for (int id : ProjectorMachines.hatches()) {
                simulation.placeMachine(id, x, y, z);
                if (simulation.checks(element, x, y, z)) candidates.add(id);
                world.setBlockToAir(x, y, z);
            }
            if (candidates.isEmpty()) return -1;
        }
        List<String> spotKinds = describe(element);
        IntArrayList allowed = new IntArrayList(candidates.size());
        for (int id : candidates) {
            if (ProjectorMachines.allowedAt(id, spotKinds, simulation.machine)) allowed.add(id);
        }
        if (allowed.isEmpty()) return -1;
        simulation.hatchSlots.add(new HatchSlot(allowed.toIntArray(), candidates.toIntArray(), casing));
        return simulation.hatchSlots.size() - 1;
    }

    private List<String> describe(IStructureElement<Object> element) {
        try {
            List<String> description = element.getDescription(simulation.machine);
            return description == null ? Collections.emptyList() : description;
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
    }

    // Legacy hatch adders cannot place anything, so the build leaves their spots empty.
    public void fillHatchOnlySpots() {
        for (Long2ObjectMap.Entry<IStructureElement<Object>> entry : simulation.elements.long2ObjectEntrySet()) {
            long position = entry.getLongKey();
            int x = ProjectorPlan.unpackA(position), y = ProjectorPlan.unpackB(position),
                z = ProjectorPlan.unpackC(position);
            Integer slot = simulation.slotOfElement.get(entry.getValue());
            if (slot == null || slot < 0 || !world.isAirBlock(x, y, z) || simulation.checks(entry.getValue(), x, y, z))
                continue;
            simulation.placeMachine(preferred(simulation.hatchSlots.get(slot)), x, y, z);
        }
    }

    private static int preferred(HatchSlot slot) {
        int best = slot.candidates()[0];
        for (int id : slot.candidates()) {
            if (ProjectorMachines.compareHatches(id, best, ProjectorPlan.DEFAULT_HATCH_TIER) < 0) best = id;
        }
        return best;
    }

    // The build needs hatches so that spots without a casing do not stop it; the plan keeps hatches only there.
    public void replaceHatchesWithCasings() {
        for (Long2ObjectMap.Entry<IStructureElement<Object>> entry : simulation.elements.long2ObjectEntrySet()) {
            Integer slot = simulation.slotOfElement.get(entry.getValue());
            if (slot == null || slot < 0) continue;
            ItemStack casing = simulation.hatchSlots.get(slot)
                .casing();
            long position = entry.getLongKey();
            int x = ProjectorPlan.unpackA(position), y = ProjectorPlan.unpackB(position),
                z = ProjectorPlan.unpackC(position);
            if (casing == null || !(world.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile)
                || !ProjectorMachines.isHatch(tile.getMetaTileID())) continue;
            world.setBlockToAir(x, y, z);
            simulation.place(casing, x, y, z);
        }
    }

    private IntArrayList tryHatchesInPlace(IStructureElement<Object> element, int x, int y, int z) {
        IntArrayList candidates = new IntArrayList();
        Block block = world.getBlock(x, y, z);
        if (block == Blocks.air || world.getTileEntity(x, y, z) != null) return candidates;
        int meta = world.getBlockMetadata(x, y, z);
        boolean accepted = false;
        for (int id : ProjectorMachines.probeHatches(ProjectorPlan.DEFAULT_HATCH_TIER)) {
            accepted = tryHatch(element, id, x, y, z);
            if (accepted) break;
        }
        if (accepted) {
            for (int id : ProjectorMachines.hatches()) {
                if (tryHatch(element, id, x, y, z)) candidates.add(id);
            }
        }
        world.setBlock(x, y, z, block, meta, 2);
        return candidates;
    }

    private boolean tryHatch(IStructureElement<Object> element, int id, int x, int y, int z) {
        world.setBlockToAir(x, y, z);
        simulation.placeMachine(id, x, y, z);
        return simulation.checks(element, x, y, z);
    }

    private static boolean accepts(Predicate<ItemStack> predicate, ItemStack stack) {
        try {
            return stack != null && predicate.test(stack);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
