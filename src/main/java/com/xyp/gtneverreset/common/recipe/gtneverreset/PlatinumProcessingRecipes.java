package com.xyp.gtneverreset.common.recipe.gtneverreset;

import static bartworks.system.material.WerkstoffLoader.AcidicIridiumSolution;
import static bartworks.system.material.WerkstoffLoader.AcidicOsmiumSolution;
import static bartworks.system.material.WerkstoffLoader.AquaRegia;
import static bartworks.system.material.WerkstoffLoader.CrudeRhMetall;
import static bartworks.system.material.WerkstoffLoader.HotRutheniumTetroxideSollution;
import static bartworks.system.material.WerkstoffLoader.IrLeachResidue;
import static bartworks.system.material.WerkstoffLoader.IrOsLeachResidue;
import static bartworks.system.material.WerkstoffLoader.IridiumChloride;
import static bartworks.system.material.WerkstoffLoader.IridiumDioxide;
import static bartworks.system.material.WerkstoffLoader.LeachResidue;
import static bartworks.system.material.WerkstoffLoader.OsmiumSolution;
import static bartworks.system.material.WerkstoffLoader.PDAmmonia;
import static bartworks.system.material.WerkstoffLoader.PDMetallicPowder;
import static bartworks.system.material.WerkstoffLoader.PDRawPowder;
import static bartworks.system.material.WerkstoffLoader.PDSalt;
import static bartworks.system.material.WerkstoffLoader.PTConcentrate;
import static bartworks.system.material.WerkstoffLoader.PTMetallicPowder;
import static bartworks.system.material.WerkstoffLoader.PTRawPowder;
import static bartworks.system.material.WerkstoffLoader.PTResidue;
import static bartworks.system.material.WerkstoffLoader.PTSaltCrude;
import static bartworks.system.material.WerkstoffLoader.PTSaltRefined;
import static bartworks.system.material.WerkstoffLoader.RHFilterCakeSolution;
import static bartworks.system.material.WerkstoffLoader.RHNitrate;
import static bartworks.system.material.WerkstoffLoader.RHSalt;
import static bartworks.system.material.WerkstoffLoader.RHSaltSolution;
import static bartworks.system.material.WerkstoffLoader.RHSulfate;
import static bartworks.system.material.WerkstoffLoader.RHSulfateSolution;
import static bartworks.system.material.WerkstoffLoader.ReRh;
import static bartworks.system.material.WerkstoffLoader.RhFilterCake;
import static bartworks.system.material.WerkstoffLoader.Rhodium;
import static bartworks.system.material.WerkstoffLoader.Ruthenium;
import static bartworks.system.material.WerkstoffLoader.RutheniumTetroxide;
import static bartworks.system.material.WerkstoffLoader.RutheniumTetroxideSollution;
import static bartworks.system.material.WerkstoffLoader.SodiumRuthenate;
import static gregtech.api.enums.OrePrefixes.dust;
import static gregtech.api.enums.OrePrefixes.dustTiny;
import static gregtech.api.recipe.RecipeMaps.blastFurnaceRecipes;
import static gregtech.api.recipe.RecipeMaps.distillationTowerRecipes;
import static gregtech.api.recipe.RecipeMaps.fluidHeaterRecipes;
import static gregtech.api.recipe.RecipeMaps.fluidSolidifierRecipes;
import static gregtech.api.recipe.RecipeMaps.mixerRecipes;
import static gregtech.api.recipe.RecipeMaps.multiblockChemicalReactorRecipes;
import static gregtech.api.recipe.RecipeMaps.packagerRecipes;
import static gregtech.api.recipe.RecipeMaps.sifterRecipes;
import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;

