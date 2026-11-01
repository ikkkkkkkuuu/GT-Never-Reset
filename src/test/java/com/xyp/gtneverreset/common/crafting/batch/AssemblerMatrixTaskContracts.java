package com.xyp.gtneverreset.common.crafting.batch;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import appeng.api.storage.data.IAEItemStack;

public final class AssemblerMatrixTaskContracts {

    public static void main(String[] args) throws Exception {
        IAEItemStack stack = stack(1);
        checkPartialDelivery(stack);
        checkFailureRetry(stack);
        checkLegacySave(stack);
        System.out
            .println("Matrix batch conservation, partial delivery, retry and legacy NBT writing contracts passed");
    }

    private static void checkPartialDelivery(IAEItemStack stack) throws Exception {
        AssemblerMatrixTasks tasks = new AssemblerMatrixTasks();
        seed(tasks, stack, 100, 7, 11);
        require(tasks.takeBatch(64) == 64, "parallel limit splits a queued stack");
        require(
            tasks.batchView()
                .stream()
                .mapToLong(IAEItemStack::getStackSize)
                .sum() == 64,
            "active batch quantity");
        tasks.requeueBatch();
        tasks.requeueBatch();
        AtomicLong delivered = new AtomicLong();
        tasks.sendPending(value -> {
            long accepted = Math.min(20, value.getStackSize());
            delivered.addAndGet(accepted);
            long remaining = value.getStackSize() - accepted;
            return remaining == 0 ? null
                : value.copy()
                    .setStackSize(remaining);
        });
        require(delivered.get() > 0 && delivered.get() < 107, "partial network acceptance leaves queued outputs");
        tasks.sendPending(value -> {
            delivered.addAndGet(value.getStackSize());
            return null;
        });
        require(delivered.get() == 107 && !tasks.hasPending(), "partial outputs are neither lost nor duplicated");
        AtomicLong returned = new AtomicLong();
        tasks.sendReturns(value -> {
            returned.addAndGet(value.getStackSize());
            return null;
        });
        require(returned.get() == 11, "container returns remain independent of output batches");
    }

    private static void checkFailureRetry(IAEItemStack stack) throws Exception {
        AssemblerMatrixTasks tasks = new AssemblerMatrixTasks();
        seed(tasks, stack, 19, 0, 3);
        try {
            tasks.sendPending(value -> { throw new IllegalStateException("network detached"); });
            throw new AssertionError("insertion failure must propagate");
        } catch (IllegalStateException expected) {
            require("network detached".equals(expected.getMessage()), "expected insertion failure");
        }
        NBTTagCompound saved = new NBTTagCompound();
        tasks.writeToNBT(saved);
        require(
            saved.getTagList("OUTPUT_ITEMS", 10)
                .getCompoundTagAt(0)
                .getLong("Cnt") == 19,
            "failed insertion remains available for saving");
        AtomicLong delivered = new AtomicLong();
        tasks.sendPending(value -> {
            delivered.addAndGet(value.getStackSize());
            return null;
        });
        require(delivered.get() == 19, "failed insertion is recoverable on retry");
        tasks.sendReturns(value -> value);
        AtomicLong returned = new AtomicLong();
        tasks.sendReturns(value -> {
            returned.addAndGet(value.getStackSize());
            return null;
        });
        require(returned.get() == 3, "a full network retains container returns");
    }

    private static void checkLegacySave(IAEItemStack stack) throws Exception {
        long amount = 1L << 40;
        NBTTagCompound legacy = fixture(stack, amount, 17, 5);
        AssemblerMatrixTasks tasks = new AssemblerMatrixTasks();
        seed(tasks, stack, amount, 17, 5);
        NBTTagCompound written = new NBTTagCompound();
        tasks.writeToNBT(written);
        require(legacy.equals(written), "old tag names and long amounts are preserved");
        require(tasks.takeBatch(Long.MAX_VALUE) == amount + 17, "active batch joins pending work exactly once");
        tasks.mirrorBatch(List.of());
        tasks.readFromNBT(new NBTTagCompound());
        require(!tasks.hasPending() && !tasks.hasBatch(), "missing legacy tags load as empty queues");
    }

    @SuppressWarnings("unchecked")
    private static void seed(AssemblerMatrixTasks tasks, IAEItemStack stack, long pending, long batch, long returns)
        throws Exception {
        tasks.mirrorBatch(
            List.of(
                stack.copy()
                    .setStackSize(pending)));
        tasks.requeueBatch();
        if (batch > 0) tasks.mirrorBatch(
            List.of(
                stack.copy()
                    .setStackSize(batch)));
        if (returns > 0) {
            Field field = AssemblerMatrixTasks.class.getDeclaredField("returns");
            field.setAccessible(true);
            ((Deque<IAEItemStack>) field.get(tasks)).add(
                stack.copy()
                    .setStackSize(returns));
        }
    }

    private static IAEItemStack stack(long amount) {
        long[] size = { amount };
        return (IAEItemStack) Proxy.newProxyInstance(
            AssemblerMatrixTaskContracts.class.getClassLoader(),
            new Class<?>[] { IAEItemStack.class },
            (proxy, method, arguments) -> {
                switch (method.getName()) {
                    case "getStackSize":
                        return size[0];
                    case "setStackSize":
                        size[0] = (long) arguments[0];
                        return proxy;
                    case "copy":
                        return stack(size[0]);
                    case "writeToNBT":
                        ((NBTTagCompound) arguments[0]).setLong("Cnt", size[0]);
                        return null;
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });
    }

    private static NBTTagCompound fixture(IAEItemStack stack, long pending, long batch, long returns) {
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("OUTPUT_ITEMS", stacks(stack, pending));
        root.setTag("CACHED_OUTPUT_ITEMS", stacks(stack, batch));
        root.setTag("INPUT_ITEMS", stacks(stack, returns));
        return root;
    }

    private static NBTTagList stacks(IAEItemStack stack, long amount) {
        NBTTagList list = new NBTTagList();
        if (amount > 0) {
            NBTTagCompound tag = new NBTTagCompound();
            stack.copy()
                .setStackSize(amount)
                .writeToNBT(tag);
            list.appendTag(tag);
        }
        return list;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
