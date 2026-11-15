package com.xyp.gtneverreset.common.item.items;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.common.item.ChaosMetaItem;

public final class ChaosComponentsItem extends ChaosMetaItem {

    public ChaosComponentsItem() {
        super("chaos_components", "chaos_components");
        setMaxStackSize(64);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return switch (stack.getItemDamage()) {
            // #tr item.gtnr.chaos_components.0.name
            // # Critical Photon
            // # zh_CN 临界光子
            case 0 -> StatCollector.translateToLocal("item.gtnr.chaos_components.0.name");
            // #tr item.gtnr.chaos_components.1.name
            // # Gravitational Lens
            // # zh_CN 引力透镜
            case 1 -> StatCollector.translateToLocal("item.gtnr.chaos_components.1.name");
            // #tr item.gtnr.chaos_components.2.name
            // # Ball Lightning Upgrade Chip
            // # zh_CN 球状闪电升级芯片
            case 2 -> StatCollector.translateToLocal("item.gtnr.chaos_components.2.name");
            // #tr item.gtnr.chaos_components.3.name
            // # White Dwarf Ingot Mold
            // # zh_CN 白矮星铸锭模具
            case 3 -> StatCollector.translateToLocal("item.gtnr.chaos_components.3.name");
            // #tr item.gtnr.chaos_components.4.name
            // # Swelegfyr Upgrade Chip
            // # zh_CN 炎狱高炉升级芯片
            case 4 -> StatCollector.translateToLocal("item.gtnr.chaos_components.4.name");
            // #tr item.gtnr.chaos_components.5.name
            // # Prototype Conversion Protocol
            // # zh_CN 原型机转换协议
            case 5 -> StatCollector.translateToLocal("item.gtnr.chaos_components.5.name");
            default -> super.getItemStackDisplayName(stack);
        };
    }
}
