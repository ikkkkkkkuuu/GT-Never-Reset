package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.crafting.CraftingCompletionService;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.cluster.implementations.CraftingCPUCluster;

@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class CraftingCompletionMixin {

    @Shadow
    protected boolean isComplete;

    @Shadow
    protected CraftingCPUCluster.finalOutput finalOutput;

    @Unique
    private final Map<String, Set<IAEItemStack>> gtnr$completionOwners = new LinkedHashMap<>();

    @Inject(method = { "submitJob", "mergeJob" }, at = @At("RETURN"))
    private void gtnr$recordAcceptedOrder(IGrid grid, ICraftingJob<?> job, BaseActionSource source,
        ICraftingRequester requester, CallbackInfoReturnable<ICraftingLink> callback) {
        if (callback.getReturnValue() != null && source instanceof PlayerSource player && player.player != null) {
            gtnr$rememberOrder(player.player.getCommandSenderName(), job.getOutput());
        }
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void gtnr$restoreOrder(NBTTagCompound tag, CallbackInfo callback) {
        gtnr$completionOwners.clear();
        if (!isComplete) {
            CraftingCPUCluster cpu = (CraftingCPUCluster) (Object) this;
            gtnr$rememberOrder(cpu.getSourcePlayer(), finalOutput.getOriginalOutput());
        }
    }

    @Inject(
        method = "completeJob",
        at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"))
    private void gtnr$recordCompletion(CallbackInfo callback) {
        if (isComplete) return;
        try {
            // AE2 can finish a virtual output without physically crafting it.
            if (finalOutput.isFakeCrafting()) return;
            IGrid grid = ((CraftingCPUCluster) (Object) this).getGrid();
            for (var owner : gtnr$completionOwners.entrySet()) {
                for (IAEItemStack output : owner.getValue()) {
                    CraftingCompletionService.completed(grid, owner.getKey(), output.getItemStack());
                }
            }
        } catch (RuntimeException | LinkageError failure) {
            GTNeverReset.LOG.warn("could not record completed crafting items", failure);
        } finally {
            gtnr$completionOwners.clear();
        }
    }

    @Inject(method = { "cancel", "resetFinalOutput" }, at = @At("HEAD"))
    private void gtnr$forgetOrder(CallbackInfo callback) {
        gtnr$completionOwners.clear();
    }

    @Unique
    private void gtnr$rememberOrder(String owner, IAEStack<?> output) {
        if (owner != null && !owner.isEmpty() && output instanceof IAEItemStack item) {
            gtnr$completionOwners.computeIfAbsent(owner, ignored -> new LinkedHashSet<>())
                .add(item.copy());
        }
    }
}
