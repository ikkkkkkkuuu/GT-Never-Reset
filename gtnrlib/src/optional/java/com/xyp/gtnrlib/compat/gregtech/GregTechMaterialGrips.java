package com.xyp.gtnrlib.compat.gregtech;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

public final class GregTechMaterialGrips {

    private GregTechMaterialGrips() {}

    public static boolean matches(ItemStack stack, int[] oreIds) {
        var data = GTOreDictUnificator.getAssociation(stack);
        if (data != null && data.mPrefix.isMaterialBased() && data.mMaterial.mMaterial != Materials._NULL) return true;
        // Addon materials and non-unifying forms can belong to a GT prefix without a unification entry.
        for (int ore : oreIds) {
            var prefix = OrePrefixes.getOrePrefix(OreDictionary.getOreName(ore));
            if (prefix != null && prefix.isMaterialBased() && prefix.contains(stack)) return true;
        }
        return false;
    }
}
