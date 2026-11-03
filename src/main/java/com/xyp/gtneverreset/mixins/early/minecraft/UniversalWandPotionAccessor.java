package com.xyp.gtneverreset.mixins.early.minecraft;

import net.minecraft.potion.Potion;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Potion's public isBadEffect getter is client-only in Minecraft 1.7.10. */
@Mixin(Potion.class)
public interface UniversalWandPotionAccessor {

    @Accessor("isBadEffect")
    boolean gtneverreset$isBadEffect();
}
