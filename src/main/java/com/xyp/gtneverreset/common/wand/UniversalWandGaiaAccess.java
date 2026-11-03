package com.xyp.gtneverreset.common.wand;

/** Optional Gaia bridge that remains loadable when Botania is absent. */
public interface UniversalWandGaiaAccess {

    int gtneverreset$getGaiaInvulnerability();

    void gtneverreset$setGaiaInvulnerability(int ticks);
}
