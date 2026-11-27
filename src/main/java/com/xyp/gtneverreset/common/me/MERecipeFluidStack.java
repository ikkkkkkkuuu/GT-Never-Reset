package com.xyp.gtneverreset.common.me;

import java.util.function.ToLongFunction;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnrlib.utils.LongMath;

/** A temporary ME recipe input with an int view for Forge callers. */
public final class MERecipeFluidStack extends FluidStack {

    private final long offered;
    private final ToLongFunction<MERecipeFluidStack> currentOffer;
    private long remaining;
    private int displayedAmount;

    public MERecipeFluidStack(FluidStack template, long amount, ToLongFunction<MERecipeFluidStack> currentOffer) {
        super(template.getFluid(), 0, template.tag == null ? null : (NBTTagCompound) template.tag.copy());
        offered = amount;
        this.currentOffer = currentOffer;
        setRemaining(amount);
    }

    public long getConsumedAmount() {
        return offered - getRemaining();
    }

    public long getCurrentOffer() {
        return currentOffer == null ? getRemaining() : currentOffer.applyAsLong(this);
    }

    @Override
    public FluidStack copy() {
        return new MERecipeFluidStack(this, getRemaining(), null);
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

    private long getRemaining() {
        if (amount != displayedAmount) {
            // Record consumption by callers using the Forge int view.
            setRemaining(Math.max(0, LongMath.saturatedAdd(remaining, (long) amount - displayedAmount)));
        }
        return remaining;
    }

    private void setRemaining(long amount) {
        remaining = amount;
        displayedAmount = (int) Math.min(Integer.MAX_VALUE, amount);
        this.amount = displayedAmount;
    }
}
