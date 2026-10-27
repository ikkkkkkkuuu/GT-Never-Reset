// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from ABKQPO/GT-Not-Leisure, commit 6cbc6927af4f44c445ea7a879796b4764b00988d.
// Adapted to GT Never Reset machine registries and GUI integration.
package com.xyp.gtneverreset.common.gui.modularui;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.common.machine.multiblock.QuantumComputer;

import gregtech.api.util.GTUtility;

public class QuantumComputerGui extends GTNRMultiBlockBaseGui<QuantumComputer> {

    private static final String WIDTH_SYNC_KEY = "quantumComputerWidth";
    private static final String HEIGHT_SYNC_KEY = "quantumComputerHeight";
    private static final String DEPTH_SYNC_KEY = "quantumComputerDepth";
    private static final String MAXIMUM_PARALLEL_SYNC_KEY = "quantumComputerMaximumParallel";
    private static final String USED_PARALLEL_SYNC_KEY = "quantumComputerUsedParallel";
    private static final String MAXIMUM_STORAGE_SYNC_KEY = "quantumComputerMaximumStorage";
    private static final String USED_STORAGE_SYNC_KEY = "quantumComputerUsedStorage";
    private static final String CUSTOM_NAME_SYNC_KEY = "quantumComputerCustomName";
    private static final String AE_ACTIVE_SYNC_KEY = "quantumComputerAeActive";

    public QuantumComputerGui(QuantumComputer multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue(AE_ACTIVE_SYNC_KEY, new BooleanSyncValue(multiblock::isActive));
        syncManager
            .syncValue(WIDTH_SYNC_KEY, new IntSyncValue(multiblock::getWidthForGui, multiblock::setWidthFromGui));
        syncManager
            .syncValue(HEIGHT_SYNC_KEY, new IntSyncValue(multiblock::getHeightForGui, multiblock::setHeightFromGui));
        syncManager
            .syncValue(DEPTH_SYNC_KEY, new IntSyncValue(multiblock::getDepthForGui, multiblock::setDepthFromGui));
        syncManager.syncValue(
            MAXIMUM_PARALLEL_SYNC_KEY,
            new IntSyncValue(multiblock::getMaximumParallelForGui, multiblock::setMaximumParallelFromGui));
        syncManager.syncValue(
            USED_PARALLEL_SYNC_KEY,
            new IntSyncValue(multiblock::getUsedParallelForGui, multiblock::setUsedParallelFromGui));
        syncManager.syncValue(
            MAXIMUM_STORAGE_SYNC_KEY,
            new LongSyncValue(multiblock::getMaximumStorageForGui, multiblock::setMaximumStorageFromGui));
        syncManager.syncValue(
            USED_STORAGE_SYNC_KEY,
            new LongSyncValue(multiblock::getUsedStorageForGui, multiblock::setUsedStorageFromGui));
        syncManager.syncValue(
            CUSTOM_NAME_SYNC_KEY,
            new StringSyncValue(multiblock::getCustomNameForGui, multiblock::setCustomName).allowC2S());
    }

