package com.xyp.gtneverreset.common.crafting;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadOutputs.FluidOutputRouter;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadTask;
import com.xyp.gtneverreset.common.recipe.gtneverreset.PetrochemicalPlantRecipes;

public final class PetrochemicalFluidBuffer implements FluidOutputRouter {

    private static final int CAPACITY_PER_FLUID = 64_000;

    private final Map<Fluid, FluidStack> fluids = new LinkedHashMap<>();

    public void prependInputs(List<FluidStack> inputs) {
        inputs.addAll(
            0,
            fluids.values()
                .stream()
                .filter(stack -> stack.amount > 0)
                .toList());
    }

    public int limitParallels(FluidStack[] outputs, List<RecipeThreadTask> inFlight) {
        Map<Fluid, Long> amounts = new HashMap<>();
        for (FluidStack stack : outputs) {
            if (routes(stack) && stack.amount > 0) amounts.merge(stack.getFluid(), (long) stack.amount, Long::sum);
        }
        int limit = Integer.MAX_VALUE;
        for (var output : amounts.entrySet()) {
            int remaining = freeSpace(output.getKey());
            FluidStack fluid = new FluidStack(output.getKey(), 1);
            for (RecipeThreadTask task : inFlight) {
                remaining -= task.getOutputs()
                    .getFluidAmount(fluid)
                    .min(BigInteger.valueOf(remaining))
                    .intValueExact();
                if (remaining == 0) break;
            }
            limit = Math.min(limit, (int) (remaining / output.getValue()));
        }
        return limit;
    }

    @Override
    public boolean routes(FluidStack stack) {
        return stack != null && PetrochemicalPlantRecipes.isIntermediate(stack);
    }

    @Override
    public boolean canStore(List<FluidStack> stacks) {
        Map<Fluid, Long> amounts = new HashMap<>();
        for (FluidStack stack : stacks) {
            Fluid fluid = stack.getFluid();
            if (amounts.merge(fluid, (long) stack.amount, Long::sum) > freeSpace(fluid)) return false;
        }
        return true;
    }

    @Override
    public void store(List<FluidStack> stacks) {
        for (FluidStack stack : stacks) {
            FluidStack stored = fluids.get(stack.getFluid());
            if (stored == null) fluids.put(stack.getFluid(), stack.copy());
            else stored.amount += stack.amount;
        }
    }

    public NBTTagList save() {
        NBTTagList data = new NBTTagList();
        for (FluidStack stack : fluids.values()) {
            if (stack.amount > 0) data.appendTag(stack.writeToNBT(new NBTTagCompound()));
        }
        return data;
    }

    public void load(NBTTagList data) {
        fluids.clear();
        for (int i = 0; i < data.tagCount(); i++) {
            FluidStack stack = FluidStack.loadFluidStackFromNBT(data.getCompoundTagAt(i));
            if (routes(stack) && stack.amount > 0) fluids.putIfAbsent(stack.getFluid(), stack);
        }
    }

    private int freeSpace(Fluid fluid) {
        FluidStack stored = fluids.get(fluid);
        return stored == null ? CAPACITY_PER_FLUID : Math.max(0, CAPACITY_PER_FLUID - stored.amount);
    }
}
