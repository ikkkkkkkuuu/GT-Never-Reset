package com.xyp.gtneverreset.utils.crafting.quantum.crafting.inv;

import com.xyp.gtneverreset.utils.crafting.quantum.api.stacks.AEKey;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;

/**
 * Simplified inventory with unbounded capacity and support for multiple IAEStacks. Used for crafting, both for the
 * simulation and for the CPU's own inventory.
 */
public interface ICraftingInventory {

    /**
     * Inject items. Can never fail.
     */
    void insert(AEKey what, long amount, Actionable mode);

    long extract(AEKey what, long amount, Actionable mode);

    /**
     * Return a list of templates that match the input with {@link FuzzyMode#IGNORE_ALL} to obtain compatible crafting
     * ingredients in case patterns allow substitutions.
     * <p/>
     * Use {@link #extract} to query the exact amount that is available.
     */
    Iterable<AEKey> findFuzzyTemplates(AEKey input);
}
