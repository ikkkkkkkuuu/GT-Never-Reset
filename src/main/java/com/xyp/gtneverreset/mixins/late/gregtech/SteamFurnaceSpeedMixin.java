package com.xyp.gtneverreset.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.xyp.gtneverreset.loader.ConfigLoader;
import com.xyp.gtneverreset.utils.recipes.RecipeSpeed;

import gregtech.common.tileentities.machines.steam.MTESteamFurnaceBronze;
import gregtech.common.tileentities.machines.steam.MTESteamFurnaceSteel;

@Mixin(value = { MTESteamFurnaceBronze.class, MTESteamFurnaceSteel.class }, remap = false)
public abstract class SteamFurnaceSpeedMixin {

    @ModifyConstant(
        method = "checkRecipe",
        constant = { @Constant(intValue = 256), @Constant(intValue = 128) },
        require = 1,
        allow = 1)
    private int gtneverreset$modifySteamFurnaceDuration(int duration) {
        ConfigLoader.ensureLoaded();
        return RecipeSpeed.getModifiedRecipeDuration(duration);
    }
}
