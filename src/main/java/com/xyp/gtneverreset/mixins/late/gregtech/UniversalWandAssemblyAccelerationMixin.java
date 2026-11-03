package com.xyp.gtneverreset.mixins.late.gregtech;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.common.wand.UniversalWandAccelerationHandler;
import com.xyp.gtneverreset.compat.gregtech.WandAssemblyAcceleration;

import ggfab.mte.MTEAdvAssLine;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;

@Mixin(value = MTEAdvAssLine.class, remap = false)
public abstract class UniversalWandAssemblyAccelerationMixin extends MTEExtendedPowerMultiBlockBase<MTEAdvAssLine>
    implements WandAssemblyAcceleration {

    @Shadow
    private boolean stuck;

    protected UniversalWandAssemblyAccelerationMixin(int id, String name, String regional) {
        super(id, name, regional);
    }

    @Override
    public boolean gtneverreset$isAssemblyBlocked() {
        return stuck;
    }

    @Inject(
        method = "onRunningTick",
        at = @At(value = "FIELD", target = "Lggfab/mte/MTEAdvAssLine;baseEUt:J", ordinal = 0),
        cancellable = true)
    private void gtneverreset$skipAdditionalEnergy(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (UniversalWandAccelerationHandler.isUpdating((TileEntity) getBaseMetaTileEntity()))
            callback.setReturnValue(true);
    }
}
