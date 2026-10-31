package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.PlayerSource;
import appeng.me.cluster.implementations.CraftingCPUCluster;

@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class MixinCraftingCPUClusterFollow {

    @Shadow
    @Final
    private List<String> playersFollowingCurrentCraft;

    @Inject(method = "submitJob", at = @At("RETURN"), require = 1)
    private void gtnr$autoFollowSubmitter(IGrid grid, ICraftingJob job, BaseActionSource source,
        ICraftingRequester requestingMachine, CallbackInfoReturnable<ICraftingLink> cir) {
        if (cir.getReturnValue() == null) return;
        if (!(source instanceof PlayerSource playerSource) || playerSource.player == null) return;
        String name = playerSource.player.getCommandSenderName();
        if (name != null && !playersFollowingCurrentCraft.contains(name)) {
            playersFollowingCurrentCraft.add(name);
        }
    }
}
