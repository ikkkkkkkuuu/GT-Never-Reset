package com.xyp.gtneverreset.common.me;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.storage.data.IAEStack;
import appeng.me.cluster.implementations.CraftingCPUCluster;

/** Output quantities planned and received by each native AE crafting job. Accessed on the server thread. */
public final class CraftingJobProgress {

    private static final String NBT_PROGRESS = "gtnr:craftingJobProgress";
    private static final Map<CraftingCPUCluster, Job> JOBS = new WeakHashMap<>();

    private CraftingJobProgress() {}

    public static Progress get(CraftingCPUCluster cpu, IAEStack<?> stack) {
        Job job = currentJob(cpu);
        return job == null ? null : job.outputs.get(stack);
    }

    public static void begin(CraftingCPUCluster cpu) {
        String id = craftingId(cpu);
        if (id != null) JOBS.put(cpu, new Job(id, new HashMap<>()));
        else clear(cpu);
    }

    /** AE enumerates outstanding outputs at successful submit and merge, excluding the task's stored inventory. */
    public static void updatePlan(CraftingCPUCluster cpu, Iterable<IAEStack<?>> outstanding) {
        Job job = currentJob(cpu);
        if (job == null) return;
        Map<IAEStack<?>, Progress> outputs = job.outputs;
        outputs.replaceAll((stack, progress) -> new Progress(progress.completed, progress.completed));
        for (IAEStack<?> stack : outstanding) {
            if (stack.getStackSize() <= 0) continue;
            Progress previous = outputs.get(stack);
            long completed = previous == null ? 0 : previous.completed;
            outputs.put(key(stack), new Progress(completed, add(completed, stack.getStackSize())));
        }
        outputs.values()
            .removeIf(progress -> progress.total == 0);
    }

    public static void expectRemainder(CraftingCPUCluster cpu, IAEStack<?> stack) {
        Job job = currentJob(cpu);
        long amount = stack.getStackSize();
        if (job == null || amount <= 0) return;
        Progress previous = job.outputs.get(stack);
        if (previous == null) job.outputs.put(key(stack), new Progress(0, amount));
        else job.outputs.put(stack, new Progress(previous.completed, add(previous.total, amount)));
    }

    public static void receive(CraftingCPUCluster cpu, IAEStack<?> stack) {
        Job job = currentJob(cpu);
        if (job == null || stack.getStackSize() <= 0) return;
        Progress previous = job.outputs.get(stack);
        if (previous == null || previous.completed >= previous.total) return;
        long completed = previous.completed + Math.min(stack.getStackSize(), previous.total - previous.completed);
        job.outputs.put(stack, new Progress(completed, previous.total));
    }

    public static void clear(CraftingCPUCluster cpu) {
        JOBS.remove(cpu);
    }

    public static void write(CraftingCPUCluster cpu, NBTTagCompound data) {
        Job job = currentJob(cpu);
        if (job == null) {
            data.removeTag(NBT_PROGRESS);
            return;
        }
        var saved = new NBTTagCompound();
        saved.setString("craftingId", job.id);
        var entries = new NBTTagList();
        for (var output : job.outputs.entrySet()) {
            var entry = new NBTTagCompound();
            entry.setTag(
                "stack",
                output.getKey()
                    .toNBTGeneric());
            entry.setLong("completed", output.getValue().completed);
            entry.setLong("total", output.getValue().total);
            entries.appendTag(entry);
        }
        saved.setTag("outputs", entries);
        data.setTag(NBT_PROGRESS, saved);
    }

    public static void read(CraftingCPUCluster cpu, NBTTagCompound data) {
        clear(cpu);
        String id = craftingId(cpu);
        // Jobs saved before this ledger was added have no recoverable per-output history.
        if (id == null || !data.hasKey(NBT_PROGRESS, NBT.TAG_COMPOUND)) return;
        NBTTagCompound saved = data.getCompoundTag(NBT_PROGRESS);
        if (!id.equals(saved.getString("craftingId"))) return;

        Map<IAEStack<?>, Progress> outputs = new HashMap<>();
        NBTTagList entries = saved.getTagList("outputs", NBT.TAG_COMPOUND);
        for (int index = 0; index < entries.tagCount(); index++) {
            NBTTagCompound entry = entries.getCompoundTagAt(index);
            long completed = entry.getLong("completed");
            long total = entry.getLong("total");
            if (completed < 0 || total <= 0 || completed > total) continue;
            IAEStack<?> stack = IAEStack.fromNBTGeneric(entry.getCompoundTag("stack"));
            if (stack != null) outputs.putIfAbsent(key(stack), new Progress(completed, total));
        }
        JOBS.put(cpu, new Job(id, outputs));
    }

    private static Job currentJob(CraftingCPUCluster cpu) {
        Job job = JOBS.get(cpu);
        return job != null && job.id.equals(craftingId(cpu)) ? job : null;
    }

    private static String craftingId(CraftingCPUCluster cpu) {
        ICraftingLink link = cpu.getLastCraftingLink();
        return link == null ? null : link.getCraftingID();
    }

    private static IAEStack<?> key(IAEStack<?> stack) {
        return stack.copy()
            .setStackSize(1)
            .setCountRequestable(0)
            .setCraftable(false);
    }

    private static long add(long count, long amount) {
        return count + Math.min(amount, Long.MAX_VALUE - count);
    }

    public record Progress(long completed, long total) {}

    private record Job(String id, Map<IAEStack<?>, Progress> outputs) {}
}
