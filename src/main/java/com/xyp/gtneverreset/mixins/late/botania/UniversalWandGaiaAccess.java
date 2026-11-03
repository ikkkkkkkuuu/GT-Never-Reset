package com.xyp.gtneverreset.mixins.late.botania;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "vazkii.botania.common.entity.EntityDoppleganger", remap = false)
public abstract class UniversalWandGaiaAccess implements com.xyp.gtneverreset.common.wand.UniversalWandGaiaAccess {

    @Invoker(value = "getInvulTime", remap = false)
    public abstract int gtneverreset$getGaiaInvulnerability();

    @Invoker(value = "setInvulTime", remap = false)
    public abstract void gtneverreset$setGaiaInvulnerability(int ticks);
}
