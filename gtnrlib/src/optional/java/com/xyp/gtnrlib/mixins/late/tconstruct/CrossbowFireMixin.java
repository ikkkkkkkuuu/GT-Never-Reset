package com.xyp.gtnrlib.mixins.late.tconstruct;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnrlib.compat.tconstruct.TinkersCrossbowAnimation;

import tconstruct.weaponry.weapons.Crossbow;

@Mixin(value = Crossbow.class, remap = false)
public abstract class CrossbowFireMixin {

    // fire reaches this point only after resolving a loaded bolt and creating its projectile.
    @Inject(
        method = "fire",
        at = @At(
            value = "INVOKE",
            target = "Ltconstruct/weaponry/weapons/Crossbow;playFiringSound(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;FF)V"))
    private void gtnrlib$fire(ItemStack weapon, World world, EntityPlayer player, CallbackInfo ci) {
        if (world.isRemote && player == Minecraft.getMinecraft().thePlayer && weapon == player.getHeldItem())
            TinkersCrossbowAnimation.fire(weapon);
    }
}
