package com.xyp.gtneverreset.common.gui.modularui.multiblock;

import com.xyp.gtneverreset.common.machine.multiMachineBase.CrossRecipeMultiblockBase;

import gregtech.api.modularui2.GTGuiTextures;

public class CrossRecipeMultiblockGui<T extends CrossRecipeMultiblockBase<?>> extends GTNRMultiBlockBaseGui<T> {

    public CrossRecipeMultiblockGui(T multiblock) {
        super(multiblock);
        for (int mode = 0; mode < multiblock.getAvailableRecipeMaps()
            .size(); mode++) {
            withMachineModeIcons(GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_DEFAULT);
        }
    }
}
