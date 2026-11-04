package com.xyp.gtneverreset.compat.tconstruct;

import com.xyp.gtnrlib.client.animation.ToolGrip;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import tconstruct.items.tools.Battleaxe;
import tconstruct.items.tools.Chisel;
import tconstruct.items.tools.Excavator;
import tconstruct.items.tools.FryingPan;
import tconstruct.items.tools.Hammer;
import tconstruct.items.tools.Hatchet;
import tconstruct.items.tools.LumberAxe;
import tconstruct.items.tools.Mattock;
import tconstruct.items.tools.Pickaxe;
import tconstruct.items.tools.Scythe;
import tconstruct.items.tools.Shovel;
import tconstruct.library.tools.HarvestTool;
import tconstruct.library.tools.Weapon;

@SideOnly(Side.CLIENT)
public final class TinkersToolGrips {

    public static void register() {
        ToolGrip.registerFamily(Weapon.class, stack -> ToolGrip.SWORD);
        ToolGrip.registerFamily(Scythe.class, stack -> ToolGrip.HOE);
        ToolGrip.registerFamily(FryingPan.class, stack -> ToolGrip.HANDHELD);
        ToolGrip.registerFamily(Chisel.class, stack -> ToolGrip.HANDHELD);
        // Broken Tinkers' tools stop advertising harvest classes but keep their physical shape.
        ToolGrip.registerFamily(HarvestTool.class, stack -> {
            var item = stack.getItem();
            if (item instanceof Pickaxe || item instanceof Hammer) return ToolGrip.PICKAXE;
            if (item instanceof Hatchet || item instanceof LumberAxe
                || item instanceof Mattock
                || item instanceof Battleaxe) return ToolGrip.AXE;
            if (item instanceof Shovel || item instanceof Excavator) return ToolGrip.SHOVEL;
            return ToolGrip.HANDHELD;
        });
    }

    private TinkersToolGrips() {}
}
