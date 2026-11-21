package com.xyp.gtneverreset.common.gui.modularui.multiblock;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.common.machine.multiblock.NexusSatelliteFactory;

import gregtech.api.modularui2.GTGuiTextures;

public final class NexusSatelliteFactoryGui extends CrossRecipeMultiblockGui<NexusSatelliteFactory> {

    public NexusSatelliteFactoryGui(NexusSatelliteFactory machine) {
        super(machine);
    }

    @Override
    protected ButtonWidget<?> createPowerPanelButton(PanelSyncManager sync, ModularPanel parent) {
        var handle = sync.syncedPanel("satelliteVoltage", true, (manager, handler) -> {
            var voltage = new LongSyncValue(multiblock::getVoltage, multiblock::setVoltage).allowC2S();
            var formed = new BooleanSyncValue(() -> multiblock.mMachine);
            manager.syncValue("formed", formed);
            manager.syncValue("processingVoltage", voltage);
            return new ModularPanel("satelliteVoltage").relative(parent)
                .leftRel(1)
                .topRel(0)
                .size(148, 58)
                .child(
                    // #tr gtnr.light_hunter.voltage
                    // # Voltage per parallel
                    // # zh_CN 每份并行电压
                    IKey.lang("gtnr.light_hunter.voltage")
                        .alignment(Alignment.CENTER)
                        .asWidget()
                        .top(6)
                        .widthRel(1)
                        .height(14))
                .child(
                    new TextFieldWidget().name("satelliteVoltageInput")
                        .value(voltage)
                        .formatAsInteger(true)
                        .numbersLong(() -> NexusSatelliteFactory.MIN_VOLTAGE, () -> Long.MAX_VALUE)
                        .scrollValues(1, 64, 4, 16)
                        .setTextAlignment(Alignment.Center)
                        .size(130, 18)
                        .pos(9, 27)
                        .setEnabledIf(widget -> formed.getBoolValue()))
                .child(
                    GTGuiTextures.OVERLAY_BUTTON_CROSS.asWidget()
                        .size(18)
                        .pos(65, 27)
                        .setEnabledIf(widget -> !formed.getBoolValue()));
        });
        return new ButtonWidget<>().marginLeft(4)
            .overlay(GTGuiTextures.TT_OVERLAY_BUTTON_POWER_PANEL)
            .tooltip(t -> t.addLine(IKey.lang("GT5U.gui.button.power_panel")))
            .onMousePressed(d -> {
                if (handle.isPanelOpen()) handle.closePanel();
                else handle.openPanel();
                return true;
            });
    }
}
