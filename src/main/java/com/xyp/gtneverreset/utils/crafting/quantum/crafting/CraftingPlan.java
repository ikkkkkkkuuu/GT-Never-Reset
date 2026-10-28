package com.xyp.gtneverreset.utils.crafting.quantum.crafting;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.xyp.gtneverreset.utils.crafting.quantum.api.crafting.IPatternDetails;
import com.xyp.gtneverreset.utils.crafting.quantum.api.stacks.AEKey;
import com.xyp.gtneverreset.utils.crafting.quantum.api.stacks.GenericStack;
import com.xyp.gtneverreset.utils.crafting.quantum.api.stacks.KeyCounter;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

public record CraftingPlan(GenericStack finalOutput, long bytes, boolean simulation, KeyCounter usedItems,
    KeyCounter emittedItems, KeyCounter missingItems, Object2LongMap<IPatternDetails> patternTimes,
    @Nullable Map<IPatternDetails, Map<AEKey, KeyCounter>> variantBudgets) {

    public CraftingPlan withMissing(KeyCounter missing) {
        return new CraftingPlan(
            this.finalOutput,
            this.bytes,
            this.simulation,
            this.usedItems,
            this.emittedItems,
            missing,
            this.patternTimes,
            this.variantBudgets);
    }
}
