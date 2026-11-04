package com.xyp.gtnrlib.compat.animation;

import com.xyp.gtnrlib.compat.gregtech.GregTechToolGrips;
import com.xyp.gtnrlib.compat.gregtech.GregTechWireCutterAnimation;
import com.xyp.gtnrlib.compat.gregtech.GregTechWrenchAnimation;
import com.xyp.gtnrlib.compat.ic2.IndustrialCraftToolGrips;
import com.xyp.gtnrlib.compat.tconstruct.TinkersToolGrips;
import com.xyp.gtnrlib.mod.ModList;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ToolAnimationCompat {

    private ToolAnimationCompat() {}

    /** An installed older GTNR can already own this exact hook or adapter. Avoid registering it twice. */
    public static boolean legacyClass(String path) {
        return ToolAnimationCompat.class.getClassLoader()
            .getResource("com/xyp/gtneverreset/" + path + ".class") != null;
    }

    @SideOnly(Side.CLIENT)
    public static void register() {
        if (ModList.GregTech.isModLoaded()) {
            if (!legacyClass("compat/gregtech/GregTechToolGrips")) GregTechToolGrips.register();
            if (!legacyClass("compat/gregtech/GregTechWrenchAnimation")) GregTechWrenchAnimation.register();
            if (!legacyClass("compat/gregtech/GregTechWireCutterAnimation")) GregTechWireCutterAnimation.register();
        }
        if (Loader.isModLoaded("IC2") && !legacyClass("compat/ic2/IndustrialCraftToolGrips"))
            IndustrialCraftToolGrips.register();
        if (Loader.isModLoaded("TConstruct") && !legacyClass("compat/tconstruct/TinkersToolGrips"))
            TinkersToolGrips.register();
    }
}
