package com.xyp.gtneverreset.common.crafting.chaos.material;

import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.Nullable;

import appeng.api.storage.data.IAEItemStack;
import gregtech.api.util.GTUtility;

public class ChaosItemKey extends GTUtility.ItemId {

    private Item item;
    private int metaData;
    private NBTTagCompound nbt;

    public ChaosItemKey(Item item, int metaData, NBTTagCompound nbt) {
        this.item = item;
        this.metaData = metaData;
        this.nbt = nbt;
    }

    public ChaosItemKey(Item item, int metaData) {
        this.item = item;
        this.metaData = metaData;
    }

    public ChaosItemKey(Item item) {
        this.item = item;
        this.metaData = 0;
    }

    public ChaosItemKey() {}

    public static final ChaosItemKey NULL = new ChaosItemKey();

    public static ChaosItemKey create(ItemStack itemStack) {
        if (null == itemStack) return NULL;
        return new ChaosItemKey(itemStack.getItem(), itemStack.getItemDamage(), itemStack.getTagCompound());
    }

    public static ChaosItemKey create(Item item) {
        if (null == item) return NULL;
        return new ChaosItemKey(item, 0);
    }

    public static ChaosItemKey create(Item item, int metaData) {
        if (null == item) return NULL;
        return new ChaosItemKey(item, metaData);
    }

    public static ChaosItemKey create(Block block) {
        if (null == block) return NULL;
        return create(new ItemStack(block));
    }

    public static ChaosItemKey create(Block block, int metaData) {
        if (null == block) return NULL;
        return create(new ItemStack(block, 1, metaData));
    }

    public static ChaosItemKey createNoNBT(ItemStack itemStack) {
        if (null == itemStack) return NULL;
        return new ChaosItemKey(itemStack.getItem(), itemStack.getItemDamage());
    }

    public static ChaosItemKey createAsWildcard(ItemStack itemStack) {
        if (null == itemStack) return NULL;
        return new ChaosItemKey(itemStack.getItem(), OreDictionary.WILDCARD_VALUE);
    }

    public static ChaosItemKey create(IAEItemStack aeStack) {
        return create(aeStack.getItemStack());
    }

    public static ChaosItemKey[] create(ItemStack... itemStacks) {
        ChaosItemKey[] out = new ChaosItemKey[itemStacks.length];
        for (int i = 0; i < itemStacks.length; i++) {
            out[i] = ChaosItemKey.create(itemStacks[i]);
        }
        return out;
    }

    public ItemStack getItemStack() {
        ItemStack r = new ItemStack(item, 1, metaData);
        if (nbt != null) {
            r.setTagCompound(nbt);
        }
        return r;
    }

    public ItemStack getItemStack(int amount) {
        ItemStack r = new ItemStack(item, amount, metaData);
        if (nbt != null) {
            r.setTagCompound(nbt);
        }
        return r;
    }

    public ItemStack getItemStackWithNBT() {
        ItemStack itemStack = new ItemStack(item, 1, metaData);
        itemStack.setTagCompound(nbt);
        return itemStack;
    }

    public ItemStack getItemStackWithNBT(int amount) {
        ItemStack itemStack = new ItemStack(item, amount, metaData);
        itemStack.setTagCompound(nbt);
        return itemStack;
    }

    public ItemStack getItemStackWithoutNBT() {
        return new ItemStack(item, 1, metaData);
    }

    public ItemStack getItemStackWithoutNBT(int amount) {
        return new ItemStack(item, amount, metaData);
    }

    public boolean isWildcard() {
        return this.metaData == OreDictionary.WILDCARD_VALUE;
    }

    public ChaosItemKey setItem(Item item) {
        this.item = item;
        return this;
    }

    public ChaosItemKey setMetaData(int metaData) {
        this.metaData = metaData;
        return this;
    }

    public ChaosItemKey setNbt(NBTTagCompound nbt) {
        this.nbt = nbt;
        return this;
    }

    @Override
    public String toString() {
        return item == null ? "<missing item>"
            : Item.itemRegistry.getNameForObject(item) + ":" + metaData + (nbt == null ? "" : nbt.toString());
    }

    @Override
    public Item item() {
        return item;
    }

    @Override
    public int metaData() {
        return metaData;
    }

    @Nullable
    @Override
    public NBTTagCompound nbt() {
        return nbt;
    }

    @Nullable
    @Override
    public Integer stackSize() {
        // todo
        return null;
    }

    @Nullable
    protected NBTTagCompound getNBT() {
        return nbt;
    }

    protected Item getItem() {
        return item;
    }

    protected int getMetaData() {
        return metaData;
    }

    public boolean equalItemStack(ItemStack itemStack) {
        if (isWildcard()) return equals(createAsWildcard(itemStack));
        if (nbt != null) return equals(create(itemStack));
        return equals(createNoNBT(itemStack));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChaosItemKey)) {
            return false;
        }
        ChaosItemKey tstItemID = (ChaosItemKey) o;
        return metaData == tstItemID.metaData && Objects.equals(item, tstItemID.item)
            && Objects.equals(nbt, tstItemID.nbt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, metaData, nbt);
    }
}
