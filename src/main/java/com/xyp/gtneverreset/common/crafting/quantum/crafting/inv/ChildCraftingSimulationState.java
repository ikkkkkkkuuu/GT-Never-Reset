package com.xyp.gtneverreset.common.crafting.quantum.crafting.inv;

import com.xyp.gtneverreset.common.crafting.quantum.api.stacks.AEKey;

import appeng.api.config.Actionable;

public class ChildCraftingSimulationState extends CraftingSimulationState {

    private final ICraftingInventory parent;

    public ChildCraftingSimulationState(ICraftingInventory parent) {
        this.parent = parent;
    }

    @Override
    protected long simulateExtractParent(AEKey what) {
        return parent.extract(what, Long.MAX_VALUE, Actionable.SIMULATE);
    }

    @Override
    protected Iterable<AEKey> findFuzzyParent(AEKey input) {
        return parent.findFuzzyTemplates(input);
    }

    @Override
    protected void cacheFuzzyParent(AEKey what) {
        // Subclasses may override extraction or fuzzy lookup. Only bypass virtual inventory calls for the built-ins.
        if (getClass() == ChildCraftingSimulationState.class && (parent.getClass() == ChildCraftingSimulationState.class
            || parent.getClass() == NetworkCraftingSimulationState.class)) {
            ((CraftingSimulationState) parent).copyFuzzyTo(what, this);
        } else {
            super.cacheFuzzyParent(what);
        }
    }
}
