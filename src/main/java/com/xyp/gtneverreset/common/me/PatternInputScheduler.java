package com.xyp.gtneverreset.common.me;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import org.apache.commons.lang3.ArrayUtils;

import com.xyp.gtneverreset.utils.machine.RecipeThreadProcessingLogic;

import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.IDualInputInventory;
import gregtech.common.tileentities.machines.IDualInputInventoryWithPattern;

/** Keeps pattern inventories separate while sharing the machine's transaction and scheduling budget. */
public final class PatternInputScheduler {

    private int cursor;

    private record Entry(IDualInputInventory inventory, ItemStack[] shared) {

        private RecipeThreadProcessingLogic.RecipeInputs inputs() {
            return new RecipeThreadProcessingLogic.RecipeInputs(
                ArrayUtils.addAll(shared, inventory.getItemInputs()),
                inventory.getFluidInputs());
        }
    }

    private static List<Entry> entries(List<IDualInputHatch> hatches) {
        List<Entry> entries = new ArrayList<>();
        Set<IDualInputInventory> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IDualInputHatch hatch : hatches) {
            ItemStack[] shared = hatch.getSharedItems();
            for (var it = hatch.inventories(); it.hasNext();) {
                IDualInputInventory inventory = it.next();
                if (seen.add(inventory) && !inventory.isEmpty()) entries.add(new Entry(inventory, shared));
            }
        }
        return entries;
    }

    public CheckRecipeResult checkNext(List<IDualInputHatch> hatches, RecipeThreadProcessingLogic logic) {
        List<Entry> entries = entries(hatches);
        CheckRecipeResult result = CheckRecipeResultRegistry.NO_RECIPE;
        for (int i = 0; i < entries.size(); i++) {
            int index = (cursor + i) % entries.size();
            Entry entry = entries.get(index);
            logic.resetPatternInventory();
            if (entry.inventory() instanceof IDualInputInventoryWithPattern pattern
                && !logic.tryCachePossibleRecipesFromPattern(pattern)) continue;
            var inputs = entry.inputs();
            logic.setInputItems(inputs.items())
                .setInputFluids(inputs.fluids());
            CheckRecipeResult found = logic.process();
            if (found.wasSuccessful()) {
                cursor = (index + 1) % entries.size();
                return found;
            }
            if (found != CheckRecipeResultRegistry.NO_RECIPE) result = found;
        }
        logic.resetPatternInventory();
        return result;
    }
}
