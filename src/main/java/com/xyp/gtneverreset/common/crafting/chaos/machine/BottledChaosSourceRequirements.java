package com.xyp.gtneverreset.common.crafting.chaos.machine;

import com.xyp.gtneverreset.common.crafting.chaos.material.ChaosItemKey;

/** Only immutable source requirements survive startup; radiation is satisfied by the packaged radio hatch. */
public record BottledChaosSourceRequirements(int heat, int glassTier, int casingTier, int compressionTier,
    boolean heavyCentrifuge, ChaosItemKey specialInput, int plasmaTier, boolean multiStepPlasma,
    long requiredSourceVoltage, long fusionThreshold) {

    public static final BottledChaosSourceRequirements NONE = new BottledChaosSourceRequirements(
        0,
        0,
        0,
        0,
        false,
        null,
        0,
        false,
        0,
        0);
}
