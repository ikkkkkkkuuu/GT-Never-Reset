package com.xyp.gtneverreset.mixins.late.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.xyp.gtneverreset.api.mixinHelper.CrucibleCooldownAccess;

import thaumcraft.common.tiles.TileCrucible;

/** Exposes only the native successful-craft cooldown; ordinary thrown-item crafting remains unchanged. */
@Mixin(value = TileCrucible.class, remap = false)
public abstract class MixinPackagedCrucible implements CrucibleCooldownAccess {

    @Shadow
    // This shadow writes the target object's state and must remain a field.
    @SuppressWarnings("FieldCanBeLocal")
    private long counter;

    @Override
    public void gtnr$resetCraftingCooldown() {
        counter = -250L;
    }
}
