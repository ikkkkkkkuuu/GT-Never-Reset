package com.xyp.gtneverreset.utils.recipes.wildcard.model.io;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.common.item.ItemFluidDrop;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.IWildcardIOComponent;

import gregtech.api.enums.Materials;
import lombok.Getter;

public final class SimpleIOComponent implements IWildcardIOComponent {

    public static final String TYPE = "simple";

    private static final String KEY_STACK = "Stack";
    private static final String KEY_AMOUNT = "Amount";

    private ItemStack stack;
    @Getter
    private long amount;

    public SimpleIOComponent(ItemStack stack, long amount) {
        this.stack = stack == null ? null : stack.copy();
        this.amount = Math.max(1L, amount);
    }

    public static SimpleIOComponent empty() {
        return new SimpleIOComponent(null, 1L);
    }

    public static SimpleIOComponent readData(NBTTagCompound data) {
        ItemStack stack = data.hasKey(KEY_STACK) ? ItemStack.loadItemStackFromNBT(data.getCompoundTag(KEY_STACK))
            : null;
        long amount = Math.max(1L, data.getLong(KEY_AMOUNT));
        return new SimpleIOComponent(stack, amount);
    }

    /** 放入的物品（GT 流体显示物品应在放入前转成 ItemFluidDrop）。设置时用其自带数量初始化 amount。 */
    public void setStack(ItemStack stack) {
        this.stack = stack == null ? null : stack.copy();
        if (stack != null) {
            FluidStack fluid = ItemFluidDrop.getFluidStack(stack);
            if (fluid != null) {
                this.amount = Math.max(1L, fluid.amount);
            } else {
                this.amount = Math.max(1L, stack.stackSize);
            }
        }
    }

    public ItemStack getStack() {
        return stack == null ? null : stack.copy();
    }

    public void setAmount(long amount) {
        this.amount = Math.max(1L, amount);
    }

    /** 是否为流体（ItemFluidDrop）。 */
    public boolean isFluid() {
        return stack != null && ItemFluidDrop.getFluidStack(stack) != null;
    }

    @Override
    public ItemStack apply(Materials material) {
        if (stack == null) return null;
        FluidStack fluid = ItemFluidDrop.getFluidStack(stack);
        if (fluid != null) {
            FluidStack copy = fluid.copy();
            copy.amount = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, amount));
            ItemStack result = ItemFluidDrop.newStack(copy);
            if (result != null && copy.tag != null) result.getTagCompound()
                .setTag("FluidTag", copy.tag.copy());
            return result;
        }
        ItemStack result = stack.copy();
        result.stackSize = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, amount));
        return result;
    }

    @Override
    public ItemStack apply(String materialName) {
        return apply((Materials) null);
    }

    @Override
    public ItemStack getDisplayStack() {
        return stack == null ? null : stack.copy();
    }

    @Override
    public boolean isEmpty() {
        return stack == null;
    }

    @Override
    public String typeKey() {
        return TYPE;
    }

    @Override
    public NBTTagCompound writeData() {
        NBTTagCompound data = new NBTTagCompound();
        if (stack != null) {
            NBTTagCompound stackTag = new NBTTagCompound();
            stack.writeToNBT(stackTag);
            data.setTag(KEY_STACK, stackTag);
        }
        data.setLong(KEY_AMOUNT, amount);
        return data;
    }
}
