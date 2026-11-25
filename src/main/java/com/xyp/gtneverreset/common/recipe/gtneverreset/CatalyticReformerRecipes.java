package com.xyp.gtneverreset.common.recipe.gtneverreset;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTUtility;
import gtPlusPlus.core.fluids.GTPPFluids;
import gtPlusPlus.core.material.MaterialsElements;

public final class CatalyticReformerRecipes {

    private static final int FEEDSTOCK_AMOUNT = 1_000;
    private static final int DURATION_TICKS = 6 * 20;

    private CatalyticReformerRecipes() {}

    public static void register() {
        ItemStack platinum = GTUtility.copyAmount(0, Materials.Platinum.getPlates(1));
        ItemStack rhenium = GTUtility.copyAmount(0, MaterialsElements.getInstance().RHENIUM.getPlate(1));
        ItemStack zsm5 = GTUtility.copyAmount(0, GTNRItemList.Zsm5Plate.get(1));

        registerFeedstock(
            platinum,
            rhenium,
            zsm5,
            Materials.LightFuel.getFluid(FEEDSTOCK_AMOUNT),
            Materials.Toluene.getFluid(160),
            Materials.Benzene.getFluid(200),
            Materials.IVDimethylbenzene.getFluid(250),
            Materials.Dimethylbenzene.getFluid(300));

        registerFeedstock(
            platinum,
            rhenium,
            zsm5,
            Materials.HeavyFuel.getFluid(FEEDSTOCK_AMOUNT),
            Materials.Toluene.getFluid(180),
            Materials.Benzene.getFluid(100),
            Materials.IVDimethylbenzene.getFluid(150),
            Materials.Dimethylbenzene.getFluid(400));

        registerFeedstock(
            platinum,
            rhenium,
            zsm5,
            Materials.Naphtha.getFluid(FEEDSTOCK_AMOUNT),
            Materials.Toluene.getFluid(60),
            Materials.Benzene.getFluid(200),
            Materials.IVDimethylbenzene.getFluid(350),
            new FluidStack(GTPPFluids.Ethylbenzene, 200));

        registerFeedstock(
            platinum,
            rhenium,
            zsm5,
            Materials.Gas.getGas(FEEDSTOCK_AMOUNT),
            Materials.Benzene.getFluid(50),
            new FluidStack(GTPPFluids.Ethylbenzene, 250),
            Materials.IVDimethylbenzene.getFluid(250),
            Materials.Dimethylbenzene.getFluid(150));
    }

    private static void registerFeedstock(ItemStack platinum, ItemStack rhenium, ItemStack zsm5, FluidStack feedstock,
        FluidStack... baseOutputs) {
        registerRecipe(platinum, feedstock, 1, baseOutputs);
        registerRecipe(rhenium, feedstock, 2, baseOutputs);
        registerRecipe(zsm5, feedstock, 4, baseOutputs);
    }

    private static void registerRecipe(ItemStack catalyst, FluidStack feedstock, int yieldMultiplier,
        FluidStack[] baseOutputs) {
        FluidStack[] outputs = new FluidStack[baseOutputs.length];
        for (int i = 0; i < baseOutputs.length; i++) {
            FluidStack output = baseOutputs[i];
            outputs[i] = GTUtility.copyAmount(output.amount * yieldMultiplier, output);
        }

        GTValues.RA.stdBuilder()
            .itemInputs(catalyst)
            .fluidInputs(feedstock.copy())
            .fluidOutputs(outputs)
            .duration(DURATION_TICKS)
            .eut(TierEU.RECIPE_MV)
            .addTo(GTNRRecipeMaps.CATALYTIC_REFORMER);
    }
}
