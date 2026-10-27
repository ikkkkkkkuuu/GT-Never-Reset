package com.xyp.gtneverreset.mixins.early.minecraft;

import net.minecraft.tileentity.TileEntityFurnace;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.utils.recipes.RecipeSpeed;

@Mixin(TileEntityFurnace.class)
public abstract class VanillaFurnaceMixin {

    @ModifyConstant(method = "updateEntity", constant = @Constant(intValue = 200), require = 0)
    private int gtneverreset$modifyFurnaceCookTime(int constant) {
        if (MainConfig.recipeSpeed.mode == 0) {
            return constant;
        }
        return RecipeSpeed.getModifiedRecipeDuration(constant);
    }
}
