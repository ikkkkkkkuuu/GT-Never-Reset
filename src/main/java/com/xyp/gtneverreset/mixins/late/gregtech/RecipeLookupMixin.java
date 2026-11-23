package com.xyp.gtneverreset.mixins.late.gregtech;

import java.util.Iterator;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeCandidateCache;

import gregtech.api.recipe.lookup.GTRecipeLookup;
import gregtech.api.recipe.lookup.GTRecipeLookupIngredient;
import gregtech.api.util.GTRecipe;

@Mixin(value = GTRecipeLookup.class, remap = false)
public abstract class RecipeLookupMixin {

    @Unique
    private final Object gtnr$cacheIdentity = new Object();
    @Unique
    private volatile long gtnr$revision;

    @WrapMethod(method = "add")
    private void gtnr$updateLookup(GTRecipe recipe, List<List<GTRecipeLookupIngredient>> ingredients,
        Operation<Void> original) {
        synchronized (this) {
            gtnr$revision++;
            original.call(recipe, ingredients);
        }
    }

    @Inject(method = "iterator", at = @At("HEAD"), cancellable = true)
    private void gtnr$cachedCandidates(List<GTRecipeLookupIngredient> ingredients,
        CallbackInfoReturnable<Iterator<GTRecipe>> cir) {
        Iterator<GTRecipe> cached = RecipeCandidateCache
            .candidates((GTRecipeLookup) (Object) this, gtnr$cacheIdentity, gtnr$revision, ingredients);
        if (cached != null) cir.setReturnValue(cached);
    }
}
