package com.xyp.gtneverreset.common.recipe.gregtech;

import static gregtech.api.util.GTRecipeConstants.NANITE_TIERS;

import net.minecraft.item.ItemStack;

import com.dreammaster.item.NHItemList;
import com.xyp.gtneverreset.loader.BlockLoader;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.enums.CondensateType;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.NaniteTier;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gtPlusPlus.core.material.MaterialsElements;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import tectech.recipe.TecTechRecipeMaps;
import tectech.thing.CustomItemList;

public final class CondensateAssemblerRecipes {

    private CondensateAssemblerRecipes() {}

    public static void register() {
        registerChaosGenerators();
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.ProtoHalkonite, 4),
                NHItemList.CircuitUIV.get(8),
                ItemList.MetaMaterial_ResonanceChamber1.get(8),
                ItemList.MetaMaterial_FieldManipulator1.get(8),

                GregtechItemList.CosmicFabricManipulator.get(2),
                ItemList.Field_Generator_UIV.get(2),
                ItemList.EnergisedTesseract.get(16),
                ItemList.StableBosonContainmentUnit.get(4),

                ItemList.ZPM2.get(1),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.SixPhasedCopper, 4),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Creon, 4),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.RadoxPolymer, 16))
            .metadata(
                NANITE_TIERS,
                new NaniteTier[] { NaniteTier.Gold, NaniteTier.Transcendent, NaniteTier.Gold, NaniteTier.Transcendent,

                    NaniteTier.Transcendent, NaniteTier.Transcendent, NaniteTier.Gold, NaniteTier.Transcendent,

                    NaniteTier.Transcendent, NaniteTier.Gold, NaniteTier.Transcendent, NaniteTier.Gold })
            .fluidInputs(
                CondensateType.Bedrockium.getEntangled(9216),
                CondensateType.CelestialTungsten.getEntangled(4608),
                CondensateType.Neutronium.getEntangled(9216),
                CondensateType.CosmicNeutronium.getEntangled(9216))
            .itemOutputs(GTNRItemList.ChaosCapacityTier4.get(4))
            .duration(20 * 10)
            .eut(TierEU.RECIPE_UIV)
            .requireMods(Mods.NewHorizonsCoreMod)
            .addTo(TecTechRecipeMaps.condensateAssemblingRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.MHDCSM, 4),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Eternity, 8),
                NHItemList.CircuitMAX.get(8),
                ItemList.MetaMaterial_ResonanceChamber4.get(4),

                ItemList.MetaMaterial_Shielding3.get(8),
                ItemList.MetaMaterial_Waveguide3.get(8),
                ItemList.MetaMaterial_SensorArray4.get(4),
                ItemList.Field_Generator_UXV.get(2),

                ItemList.ZPM3.get(1),
                GregtechItemList.SpaceTimeContinuumRipper.get(1),
                ItemList.PseudoStar.get(4),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.MagMatter, 16))
            .metadata(
                NANITE_TIERS,
                new NaniteTier[] { NaniteTier.Eternity, NaniteTier.Eternity, NaniteTier.MagMatter,
                    NaniteTier.BlackDwarf,

                    NaniteTier.Eternity, NaniteTier.Eternity, NaniteTier.BlackDwarf, NaniteTier.Universium,

                    NaniteTier.Eternity, NaniteTier.Universium, NaniteTier.BlackDwarf, NaniteTier.MagMatter })
            .fluidInputs(
                CondensateType.BoundlessCosmicSolder.getEntangled(16000),
                CondensateType.SpaceTime.getEntangled(9216),
                CondensateType.Space.getEntangled(9216),
                CondensateType.QuarkGluonPlasma.getEntangled(32000))
            .itemOutputs(GTNRItemList.ChaosCapacityTier5.get(4))
            .duration(20 * 15)
            .eut(TierEU.RECIPE_UXV)
            .requireMods(Mods.NewHorizonsCoreMod)
            .addTo(TecTechRecipeMaps.condensateAssemblingRecipes);
    }

    private static void registerChaosGenerators() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BlockLoader.chaosEnergyMatrix, 1, 2),
                CustomItemList.eM_Ultimate_Containment.get(16),
                CustomItemList.eM_energyTunnel8_UMV.get(1),
                CustomItemList.eM_dynamoTunnel8_UMV.get(1),

                ItemList.ZPM5.get(4),
                ItemList.Field_Generator_UMV.get(32),
                ItemList.Sensor_UMV.get(32),
                NHItemList.CircuitUXV.get(16),

                ItemList.MetaMaterial_Waveguide2.get(32),
                ItemList.MetaMaterial_FieldManipulator3.get(64),
                ItemList.Circuit_Wafer_YPIC.get(32),
                ItemList.Transdimensional_Alignment_Matrix.get(4),

                GTOreDictUnificator.get(OrePrefixes.wireGt16, Materials.SuperconductorUMV, 64),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.SixPhasedCopper, 64),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.BlackDwarfMatter, 64),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.RadoxPolymer, 64))
            .metadata(
                NANITE_TIERS,
                new NaniteTier[] { NaniteTier.WhiteDwarf, NaniteTier.SixPhasedCopper, NaniteTier.BlackDwarf,
                    NaniteTier.BlackDwarf,

                    NaniteTier.BlackDwarf, NaniteTier.WhiteDwarf, NaniteTier.WhiteDwarf, NaniteTier.BlackDwarf,

                    NaniteTier.BlackDwarf, NaniteTier.BlackDwarf, NaniteTier.BlackDwarf, NaniteTier.BlackDwarf,

                    NaniteTier.WhiteDwarf, NaniteTier.SixPhasedCopper, NaniteTier.BlackDwarf, NaniteTier.Transcendent })
            .fluidInputs(
                CondensateType.BoundlessCosmicSolder.getEntangled(1_024_000),
                CondensateType.ChromaticGlass.getEntangled(144 * 16384),
                CondensateType.DimensionallyShiftedSuperfluid.getEntangled(4_096_000),
                CondensateType.SpaceTime.getEntangled(100080))
            .itemOutputs(new ItemStack(BlockLoader.chaosEnergyMatrix, 1, 3))
            .duration(20 * 60 * 64)
            .eut(TierEU.RECIPE_UMV)
            .addTo(TecTechRecipeMaps.condensateAssemblingRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BlockLoader.chaosEnergyMatrix, 1, 3),
                CustomItemList.eM_Ultimate_Containment_Advanced.get(16),
                CustomItemList.eM_energyTunnel9_UXV.get(1),
                CustomItemList.eM_dynamoTunnel9_UXV.get(1),

                ItemList.ZPM6.get(4),
                ItemList.Field_Generator_UXV.get(64),
                ItemList.Sensor_UXV.get(64),
                NHItemList.CircuitMAX.get(16),

                ItemList.MetaMaterial_Waveguide3.get(64),
                ItemList.MetaMaterial_FieldManipulator3.get(64),
                ItemList.Circuit_Wafer_YPIC.get(64),
                ItemList.MetaMaterial_FieldManipulator4.get(64),

                GTOreDictUnificator.get(OrePrefixes.wireGt16, Materials.BlackPlutonium, 64),
                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.WhiteDwarfMatter, 64),
                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.BlackDwarfMatter, 64),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.RadoxPolymer, 64))
            .metadata(
                NANITE_TIERS,
                new NaniteTier[] { NaniteTier.Universium, NaniteTier.Eternity, NaniteTier.Universium,
                    NaniteTier.Universium,

                    NaniteTier.Eternity, NaniteTier.Universium, NaniteTier.Universium, NaniteTier.MagMatter,

                    NaniteTier.Eternity, NaniteTier.Universium, NaniteTier.Universium, NaniteTier.Eternity,

                    NaniteTier.Universium, NaniteTier.Eternity, NaniteTier.MagMatter, NaniteTier.Transcendent })
            .fluidInputs(
                CondensateType.BoundlessCosmicSolder.getEntangled(2_048_000),
                CondensateType.QuarkGluonPlasma.getEntangled(512_000),
                CondensateType.PhononMedium.getEntangled(8_192_000),
                CondensateType.SpaceTime.getEntangled(200000016))
            .itemOutputs(new ItemStack(BlockLoader.chaosEnergyMatrix, 1, 4))
            .duration(20 * 60 * 256)
            .eut(TierEU.RECIPE_UXV)
            .addTo(TecTechRecipeMaps.condensateAssemblingRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BlockLoader.chaosFieldGenerator, 1, 2),
                CustomItemList.eM_Ultimate_Containment.get(16),
                new ItemStack(BlockLoader.chaosPhotonUpgrade, 2, 11),
                ItemList.MetaMaterial_Shielding2.get(32),

                ItemList.Field_Generator_UMV.get(64),
                ItemList.MetaMaterial_FieldManipulator3.get(64),
                ItemList.MetaMaterial_FieldManipulator3.get(64),
                ItemList.MetaMaterial_Shielding2.get(32),

                NHItemList.CircuitUXV.get(32),
                GregtechItemList.SpaceTimeContinuumRipper.get(32),
                Materials.BlackDwarfMatter.getNanite(64),
                ItemList.Circuit_Chip_ZPIC.get(64),

                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.TranscendentMetal, 64),
                MaterialsElements.STANDALONE.RHUGNOR.getPlateSuperdense(64),
                MaterialsElements.STANDALONE.CELESTIAL_TUNGSTEN.getPlateSuperdense(64),
                MaterialsElements.STANDALONE.ASTRAL_TITANIUM.getPlateSuperdense(64))
            .metadata(
                NANITE_TIERS,
                new NaniteTier[] { NaniteTier.WhiteDwarf, NaniteTier.SixPhasedCopper, NaniteTier.BlackDwarf,
                    NaniteTier.BlackDwarf,

                    NaniteTier.WhiteDwarf, NaniteTier.BlackDwarf, NaniteTier.BlackDwarf, NaniteTier.BlackDwarf,

                    NaniteTier.BlackDwarf, NaniteTier.SixPhasedCopper, NaniteTier.BlackDwarf,
                    NaniteTier.SixPhasedCopper,

                    NaniteTier.Transcendent, NaniteTier.SixPhasedCopper, NaniteTier.WhiteDwarf, NaniteTier.BlackDwarf })
            .fluidInputs(
                CondensateType.BoundlessCosmicSolder.getEntangled(2_048_000),
                CondensateType.Infinity.getEntangled(1_728_000),
                CondensateType.DimensionallyShiftedSuperfluid.getEntangled(8_192_000),
                CondensateType.SpaceTime.getEntangled(200016))
            .itemOutputs(new ItemStack(BlockLoader.chaosFieldGenerator, 1, 3))
            .duration(20 * 60 * 64)
            .eut(TierEU.RECIPE_UMV)
            .addTo(TecTechRecipeMaps.condensateAssemblingRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BlockLoader.chaosFieldGenerator, 1, 3),
                CustomItemList.eM_Ultimate_Containment_Advanced.get(16),
                new ItemStack(BlockLoader.chaosPhotonUpgrade, 2, 12),
                ItemList.MetaMaterial_Shielding3.get(64),

                ItemList.Field_Generator_UXV.get(64),
                ItemList.MetaMaterial_FieldManipulator4.get(64),
                ItemList.MetaMaterial_SensorArray4.get(64),
                ItemList.MetaMaterial_ElectrograviticValve3.get(64),

                NHItemList.CircuitMAX.get(32),
                ItemList.MetaMaterial_ResonanceChamber4.get(64),
                Materials.Universium.getNanite(64),
                ItemList.Circuit_Chip_YPIC.get(64),

                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.SpaceTime, 64),
                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.MHDCSM, 64),
                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.MagMatter, 64),
                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.Eternity, 64))
            .metadata(
                NANITE_TIERS,
                new NaniteTier[] { NaniteTier.Universium, NaniteTier.Eternity, NaniteTier.Universium,
                    NaniteTier.MagMatter,

                    NaniteTier.Universium, NaniteTier.Eternity, NaniteTier.Eternity, NaniteTier.MagMatter,

                    NaniteTier.MagMatter, NaniteTier.MagMatter, NaniteTier.Universium, NaniteTier.BlackDwarf,

                    NaniteTier.Universium, NaniteTier.Eternity, NaniteTier.Eternity, NaniteTier.BlackDwarf })
            .fluidInputs(
                CondensateType.BoundlessCosmicSolder.getEntangled(4_096_000),
                CondensateType.QuarkGluonPlasma.getEntangled(1_024_000),
                CondensateType.PhononMedium.getEntangled(16_384_000),
                CondensateType.SpaceTime.getEntangled(400000032))
            .itemOutputs(new ItemStack(BlockLoader.chaosFieldGenerator, 1, 4))
            // 64 minutes with 64 matching nanites, in addition to upstream BEC processing.
            .duration(20 * 60 * 4096)
            .eut(TierEU.RECIPE_UXV)
            .addTo(TecTechRecipeMaps.condensateAssemblingRecipes);
    }
}
