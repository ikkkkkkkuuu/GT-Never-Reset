package com.xyp.gtneverreset.compat.gregtech;

import com.xyp.gtnrlib.client.animation.ToolGrip;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.items.MetaGeneratedTool;
import gregtech.common.tools.ToolButcheryKnife;
import gregtech.common.tools.ToolDrillLV;
import gregtech.common.tools.ToolKnife;
import gregtech.common.tools.ToolScoop;
import gregtech.common.tools.ToolTurbine;

@SideOnly(Side.CLIENT)
public final class GregTechToolGrips {

    public static void register() {
        ToolGrip.registerFamily(MetaGeneratedTool.class, stack -> {
            // The stats table includes discharged metadata. getToolStats would first update the stack's use state.
            var tool = (MetaGeneratedTool) stack.getItem();
            var stats = tool.mToolStats.get((short) stack.getItemDamage());
            if (stats == null || stats instanceof ToolTurbine) return null;
            if (stats instanceof ToolDrillLV) return ToolGrip.PICKAXE;
            if (stats.isChainsaw() || stats.isSaw()) return ToolGrip.AXE;
            if (stats instanceof ToolKnife || stats instanceof ToolButcheryKnife) return ToolGrip.SWORD;
            if (stats instanceof ToolScoop) return ToolGrip.SHOVEL;
            return ToolGrip.HANDHELD;
        });
    }

    private GregTechToolGrips() {}
}
