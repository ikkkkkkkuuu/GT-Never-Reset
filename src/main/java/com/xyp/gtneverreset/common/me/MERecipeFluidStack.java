package com.xyp.gtneverreset.common.me;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

/** A stocking hatch's temporary recipe input with an int view for Forge callers. */
public final class MERecipeFluidStack extends FluidStack {

    private long remaining;
    private int displayedAmount;

    public MERecipeFluidStack(FluidStack template, long amount) {
        super(template.getFluid(), 0, template.tag == null ? null : (NBTTagCompound) template.tag.copy());
        setRemaining(amount);
    }

    @Override
    public FluidStack copy() {
        return new MERecipeFluidStack(this, getRemaining());
    }

    public static long getAmount(FluidStack stack) {
        return stack instanceof MERecipeFluidStack stocked ? stocked.getRemaining() : stack.amount;
    }

    public static void setAmount(FluidStack stack, long amount) {
        if (stack instanceof MERecipeFluidStack stocked) stocked.setRemaining(amount);
        else stack.amount = Math.toIntExact(amount);
    }

    public static boolean hasStockedInput(FluidStack[] stacks) {
        if (stacks == null) return false;
        for (FluidStack stack : stacks) if (stack instanceof MERecipeFluidStack) return true;
        return false;
    }

    public static long addAmounts(long first, long second) {
        return second > 0 && first > Long.MAX_VALUE - second ? Long.MAX_VALUE : first + second;
    }

    private long getRemaining() {
        if (amount != displayedAmount) {
            // Record consumption by callers using the Forge int view.
            setRemaining(Math.max(0, addAmounts(remaining, (long) amount - displayedAmount)));
        }
        return remaining;
    }

    private void setRemaining(long amount) {
        remaining = amount;
        displayedAmount = (int) Math.min(Integer.MAX_VALUE, amount);
        this.amount = displayedAmount;
    }
}
