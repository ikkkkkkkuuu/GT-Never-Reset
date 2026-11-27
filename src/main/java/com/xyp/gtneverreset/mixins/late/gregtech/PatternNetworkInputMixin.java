package com.xyp.gtneverreset.mixins.late.gregtech;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputSlave;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.tileentities.machines.IDualInputHatch;

/** GT calls processing-aware hooks on input buses and hatches, but keeps pattern hatches in its dual-input list. */
@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class PatternNetworkInputMixin {

    @Shadow
    public ArrayList<IDualInputHatch> mDualInputHatches;
    @Unique
    private List<CircuitMEPatternBuffer> gtnr$supplyProviders;

    @Unique
    private int gtnr$nestedChecks;

    @Inject(method = "startRecipeProcessing", at = @At("RETURN"))
    private void gtnr$beginNetworkInputs(CallbackInfo ci) {
        if (gtnr$supplyProviders != null && !gtnr$supplyProviders.isEmpty()) {
            gtnr$nestedChecks++;
            return;
        }
        for (var input : mDualInputHatches) {
            var master = input instanceof SuperMTEHatchCraftingInputSlave mirror ? mirror.getMaster() : input;
            if (master instanceof CircuitMEPatternBuffer hatch && hatch.isValid()
                && hatch.getSharedSupplies()
                    .beginNetworkCheck()) {
                if (gtnr$supplyProviders == null) gtnr$supplyProviders = new ArrayList<>();
                gtnr$supplyProviders.add(hatch);
            }
        }
    }

    @Inject(method = "endRecipeProcessing", at = @At("RETURN"))
    private void gtnr$settleNetworkInputs(CallbackInfo ci) {
        if (gtnr$supplyProviders == null || gtnr$supplyProviders.isEmpty()) return;
        if (gtnr$nestedChecks > 0) {
            gtnr$nestedChecks--;
            return;
        }
        boolean success = true;
        for (var hatch : gtnr$supplyProviders) {
            success &= hatch.getSharedSupplies()
                .endNetworkCheck(hatch.getProxy(), hatch.getMEOutputActionSource());
        }
        gtnr$supplyProviders.clear();
        if (!success) {
            var controller = (MTEMultiBlockBase) (Object) this;
            controller.stopMachine(ShutDownReasonRegistry.CRITICAL_NONE);
            controller.setResultIfFailure(
                // #tr GT5U.gui.text.recipe_result.gtnr_pattern_supply_extraction_failed
                // # The pattern hatch could not withdraw the recipe inputs from ME storage.
                // # zh_CN 样板总成未能从 ME 网络扣除配方原料。
                SimpleCheckRecipeResult.ofFailurePersistOnShutdown("gtnr_pattern_supply_extraction_failed"));
        }
    }
}
