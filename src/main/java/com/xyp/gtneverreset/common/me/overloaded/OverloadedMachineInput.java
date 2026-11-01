package com.xyp.gtneverreset.common.me.overloaded;

import static com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineRecipeMatcher.add;
import static com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineRecipeMatcher.addAE;
import static com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineRecipeMatcher.describe;
import static com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineRecipeMatcher.matches;
import static com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineRecipeMatcher.matchesConfiguration;
import static com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineRecipeMatcher.matchingInputs;
import static com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineRecipeMatcher.sameCircuit;
import static com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineRecipeMatcher.sameItem;
import static com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineRecipeMatcher.sameRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.api.mixinHelper.LargeInventoryCrafting;
import com.xyp.gtneverreset.common.machine.basicMachine.ModernFluidMachine;
import com.xyp.gtneverreset.common.me.CircuitPatternQuantities;
import com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineRecipeMatcher.Ingredient;
import com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineRecipeMatcher.Match;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.inv.MEInventoryCrafting;
import gregtech.api.enums.GTValues;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.util.GTRecipe;

/**
 * Selects a virtual circuit from a complete AE processing pattern and accepts its resources atomically.
 * All calls run synchronously on the server. Oversized batches retain their remaining inputs in the provider's
 * persistent buffer and refill physical slots as processing frees capacity. Changing circuits or molds requires
 * an idle machine with empty inputs. Recipe definitions are assumed
 * stable after pack startup, like the encoded pattern definitions cached by AE.
 */
public final class OverloadedMachineInput {

    /** Successful deliveries only; weak machine keys do not keep unloaded tiles alive. */
    private static final Map<MTEBasicMachine, Match> LAST_DELIVERIES = new WeakHashMap<>();

    /** At most eight distinct diagnostic reports per loaded tile; ordinary busy/capacity retries remain silent. */
    private static final Map<BaseMetaTileEntity, Set<String>> REPORTED_REJECTIONS = new WeakHashMap<>();

    private OverloadedMachineInput() {}

    /**
     * Keeps unsupported GT tile entities on AE's ordinary insertion path.
     *
     * @param tile receiving GT tile entity
     * @return whether automatic pattern reception owns this machine's AE input
     */
    public static boolean supports(BaseMetaTileEntity tile) {
        return tile.getMetaTileEntity() instanceof MTEBasicMachine machine && machine.allowSelectCircuit()
            && machine instanceof com.xyp.gtneverreset.api.machine.OverloadedMachineConfiguration
            && !machine.isSteampowered()
            && machine.getRecipeMap() != null;
    }

    /**
     * Validates recipe identity, actual CPU inputs and space for at least one runnable operation before committing.
     * A failed push leaves the circuit, input slots, tank and CPU table untouched. AE retries the complete batch.
     *
     * @param tile    receiving machine
     * @param pattern processing pattern with encoded tools retained as runtime configuration, or omitted
     * @param table   actual resources supplied by AE, including native fluid stacks
     * @param side    receiving machine face
     * @return true only when every supplied resource has been inserted
     */
    public static boolean push(BaseMetaTileEntity tile, ICraftingPatternDetails pattern, InventoryCrafting table,
        ForgeDirection side, OverloadedMachineBuffer overflow) {
        PreparedInput prepared = prepare(tile, pattern, table, side, overflow);
        return prepared != null && commit(List.of(prepared));
    }

    /** Prepares a delivery without changing machine inputs or claiming its overflow buffer. */
    public static PreparedInput prepare(BaseMetaTileEntity tile, ICraftingPatternDetails pattern,
        InventoryCrafting table, ForgeDirection side, OverloadedMachineBuffer overflow) {
        List<PreparedInput> prepared = new ArrayList<>(1);
        return prepare(tile, pattern, table, side, overflow, prepared) ? prepared.get(0) : null;
    }

