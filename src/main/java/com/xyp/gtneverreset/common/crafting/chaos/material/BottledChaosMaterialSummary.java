package com.xyp.gtneverreset.common.crafting.chaos.material;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeProcessor;
import com.xyp.gtneverreset.common.item.items.ChaosRouteItem;

import gregtech.api.util.GTUtility;

/** Output roles affect presentation only; cache and byproduct previews remain bounded. */
public final class BottledChaosMaterialSummary {

    public static final int WAILA_OUTPUT_LIMIT = 20;
    public static final int BYPRODUCT_OUTPUT_LIMIT = 10;
    public static final int GROUP_OUTPUT_LIMIT = 3;

    public static NBTTagCompound forCache(BottledChaosMaterialCache cache, long tagMask) {
        Map<Object, Long> amounts = cache.snapshot();
        return summarize(amounts, outputPriorities(amounts, tagMask, false), WAILA_OUTPUT_LIMIT, -1);
    }

    /** Actual exports and pending carry are separate presentation sources; never merge carry into final products. */
    public static NBTTagCompound forJob(BottledChaosOutputBuffer outputs, BottledChaosOutputBuffer pendingCache,
        ItemStack authorization) {
        long tagMask = ChaosRouteItem.getRouteMask(authorization);
        var amounts = outputAmounts(outputs);
        var priorities = jobOutputPriorities(amounts, authorization);
        NBTTagCompound summary = summarize(amounts, priorities, Integer.MAX_VALUE, 0);
        Map<Object, BigInteger> intermediates = new LinkedHashMap<>();
        outputAmounts(pendingCache).forEach((key, amount) -> intermediates.put(key, BigInteger.valueOf(amount)));
        amounts.forEach(
            (key, amount) -> {
                if (priorities.getOrDefault(key, 2) == 1)
                    intermediates.merge(key, BigInteger.valueOf(amount), BigInteger::add);
            });
        summary.setTag(
            "intermediates",
            summarize(intermediates, outputPriorities(intermediates, tagMask, true), Integer.MAX_VALUE, -1));
        summary.setTag("byproducts", summarize(amounts, priorities, BYPRODUCT_OUTPUT_LIMIT, 2));
        return summary;
    }

    /** Only terminal targets of the authorized chain, never intermediate materials or unmarked coproducts. */
    public static NBTTagCompound forFinalJob(BottledChaosOutputBuffer outputs, ItemStack authorization) {
        var amounts = outputAmounts(outputs);
        return summarize(amounts, jobOutputPriorities(amounts, authorization), Integer.MAX_VALUE, 0);
    }

    private static Map<Object, Integer> jobOutputPriorities(Map<Object, Long> amounts, ItemStack authorization) {
        if (!ChaosRouteItem.hasPatternAuthorization(authorization))
            return outputPriorities(amounts, ChaosRouteItem.getRouteMask(authorization), false);
        // The private AE graph folds its steps into one route with explicit net outputs.
        Map<Object, Integer> roles = new LinkedHashMap<>();
        for (Object key : amounts.keySet()) roles.put(key, 0);
        return roles;
    }

    private static Map<Object, Long> outputAmounts(BottledChaosOutputBuffer outputs) {
        Map<Object, Long> amounts = new LinkedHashMap<>();
        outputs.collectAmounts(amounts);
        return amounts;
    }

    private static Map<Object, Integer> outputPriorities(Map<Object, ?> amounts, long tagMask,
        boolean intermediateOnly) {
        return amounts.isEmpty() ? Collections.emptyMap()
            : BottledChaosRecipeProcessor.outputPriorities(tagMask, intermediateOnly);
    }

    private static NBTTagCompound summarize(Map<Object, ? extends Number> amounts, Map<Object, Integer> priorities,
        int limit, int category) {
        Map<Object, Number> ordered = new LinkedHashMap<>();
        for (Object key : priorities.keySet()) {
            Number amount = amounts.get(key);
            if (amount != null) ordered.put(key, amount);
        }
        amounts.forEach(ordered::putIfAbsent);
        List<Map.Entry<Object, Number>> entries = new ArrayList<>(ordered.entrySet());
        NBTTagCompound tag = new NBTTagCompound();
        int total = 0;
        NBTTagList outputs = new NBTTagList();
        for (int priority = 0; priority < 3; priority++) {
            if (category >= 0 && category != priority) continue;
            // Stable partition: keep tag/recipe order within items and fluids; final products retain their order.
            if (priority > 0)
                entries.sort(Comparator.comparingInt(entry -> entry.getKey() instanceof ChaosItemKey ? 0 : 1));
            for (var output : entries) {
                Object key = output.getKey();
                if (priorities.getOrDefault(key, 2) != priority) continue;
                total++;
                if (outputs.tagCount() >= limit) continue;
                NBTTagCompound entry = new NBTTagCompound();
                if (key instanceof ChaosItemKey item) {
                    entry.setTag(
                        "item",
                        item.getItemStack(1)
                            .writeToNBT(new NBTTagCompound()));
                } else {
                    entry.setTag(
                        "fluid",
                        ((GTUtility.FluidId) key).getFluidStack(1)
                            .writeToNBT(new NBTTagCompound()));
                }
                entry.setString(
                    "amount",
                    output.getValue()
                        .toString());
                outputs.appendTag(entry);
            }
        }
        tag.setInteger("total", total);
        tag.setTag("entries", outputs);
        return tag;
    }
}
