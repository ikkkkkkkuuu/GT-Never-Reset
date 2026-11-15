package com.xyp.gtneverreset.common.recipe.gregtech;

import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.config.ChaosConfig;
import com.xyp.gtneverreset.loader.BlockLoader;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gtPlusPlus.core.material.MaterialsAlloy;

public final class FormingPressRecipes {

    private FormingPressRecipes() {}

    public static void register() {
        if (!ChaosConfig.enableBottleInChaos) return;
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Bedrockium, 1),
                MaterialsAlloy.PIKYONIUM.getPlateDense(1),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Neutronium, 6),
                GTOreDictUnificator.get(OrePrefixes.screw, Materials.Osmiridium, 4))
            .itemOutputs(new ItemStack(BlockLoader.chaosCasing02, 1, 8))
            .eut(TierEU.RECIPE_LuV)
            .duration(20 * 2)
            .addTo(RecipeMaps.formingPressRecipes);
    }
}
