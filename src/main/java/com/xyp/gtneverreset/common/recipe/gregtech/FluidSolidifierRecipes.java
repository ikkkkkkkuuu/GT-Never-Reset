package com.xyp.gtneverreset.common.recipe.gregtech;

import com.xyp.gtneverreset.config.ChaosConfig;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;

public final class FluidSolidifierRecipes {

    private FluidSolidifierRecipes() {}

    public static void register() {
        if (!ChaosConfig.enableBottleInChaos) return;
        GTValues.RA.stdBuilder()
            .itemInputs(Materials.Neutronium.getIngots(1))
            .fluidInputs(Materials.WhiteDwarfMatter.getMolten(576))
            .itemOutputs(GTNRItemList.WhiteDwarfMold_Ingot.get(1))
            .eut(TierEU.RECIPE_UMV)
            .duration(20 * 10)
            .addTo(RecipeMaps.fluidSolidifierRecipes);
    }
}
