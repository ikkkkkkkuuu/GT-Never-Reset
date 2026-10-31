package com.xyp.gtneverreset.mixins.late.gregtech;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.config.RecipeSpeedConfig;
import com.xyp.gtneverreset.utils.recipes.RecipeSpeed;

import gregtech.api.recipe.maps.FurnaceBackend;
import gregtech.api.util.GTRecipe;

@Mixin(value = FurnaceBackend.class, remap = false)
public abstract class FurnaceBackendMixin {

    @Inject(method = "overwriteFindRecipe", at = @At("RETURN"), remap = false, require = 0)
    private void gtneverreset$modifyFurnaceDuration(ItemStack[] items, FluidStack[] fluids,
        @Nullable ItemStack specialSlot, @Nullable GTRecipe cachedRecipe, CallbackInfoReturnable<GTRecipe> cir) {
        if (RecipeSpeedConfig.mode == 0) {
            return;
        }
        GTRecipe recipe = cir.getReturnValue();
        if (recipe != null) {
            recipe.mDuration = RecipeSpeed.getModifiedRecipeDuration(recipe.mDuration);
        }
    }
}
