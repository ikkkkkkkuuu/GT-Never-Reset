package com.xyp.gtneverreset.mixins.late.gregtech;

import net.minecraftforge.fluids.FluidStack;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;
import com.xyp.gtneverreset.utils.recipes.RecipeSpeed;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.common.tileentities.machines.multi.MTEThermalBoiler;
import gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production.MTEThermalBoilerLegacy;

@Mixin(value = { MTEThermalBoiler.class, MTEThermalBoilerLegacy.class }, remap = false)
public abstract class ThermalBoilerSpeedMixin {

    @ModifyExpressionValue(
        method = "checkProcessing",
        at = @At(value = "INVOKE", target = "checkProcessing()Lgregtech/api/recipe/check/CheckRecipeResult;"),
        require = 1,
        allow = 1)
    private CheckRecipeResult gtneverreset$limitBoilerBatchDuration(CheckRecipeResult result) {
        if (!result.wasSuccessful()) return result;
        ConfigLoader.ensureLoaded();
        MTEMultiBlockBase boiler = (MTEMultiBlockBase) (Object) this;
        boiler.mMaxProgresstime = limitBatchDuration(boiler.mMaxProgresstime);
        return result;
    }

    @ModifyReturnValue(method = "getEfficiencyIncrease", at = @At("RETURN"), require = 1, allow = 1)
    private int gtneverreset$modifyWarmupRate(int increase) {
        ConfigLoader.ensureLoaded();
        MTEMultiBlockBase boiler = (MTEMultiBlockBase) (Object) this;
        return scaledWarmupRate(increase, boiler.getMaxEfficiency(null));
    }

    @Redirect(
        method = "checkProcessing",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraftforge/fluids/FluidStack;amount:I",
            opcode = Opcodes.PUTFIELD,
            ordinal = 0),
        require = 1,
        allow = 1)
    private void gtneverreset$scaleSteamWithoutOverflow(FluidStack steam, int amount) {
        if (MainConfig.recipeSpeed.mode == 0) {
            steam.amount = amount;
            return;
        }
        MTEMultiBlockBase boiler = (MTEMultiBlockBase) (Object) this;
        steam.amount = scaleSteamOutput(steam.amount, boiler.mEfficiency, boiler.getMaxEfficiency(null));
    }

    @Unique
    private static int scaledWarmupRate(int increase, int maximum) {
        if (MainConfig.recipeSpeed.mode == 0 || increase <= 0 || maximum <= 0) return increase;
        int warmupTicks = (int) (((long) maximum + increase - 1) / increase);
        int duration = RecipeSpeed.getModifiedRecipeDuration(warmupTicks);
        return (int) (((long) maximum + duration - 1) / duration);
    }

    @Unique
    private static int scaleSteamOutput(int amount, int efficiency, int maximum) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (long) amount * efficiency / maximum));
    }

    @Unique
    private static int limitBatchDuration(int duration) {
        // Multiplier mode has already been applied by RecipeMapBackend.
        if (MainConfig.recipeSpeed.mode != 1 || duration <= 0) return duration;
        return Math.min(duration, RecipeSpeed.getModifiedRecipeDuration(duration));
    }
}
