package com.xyp.gtneverreset.common.gui.modularui.multiblock;

import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactory;

import gregtech.api.modularui2.GTGuiTextures;

public final class UniversalJointFactoryGui extends GTNRMultiBlockBaseGui<UniversalJointFactory> {

    public UniversalJointFactoryGui(UniversalJointFactory multiblock) {
        super(multiblock);
        for (int mode = 0; mode < UniversalJointFactory.RECIPE_MAPS.size(); mode++) {
            withMachineModeIcons(GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_DEFAULT);
        }
    }
}
