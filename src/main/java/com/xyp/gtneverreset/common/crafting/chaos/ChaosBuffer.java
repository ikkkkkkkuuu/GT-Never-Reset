package com.xyp.gtneverreset.common.crafting.chaos;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;

import gregtech.api.util.GTUtility;

/** Group-owned material ledger. Prototypes never carry the authoritative quantity. */
public final class ChaosBuffer {

    public static final int MAX_TYPES = 256;
    private final List<Entry> entries = new ArrayList<>();

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public long amount() {
        long sum = 0;
        for (Entry entry : entries) sum = Math.addExact(sum, entry.amount);
        return sum;
    }

    public int types() {
        return entries.size();
    }

    public ChaosBuffer copy() {
        return load(save());
    }

    public void add(ItemStack stack, long amount) {
        if (stack != null && amount > 0) add(new Entry(stack.copy(), null, amount));
    }

    public void add(FluidStack stack, long amount) {
        if (stack != null && amount > 0) add(new Entry(null, stack.copy(), amount));
    }

    public void add(ChaosBuffer other) {
        for (Entry entry : other.entries) {
            if (entry.item != null) add(entry.item, entry.amount);
            else add(entry.fluid, entry.amount);
        }
    }

    public void add(ItemStack[] items, FluidStack[] fluids) {
        if (items != null) for (ItemStack stack : items) if (stack != null) add(stack, stack.stackSize);
        if (fluids != null) for (FluidStack stack : fluids) if (stack != null) add(stack, stack.amount);
    }

    private void add(Entry addition) {
        for (Entry entry : entries) if (entry.sameType(addition)) {
            entry.amount = Math.addExact(entry.amount, addition.amount);
            return;
        }
        if (entries.size() >= MAX_TYPES) throw new IllegalArgumentException("too many cached material types");
        if (addition.item != null) addition.item.stackSize = 1;
        else addition.fluid.amount = 1;
        entries.add(addition);
    }

    public View view() {
        return new View(this);
    }

    public void removeSlice(View view) {
        for (int i = 0; i < view.items.length; i++) subtract(view.items[i], null, view.itemAmounts[i]);
        for (int i = 0; i < view.fluids.length; i++) subtract(null, view.fluids[i], view.fluidAmounts[i]);
    }

    private void subtract(ItemStack item, FluidStack fluid, long amount) {
        Entry key = new Entry(item, fluid, amount);
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (!entry.sameType(key)) continue;
            if (amount < 0 || amount > entry.amount) throw new IllegalArgumentException("invalid cache debit");
            entry.amount -= amount;
            if (entry.amount == 0) entries.remove(i);
            return;
        }
        if (amount != 0) throw new IllegalArgumentException("missing cached material");
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (Entry entry : entries) {
            NBTTagCompound data = new NBTTagCompound();
            if (entry.item != null) GTUtility.saveItem(data, "item", entry.item);
            else data.setTag("fluid", entry.fluid.writeToNBT(new NBTTagCompound()));
            data.setLong("amount", entry.amount);
            list.appendTag(data);
        }
        tag.setTag("entries", list);
        return tag;
    }

    public static ChaosBuffer load(NBTTagCompound tag) {
        ChaosBuffer buffer = new ChaosBuffer();
        NBTTagList list = tag.getTagList("entries", 10);
        if (list.tagCount() > MAX_TYPES) throw new IllegalArgumentException("oversized material cache");
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound data = list.getCompoundTagAt(i);
            long amount = data.getLong("amount");
            if (amount <= 0) throw new IllegalArgumentException("invalid material quantity");
            if (data.hasKey("item")) {
                ItemStack stack = GTUtility.loadItem(data, "item");
                if (stack == null) throw new IllegalArgumentException("unknown cached item");
                buffer.add(stack, amount);
            } else {
                FluidStack stack = FluidStack.loadFluidStackFromNBT(data.getCompoundTag("fluid"));
                if (stack == null) throw new IllegalArgumentException("unknown cached fluid");
                buffer.add(stack, amount);
            }
        }
        buffer.amount();
        return buffer;
    }

    public static final class View {

        public final ItemStack[] items;
        public final FluidStack[] fluids;
        private final int[] itemAmounts;
        private final int[] fluidAmounts;

        private View(ChaosBuffer buffer) {
            List<ItemStack> itemList = new ArrayList<>();
            List<FluidStack> fluidList = new ArrayList<>();
            for (Entry entry : buffer.entries) {
                int amount = (int) Math.min(Integer.MAX_VALUE, entry.amount);
                if (entry.item != null) {
                    ItemStack stack = entry.item.copy();
                    stack.stackSize = amount;
                    itemList.add(stack);
                } else {
                    FluidStack stack = entry.fluid.copy();
                    stack.amount = amount;
                    fluidList.add(stack);
                }
            }
            items = itemList.toArray(new ItemStack[0]);
            fluids = fluidList.toArray(new FluidStack[0]);
            itemAmounts = itemList.stream()
                .mapToInt(stack -> stack.stackSize)
                .toArray();
            fluidAmounts = fluidList.stream()
                .mapToInt(stack -> stack.amount)
                .toArray();
        }

        public ChaosBuffer after(ChaosBuffer source, RecipeThreadProcessingLogic plan) {
            ChaosBuffer result = source.copy();
            for (ItemStack item : items) result.subtract(item, null, plan.getPlannedConsumption(item));
            for (FluidStack fluid : fluids) result.subtract(null, fluid, plan.getPlannedConsumption(fluid));
            return result;
        }
    }

    private static final class Entry {

        private final ItemStack item;
        private final FluidStack fluid;
        private long amount;

        private Entry(ItemStack item, FluidStack fluid, long amount) {
            this.item = item;
            this.fluid = fluid;
            this.amount = amount;
        }

        private boolean sameType(Entry other) {
            return item != null
                ? other.item != null && item.isItemEqual(other.item)
                    && ItemStack.areItemStackTagsEqual(item, other.item)
                : other.fluid != null && fluid.isFluidEqual(other.fluid);
        }
    }
}
