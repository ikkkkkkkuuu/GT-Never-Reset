package com.xyp.gtneverreset.common.item.items;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.GTNRCreativeTabs;
import com.xyp.gtneverreset.common.crafting.chaos.ChaosChain;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ChaosCredentialItem extends Item {

    public static final String REGISTRY_NAME = "chaos_credential";
    public static final String CHAIN_KEY = "chaosChain";

    public ChaosCredentialItem() {
        // #tr item.chaos_credential.name
        // # Chaos Processing Credential
        // # zh_CN 混沌加工凭证
        setUnlocalizedName(REGISTRY_NAME);
        setTextureName(GTNeverReset.MODID + ":chaos_credential");
        setCreativeTab(GTNRCreativeTabs.ITEMS);
        setMaxStackSize(1);
    }

    public static boolean isBound(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ChaosCredentialItem
            && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey(CHAIN_KEY, 10);
    }

    public static ItemStack bind(ItemStack blank, ChaosChain chain) {
        if (blank == null || !(blank.getItem() instanceof ChaosCredentialItem) || isBound(blank))
            throw new IllegalArgumentException("blank credential required");
        ItemStack result = blank.copy();
        result.stackSize = 1;
        if (!result.hasTagCompound()) result.setTagCompound(new NBTTagCompound());
        result.getTagCompound()
            .setTag(CHAIN_KEY, chain.save());
        return result;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advanced) {
        if (!isBound(stack)) {
            // #tr gtnr.chaos.credential.blank
            // # Legacy credential. Select routes directly in a processing group hatch.
            // # zh_CN 旧版凭证；请直接在加工组仓选择产线。
            lines.add(StatCollector.translateToLocal("gtnr.chaos.credential.blank"));
            return;
        }
        NBTTagCompound chain = stack.getTagCompound()
            .getCompoundTag(CHAIN_KEY);
        // #tr gtnr.chaos.credential.bound
        // # Route: %s (%s steps)
        // # zh_CN 路线：%s（%s 道工序）
        lines.add(
            StatCollector.translateToLocalFormatted(
                "gtnr.chaos.credential.bound",
                StatCollector.translateToLocal(chain.getString("name")),
                chain.getTagList("steps", 10)
                    .tagCount()));
        // #tr gtnr.chaos.credential.install
        // # Preserved for old saves; the machine no longer requires this item.
        // # zh_CN 保留用于旧存档兼容；机器已不再需要此物品。
        lines.add(StatCollector.translateToLocal("gtnr.chaos.credential.install"));
    }
}
