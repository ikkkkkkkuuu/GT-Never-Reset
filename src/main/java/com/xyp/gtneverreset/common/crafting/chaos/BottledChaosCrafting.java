package com.xyp.gtneverreset.common.crafting.chaos;

import java.util.List;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.google.common.collect.ImmutableList;
import com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructurePackaging;
import com.xyp.gtneverreset.common.item.items.ChaosStructureModuleItem;

/** Shared workbench transaction: cached previews are detached from the persistent inventory. */
public final class BottledChaosCrafting {

    public static final int CARRIER_SLOT = 0;
    public static final int MATERIAL_SLOT_START = 1;
    public static final int MATERIAL_SLOT_COUNT = 8;
    public static final int OUTPUT_SLOT = 9;
    public static final int SECOND_OUTPUT_SLOT = 10;
    public static final int SLOT_COUNT = SECOND_OUTPUT_SLOT + 1;

    private final ItemStackHandler inventory;
    private final Runnable changed;
    private Preview preview;
    private long revision;
    private boolean dirty = true;
    private boolean committing;
    // #tr gtnr.machine.BottleInChaos.gui.credential_injection.ready
    // # Ready
    // # zh_CN 就绪
    private String status = "gtnr.machine.BottleInChaos.gui.credential_injection.ready";

    public BottledChaosCrafting(ItemStackHandler inventory, Runnable changed) {
        this.inventory = inventory;
        this.changed = changed;
    }

    public void invalidate() {
        if (committing) return;
        dirty = true;
        revision++;
    }

    public String status() {
        preview();
        return status;
    }

    /** Re-evaluate only after an input change, including at most one copied-disk transaction. */
    public Preview preview() {
        if (!dirty) return preview;
        dirty = false;
        preview = null;
        ItemStack[] before = snapshot();
        if (before[OUTPUT_SLOT] != null || before[SECOND_OUTPUT_SLOT] != null) {
            // #tr gtnr.machine.BottleInChaos.gui.credential_injection.output_occupied
            // # Output slots occupied.
            // # zh_CN 结果槽被占用.
            status = "gtnr.machine.BottleInChaos.gui.credential_injection.output_occupied";
            return null;
        }
        ItemStackHandler after = new ItemStackHandler(inventory.getSlots());
        for (int slot = 0; slot < before.length; slot++) after.setStackInSlot(slot, copy(before[slot]));
        Result result = ChaosStructureModuleItem.isModule(before[CARRIER_SLOT])
            ? BottledChaosStructurePackaging.execute(after)
            : Result.failure("gtnr.machine.BottleInChaos.gui.credential_injection.missing_target");
        status = result.statusKey();
        if (!result.wasSuccessful()) return null;
        preview = new Preview(revision, before, after, result.primaryOutputs());
        status = "";
        return preview;
    }

    public boolean isCurrent(Preview candidate) {
        return candidate != null && candidate == preview() && candidate.revision == revision;
    }

    /** Check the complete snapshot at take time; no periodic equality scan of large ME disk NBT is needed. */
    public boolean canTake(Preview candidate) {
        if (!isCurrent(candidate)) return false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (!ItemStack.areItemStacksEqual(candidate.before[slot], inventory.getStackInSlot(slot))) {
                invalidate();
                return false;
            }
        }
        return true;
    }

    /** Commit both results once. The untaken result becomes an ordinary, persistent inventory item. */
    public boolean take(Preview candidate, int variant, int outputSlot) {
        if (!canTake(candidate) || variant < 0
            || variant >= candidate.outputs.size()
            || outputSlot < OUTPUT_SLOT
            || outputSlot > SECOND_OUTPUT_SLOT
            || candidate.output(outputSlot, variant) == null) return false;
        committing = true;
        try {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = slot == OUTPUT_SLOT ? candidate.outputs.get(variant)
                    : candidate.after.getStackInSlot(slot);
                inventory.setStackInSlot(slot, slot == outputSlot ? null : copy(stack));
            }
        } finally {
            committing = false;
            invalidate();
        }
        changed.run();
        return true;
    }

    public ItemStack result(int outputSlot) {
        return displayStack(inventory.getStackInSlot(outputSlot));
    }

    public void takeResult(int outputSlot) {
        inventory.setStackInSlot(outputSlot, null);
    }

    private ItemStack[] snapshot() {
        ItemStack[] stacks = new ItemStack[inventory.getSlots()];
        for (int slot = 0; slot < stacks.length; slot++) stacks[slot] = copy(inventory.getStackInSlot(slot));
        return stacks;
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    /** Detach the mutable count for native slot clicks; disk and module NBT remain read-only during preview. */
    public static ItemStack displayStack(ItemStack stack) {
        if (stack == null) return null;
        ItemStack display = new ItemStack(stack.getItem(), stack.stackSize, stack.getItemDamage());
        display.setTagCompound(stack.getTagCompound());
        return display;
    }

    /** One immutable input snapshot and deduction plan, with interchangeable primary results. */
    public static final class Preview {

        private final long revision;
        private final ItemStack[] before;
        private final ItemStackHandler after;
        private final List<ItemStack> outputs;

        private Preview(long revision, ItemStack[] before, ItemStackHandler after, List<ItemStack> outputs) {
            this.revision = revision;
            this.before = before;
            this.after = after;
            this.outputs = outputs;
        }

        public long revision() {
            return revision;
        }

        public int variants() {
            return outputs.size();
        }

        public ItemStack output(int slot, int variant) {
            return displayStack(slot == OUTPUT_SLOT ? outputs.get(variant) : after.getStackInSlot(slot));
        }
    }

    /**
     * Outcome of an operation on a detached inventory. Primary alternatives share its deduction plan and secondary
     * output; their stacks are owned by that preview and must not be mutated by callers.
     */
    public record Result(boolean wasSuccessful, String statusKey, List<ItemStack> primaryOutputs) {

        public static Result success(String statusKey, ItemStack primaryOutput) {
            return success(statusKey, ImmutableList.of(primaryOutput));
        }

        public static Result success(String statusKey, List<ItemStack> primaryOutputs) {
            return new Result(true, statusKey, ImmutableList.copyOf(primaryOutputs));
        }

        public static Result failure(String statusKey) {
            return new Result(false, statusKey, ImmutableList.of());
        }
    }

}
