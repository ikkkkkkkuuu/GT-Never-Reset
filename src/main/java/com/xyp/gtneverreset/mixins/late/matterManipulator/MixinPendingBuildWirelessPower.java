package com.xyp.gtneverreset.mixins.late.matterManipulator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBuild;
import com.xyp.gtneverreset.common.world.WirelessPowerEvents;

@Mixin(value = PendingBuild.class, remap = false)
public abstract class MixinPendingBuildWirelessPower {

    @WrapOperation(
        method = "tryPlaceBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemBlock;placeBlockAt(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;IIIIFFFI)Z"))
    private boolean gtnr$placed(ItemBlock item, ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
        int side, float hitX, float hitY, float hitZ, int metadata, Operation<Boolean> original) {
        boolean placed = original.call(item, stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata);
        if (placed && !world.isRemote)
            WirelessPowerEvents.INSTANCE.placedByManipulator(player, world.getTileEntity(x, y, z));
        return placed;
    }
}
