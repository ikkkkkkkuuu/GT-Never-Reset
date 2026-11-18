package com.xyp.gtneverreset.common.me;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import org.apache.commons.lang3.ArrayUtils;

import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;

import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.IDualInputInventory;
import gregtech.common.tileentities.machines.IDualInputInventoryWithPattern;

/** Keeps pattern inventories separate while sharing the machine's transaction and scheduling budget. */
public final class PatternInputScheduler {

    private int cursor;
    private List<Entry> passEntries;
    private int selectedIndex = -1;

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

    /** Builds the snapshot reused by every start in one fill pass. */
    public void beginPass(List<IDualInputHatch> hatches) {
        passEntries = entries(hatches);
        selectedIndex = -1;
    }

    public void endPass() {
        passEntries = null;
        selectedIndex = -1;
    }

    public int entryCount() {
        return passEntries == null ? 0 : passEntries.size();
    }

    /** Iterates the snapshot from the persistent cursor so repeated passes still rotate recipe types. */
    public int[] passOrder() {
        int size = entryCount();
        int[] order = new int[size];
        for (int i = 0; i < size; i++) order[i] = (cursor + i) % size;
        return order;
    }

    public boolean isStocked(int index) {
        return passEntries != null && index >= 0
            && index < passEntries.size()
            && !passEntries.get(index)
                .inventory()
                .isEmpty();
    }

    public void select(int index) {
        selectedIndex = index;
    }

    public void clearSelection() {
        selectedIndex = -1;
    }

    /** Moves the next pass to the following pattern when a bounded pass could not start any thread. */
    public void advanceCursor() {
        if (passEntries != null && !passEntries.isEmpty()) cursor = (cursor + 1) % passEntries.size();
    }

    public boolean hasSelection() {
        return passEntries != null && selectedIndex >= 0 && selectedIndex < passEntries.size();
    }

    public CheckRecipeResult processSelected(RecipeThreadProcessingLogic logic) {
        if (!hasSelection()) return CheckRecipeResultRegistry.NO_RECIPE;
        Entry entry = passEntries.get(selectedIndex);
        if (entry.inventory()
            .isEmpty()) return CheckRecipeResultRegistry.NO_RECIPE;
        CheckRecipeResult found = processEntry(entry, logic);
        if (found.wasSuccessful()) cursor = (selectedIndex + 1) % passEntries.size();
        return found;
    }

    public CheckRecipeResult checkNext(List<IDualInputHatch> hatches, RecipeThreadProcessingLogic logic) {
        List<Entry> entries = passEntries != null ? passEntries : entries(hatches);
        CheckRecipeResult result = CheckRecipeResultRegistry.NO_RECIPE;
        for (int i = 0; i < entries.size(); i++) {
            int index = (cursor + i) % entries.size();
            Entry entry = entries.get(index);
            if (entry.inventory()
                .isEmpty()) continue;
            CheckRecipeResult found = processEntry(entry, logic);
            if (found.wasSuccessful()) {
                cursor = (index + 1) % entries.size();
                return found;
            }
            if (found != CheckRecipeResultRegistry.NO_RECIPE) result = found;
        }
        logic.resetPatternInventory();
        return result;
    }

    private CheckRecipeResult processEntry(Entry entry, RecipeThreadProcessingLogic logic) {
        logic.resetPatternInventory();
        if (entry.inventory() instanceof IDualInputInventoryWithPattern pattern
            && !logic.tryCachePossibleRecipesFromPattern(pattern)) return CheckRecipeResultRegistry.NO_RECIPE;
        var inputs = entry.inputs();
        logic.setInputItems(inputs.items())
            .setInputFluids(inputs.fluids());
        return logic.process();
    }
}
