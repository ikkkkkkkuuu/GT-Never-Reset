package com.xyp.gtneverreset.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.loader.ConfigLoader;
import com.xyp.gtneverreset.utils.recipes.RecipeSpeed;

import gregtech.api.recipe.check.CheckRecipeResult;
import kubatech.api.implementations.KubaTechGTMultiBlockBase;
import kubatech.tileentity.gregtech.multiblock.MTEExtremeEntityCrusher;

@Mixin(value = MTEExtremeEntityCrusher.class, remap = false)
public abstract class ExtremeEntityCrusherSpeedMixin extends KubaTechGTMultiBlockBase<MTEExtremeEntityCrusher> {

    protected ExtremeEntityCrusherSpeedMixin(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    @Inject(method = "checkProcessing", at = @At("RETURN"))
    private void gtneverreset$modifyMobProcessingDuration(CallbackInfoReturnable<CheckRecipeResult> cir) {
        if (!cir.getReturnValue()
            .wasSuccessful() || mMaxProgresstime <= 0) return;
        ConfigLoader.ensureLoaded();
        mMaxProgresstime = RecipeSpeed.getModifiedRecipeDuration(mMaxProgresstime);
    }
}
