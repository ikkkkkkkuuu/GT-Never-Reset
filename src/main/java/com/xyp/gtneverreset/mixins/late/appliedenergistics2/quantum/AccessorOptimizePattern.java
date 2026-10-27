// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtneverreset.mixins.late.appliedenergistics2.quantum;

import java.util.HashSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEStack;

@Mixin(targets = "appeng.container.implementations.ContainerOptimizePatterns$Pattern", remap = false)
public interface AccessorOptimizePattern {

    @Accessor("patternDetails")
    HashSet<ICraftingPatternDetails> gtnr$getPatternDetails();

    @Accessor("requestedCrafts")
    long gtnr$getRequestedCrafts();

    @Invoker("addCraftingTask")
    void gtnr$addCraftingTask(ICraftingPatternDetails pattern, long totalCrafts);

    @Invoker("getCraftAmountForItem")
    long gtnr$getCraftAmountForItem(IAEStack<?> stack);

    @Invoker("getPattern")
    ICraftingPatternDetails gtnr$getPattern();

    @Invoker("getMaxBitMultiplier")
    int gtnr$getMaxBitMultiplier();
}
