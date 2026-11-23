package com.xyp.gtneverreset.mixins.late.gregtech;

import java.util.Collections;
import java.util.Iterator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtneverreset.common.machine.multiMachineBase.CrossRecipeMultiblockBase;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.IDualInputInventory;

@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class PatternInputFallbackMixin {

    @WrapOperation(
        method = "doCheckRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/common/tileentities/machines/IDualInputHatch;inventories()Ljava/util/Iterator;"))
    private Iterator<? extends IDualInputInventory> gtnr$uncheckedInventories(IDualInputHatch hatch,
        Operation<Iterator<? extends IDualInputInventory>> original) {
        if ((Object) this instanceof CrossRecipeMultiblockBase<?>controller
            && controller.alreadyCheckedPatternHatch(hatch)) return Collections.emptyIterator();
        return original.call(hatch);
    }
}