    private static boolean prepare(BaseMetaTileEntity tile, ICraftingPatternDetails pattern, InventoryCrafting table,
        ForgeDirection side, OverloadedMachineBuffer overflow, List<PreparedInput> prepared) {
        if (!supports(tile) || tile.getWorldObj() == null
            || tile.getWorldObj().isRemote
            || side == null
            || side == ForgeDirection.UNKNOWN
            || pattern.isCraftable()
            || !(table instanceof MEInventoryCrafting aeTable)) return false;
        long crafts = table instanceof LargeInventoryCrafting large ? large.getAssemblerSize() : 1;
        if (crafts <= 0) return false;
        MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
        if (!tile.isAllowedToWork()) return false;
        if (!overflow.isEmpty()) return false;
        if (((com.xyp.gtneverreset.api.machine.OverloadedMachineConfiguration) machine).gtnr$hasPending()) return false;
        int first = machine.getInputSlot();
        Map<Ingredient, Long> buffered = new HashMap<>();
        for (int slot = first; slot < first + machine.mInputSlotCount; slot++) {
            ItemStack existing = machine.getStackInSlot(slot);
            if (existing != null && existing.stackSize > 0) {
                add(buffered, new Ingredient(existing, false), existing.stackSize);
            }
        }
        FluidStack[] oldFluids = copyInputFluids(machine);
        for (FluidStack oldFluid : oldFluids) {
            if (oldFluid != null && oldFluid.amount > 0)
                add(buffered, new Ingredient(oldFluid, false), oldFluid.amount);
        }

        Map<Ingredient, Long> actual = new HashMap<>();
        List<ItemStack> items = new ArrayList<>();
        List<FluidStack> fluids = new ArrayList<>();
        for (int slot = 0; slot < aeTable.getSizeInventory(); slot++) {
            IAEStack<?> stack = aeTable.getAEStackInSlot(slot);
            if (stack == null) continue;
            long amount = stack.getStackSize();
            if (amount <= 0 || amount > Integer.MAX_VALUE) return false;
            if (!addAE(actual, stack, false)) return false;
            if (stack instanceof IAEItemStack item) {
                ItemStack copy = item.getItemStack()
                    .copy();
                copy.stackSize = (int) amount;
                items.add(copy);
            } else if (stack instanceof IAEFluidStack liquid) {
                FluidStack copy = liquid.getFluidStack()
                    .copy();
                copy.amount = (int) amount;
                fluids.add(copy);
            } else return false;
        }
        Match selected = null;
        List<Match> candidates = matches(machine.getRecipeMap(), pattern);
        Map<Ingredient, Long> expectedOutputs = new HashMap<>();
        for (IAEStack<?> output : pattern.getAEOutputs())
            if (output != null && !addAE(expectedOutputs, output, true)) return false;
        for (Match candidate : candidates) {
            long expectedBatches = CircuitPatternQuantities.requestedOutputBatches(candidate.outputs, expectedOutputs);
            if (candidate.recipe.mEUt > GTValues.V[machine.mTier] || expectedBatches <= 0
                || !matchesDeliveryBatches(expectedBatches, crafts, matchingInputs(candidate, actual))) continue;
            if (selected != null && !sameCircuit(selected.circuit, candidate.circuit))
                return reportRejection(tile, pattern, aeTable, "ambiguous-circuit", candidates);
            // Several catalog lenses can produce the same encoded output from the same delivery.
            // Keep the first valid choice unless another matches the installed mold, so retries and
            // continuous refills do not switch catalysts merely because registration order differs.
            if (selected == null || sameCircuit(OverloadedMachineMolds.get(machine), candidate.mold))
                selected = candidate;
        }
        if (selected == null) return reportRejection(
            tile,
            pattern,
            aeTable,
            candidates.isEmpty() ? "no-pattern-match" : "voltage-or-delivered-inputs",
            candidates);
        if (!canAccept(
            machine.mMaxProgresstime > 0,
            !buffered.isEmpty(),
            sameCircuit(machine.getStackInSlot(machine.getCircuitSlot()), selected.circuit)
                && sameCircuit(OverloadedMachineMolds.get(machine), selected.mold),
            sameRecipe(LAST_DELIVERIES.get(machine), selected),
            buffered.isEmpty() || matchingInputs(selected, buffered) > 0)) return false;

        // Simulate sequential slot insertion against a temporary inventory, including GT's multi-stack filter.
        // Restore the original array before notifying GT of the committed change.
        ItemStack[] original = machine.mInventory.clone();
        ItemStack[] staged = machine.mInventory;
        ItemStack[] committed;
        List<ItemStack> pendingItems = new ArrayList<>();
        List<FluidStack> insertedFluids = new ArrayList<>();
        List<FluidStack> pendingFluids = new ArrayList<>();
        ItemStack originalMold = OverloadedMachineMolds.get(machine);
        staged[machine.getCircuitSlot()] = selected.circuit == null ? null : selected.circuit.copy();
        OverloadedMachineMolds.set(machine, selected.mold);
        try {
            // Stage sequential fills so different fluids cannot claim the same empty tank.
            // The finally block restores all tanks before this preparation returns.
            for (FluidStack fluid : fluids) {
                int accepted = tile.fill(side, fluid, false);
                if (accepted <= 0 || accepted > fluid.amount) return false;
                if (accepted > 0) {
                    FluidStack inserted = fluid.copy();
                    inserted.amount = accepted;
                    insertedFluids.add(inserted);
                    if (machine instanceof ModernFluidMachine modern) {
                        if (modern.fill(inserted, true) != accepted) return false;
                    } else {
                        FluidStack existing = machine.getFillableStack();
                        if (existing == null || existing.amount <= 0) machine.setFillableStack(inserted.copy());
                        else {
                            if (!existing.isFluidEqual(inserted)
                                || (long) existing.amount + accepted > Integer.MAX_VALUE) return false;
                            FluidStack combined = existing.copy();
                            combined.amount += accepted;
                            machine.setFillableStack(combined);
                        }
                    }
                }
                if (accepted < fluid.amount) {
                    FluidStack pending = fluid.copy();
                    pending.amount -= accepted;
                    pendingFluids.add(pending);
                }
            }
            for (ItemStack item : items) {
                int remaining = item.stackSize;
                boolean allowedSlot = false;
                for (int slot = first; slot < first + machine.mInputSlotCount && remaining > 0; slot++) {
                    if (!tile.canInsertItem(slot, item, side.ordinal())) continue;
                    allowedSlot = true;
                    ItemStack existing = staged[slot];
                    if (existing != null && existing.stackSize > 0 && !sameItem(existing, item)) continue;
                    int present = existing == null ? 0 : Math.max(0, existing.stackSize);
                    int moved = Math
                        .min(remaining, Math.min(item.getMaxStackSize(), tile.getInventoryStackLimit()) - present);
                    if (moved <= 0) continue;
                    ItemStack copy = item.copy();
                    copy.stackSize = present + moved;
                    staged[slot] = copy;
                    remaining -= moved;
                }
                if (remaining != 0) {
                    if (!allowedSlot && buffered.isEmpty() && machine.mMaxProgresstime <= 0)
                        return reportRejection(tile, pattern, aeTable, "empty-machine-insertion-filter", candidates);
                    ItemStack pending = item.copy();
                    pending.stackSize = remaining;
                    pendingItems.add(pending);
                }
            }
            ItemStack[] recipeInputs = new ItemStack[machine.mInputSlotCount + 1];
            System.arraycopy(staged, first, recipeInputs, 0, machine.mInputSlotCount);
            recipeInputs[machine.mInputSlotCount] = staged[machine.getCircuitSlot()];
            // Let GT choose a runnable recipe, then compare its contents. Duplicate registrations can
            // produce different recipe objects with the same inputs, outputs and configuration circuit.
            GTRecipe runnable = machine.getRecipeMap()
                .findRecipeQuery()
                .items(OverloadedMachineMolds.append(recipeInputs, selected.mold))
                .fluids(copyInputFluids(machine))
                .specialSlot(machine.getStackInSlot(machine.getSpecialSlotIndex()))
                .voltage(GTValues.V[machine.mTier])
                .find();
            Match runnableMatch = describe(runnable);
            if (runnableMatch == null || !matchesConfiguration(pattern, runnableMatch)
                || !sameRecipe(selected, runnableMatch))
                return reportRejection(
                    tile,
                    pattern,
                    aeTable,
                    "final-lookup-mismatch: " + recipeDescription(runnable),
                    candidates);
            committed = staged.clone();
        } finally {
            System.arraycopy(original, 0, machine.mInventory, 0, original.length);
            OverloadedMachineMolds.set(machine, originalMold);
            restoreInputFluids(machine, oldFluids);
        }
        prepared.add(
            new PreparedInput(
                tile,
                selected,
                original,
                committed,
                oldFluids,
                insertedFluids,
                pendingItems,
                pendingFluids,
                side,
                overflow));
        return true;
    }

