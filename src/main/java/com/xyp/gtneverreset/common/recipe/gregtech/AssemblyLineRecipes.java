package com.xyp.gtneverreset.common.recipe.gregtech;

import static gregtech.api.util.GTModHandler.getModItem;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.dreammaster.item.NHItemList;
import com.xyp.gtneverreset.common.block.blocks.BlockLightHunterCasing.Variant;
import com.xyp.gtneverreset.config.ChaosConfig;
import com.xyp.gtneverreset.loader.BlockLoader;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import bartworks.common.loaders.ItemRegistry;
import goodgenerator.items.GGMaterial;
import goodgenerator.util.ItemRefer;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeConstants;
import gregtech.api.util.recipe.Scanning;
import gtPlusPlus.core.material.MaterialMisc;
import gtPlusPlus.core.material.MaterialsAlloy;
import gtPlusPlus.core.material.MaterialsElements;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import gtnhlanth.common.register.WerkstoffMaterialPool;
import tectech.recipe.TTRecipeAdder;
import tectech.thing.CustomItemList;

public final class AssemblyLineRecipes {

    private AssemblyLineRecipes() {}

    private static void registerStarCoreStripper() {
        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, GTNRItemList.FragmentWorldCollector.get(1))
            .metadata(SCANNING, new Scanning(20 * 60, TierEU.RECIPE_UV))
            .itemInputs(
                GTNRItemList.FragmentWorldCollector.get(4),
                ItemList.Hull_MAX.get(8),
                ItemList.Field_Generator_UHV.get(16),
                ItemList.Sensor_UHV.get(16),
                ItemList.Emitter_UHV.get(16),
                ItemList.Electric_Pump_UHV.get(16),
                ItemList.Robot_Arm_UHV.get(16),
                ItemList.Electric_Motor_UHV.get(16),
                new Object[] { OrePrefixes.circuit.get(Materials.UHV), 16 },
                Materials.Neutronium.getPlates(64),
                ItemList.Casing_Advanced_Iridium.get(64),
                ItemList.Casing_Fusion_Coil.get(32))
            .fluidInputs(Materials.SolderingAlloy.getMolten(18432))
            .itemOutputs(GTNRItemList.StarCoreStripper.get(1))
            .duration(4800)
            .eut(TierEU.RECIPE_UHV)
            .addTo(GTRecipeConstants.AssemblyLine);
    }

    public static void register() {
        spacetimeAssemblyLineUnit();
        registerLightHunterStation();
        registerSatelliteFactories();
        registerSatelliteUpgrades();
        registerStarCoreStripper();
        if (!ChaosConfig.enableBottleInChaos) return;
        registerChaosPhotonUpgrades();
        registerChaosGenerators();
        registerChaosCasings();
        registerChaosSourceComponents();

        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, ItemList.Hatch_DataAccess_LuV.get(1))
            .metadata(SCANNING, new Scanning(20 * 30, TierEU.RECIPE_ZPM))
            .itemInputs(
                ItemList.Hatch_Input_Bus_ZPM.get(1),
                ItemList.Hatch_DataAccess_LuV.get(1),
                getModItem(Mods.AE2FluidCraft.ID, "fluid_interface", 4),
                getModItem(Mods.AppliedEnergistics2.ID, "tile.BlockWirelessConnector", 1),

                new Object[] { OrePrefixes.circuit.get(Materials.UV), 4 },
                getModItem(Mods.OpenComputers.ID, "item", 8, 72), // Component buses, tier 3.
                getModItem(Mods.OpenComputers.ID, "item", 32, 28), // Control units.
                getModItem(Mods.AppliedEnergistics2.ID, "item.ItemMultiMaterial", 16, 58), // 1024k storage
                                                                                           // components.

                ItemList.Tool_DataOrb.get(32),
                ItemList.Circuit_Chip_Ram.get(64),
                ItemList.Circuit_Chip_NAND.get(64),
                ItemList.Circuit_Chip_NOR.get(64),

                ItemList.Sensor_ZPM.get(2),
                ItemList.Robot_Arm_ZPM.get(2),
                GTOreDictUnificator.get(OrePrefixes.wireFine, Materials.Osmium, 64),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Polybenzimidazole, 16))
            .fluidInputs(MaterialsAlloy.INDALLOY_140.getFluidStack(2304), Materials.RubberSilicone.getMolten(2304))
            .itemOutputs(GTNRItemList.ChaosCredentialHatch.get(1))
            .duration(20 * 40)
            .eut(TierEU.RECIPE_ZPM)
            .requireMods(Mods.AppliedEnergistics2, Mods.AE2FluidCraft, Mods.OpenComputers)
            .addTo(GTRecipeConstants.AssemblyLine);

    }

    private static void registerChaosCasings() {
        TTRecipeAdder.addResearchableAssemblylineRecipe(
            GTOreDictUnificator.get(OrePrefixes.block, Materials.Iridium, 1),
            64_000,
            32,
            800_000,
            4,
            new Object[] { GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Neutronium, 1),
                ItemList.Neutron_Reflector.get(12), ItemList.Electric_Piston_ZPM.get(2),
                GTOreDictUnificator.get(OrePrefixes.screw, Materials.Bedrockium, 6),

                ItemList.UHTResistantMesh.get(6),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.CosmicNeutronium, 3),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Osmiridium, 4),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Trinium, 4),

                GTOreDictUnificator.get(OrePrefixes.plate, Materials.PrismaticNaquadah, 4),
                ItemRefer.Radiation_Protection_Plate.get(4) },
            new FluidStack[] { MaterialsAlloy.INDALLOY_140.getFluidStack(144 * 8),
                GGMaterial.marCeM200.getMolten(144 * 8), Materials.SuperCoolant.getFluid(1000 * 8), },
            new ItemStack(BlockLoader.chaosCasing02, 1, 2),
            20 * 5,
            (int) TierEU.RECIPE_ZPM);

        TTRecipeAdder.addResearchableAssemblylineRecipe(
            new ItemStack(BlockLoader.chaosCasing02, 1, 2),
            64_000,
            32,
            800_000,
            4,
            new Object[] { MaterialsAlloy.PIKYONIUM.getFrameBox(1), CustomItemList.eM_Power.get(1),
                ItemList.Field_Generator_ZPM.get(1), ItemList.Electric_Pump_ZPM.get(1),

                GGMaterial.incoloy903.get(OrePrefixes.pipeHuge, 2), MaterialsAlloy.HASTELLOY_X.getScrew(8),
                GTOreDictUnificator.get(OrePrefixes.ring, Materials.RubberSilicone, 4),
                GTOreDictUnificator.get(OrePrefixes.wireFine, Materials.YttriumBariumCuprate, 16),

                MaterialsAlloy.HASTELLOY_C276.getPlate(4), MaterialsAlloy.ZIRCONIUM_CARBIDE.getPlate(4),
                ItemRefer.Advanced_Radiation_Protection_Plate.get(1) },
            new FluidStack[] { Materials.NickelZincFerrite.getMolten(144 * 64),
                Materials.Polybenzimidazole.getMolten(144 * 16), MaterialsAlloy.INDALLOY_140.getFluidStack(144 * 8),
                Materials.SuperCoolant.getFluid(1000 * 8) },
            new ItemStack(BlockLoader.chaosCasing01, 1, 1),
            20 * 10,
            (int) TierEU.RECIPE_ZPM);

        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, CustomItemList.eM_dynamoTunnel1_ZPM.get(1))
            .metadata(SCANNING, new Scanning(20 * 60, TierEU.RECIPE_ZPM))
            .itemInputs(
                new ItemStack(ItemRegistry.bw_glasses[0], 1, 5),
                CustomItemList.eM_dynamoTunnel1_ZPM.get(1),
                CustomItemList.eM_energyTunnel1_ZPM.get(1),
                ItemList.Field_Generator_ZPM.get(1),

                ItemList.Emitter_ZPM.get(2),
                ItemList.Sensor_ZPM.get(2),
                ItemList.Circuit_Parts_Crystal_Chip_Master.get(4),
                ItemList.QuantumStar.get(4),

                GTOreDictUnificator.get(OrePrefixes.lens, Materials.Ruby, 1),
                GTOreDictUnificator.get(OrePrefixes.lens, Materials.Emerald, 1),
                GTOreDictUnificator.get(OrePrefixes.lens, Materials.Sapphire, 1),
                ItemList.Circuit_Parts_GlassFiber.get(32))
            .fluidInputs(
                Materials.SuperconductorZPMBase.getMolten(144 * 16),
                GGMaterial.preciousMetalAlloy.getMolten(144 * 8),
                MaterialsAlloy.INDALLOY_140.getFluidStack(144 * 8))
            .itemOutputs(new ItemStack(BlockLoader.chaosGlass, 1, 0))
            .eut(TierEU.RECIPE_UV)
            .duration(20 * 10)
            .addTo(GTRecipeConstants.AssemblyLine);
    }

    private static void registerChaosGenerators() {
        TTRecipeAdder.addResearchableAssemblylineRecipe(
            new ItemStack(BlockLoader.chaosEnergyMatrix, 1, 0),
            32_768_000,
            2048,
            (int) TierEU.RECIPE_UEV,
            8,
            new Object[] { new ItemStack(BlockLoader.chaosEnergyMatrix, 1, 0), CustomItemList.eM_Power.get(16),
                CustomItemList.eM_energyTunnel6_UEV.get(1), CustomItemList.eM_dynamoTunnel6_UEV.get(1),

                ItemList.ZPM3.get(4), ItemList.Field_Generator_UEV.get(8), ItemList.Sensor_UEV.get(8),
                new Object[] { OrePrefixes.circuit.get(Materials.UIV), 16 },

                NHItemList.HighEnergyFlowCircuit.get(64), ItemList.Circuit_Wafer_PPIC.get(64),
                Materials.Silver.getNanite(32), ItemList.Energy_LapotronicOrb2.get(64),

                GTOreDictUnificator.get(OrePrefixes.wireGt16, Materials.BlackPlutonium, 64),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Flerovium, 64),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.NaquadahEnriched, 64),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.RadoxPolymer, 64) },
            new FluidStack[] { MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(144 * 128),
                Materials.Naquadria.getMolten(144 * 512), Materials.SuperCoolant.getFluid(256_000),
                Materials.Grade8PurifiedWater.getFluid(64_000) },
            new ItemStack(BlockLoader.chaosEnergyMatrix, 1, 1),
            20 * 60 * 4,
            (int) TierEU.RECIPE_UEV);

        TTRecipeAdder.addResearchableAssemblylineRecipe(
            new ItemStack(BlockLoader.chaosEnergyMatrix, 1, 1),
            131_072_000,
            4096,
            (int) TierEU.RECIPE_UIV,
            16,
            new Object[] { new ItemStack(BlockLoader.chaosEnergyMatrix, 1, 1),
                CustomItemList.eM_Containment_Advanced.get(16), CustomItemList.eM_energyTunnel7_UIV.get(1),
                CustomItemList.eM_dynamoTunnel7_UIV.get(1),

                ItemList.ZPM4.get(4), ItemList.Field_Generator_UIV.get(16), ItemList.Sensor_UIV.get(16),
                new Object[] { OrePrefixes.circuit.get(Materials.UMV), 16 },

                NHItemList.HighEnergyFlowCircuit.get(64), ItemList.Optically_Compatible_Memory.get(64),
                Materials.Gold.getNanite(32), ItemList.StableBosonContainmentUnit.get(16),

                GTOreDictUnificator.get(OrePrefixes.wireGt16, Materials.SuperconductorUIV, 64),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Naquadria, 64),
                MaterialsAlloy.BLACK_TITANIUM.getPlateDouble(64),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.Kevlar, 64) },
            new FluidStack[] { MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(144 * 192),
                Materials.DimensionallyShiftedSuperfluid.getFluid(256_000),
                Materials.StableBaryonicMatter.getFluid(256_000), Materials.SuperCoolant.getFluid(1_024_000) },
            new ItemStack(BlockLoader.chaosEnergyMatrix, 1, 2),
            20 * 60 * 16,
            (int) TierEU.RECIPE_UIV);

        TTRecipeAdder.addResearchableAssemblylineRecipe(
            new ItemStack(BlockLoader.chaosFieldGenerator, 1, 0),
            32_768_000,
            2048,
            (int) TierEU.RECIPE_UEV,
            8,
            new Object[] { new ItemStack(BlockLoader.chaosFieldGenerator, 1, 0), CustomItemList.eM_Containment.get(16),
                new ItemStack(BlockLoader.chaosPhotonUpgrade, 2, 9),
                NHItemList.IrradiantReinforcedBedrockiumPlate.get(16),

                ItemList.Field_Generator_UEV.get(16), ItemList.Emitter_UEV.get(16), ItemList.Sensor_UEV.get(16),
                ItemList.NuclearStar.get(16),

                new Object[] { OrePrefixes.circuit.get(Materials.UIV), 32 },
                GregtechItemList.Laser_Lens_Special.get(32), Materials.Neutronium.getNanite(32),
                ItemList.Circuit_Chip_FPIC.get(64),

                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.CosmicNeutronium, 64),
                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.Bedrockium, 64),
                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.Neutronium, 64),
                MaterialsAlloy.QUANTUM.getPlateSuperdense(64), },
            new FluidStack[] { MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(144 * 256),
                Materials.StableBaryonicMatter.getFluid(32_000), GGMaterial.tairitsu.getMolten(144 * 256),
                Materials.UUMatter.getFluid(200_000) },
            new ItemStack(BlockLoader.chaosFieldGenerator, 1, 1),
            20 * 60 * 4,
            (int) TierEU.RECIPE_UEV);

        TTRecipeAdder.addResearchableAssemblylineRecipe(
            new ItemStack(BlockLoader.chaosFieldGenerator, 1, 1),
            131_072_000,
            4096,
            (int) TierEU.RECIPE_UIV,
            16,
            new Object[] { new ItemStack(BlockLoader.chaosFieldGenerator, 1, 1),
                CustomItemList.eM_Containment_Advanced.get(16), new ItemStack(BlockLoader.chaosPhotonUpgrade, 2, 10),
                ItemList.MetaMaterial_Shielding1.get(16),

                ItemList.Field_Generator_UIV.get(32), GregtechItemList.SpaceTimeContinuumRipper.get(8),
                ItemList.Optically_Perfected_CPU.get(32), ItemList.StableBosonContainmentUnit.get(32),

                new Object[] { OrePrefixes.circuit.get(Materials.UMV), 32 },
                GregtechItemList.Laser_Lens_Special.get(64), Materials.TranscendentMetal.getNanite(32),
                ItemList.Circuit_Chip_APIC.get(64),

                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.Infinity, 64),
                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.CosmicNeutronium, 64),
                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.Tritanium, 64),
                MaterialsElements.STANDALONE.HYPOGEN.getPlateSuperdense(64), },
            new FluidStack[] { MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(144 * 384),
                Materials.DimensionallyShiftedSuperfluid.getFluid(512_000),
                GGMaterial.metastableOganesson.getMolten(144 * 1024), Materials.UUMatter.getFluid(100_000_000) },
            new ItemStack(BlockLoader.chaosFieldGenerator, 1, 2),
            20 * 60 * 16,
            (int) TierEU.RECIPE_UIV);
    }

    private static void registerChaosPhotonUpgrades() {
        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, getModItem("gregtech", "gt.blockmachines", 1, 11105))
            .metadata(SCANNING, new Scanning(20 * 3600, TierEU.RECIPE_LV))
            .itemInputs(
                ItemList.Casing_Advanced_Iridium.get(1),
                getModItem("gregtech", "gt.blockmachines", 1, 11105),
                ItemList.Transformer_ZPM_LuV.get(1),
                CustomItemList.eM_Power.get(4),
                ItemList.Emitter_LuV.get(4),
                WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet.get(OrePrefixes.lens, 4),
                WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet.get(OrePrefixes.gemExquisite, 4),
                ItemList.Field_Generator_LuV.get(2),
                new Object[] { OrePrefixes.circuit.get(Materials.LuV), 4 },
                new Object[] { OrePrefixes.circuit.get(Materials.IV), 8 },
                GTOreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorLuV, 4))
            .fluidInputs(MaterialsAlloy.INDALLOY_140.getFluidStack(144 * 4))
            .itemOutputs(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 5))
            .eut(TierEU.RECIPE_LuV)
            .duration(20 * 20)
            .addTo(GTRecipeConstants.AssemblyLine);

        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, getModItem("gregtech", "gt.blockmachines", 1, 11106))
            .metadata(SCANNING, new Scanning(20 * 7200, TierEU.RECIPE_LV))
            .itemInputs(
                ItemList.Casing_Advanced_Iridium.get(1),
                getModItem("gregtech", "gt.blockmachines", 1, 11106),
                ItemList.Transformer_UV_ZPM.get(1),
                CustomItemList.eM_Power.get(4),
                ItemList.Emitter_ZPM.get(4),
                WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet.get(OrePrefixes.lens, 8),
                WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet.get(OrePrefixes.gemExquisite, 8),
                ItemList.Field_Generator_ZPM.get(2),
                new Object[] { OrePrefixes.circuit.get(Materials.ZPM), 4 },
                new Object[] { OrePrefixes.circuit.get(Materials.LuV), 8 },
                GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.SuperconductorZPM, 4))
            .fluidInputs(MaterialsAlloy.INDALLOY_140.getFluidStack(144 * 8))
            .itemOutputs(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 6))

            .eut(TierEU.RECIPE_ZPM)
            .duration(20 * 40)
            .addTo(GTRecipeConstants.AssemblyLine);

        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, getModItem("gregtech", "gt.blockmachines", 1, 11107))
            .metadata(SCANNING, new Scanning(20 * 7200, TierEU.RECIPE_LV))
            .itemInputs(
                ItemList.Casing_Advanced_Iridium.get(1),
                getModItem("gregtech", "gt.blockmachines", 1, 11107),
                ItemList.Transformer_MAX_UV.get(1),
                CustomItemList.eM_Power.get(4),
                ItemList.Emitter_UV.get(4),
                WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet.get(OrePrefixes.lens, 16),
                WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet.get(OrePrefixes.gemExquisite, 16),
                ItemList.Field_Generator_UV.get(2),
                new Object[] { OrePrefixes.circuit.get(Materials.SuperconductorUHV), 4 },
                new Object[] { OrePrefixes.circuit.get(Materials.ZPM), 8 },
                GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.SuperconductorUV, 4))
            .fluidInputs(MaterialsAlloy.INDALLOY_140.getFluidStack(144 * 16))
            .itemOutputs(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 7))
            .eut(TierEU.RECIPE_UV)
            .duration(20 * 80)
            .addTo(GTRecipeConstants.AssemblyLine);

        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 7))
            .metadata(SCANNING, new Scanning(20 * 7200, TierEU.RECIPE_LV))
            .itemInputs(
                ItemList.Casing_Advanced_Iridium.get(1),
                getModItem("gregtech", "gt.blockmachines", 8, 11107),
                ItemList.Transformer_UEV_UHV.get(1),
                CustomItemList.eM_Power.get(4),

                ItemList.Emitter_UHV.get(4),
                WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet.get(OrePrefixes.lens, 32),
                WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet.get(OrePrefixes.gemExquisite, 32),
                ItemList.Field_Generator_UHV.get(2),

                new Object[] { OrePrefixes.circuit.get(Materials.UHV), 4 },
                new Object[] { OrePrefixes.circuit.get(Materials.SuperconductorUHV), 8 },
                GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.SuperconductorUHV, 4))
            .fluidInputs(MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(16 * 144))
            .itemOutputs(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 8))
            .eut(TierEU.RECIPE_UHV)
            .duration(20 * 160)
            .addTo(GTRecipeConstants.AssemblyLine);

        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 8))
            .metadata(SCANNING, new Scanning(20 * 7200, TierEU.RECIPE_LV))
            .itemInputs(
                ItemList.Casing_Advanced_Iridium.get(1),
                getModItem("gregtech", "gt.blockmachines", 64, 11107),
                ItemList.Transformer_UIV_UEV.get(1),
                CustomItemList.eM_Power.get(4),

                ItemList.Emitter_UEV.get(4),
                WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet.get(OrePrefixes.lens, 64),
                WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet.get(OrePrefixes.gemExquisite, 64),
                ItemList.Field_Generator_UEV.get(2),

                new Object[] { OrePrefixes.circuit.get(Materials.UEV), 4 },
                new Object[] { OrePrefixes.circuit.get(Materials.UHV), 8 },
                GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.SuperconductorUEV, 4))
            .fluidInputs(
                MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(64 * 144),
                Materials.SuperconductorUEVBase.getMolten(64 * 144))
            .itemOutputs(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 9))

            .eut(TierEU.RECIPE_UEV)
            .duration(20 * 320)
            .addTo(GTRecipeConstants.AssemblyLine);

        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 9))
            .metadata(SCANNING, new Scanning(20 * 7200, TierEU.RECIPE_LV))
            .itemInputs(
                ItemList.Casing_Advanced_Iridium.get(1),
                GregtechItemList.SpaceTimeContinuumRipper.get(1),
                ItemList.Transformer_UMV_UIV.get(1),
                ItemList.Casing_Dim_Injector.get(1),

                ItemList.Emitter_UIV.get(4),
                getModItem(Mods.GTPlusPlus.ID, "MU-metaitem.01", 4, 32105),
                WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet.get(OrePrefixes.gemExquisite, 64),
                ItemList.Field_Generator_UIV.get(2),

                new Object[] { OrePrefixes.circuit.get(Materials.UIV), 4 },
                new Object[] { OrePrefixes.circuit.get(Materials.UEV), 8 },
                GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.SuperconductorUIV, 8))
            .fluidInputs(
                MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(256 * 144),
                Materials.SuperconductorUIVBase.getMolten(64 * 144))
            .itemOutputs(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 10))

            .eut(TierEU.RECIPE_UIV)
            .duration(20 * 640)
            .addTo(GTRecipeConstants.AssemblyLine);

        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, GregtechItemList.SpaceTimeContinuumRipper.get(1))
            .metadata(SCANNING, new Scanning(20 * 7200, TierEU.RECIPE_LV))
            .itemInputs(
                ItemList.Casing_Advanced_Iridium.get(1),
                GregtechItemList.SpaceTimeContinuumRipper.get(4),
                ItemList.Transformer_UXV_UMV.get(1),
                ItemList.Casing_Dim_Injector.get(4),

                ItemList.Emitter_UMV.get(8),
                getModItem(Mods.GTPlusPlus.ID, "MU-metaitem.01", 16, 32105),
                WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet.get(OrePrefixes.gemExquisite, 64),
                ItemList.Field_Generator_UMV.get(4),

                new Object[] { OrePrefixes.circuit.get(Materials.UMV), 8 },
                new Object[] { OrePrefixes.circuit.get(Materials.UIV), 32 },
                GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.SuperconductorUMV, 16),
                Materials.TranscendentMetal.getNanite(16),

                CustomItemList.EOH_Reinforced_Temporal_Casing.get(4))
            .fluidInputs(
                MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(256 * 144),
                Materials.SuperconductorUMVBase.getMolten(64 * 144),
                Materials.Space.getMolten(144 * 64 * 8),
                Materials.Time.getMolten(144 * 64 * 8))
            .itemOutputs(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 11))

            .eut(TierEU.RECIPE_UMV)
            .duration(20 * 1280)
            .addTo(GTRecipeConstants.AssemblyLine);
    }

    private static void registerChaosSourceComponents() {
        TTRecipeAdder.addResearchableAssemblylineRecipe(
            GregtechItemList.Machine_Adv_BlastFurnace.get(1),
            64_000,
            32,
            600_000,
            4,
            new Object[] { ItemList.Circuit_Board_Multifiberglass_Elite.get(1), ItemList.ZPM3.get(1),
                Materials.Silver.getNanite(2), ItemRefer.Fluid_Storage_Core_T5.get(2), ItemList.UHV_Coil.get(64),
                NHItemList.PicoWafer.get(64), ItemList.Circuit_Chip_QPIC.get(64), ItemList.Circuit_Chip_QPIC.get(64),
                new Object[] { OrePrefixes.circuit.get(Materials.UEV), 16 },
                new Object[] { OrePrefixes.circuit.get(Materials.UHV), 32 },
                MaterialsElements.STANDALONE.CELESTIAL_TUNGSTEN.getBolt(48),
                MaterialsElements.STANDALONE.CHRONOMATIC_GLASS.getFoil(64) },
            new FluidStack[] { MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(144 * 16),
                MaterialsAlloy.ABYSSAL.getFluidStack(144 * 64),
                net.minecraftforge.fluids.FluidRegistry.getFluidStack("pyrotheum", 1000 * 4096) },
            GTNRItemList.SwelegfyrUpgradeChip.get(1),
            20 * 320,
            (int) TierEU.RECIPE_UEV);

        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, GregtechItemList.Casing_Coil_QuantumForceTransformer.get(1))
            .metadata(SCANNING, new Scanning(20 * 24 * 3600, TierEU.RECIPE_LV))
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.SuperconductorUHVBase, 64),
                GregtechItemList.Casing_Coil_QuantumForceTransformer.get(4),
                ItemRefer.Compact_Fusion_Coil_T4.get(16),
                CustomItemList.LASERpipe.get(16),
                ItemList.Emitter_UIV.get(6),
                ItemList.Sensor_UIV.get(6),
                new Object[] { OrePrefixes.circuit.get(Materials.UIV), 12 },
                new Object[] { OrePrefixes.circuit.get(Materials.UEV), 24 },
                ItemRefer.HiC_T5.get(64),
                GTNRItemList.GravitationalLens.get(8),
                ItemList.Energy_LapotronicOrb2.get(64),
                getModItem("eternalsingularity", "combined_singularity", 1, 0),
                ItemList.MetaMaterial_ResonanceChamber4.get(1),
                ItemList.ZPM2.get(1),
                getModItem(Mods.GTPlusPlus.ID, "item.itemBufferCore10", 1),
                GregtechItemList.Laser_Lens_Special.get(4))
            .fluidInputs(
                Materials.Hydrogen.getPlasma(1000 * 2048),
                MaterialsElements.getInstance().CALIFORNIUM.getFluidStack(144 * 64),
                MaterialsAlloy.QUANTUM.getFluidStack(144 * 256),
                MaterialsElements.STANDALONE.RHUGNOR.getFluidStack(144 * 32))
            .itemOutputs(new ItemStack(BlockLoader.chaosCasing01, 1, 2))
            .eut(TierEU.RECIPE_UIV)
            .duration(20 * 240)
            .addTo(GTRecipeConstants.AssemblyLine);
    }

    private static void registerLightHunterStation() {
        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, ItemList.SpaceElevatorController.get(1))
            .metadata(SCANNING, new Scanning(2400, TierEU.RECIPE_UXV))
            .itemInputs(
                CustomItemList.Machine_Multi_Research.get(16),
                ItemList.MiningDroneUHV.get(64),
                ItemList.MiningDroneUEV.get(64),
                ItemList.MiningDroneUIV.get(64),
                ItemList.MiningDroneUMV.get(64),
                ItemList.MiningDroneUXV.get(64),
                ItemList.SpaceElevatorModuleMinerT3.get(64),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.UXV, 32),
                ItemList.Energy_Cluster.get(64),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Infinity, 16),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Tritanium, 16),
                ItemList.Gravistar.get(16))
            .fluidInputs(
                MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(1600000),
                Materials.UUMatter.getFluid(1600000),
                Materials.TranscendentMetal.getMolten(1600000))
            .itemOutputs(GTNRItemList.LightHunterSpaceStation.get(1))
            .duration(2400)
            .eut(TierEU.RECIPE_UXV)
            .addTo(GTRecipeConstants.AssemblyLine);
    }

    private static void registerSatelliteFactories() {
        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, GregtechItemList.Industrial_Mixer.get(1))
            .metadata(SCANNING, new Scanning(1200, TierEU.RECIPE_UXV))
            .itemInputs(
                GregtechItemList.Industrial_PlatePress.get(16),
                GregtechItemList.Industrial_WireFactory.get(16),
                ItemList.Machine_Multi_IndustrialElectromagneticSeparator.get(16),
                GregtechItemList.Industrial_Extruder.get(16),
                GregtechItemList.Industrial_Mixer.get(4),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.UXV, 32),
                ItemList.Energy_Cluster.get(1),
                ItemList.Emitter_UXV.get(16),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.CosmicNeutronium, 16),
                MaterialsAlloy.BLACK_TITANIUM.getPlate(32),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Infinity, 16),
                MaterialsAlloy.CINOBITE.getPlate(32))
            .fluidInputs(
                MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(16000),
                Materials.UUMatter.getFluid(16000),
                Materials.Cerium.getMolten(16000))
            .itemOutputs(GTNRItemList.MetalFormingSatelliteMkI.get(1))
            .duration(1200)
            .eut(TierEU.RECIPE_UXV)
            .addTo(GTRecipeConstants.AssemblyLine);

        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, GregtechItemList.Industrial_Centrifuge.get(1))
            .metadata(SCANNING, new Scanning(1200, TierEU.RECIPE_UXV))
            .itemInputs(
                ItemList.ChemicalBathUMV.get(64),
                GregtechItemList.Industrial_Sifter.get(64),
                GregtechItemList.Industrial_Centrifuge.get(64),
                ItemList.Machine_Multi_IndustrialElectromagneticSeparator.get(16),
                GregtechItemList.Controller_Vacuum_Furnace.get(16),
                GregtechItemList.Industrial_Electrolyzer.get(16),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.UXV, 32),
                ItemList.Energy_Cluster.get(1),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Enderium, 16),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Osmiridium, 16),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Trinium, 16),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Quantium, 16))
            .fluidInputs(
                MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(16000),
                Materials.UUMatter.getFluid(16000),
                Materials.Neutronium.getMolten(16000))
            .itemOutputs(GTNRItemList.MineralProcessingSatelliteMkI.get(1))
            .duration(1200)
            .eut(TierEU.RECIPE_UXV)
            .addTo(GTRecipeConstants.AssemblyLine);

        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, ItemList.SpaceElevatorModulePumpT3.get(1))
            .metadata(SCANNING, new Scanning(1200, TierEU.RECIPE_UXV))
            .itemInputs(
                ItemList.AutoclaveUMV.get(16),
                ItemList.Machine_Multi_IndustrialExtractor.get(16),
                GregtechItemList.Machine_Adv_DistillationTower.get(16),
                ItemList.FluidHeaterUMV.get(16),
                ItemList.Machine_Multi_IndustrialBrewery.get(64),
                ItemList.Distillation_Tower.get(64),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.UXV, 32),
                ItemList.Energy_Cluster.get(1),
                MaterialsElements.STANDALONE.CELESTIAL_TUNGSTEN.getPlate(32),
                MaterialsElements.STANDALONE.ASTRAL_TITANIUM.getPlate(32),
                MaterialsElements.getInstance().RHENIUM.getPlate(32),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Neutronium, 32))
            .fluidInputs(
                MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(16000),
                Materials.UUMatter.getFluid(16000),
                MaterialsElements.STANDALONE.HYPOGEN.getFluidStack(16000))
            .itemOutputs(GTNRItemList.FluidProcessingSatelliteMkI.get(1))
            .duration(1200)
            .eut(TierEU.RECIPE_UXV)
            .addTo(GTRecipeConstants.AssemblyLine);

        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, ItemRefer.Precise_Assembler.get(1))
            .metadata(SCANNING, new Scanning(1200, TierEU.RECIPE_UXV))
            .itemInputs(
                ItemList.Machine_Multi_Canner.get(16),
                ItemList.Machine_Multi_IndustrialElectromagneticSeparator.get(16),
                ItemList.AssemblingMachineUMV.get(16),
                ItemRefer.Precise_Assembler.get(64),
                GregtechItemList.Industrial_Arc_Furnace.get(64),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.UXV, 32),
                ItemList.Energy_Cluster.get(1),
                ItemList.Field_Generator_UXV.get(16),
                MaterialsAlloy.HASTELLOY_C276.getPlate(32),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.DraconiumAwakened, 16),
                MaterialsAlloy.HASTELLOY_X.getPlate(32),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Infinity, 16))
            .fluidInputs(
                MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(16000),
                Materials.UUMatter.getFluid(16000),
                Materials.TranscendentMetal.getMolten(16000))
            .itemOutputs(GTNRItemList.AssemblySatelliteMkI.get(1))
            .duration(1200)
            .eut(TierEU.RECIPE_UXV)
            .addTo(GTRecipeConstants.AssemblyLine);
    }

    private static void registerSatelliteUpgrades() {
        GTNRItemList[][] families = {
            { GTNRItemList.MetalFormingSatelliteMkI, GTNRItemList.MetalFormingSatelliteMkII,
                GTNRItemList.MetalFormingSatelliteMkIII, GTNRItemList.MetalFormingSatelliteMkIV },
            { GTNRItemList.MineralProcessingSatelliteMkI, GTNRItemList.MineralProcessingSatelliteMkII,
                GTNRItemList.MineralProcessingSatelliteMkIII, GTNRItemList.MineralProcessingSatelliteMkIV },
            { GTNRItemList.FluidProcessingSatelliteMkI, GTNRItemList.FluidProcessingSatelliteMkII,
                GTNRItemList.FluidProcessingSatelliteMkIII, GTNRItemList.FluidProcessingSatelliteMkIV },
            { GTNRItemList.AssemblySatelliteMkI, GTNRItemList.AssemblySatelliteMkII,
                GTNRItemList.AssemblySatelliteMkIII, GTNRItemList.AssemblySatelliteMkIV } };
        for (GTNRItemList[] family : families) {
            for (int grade = 1; grade < family.length; grade++) {
                int count = 1 << grade;
                GTValues.RA.stdBuilder()
                    .metadata(RESEARCH_ITEM, family[grade - 1].get(1))
                    .metadata(SCANNING, new Scanning(1200 * count, TierEU.RECIPE_UXV))
                    .itemInputs(
                        family[grade - 1].get(1),
                        ItemList.Field_Generator_UXV.get(count),
                        ItemList.Emitter_UXV.get(count),
                        new Object[] { OrePrefixes.circuit.get(Materials.UXV), 8 * count })
                    .fluidInputs(
                        Materials.SpaceTime.getMolten(2304 * count),
                        Materials.UUMatter.getFluid(16000 * count))
                    .itemOutputs(family[grade].get(1))
                    .eut(TierEU.RECIPE_UXV)
                    .duration(1200 * count)
                    .addTo(GTRecipeConstants.AssemblyLine);
            }
        }
    }

    private static void spacetimeAssemblyLineUnit() {
        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, ItemList.Casing_Assembler.get(1))
            .metadata(SCANNING, new Scanning(2400, TierEU.RECIPE_UHV))
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Adamantium, 1),
                ItemList.Casing_Assembler.get(1),
                ItemRefer.Precise_Electronic_Unit_T4.get(1),
                ItemList.Robot_Arm_UHV.get(1),
                Materials.Neutronium.getNanite(1),
                ItemList.Circuit_Chip_Optical.get(6),
                new Object[] { OrePrefixes.circuit.get(Materials.UEV), 2 },
                GTOreDictUnificator.get(OrePrefixes.gearGt, Materials.BlueSteel, 4),
                GTOreDictUnificator.get(OrePrefixes.gearGt, Materials.RedSteel, 4),
                ItemList.Robot_Arm_ZPM.get(4),
                MaterialsAlloy.PIKYONIUM.getPlateDouble(6))
            .fluidInputs(
                Materials.LiquidCrystalKevlar.getFluid(5760),
                MaterialsAlloy.ABYSSAL.getFluidStack(1296),
                MaterialsAlloy.INCOLOY_MA956.getFluidStack(1296),
                MaterialsAlloy.PIKYONIUM.getFluidStack(1296))
            .itemOutputs(Variant.SPACETIME_ASSEMBLY_LINE_UNIT.stack(1))
            .duration(400)
            .eut(TierEU.RECIPE_UHV)
            .addTo(GTRecipeConstants.AssemblyLine);
    }

}
