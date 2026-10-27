// SPDX-License-Identifier: MIT
// Ported from GT-Not-Good; see META-INF/fragment-world-collector-port/NOTICE.md.
package com.xyp.gtneverreset.common.recipe.gregtech;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.utils.enums.ModsItemlist;

import gregtech.api.enums.Mods;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipeBuilder;

public final class MixerRecipes {

    private MixerRecipes() {}

    public static void register() {
        if (!Mods.NEIOrePlugin.isModLoaded()) return;
        if (Mods.GalacticraftCore.isModLoaded()) {
            addDimensionRecipe(ModsItemlist.Tier1Rocket, ModsItemlist.DimensionOverworld, 1);
            addDimensionRecipe(ModsItemlist.Tier1Rocket, ModsItemlist.DimensionNether, 2);
            addDimensionRecipe(ModsItemlist.Tier1Rocket, ModsItemlist.DimensionTwilight, 3);
            addDimensionRecipe(ModsItemlist.Tier1Rocket, ModsItemlist.DimensionEnd, 4);
            addDimensionRecipe(ModsItemlist.Tier1Rocket, ModsItemlist.DimensionMoon, 7);
        }
        if (Mods.GalacticraftMars.isModLoaded()) {
            addDimensionRecipe(ModsItemlist.Tier2Rocket, ModsItemlist.DimensionMars, 2);
            addDimensionRecipe(ModsItemlist.Tier3Rocket, ModsItemlist.DimensionCeres, 3);
            addDimensionRecipe(ModsItemlist.Tier3Rocket, ModsItemlist.DimensionGanymede, 5);
        }
        if (Mods.GalaxySpace.isModLoaded()) {
            addDimensionRecipe(ModsItemlist.Tier4Rocket, ModsItemlist.DimensionIo, 1);
            addDimensionRecipe(ModsItemlist.Tier4Rocket, ModsItemlist.DimensionMercury, 2);
            addDimensionRecipe(ModsItemlist.Tier4Rocket, ModsItemlist.DimensionVenus, 3);
            addDimensionRecipe(ModsItemlist.Tier5Rocket, ModsItemlist.DimensionEnceladus, 1);
            addDimensionRecipe(ModsItemlist.Tier5Rocket, ModsItemlist.DimensionTitan, 4);
            addDimensionRecipe(ModsItemlist.Tier6Rocket, ModsItemlist.DimensionTriton, 2);
            addDimensionRecipe(ModsItemlist.Tier7Rocket, ModsItemlist.DimensionPluto, 4);
            addDimensionRecipe(ModsItemlist.Tier8Rocket, ModsItemlist.DimensionBarnardC, 1);
        }
    }

    private static void addDimensionRecipe(ModsItemlist rocket, ModsItemlist dimension, int circuit) {
        ItemStack rocketStack = rocket.get(0);
        ItemStack output = dimension.get(1);
        if (rocketStack == null || output == null) return;
        GTRecipeBuilder.builder()
            .itemInputs(rocketStack, new ItemStack(Blocks.cobblestone))
            .circuit(circuit)
            .itemOutputs(output)
            .duration(5 * GTRecipeBuilder.SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.mixerNonCellRecipes);
    }
}
