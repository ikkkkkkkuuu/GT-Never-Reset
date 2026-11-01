package com.xyp.gtneverreset.common.block.blocks.tile;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtneverreset.common.crafting.batch.CraftingBatchPlanner.BatchRequirement;
import com.xyp.gtneverreset.common.crafting.batch.CraftingBatchPlanner.MediumStrategy;
import com.xyp.gtneverreset.common.crafting.batch.CraftingBatchPlannerImpl;
import com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineBuffer;
import com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineInput;
import com.xyp.gtneverreset.common.me.wildcard.WildcardPatternCache;

public final class OverloadedProviderContracts {

    public static void main(String[] args) throws Exception {
        checkBatchAccounting();
        Item item = new Item();
        checkExpansionCache(item);
        checkOverflowPersistence(item);
        checkRecoveryQuantities(item);
        checkServiceLatency();
        System.out.println("Overloaded provider cache, batch accounting and overflow contracts passed");
    }

    private static void checkRecoveryQuantities(Item item) throws Exception {
        var write = TileEntityOverloadedProvider.class.getDeclaredMethod("writeStacks", List.class);
        write.setAccessible(true);
        for (int amount : new int[] { 128, 256, 1000000, Integer.MAX_VALUE }) {
            ItemStack stack = new ItemStack(item, amount);
            NBTTagCompound identity = new NBTTagCompound();
            identity.setString("identity", "preserved");
            stack.setTagCompound(identity);
            var saved = (net.minecraft.nbt.NBTTagList) write.invoke(null, List.of(stack));
            NBTTagCompound entry = saved.getCompoundTagAt(0);
            require(
                entry.getInteger("Amount") == amount,
                "recovery stores full quantities independently of byte Count");
            require(identity.equals(entry.getCompoundTag("tag")), "recovery preserves item NBT");
        }
    }

    private static void checkServiceLatency() {
        for (int count : new int[] { 0, 1, 8, 9, 127, 1024 }) {
            boolean[] visited = new boolean[count];
            int cursor = 0;
            int budget = TileEntityOverloadedProvider.serviceBudget(count);
            require(budget <= count, "a lane is not serviced twice in the same tick");
            for (int tick = 0; tick < 5; tick++) {
                for (int work = 0; work < budget; work++) visited[cursor++ % count] = true;
            }
            for (boolean serviced : visited) require(serviced, "all lanes are serviced within five ticks");
        }
    }

    private static void checkBatchAccounting() throws Exception {
        CraftingBatchPlannerImpl planner = new CraftingBatchPlannerImpl();
        require(
            planner.resolveMediumStrategy(List.of(true, false)) == MediumStrategy.Native,
            "mixed media must retain native dispatch");
        var plan = planner.planRequirements(
            100,
            MediumStrategy.Batch,
            List.of(new BatchRequirement("item", 2, 130, 1)),
            new long[] { 3 },
            requested -> requested);
        require(plan.getCrafts() == 65, "batch limited by available inputs");
        require(matchesDeliveryBatches(2, plan.getCrafts(), 130), "encoded multiplier and CPU multiplier compose");
        require(!matchesDeliveryBatches(2, plan.getCrafts(), 129), "partial delivery rejected");
        require(!matchesDeliveryBatches(2, plan.getCrafts(), 131), "extra delivery rejected");
        require(matchesDeliveryBatches(65, 1, 65), "ordinary multiplied pattern remains valid");
        require(!matchesDeliveryBatches(Long.MAX_VALUE, 2, -2), "overflow cannot match");
        require(!matchesDeliveryBatches(1, 0, 0), "zero multiplier rejected");
        var rejected = planner.commit(plan, false, 100, 100);
        require(
            !rejected.isCommitted() && rejected.getTaskValueBeforeNativeDecrement() == 100
                && rejected.getRemainingOperationsBeforeNativeDecrement() == 100,
            "rejection preserves CPU counters");
        var accepted = planner.commit(plan, true, 100, 100);
        require(
            accepted.getTaskValueBeforeNativeDecrement() - 1 == 35
                && accepted.getRemainingOperationsBeforeNativeDecrement() - 1 == 35,
            "commit accounts for every craft");
    }

    private static boolean matchesDeliveryBatches(long pattern, long crafts, long delivered) throws Exception {
        var check = OverloadedMachineInput.class
            .getDeclaredMethod("matchesDeliveryBatches", long.class, long.class, long.class);
        check.setAccessible(true);
        return (boolean) check.invoke(null, pattern, crafts, delivered);
    }

    private static void checkExpansionCache(Item item) {
        ItemStack template = new ItemStack(item);
        template.setTagCompound(new NBTTagCompound());
        Object world = new Object();
        AtomicInteger builds = new AtomicInteger();
        WildcardPatternCache<Object> firstSlot = new WildcardPatternCache<>();
        var builder = (java.util.function.Supplier<Object>) () -> {
            builds.incrementAndGet();
            template.getTagCompound()
                .setInteger("expandedCount", 2);
            return new Object();
        };
        Object first = firstSlot.get(template, world, builder);
        require(
            firstSlot.get(template.copy(), world, builder) == first && builds.get() == 1,
            "unchanged template reuses expansion after generator writes count");
        require(
            new WildcardPatternCache<>().get(template, world, builder) != first,
            "separate slots own separate mutable details");
        template.getTagCompound()
            .setString("filter", "Iron");
        Object changed = firstSlot.get(template, world, builder);
        require(changed != first, "in-place NBT edits invalidate expansion");
        require(firstSlot.get(template, new Object(), builder) != changed, "world changes invalidate expansion");
        firstSlot.get(null, world, () -> null);
        require(firstSlot.get(template, world, builder) != changed, "removed template can be rebuilt");
    }

    private static void checkOverflowPersistence(Item item) {
        OverloadedMachineBuffer buffer = new OverloadedMachineBuffer();
        ItemStack pending = new ItemStack(item, 1000000);
        buffer.replace(List.of(pending), List.of(), ForgeDirection.UP);
        pending.stackSize = 1;
        NBTTagCompound saved = new NBTTagCompound();
        buffer.writeToNBT(saved);
        require(
            saved.getCompoundTag("OverloadedInputBuffer")
                .getTagList("Items", 10)
                .getCompoundTagAt(0)
                .getInteger("Quantity") == 1000000,
            "large accepted batch saves without byte-count truncation or caller mutation");
        require(
            saved.getCompoundTag("OverloadedInputBuffer")
                .getInteger("Side") == ForgeDirection.UP.ordinal(),
            "refill side is persisted");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
