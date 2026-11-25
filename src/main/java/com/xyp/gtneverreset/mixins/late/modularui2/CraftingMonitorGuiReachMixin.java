package com.xyp.gtneverreset.mixins.late.modularui2;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.factory.TileEntityGuiFactory;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xyp.gtneverreset.common.me.CraftingMonitorGuiAccess;

import gregtech.api.modularui2.MetaTileEntityGuiHandler;

@Mixin(value = { TileEntityGuiFactory.class, MetaTileEntityGuiHandler.class }, remap = false)
public abstract class CraftingMonitorGuiReachMixin {

    @ModifyExpressionValue(
        method = "canInteractWith(Lnet/minecraft/entity/player/EntityPlayer;Lcom/cleanroommc/modularui/factory/PosGuiData;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lcom/cleanroommc/modularui/factory/PosGuiData;getSquaredDistance(Lnet/minecraft/entity/Entity;)D"),
        require = 0)
    private double gtnr$monitorDistance(double distance, EntityPlayer player, PosGuiData data) {
        return CraftingMonitorGuiAccess
            .interactionDistance(distance, player, data.getWorld(), data.getX(), data.getY(), data.getZ());
    }
}
