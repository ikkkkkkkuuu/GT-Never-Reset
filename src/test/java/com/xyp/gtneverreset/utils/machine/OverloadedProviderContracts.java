package com.xyp.gtneverreset.utils.machine;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtneverreset.utils.crafting.CraftingBatchPlanner.BatchRequirement;
import com.xyp.gtneverreset.utils.crafting.CraftingBatchPlanner.MediumStrategy;
import com.xyp.gtneverreset.utils.crafting.CraftingBatchPlannerImpl;
import com.xyp.gtneverreset.utils.recipes.wildcard.WildcardPatternCache;

public final class OverloadedProviderContracts {

    public static void main(String[] args) {
        checkBatchAccounting();
        Item item = new Item();
        checkExpansionCache(item);
        checkOverflowPersistence(item);
        System.out.println("Overloaded provider cache, batch accounting and overflow contracts passed");
    }

    private static void checkBatchAccounting() {
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
        require(
            OverloadedMachineInput.matchesDeliveryBatches(2, plan.getCrafts(), 130),
            "encoded multiplier and CPU multiplier compose");
        require(!OverloadedMachineInput.matchesDeliveryBatches(2, plan.getCrafts(), 129), "partial delivery rejected");
        require(!OverloadedMachineInput.matchesDeliveryBatches(2, plan.getCrafts(), 131), "extra delivery rejected");
        require(OverloadedMachineInput.matchesDeliveryBatches(65, 1, 65), "ordinary multiplied pattern remains valid");
        require(!OverloadedMachineInput.matchesDeliveryBatches(Long.MAX_VALUE, 2, -2), "overflow cannot match");
        require(!OverloadedMachineInput.matchesDeliveryBatches(1, 0, 0), "zero multiplier rejected");
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
        buffer.replace(List.of(pending), null, ForgeDirection.UP);
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
