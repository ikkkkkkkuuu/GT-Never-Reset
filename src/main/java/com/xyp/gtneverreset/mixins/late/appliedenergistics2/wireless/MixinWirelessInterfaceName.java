package com.xyp.gtneverreset.mixins.late.appliedenergistics2.wireless;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessInterfaceNameOverride;

import appeng.helpers.DualityInterface;
import appeng.helpers.ICustomNameObject;

@Mixin(value = DualityInterface.class, remap = false)
public abstract class MixinWirelessInterfaceName implements WirelessInterfaceNameOverride {

    @Unique
    private boolean gtnr$readingTargetName;

    @Override
    public <T> T wirelessFrequency$withoutCustomName(Supplier<T> reader) {
        boolean previous = gtnr$readingTargetName;
        gtnr$readingTargetName = true;
        try {
            return reader.get();
        } finally {
            gtnr$readingTargetName = previous;
        }
    }

    @WrapOperation(
        method = { "getCrafterIcon", "getRawTermName", "getAdjacentNameSuffix" },
        at = @At(value = "INVOKE", target = "Lappeng/helpers/ICustomNameObject;hasCustomName()Z"))
    private boolean gtnr$readTargetMetadata(ICustomNameObject host, Operation<Boolean> original) {
        return !gtnr$readingTargetName && original.call(host);
    }
}
