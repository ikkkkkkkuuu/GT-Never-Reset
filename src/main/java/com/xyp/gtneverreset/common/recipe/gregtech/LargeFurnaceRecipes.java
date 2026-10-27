package com.xyp.gtneverreset.common.recipe.gregtech;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;

public final class LargeFurnaceRecipes {

    private LargeFurnaceRecipes() {}

    public static void register() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Machine_LV_E_Furnace.get(4),
                ItemList.Casing_SolidSteel.get(4),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 4 },
                Materials.Steel.getPlates(8))
            .itemOutputs(GTNRItemList.LargeFurnace.get(1))
            .duration(400)
            .eut(30)
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
