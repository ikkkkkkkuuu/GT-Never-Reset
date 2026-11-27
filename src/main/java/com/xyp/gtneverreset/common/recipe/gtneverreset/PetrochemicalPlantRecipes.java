package com.xyp.gtneverreset.common.recipe.gtneverreset;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.api.recipe.metadata.SimpleRecipeMetadataKey;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class PetrochemicalPlantRecipes {

    public static final RecipeMetadataKey<Step> STEP = SimpleRecipeMetadataKey
        .create(Step.class, "gtnr_petrochemical_step");

    private PetrochemicalPlantRecipes() {}

    public static void register() {
        FluidStack steam = Materials.Steam.getGas(1);
        FluidStack gas = Materials.Gas.getGas(1);
        FluidStack naphtha = Materials.Naphtha.getFluid(1);
        FluidStack crackedNaphtha = Materials.Naphtha.getLightlySteamCracked(1);
        FluidStack crackedGas = Materials.Gas.getSeverelySteamCracked(1);
        List<GTRecipeBuilder> stages;
        try {
            stages = List.of(
                separation(Materials.Oil),
                separation(Materials.OilLight),
                separation(Materials.OilMedium),
                separation(Materials.OilHeavy),
                desulfurization(Materials.NaturalGas.getGas(1), gas),
                desulfurization(Materials.SulfuricGas.getGas(1), gas),
                desulfurization(Materials.SulfuricNaphtha.getFluid(1), naphtha),
                desulfurization(Materials.SulfuricLightFuel.getFluid(1), Materials.LightFuel.getFluid(1)),
                desulfurization(Materials.SulfuricHeavyFuel.getFluid(1), Materials.HeavyFuel.getFluid(1)),
                stage(Step.CRACKING, RecipeMaps.crackingRecipes, 1, crackedNaphtha, naphtha, steam),
                stage(Step.CRACKING, RecipeMaps.crackingRecipes, 3, crackedGas, gas, steam),
                stage(
                    Step.REDISTILLATION,
                    RecipeMaps.distillationTowerRecipes,
                    0,
                    Materials.Benzene.getFluid(1),
                    crackedNaphtha),
                stage(
                    Step.REDISTILLATION,
                    RecipeMaps.distillationTowerRecipes,
                    0,
                    Materials.Helium.getGas(1),
                    crackedGas));
        } catch (IllegalStateException exception) {
            GTNeverReset.LOG.warn("cannot register the integrated petrochemical route: {}", exception.getMessage());
            return;
        }
        for (GTRecipeBuilder stage : stages) {
            stage.addTo(GTNRRecipeMaps.PETROCHEMICAL_PLANT);
        }
        if (ItemList.sOilExtraHeavy == null) return;
        try {
            stage(
                Step.PRETREATMENT,
                RecipeMaps.distillationTowerRecipes,
                0,
                Materials.OilHeavy.getFluid(1),
                new FluidStack(ItemList.sOilExtraHeavy, 1)).addTo(GTNRRecipeMaps.PETROCHEMICAL_PLANT);
        } catch (IllegalStateException exception) {
            GTNeverReset.LOG.warn("cannot register extra-heavy oil pretreatment: {}", exception.getMessage());
        }
    }

    public static boolean isIntermediate(FluidStack fluid) {
        return Intermediates.FLUIDS.contains(fluid.getFluid());
    }

    private static GTRecipeBuilder separation(Materials oil) {
        return stage(
            Step.SEPARATION,
            RecipeMaps.distillationTowerRecipes,
            9,
            Materials.NaphthenicAcid.getFluid(1),
            oil.getFluid(1));
    }

    private static GTRecipeBuilder desulfurization(FluidStack input, FluidStack output) {
        return stage(
            Step.DESULFURIZATION,
            RecipeMaps.multiblockChemicalReactorRecipes,
            4,
            output,
            input,
            Materials.Hydrogen.getGas(1));
    }

    private static GTRecipeBuilder stage(Step step, RecipeMap<?> pool, int circuit, FluidStack output,
        FluidStack... inputs) {
        ItemStack[] items = circuit == 0 ? GTValues.emptyItemStackArray
            : new ItemStack[] { GTUtility.getIntegratedCircuit(circuit) };
        List<GTRecipe> matches = pool.findRecipeQuery()
            .items(items)
            .fluids(inputs)
            .dontCheckStackSizes(true)
            .filter(
                recipe -> recipe.mEnabled && !recipe.mFakeRecipe
                    && recipe.mInputs.length == items.length
                    && (circuit == 0 || recipe.mInputs[0].getItemDamage() == circuit)
                    && recipe.mFluidInputs.length == inputs.length
                    && Arrays.stream(inputs)
                        .allMatch(
                            input -> Arrays.stream(recipe.mFluidInputs)
                                .anyMatch(input::isFluidEqual))
                    && Arrays.stream(recipe.mFluidOutputs)
                        .anyMatch(output::isFluidEqual))
            .findAll()
            .limit(2)
            .toList();
        if (matches.size() != 1) throw new IllegalStateException(
            "expected one source in " + pool.unlocalizedName
                + " for "
                + Arrays.toString(inputs)
                + " with circuit "
                + circuit
                + ", found "
                + matches.size());
        GTRecipe nativeRecipe = matches.get(0)
            .copy();
        return GTValues.RA.stdBuilder()
            .fluidInputs(nativeRecipe.mFluidInputs)
            .fluidOutputs(nativeRecipe.mFluidOutputs)
            .itemOutputs(nativeRecipe.mOutputs)
            .duration(nativeRecipe.mDuration)
            .eut(nativeRecipe.mEUt)
            .metadata(STEP, step);
    }

    public enum Step {
        REDISTILLATION,
        CRACKING,
        DESULFURIZATION,
        SEPARATION,
        PRETREATMENT
    }

    private static final class Intermediates {

        private static final Set<Fluid> FLUIDS = Set.of(
            Materials.OilHeavy.getFluid(1)
                .getFluid(),
            Materials.SulfuricHeavyFuel.getFluid(1)
                .getFluid(),
            Materials.SulfuricLightFuel.getFluid(1)
                .getFluid(),
            Materials.SulfuricNaphtha.getFluid(1)
                .getFluid(),
            Materials.SulfuricGas.getGas(1)
                .getFluid(),
            Materials.Gas.getGas(1)
                .getFluid(),
            Materials.Naphtha.getFluid(1)
                .getFluid(),
            Materials.Naphtha.getLightlySteamCracked(1)
                .getFluid(),
            Materials.Gas.getSeverelySteamCracked(1)
                .getFluid());
    }
}
