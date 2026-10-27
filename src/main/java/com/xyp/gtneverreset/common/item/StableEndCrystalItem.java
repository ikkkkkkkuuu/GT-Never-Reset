package com.xyp.gtneverreset.common.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.entity.StableEndCrystalEntity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class StableEndCrystalItem extends Item {

    public static final String REGISTRY_NAME = "StableEndCrystal";

    public StableEndCrystalItem() {
        // #tr item.gtneverreset.stable_end_crystal.name
        // # Stable End Crystal
        // # zh_CN 稳定末影水晶
        setUnlocalizedName("gtneverreset.stable_end_crystal");
        setTextureName(GTNeverReset.MODID + ":stable_end_crystal");
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (stack.stackSize <= 0 || side != ForgeDirection.UP.ordinal()
            || !world.isSideSolid(x, y, z, ForgeDirection.UP)) return false;
        int crystalY = y + 1;
        if (!world.canMineBlock(player, x, y, z) || !player.canPlayerEdit(x, y, z, side, stack)) return false;
        for (int dy = 0; dy < 2; dy++) {
            int targetY = crystalY + dy;
            if (targetY >= world.getHeight() || !world.isAirBlock(x, targetY, z)
                || !world.canMineBlock(player, x, targetY, z)
                || !player.canPlayerEdit(x, targetY, z, side, stack)) return false;
        }
        StableEndCrystalEntity crystal = new StableEndCrystalEntity(world);
        crystal.setPosition(x + 0.5, crystalY, z + 0.5);
        if (!world.checkNoEntityCollision(crystal.boundingBox)) return false;
        if (world.isRemote) return true;
        if (!world.spawnEntityInWorld(crystal)) return false;
        if (!player.capabilities.isCreativeMode) {
            stack.stackSize--;
            player.inventory.markDirty();
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advanced) {
        // #tr tooltip.gtnr.stable_end_crystal.safe
        // # Does not explode or ignite blocks
        // # zh_CN 不会爆炸或点燃方块
        lines.add(StatCollector.translateToLocal("tooltip.gtnr.stable_end_crystal.safe"));
        // #tr tooltip.gtnr.stable_end_crystal.place
        // # Place on a solid top face with two air blocks above
        // # zh_CN 放置在坚固顶面上，上方需有两格空气
        lines.add(StatCollector.translateToLocal("tooltip.gtnr.stable_end_crystal.place"));
        // #tr tooltip.gtnr.stable_end_crystal.recover
        // # Break to recover; powers End Crystal Resonators without being consumed
        // # zh_CN 打破可回收；为末晶谐振器供能且不被消耗
        lines.add(StatCollector.translateToLocal("tooltip.gtnr.stable_end_crystal.recover"));
    }
}
