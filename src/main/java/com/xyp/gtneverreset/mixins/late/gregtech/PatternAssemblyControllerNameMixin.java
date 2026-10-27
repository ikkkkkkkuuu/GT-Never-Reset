package com.xyp.gtneverreset.mixins.late.gregtech;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.common.tileentities.machines.IDualInputHatch;

/** Captures the controller recipe category for pattern-assembly names in AE2 interface terminals. */
@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class PatternAssemblyControllerNameMixin {

    @Shadow
    public ArrayList<IDualInputHatch> mDualInputHatches;

    @Shadow
    public abstract RecipeMap<?> getRecipeMap();

    @Inject(
        method = { "addToMachineList", "addInputBusToMachineList", "addInputHatchToMachineList" },
        at = @At("RETURN"))
    private void gtneverreset$captureRecipeMap(IGregTechTileEntity tile, int casingIndex,
        CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || tile == null) return;
        if (tile.getMetaTileEntity() instanceof SuperMTEHatchCraftingInputME hatch) {
            gtneverreset$updateRecipeMap(hatch);
        }
    }

    @Inject(method = "setMachineMode", at = @At("TAIL"))
    private void gtneverreset$refreshRecipeMap(int index, CallbackInfo ci) {
        for (IDualInputHatch hatch : mDualInputHatches) {
            if (hatch instanceof SuperMTEHatchCraftingInputME assembly) {
                gtneverreset$updateRecipeMap(assembly);
            }
        }
    }

    @Unique
    private void gtneverreset$updateRecipeMap(SuperMTEHatchCraftingInputME hatch) {
        hatch.setControllerRecipeMap(getRecipeMap());
    }
}
