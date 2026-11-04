package com.xyp.gtnrlib.mixins.late.tconstruct;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import tconstruct.weaponry.weapons.Crossbow;

@Mixin(value = Crossbow.class, remap = false)
public interface CrossbowAmmoAccess {

    @Invoker("searchForAmmo")
    ItemStack gtnrlib$findBolt(EntityPlayer player, ItemStack weapon);

    @Invoker("getLoadedAmmo")
    ItemStack gtnrlib$loadedBolt(ItemStack weapon);
}
