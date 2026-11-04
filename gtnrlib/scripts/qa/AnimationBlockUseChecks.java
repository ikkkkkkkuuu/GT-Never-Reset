package com.xyp.gtnrlib.qa;

import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import com.xyp.gtnrlib.client.animation.FirstPersonItemActions;

import cpw.mods.fml.common.Loader;

final class AnimationBlockUseChecks {

    static void verify(Minecraft mc) throws Exception {
        var player = mc.thePlayer;
        var world = mc.theWorld;
        int x = (int) Math.floor(player.posX) + 2;
        int y = (int) Math.ceil(player.posY) + 1;
        int z = (int) Math.floor(player.posZ);
        var hit = mc.objectMouseOver;
        var held = player.getHeldItem();
        var block = new ItemStack(Blocks.brick_block, 8);
        var animations = FirstPersonItemActions.INSTANCE;
        var click = Minecraft.class.getDeclaredMethod("func_147121_ag");
        click.setAccessible(true);
        Method setOffhand = null;
        ItemStack offhand = null;
        if (!world.isAirBlock(x, y, z) || !world.isAirBlock(x, y + 1, z))
            throw new AssertionError("Expected clear space for the block-use fixture");
        try {
            world.setBlock(x, y, z, Blocks.stone);
            mc.objectMouseOver = new MovingObjectPosition(x, y, z, 1, Vec3.createVectorHelper(x + .5, y + 1, z + .5));
            if (Loader.isModLoaded("backhand")) {
                var api = Class.forName("xonin.backhand.api.core.BackhandUtils");
                offhand = (ItemStack) api.getMethod("getOffhandItem", EntityPlayer.class)
                    .invoke(null, player);
                setOffhand = api.getMethod("setPlayerOffhandItem", EntityPlayer.class, ItemStack.class);
                setOffhand.invoke(null, player, block);
                player.inventory.mainInventory[player.inventory.currentItem] = null;
                player.isSwingInProgress = false;
                click.invoke(mc);
                if (world.getBlock(x, y + 1, z) != Blocks.brick_block || player.isSwingInProgress
                    || animations.playing("place"))
                    throw new AssertionError(
                        "Offhand placement must succeed without starting a main-hand swing or clip");
                setOffhand.invoke(null, player, null);
                world.setBlockToAir(x, y + 1, z);
            }
            player.inventory.mainInventory[player.inventory.currentItem] = block;
            world.setBlock(x, y, z, Blocks.noteblock);
            player.isSwingInProgress = false;
            click.invoke(mc);
            if (!player.isSwingInProgress || !animations.playing("interact"))
                throw new AssertionError("Expected main-hand block interaction and interact clip");
            world.setBlock(x, y, z, Blocks.stone);
            player.isSwingInProgress = false;
            click.invoke(mc);
            if (world.getBlock(x, y + 1, z) != Blocks.brick_block || !player.isSwingInProgress
                || !animations.playing("place"))
                throw new AssertionError("Expected main-hand placement and place clip");
        } finally {
            if (setOffhand != null) setOffhand.invoke(null, player, offhand);
            player.inventory.mainInventory[player.inventory.currentItem] = held;
            mc.objectMouseOver = hit;
            world.setBlockToAir(x, y + 1, z);
            world.setBlockToAir(x, y, z);
        }
    }
}
