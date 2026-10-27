// SPDX-License-Identifier: MIT
package com.xyp.gtneverreset.common.recipe.gregtech;

import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.loader.MachineLoader;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class AssemblerRecipes {

    private static final int UPGRADE_CIRCUIT = 24;

    private AssemblerRecipes() {}

    public static void registerOreProcessorRecipe() {
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

    public static void registerModernFluidMachineRecipes() {
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
            registerUpgrade(mixers[index], pumps[index], MachineLoader.MODERN_MIXERS[tier], tier);
            registerUpgrade(electrolyzers[index], pumps[index], MachineLoader.MODERN_ELECTROLYZERS[tier], tier);
            registerUpgrade(chemicalReactors[index], pumps[index], MachineLoader.MODERN_CHEMICAL_REACTORS[tier], tier);
            registerUpgrade(centrifuges[index], pumps[index], MachineLoader.MODERN_CENTRIFUGES[tier], tier);
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
}
