package com.xyp.gtneverreset.common.crafting.quantum.crafting;

import com.xyp.gtneverreset.common.crafting.quantum.api.stacks.AmountOverflowException;
import com.xyp.gtneverreset.common.crafting.quantum.core.localization.PlayerMessages;

/**
 * Terminal planning failure; retrying or clamping would hide an unrepresentable material requirement.
 */
public final class CraftingAmountOverflow extends CraftingCalculationFailure {

    private final AmountOverflowException overflow;

    public CraftingAmountOverflow(AmountOverflowException overflow) {
        super(PlayerMessages.CraftingAmountOverflow);
        this.overflow = overflow;
        initCause(overflow);
    }

    public AmountOverflowException getOverflow() {
        return this.overflow;
    }
}
