package com.xyp.gtneverreset.client.quantum;

import java.util.List;

import com.xyp.gtneverreset.utils.crafting.cycle.RecursivePatternFinder.Cycle;

import appeng.api.storage.data.IAEStack;

/**
 * A screen confirming a request, told the crafting cycles found for it.
 */
public interface ICraftingCycleHost {

    /**
     * @param manual asked for by the player, to be shown in full
     */
    void gtnr$cyclesFound(IAEStack<?> request, List<Cycle> cycles, boolean manual);
}
