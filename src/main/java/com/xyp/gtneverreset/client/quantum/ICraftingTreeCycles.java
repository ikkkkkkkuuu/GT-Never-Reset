// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtneverreset.client.quantum;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.xyp.gtneverreset.utils.crafting.cycle.RecursivePatternFinder.Cycle;

import appeng.api.storage.data.IAEStack;

/**
 * A crafting tree that marks the crafting cycles found for its request and can show one of its cycles as a ring, see
 * {@link CraftingRingView}.
 */
public interface ICraftingTreeCycles {

    void gtnr$setCycles(CraftingCycleIndex index);

    @Nullable
    CraftingRingView gtnr$ringView();

    /**
     * Shows a cycle found in the patterns as a ring, going through the item if it can.
     *
     * @return whether it could be made of the patterns
     */
    boolean gtnr$openCycle(Cycle cycle, @Nullable IAEStack<?> start);

    void gtnr$closeRingView();

    /**
     * The cycle patterns of what is under the mouse: a craft, a folded cycle, or in a ring the whole cycle.
     */
    List<CraftingCycleIndex.Match> gtnr$hoveredPatterns();
}
