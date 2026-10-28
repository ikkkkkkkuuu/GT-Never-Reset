package com.xyp.gtneverreset.utils.crafting.quantum.crafting.inv;

import com.xyp.gtneverreset.utils.crafting.quantum.api.stacks.AEKey;

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
}
