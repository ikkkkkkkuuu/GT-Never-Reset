package com.xyp.gtneverreset.mixins.late.gregtech;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtneverreset.common.projector.ProjectorPlacementUpdates;

import gregtech.common.blocks.ItemMachines;

@Mixin(value = ItemMachines.class, remap = false)
public abstract class ProjectorItemMachinesMixin {

    @WrapOperation(
        method = "placeBlockAt",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetHandlerPlayServer;sendPacket(Lnet/minecraft/network/Packet;)V",
            remap = true),
        require = 1)
    private void gtnr$holdProjectedMachine(NetHandlerPlayServer handler, Packet packet, Operation<Void> original,
        ItemStack item, EntityPlayer player, World world, int x, int y, int z) {
        if (!ProjectorPlacementUpdates.isHeld(world, x, y, z)) original.call(handler, packet);
    }
}
