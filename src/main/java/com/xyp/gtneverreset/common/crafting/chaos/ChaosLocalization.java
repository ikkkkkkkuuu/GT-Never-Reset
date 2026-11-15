package com.xyp.gtneverreset.common.crafting.chaos;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import gregtech.api.util.GTUtility;

public final class ChaosLocalization {

    private ChaosLocalization() {}

    public static String tr(String key, Object... args) {
        return args.length == 0 ? StatCollector.translateToLocal(key)
            : StatCollector.translateToLocalFormatted(key, args);
    }

    public static int getIntegratedCircuitConfigurationValue(ItemStack stack) {
        return GTUtility.isAnyIntegratedCircuit(stack) ? stack.getItemDamage() : -1;
    }
}
