package com.xyp.gtneverreset.mixins.late.gregtech;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalLongRef;
import com.xyp.gtneverreset.common.me.MERecipeFluidStack;
import com.xyp.gtnrlib.utils.LongMath;

import gregtech.api.util.GTRecipe;
import it.unimi.dsi.fastutil.longs.LongBinaryOperator;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;

@Mixin(value = GTRecipe.class, remap = false)
public abstract class StockingHatchRecipeMixin {

    @WrapOperation(
        method = "maxParallelCalculatedByInputs",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/objects/Reference2LongMap;mergeLong(Ljava/lang/Object;JLit/unimi/dsi/fastutil/longs/LongBinaryOperator;)J",
            ordinal = 0))
    private long gtnr$countAvailableFluids(Reference2LongMap<Fluid> amounts, Object fluid, long amount,
        LongBinaryOperator merger, Operation<Long> original, @Local FluidStack input) {
        return original
            .call(amounts, fluid, MERecipeFluidStack.getAmount(input), (LongBinaryOperator) LongMath::saturatedAdd);
    }

    @WrapOperation(
        method = "consumeInput",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/fluids/FluidStack;isFluidEqual(Lnet/minecraftforge/fluids/FluidStack;)Z",
            ordinal = 0))
    private boolean gtnr$countAvailableAlternative(FluidStack input, FluidStack alternative,
        Operation<Boolean> original, @Local(index = 13) LocalLongRef totalAvailable) {
        if (!original.call(input, alternative)) return false;
        long available = totalAvailable.get();
        if (!(input instanceof MERecipeFluidStack) && (input.amount <= 0 || available <= Long.MAX_VALUE - input.amount))
            return true;
        totalAvailable.set(LongMath.saturatedAdd(available, MERecipeFluidStack.getAmount(input)));
        return false;
    }

    @WrapOperation(
        method = "consumeInput",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/fluids/FluidStack;isFluidEqual(Lnet/minecraftforge/fluids/FluidStack;)Z",
            ordinal = 1))
    private boolean gtnr$consumeStockedFluid(FluidStack input, FluidStack alternative, Operation<Boolean> original,
        @Local(index = 12) LocalLongRef remainingCost) {
        boolean matches = original.call(input, alternative);
        if (!matches || !(input instanceof MERecipeFluidStack)) return matches;
        long available = MERecipeFluidStack.getAmount(input);
        long consumed = Math.min(available, remainingCost.get());
        MERecipeFluidStack.setAmount(input, available - consumed);
        remainingCost.set(remainingCost.get() - consumed);
        return false;
    }
}