    /** Commits freshly prepared deliveries on the same server tick, or restores every attempted fluid fill. */
    public static boolean commit(List<PreparedInput> prepared) {
        if (prepared.isEmpty()) return false;
        Set<BaseMetaTileEntity> targets = new HashSet<>();
        for (PreparedInput input : prepared) if (!targets.add(input.tile)) return false;
        int attempted = 0;
        boolean filled = false;
        try {
            for (PreparedInput input : prepared) {
                attempted++;
                if (!input.fill()) return false;
            }
            filled = true;
        } finally {
            if (!filled) for (int i = 0; i < attempted; i++) prepared.get(i)
                .restoreFluid();
        }
        for (PreparedInput input : prepared) input.commit();
        return true;
    }

    static boolean matchesDeliveryBatches(long patternBatches, long crafts, long deliveredBatches) {
        return patternBatches > 0 && crafts > 0
            && patternBatches <= Long.MAX_VALUE / crafts
            && deliveredBatches == patternBatches * crafts;
    }

    /** Moves accepted overflow into physical machine inputs as processing frees capacity. */
    public static void refill(MTEBasicMachine machine, OverloadedMachineBuffer overflow) {
        if (!(machine.getBaseMetaTileEntity() instanceof BaseMetaTileEntity tile)) return;
        if (overflow.isEmpty() || overflow.getSide() == ForgeDirection.UNKNOWN) return;
        ForgeDirection side = overflow.getSide();
        boolean changed = false;
        for (Iterator<FluidStack> iterator = overflow.getFluids()
            .iterator(); iterator.hasNext();) {
            FluidStack remainingFluid = iterator.next();
            int moved = tile.fill(side, remainingFluid, true);
            if (moved > 0) {
                remainingFluid.amount -= moved;
                if (remainingFluid.amount <= 0) iterator.remove();
                changed = true;
            }
        }
        int first = machine.getInputSlot();
        for (Iterator<ItemStack> iterator = overflow.getItems()
            .iterator(); iterator.hasNext();) {
            ItemStack pending = iterator.next();
            for (int slot = first; slot < first + machine.mInputSlotCount && pending.stackSize > 0; slot++) {
                if (!tile.canInsertItem(slot, pending, side.ordinal())) continue;
                ItemStack existing = machine.getStackInSlot(slot);
                if (existing != null && !sameItem(existing, pending)) continue;
                int present = existing == null ? 0 : Math.max(0, existing.stackSize);
                int moved = Math.min(
                    pending.stackSize,
                    Math.min(pending.getMaxStackSize(), tile.getInventoryStackLimit()) - present);
                if (moved <= 0) continue;
                ItemStack inserted = pending.copy();
                inserted.stackSize = present + moved;
                tile.setInventorySlotContents(slot, inserted);
                pending.stackSize -= moved;
                changed = true;
            }
            if (pending.stackSize <= 0) iterator.remove();
        }
        overflow.clearIfEmpty();
        ((com.xyp.gtneverreset.api.machine.OverloadedMachineConfiguration) machine)
            .gtnr$setPending(!overflow.isEmpty());
        if (changed) tile.markDirty();
    }

