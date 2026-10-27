package com.xyp.gtneverreset.common.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;

public final class StableEndCrystalEntity extends Entity {

    public StableEndCrystalEntity(World world) {
        super(world);
        preventEntitySpawning = true;
        isImmuneToFire = true;
        setSize(1.0F, 2.0F);
    }

    @Override
    protected void entityInit() {}

    @Override
    public boolean canBeCollidedWith() {
        return !isDead;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (isDead || source.isExplosion() || !(source.getEntity() instanceof EntityPlayer player)) return false;
        int x = MathHelper.floor_double(posX), y = MathHelper.floor_double(posY), z = MathHelper.floor_double(posZ);
        ItemStack crystal = GTNRItemList.StableEndCrystal.get(1);
        if (!worldObj.canMineBlock(player, x, y, z) || !player.canPlayerEdit(x, y, z, 1, crystal)) return false;
        if (!worldObj.isRemote) {
            setDead();
            if (!player.capabilities.isCreativeMode) entityDropItem(crystal, 0);
        }
        return true;
    }

    @Override
    public ItemStack getPickedResult(MovingObjectPosition target) {
        return GTNRItemList.StableEndCrystal.get(1);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {}

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {}
}
