package com.xyp.gtneverreset.mixins.early.minecraft;

import net.minecraft.util.FoodStats;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FoodStats.class)
public interface FoodStatsMiningAccessor {

    @Accessor("foodExhaustionLevel")
    float gtneverreset$getMiningExhaustion();

    @Accessor("foodExhaustionLevel")
    void gtneverreset$setMiningExhaustion(float exhaustion);
}
