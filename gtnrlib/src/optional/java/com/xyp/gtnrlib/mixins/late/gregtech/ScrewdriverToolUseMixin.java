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
import gregtech.common.items.behaviors.BehaviourScrewdriver;

@Mixin(value = BehaviourScrewdriver.class, remap = false)
public abstract class ScrewdriverToolUseMixin {

    // The behaviour also returns true when it cannot pay the tool cost. The sound follows a paid use.
    @Inject(
        method = "onItemUseFirst",
        at = @At(
            value = "FIELD",
            target = "Lgregtech/api/enums/SoundResource;GTCEU_OP_SCREWDRIVER:Lgregtech/api/enums/SoundResource;"))
    private void gtnrlib$turnedScrewdriver(MetaBaseItem item, ItemStack stack, EntityPlayer player, World world, int x,
        int y, int z, ForgeDirection side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
        if (player.getHeldItem() == stack) GregTechToolUsePacket.notifyUse(player, IDMetaTool01.SCREWDRIVER);
    }
}
