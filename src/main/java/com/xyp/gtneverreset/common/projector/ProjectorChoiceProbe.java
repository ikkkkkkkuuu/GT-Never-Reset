package com.xyp.gtneverreset.common.projector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.ChannelDescription;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElement.BlocksToPlace;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Choice;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

public class ProjectorChoiceProbe {

    public static final int MAX_OPTIONS = 64, MAX_CHANNEL_PROBE = 64, CHANNEL_PROBE_MISSES = 4;

    private final ProjectorSimulation simulation;
    private final ProjectorSimulationWorld world;

    public ProjectorChoiceProbe(ProjectorSimulation simulation) {
        this.simulation = simulation;
        this.world = simulation.world;
    }

    public void findChoices() {
        Map<IStructureElement<Object>, Long> placed = new IdentityHashMap<>(), any = new IdentityHashMap<>();
        Set<IStructureElement<Object>> empty = Collections.newSetFromMap(new IdentityHashMap<>());
        List<IStructureElement<Object>> order = new ArrayList<>();
        for (Long2ObjectMap.Entry<IStructureElement<Object>> entry : simulation.elements.long2ObjectEntrySet()) {
            IStructureElement<Object> element = entry.getValue();
            long position = entry.getLongKey();
            int x = ProjectorPlan.unpackA(position), y = ProjectorPlan.unpackB(position),
                z = ProjectorPlan.unpackC(position);
            if (any.putIfAbsent(element, position) == null) order.add(element);
            if (world.isAirBlock(x, y, z)) {
                if (simulation.checks(element, x, y, z)) empty.add(element);
            } else if (!(world.getTileEntity(x, y, z) instanceof IGregTechTileEntity)) {
                placed.putIfAbsent(element, position);
            }
        }
        AutoPlaceEnvironment placement = AutoPlaceEnvironment
            .fromLegacy(simulation.source, simulation.actor, message -> {});
        for (IStructureElement<Object> element : order) {
            Choice choice = choiceOf(
                element,
                any.get(element),
                placed.get(element),
                empty.contains(element),
                placement);
            if (choice == null) continue;
            simulation.choiceOfElement.put(element, simulation.choices.size());
            simulation.choices.add(choice);
        }
    }

    private Choice choiceOf(IStructureElement<Object> element, long position, Long basePosition, boolean air,
        AutoPlaceEnvironment placement) {
        int x = ProjectorPlan.unpackA(position), y = ProjectorPlan.unpackB(position),
            z = ProjectorPlan.unpackC(position);
        ItemStack base = null;
        if (basePosition != null) {
            int bx = ProjectorPlan.unpackA(basePosition), by = ProjectorPlan.unpackB(basePosition),
                bz = ProjectorPlan.unpackC(basePosition);
            base = simulation.itemOf(world.getBlock(bx, by, bz), world.getBlockMetadata(bx, by, bz), bx, by, bz);
        }
        String channel = channelOf(element, base);
        if (channel != null) return channelChoice(element, x, y, z, base, air, channel, placement);
        List<ItemStack> blocks = new ArrayList<>();
        IntOpenHashSet seen = new IntOpenHashSet();
        if (base != null && seen.add(Choice.idOf(base.getItem(), base.getItemDamage()))) blocks.add(base);
        for (ItemStack block : placeable(element, x, y, z, simulation.trigger, placement)) {
            if (blocks.size() >= MAX_OPTIONS) break;
            if (seen.add(Choice.idOf(block.getItem(), block.getItemDamage()))) blocks.add(block);
        }
        if (blocks.size() < 2 && !(air && !blocks.isEmpty())) return null;
        return Choice.of(base, blocks, air, null, new int[0]);
    }

    // Channels such as machine_casing are shared by many machines, so the tiers come from this element alone.
    private Choice channelChoice(IStructureElement<Object> element, int x, int y, int z, ItemStack base, boolean air,
        String channel, AutoPlaceEnvironment placement) {
        List<ItemStack> blocks = new ArrayList<>();
        IntArrayList tiers = new IntArrayList();
        IntOpenHashSet seen = new IntOpenHashSet();
        for (int tier = 1, misses = 0; tier <= MAX_CHANNEL_PROBE && misses < CHANNEL_PROBE_MISSES
            && blocks.size() < MAX_OPTIONS; tier++) {
            ItemStack probe = simulation.trigger.copy();
            ChannelDataAccessor.setChannelData(probe, channel, tier);
            boolean found = false;
            for (ItemStack block : placeable(element, x, y, z, probe, placement)) {
                if (!seen.add(Choice.idOf(block.getItem(), block.getItemDamage()))) continue;
                blocks.add(block);
                tiers.add(tier);
                found = true;
            }
            misses = found ? 0 : misses + 1;
        }
        return Choice.of(base, blocks, air, channel, tiers.toIntArray());
    }

    private List<ItemStack> placeable(IStructureElement<Object> element, int x, int y, int z, ItemStack probe,
        AutoPlaceEnvironment placement) {
        List<ItemStack> blocks = new ArrayList<>();
        if (simulation.machine instanceof MTEMultiBlockBase multiblock) multiblock.clearHatches();
        BlocksToPlace offered;
        try {
            offered = element.getBlocksToPlace(simulation.machine, world, x, y, z, probe, placement);
        } catch (RuntimeException e) {
            return blocks;
        }
        if (offered == null || offered.getStacks() == null) return blocks;
        for (ItemStack stack : offered.getStacks()) {
            if (stack == null || !(stack.getItem() instanceof ItemBlock) || ProjectorMachines.idOf(stack) > 0) continue;
            ItemStack single = stack.copy();
            single.stackSize = 1;
            blocks.add(single);
        }
        return blocks;
    }

    private String channelOf(IStructureElement<Object> element, ItemStack base) {
        try {
            for (String channel : element.getChannels()) {
                if (simulation.channels.contains(channel)) return channel;
            }
        } catch (RuntimeException e) {
            return null;
        }
        if (base == null) return null;
        for (Map.Entry<String, Integer> indicated : ChannelDescription.iterate(base)) {
            if (simulation.channels.contains(indicated.getKey())) return indicated.getKey();
        }
        return null;
    }
}
