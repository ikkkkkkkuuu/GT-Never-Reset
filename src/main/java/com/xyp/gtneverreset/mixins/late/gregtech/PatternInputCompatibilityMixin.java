package com.xyp.gtneverreset.mixins.late.gregtech;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternMirror;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternMirror;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputSlave;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.common.tileentities.machines.IDualInputHatch;

/** Keeps each controller's pattern assemblies and mirrors in one compatible family. */
@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class PatternInputCompatibilityMixin {

    @Shadow
    public ArrayList<IDualInputHatch> mDualInputHatches;

    @WrapOperation(
        method = { "checkStructure", "checkMachine_TT" },
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/metatileentity/implementations/MTEMultiBlockBase;checkMachine(Lgregtech/api/interfaces/tileentity/IGregTechTileEntity;Lnet/minecraft/item/ItemStack;Ljava/util/List;)V"))
    private void gtneverreset$checkPatternFamilies(MTEMultiBlockBase controller, IGregTechTileEntity tile,
        ItemStack stack, List<StructureError> errors, Operation<Void> original) {
        original.call(controller, tile, stack, errors);
        if (gtneverreset$hasMixedPatternInputs()) {
            // #tr GT5U.gui.text.recipe_result.gtnr_pattern_input_mixed
            // # Ordinary, circuit and cross-recipe pattern inputs cannot be mixed; mirrors count as their own family
            // # zh_CN 普通、电路、跨配方类型总成不可混装；镜像仓按自身类型计算
            errors.add(StructureErrors.of("GT5U.gui.text.recipe_result.gtnr_pattern_input_mixed"));
        }
    }

    @Inject(method = "doCheckRecipe", at = @At("HEAD"), cancellable = true)
    private void gtneverreset$rejectMixedPatternProcessing(CallbackInfoReturnable<CheckRecipeResult> cir) {
        if (gtneverreset$hasMixedPatternInputs()) {
            cir.setReturnValue(SimpleCheckRecipeResult.ofFailure("gtnr_pattern_input_mixed"));
        }
    }

    @Unique
    private boolean gtneverreset$hasMixedPatternInputs() {
        Class<?> family = null;
        for (IDualInputHatch input : mDualInputHatches) {
            Class<?> next = gtneverreset$patternFamily(input);
            if (next == null) continue;
            if (family != null && family != next) return true;
            family = next;
        }
        return false;
    }

    @Unique
    private static Class<?> gtneverreset$patternFamily(IDualInputHatch input) {
        if (input instanceof CrossRecipeMEPatternBuffer || input instanceof CrossRecipeMEPatternMirror)
            return CrossRecipeMEPatternBuffer.class;
        if (input instanceof CircuitMEPatternBuffer || input instanceof CircuitMEPatternMirror)
            return CircuitMEPatternBuffer.class;
        if (input instanceof SuperMTEHatchCraftingInputME || input instanceof SuperMTEHatchCraftingInputSlave)
            return SuperMTEHatchCraftingInputME.class;
        return null;
    }
}
