package com.xyp.gtneverreset.common.recipe.gregtech;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

public final class PatternInputRecipes {

    private PatternInputRecipes() {}

    public static void register() {
        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.Hatch_Input_Bus_LV.get(1),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .circuit(21)
            .itemOutputs(GTNRItemList.SuperMTEHatchCraftingInputME.get(1))
            .duration(5 * SECONDS)
            .eut(32)
            .addTo(assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(
                GTNRItemList.SuperMTEHatchCraftingInputME.get(1),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .circuit(23)
            .itemOutputs(GTNRItemList.CircuitMEPatternBuffer.get(1))
            .duration(5 * SECONDS)
            .eut(32)
            .addTo(assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.Hatch_Input_Bus_LV.get(1),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .circuit(23)
            .itemOutputs(GTNRItemList.SuperMTEHatchCraftingInputSlave.get(1))
            .duration(5 * SECONDS)
            .eut(32)
            .addTo(assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(
                GTNRItemList.SuperMTEHatchCraftingInputSlave.get(1),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .circuit(23)
            .itemOutputs(GTNRItemList.CircuitMEPatternMirror.get(1))
            .duration(5 * SECONDS)
            .eut(32)
            .addTo(assemblerRecipes);
    }
}
