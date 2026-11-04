package com.xyp.gtnrlib.compat.ic2;

import com.xyp.gtnrlib.client.animation.ToolGrip;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.core.item.tool.ItemDrill;
import ic2.core.item.tool.ItemElectricToolChainsaw;
import ic2.core.item.tool.ItemElectricToolHoe;
import ic2.core.item.tool.ItemNanoSaber;
import ic2.core.item.tool.ItemToolWrench;

@SideOnly(Side.CLIENT)
public final class IndustrialCraftToolGrips {

    public static void register() {
        ToolGrip.registerFamily(ItemDrill.class, stack -> ToolGrip.PICKAXE);
        ToolGrip.registerFamily(ItemElectricToolChainsaw.class, stack -> ToolGrip.AXE);
        ToolGrip.registerFamily(ItemElectricToolHoe.class, stack -> ToolGrip.HOE);
        ToolGrip.registerFamily(ItemNanoSaber.class, stack -> ToolGrip.SWORD);
        ToolGrip.registerFamily(ItemToolWrench.class, stack -> ToolGrip.HANDHELD);
    }

    private IndustrialCraftToolGrips() {}
}
