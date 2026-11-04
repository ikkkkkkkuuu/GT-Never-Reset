package com.xyp.gtnrlib.mixins.late.gregtech;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnrlib.network.GregTechToolUsePacket;

import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.items.IDMetaTool01;

// Keep the screwdriver hook active when an older host supplies the wrench/cutter hooks.
@Mixin(value = { BaseMetaTileEntity.class, BaseMetaPipeEntity.class }, remap = false)
public abstract class ScrewdriverMachineUseMixin {

    @Inject(
        method = "onRightclick",
        at = @At(
            value = "FIELD",
            target = "Lgregtech/api/enums/SoundResource;GTCEU_OP_SCREWDRIVER:Lgregtech/api/enums/SoundResource;"))
    private void gtnrlib$turnedScrewdriver(EntityPlayer player, ForgeDirection side, float x, float y, float z,
        CallbackInfoReturnable<Boolean> cir) {
        GregTechToolUsePacket.notifyUse(player, IDMetaTool01.SCREWDRIVER);
    }
}
