package com.xyp.gtneverreset.common.projector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.structure.IItemSource;

import gregtech.api.GregTechAPI;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;

public class ProjectorItemSource implements IItemSource {

    private static List<ItemStack> hatches;
    private static int[] machines;
    private static Item[] blockItems;

    private final List<ItemStack> recent = new ArrayList<>();

    public ItemStack first(Predicate<ItemStack> predicate) {
        for (ItemStack stack : recent) {
            if (predicate.test(stack)) return stack;
        }
        for (ItemStack stack : hatches()) {
            if (predicate.test(stack)) {
                recent.add(stack);
                return stack;
            }
        }
        ItemStack probe = new ItemStack(GregTechAPI.sBlockMachines, 1, 0);
        for (int id : machines()) {
            probe.setItemDamage(id);
            if (probe.stackTagCompound != null) probe.setTagCompound(null);
            if (predicate.test(probe)) return remember(probe);
        }
        for (Item item : blockItems()) {
            probe.func_150996_a(item);
            for (int meta = 0; meta < 16; meta++) {
                probe.setItemDamage(meta);
                if (probe.stackTagCompound != null) probe.setTagCompound(null);
                if (probe.getItemDamage() != meta) continue;
                if (predicate.test(probe)) return remember(probe);
            }
        }
        return null;
    }

    private ItemStack remember(ItemStack probe) {
        ItemStack stack = probe.copy();
        recent.add(stack);
        return stack;
    }

    @Override
    public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int count) {
        ItemStack stack = first(predicate);
        return stack == null ? Collections.emptyMap() : Collections.singletonMap(stack.copy(), count);
    }

    @Override
    public boolean takeOne(ItemStack stack, boolean simulate) {
        return true;
    }

    @Override
    public boolean takeAll(ItemStack stack, boolean simulate) {
        return true;
    }

    private static synchronized List<ItemStack> hatches() {
        if (hatches == null) {
            IntArrayList ids = new IntArrayList(ProjectorMachines.hatches());
            ids.sort(
                (IntComparator) (first, second) -> ProjectorMachines
                    .compareHatches(first, second, ProjectorPlan.DEFAULT_HATCH_TIER));
            List<ItemStack> list = new ArrayList<>();
            for (int id : ids) list.add(new ItemStack(GregTechAPI.sBlockMachines, 1, id));
            hatches = Collections.unmodifiableList(list);
        }
        return hatches;
    }

    private static synchronized int[] machines() {
        if (machines == null) {
            IntArrayList ids = new IntArrayList();
            for (int id = 1; id < GregTechAPI.METATILEENTITIES.length; id++) {
                if (GregTechAPI.METATILEENTITIES[id] != null && !ProjectorMachines.isHatch(id)) ids.add(id);
            }
            machines = ids.toIntArray();
        }
        return machines;
    }

    private static synchronized Item[] blockItems() {
        if (blockItems == null) {
            List<Item> items = new ArrayList<>();
            for (Object entry : Block.blockRegistry) {
                Item item = Item.getItemFromBlock((Block) entry);
                if (item != null && entry != GregTechAPI.sBlockMachines) items.add(item);
            }
            blockItems = items.toArray(new Item[0]);
        }
        return blockItems;
    }
}
