package com.xyp.gtnrlib.mixins.early.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;
import com.xyp.gtnrlib.client.animation.FirstPersonItemActions;

@Mixin(ItemBlock.class)
public abstract class ItemBlockPlacementMixin {

    @Inject(method = "placeBlockAt", at = @At("RETURN"), remap = false, require = 0)
    private void gtnrlib$placed(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ, int meta, CallbackInfoReturnable<Boolean> result) {
        if (!world.isRemote || !result.getReturnValue() || player != Minecraft.getMinecraft().thePlayer) return;
        // Backhand temporarily selects its extra inventory slot while using the offhand.
        int slot = player.inventory.currentItem;
        if (slot < 0 || slot >= InventoryPlayer.getHotbarSize()) return;
        FirstPersonItemActions.INSTANCE.trigger("place");
        FirstPersonAnimations.INSTANCE.trigger("useitem");
    }
}
