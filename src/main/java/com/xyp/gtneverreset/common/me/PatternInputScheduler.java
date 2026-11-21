package com.xyp.gtneverreset.common.me;

import java.util.ArrayDeque;
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
    private final ArrayDeque<Integer> candidates = new ArrayDeque<>();
    private int selectedIndex = -1;
    private boolean startedInPass;

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
        int count = passEntries.size();
        for (int i = 0; i < count; i++) candidates.addLast((cursor + i) % count);
        startedInPass = false;
    }

    public void endPass() {
        passEntries = null;
        selectedIndex = -1;
        candidates.clear();
    }

    public boolean inPass() {
        return passEntries != null;
    }

    public boolean selectNext() {
        Integer next = candidates.pollFirst();
        selectedIndex = next == null ? -1 : next;
        return next != null;
    }

    public void finishSelection(boolean started) {
        if (started || !startedInPass) cursor = (selectedIndex + 1) % passEntries.size();
        if (started) {
            startedInPass = true;
            if (!passEntries.get(selectedIndex)
                .inventory()
                .isEmpty()) candidates.addLast(selectedIndex);
        }
        selectedIndex = -1;
    }

    public boolean hasSelection() {
        return passEntries != null && selectedIndex >= 0 && selectedIndex < passEntries.size();
    }

    public CheckRecipeResult processSelected(RecipeThreadProcessingLogic logic) {
        if (!hasSelection()) return CheckRecipeResultRegistry.NO_RECIPE;
        Entry entry = passEntries.get(selectedIndex);
        if (entry.inventory()
            .isEmpty()) return CheckRecipeResultRegistry.NO_RECIPE;
        return processEntry(entry, logic);
    }

    public CheckRecipeResult checkNext(List<IDualInputHatch> hatches, RecipeThreadProcessingLogic logic) {
        List<Entry> entries = entries(hatches);
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
