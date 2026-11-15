package com.xyp.gtneverreset.common.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.GTNRCreativeTabs;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class ChaosMetaItem extends Item {

    protected final Map<Integer, IIcon> iconMap = new HashMap<>();
    protected final Set<Integer> usedMetaIds = new TreeSet<>();
    private final String localizationName;
    private final String iconDirectory;

    protected ChaosMetaItem(String textureName, String localizationName) {
        this.iconDirectory = textureName;
        this.localizationName = localizationName.replace("bottledchaos", "bottleinchaos");
        setUnlocalizedName(textureName);
        setHasSubtypes(true);
        setMaxDamage(0);
        setCreativeTab(GTNRCreativeTabs.ITEMS);
    }

    @Override
    public String getUnlocalizedName() {
        return "item.gtnr." + localizationName;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return getUnlocalizedName() + "." + stack.getItemDamage();
    }

    @Override
    public int getMetadata(int meta) {
        return meta;
    }

    public ItemStack registerVariant(int meta) {
        if (!usedMetaIds.add(meta)) throw new IllegalArgumentException("Duplicate item metadata: " + meta);
        return getVariant(meta);
    }

    public ItemStack getVariant(int meta) {
        if (!usedMetaIds.contains(meta)) throw new IllegalArgumentException("Unknown item metadata: " + meta);
        return new ItemStack(this, 1, meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> items) {
        for (int meta : usedMetaIds) items.add(getVariant(meta));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        for (int meta : usedMetaIds) {
            iconMap.put(meta, register.registerIcon(GTNeverReset.MODID + ":" + iconDirectory + "/" + meta));
        }
        itemIcon = iconMap.get(0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int meta) {
        return iconMap.getOrDefault(meta, itemIcon);
    }
}
