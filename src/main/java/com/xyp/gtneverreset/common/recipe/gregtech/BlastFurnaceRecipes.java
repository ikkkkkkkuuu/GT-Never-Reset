package com.xyp.gtneverreset.common.recipe.gregtech;

import static gregtech.api.enums.TierEU.RECIPE_MV;
import static gregtech.api.recipe.RecipeMaps.blastFurnaceRecipes;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

public final class BlastFurnaceRecipes {

    public static void register() {
        GTRecipeBuilder.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.SiliconSG, 64),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.SiliconSG, 64),
                ItemList.GalliumArsenideCrystal.get(1))
            .circuit(2)
            .itemOutputs(ItemList.Circuit_Silicon_Ingot.get(4))
            .duration(30 * MINUTES)
            .eut(RECIPE_MV)
            .metadata(COIL_HEAT, 1784)
            .addTo(blastFurnaceRecipes);
    }
}
