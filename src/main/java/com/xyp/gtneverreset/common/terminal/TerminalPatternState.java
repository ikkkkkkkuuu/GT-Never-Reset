package com.xyp.gtneverreset.common.terminal;

import java.util.Arrays;
import java.util.function.Supplier;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.tile.inventory.IAEStackInventory;

public final class TerminalPatternState {

    private final DualTerminalGuiObject terminal;
    private final Supplier<IAEStackInventory> inputs;
    private final Supplier<IAEStackInventory> outputs;
    private final IAEStack<?>[] craftingInputSnapshot = new IAEStack<?>[DualTerminalGuiObject.PATTERN_SLOT_COUNT];
    private final IAEStack<?>[] craftingOutputSnapshot = new IAEStack<?>[DualTerminalGuiObject.PATTERN_SLOT_COUNT];
    private final IAEStack<?>[] processingInputSnapshot = new IAEStack<?>[DualTerminalGuiObject.PATTERN_SLOT_COUNT];
    private final IAEStack<?>[] processingOutputSnapshot = new IAEStack<?>[DualTerminalGuiObject.PATTERN_SLOT_COUNT];
    private boolean craftingSnapshotValid;
    private boolean processingSnapshotValid;
    /**
     * The two snapshots are representations of one recipe, never independent
     * recipes. The flag is valid only while the active representation still
     * matches its snapshot. Editing either view invalidates the relationship;
     * the next mode switch then regenerates the target view from the active one.
     */
    private boolean patternSnapshotsLinked;

