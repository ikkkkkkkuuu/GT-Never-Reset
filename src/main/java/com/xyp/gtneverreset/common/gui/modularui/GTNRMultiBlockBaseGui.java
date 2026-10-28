package com.xyp.gtneverreset.common.gui.modularui;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
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

    @Override
    protected ToggleButton createMuffleButton() {
        return super.createMuffleButton().overlay(false, GTGuiTextures.GODFORGE_SOUND_ON)
            .overlay(true, GTGuiTextures.GODFORGE_SOUND_OFF);
    }

    @Override
    protected ToggleButton createPowerSwitchButton() {
        return super.createPowerSwitchButton().overlay(false, GTGuiTextures.TT_OVERLAY_BUTTON_POWER_SWITCH_DISABLED)
            .overlay(true, GTGuiTextures.TT_OVERLAY_BUTTON_POWER_SWITCH_ON);
    }

    @Override
    protected IWidget createStructureUpdateButton(PanelSyncManager syncManager) {
        return ((ToggleButton) super.createStructureUpdateButton(syncManager))
            .overlay(false, GTGuiTextures.TT_OVERLAY_BUTTON_STRUCTURE_CHECK_OFF)
            .overlay(true, GTGuiTextures.TT_OVERLAY_BUTTON_STRUCTURE_CHECK);
    }

    @Override
    protected ButtonWidget<?> createVoidExcessButton(PanelSyncManager syncManager) {
        return super.createVoidExcessButton(syncManager).overlay(new DynamicDrawable(() -> {
            return switch (multiblock.getVoidingMode()) {
                case VOID_NONE -> GTGuiTextures.TT_OVERLAY_BUTTON_VOIDING_OFF;
                case VOID_ITEM -> GTGuiTextures.TT_OVERLAY_BUTTON_VOIDING_ITEMS;
                case VOID_FLUID -> GTGuiTextures.TT_OVERLAY_BUTTON_VOIDING_FLUIDS;
                case VOID_ALL -> GTGuiTextures.TT_OVERLAY_BUTTON_VOIDING_BOTH;
            };
        }));
    }

    @Override
    protected ToggleButton createInputSeparationButton(PanelSyncManager syncManager) {
        return super.createInputSeparationButton(syncManager)
            .overlay(false, GTGuiTextures.TT_OVERLAY_BUTTON_INPUT_SEPARATION_OFF)
            .overlay(true, GTGuiTextures.TT_OVERLAY_BUTTON_INPUT_SEPARATION);
    }

    @Override
    protected ToggleButton createBatchModeButton(PanelSyncManager syncManager) {
        return super.createBatchModeButton(syncManager).overlay(false, GTGuiTextures.TT_OVERLAY_BUTTON_BATCH_MODE_OFF)
            .overlay(true, GTGuiTextures.TT_OVERLAY_BUTTON_BATCH_MODE);
    }

    @Override
    protected ToggleButton createLockToSingleRecipeButton(PanelSyncManager syncManager) {
        return super.createLockToSingleRecipeButton(syncManager)
            .overlay(false, GTGuiTextures.TT_OVERLAY_BUTTON_RECIPE_UNLOCKED)
            .overlay(true, GTGuiTextures.TT_OVERLAY_BUTTON_RECIPE_LOCKED);
    }

    @Override
    protected ButtonWidget<?> createPowerPanelButton(PanelSyncManager syncManager, ModularPanel parent) {
        return super.createPowerPanelButton(syncManager, parent).overlay(GTGuiTextures.TT_OVERLAY_BUTTON_POWER_PANEL);
    }
}
