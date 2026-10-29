package com.xyp.gtneverreset.utils.machine;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTUtility;
import gregtech.api.util.ItemEjectionHelper;

/** Compact output ledger: stack objects describe types, while quantities never pass through int or long. */
public final class RecipeThreadOutputs {

    final ItemStack[] items;
    final FluidStack[] fluids;
    final BigInteger[] itemAmounts;
    final BigInteger[] fluidAmounts;

    public RecipeThreadOutputs(ItemStack[] items, FluidStack[] fluids) {
        this.items = items == null ? new ItemStack[0] : new ItemStack[items.length];
        this.fluids = fluids == null ? new FluidStack[0] : new FluidStack[fluids.length];
        itemAmounts = new BigInteger[this.items.length];
        fluidAmounts = new BigInteger[this.fluids.length];
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = items[i] == null ? null : items[i].copy();
            itemAmounts[i] = BigInteger.valueOf(items[i] == null ? 0 : Math.max(0, items[i].stackSize));
            if (this.items[i] != null) this.items[i].stackSize = 1;
        }
        for (int i = 0; i < this.fluids.length; i++) {
            this.fluids[i] = fluids[i] == null ? null : fluids[i].copy();
            fluidAmounts[i] = BigInteger.valueOf(fluids[i] == null ? 0 : Math.max(0, fluids[i].amount));
            if (this.fluids[i] != null) this.fluids[i].amount = 1;
        }
    }

    private BigInteger largest() {
        BigInteger largest = BigInteger.ZERO;
        for (BigInteger n : itemAmounts) largest = largest.max(n);
        for (BigInteger n : fluidAmounts) largest = largest.max(n);
        return largest;
    }

    /** Adds detached stacks for the native GUI; the output ledger remains authoritative. */
    public void appendDisplayItems(List<ItemStack> itemOutputs) {
        BigInteger stackLimit = BigInteger.valueOf(Integer.MAX_VALUE);
        for (int i = 0; i < items.length; i++) {
            for (BigInteger remaining = itemAmounts[i]; remaining.signum() > 0;) {
                ItemStack stack = items[i].copy();
                stack.stackSize = remaining.min(stackLimit)
                    .intValueExact();
                itemOutputs.add(stack);
                remaining = remaining.subtract(BigInteger.valueOf(stack.stackSize));
            }
        }
    }

    public void appendDisplayFluids(List<FluidStack> fluidOutputs) {
        BigInteger stackLimit = BigInteger.valueOf(Integer.MAX_VALUE);
        for (int i = 0; i < fluids.length; i++) {
            for (BigInteger remaining = fluidAmounts[i]; remaining.signum() > 0;) {
                FluidStack stack = fluids[i].copy();
                stack.amount = remaining.min(stackLimit)
                    .intValueExact();
                fluidOutputs.add(stack);
                remaining = remaining.subtract(BigInteger.valueOf(stack.amount));
            }
        }
    }

    /**
     * Delivers a bounded slice using native transactions. A blocked fluid output cannot commit only the items.
     * Remaining amounts are reduced only after both output transactions commit, and persist in the task NBT.
     */
    public boolean flush(MTEMultiBlockBase machine) {
        BigInteger largest = largest();
        if (largest.signum() == 0) return true;
        int high = largest.min(BigInteger.valueOf(Integer.MAX_VALUE))
            .intValueExact();
        if (transfer(machine, largest, high, false)) {
            transfer(machine, largest, high, true);
            return largest().signum() == 0;
        }
        if (high == 1 || !transfer(machine, largest, 1, false)) return false;
        int low = 1;
        high--;
        while (low < high) {
            int middle = (int) (((long) low + high + 1) / 2);
            if (transfer(machine, largest, middle, false)) low = middle;
            else high = middle - 1;
        }
        if (low > 0) transfer(machine, largest, low, true);
        return largest().signum() == 0;
    }

    private static int slice(BigInteger amount, BigInteger largest, int units) {
        if (amount.signum() == 0) return 0;
        return amount.multiply(BigInteger.valueOf(units))
            .divide(largest)
            .max(BigInteger.ONE)
            .intValueExact();
    }

    private boolean transfer(MTEMultiBlockBase machine, BigInteger largest, int units, boolean commit) {
        ItemEjectionHelper itemHelper = new ItemEjectionHelper(machine.getOutputBusses(), machine.protectsExcessItem());
        FluidEjectionHelper fluidHelper = new FluidEjectionHelper(
            machine.getOutputHatches(),
            machine.protectsExcessFluid());
        int[] itemSlice = new int[items.length];
        int[] fluidSlice = new int[fluids.length];
        for (int i = 0; i < items.length; i++) {
            itemSlice[i] = slice(itemAmounts[i], largest, units);
            if (itemSlice[i] > 0
                && itemHelper.ejectItems(Collections.singletonList(items[i]), itemSlice[i]) != itemSlice[i])
                return false;
        }
        for (int i = 0; i < fluids.length; i++) {
            fluidSlice[i] = slice(fluidAmounts[i], largest, units);
            if (fluidSlice[i] > 0
                && fluidHelper.ejectFluids(Collections.singletonList(fluids[i]), fluidSlice[i]) != fluidSlice[i])
                return false;
        }
        if (commit) {
            itemHelper.commit();
            fluidHelper.commit();
            for (int i = 0; i < items.length; i++)
                itemAmounts[i] = itemAmounts[i].subtract(BigInteger.valueOf(itemSlice[i]));
            for (int i = 0; i < fluids.length; i++)
                fluidAmounts[i] = fluidAmounts[i].subtract(BigInteger.valueOf(fluidSlice[i]));
            machine.markDirty();
        }
        return true;
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList itemTags = new NBTTagList();
        for (int i = 0; i < items.length; i++) if (items[i] != null && itemAmounts[i].signum() > 0) {
            NBTTagCompound entry = new NBTTagCompound();
            GTUtility.saveItem(entry, "stack", items[i]);
            entry.setString("amount", itemAmounts[i].toString());
            itemTags.appendTag(entry);
        }
        tag.setTag("items", itemTags);
        NBTTagList fluidTags = new NBTTagList();
        for (int i = 0; i < fluids.length; i++) if (fluids[i] != null && fluidAmounts[i].signum() > 0) {
            NBTTagCompound entry = fluids[i].writeToNBT(new NBTTagCompound());
            entry.setString("bigAmount", fluidAmounts[i].toString());
            fluidTags.appendTag(entry);
        }
        tag.setTag("fluids", fluidTags);
        return tag;
    }

    public static RecipeThreadOutputs load(NBTTagCompound tag) {
        NBTTagList itemTags = tag.getTagList("items", 10), fluidTags = tag.getTagList("fluids", 10);
        ItemStack[] items = new ItemStack[itemTags.tagCount()];
        FluidStack[] fluids = new FluidStack[fluidTags.tagCount()];
        for (int i = 0; i < items.length; i++) items[i] = GTUtility.loadItem(itemTags.getCompoundTagAt(i), "stack");
        for (int i = 0; i < fluids.length; i++)
            fluids[i] = FluidStack.loadFluidStackFromNBT(fluidTags.getCompoundTagAt(i));
        RecipeThreadOutputs result = new RecipeThreadOutputs(items, fluids);
        for (int i = 0; i < items.length; i++) if (itemTags.getCompoundTagAt(i)
            .hasKey("amount", 8))
            result.itemAmounts[i] = new BigInteger(
                itemTags.getCompoundTagAt(i)
                    .getString("amount"));
        for (int i = 0; i < fluids.length; i++) if (fluidTags.getCompoundTagAt(i)
            .hasKey("bigAmount", 8))
            result.fluidAmounts[i] = new BigInteger(
                fluidTags.getCompoundTagAt(i)
                    .getString("bigAmount"));
        return result;
    }
}
