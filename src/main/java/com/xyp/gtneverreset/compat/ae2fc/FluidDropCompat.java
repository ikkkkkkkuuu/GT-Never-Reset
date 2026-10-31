package com.xyp.gtneverreset.compat.ae2fc;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.common.item.ItemFluidDrop;

public final class FluidDropCompat {

    public static boolean isFluidDrop(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemFluidDrop;
    }

    @Nullable
    public static FluidStack getFluidStack(@Nullable ItemStack stack) {
        return ItemFluidDrop.getFluidStack(stack);
    }

    public static ItemStack newStack(FluidStack fluid) {
        return ItemFluidDrop.newStack(fluid);
    }
}
