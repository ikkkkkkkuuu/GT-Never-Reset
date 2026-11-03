package com.xyp.gtneverreset.mixins.early.minecraft;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityLivingBase.class)
public interface UniversalWandLivingAccess {

    @Accessor("dead")
    boolean gtneverreset$isDeathCommitted();

    @Accessor("dead")
    void gtneverreset$setDeathCommitted(boolean dead);

    @Accessor("recentlyHit")
    void gtneverreset$setRecentlyHit(int ticks);

    @Accessor("attackingPlayer")
    void gtneverreset$setAttackingPlayer(EntityPlayer player);

    @Invoker("dropFewItems")
    void gtneverreset$dropFewItems(boolean recentlyHit, int looting);

    @Invoker("dropEquipment")
    void gtneverreset$dropEquipment(boolean recentlyHit, int looting);

    @Invoker("dropRareDrop")
    void gtneverreset$dropRareDrop(int bonus);

    @Invoker("func_146066_aG")
    boolean gtneverreset$canDropLoot();
}