import bartworks.system.material.Werkstoff;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.api.recipe.metadata.SimpleRecipeMetadataKey;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class PlatinumProcessingRecipes {

    @SuppressWarnings("unchecked")
    public static final RecipeMetadataKey<RecipeMap<?>> SOURCE = (RecipeMetadataKey<RecipeMap<?>>) (RecipeMetadataKey<?>) SimpleRecipeMetadataKey
        .create(RecipeMap.class, "gtnr_platinum_processing_source");
    public static final RecipeMetadataKey<Integer> STATION = SimpleRecipeMetadataKey
        .create(Integer.class, "gtnr_platinum_processing_station");
    public static final int STATION_COUNT = 44;

    private PlatinumProcessingRecipes() {}

    public static void register() {
        List<NativeStage> stages;
        try {
            // Resolve the entire route before publishing any of its stages.
            stages = List.of(
                // Platinum and its salt recovery loop.
                stage(multiblockChemicalReactorRecipes, 9, PTMetallicPowder.get(dust), PTResidue.get(dust)),
                stage(multiblockChemicalReactorRecipes, 1, PTMetallicPowder.get(dust), PTResidue.get(dustTiny)),
                stage(multiblockChemicalReactorRecipes, 3, fluid(PTConcentrate), PTSaltCrude.get(dust)),
                stage(multiblockChemicalReactorRecipes, 1, fluid(PTConcentrate), PTSaltCrude.get(dustTiny)),
                stage(multiblockChemicalReactorRecipes, -1, PTRawPowder.get(dust), Materials.Platinum.getDust(1)),
                stage(sifterRecipes, -1, PTSaltCrude.get(dust), PTSaltRefined.get(dust)),
                stage(blastFurnaceRecipes, 1, PTSaltRefined.get(dust), PTMetallicPowder.get(dust)),
                // Palladium digestion, enrichment, and salt recovery.
                stage(multiblockChemicalReactorRecipes, 9, PDMetallicPowder.get(dust), PDRawPowder.get(dust)),
                stage(multiblockChemicalReactorRecipes, 1, PDMetallicPowder.get(dust), PDRawPowder.get(dustTiny)),
                stage(multiblockChemicalReactorRecipes, 1, PDMetallicPowder.get(dust), fluid(PDAmmonia)),
                stage(multiblockChemicalReactorRecipes, 2, fluid(PDAmmonia), PDSalt.get(dust)),
                stage(sifterRecipes, -1, PDSalt.get(dust), PDMetallicPowder.get(dust)),
                stage(multiblockChemicalReactorRecipes, -1, PDRawPowder.get(dust), Materials.Palladium.getDust(1)),
                // Platinum residue splits into the rhodium and ruthenium branches.
                stage(blastFurnaceRecipes, 11, PTResidue.get(dust), LeachResidue.get(dust)),
                stage(multiblockChemicalReactorRecipes, 3, fluid(RHSulfate), fluid(RHSulfateSolution)),
                stage(multiblockChemicalReactorRecipes, 1, fluid(RHSulfate), fluid(RHSulfateSolution)),
                stage(multiblockChemicalReactorRecipes, -1, fluid(RHSulfateSolution), CrudeRhMetall.get(dust)),
                stage(blastFurnaceRecipes, -1, CrudeRhMetall.get(dust), RHSalt.get(dust)),
                stage(mixerRecipes, -1, RHSalt.get(dust), fluid(RHSaltSolution)),
                stage(multiblockChemicalReactorRecipes, 1, fluid(RHSaltSolution), RHNitrate.get(dust)),
                stage(sifterRecipes, -1, RHNitrate.get(dust), RhFilterCake.get(dust)),
                stage(mixerRecipes, -1, RhFilterCake.get(dust), fluid(RHFilterCakeSolution)),
                stage(multiblockChemicalReactorRecipes, -1, fluid(RHFilterCakeSolution), ReRh.get(dust)),
                stage(multiblockChemicalReactorRecipes, -1, ReRh.get(dust), Rhodium.get(dust)),
                stage(blastFurnaceRecipes, -1, LeachResidue.get(dust), SodiumRuthenate.get(dust)),
                stage(
                    multiblockChemicalReactorRecipes,
                    -1,
                    SodiumRuthenate.get(dust),
                    fluid(RutheniumTetroxideSollution)),
                stage(
                    fluidHeaterRecipes,
                    -1,
                    fluid(RutheniumTetroxideSollution),
                    fluid(HotRutheniumTetroxideSollution)),
                stage(distillationTowerRecipes, -1, fluid(HotRutheniumTetroxideSollution), fluid(RutheniumTetroxide)),
                stage(fluidSolidifierRecipes, 1, fluid(RutheniumTetroxide), RutheniumTetroxide.get(dust)),
                stage(multiblockChemicalReactorRecipes, -1, RutheniumTetroxide.get(dust), Ruthenium.get(dust)),
                // Ruthenium extraction leaves the common residue for osmium and iridium.
                stage(blastFurnaceRecipes, 11, IrOsLeachResidue.get(dust), fluid(AcidicOsmiumSolution)),
                stage(distillationTowerRecipes, -1, fluid(AcidicOsmiumSolution), fluid(OsmiumSolution)),
                stage(multiblockChemicalReactorRecipes, -1, fluid(OsmiumSolution), Materials.Osmium.getDust(1)),
                stage(blastFurnaceRecipes, 1, IrLeachResidue.get(dust), IridiumDioxide.get(dust)),
                stage(multiblockChemicalReactorRecipes, -1, IridiumDioxide.get(dust), fluid(AcidicIridiumSolution)),
                stage(multiblockChemicalReactorRecipes, -1, fluid(AcidicIridiumSolution), IridiumChloride.get(dust)),
                stage(multiblockChemicalReactorRecipes, -1, IridiumChloride.get(dust), Materials.Iridium.getDust(1)),
                packing(PTResidue),
                packing(PTSaltCrude),
                packing(PTRawPowder),
                packing(PDSalt),
                packing(PDRawPowder),
                packing(LeachResidue));
        } catch (IllegalStateException exception) {
            GTNeverReset.LOG.warn("cannot register platinum processing stages: {}", exception.getMessage());
            return;
        }
        for (int station = 0; station < stages.size(); station++) {
            registerStage(stages.get(station), station);
        }
        registerPurifiedOreFeeds();
    }

    private static void registerPurifiedOreFeeds() {
        FluidStack aquaRegia = fluid(AquaRegia);
        FluidStack concentrate = fluid(PTConcentrate);
        for (GTRecipe recipe : multiblockChemicalReactorRecipes.getAllRecipes()) {
            if (usable(recipe) && matchesCircuit(recipe, 1)
                && recipe.mInputs.length == 2
                && recipe.mFluidInputs.length == 1
                && recipe.mFluidInputs[0].isFluidEqual(aquaRegia)
                && recipe.mOutputs.length == 0
                && recipe.mFluidOutputs.length == 1
                && recipe.mFluidOutputs[0].isFluidEqual(concentrate)
                && Arrays.stream(recipe.mInputs)
                    .anyMatch(PlatinumProcessingRecipes::isPurifiedOre)) {
                registerStage(new NativeStage(multiblockChemicalReactorRecipes, recipe), STATION_COUNT - 1);
            }
        }
    }

    private static boolean isPurifiedOre(ItemStack item) {
        if (item == null || item.stackSize != 1 || GTUtility.isAnyIntegratedCircuit(item)) return false;
        for (int ore : OreDictionary.getOreIDs(item)) {
            if (OreDictionary.getOreName(ore)
                .startsWith("crushedPurified")) return true;
        }
        return false;
    }

    private static NativeStage stage(RecipeMap<?> map, int circuit, Object input, Object output) {
        return stage(map, circuit, input, output, recipe -> true);
    }

    private static NativeStage packing(Werkstoff material) {
        ItemStack schematic = ItemList.Schematic_Dust.get(1);
        return stage(
            packagerRecipes,
            -1,
            material.get(dustTiny),
            material.get(dust),
            recipe -> Arrays.stream(recipe.mInputs)
                .anyMatch(input -> GTUtility.areStacksEqual(input, schematic)));
    }

    private static NativeStage stage(RecipeMap<?> map, int circuit, Object input, Object output,
        Predicate<GTRecipe> extraMatch) {
        List<GTRecipe> candidates = map.getAllRecipes()
            .stream()
            .filter(
                recipe -> usable(recipe) && matchesCircuit(recipe, circuit)
                    && contains(recipe.mInputs, recipe.mFluidInputs, input)
                    && contains(recipe.mOutputs, recipe.mFluidOutputs, output)
                    && extraMatch.test(recipe))
            .limit(2)
            .toList();
        if (candidates.size() != 1) throw new IllegalStateException(
            "expected one recipe in " + map.unlocalizedName
                + " for "
                + input
                + " -> "
                + output
                + " (circuit "
                + circuit
                + "), found "
                + candidates.size());
        return new NativeStage(map, candidates.get(0));
    }

    private static boolean usable(GTRecipe recipe) {
        return recipe.mEnabled && !recipe.mFakeRecipe && recipe.mDuration > 0 && recipe.mEUt > 0;
    }

    private static boolean matchesCircuit(GTRecipe recipe, int circuit) {
        for (ItemStack item : recipe.mInputs) {
            if (item != null && GTUtility.isAnyIntegratedCircuit(item)) return item.getItemDamage() == circuit;
        }
        return circuit < 0;
    }

    private static boolean contains(ItemStack[] items, FluidStack[] fluids, Object ingredient) {
        return switch (ingredient) {
            case ItemStack stack -> Arrays.stream(items).anyMatch(item -> GTUtility.areStacksEqual(item, stack));
            case FluidStack stack -> Arrays.stream(fluids).anyMatch(stack::isFluidEqual);
            default -> false;
        };
    }

    private static FluidStack fluid(Werkstoff material) {
        return material.getFluidOrGas(1);
    }

    private static void registerStage(NativeStage stage, int station) {
        GTRecipe source = stage.recipe()
            .copy();
        GTRecipeBuilder builder = GTValues.RA.stdBuilder()
            .itemInputsUnified(source.mInputs, chances(source.mInputs.length, source::getInputChance))
            .itemOutputs(source.mOutputs, chances(source.mOutputs.length, source::getOutputChance))
            .fluidInputs(source.mFluidInputs, chances(source.mFluidInputs.length, source::getFluidInputChance))
            .fluidOutputs(source.mFluidOutputs, chances(source.mFluidOutputs.length, source::getFluidOutputChance))
            .duration(source.mDuration)
            .eut(source.mEUt)
            .specialValue(source.mSpecialValue)
            .metadata(SOURCE, stage.map())
            .metadata(STATION, station);
        Integer heat = stage.recipe()
            .getMetadata(COIL_HEAT);
        if (heat != null) builder.metadata(COIL_HEAT, heat);
        builder.addTo(GTNRRecipeMaps.PLATINUM_PROCESSING);
    }

    private static int[] chances(int count, IntUnaryOperator chance) {
        return IntStream.range(0, count)
            .map(chance)
            .toArray();
    }

    private record NativeStage(RecipeMap<?> map, GTRecipe recipe) {}
}
