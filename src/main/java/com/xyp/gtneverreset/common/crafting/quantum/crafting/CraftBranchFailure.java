package com.xyp.gtneverreset.common.crafting.quantum.crafting;

import org.jetbrains.annotations.Nullable;

import com.xyp.gtneverreset.common.crafting.quantum.api.stacks.AEKey;
import com.xyp.gtneverreset.common.crafting.quantum.core.localization.PlayerMessages;

public class CraftBranchFailure extends Exception {

    private final AEKey what;
    private final long howMany;
    @Nullable
    private final PlayerMessages messageKey;
    @Nullable
    private final NoNetOutputCycle cycle;

    public CraftBranchFailure(AEKey what, long howMany) {
        super("Failed: " + what + " x " + howMany);
        this.what = what;
        this.howMany = howMany;
        this.messageKey = null;
        this.cycle = null;
    }

    public CraftBranchFailure(AEKey what, long howMany, PlayerMessages messageKey) {
        this(what, howMany, messageKey, null);
    }

    public CraftBranchFailure(AEKey what, long howMany, PlayerMessages messageKey, @Nullable NoNetOutputCycle cycle) {
        super("Failed: " + what + " x " + howMany + " (" + messageKey.getTranslationKey() + ")");
        this.what = what;
        this.howMany = howMany;
        this.messageKey = messageKey;
        this.cycle = cycle;
    }

    @Nullable
    public NoNetOutputCycle getCycle() {
        return this.cycle;
    }

    public AEKey getWhat() {
        return this.what;
    }

    public PlayerMessages getLocalizedMessageKey() {
        return this.messageKey != null ? this.messageKey : PlayerMessages.CraftingMissingIngredient;
    }

    public long getMissingAmount() {
        return this.howMany;
    }

    public boolean hasExplicitMessageKey() {
        return this.messageKey != null;
    }

}
