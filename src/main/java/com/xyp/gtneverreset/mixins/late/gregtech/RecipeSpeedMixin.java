package com.xyp.gtneverreset.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;
import com.xyp.gtneverreset.utils.recipes.RecipeSpeed;

import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.util.GTRecipe;

@Mixin(value = RecipeMapBackend.class, remap = false)
public class RecipeSpeedMixin {

    @Inject(method = "compileRecipe", at = @At("HEAD"))
    private void gtneverreset$modifyRecipeDuration(GTRecipe recipe, CallbackInfoReturnable<GTRecipe> cir) {
        ConfigLoader.ensureLoaded();
        if (MainConfig.recipeSpeed.mode == 0 || recipe == null) return;
        recipe.mDuration = RecipeSpeed.getModifiedRecipeDuration(recipe.mDuration);
    }
}
