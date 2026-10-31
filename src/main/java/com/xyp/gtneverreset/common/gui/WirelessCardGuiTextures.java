package com.xyp.gtneverreset.common.gui;

import net.minecraft.util.ResourceLocation;

import com.cleanroommc.modularui.drawable.UITexture;
import com.xyp.gtnrlib.mod.ModList;

public final class WirelessCardGuiTextures {

    public static final UITexture BACKGROUND = UITexture.builder()
        .location(ModList.AE2.getResourceLocation("textures/guis/wireless_network_manager.png"))
        .imageSize(256, 256)
        .subAreaXYWH(0, 0, 106, 232)
        .adaptable(4)
        .build();
    public static final UITexture[] BUTTONS = { button(0), button(1), button(2) };
    public static final UITexture TOGGLE_BACKGROUND = toggle(255), TOGGLE_ON = toggle(129), TOGGLE_OFF = toggle(128);

    private WirelessCardGuiTextures() {}

    private static UITexture button(int state) {
        return UITexture.builder()
            .location(new ResourceLocation("minecraft", "textures/gui/widgets.png"))
            .imageSize(256, 256)
            .subAreaXYWH(0, 46 + state * 20, 200, 20)
            .adaptable(2)
            .build();
    }

    private static UITexture toggle(int icon) {
        return UITexture.builder()
            .location(ModList.AE2.getResourceLocation("textures/guis/states.png"))
            .imageSize(256, 256)
            .subAreaXYWH(icon % 16 * 16, icon / 16 * 16, 16, 16)
            .build();
    }
}
