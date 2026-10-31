package com.xyp.gtneverreset.common.flux;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class FluxDropData {

    public static void normalize(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return;
        NBTTagCompound original = stack.getTagCompound();
        NBTTagCompound tag = (NBTTagCompound) original.copy();
        tag.removeTag("fluxVoltage");
        tag.removeTag("fluxAmperage");
        tag.removeTag("fluxEnabled");
        tag.removeTag("fluxConnected");
        tag.removeTag("fluxRedstone");
        tag.removeTag("fluxPriority");
        tag.removeTag("fluxSurge");
        tag.removeTag("fluxName");
        tag.removeTag("fluxLimit");
        tag.removeTag("fluxDisableLimit");
        if (tag.hasNoTags()) stack.setTagCompound(null);
        else if (!tag.equals(original)) stack.setTagCompound(tag);
    }
}
