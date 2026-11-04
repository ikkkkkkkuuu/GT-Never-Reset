package com.xyp.gtnrlib.mixins.late.gregtech;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnrlib.network.GregTechToolUsePacket;

import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.items.IDMetaTool01;

@Mixin(value = { BaseMetaTileEntity.class, BaseMetaPipeEntity.class }, remap = false)
public abstract class ToolMachineUseMixin {

    // GT also consumes unsuccessful machine clicks. Only successful wrench branches reach this sound.
    @Inject(
        method = "onRightclick",
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lgregtech/api/GregTechAPI;sWrenchList:Lgregtech/api/objects/GTHashSet;")),
        at = @At(
            value = "FIELD",
            target = "Lgregtech/api/enums/SoundResource;GTCEU_OP_WRENCH:Lgregtech/api/enums/SoundResource;"),
        require = 0)
    private void gtnrlib$wrenched(EntityPlayer player, ForgeDirection side, float x, float y, float z,
        CallbackInfoReturnable<Boolean> cir) {
        GregTechToolUsePacket.notifyUse(player, IDMetaTool01.WRENCH);
    }

    @Inject(
        method = "onRightclick",
        at = @At(
            value = "FIELD",
            target = "Lgregtech/api/enums/SoundResource;GTCEU_OP_WIRECUTTER:Lgregtech/api/enums/SoundResource;"),
        require = 0)
    private void gtnrlib$snipped(EntityPlayer player, ForgeDirection side, float x, float y, float z,
        CallbackInfoReturnable<Boolean> cir) {
        GregTechToolUsePacket.notifyUse(player, IDMetaTool01.WIRECUTTER);
    }
}
