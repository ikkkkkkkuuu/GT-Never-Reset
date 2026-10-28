package com.xyp.gtneverreset.utils.crafting.quantum.crafting;

import org.jetbrains.annotations.Nullable;

import com.xyp.gtneverreset.utils.crafting.quantum.api.stacks.AEKey;
import com.xyp.gtneverreset.utils.crafting.quantum.core.localization.PlayerMessages;

public class CraftBranchFailure extends Exception {

    private final AEKey what;
    @Nullable
    private final PlayerMessages messageKey;

    public CraftBranchFailure(AEKey what, long howMany) {
        super("Failed: " + what + " x " + howMany);
        this.what = what;
        this.messageKey = null;
    }

    public CraftBranchFailure(AEKey what, long howMany, PlayerMessages messageKey) {
        super("Failed: " + what + " x " + howMany + " (" + messageKey.getTranslationKey() + ")");
        this.what = what;
        this.messageKey = messageKey;
    }

    public AEKey getWhat() {
        return this.what;
    }

    public PlayerMessages getLocalizedMessageKey() {
        return this.messageKey != null ? this.messageKey : PlayerMessages.CraftingNoPlan;
    }

    public boolean hasExplicitMessageKey() {
        return this.messageKey != null;
    }

}