    /**
     * Records actionable matching failures without changing reception decisions or repeatedly logging AE retries.
     * Reports include raw stack metadata/NBT so material unification and wildcard expansion can be distinguished.
     * The bounded state holds no world references and is released when the receiving tile unloads.
     *
     * @return false, allowing use at an existing rejection point
     */
    private static boolean reportRejection(BaseMetaTileEntity tile, ICraftingPatternDetails pattern,
        MEInventoryCrafting table, String reason, List<Match> candidates) {
        Set<String> reported = REPORTED_REJECTIONS.computeIfAbsent(tile, ignored -> new HashSet<>());
        if (reported.size() >= 8 || !reported.add(pattern.hashCode() + ":" + reason)) return false;
        MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
        List<IAEStack<?>> delivered = new ArrayList<>();
        for (int slot = 0; slot < table.getSizeInventory(); slot++) {
            IAEStack<?> stack = table.getAEStackInSlot(slot);
            if (stack != null) delivered.add(stack);
        }
        GTNeverReset.LOG.info(
            "[AutoCircuit] rejected={} dimension={} pos={},{},{} tier={} pattern={} inputs={} outputs={} delivered={}",
            reason,
            tile.getWorldObj().provider.dimensionId,
            tile.xCoord,
            tile.yCoord,
            tile.zCoord,
            machine.mTier,
            pattern.getPattern()
                .writeToNBT(new NBTTagCompound()),
            Arrays.toString(pattern.getAEInputs()),
            Arrays.toString(pattern.getAEOutputs()),
            delivered);
        int shown = 0;
        for (GTRecipe recipe : machine.getRecipeMap()
            .getAllRecipes()) {
            boolean related = false;
            for (IAEStack<?> output : pattern.getAEOutputs()) {
                if (!(output instanceof IAEItemStack item)) continue;
                for (ItemStack recipeOutput : recipe.mOutputs) {
                    if (recipeOutput != null
                        && new Ingredient(recipeOutput, true).equals(new Ingredient(item.getItemStack(), true)))
                        related = true;
                }
            }
            if (!related) continue;
            GTNeverReset.LOG.info("[AutoCircuit] output-related recipe: {}", recipeDescription(recipe));
            if (++shown >= 8) break;
        }
        GTNeverReset.LOG
            .info("[AutoCircuit] cached candidates={} (at most 8 related recipes shown)", candidates.size());
        return false;
    }

