package com.xyp.gtneverreset.common.crafting.chaos;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;

/** Presets select current pack recipes; they never synthesize cheaper replacement recipes. */
public final class ChaosPresets {

    private static final List<ChaosChain> CHAINS = new ArrayList<>();

    private ChaosPresets() {}

    public static List<ChaosChain> all() {
        return List.copyOf(CHAINS);
    }

    public static void initialize() {
        CHAINS.clear();
        // #tr gtnr.chaos.preset.iron
        // # Iron ore refining
        // # zh_CN 铁矿精炼
        ore("gtnr.chaos.preset.iron", Materials.Iron);
        // #tr gtnr.chaos.preset.copper
        // # Copper ore refining
        // # zh_CN 铜矿精炼
        ore("gtnr.chaos.preset.copper", Materials.Copper);
        // #tr gtnr.chaos.preset.gold
        // # Gold ore refining
        // # zh_CN 金矿精炼
        ore("gtnr.chaos.preset.gold", Materials.Gold);
        // #tr gtnr.chaos.preset.tin
        // # Tin ore refining
        // # zh_CN 锡矿精炼
        ore("gtnr.chaos.preset.tin", Materials.Tin);
        // #tr gtnr.chaos.preset.nickel
        // # Nickel ore refining
        // # zh_CN 镍矿精炼
        ore("gtnr.chaos.preset.nickel", Materials.Nickel);
        // #tr gtnr.chaos.preset.lead
        // # Lead ore refining
        // # zh_CN 铅矿精炼
        ore("gtnr.chaos.preset.lead", Materials.Lead);
    }

    private static void ore(String name, Materials material) {
        List<ChaosChain.Step> steps = new ArrayList<>();
        if (!select(steps, RecipeMaps.maceratorRecipes, OrePrefixes.ore, OrePrefixes.crushed, material)) return;
        if (!select(steps, RecipeMaps.oreWasherRecipes, OrePrefixes.crushed, OrePrefixes.crushedPurified, material))
            return;
        if (!select(
            steps,
            RecipeMaps.thermalCentrifugeRecipes,
            OrePrefixes.crushedPurified,
            OrePrefixes.crushedCentrifuged,
            material)) return;
        if (!select(steps, RecipeMaps.maceratorRecipes, OrePrefixes.crushedCentrifuged, OrePrefixes.dust, material))
            return;
        CHAINS.add(new ChaosChain(name, steps));
    }

    private static boolean select(List<ChaosChain.Step> steps, RecipeMap<?> map, OrePrefixes in, OrePrefixes out,
        Materials material) {
        ItemStack input = GTOreDictUnificator.get(in, material, 1);
        ItemStack output = GTOreDictUnificator.get(out, material, 1);
        if (input == null || output == null) return false;
        GTRecipe selected = null;
        for (GTRecipe recipe : map.getAllRecipes()) {
            if (!ChaosChain.safe(recipe) || recipe.mInputs.length != 1 || recipe.mOutputs.length == 0) continue;
            ItemStack first = recipe.mInputs[0];
            ItemStack product = recipe.mOutputs[0];
            if (first == null || product == null
                || first.stackSize <= 0
                || product.stackSize <= 0
                || recipe.getInputChance(0) != 10000
                || recipe.getOutputChance(0) != 10000
                || !first.isItemEqual(input)
                || !product.isItemEqual(output)) continue;
            if (selected == null || recipe.mEUt < selected.mEUt) selected = recipe;
        }
        if (selected == null) return false;
        // Preserve the per-step source stoichiometry by executing enough downstream runs for the primary output.
        int repeats = 1;
        if (!steps.isEmpty()) {
            ChaosChain.Step previous = steps.get(steps.size() - 1);
            long amount = (long) previous.recipe().mOutputs[0].stackSize * previous.repeats();
            if (amount % selected.mInputs[0].stackSize != 0 || amount / selected.mInputs[0].stackSize > 4096)
                return false;
            repeats = (int) (amount / selected.mInputs[0].stackSize);
        }
        steps.add(new ChaosChain.Step(map, selected, repeats));
        return true;
    }
}