    public TerminalPatternState(DualTerminalGuiObject terminal, Supplier<IAEStackInventory> inputs,
        Supplier<IAEStackInventory> outputs) {
        this.terminal = terminal;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public boolean rememberBeforeSwitch(boolean crafting) {
        boolean canRestore = patternSnapshotsLinked && activePatternMatches(crafting);
        rememberActivePattern(crafting);
        return canRestore;
    }

    public void restoreMode(boolean crafting, IAEStack<?> craftingResult, boolean canRestoreLinkedView) {
        if (crafting) {
            if (!canRestoreLinkedView || !craftingSnapshotValid) seedCraftingFromProcessing();
            restorePattern(craftingInputSnapshot, craftingOutputSnapshot);
        } else {
            if (!canRestoreLinkedView || !processingSnapshotValid) seedProcessingFromCrafting(craftingResult);
            restorePattern(processingInputSnapshot, processingOutputSnapshot);
        }
    }

    public void linkAndPersist() {
        patternSnapshotsLinked = true;
        persistPatternSnapshots();
    }

    public void saveActive(boolean crafting) {
        patternSnapshotsLinked &= activePatternMatches(crafting);
        rememberActivePattern(crafting);
        terminal.setPatternSnapshotsLinked(patternSnapshotsLinked);
    }

    public void invalidateLink() {
        patternSnapshotsLinked = false;
        terminal.setPatternSnapshotsLinked(false);
    }

    public void loadPatternSnapshots(boolean crafting) {
        craftingSnapshotValid = terminal.hasPatternSnapshot(true);
        processingSnapshotValid = terminal.hasPatternSnapshot(false);
        patternSnapshotsLinked = terminal.arePatternSnapshotsLinked();
        copyArray(terminal.getPatternSnapshotInputs(true, craftingInputSnapshot.length), craftingInputSnapshot);
        copyArray(terminal.getPatternSnapshotOutputs(true, craftingOutputSnapshot.length), craftingOutputSnapshot);
        copyArray(terminal.getPatternSnapshotInputs(false, processingInputSnapshot.length), processingInputSnapshot);
        copyArray(terminal.getPatternSnapshotOutputs(false, processingOutputSnapshot.length), processingOutputSnapshot);

        if (crafting) {
            patternSnapshotsLinked &= activePatternMatches(true);
            copyInventory(inputs.get(), craftingInputSnapshot);
            // Crafting uses AE2's separate computed cOut slot. Anything left in
            // the shared processing-output inventory is stale and must not
            // become part of the crafting snapshot.
            clearArray(craftingOutputSnapshot);
            craftingSnapshotValid = true;
            migrateLegacyProcessingFluids();
        } else {
            patternSnapshotsLinked &= activePatternMatches(false);
            copyInventory(inputs.get(), processingInputSnapshot);
            copyInventory(outputs.get(), processingOutputSnapshot);
            processingSnapshotValid = true;
        }
        terminal.setPatternSnapshotsLinked(patternSnapshotsLinked);
    }

    private void migrateLegacyProcessingFluids() {
        if (processingSnapshotValid) return;
        IAEStack<?>[] legacyFluids = terminal.getProcessingFluidInputs(processingInputSnapshot.length);
        if (!containsStack(legacyFluids)) return;

        copyInventory(inputs.get(), processingInputSnapshot);
        copyInventory(outputs.get(), processingOutputSnapshot);
        for (int slot = 0; slot < legacyFluids.length; slot++) {
            if (legacyFluids[slot] != null) processingInputSnapshot[slot] = legacyFluids[slot].copy();
        }
        processingSnapshotValid = true;
        persistPatternSnapshot(false);
    }

    public void rememberActivePattern(boolean crafting) {
        IAEStack<?>[] inputs = crafting ? craftingInputSnapshot : processingInputSnapshot;
        IAEStack<?>[] outputs = crafting ? craftingOutputSnapshot : processingOutputSnapshot;
        copyInventory(this.inputs.get(), inputs);
        if (crafting) {
            clearArray(outputs);
        } else {
            copyInventory(this.outputs.get(), outputs);
        }
        if (crafting) {
            craftingSnapshotValid = true;
        } else {
            processingSnapshotValid = true;
        }
        persistPatternSnapshot(crafting);
    }

    private void seedCraftingFromProcessing() {
        clearArray(craftingInputSnapshot);
        clearArray(craftingOutputSnapshot);
        int target = 0;
        for (IAEStack<?> stack : processingInputSnapshot) {
            if (!(stack instanceof IAEItemStack) || target >= 9) continue;
            craftingInputSnapshot[target++] = stack.copy();
        }
        craftingSnapshotValid = true;
        persistPatternSnapshot(true);
    }

    private void seedProcessingFromCrafting(IAEStack<?> craftingResult) {
        terminal.setArcaneLayout(null);
        clearArray(processingInputSnapshot);
        clearArray(processingOutputSnapshot);
        boolean compact = terminal.getProcessingGridSize() == 4;
        int packedSlot = 0;
        for (int sourceSlot = 0; sourceSlot < 9; sourceSlot++) {
            IAEStack<?> stack = craftingInputSnapshot[sourceSlot];
            if (stack == null) continue;
            int targetSlot = compact ? packedSlot++ : sourceSlot;
            processingInputSnapshot[targetSlot] = stack.copy();
        }
        if (craftingResult != null) processingOutputSnapshot[0] = craftingResult.copy();
        processingSnapshotValid = true;
        persistPatternSnapshot(false);
    }

    private void restorePattern(IAEStack<?>[] inputSnapshot, IAEStack<?>[] outputSnapshot) {
        restoreInventory(inputs.get(), inputSnapshot);
        restoreInventory(outputs.get(), outputSnapshot);
    }

    private void persistPatternSnapshot(boolean crafting) {
        terminal.setPatternSnapshot(
            crafting,
            crafting ? craftingInputSnapshot : processingInputSnapshot,
            crafting ? craftingOutputSnapshot : processingOutputSnapshot);
    }

    private void persistPatternSnapshots() {
        persistPatternSnapshot(true);
        persistPatternSnapshot(false);
        terminal.setPatternSnapshotsLinked(patternSnapshotsLinked);
    }

    private boolean activePatternMatches(boolean crafting) {
        if (crafting) {
            return craftingSnapshotValid && craftingInventoryMatches(inputs.get(), craftingInputSnapshot);
        }
        return processingSnapshotValid && inventoryMatches(inputs.get(), processingInputSnapshot)
            && inventoryMatches(outputs.get(), processingOutputSnapshot);
    }

    private static boolean craftingInventoryMatches(IAEStackInventory inventory, IAEStack<?>[] snapshot) {
        int size = Math.max(inventory.getSizeInventory(), snapshot.length);
        for (int slot = 0; slot < size; slot++) {
            IAEStack<?> active = slot < inventory.getSizeInventory() ? inventory.getAEStackInSlot(slot) : null;
            IAEStack<?> saved = slot < snapshot.length ? snapshot[slot] : null;
            if (active == null || saved == null) {
                if (active != saved) return false;
            } else if (!(active instanceof IAEItemStack activeItem) || !(saved instanceof IAEItemStack savedItem)
                || !activeItem.isSameType(savedItem)) {
                    return false;
                }
        }
        return true;
    }

    private static boolean inventoryMatches(IAEStackInventory inventory, IAEStack<?>[] snapshot) {
        int size = Math.max(inventory.getSizeInventory(), snapshot.length);
        for (int slot = 0; slot < size; slot++) {
            IAEStack<?> active = slot < inventory.getSizeInventory() ? inventory.getAEStackInSlot(slot) : null;
            IAEStack<?> saved = slot < snapshot.length ? snapshot[slot] : null;
            if (active == null || saved == null) {
                if (active != saved) return false;
            } else if (!active.toNBTGeneric()
                .equals(saved.toNBTGeneric())) {
                    return false;
                }
        }
        return true;
    }

    private static void copyInventory(IAEStackInventory source, IAEStack<?>[] destination) {
        clearArray(destination);
        int size = Math.min(source.getSizeInventory(), destination.length);
        for (int slot = 0; slot < size; slot++) {
            IAEStack<?> stack = source.getAEStackInSlot(slot);
            destination[slot] = stack == null ? null : stack.copy();
        }
    }

    public static void restoreInventory(IAEStackInventory destination, IAEStack<?>[] source) {
        for (int slot = 0; slot < destination.getSizeInventory(); slot++) {
            IAEStack<?> stack = slot < source.length ? source[slot] : null;
            destination.putAEStackInSlot(slot, stack == null ? null : stack.copy());
        }
    }

    public static void compactInventory(IAEStackInventory inventory) {
        IAEStack<?>[] compacted = new IAEStack<?>[inventory.getSizeInventory()];
        int target = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            IAEStack<?> stack = inventory.getAEStackInSlot(slot);
            if (stack != null) compacted[target++] = stack.copy();
        }
        restoreInventory(inventory, compacted);
        inventory.markDirty();
    }

    private static void copyArray(IAEStack<?>[] source, IAEStack<?>[] destination) {
        clearArray(destination);
        int size = Math.min(source.length, destination.length);
        for (int slot = 0; slot < size; slot++) {
            destination[slot] = source[slot] == null ? null : source[slot].copy();
        }
    }

    private static void clearArray(IAEStack<?>[] stacks) {
        Arrays.fill(stacks, null);
    }

    private static boolean containsStack(IAEStack<?>[] stacks) {
        for (IAEStack<?> stack : stacks) {
            if (stack != null) return true;
        }
        return false;
    }

}
