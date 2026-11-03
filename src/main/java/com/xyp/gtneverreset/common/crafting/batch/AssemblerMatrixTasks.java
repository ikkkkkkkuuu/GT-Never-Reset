package com.xyp.gtneverreset.common.crafting.batch;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.api.mixinHelper.LargeInventoryCrafting;
import com.xyp.gtneverreset.common.me.DireCraftingPatternDetails;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;

public final class AssemblerMatrixTasks {

    private static final String BATCH_TAG = "CACHED_OUTPUT_ITEMS";
    private static final String PENDING_TAG = "OUTPUT_ITEMS";
    private static final String RETURNS_TAG = "INPUT_ITEMS";
    private final Deque<IAEItemStack> pending = new ArrayDeque<>();
    private final Deque<IAEItemStack> returns = new ArrayDeque<>();
    private final List<IAEItemStack> batch = new ArrayList<>();

    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table,
        Function<ItemStack, ItemStack> containerResolver) {
        if (!(patternDetails instanceof DireCraftingPatternDetails direPattern)) {
            return false;
        }

        if (!(table instanceof LargeInventoryCrafting largeInventory)) {
            GTNeverReset.LOG.error("Assembler Matrix received a crafting inventory without long batch metadata");
            return false;
        }
        long assemblerSize = largeInventory.getAssemblerSize();
        if (assemblerSize < 1) {
            GTNeverReset.LOG.error("Assembler Matrix received an invalid crafting batch size: {}", assemblerSize);
            return false;
        }

        Deque<IAEItemStack> batchInputs = new ArrayDeque<>();
        Deque<IAEItemStack> batchOutputs = new ArrayDeque<>();
        IAEItemStack[] patternInputs = direPattern.getInputs();
        try {
            for (int slot = 0; slot < table.getSizeInventory(); slot++) {
                ItemStack stack = table.getStackInSlot(slot);
                if (stack != null) {
                    ItemStack containerItem = containerResolver.apply(stack);
                    if (containerItem != null) {
                        IAEItemStack patternInput = slot < patternInputs.length ? patternInputs[slot] : null;
                        long containerAmount = patternInput == null ? assemblerSize
                            : multiplyStackSize(patternInput.getStackSize(), assemblerSize);
                        batchInputs.add(
                            AEItemStack.create(containerItem)
                                .setStackSize(containerAmount));
                    }
                }
            }
            for (IAEItemStack output : patternDetails.getCondensedOutputs()) {
                if (output == null) continue;
                batchOutputs.add(
                    output.copy()
                        .setStackSize(multiplyStackSize(output.getStackSize(), assemblerSize)));
            }
        } catch (ArithmeticException exception) {
            GTNeverReset.LOG.error("Assembler Matrix stack overflowed after an accepted AE batch plan", exception);
            return false;
        }

        for (int slot = 0; slot < table.getSizeInventory(); slot++) {
            ItemStack stack = table.getStackInSlot(slot);
            if (stack != null) stack.stackSize = 1;
        }
        returns.addAll(batchInputs);
        pending.addAll(batchOutputs);
        return true;
    }

    public boolean hasPending() {
        return !pending.isEmpty();
    }

    public boolean hasReturns() {
        return !returns.isEmpty();
    }

    public boolean hasQueuedWork() {
        return hasPending() || hasReturns();
    }

    public boolean hasBatch() {
        return !batch.isEmpty();
    }

    public void sendReturns(UnaryOperator<IAEItemStack> insert) {
        send(returns, insert);
    }

    public long takeBatch(long parallel) {
        requeueBatch();
        long taken = 0;
        for (int count = pending.size(); count > 0 && parallel > 0; count--) {
            IAEItemStack stack = pending.removeFirst();
            long size = stack.getStackSize();
            if (size <= parallel) {
                batch.add(stack);
                taken += size;
                parallel -= size;
            } else {
                batch.add(
                    stack.copy()
                        .setStackSize(parallel));
                pending.addLast(
                    stack.copy()
                        .setStackSize(size - parallel));
                taken += parallel;
                parallel = 0;
            }
        }
        return taken;
    }

    public void requeueBatch() {
        pending.addAll(batch);
        batch.clear();
    }

    public void sendPending(UnaryOperator<IAEItemStack> insert) {
        send(pending, insert);
    }

    public List<IAEItemStack> batchView() {
        return Collections.unmodifiableList(batch);
    }

    public void mirrorBatch(List<IAEItemStack> stacks) {
        batch.clear();
        addAll(batch, stacks);
    }

    public void clear() {
        pending.clear();
        returns.clear();
        batch.clear();
    }

    public void writeToNBT(NBTTagCompound root) {
        root.setTag(BATCH_TAG, write(batch));
        root.setTag(PENDING_TAG, write(pending));
        root.setTag(RETURNS_TAG, write(returns));
    }

    public void readFromNBT(NBTTagCompound root) {
        clear();
        read(root, BATCH_TAG, batch);
        read(root, PENDING_TAG, pending);
        read(root, RETURNS_TAG, returns);
    }

    private static long multiplyStackSize(long stackSize, long multiplier) {
        if (stackSize <= 0 || multiplier <= 0) {
            throw new ArithmeticException("Assembler Matrix stack quantities must be positive");
        }
        return Math.multiplyExact(stackSize, multiplier);
    }

    private static void send(Deque<IAEItemStack> queue, UnaryOperator<IAEItemStack> insert) {
        for (int count = queue.size(); count > 0; count--) {
            IAEItemStack stack = queue.removeFirst();
            IAEItemStack leftover;
            try {
                leftover = insert.apply(stack);
            } catch (RuntimeException failure) {
                queue.addFirst(stack);
                throw failure;
            }
            if (leftover != null) queue.addLast(leftover);
        }
    }

    private static void addAll(Collection<IAEItemStack> target, Collection<IAEItemStack> stacks) {
        for (IAEItemStack stack : stacks) {
            if (stack != null) target.add(stack);
        }
    }

    private static NBTTagList write(Collection<IAEItemStack> stacks) {
        NBTTagList list = new NBTTagList();
        for (IAEItemStack stack : stacks) {
            NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBT(tag);
            list.appendTag(tag);
        }
        return list;
    }

    private static void read(NBTTagCompound root, String key, Collection<IAEItemStack> target) {
        NBTTagList list = root.getTagList(key, 10);
        for (int index = 0; index < list.tagCount(); index++) {
            IAEItemStack stack = AEItemStack.loadItemStackFromNBT(list.getCompoundTagAt(index));
            if (stack != null) target.add(stack);
        }
    }
}
