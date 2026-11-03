package com.xyp.gtnrlib.mixins.early.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;

@Mixin(ItemBlock.class)
public abstract class ItemBlockPlacementMixin {

    @Inject(method = "placeBlockAt", at = @At("RETURN"), remap = false, require = 0)
    private void gtnrlib$placed(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ, int meta, CallbackInfoReturnable<Boolean> result) {
        if (world.isRemote && result.getReturnValue()
            && FirstPersonAnimations.INSTANCE.enabled()
            && player == Minecraft.getMinecraft().thePlayer) FirstPersonAnimations.INSTANCE.trigger("useitem");
    }
}
