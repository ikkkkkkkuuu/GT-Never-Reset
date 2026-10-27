package com.xyp.gtneverreset.common.recipe.gregtech;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;

public final class FluxConnectorRecipes {

    private FluxConnectorRecipes() {}

    public static void register() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Hull_MV.get(1),
                ItemList.Emitter_MV.get(1),
                new Object[] { OrePrefixes.circuit.get(Materials.MV), 2 },
                new ItemStack(Items.ender_pearl, 2))
            .circuit(1)
            .itemOutputs(GTNRItemList.FluxPlug.get(1))
            .duration(200)
            .eut(TierEU.RECIPE_MV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Hull_MV.get(1),
                ItemList.Sensor_MV.get(1),
                new Object[] { OrePrefixes.circuit.get(Materials.MV), 2 },
                new ItemStack(Items.ender_pearl, 2))
            .circuit(2)
            .itemOutputs(GTNRItemList.FluxPoint.get(1))
            .duration(200)
            .eut(TierEU.RECIPE_MV)
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
