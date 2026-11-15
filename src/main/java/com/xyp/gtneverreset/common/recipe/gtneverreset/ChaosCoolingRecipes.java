package com.xyp.gtneverreset.common.recipe.gtneverreset;

import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;

import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.util.GTRecipe;
import gtnhlanth.common.register.WerkstoffMaterialPool;

public final class ChaosCoolingRecipes {

    public static void register() {
        for (GTRecipe recipe : GoodGeneratorRecipeMaps.extremeHeatExchangerFuels.getAllRecipes()) {
            if (recipe.mFluidInputs.length < 1 || recipe.mFluidOutputs.length < 3
                || recipe.mFluidInputs[0] == null
                || recipe.mFluidOutputs[2] == null) continue;
            add(recipe.mFluidInputs[0], recipe.mFluidOutputs[2]);
        }
        add(WerkstoffMaterialPool.HotSuperCoolant.getFluidOrGas(1), Materials.SuperCoolant.getFluid(1));
        add(Materials.Creon.getPlasma(1), Materials.Creon.getMolten(1));
    }

    private static void add(FluidStack input, FluidStack output) {
        FluidStack hot = input.copy();
        FluidStack cold = output.copy();
        hot.amount = cold.amount = 1;
        GTValues.RA.stdBuilder()
            .fluidInputs(hot)
            .fluidOutputs(cold)
            .duration(20)
            .eut(0)
            .addTo(GTNRRecipeMaps.RapidCoolingDownRecipeMap);
    }
}
