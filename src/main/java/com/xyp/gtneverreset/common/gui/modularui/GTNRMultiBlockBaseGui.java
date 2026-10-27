// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtneverreset.common.gui.modularui;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.Widget;
import com.xyp.gtneverreset.GTNeverReset;

import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public class GTNRMultiBlockBaseGui<T extends MTEExtendedPowerMultiBlockBase<?>> extends MTEMultiBlockBaseGui<T> {

    private static final String THEME_ID = GTNeverReset.MODID + ":multiblock";

    public GTNRMultiBlockBaseGui(T multiblock) {
        super(multiblock);
    }

    @Override
    protected ModularPanel getBasePanel(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        uiSettings.useTheme(THEME_ID);
        return super.getBasePanel(guiData, syncManager, uiSettings);
    }

    @Override
    protected Widget<? extends Widget<?>> makeLogoWidget(PanelSyncManager syncManager, ModularPanel parent) {
        return GTGuiTextures.PICTURE_GODFORGE_LOGO.asWidget()
            .size(18)
            .marginTop(4);
    }
}
