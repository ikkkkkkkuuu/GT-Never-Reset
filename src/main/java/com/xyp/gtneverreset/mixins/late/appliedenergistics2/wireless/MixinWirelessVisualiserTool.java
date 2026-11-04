package com.xyp.gtneverreset.mixins.late.appliedenergistics2.wireless;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.WirelessVisualisationPacket;
import com.xyp.gtneverreset.common.wireless.WirelessCardVisualisation;

import appeng.api.util.DimensionalCoord;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.NetworkHandler;
import appeng.items.tools.ToolNetworkVisualiser;

@Mixin(value = ToolNetworkVisualiser.class, remap = false)
public abstract class MixinWirelessVisualiserTool {

    @WrapOperation(
        method = "onUpdate",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lappeng/items/tools/ToolNetworkVisualiser;needToUpdate("
                + "Lnet/minecraft/entity/player/EntityPlayerMP;Lappeng/api/util/DimensionalCoord;)Z",
            remap = false))
    private boolean gtnr$refreshCadence(EntityPlayerMP player, DimensionalCoord position, Operation<Boolean> original,
        ItemStack stack, World world, Entity entity, int slot, boolean active) {
        return WirelessCardVisualisation.delegated(stack) ? WirelessCardVisualisation.needsUpdate(player, stack)
            : original.call(player, position);
    }

    @WrapOperation(
        method = "onUpdate",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/util/DimensionalCoord;isInWorld(Lnet/minecraft/world/World;)Z",
            remap = false))
    private boolean gtnr$viewerDimension(DimensionalCoord position, World world, Operation<Boolean> original,
        ItemStack stack, World currentWorld, Entity entity, int slot, boolean active) {
        return original.call(position, WirelessCardVisualisation.delegated(stack) ? entity.worldObj : world);
    }

    @WrapOperation(
        method = "onUpdate",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lappeng/core/sync/network/NetworkHandler;sendTo("
                + "Lappeng/core/sync/AppEngPacket;Lnet/minecraft/entity/player/EntityPlayerMP;)V",
            remap = false))
    private void gtnr$cardSnapshot(NetworkHandler network, AppEngPacket message, EntityPlayerMP player,
        Operation<Void> original, ItemStack stack, World world, Entity entity, int slot, boolean active) {
        if (WirelessCardVisualisation.delegated(stack)) {
            NetWorkHandler
                .sendTo(new WirelessVisualisationPacket(stack.getTagCompound(), player.dimension, message), player);
        } else {
            original.call(network, message, player);
        }
    }
}
