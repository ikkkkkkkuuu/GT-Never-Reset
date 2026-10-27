package com.xyp.gtneverreset.qa;

import static gregtech.api.enums.GTValues.VP;

import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.loader.MachineLoader;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

public final class OverclockRecipeChecks {

    private OverclockRecipeChecks() {}

    public static int verify() {
        int checks = 0;
        int modules = 0;
        for (GTNRItemList[] group : List.of(
            MachineLoader.STATIC_PARALLEL,
            MachineLoader.DYNAMIC_PARALLEL,
            MachineLoader.STATIC_SPEED,
            MachineLoader.DYNAMIC_SPEED,
            MachineLoader.POWER,
            MachineLoader.OVERCLOCK)) {
            boolean dynamic = group == MachineLoader.DYNAMIC_PARALLEL || group == MachineLoader.DYNAMIC_SPEED;
            for (int index = 0; index < group.length; index++) {
                GTNRItemList entry = group[index];
                String label = entry.name();
                ItemStack output = entry.get(1);
                List<GTRecipe> recipes = RecipeMaps.assemblerRecipes.getAllRecipes()
                    .stream()
                    .filter(
                        recipe -> Arrays.stream(recipe.mOutputs)
                            .anyMatch(item -> sameItem(item, output)))
                    .toList();
                checks += require(recipes.size() == 1, label + " has one assembler recipe");
                GTRecipe recipe = recipes.get(0);
                checks += require(
                    recipe.mOutputs.length == 1 && recipe.mOutputs[0].stackSize == 1,
                    label + " produces exactly one module");
                checks += require(
                    recipe.mInputs != null && recipe.mInputs.length <= 6
                        && Arrays.stream(recipe.mInputs)
                            .allMatch(input -> input != null && input.stackSize <= input.getMaxStackSize()),
                    label + " fits a singleblock assembler's six input slots");
                checks += require(recipe.mEUt == VP[index + 1], label + " uses its own voltage tier");
                checks += require(
                    recipe.mDuration == 20 * (dynamic ? 1 : 10) * (index + 1),
                    label + " uses the reduced processing time");
                List<ItemStack> circuits = Arrays.stream(recipe.mInputs)
                    .filter(GTUtility::isAnyIntegratedCircuit)
                    .toList();
                checks += require(
                    circuits.size() == 1 && circuits.get(0)
                        .getItemDamage() == 19 && circuits.get(0).stackSize == 0,
                    label + " uses non-consumed circuit 19");
                checks += dynamic ? verifyFluids(label, recipe.mFluidInputs)
                    : verifyFluids(label, recipe.mFluidInputs, Materials.Tin.getMolten(144));
                checks += require(
                    GregTechAPI.METATILEENTITIES[output.getItemDamage()] instanceof ParameterModuleHatch,
                    label + " registers a parameter module hatch");
                ParameterModuleHatch hatch = (ParameterModuleHatch) GregTechAPI.METATILEENTITIES[output
                    .getItemDamage()];
                checks += require(hatch.mTier == index + 1, label + " has the corresponding hatch tier");
                checks += require(
                    ((ParameterModuleHatch) hatch.newMetaEntity(null)).mTier == index + 1,
                    label + " preserves its tier when instantiated");
                checks += require(
                    GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
                        .noneMatch(line -> sameItem(line.mOutput, output)),
                    label + " has no obsolete assembly-line recipe");
                if (group == MachineLoader.OVERCLOCK) checks += verifyOverclockItems(index, recipe.mInputs);
                modules++;
            }
        }
        checks += require(modules == 43, "all 43 module variants have survival recipes");
        return checks;
    }

    private static int verifyOverclockItems(int index, ItemStack[] inputs) {
        String[] tiers = { "LV", "MV", "HV" };
        Materials[] circuits = { Materials.LV, Materials.MV, Materials.HV };
        ItemStack upgrade = index == 0 ? GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.Tin, 1)
            : MachineLoader.OVERCLOCK[index - 1].get(1);
        return verifyItems(
            MachineLoader.OVERCLOCK[index].name(),
            inputs,
            new ItemAmount(GTUtility.getIntegratedCircuit(19), 0),
            new ItemAmount(
                ItemList.valueOf("Hull_" + tiers[index])
                    .get(1),
                1),
            new ItemAmount(OrePrefixes.circuit.get(circuits[index]), 4),
            new ItemAmount(
                ItemList.valueOf("Emitter_" + tiers[index])
                    .get(1),
                2),
            new ItemAmount(
                ItemList.valueOf("Sensor_" + tiers[index])
                    .get(1),
                2),
            new ItemAmount(upgrade, index == 0 ? 2 : 1));
    }

    private static int verifyItems(String label, ItemStack[] actual, ItemAmount... expected) {
        int checks = require(actual.length == expected.length, label + " has the expected input count");
        for (ItemAmount item : expected) {
            checks += require(
                item.ingredient() != null && amountOf(actual, item.ingredient()) == item.amount(),
                label + " uses ingredient quantity " + item.amount());
        }
        for (ItemStack item : actual) {
            checks += require(
                item != null && Arrays.stream(expected)
                    .anyMatch(entry -> matchesIngredient(item, entry.ingredient())),
                label + " has no unplanned ingredient");
        }
        return checks;
    }

    private static int verifyFluids(String label, FluidStack[] actual, FluidStack... expected) {
        int checks = require(
            actual != null && actual.length == expected.length,
            label + " has the expected fluid count");
        for (int index = 0; index < expected.length; index++) {
            FluidStack expectedFluid = expected[index];
            checks += require(
                actual[index] != null && expectedFluid != null
                    && actual[index].isFluidEqual(expectedFluid)
                    && actual[index].amount == expectedFluid.amount,
                label + " uses the expected fluid and amount");
        }
        return checks;
    }

    private static long amountOf(ItemStack[] inputs, Object expected) {
        return Arrays.stream(inputs)
            .filter(input -> matchesIngredient(input, expected))
            .mapToLong(input -> input.stackSize)
            .sum();
    }

    private static boolean matchesIngredient(ItemStack actual, Object expected) {
        return expected instanceof ItemStack stack ? sameItem(actual, stack)
            : GTOreDictUnificator.isItemStackInstanceOf(actual, expected);
    }

    private static boolean sameItem(ItemStack first, ItemStack second) {
        return first != null && second != null && GTUtility.areStacksEqual(first, second);
    }

    private static int require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        return 1;
    }

    private record ItemAmount(Object ingredient, long amount) {}
}
