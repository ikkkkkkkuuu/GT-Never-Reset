package com.xyp.gtneverreset.common.recipe.gregtech;

import static gregtech.api.enums.GTValues.VP;
import static gregtech.api.enums.TierEU.RECIPE_LV;
import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.ModsItemlist;
import com.xyp.gtnrlib.mod.ModList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class AssemblerRecipes {

    private static final int UPGRADE_CIRCUIT = 24;
    private static final int MODULE_CIRCUIT = 19;

    public static void register() {
        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.Hatch_DataAccess_EV.get(1),
                ModsItemlist.MEInterface.get(1),
                ItemList.Hull_IV.get(1),
                new Object[] { OrePrefixes.circuit.get(Materials.IV), 2 },
                ItemList.Sensor_IV.get(1),
                ItemList.Emitter_IV.get(1))
            .fluidInputs(Materials.SolderingAlloy.getMolten(576))
            .itemOutputs(GTNRItemList.MEDataAccessHatch.get(1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(assemblerRecipes);
        registerPatternInputRecipes();
        registerMaxCapacityMEOutputRecipes();
        registerSuperAdvancedMEInputRecipes();
        registerModuleHatchRecipes();
        registerLargeFurnaceRecipes();
        if (ModList.Forestry.isModLoaded()) registerLargeBeeBreederRecipes();
        registerFluxConnectorRecipes();
        registerEndCrystalResonatorRecipes();
        registerSingularityDataHubRecipes();
        registerOreProcessorRecipe();
        registerModernFluidMachineRecipes();
    }

    private static void registerSuperAdvancedMEInputRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.Hatch_Input_ME_Advanced.get(1),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .itemOutputs(GTNRItemList.SuperAdvancedMEInputHatch.get(1))
            .duration(5 * SECONDS)
            .eut(RECIPE_LV)
            .addTo(assemblerRecipes);
        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.Hatch_Input_Bus_ME_Advanced.get(1),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .itemOutputs(GTNRItemList.SuperAdvancedMEInputBus.get(1))
            .duration(5 * SECONDS)
            .eut(RECIPE_LV)
            .addTo(assemblerRecipes);
    }

    private static void registerOreProcessorRecipe() {
        GTRecipeBuilder.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Copper, 4),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 4),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 4 },
                GTUtility.getIntegratedCircuit(24))
            .itemOutputs(GTNRItemList.LargeOreProcessor.get(1))
            .duration(200)
            .eut(32)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    private static void registerModernFluidMachineRecipes() {
        ItemList[] pumps = { ItemList.Electric_Pump_LV, ItemList.Electric_Pump_MV, ItemList.Electric_Pump_HV,
            ItemList.Electric_Pump_EV, ItemList.Electric_Pump_IV, ItemList.Electric_Pump_LuV,
            ItemList.Electric_Pump_ZPM, ItemList.Electric_Pump_UV };
        ItemList[] mixers = { ItemList.Machine_LV_Mixer, ItemList.Machine_MV_Mixer, ItemList.Machine_HV_Mixer,
            ItemList.Machine_EV_Mixer, ItemList.Machine_IV_Mixer, ItemList.MixerLuV, ItemList.MixerZPM,
            ItemList.MixerUV };
        ItemList[] electrolyzers = { ItemList.Machine_LV_Electrolyzer, ItemList.Machine_MV_Electrolyzer,
            ItemList.Machine_HV_Electrolyzer, ItemList.Machine_EV_Electrolyzer, ItemList.Machine_IV_Electrolyzer,
            ItemList.ElectrolyzerLuV, ItemList.ElectrolyzerZPM, ItemList.ElectrolyzerUV };
        ItemList[] chemicalReactors = { ItemList.Machine_LV_ChemicalReactor, ItemList.Machine_MV_ChemicalReactor,
            ItemList.Machine_HV_ChemicalReactor, ItemList.Machine_EV_ChemicalReactor,
            ItemList.Machine_IV_ChemicalReactor, ItemList.ChemicalReactorLuV, ItemList.ChemicalReactorZPM,
            ItemList.ChemicalReactorUV };
        ItemList[] centrifuges = { ItemList.Machine_LV_Centrifuge, ItemList.Machine_MV_Centrifuge,
            ItemList.Machine_HV_Centrifuge, ItemList.Machine_EV_Centrifuge, ItemList.Machine_IV_Centrifuge,
            ItemList.CentrifugeLuV, ItemList.CentrifugeZPM, ItemList.CentrifugeUV };
        for (int tier = VoltageIndex.LV; tier <= VoltageIndex.UV; tier++) {
            int index = tier - VoltageIndex.LV;
            registerUpgrade(mixers[index], pumps[index], GTNRItemList.MODERN_MIXERS[tier].get(1), tier);
            registerUpgrade(electrolyzers[index], pumps[index], GTNRItemList.MODERN_ELECTROLYZERS[tier].get(1), tier);
            registerUpgrade(
                chemicalReactors[index],
                pumps[index],
                GTNRItemList.MODERN_CHEMICAL_REACTORS[tier].get(1),
                tier);
            registerUpgrade(centrifuges[index], pumps[index], GTNRItemList.MODERN_CENTRIFUGES[tier].get(1), tier);
        }
    }

    private static void registerUpgrade(ItemList original, ItemList pump, ItemStack result, int tier) {
        GTValues.RA.stdBuilder()
            .itemInputs(original.get(1), pump.get(2))
            .circuit(UPGRADE_CIRCUIT)
            .fluidInputs(Materials.Tin.getMolten(144))
            .itemOutputs(result.copy())
            .duration(10 * GTRecipeBuilder.SECONDS)
            .eut(GTValues.VP[tier])
            .addTo(RecipeMaps.assemblerRecipes);
    }

    private static void registerPatternInputRecipes() {
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

    /** Adds upgrades that retain the ordinary ME output bus and hatch as crafting prerequisites. */
    private static void registerMaxCapacityMEOutputRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.Hatch_Output_Bus_ME.get(1L),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .itemOutputs(GTNRItemList.MaxCapacityMEOutputBus.get(1))
            .duration(5 * SECONDS)
            .eut(RECIPE_LV)
            .addTo(assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.Hatch_Output_ME.get(1L),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .itemOutputs(GTNRItemList.MaxCapacityMEOutputHatch.get(1))
            .duration(5 * SECONDS)
            .eut(RECIPE_LV)
            .addTo(assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(GTNRItemList.MaxCapacityMEOutputBus.get(1), GTNRItemList.MaxCapacityMEOutputHatch.get(1))
            .itemOutputs(GTNRItemList.MEOutputAssembly.get(1))
            .duration(5 * SECONDS)
            .eut(RECIPE_LV)
            .addTo(assemblerRecipes);
    }

    private static void registerModuleHatchRecipes() {
        Materials[] materials = { Materials.Steel, Materials.Aluminium, Materials.StainlessSteel, Materials.Titanium,
            Materials.TungstenSteel, Materials.Chrome, Materials.Iridium, Materials.Osmium };
        Materials[] circuits = { Materials.LV, Materials.MV, Materials.HV, Materials.EV, Materials.IV, Materials.LuV,
            Materials.ZPM, Materials.SuperconductorUHV };
        ItemList[] hulls = { ItemList.Hull_LV, ItemList.Hull_MV, ItemList.Hull_HV, ItemList.Hull_EV, ItemList.Hull_IV,
            ItemList.Hull_LuV, ItemList.Hull_ZPM, ItemList.Hull_UV };
        ItemList[] robotArms = { ItemList.Robot_Arm_LV, ItemList.Robot_Arm_MV, ItemList.Robot_Arm_HV,
            ItemList.Robot_Arm_EV, ItemList.Robot_Arm_IV, ItemList.Robot_Arm_LuV, ItemList.Robot_Arm_ZPM,
            ItemList.Robot_Arm_UV };
        ItemList[] conveyors = { ItemList.Conveyor_Module_LV, ItemList.Conveyor_Module_MV, ItemList.Conveyor_Module_HV,
            ItemList.Conveyor_Module_EV, ItemList.Conveyor_Module_IV, ItemList.Conveyor_Module_LuV,
            ItemList.Conveyor_Module_ZPM, ItemList.Conveyor_Module_UV };
        ItemList[] motors = { ItemList.Electric_Motor_LV, ItemList.Electric_Motor_MV, ItemList.Electric_Motor_HV,
            ItemList.Electric_Motor_EV, ItemList.Electric_Motor_IV, ItemList.Electric_Motor_LuV,
            ItemList.Electric_Motor_ZPM, ItemList.Electric_Motor_UV };
        ItemList[] pistons = { ItemList.Electric_Piston_LV, ItemList.Electric_Piston_MV, ItemList.Electric_Piston_HV,
            ItemList.Electric_Piston_EV, ItemList.Electric_Piston_IV, ItemList.Electric_Piston_LuV,
            ItemList.Electric_Piston_ZPM, ItemList.Electric_Piston_UV };
        ItemList[] emitters = { ItemList.Emitter_LV, ItemList.Emitter_MV, ItemList.Emitter_HV, ItemList.Emitter_EV,
            ItemList.Emitter_IV, ItemList.Emitter_LuV, ItemList.Emitter_ZPM, ItemList.Emitter_UV };
        ItemList[] sensors = { ItemList.Sensor_LV, ItemList.Sensor_MV, ItemList.Sensor_HV, ItemList.Sensor_EV,
            ItemList.Sensor_IV, ItemList.Sensor_LuV, ItemList.Sensor_ZPM, ItemList.Sensor_UV };

        for (int i = 0; i < GTNRItemList.STATIC_PARALLEL.length; i++) {
            GTValues.RA.stdBuilder()
                .itemInputs(
                    hulls[i].get(1),
                    new Object[] { OrePrefixes.circuit.get(circuits[i]), 2 },
                    robotArms[i].get(1),
                    conveyors[i].get(1),
                    GTOreDictUnificator.get(OrePrefixes.plate, materials[i], 2))
                .circuit(MODULE_CIRCUIT)
                .fluidInputs(Materials.Tin.getMolten(144))
                .itemOutputs(GTNRItemList.STATIC_PARALLEL[i].get(1))
                .eut(VP[i + 1])
                .duration(10 * (i + 1) * SECONDS)
                .addTo(assemblerRecipes);

            GTValues.RA.stdBuilder()
                .itemInputs(
                    hulls[i].get(1),
                    new Object[] { OrePrefixes.circuit.get(circuits[i]), 2 },
                    motors[i].get(2),
                    pistons[i].get(1),
                    GTOreDictUnificator.get(OrePrefixes.plate, materials[i], 2))
                .circuit(MODULE_CIRCUIT)
                .fluidInputs(Materials.Tin.getMolten(144))
                .itemOutputs(GTNRItemList.STATIC_SPEED[i].get(1))
                .eut(VP[i + 1])
                .duration(10 * (i + 1) * SECONDS)
                .addTo(assemblerRecipes);

            GTValues.RA.stdBuilder()
                .itemInputs(
                    hulls[i].get(1),
                    new Object[] { OrePrefixes.circuit.get(circuits[i]), 2 },
                    emitters[i].get(1),
                    sensors[i].get(1),
                    GTOreDictUnificator.get(OrePrefixes.plate, materials[i], 2))
                .circuit(MODULE_CIRCUIT)
                .fluidInputs(Materials.Tin.getMolten(144))
                .itemOutputs(GTNRItemList.POWER[i].get(1))
                .eut(VP[i + 1])
                .duration(10 * (i + 1) * SECONDS)
                .addTo(assemblerRecipes);

            registerDynamicRecipe(
                i,
                GTNRItemList.STATIC_PARALLEL[i],
                GTNRItemList.DYNAMIC_PARALLEL[i],
                circuits[i],
                sensors[i]);
            registerDynamicRecipe(
                i,
                GTNRItemList.STATIC_SPEED[i],
                GTNRItemList.DYNAMIC_SPEED[i],
                circuits[i],
                sensors[i]);
        }
        registerOverclockRecipes(hulls, circuits, emitters, sensors);
    }

    private static void registerDynamicRecipe(int tierIndex, GTNRItemList staticModule, GTNRItemList dynamicModule,
        Materials circuit, ItemList sensor) {
        GTValues.RA.stdBuilder()
            .itemInputs(staticModule.get(1), new Object[] { OrePrefixes.circuit.get(circuit), 2 }, sensor.get(1))
            .circuit(MODULE_CIRCUIT)
            .itemOutputs(dynamicModule.get(1))
            .eut(VP[tierIndex + 1])
            .duration((tierIndex + 1) * SECONDS)
            .addTo(assemblerRecipes);
    }

    private static void registerOverclockRecipes(ItemList[] hulls, Materials[] circuits, ItemList[] emitters,
        ItemList[] sensors) {
        for (int i = 0; i < GTNRItemList.OVERCLOCK.length; i++) {
            ItemStack previousModule = i == 0 ? GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.Tin, 2)
                : GTNRItemList.OVERCLOCK[i - 1].get(1);
            GTValues.RA.stdBuilder()
                .itemInputs(
                    previousModule,
                    hulls[i].get(1),
                    new Object[] { OrePrefixes.circuit.get(circuits[i]), 4 },
                    emitters[i].get(2),
                    sensors[i].get(2))
                .circuit(MODULE_CIRCUIT)
                .fluidInputs(Materials.Tin.getMolten(144))
                .itemOutputs(GTNRItemList.OVERCLOCK[i].get(1))
                .eut(VP[i + 1])
                .duration(10 * (i + 1) * SECONDS)
                .addTo(assemblerRecipes);
        }
    }

    private static void registerLargeFurnaceRecipes() {
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

    private static void registerLargeBeeBreederRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(
                ModsItemlist.ForestryRoyalJelly.get(64),
                ModsItemlist.Alveary.get(32),
                ItemList.Machine_IndustrialApiary.get(32),
                new Object[] { OrePrefixes.circuit.get(Materials.UHV), 16 },
                GTUtility.getIntegratedCircuit(17))
            .fluidInputs(Materials.SolderingAlloy.getMolten(576000))
            .itemOutputs(GTNRItemList.LargeBeeBreeder.get(1))
            .duration(36000 * SECONDS)
            .eut(TierEU.RECIPE_UHV)
            .addTo(assemblerRecipes);
    }

    private static void registerFluxConnectorRecipes() {
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

    private static void registerEndCrystalResonatorRecipes() {
        Materials[] circuits = { Materials.LV, Materials.MV, Materials.HV, Materials.EV, Materials.IV, Materials.LuV,
            Materials.ZPM, Materials.UV, Materials.UHV, Materials.UEV, Materials.UIV, Materials.UMV, Materials.UXV,
            Materials.MAX };
        for (int tier = 1; tier <= 14; tier++) {
            GTValues.RA.stdBuilder()
                .itemInputs(
                    ItemList.MACHINE_HULLS[tier].get(1),
                    ItemList.valueOf("Emitter_" + GTValues.VN[tier])
                        .get(2),
                    new Object[] { OrePrefixes.circuit.get(circuits[tier - 1]), 2 },
                    new ItemStack(Blocks.obsidian, 4),
                    new ItemStack(Items.ender_eye, 2))
                .itemOutputs(
                    GTNRItemList.valueOf("EndCrystalResonator" + GTValues.VN[tier])
                        .get(1))
                .duration(200)
                .eut(GTValues.VP[tier])
                .addTo(RecipeMaps.assemblerRecipes);
        }
    }

    private static void registerSingularityDataHubRecipes() {
        // 保险库
        GTRecipeBuilder.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 4),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 4),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 4 })
            .circuit(24)
            .itemOutputs(GTNRItemList.SingularityDataHub.get(1))
            .duration(5 * SECONDS)
            .eut(32)
            .addTo(RecipeMaps.assemblerRecipes);
        // 保险库数据中心
        GTRecipeBuilder.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Iron, 4),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 4),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 4 })
            .circuit(24)
            .itemOutputs(GTNRItemList.VaultPortHatch.get(1))
            .duration(5 * SECONDS)
            .eut(32)
            .addTo(RecipeMaps.assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 6),
                GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Steel, 1),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 4))
            .circuit(24)
            .fluidInputs(Materials.Tin.getMolten(144))
            .itemOutputs(GTNRItemList.EddingtonWallCasing.get(2))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
