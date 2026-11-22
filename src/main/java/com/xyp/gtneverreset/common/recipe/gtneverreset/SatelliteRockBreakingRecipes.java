package com.xyp.gtneverreset.common.recipe.gtneverreset;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;

import gregtech.api.enums.TierEU;
import gregtech.api.util.GTRecipeBuilder;

public final class SatelliteRockBreakingRecipes {

    private SatelliteRockBreakingRecipes() {}

    public static void register() {
        rockRecipe(1, new ItemStack(Blocks.cobblestone), 16);
        rockRecipe(2, new ItemStack(Blocks.stone), 16);
        GTRecipeBuilder.builder()
            .itemInputs(new ItemStack(Items.redstone))
            .circuit(3)
            .fluidInputs(new FluidStack(FluidRegistry.WATER, 1000), new FluidStack(FluidRegistry.LAVA, 1000))
            .itemOutputs(new ItemStack(Blocks.obsidian))
            .duration(128)
            .eut(TierEU.RECIPE_LV)
            .addTo(GTNRRecipeMaps.LightHunterRockBreakingRecipes);
    }

    private static void rockRecipe(int circuit, ItemStack output, int duration) {
        GTRecipeBuilder.builder()
            .circuit(circuit)
            .fluidInputs(new FluidStack(FluidRegistry.WATER, 1000), new FluidStack(FluidRegistry.LAVA, 1000))
            .itemOutputs(output)
            .duration(duration)
            .eut(TierEU.RECIPE_LV)
            .addTo(GTNRRecipeMaps.LightHunterRockBreakingRecipes);
    }

}
