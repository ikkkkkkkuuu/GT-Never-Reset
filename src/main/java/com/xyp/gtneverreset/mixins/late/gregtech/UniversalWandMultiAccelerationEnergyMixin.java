package com.xyp.gtneverreset.mixins.late.gregtech;

import net.minecraft.tileentity.TileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.common.wand.UniversalWandAccelerationHandler;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

@Mixin(value = { MTEMultiBlockBase.class, MTEExtendedPowerMultiBlockBase.class }, remap = false)
public abstract class UniversalWandMultiAccelerationEnergyMixin {

    @Inject(method = "drainEnergyInput", at = @At("HEAD"), cancellable = true)
    private void gtneverreset$skipCompletionEnergy(long energy, CallbackInfoReturnable<Boolean> callback) {
        if (UniversalWandAccelerationHandler.isCompletingRecipe(
            (TileEntity) ((IMetaTileEntity) (Object) this).getBaseMetaTileEntity())) callback.setReturnValue(true);
    }
}
