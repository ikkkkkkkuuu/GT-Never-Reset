package com.xyp.gtneverreset.mixins.late.avaritia;

import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fox.spiteful.avaritia.compat.draconicevolution.InfinityArmorRuntimeHandler", remap = false)
public abstract class InfinityChestFlightMixin {

    @Shadow
    @Final
    private Set<String> playersWithChest;

    // Avaritia 1.100 also resets flight speed for players who never wore its chestplate.
    @Inject(
        method = "updateFlight",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;remove(Ljava/lang/Object;)Z"),
        cancellable = true,
        require = 0)
    private void gtnr$resetOnlyAfterUnequipping(EntityPlayer player, String key, CallbackInfo ci) {
        if (!playersWithChest.contains(key)) ci.cancel();
    }
}
