package com.xyp.gtneverreset.mixins.early.minecraft;

import net.minecraft.entity.player.PlayerCapabilities;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** PlayerCapabilities.setFlySpeed is client-only in Minecraft 1.7.10. */
@Mixin(PlayerCapabilities.class)
public interface UniversalWandFlightSpeedAccessor {

    @Accessor("flySpeed")
    void gtneverreset$setFlySpeed(float speed);
}
