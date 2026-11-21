package com.xyp.gtneverreset.common.gui.modularui.multiblock;

import com.xyp.gtneverreset.common.machine.multiMachineBase.CrossRecipeMultiblockBase;

import gregtech.api.modularui2.GTGuiTextures;

public final class CrossRecipeMultiblockGui extends GTNRMultiBlockBaseGui<CrossRecipeMultiblockBase<?>> {

    public CrossRecipeMultiblockGui(CrossRecipeMultiblockBase<?> multiblock) {
        super(multiblock);
        for (int mode = 0; mode < multiblock.getAvailableRecipeMaps()
            .size(); mode++) {
            withMachineModeIcons(GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_DEFAULT);
        }
    }
}
