// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtneverreset.common.gui.modularui;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.Arrays;
import java.util.List;

import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.gtnewhorizons.modularui.common.fluid.FluidStackTank;
import com.xyp.gtneverreset.common.machine.basicMachine.ModernFluidMachine;

import gregtech.api.metatileentity.BaseTileEntity;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.singleblock.base.MTEBasicMachineBaseGui;
import gregtech.common.gui.modularui.util.MachineModularSlot;
import tectech.thing.metaTileEntity.pipe.MTEPipeData;
import tectech.thing.metaTileEntity.pipe.MTEPipeLaser;

public final class ModernFluidMachineGui extends MTEBasicMachineBaseGui<ModernFluidMachine> {

    private static final int SLOT_COLUMNS = 3;
    private static final int FLUID_ROW_GAP = 4;

    public ModernFluidMachineGui(ModernFluidMachine machine) {
        super(machine, machine.getModernUIProperties());
    }

    @Override
    protected int getBasePanelHeight() {
        int inputRows = rows(properties.maxItemInputs) + rows(machine.getInputTanks().length);
        int outputRows = rows(properties.maxItemOutputs) + rows(machine.getOutputTanks().length);
        return 166 + Math.max(0, Math.max(inputRows, outputRows) - 2) * SLOT_SIZE;
    }

    @Override
    protected Flow createItemRecipeArea(ModularPanel panel, PanelSyncManager syncManager) {
        return Flow.row()
            .coverChildren()
            .horizontalCenter()
            .verticalCenter()
            .childPadding((2 * SLOT_SIZE - properties.progressBarWidthMUI2) / 2)
            .mainAxisAlignment(Alignment.MainAxis.CENTER)
            .child(createRecipeSlots(false))
            .child(createProgressBar(panel, syncManager))
            .child(createRecipeSlots(true));
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return Flow.row()
            .coverChildren()
            .verticalCenter()
            .leftRel(0)
            .child(
                createAutoOutputButton(
                    syncManager,
                    "fluidAutoOutput",
                    GTGuiTextures.OVERLAY_BUTTON_AUTOOUTPUT_FLUID,
                    BaseTileEntity.FLUID_TRANSFER_TOOLTIP))
            .child(
                createAutoOutputButton(
                    syncManager,
                    "itemAutoOutput",
                    GTGuiTextures.OVERLAY_BUTTON_AUTOOUTPUT_ITEM,
                    BaseTileEntity.ITEM_TRANSFER_TOOLTIP));
    }

