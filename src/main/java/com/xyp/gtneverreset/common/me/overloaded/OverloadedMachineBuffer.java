package com.xyp.gtneverreset.common.me.overloaded;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

/** Stores quantities separately from ItemStack's byte-sized NBT Count field. */
public final class OverloadedMachineBuffer {

    private static final String KEY_ITEMS = "Items";
    private static final String KEY_FLUID = "Fluid";
    private static final String KEY_SIDE = "Side";
    private static final String KEY_QUANTITY = "Quantity";

    private final List<ItemStack> items = new ArrayList<>();
    private final List<FluidStack> fluids = new ArrayList<>();
    private ForgeDirection side = ForgeDirection.UNKNOWN;

    public boolean isEmpty() {
        return items.isEmpty() && fluids.isEmpty();
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public List<FluidStack> getFluids() {
        return fluids;
    }

    public ForgeDirection getSide() {
        return side;
    }

    public void replace(List<ItemStack> pendingItems, List<FluidStack> pendingFluids, ForgeDirection inputSide) {
        items.clear();
        for (ItemStack item : pendingItems) if (item != null && item.stackSize > 0) items.add(item.copy());
        fluids.clear();
        for (FluidStack fluid : pendingFluids) if (fluid != null && fluid.amount > 0) fluids.add(fluid.copy());
        side = isEmpty() ? ForgeDirection.UNKNOWN : inputSide;
    }

    public void clearIfEmpty() {
        if (isEmpty()) side = ForgeDirection.UNKNOWN;
    }

    public void writeToNBT(NBTTagCompound target) {
        if (isEmpty()) {
            target.removeTag("OverloadedInputBuffer");
            return;
        }
        NBTTagCompound saved = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (ItemStack item : items) {
            NBTTagCompound entry = new NBTTagCompound();
            ItemStack identity = item.copy();
            identity.stackSize = 1;
            identity.writeToNBT(entry);
            entry.setInteger(KEY_QUANTITY, item.stackSize);
            list.appendTag(entry);
        }
        saved.setTag(KEY_ITEMS, list);
        NBTTagList fluidList = new NBTTagList();
        for (FluidStack fluid : fluids) fluidList.appendTag(fluid.writeToNBT(new NBTTagCompound()));
        saved.setTag("Fluids", fluidList);
        saved.setInteger(KEY_SIDE, side.ordinal());
        target.setTag("OverloadedInputBuffer", saved);
    }

    public void readFromNBT(NBTTagCompound source) {
        items.clear();
        fluids.clear();
        side = ForgeDirection.UNKNOWN;
        if (!source.hasKey("OverloadedInputBuffer", Constants.NBT.TAG_COMPOUND)) return;
        NBTTagCompound saved = source.getCompoundTag("OverloadedInputBuffer");
        NBTTagList list = saved.getTagList(KEY_ITEMS, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            ItemStack item = ItemStack.loadItemStackFromNBT(entry);
            int quantity = entry.getInteger(KEY_QUANTITY);
            if (item != null && quantity > 0) {
                item.stackSize = quantity;
                items.add(item);
            }
        }
        if (saved.hasKey("Fluids", Constants.NBT.TAG_LIST)) {
            NBTTagList fluidList = saved.getTagList("Fluids", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < fluidList.tagCount(); i++) {
                FluidStack fluid = FluidStack.loadFluidStackFromNBT(fluidList.getCompoundTagAt(i));
                if (fluid != null && fluid.amount > 0) fluids.add(fluid);
            }
        } else if (saved.hasKey(KEY_FLUID, Constants.NBT.TAG_COMPOUND)) {
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(saved.getCompoundTag(KEY_FLUID));
            if (fluid != null && fluid.amount > 0) fluids.add(fluid);
        }
        int ordinal = saved.getInteger(KEY_SIDE);
        if (ordinal >= 0 && ordinal < ForgeDirection.VALID_DIRECTIONS.length)
            side = ForgeDirection.VALID_DIRECTIONS[ordinal];
        clearIfEmpty();
    }
}
