package com.xyp.gtneverreset.common.recipe.gregtech;

import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.loader.BlockLoader;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import goodgenerator.items.GGMaterial;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import gtnhintergalactic.recipe.IGRecipeMaps;
import tectech.thing.CustomItemList;

public final class SpaceAssemblerRecipes {

    private SpaceAssemblerRecipes() {}

    public static void register() {
        registerChaosPhotonUpgrades();
        GTValues.RA.stdBuilder()
            .itemInputs(GregtechItemList.SpaceTimeContinuumRipper.get(1), GregtechItemList.Laser_Lens_Special.get(4))
            .fluidInputs(Materials.MysteriousCrystal.getMolten(144 * 9 * 64 * 2))
            .itemOutputs(GTNRItemList.GravitationalLens.get(2), GTNRItemList.GravitationalLens.get(4))
            .outputChances(10000, 5000)
            .duration(20)
            .eut(TierEU.RECIPE_UIV)
            .addTo(IGRecipeMaps.spaceAssemblerRecipes);
    }

    private static void registerChaosPhotonUpgrades() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 11),
                new Object[] { OrePrefixes.circuit.get(Materials.UXV), 64 },
                CustomItemList.EOH_Infinite_Energy_Casing.get(16),
                ItemList.Transformer_MAX_UXV.get(1),
                GregtechItemList.SpaceTimeContinuumRipper.get(16),
                ItemList.Tesseract.get(64),
                ItemList.Emitter_UXV.get(16),
                ItemList.Field_Generator_UXV.get(8),
                CustomItemList.SpacetimeCompressionFieldGeneratorTier8.get(1),
                CustomItemList.TimeAccelerationFieldGeneratorTier8.get(1),
                CustomItemList.StabilisationFieldGeneratorTier8.get(1),
                Materials.Universium.getNanite(16),
                CustomItemList.EOH_Reinforced_Temporal_Casing.get(16),
                gregtech.api.util.GTOreDictUnificator.get(OrePrefixes.wireGt16, Materials.SuperconductorUMV, 16),
                gregtech.api.util.GTOreDictUnificator.get(OrePrefixes.wireGt16, Materials.SuperconductorUIV, 32),
                gregtech.api.util.GTOreDictUnificator.get(OrePrefixes.wireGt16, Materials.SuperconductorUEV, 64))
            .itemOutputs(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 12))
            .fluidInputs(
                Materials.Time.getMolten(144 * 64),
                Materials.Space.getMolten(144 * 64),
                Materials.MHDCSM.getMolten(144 * 64),
                GGMaterial.shirabon.getMolten(9 * 144 * 144))

            .eut(TierEU.RECIPE_UXV)
            .duration(2560 * 20)
            .addTo(IGRecipeMaps.spaceAssemblerRecipes);
    }
}