    @Override
    protected int getTerminalRowHeight() {
        return 85;
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        BooleanSyncValue activeSyncer = syncManager.findSyncHandler(AE_ACTIVE_SYNC_KEY, BooleanSyncValue.class);
        IntSyncValue widthSyncer = syncManager.findSyncHandler(WIDTH_SYNC_KEY, IntSyncValue.class);
        IntSyncValue heightSyncer = syncManager.findSyncHandler(HEIGHT_SYNC_KEY, IntSyncValue.class);
        IntSyncValue depthSyncer = syncManager.findSyncHandler(DEPTH_SYNC_KEY, IntSyncValue.class);
        IntSyncValue maximumParallelSyncer = syncManager.findSyncHandler(MAXIMUM_PARALLEL_SYNC_KEY, IntSyncValue.class);
        IntSyncValue usedParallelSyncer = syncManager.findSyncHandler(USED_PARALLEL_SYNC_KEY, IntSyncValue.class);
        LongSyncValue maximumStorageSyncer = syncManager.findSyncHandler(MAXIMUM_STORAGE_SYNC_KEY, LongSyncValue.class);
        LongSyncValue usedStorageSyncer = syncManager.findSyncHandler(USED_STORAGE_SYNC_KEY, LongSyncValue.class);

        return new ListWidget<>().fullWidth()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(
                IKey.dynamic(
                    () -> StatCollector
                        .translateToLocal(activeSyncer.getBoolValue() ? activeLabelKey() : inactiveLabelKey()))
                    .asWidget()
                    .color(Color.WHITE.main)
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted(
                        // #tr gtnr.compact.machine.quantum_computer.info.0
                        // # Multiblock size: %sx%sx%s
                        // # zh_CN 结构大小：%sx%sx%s
                        "gtnr.compact.machine.quantum_computer.info.0",
                        widthSyncer.getIntValue(),
                        heightSyncer.getIntValue(),
                        depthSyncer.getIntValue()))
                    .asWidget()
                    .textAlign(Alignment.CenterLeft)
                    .color(Color.WHITE.main)
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted(
                        // #tr gtnr.compact.machine.quantum_computer.info.1
                        // # Co-processors: %s total, %s used
                        // # zh_CN 合成协处理器：共 %s，已用 %s
                        "gtnr.compact.machine.quantum_computer.info.1",
                        GTUtility.formatShortenedLong(maximumParallelSyncer.getIntValue()),
                        GTUtility.formatShortenedLong(usedParallelSyncer.getIntValue())))
                    .asWidget()
                    .textAlign(Alignment.CenterLeft)
                    .color(Color.WHITE.main)
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted(
                        // #tr gtnr.compact.machine.quantum_computer.info.2
                        // # Storage: %s total, %s used
                        // # zh_CN 合成存储：共 %s，已用 %s
                        "gtnr.compact.machine.quantum_computer.info.2",
                        GTUtility.formatShortenedLong(maximumStorageSyncer.getLongValue()),
                        GTUtility.formatShortenedLong(usedStorageSyncer.getLongValue())))
                    .asWidget()
                    .textAlign(Alignment.CenterLeft)
                    .color(Color.WHITE.main)
                    .fullWidth());
    }

    @Override
    protected Flow createPanelGap(ModularPanel parent, PanelSyncManager syncManager) {
        return Flow.row()
            .fullWidth()
            .height(getTextBoxToInventoryGap())
            .paddingLeft(4)
            .paddingRight(25)
            .child(createCustomNameField(syncManager));
    }

    /** Returns the label for a formed and connected AE CPU. */
    private static String activeLabelKey() {
        // #tr gtnr.compact.quantum.active
        // # AE network connected
        // # zh_CN 已连接AE网络
        return "gtnr.compact.quantum.active";
    }

    /** Returns the label shown before structure formation or network connection. */
    private static String inactiveLabelKey() {
        // #tr gtnr.compact.quantum.inactive
        // # Waiting for AE network
        // # zh_CN 等待AE网络连接
        return "gtnr.compact.quantum.inactive";
    }

    private IWidget createCustomNameField(PanelSyncManager syncManager) {
        StringSyncValue customNameSyncer = syncManager.findSyncHandler(CUSTOM_NAME_SYNC_KEY, StringSyncValue.class);
        return new TextFieldWidget().value(customNameSyncer)
            .hintText(
                multiblock.getMachineCraftingIcon()
                    .getDisplayName())
            .setTextAlignment(Alignment.Center)
            .setTextColor(Color.WHITE.main)
            .tooltipBuilder(
                tooltip -> tooltip
                    // #tr gtnr.compact.machine.quantum_computer.info.3
                    // # Rename this Quantum Computer
                    // # zh_CN 重命名量子计算机
                    .addLine(StatCollector.translateToLocal("gtnr.compact.machine.quantum_computer.info.3")))
            .size(162, 18);
    }

}
