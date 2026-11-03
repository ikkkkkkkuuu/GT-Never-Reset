package com.xyp.gtneverreset.mixins.early.client;

import net.minecraft.client.multiplayer.PlayerControllerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerControllerMP.class)
public interface PlayerControllerMPAccessor {

    @Accessor("blockHitDelay")
    void gtneverreset$setBlockHitDelay(int delay);
}
