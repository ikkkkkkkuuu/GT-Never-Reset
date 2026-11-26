package com.xyp.gtneverreset.mixins.late.gregtech;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.xyp.gtneverreset.common.me.MERecipeFluidStack;

import gregtech.api.recipe.check.SingleRecipeCheck;

@Mixin(value = SingleRecipeCheck.class, remap = false)
public abstract class StockingHatchRecipeLockMixin {

    @Shadow
    @Final
    private ImmutableMap<Fluid, Integer> fluidCost;

    @WrapMethod(method = "checkRecipeInputs")
    private int gtnr$checkStockedFluids(boolean consume, int maxParallel, ItemStack[] items, FluidStack[] fluids,
        Operation<Integer> original) {
        if (!MERecipeFluidStack.hasStockedInput(fluids)) return original.call(consume, maxParallel, items, fluids);

        Map<Fluid, Long> totals = new IdentityHashMap<>();
        for (FluidStack fluid : fluids) if (fluid != null) {
            totals.merge(fluid.getFluid(), MERecipeFluidStack.getAmount(fluid), MERecipeFluidStack::addAmounts);
        }
        int possible = maxParallel;
        for (var cost : fluidCost.entrySet()) {
            possible = (int) Math.min(possible, totals.getOrDefault(cost.getKey(), 0L) / cost.getValue());
            if (possible <= 0) return 0;
        }
        int actual = original.call(consume, possible, items, fluids);
        if (consume && actual > 0) {
            Map<Fluid, Long> remaining = new IdentityHashMap<>();
            fluidCost.forEach((fluid, cost) -> remaining.put(fluid, (long) cost * actual));
            for (FluidStack stack : fluids) {
                if (stack == null) continue;
                long required = remaining.getOrDefault(stack.getFluid(), 0L);
                if (required <= 0) continue;
                long available = MERecipeFluidStack.getAmount(stack);
                if (available <= 0) continue;
                long consumed = Math.min(available, required);
                MERecipeFluidStack.setAmount(stack, available - consumed);
                remaining.put(stack.getFluid(), required - consumed);
            }
        }
        return actual;
    }

    @WrapOperation(
        method = "checkRecipeInputs",
        at = @At(
            value = "FIELD",
            opcode = Opcodes.GETFIELD,
            target = "Lgregtech/api/recipe/check/SingleRecipeCheck;totalFluidCost:I"))
    private int gtnr$skipIntFluidAccounting(SingleRecipeCheck check, Operation<Integer> original,
        @Local(argsOnly = true) FluidStack[] fluids) {
        return MERecipeFluidStack.hasStockedInput(fluids) ? 0 : original.call(check);
    }

    @WrapOperation(
        method = "buildFluidMap",
        at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraftforge/fluids/FluidStack;amount:I"))
    private static int gtnr$snapshotRecipeLockCost(FluidStack stack, Operation<Integer> original) {
        // The builder subtracts the before and after totals. The low bits preserve that per-recipe difference.
        return stack instanceof MERecipeFluidStack ? (int) MERecipeFluidStack.getAmount(stack) : original.call(stack);
    }
}