    @Override
    protected Flow createBottomRightCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return Flow.row()
            .mainAxisAlignment(Alignment.MainAxis.END)
            .reverseLayout(true)
            .coverChildren()
            .verticalCenter()
            .rightRel(0)
            .childIf(doesAddGregTechLogo(), this::makeLogoWidget)
            .childIf(doesAddCircuitSlot(), () -> createCircuitSlot(syncManager))
            .childIf(doesAddSpecialSlot(), this::createSpecialSlot);
    }

    private ToggleButton createAutoOutputButton(PanelSyncManager syncManager, String syncKey, UITexture overlay,
        String tooltipKey) {
        ToggleButton[] button = new ToggleButton[1];
        IPanelHandler selector = syncManager.syncedPanel(
            "modernOutputSide_" + syncKey,
            true,
            (panel, handler) -> createSideSelector(button[0], syncKey));
        button[0] = new ToggleButton() {

            @Override
            public Result onMousePressed(int mouseButton) {
                if (Interactable.hasShiftDown()) {
                    selector.openPanel();
                    return Result.SUCCESS;
                }
                return super.onMousePressed(mouseButton);
            }
        }.value(syncManager.findSyncHandler(syncKey, BooleanSyncValue.class))
            .overlay(overlay)
            .tooltipShowUpTimer(TOOLTIP_DELAY)
            .addTooltipLine(StatCollector.translateToLocal(tooltipKey));
        return button[0];
    }

    private ModularPanel createSideSelector(ToggleButton parent, String syncKey) {
        ModularPanel panel = new ModularPanel("modernOutputSide_" + syncKey) {

            @Override
            public boolean isDraggable() {
                return false;
            }
        }.relative(parent)
            .background(IDrawable.EMPTY)
            .coverChildren();
        List<IWidget> buttons = Arrays.asList(
            null,
            createSideButton(panel, ForgeDirection.UP, GTGuiTextures.OVERLAY_BUTTON_SIDE_SELECTION_UP),
            null,
            createSideButton(
                panel,
                machine.mMainFacing.getRotation(ForgeDirection.UP),
                GTGuiTextures.OVERLAY_BUTTON_SIDE_SELECTION_LEFT),
            null,
            createSideButton(
                panel,
                machine.mMainFacing.getRotation(ForgeDirection.DOWN),
                GTGuiTextures.OVERLAY_BUTTON_SIDE_SELECTION_RIGHT),
            null,
            createSideButton(panel, ForgeDirection.DOWN, GTGuiTextures.OVERLAY_BUTTON_SIDE_SELECTION_DOWN),
            createSideButton(
                panel,
                machine.mMainFacing.getOpposite(),
                GTGuiTextures.OVERLAY_BUTTON_SIDE_SELECTION_BACK));
        return panel.child(
            new Grid().coverChildren()
                .gridOf(3, buttons));
    }

    private IWidget createSideButton(ModularPanel panel, ForgeDirection direction, IDrawable overlay) {
        InteractionSyncHandler interaction = new InteractionSyncHandler().setOnMousePressed(mouseData -> {
            if (!mouseData.isClient()) {
                if (!baseMetaTileEntity.isValidFacing(direction)) return;
                baseMetaTileEntity.setFrontFacing(direction);
                for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                    var neighbor = baseMetaTileEntity.getIGregTechTileEntityAtSide(side);
                    if (neighbor == null) continue;
                    if (neighbor.getMetaTileEntity() instanceof MTEPipeLaser pipe) pipe.updateNetwork(true);
                    if (neighbor.getMetaTileEntity() instanceof MTEPipeData pipe) pipe.updateNetwork(true);
                }
            }
            panel.closeIfOpen();
        });
        return new ButtonWidget<>().syncHandler(interaction)
            .overlay(overlay);
    }

    private ParentWidget<?> createRecipeSlots(boolean output) {
        int itemSlots = output ? properties.maxItemOutputs : properties.maxItemInputs;
        FluidStackTank[] tanks = output ? machine.getOutputTanks() : machine.getInputTanks();
        int fluidY = rows(itemSlots) * SLOT_SIZE + (itemSlots > 0 && tanks.length > 0 ? FLUID_ROW_GAP : 0);
        ParentWidget<?> area = new ParentWidget<>()
            .size(SLOT_COLUMNS * SLOT_SIZE, fluidY + rows(tanks.length) * SLOT_SIZE);

        int itemColumns = Math.min(SLOT_COLUMNS, itemSlots);
        for (int i = 0; i < itemSlots; i++) {
            int slotIndex = (output ? machine.getOutputSlot() : machine.getInputSlot()) + i;
            ModularSlot slot = new MachineModularSlot(machine.inventoryHandler, slotIndex, baseMetaTileEntity)
                .slotGroup("item_inv");
            if (output) slot.accessibility(false, true);
            area.child(
                new ItemSlot().slot(slot)
                    .backgroundOverlay(slotOverlayFunction.apply(i, false, output, false))
                    .pos(slotX(i, itemColumns, output), i / itemColumns * SLOT_SIZE));
        }

        int fluidColumns = Math.min(SLOT_COLUMNS, tanks.length);
        for (int i = 0; i < tanks.length; i++) {
            FluidSlotSyncHandler syncHandler = new FluidSlotSyncHandler(tanks[i]) {

                @Override
                protected void onValueChanged() {
                    super.onValueChanged();
                    if (!getSyncManager().isClient()) baseMetaTileEntity.markInventoryBeenModified();
                }
            }.canFillSlot(!output);
            area.child(
                new FluidSlot().syncHandler(syncHandler)
                    .backgroundOverlay(slotOverlayFunction.apply(i, true, output, false))
                    .pos(slotX(i, fluidColumns, output), fluidY + i / fluidColumns * SLOT_SIZE));
        }
        return area;
    }

    private static int slotX(int index, int columns, boolean output) {
        return (index % columns + (output ? 0 : SLOT_COLUMNS - columns)) * SLOT_SIZE;
    }

    private static int rows(int slots) {
        return (slots + SLOT_COLUMNS - 1) / SLOT_COLUMNS;
    }
}
