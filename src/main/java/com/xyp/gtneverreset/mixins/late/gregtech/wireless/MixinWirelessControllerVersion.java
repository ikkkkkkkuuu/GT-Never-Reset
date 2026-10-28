package com.xyp.gtneverreset.mixins.late.gregtech.wireless;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessControllerContext;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessControllerVersion;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessHatchOwner;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.structure.error.StructureError;

@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class MixinWirelessControllerVersion implements WirelessControllerVersion {

    @Unique
    private long gtnr$wirelessStructureVersion;

    @Unique
    private long gtnr$recordedStructureVersion = Long.MIN_VALUE;

    @Override
    public long wirelessFrequency$version() {
        return gtnr$wirelessStructureVersion;
    }

    @Inject(method = "clearHatches", at = @At("HEAD"))
    private void gtnr$invalidateHatchOwners(CallbackInfo ci) {
        gtnr$wirelessStructureVersion++;
    }

    @Inject(method = "checkStructure", at = @At("RETURN"))
    private void gtnr$recordCheckedHatches(boolean forceReset, IGregTechTileEntity base,
        CallbackInfoReturnable<Boolean> cir) {
        if (!base.isServerSide() || gtnr$recordedStructureVersion == gtnr$wirelessStructureVersion) return;
        MTEMultiBlockBase controller = (MTEMultiBlockBase) (Object) this;
        // Registered hatches still belong to the controller while the rest of its structure is incomplete.
        gtnr$recordHatches(controller.mInputHatches, controller);
        gtnr$recordHatches(controller.mOutputHatches, controller);
        gtnr$recordHatches(controller.mInputBusses, controller);
        gtnr$recordHatches(controller.mOutputBusses, controller);
        gtnr$recordHatches(controller.mDualInputHatches, controller);
        gtnr$recordedStructureVersion = gtnr$wirelessStructureVersion;
    }

    @Unique
    private static void gtnr$recordHatches(Iterable<?> hatches, MTEMultiBlockBase controller) {
        for (Object hatch : hatches) {
            if (hatch instanceof WirelessHatchOwner owner) owner.wirelessFrequency$setController(controller);
        }
    }

    @WrapOperation(
        method = { "checkStructure", "checkMachine_TT" },
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/metatileentity/implementations/MTEMultiBlockBase;checkMachine("
                + "Lgregtech/api/interfaces/tileentity/IGregTechTileEntity;Lnet/minecraft/item/ItemStack;"
                + "Ljava/util/List;)V"))
    private void gtnr$trackControllerDuringStructureCheck(MTEMultiBlockBase controller, IGregTechTileEntity base,
        ItemStack stack, List<StructureError> errors, Operation<Void> original) {
        MTEMultiBlockBase previous = WirelessControllerContext.enter(controller);
        try {
            original.call(controller, base, stack, errors);
        } finally {
            WirelessControllerContext.restore(previous);
        }
    }
}
