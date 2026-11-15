package com.xyp.gtneverreset.common.recipe.gregtech;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;

public final class LaserEngraverRecipes {

    private LaserEngraverRecipes() {}

    public static void register() {
        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.QuantumStar.get(1), GregtechItemList.Laser_Lens_Special.get(0))
            .itemOutputs(GTNRItemList.ChaosCriticalPhoton.get(64))
            .duration(20 * 60)
            .eut(TierEU.RECIPE_UV)
            .addTo(RecipeMaps.laserEngraverRecipes);
    }
}
