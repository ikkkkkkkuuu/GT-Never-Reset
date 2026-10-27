// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtneverreset.common.recipe.gregtech;

import static gregtech.api.enums.GTValues.VP;
import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.loader.MachineLoader;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

public final class ModuleHatchRecipes {

    private static final int MODULE_CIRCUIT = 19;

    private ModuleHatchRecipes() {}

    public static void register() {
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

        for (int i = 0; i < MachineLoader.STATIC_PARALLEL.length; i++) {
            GTValues.RA.stdBuilder()
                .itemInputs(
                    hulls[i].get(1),
                    new Object[] { OrePrefixes.circuit.get(circuits[i]), 2 },
                    robotArms[i].get(1),
                    conveyors[i].get(1),
                    GTOreDictUnificator.get(OrePrefixes.plate, materials[i], 2))
                .circuit(MODULE_CIRCUIT)
                .fluidInputs(Materials.Tin.getMolten(144))
                .itemOutputs(MachineLoader.STATIC_PARALLEL[i].get(1))
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
                .itemOutputs(MachineLoader.STATIC_SPEED[i].get(1))
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
                .itemOutputs(MachineLoader.POWER[i].get(1))
                .eut(VP[i + 1])
                .duration(10 * (i + 1) * SECONDS)
                .addTo(assemblerRecipes);

            registerDynamicRecipe(
                i,
                MachineLoader.STATIC_PARALLEL[i],
                MachineLoader.DYNAMIC_PARALLEL[i],
                circuits[i],
                sensors[i]);
            registerDynamicRecipe(
                i,
                MachineLoader.STATIC_SPEED[i],
                MachineLoader.DYNAMIC_SPEED[i],
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
        for (int i = 0; i < MachineLoader.OVERCLOCK.length; i++) {
            ItemStack previousModule = i == 0 ? GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.Tin, 2)
                : MachineLoader.OVERCLOCK[i - 1].get(1);
            GTValues.RA.stdBuilder()
                .itemInputs(
                    previousModule,
                    hulls[i].get(1),
                    new Object[] { OrePrefixes.circuit.get(circuits[i]), 4 },
                    emitters[i].get(2),
                    sensors[i].get(2))
                .circuit(MODULE_CIRCUIT)
                .fluidInputs(Materials.Tin.getMolten(144))
                .itemOutputs(MachineLoader.OVERCLOCK[i].get(1))
                .eut(VP[i + 1])
                .duration(10 * (i + 1) * SECONDS)
                .addTo(assemblerRecipes);
        }
    }
}