    /** Serializes registered stacks, preserving quantities, metadata and tags for runtime recipe diagnosis. */
    private static String recipeDescription(GTRecipe recipe) {
        if (recipe == null) return "none";
        List<NBTTagCompound> inputs = new ArrayList<>();
        List<NBTTagCompound> outputs = new ArrayList<>();
        int[] outputChances = new int[recipe.mOutputs.length];
        for (int i = 0; i < outputChances.length; i++) outputChances[i] = recipe.getOutputChance(i);
        for (ItemStack item : recipe.mInputs) if (item != null) inputs.add(item.writeToNBT(new NBTTagCompound()));
        for (ItemStack item : recipe.mOutputs) if (item != null) outputs.add(item.writeToNBT(new NBTTagCompound()));
        return "inputs=" + inputs
            + " outputs="
            + outputs
            + " fluidInputs="
            + Arrays.toString(recipe.mFluidInputs)
            + " fluidOutputs="
            + Arrays.toString(recipe.mFluidOutputs)
            + " EUt="
            + recipe.mEUt
            + " enabled="
            + recipe.mEnabled
            + " fake="
            + recipe.mFakeRecipe
            + " special="
            + recipe.mSpecialItems
            + " inputChances="
            + Arrays.toString(recipe.mInputChances)
            + " fluidInputChances="
            + Arrays.toString(recipe.mFluidInputChances)
            + " outputChances="
            + Arrays.toString(outputChances);
    }

