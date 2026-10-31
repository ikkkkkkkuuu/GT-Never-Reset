package com.xyp.gtneverreset.common.crafting.quantum.crafting.inv;

import com.xyp.gtneverreset.common.crafting.quantum.api.crafting.IPatternDetails;
import com.xyp.gtneverreset.common.crafting.quantum.api.stacks.AEKey;

/**
 * Extended version of {@link ICraftingInventory} to keep track of other simulation state that is not directly related
 * to inventory contents.
 */
public interface ICraftingSimulationState extends ICraftingInventory {

    void emitItems(AEKey what, long amount);

    void addBytes(double bytes);

    default void addStackBytes(AEKey key, long amount, long multiplier) {
        // Crafting storage is 8 times bigger than normal storage, this is intentional.
        addBytes(
            (double) amount * multiplier
                / key.getType()
                    .getAmountPerByte()
                * 8);
    }

    void addCrafting(IPatternDetails details, long crafts);
}
