package com.xyp.gtnrlib.mixins.late.gregtech;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnrlib.network.GregTechToolUsePacket;

import gregtech.api.items.MetaBaseItem;
import gregtech.common.items.IDMetaTool01;
import gregtech.common.items.behaviors.BehaviourWrench;

@Mixin(value = BehaviourWrench.class, remap = false)
public abstract class WrenchToolUseMixin {

    @Inject(method = "onItemUseFirst", at = @At("RETURN"), require = 0)
    private void gtnrlib$wrenched(MetaBaseItem item, ItemStack stack, EntityPlayer player, World world, int x, int y,
        int z, ForgeDirection side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && player.getHeldItem() == stack)
            GregTechToolUsePacket.notifyUse(player, IDMetaTool01.WRENCH);
    }
}
