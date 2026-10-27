package com.xyp.gtneverreset.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xyp.gtneverreset.loader.ConfigLoader;
import com.xyp.gtneverreset.utils.recipes.RecipeSpeed;

import ggfab.mte.MTEAdvAssLine;
import gregtech.common.tileentities.machines.multi.MTEAssemblyLine;

@Mixin(value = { MTEAssemblyLine.class, MTEAdvAssLine.class }, remap = false)
public abstract class AssemblyLineSpeedMixin {

    // Also matches the advanced line's compiler-generated lambda without mutating shared recipes.
    @ModifyExpressionValue(
        method = "*",
        at = @At(value = "FIELD", target = "Lgregtech/api/util/GTRecipe$RecipeAssemblyLine;mDuration:I"),
        require = 1,
        allow = 1)
    private static int gtneverreset$modifyAssemblyLineDuration(int duration) {
        ConfigLoader.ensureLoaded();
        return RecipeSpeed.getModifiedRecipeDuration(duration);
    }
}
