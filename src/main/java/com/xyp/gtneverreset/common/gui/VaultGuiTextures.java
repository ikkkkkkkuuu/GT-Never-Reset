// SPDX-License-Identifier: MIT
// Adapted from GT-Not-Good's GTNGGuiTextures; see the packaged port notices for texture provenance.
package com.xyp.gtneverreset.common.gui;

import com.cleanroommc.modularui.drawable.UITexture;
import com.xyp.gtneverreset.GTNeverReset;

public final class VaultGuiTextures {

    public static final UITexture BACKGROUND = UITexture.builder()
        .location(GTNeverReset.MODID, "gui/modernity/vault_background")
        .imageSize(16, 16)
        .adaptable(2, 2, 2, 4)
        .build();
    public static final UITexture PANEL_BORDER = UITexture.builder()
        .location(GTNeverReset.MODID, "gui/modernity/vault_panel_border")
        .imageSize(16, 16)
        .adaptable(6)
        .build();
    public static final UITexture ITEM_SLOT = UITexture.builder()
        .location(GTNeverReset.MODID, "gui/modernity/vault_item_slot")
        .imageSize(18, 18)
        .adaptable(1)
        .build();
    public static final UITexture BUTTON = UITexture.builder()
        .location(GTNeverReset.MODID, "gui/modernity/button")
        .imageSize(20, 20)
        .adaptable(3)
        .build();
    public static final UITexture BUTTON_HOVER = UITexture.builder()
        .location(GTNeverReset.MODID, "gui/modernity/button_hover")
        .imageSize(20, 20)
        .subAreaXYWH(0, 1, 20, 19)
        .adaptable(3)
        .build();
    public static final UITexture BUTTON_PRESSED = UITexture.builder()
        .location(GTNeverReset.MODID, "gui/modernity/button_pressed")
        .imageSize(20, 20)
        .adaptable(3)
        .build();
    public static final UITexture BUTTON_DISABLED = UITexture.builder()
        .location(GTNeverReset.MODID, "gui/modernity/button_disabled")
        .imageSize(20, 20)
        .subAreaXYWH(0, 2, 20, 18)
        .adaptable(3)
        .build();
    public static final UITexture POWER_SWITCH_ON = UITexture
        .fullImage(GTNeverReset.MODID, "gui/overlay_button/power_switch_on");
    public static final UITexture POWER_SWITCH_DISABLED = UITexture
        .fullImage(GTNeverReset.MODID, "gui/overlay_button/power_switch_disabled");
    public static final UITexture STRUCTURE_CHECK_ON = UITexture
        .fullImage(GTNeverReset.MODID, "gui/overlay_button/structure_check_on");
    public static final UITexture STRUCTURE_CHECK_OFF = UITexture
        .fullImage(GTNeverReset.MODID, "gui/overlay_button/structure_check_off");

    private VaultGuiTextures() {}
}
