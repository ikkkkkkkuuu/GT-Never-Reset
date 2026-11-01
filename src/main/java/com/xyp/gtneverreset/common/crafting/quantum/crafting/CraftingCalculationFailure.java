package com.xyp.gtneverreset.common.crafting.quantum.crafting;

import org.jetbrains.annotations.Nullable;

import com.xyp.gtneverreset.common.crafting.quantum.core.localization.PlayerMessages;

public class CraftingCalculationFailure extends RuntimeException {

    private final PlayerMessages messageKey;
    private final Object[] messageArguments;
    @Nullable
    private final NoNetOutputCycle cycle;

    public CraftingCalculationFailure(PlayerMessages messageKey) {
        this(messageKey, null);
    }

    public CraftingCalculationFailure(PlayerMessages messageKey, @Nullable NoNetOutputCycle cycle) {
        this(messageKey, cycle, new Object[0]);
    }

    private CraftingCalculationFailure(PlayerMessages messageKey, @Nullable NoNetOutputCycle cycle,
        Object[] arguments) {
        super(messageKey.getTranslationKey());
        this.messageKey = messageKey;
        this.cycle = cycle;
        this.messageArguments = arguments.clone();
    }

    public static CraftingCalculationFailure withDetails(PlayerMessages messageKey, Object... arguments) {
        return new CraftingCalculationFailure(messageKey, null, arguments);
    }

    /**
     * The cycle that made no net output, when that is what failed.
     */
    @Nullable
    public NoNetOutputCycle getCycle() {
        return this.cycle;
    }

    public PlayerMessages getMessageKey() {
        return this.messageKey;
    }

    public Object[] getMessageArguments() {
        return this.messageArguments.clone();
    }
}
