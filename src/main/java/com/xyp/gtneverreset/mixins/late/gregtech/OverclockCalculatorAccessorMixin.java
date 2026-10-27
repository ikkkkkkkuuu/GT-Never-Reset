package com.xyp.gtneverreset.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import gregtech.api.util.OverclockCalculator;

/** Read-only access to the configured calculator before it executes an overclock. */
@Mixin(value = OverclockCalculator.class, remap = false)
public interface OverclockCalculatorAccessorMixin {

    @Accessor("heatOC")
    boolean gtneverreset$heatOverclock();

    @Accessor("heatDiscount")
    boolean gtneverreset$heatDiscount();

    @Accessor("heatDiscountExponent")
    double gtneverreset$heatDiscountFactor();

    @Accessor("recipeHeat")
    int gtneverreset$recipeHeat();

    @Accessor("machineHeat")
    int gtneverreset$machineHeat();

    @Accessor("maxTierSkip")
    int gtneverreset$maxTierSkip();
}
