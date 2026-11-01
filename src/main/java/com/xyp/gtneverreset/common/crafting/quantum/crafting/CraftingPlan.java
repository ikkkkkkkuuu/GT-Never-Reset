package com.xyp.gtneverreset.common.crafting.quantum.crafting;

import java.util.Collections;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.xyp.gtneverreset.common.crafting.quantum.api.crafting.IPatternDetails;
import com.xyp.gtneverreset.common.crafting.quantum.api.stacks.AEKey;
import com.xyp.gtneverreset.common.crafting.quantum.api.stacks.GenericStack;
import com.xyp.gtneverreset.common.crafting.quantum.api.stacks.KeyCounter;
import com.xyp.gtnrlib.utils.LongMath;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

public final class CraftingPlan {

    private final GenericStack finalOutput;
    private final long bytes;
    private final boolean simulation;
    private final KeyCounter usedItems;
    private final KeyCounter emittedItems;
    private final KeyCounter missingItems;
    private final Object2LongMap<IPatternDetails> patternTimes;
    private final @Nullable Map<IPatternDetails, Map<AEKey, KeyCounter>> variantBudgets;
    // Written by the calculation's successful CPU replay before publishing the plan
    private Map<IPatternDetails, KeyCounter> inputAllocations = Map.of();
    private Map<IPatternDetails, KeyCounter> remainderAllocations = Map.of();
    private @Nullable KeyCounter requiredItems;

    public CraftingPlan(GenericStack finalOutput, long bytes, boolean simulation, KeyCounter usedItems,
        KeyCounter emittedItems, KeyCounter missingItems, Object2LongMap<IPatternDetails> patternTimes,
        @Nullable Map<IPatternDetails, Map<AEKey, KeyCounter>> variantBudgets) {
        this.finalOutput = finalOutput;
        this.bytes = bytes;
        this.simulation = simulation || !missingItems.isEmpty();
        this.usedItems = usedItems;
        this.emittedItems = emittedItems;
        this.missingItems = missingItems;
        this.patternTimes = patternTimes;
        this.variantBudgets = variantBudgets;
    }

    public GenericStack finalOutput() {
        return this.finalOutput;
    }

    public long bytes() {
        return this.bytes;
    }

    public boolean simulation() {
        return this.simulation;
    }

    public KeyCounter usedItems() {
        return this.usedItems;
    }

    public KeyCounter emittedItems() {
        return this.emittedItems;
    }

    public KeyCounter missingItems() {
        return this.missingItems;
    }

    public Object2LongMap<IPatternDetails> patternTimes() {
        return this.patternTimes;
    }

    @Nullable
    public Map<IPatternDetails, Map<AEKey, KeyCounter>> variantBudgets() {
        return this.variantBudgets;
    }

    void recordRequiredItems(KeyCounter required) {
        this.requiredItems = required;
    }

    @Nullable
    KeyCounter requiredItems() {
        return this.requiredItems;
    }

    void recordRemainderAllocations(Map<IPatternDetails, KeyCounter> allocations) {
        this.remainderAllocations = allocations;
    }

    Map<IPatternDetails, KeyCounter> remainderAllocations() {
        return Collections.unmodifiableMap(this.remainderAllocations);
    }

    void recordInputAllocations(Map<IPatternDetails, KeyCounter> allocations) {
        this.inputAllocations = allocations;
    }

    Map<IPatternDetails, KeyCounter> inputAllocations() {
        return this.inputAllocations;
    }

    public CraftingPlan withMissing(KeyCounter missing) {
        var result = new CraftingPlan(
            this.finalOutput,
            this.bytes,
            this.simulation,
            this.usedItems,
            this.emittedItems,
            missing,
            this.patternTimes,
            this.variantBudgets);
        result.inputAllocations = this.inputAllocations;
        result.remainderAllocations = this.remainderAllocations;
        result.requiredItems = this.requiredItems;
        return result;
    }

    /**
     * The same plan with repaired contents.
     */
    CraftingPlan withContents(KeyCounter usedItems, KeyCounter emittedItems, KeyCounter missingItems,
        Object2LongMap<IPatternDetails> patternTimes) {
        return withContents(
            usedItems,
            emittedItems,
            missingItems,
            patternTimes,
            patternTimes.equals(this.patternTimes) ? this.variantBudgets : null);
    }

    CraftingPlan withContents(KeyCounter usedItems, KeyCounter emittedItems, KeyCounter missingItems,
        Object2LongMap<IPatternDetails> patternTimes,
        @Nullable Map<IPatternDetails, Map<AEKey, KeyCounter>> variantBudgets) {
        // Additional crafts also need storage; retain the original bound when a repair only reduces crafts.
        double additionalBytes = 0;
        for (var entry : patternTimes.object2LongEntrySet()) {
            long added = entry.getLongValue() - this.patternTimes.getLong(entry.getKey());
            if (added <= 0) continue;
            additionalBytes += added;
            for (var input : entry.getKey()
                .getInputs()) {
                if (input.possibleInputs().length == 0) continue;
                var first = input.possibleInputs()[0];
                additionalBytes += (double) added * first.amount()
                    * input.getMultiplier()
                    / first.what()
                        .getType()
                        .getAmountPerByte()
                    * 8;
            }
        }
        long repairedBytes = LongMath.saturatedAdd(this.bytes, (long) Math.ceil(additionalBytes));
        boolean samePatterns = patternTimes.equals(this.patternTimes);
        var result = new CraftingPlan(
            this.finalOutput,
            repairedBytes,
            !missingItems.isEmpty(),
            usedItems,
            emittedItems,
            missingItems,
            patternTimes,
            variantBudgets);
        if (samePatterns) {
            result.inputAllocations = this.inputAllocations;
            result.remainderAllocations = this.remainderAllocations;
        }
        return result;
    }
}