    private static boolean canAccept(boolean running, boolean buffered, boolean sameCircuit, boolean sameDelivery,
        boolean compatibleBuffer) {
        if (!running && !buffered) return true;
        return sameCircuit && compatibleBuffer && (!running || sameDelivery);
    }

    private static FluidStack[] copyInputFluids(MTEBasicMachine machine) {
        if (machine instanceof ModernFluidMachine modern) return modern.copyInputFluids();
        FluidStack fluid = machine.getFillableStack();
        return new FluidStack[] { fluid == null ? null : fluid.copy() };
    }

    private static void restoreInputFluids(MTEBasicMachine machine, FluidStack[] fluids) {
        if (machine instanceof ModernFluidMachine modern) modern.restoreInputFluids(fluids);
        else machine.setFillableStack(fluids[0] == null ? null : fluids[0].copy());
    }

    public static final class PreparedInput {

        private final BaseMetaTileEntity tile;
        private final MTEBasicMachine machine;
        private final Match selected;
        private final ItemStack[] original;
        private final ItemStack[] committed;
        private final FluidStack[] oldFluids;
        private final List<FluidStack> insertedFluids;
        private final List<ItemStack> pendingItems;
        private final List<FluidStack> pendingFluids;
        private final ForgeDirection side;
        private final OverloadedMachineBuffer overflow;

        private PreparedInput(BaseMetaTileEntity tile, Match selected, ItemStack[] original, ItemStack[] committed,
            FluidStack[] oldFluids, List<FluidStack> insertedFluids, List<ItemStack> pendingItems,
            List<FluidStack> pendingFluids, ForgeDirection side, OverloadedMachineBuffer overflow) {
            this.tile = tile;
            this.machine = (MTEBasicMachine) tile.getMetaTileEntity();
            this.selected = selected;
            this.original = original;
            this.committed = committed;
            this.oldFluids = oldFluids;
            this.insertedFluids = insertedFluids;
            this.pendingItems = pendingItems;
            this.pendingFluids = pendingFluids;
            this.side = side;
            this.overflow = overflow;
        }

        private boolean fill() {
            if (insertedFluids.isEmpty()) return true;
            ItemStack oldCircuit = machine.mInventory[machine.getCircuitSlot()];
            ItemStack oldMold = OverloadedMachineMolds.get(machine);
            machine.mInventory[machine.getCircuitSlot()] = committed[machine.getCircuitSlot()];
            OverloadedMachineMolds.set(machine, selected.mold);
            try {
                for (FluidStack fluid : insertedFluids) {
                    if (tile.fill(side, fluid, true) != fluid.amount) return false;
                }
                return true;
            } finally {
                machine.mInventory[machine.getCircuitSlot()] = oldCircuit;
                OverloadedMachineMolds.set(machine, oldMold);
            }
        }

        private void restoreFluid() {
            restoreInputFluids(machine, oldFluids);
        }

        private void commit() {
            ((com.xyp.gtneverreset.api.machine.OverloadedMachineConfiguration) machine)
                .gtnr$beginConfiguration(original[machine.getCircuitSlot()]);
            OverloadedMachineMolds.set(machine, selected.mold);
            tile.setInventorySlotContents(machine.getCircuitSlot(), committed[machine.getCircuitSlot()]);
            int first = machine.getInputSlot();
            for (int slot = first; slot < first + machine.mInputSlotCount; slot++) {
                tile.setInventorySlotContents(slot, committed[slot]);
            }
            overflow.replace(pendingItems, pendingFluids, side);
            ((com.xyp.gtneverreset.api.machine.OverloadedMachineConfiguration) machine)
                .gtnr$setPending(!overflow.isEmpty());
            LAST_DELIVERIES.put(machine, selected);
        }
    }

}
