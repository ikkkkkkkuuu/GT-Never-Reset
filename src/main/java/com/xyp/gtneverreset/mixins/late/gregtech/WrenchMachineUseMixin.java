package com.xyp.gtneverreset.mixins.late.gregtech;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.common.packet.WrenchTurnPacket;

import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;

@Mixin(value = { BaseMetaTileEntity.class, BaseMetaPipeEntity.class }, remap = false)
public abstract class WrenchMachineUseMixin {

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
    private void gtneverreset$wrenched(EntityPlayer player, ForgeDirection side, float x, float y, float z,
        CallbackInfoReturnable<Boolean> cir) {
        WrenchTurnPacket.notifyUse(player);
    }
}
