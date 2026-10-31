package com.xyp.gtneverreset.api.mixinHelper;

/** Preserves TC4's post-craft aspect decay delay when products are returned directly to AE. */
public interface CrucibleCooldownAccess {

    /** Restarts the same delay that TC4 applies after a successful catalyst reaction. */
    void gtnr$resetCraftingCooldown();
}
