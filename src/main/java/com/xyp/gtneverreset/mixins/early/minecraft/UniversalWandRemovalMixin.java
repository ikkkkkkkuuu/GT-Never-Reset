package com.xyp.gtneverreset.mixins.early.minecraft;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.xyp.gtneverreset.common.wand.UniversalWandProtectionHandler;

@Mixin(World.class)
public abstract class UniversalWandRemovalMixin {

    // World removal also serves logout, respawn and third-party dimension transfers.
    @WrapMethod(method = { "removeEntity", "removePlayerEntityDangerously" })
    private void gtneverreset$normalRemoval(Entity entity, Operation<Void> original) {
        UniversalWandProtectionHandler.beginRemoval(entity);
        try {
            original.call(entity);
        } finally {
            UniversalWandProtectionHandler.endRemoval();
        }
    }
}
