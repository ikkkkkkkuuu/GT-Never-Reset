package com.xyp.gtneverreset.common.projector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

final class ProjectorBlockRemoval {

    private ProjectorBlockRemoval() {}

    static boolean canEdit(EntityPlayerMP player, int x, int y, int z, ItemStack tool) {
        return player.capabilities.allowEdit && player.worldObj.canMineBlock(player, x, y, z)
            && !((WorldServer) player.worldObj).func_73046_m()
                .isBlockProtected(player.worldObj, x, y, z, player)
            && player.canPlayerEdit(x, y, z, ForgeDirection.UP.ordinal(), tool);
    }

    static boolean canClear(World world, int x, int y, int z) {
        return world.getBlock(x, y, z)
            .getBlockHardness(world, x, y, z) >= 0 && !protectedTile(world.getTileEntity(x, y, z));
    }

    static Result remove(EntityPlayerMP player, int x, int y, int z, boolean protectBlockEntity, ItemStack expectedDrop,
        Consumer<ItemStack> drops) {
        World world = player.worldObj;
        if (y < 0 || y >= world.getHeight()
            || !world.getChunkProvider()
                .chunkExists(x >> 4, z >> 4))
            return Result.FAILED;
        Block block = world.getBlock(x, y, z);
        if (block.isAir(world, x, y, z)) return Result.FAILED;
        if (block.getBlockHardness(world, x, y, z) < 0
            || protectBlockEntity && protectedTile(world.getTileEntity(x, y, z))) return Result.OCCUPIED;
        if (!canEdit(player, x, y, z, player.getCurrentEquippedItem())
            || ForgeHooks.onBlockBreakEvent(world, player.theItemInWorldManager.getGameType(), player, x, y, z)
                .isCanceled())
            return Result.DENIED;

        int meta = world.getBlockMetadata(x, y, z);
        boolean creative = player.capabilities.isCreativeMode;
        List<ItemStack> harvested = creative ? List.of()
            : expectedDrop != null && !block.hasTileEntity(meta) ? List.of(expectedDrop.copy())
                : block.getDrops(world, x, y, z, meta, 0);
        AxisAlignedBB area = AxisAlignedBB.getBoundingBox(x - 0.5, y - 0.5, z - 0.5, x + 1.5, y + 1.5, z + 1.5);
        Set<EntityItem> existing = new HashSet<>();
        for (Object entry : world.getEntitiesWithinAABB(EntityItem.class, area)) existing.add((EntityItem) entry);
        block.onBlockHarvested(world, x, y, z, meta, player);
        if (!block.removedByPlayer(world, player, x, y, z, false)) return Result.FAILED;
        block.onBlockDestroyedByPlayer(world, x, y, z, meta);
        for (ItemStack stack : harvested) if (stack != null && stack.stackSize > 0) drops.accept(stack);
        for (Object entry : world.getEntitiesWithinAABB(EntityItem.class, area)) {
            EntityItem entity = (EntityItem) entry;
            if (entity.isDead || existing.contains(entity)) continue;
            ItemStack stack = entity.getEntityItem();
            entity.setDead();
            if (stack != null && stack.stackSize > 0) drops.accept(stack);
        }
        return Result.REMOVED;
    }

    static boolean protectedTile(TileEntity tile) {
        return tile instanceof IInventory || tile instanceof IFluidHandler || tile instanceof IGregTechTileEntity;
    }

    enum Result {
        REMOVED,
        OCCUPIED,
        DENIED,
        FAILED
    }
}
