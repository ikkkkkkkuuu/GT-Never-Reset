// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from ABKQPO/GT-Not-Leisure, commit 6cbc6927af4f44c445ea7a879796b4764b00988d.
// Modified for compact, fixed-maximum, energy-free GT Not Good and GT Never Reset machines.
package com.xyp.gtneverreset.common.gui.modularui;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DynamicLinkedSyncHandler;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.DynamicSyncedWidget;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.common.machine.multiblock.AssemblerMatrix;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.api.util.GTUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;

public class AssemblerMatrixGui extends GTNRMultiBlockBaseGui<AssemblerMatrix> {

    private static final String SHOW_PATTERN_SYNC_KEY = "showPattern";
    private static final String CUSTOM_NAME_SYNC_KEY = "customName";
    private static final String PATTERN_MULTIPLY_SYNC_KEY = "patternMultiply";
    private static final String PATTERN_OUTPUTS_SYNC_KEY = "patternOutputs";
    private static final String PATTERN_OUTPUTS_WIDGET_SYNC_KEY = "patternOutputsWidget";
    private static final String PATTERN_USED_SYNC_KEY = "patternUsed";

    public AssemblerMatrixGui(AssemblerMatrix multiblock) {
        super(multiblock);
        withMachineModeIcons(
            GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_PACKAGER,
            GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_UNPACKAGER,
            GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_DEFAULT);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue(
            PATTERN_USED_SYNC_KEY,
            new IntSyncValue(
                () -> multiblock.getInventory()
                    .size()));
        syncManager.syncValue(
            SHOW_PATTERN_SYNC_KEY,
            new BooleanSyncValue(multiblock::isShowPattern, multiblock::setShowPattern).allowC2S());
        syncManager.syncValue(
            CUSTOM_NAME_SYNC_KEY,
            new StringSyncValue(multiblock::getGuiCustomName, multiblock::setCustomName).allowC2S());
        syncManager.syncValue(
            PATTERN_MULTIPLY_SYNC_KEY,
            new IntSyncValue(multiblock::getPatternMultiply, multiblock::setPatternMultiply).allowC2S());
        GenericListSyncHandler<IAEItemStack> patternOutputsSyncer = new PatternOutputsSyncHandler(multiblock);
        syncManager.syncValue(PATTERN_OUTPUTS_SYNC_KEY, patternOutputsSyncer);
        syncManager.syncValue(
            PATTERN_OUTPUTS_WIDGET_SYNC_KEY,
            new DynamicLinkedSyncHandler<>(patternOutputsSyncer)
                .widgetProvider((panelSyncManager, syncValue) -> createPatternOutputsWidget(syncValue.getValue())));
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        IntSyncValue usedSyncer = syncManager.findSyncHandler(PATTERN_USED_SYNC_KEY, IntSyncValue.class);
        return super.createTerminalTextWidget(syncManager, parent)
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted(
                        // #tr gtnr.compact.matrix.pattern_usage
                        // # Patterns: %s/%s
                        // # zh_CN 样板：%s/%s
                        "gtnr.compact.matrix.pattern_usage",
                        usedSyncer.getIntValue(),
                        AssemblerMatrix.PATTERN_CAPACITY))
                    .asWidget()
                    .color(Color.WHITE.main)
                    .fullWidth())
            .child(createPatternOutputsWidget(syncManager));
    }

    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        Flow row = super.createLeftPanelGapRow(parent, syncManager);
        row.child(createShowPatternButton(syncManager))
            .child(createCustomNameField(syncManager));
        return row;
    }

    @Override
    protected IWidget createModeSwitchButton(PanelSyncManager syncManager) {
        IntSyncValue machineModeSyncer = syncManager.findSyncHandler("machineMode", IntSyncValue.class);
        return new CycleButtonWidget().size(18, 18)
            .syncHandler("machineMode")
            .length(machineModeIcons.size())
            .overlay(new DynamicDrawable(() -> getMachineModeIcon(machineModeSyncer.getIntValue())))
            .tooltipBuilder(this::createModeSwitchTooltip)
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    @Override
    protected ButtonWidget<?> createPowerPanelButton(PanelSyncManager syncManager, ModularPanel parent) {
        IPanelHandler powerPanel = syncManager.syncedPanel(
            "assemblerMatrixPowerPanel",
            true,
            (panelSyncManager, panelHandler) -> createPatternPowerPanel(parent, syncManager));
        return new ButtonWidget<>().size(18, 18)
            .overlay(GTGuiTextures.TT_OVERLAY_BUTTON_POWER_PANEL)
            .onMousePressed(mouseButton -> {
                if (powerPanel.isPanelOpen()) {
                    powerPanel.closePanel();
                } else {
                    powerPanel.openPanel();
                }
                return true;
            })
            // #tr gtnr.compact.machine.assembler_matrix.info.1
            // # Pattern Multipliers
            // # zh_CN 样板倍率
            .tooltipBuilder(tooltip -> tooltip.addLine(IKey.lang("gtnr.compact.machine.assembler_matrix.info.1")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    @Override
    protected boolean shouldDisplayVoidExcess() {
        return false;
    }

    @Override
    protected boolean shouldDisplayInputSeparation() {
        return false;
    }

    @Override
    protected boolean shouldDisplayBatchMode() {
        return false;
    }

    @Override
    protected boolean shouldDisplayRecipeLock() {
        return false;
    }

    private IWidget createShowPatternButton(PanelSyncManager syncManager) {
        BooleanSyncValue showPatternSyncer = syncManager.findSyncHandler(SHOW_PATTERN_SYNC_KEY, BooleanSyncValue.class);
        return new ButtonWidget<>().size(18, 18)
            .overlay(
                new DynamicDrawable(
                    () -> showPatternSyncer.getBoolValue() ? GTGuiTextures.OVERLAY_BUTTON_WHITELIST
                        : GTGuiTextures.OVERLAY_BUTTON_BLACKLIST))
            .syncHandler(
                new InteractionSyncHandler().setOnMousePressed(
                    mouseData -> {
                        if (!mouseData.isClient())
                            showPatternSyncer.setBoolValue(!showPatternSyncer.getBoolValue(), true, true);
                    }))
            .tooltipDynamic(
                tooltip -> tooltip.addLine(
                    IKey.dynamic(
                        () -> StatCollector
                            .translateToLocal(AssemblerMatrix.patternVisibilityKey(showPatternSyncer.getBoolValue())))))
            .tooltipAutoUpdate(true)
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    private IWidget createCustomNameField(PanelSyncManager syncManager) {
        StringSyncValue customNameSyncer = syncManager.findSyncHandler(CUSTOM_NAME_SYNC_KEY, StringSyncValue.class);
        return new TextFieldWidget().value(customNameSyncer)
            .hintText(
                multiblock.getMachineCraftingIcon()
                    .getDisplayName())
            .setTextAlignment(Alignment.Center)
            .setTextColor(Color.WHITE.main)
            // #tr gtnr.compact.machine.assembler_matrix.info.3
            // # Rename this Assembler Matrix
            // # zh_CN 重命名装配矩阵
            .tooltipBuilder(tooltip -> tooltip.addLine(IKey.lang("gtnr.compact.machine.assembler_matrix.info.3")))
            .tooltipShowUpTimer(TOOLTIP_DELAY)
            .size(126, 18);
    }

    private ModularPanel createPatternPowerPanel(ModularPanel parent, PanelSyncManager syncManager) {
        IntSyncValue patternMultiplySyncer = syncManager.findSyncHandler(PATTERN_MULTIPLY_SYNC_KEY, IntSyncValue.class);
        Dialog<?> panel = new Dialog<>("assemblerMatrixPowerPanel", null);
        panel.relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(100, 80)
            .widgetTheme(GTWidgetThemes.BACKGROUND_POPUP);
        panel.setDisablePanelsBelow(false)
            .setCloseOnOutOfBoundsClick(false)
            .setDraggable(true);

        panel.child(ButtonWidget.panelCloseButton());
        panel.child(
            IKey.str(
                // #tr gtnr.compact.machine.assembler_matrix.info.1
                // # Pattern Multipliers
                // # zh_CN 样板倍率
                EnumChatFormatting.UNDERLINE
                    + StatCollector.translateToLocal("gtnr.compact.machine.assembler_matrix.info.1"))
                .asWidget()
                .pos(0, 2)
                .size(100, 18)
                .textAlign(Alignment.Center));
        panel.child(
            // #tr gtnr.compact.machine.assembler_matrix.info.2
            // # Current Multiplier
            // # zh_CN 当前倍率
            IKey.lang("gtnr.compact.machine.assembler_matrix.info.2")
                .asWidget()
                .pos(0, 24)
                .size(100, 18)
                .textAlign(Alignment.Center));
        panel.child(
            new TextFieldWidget().value(patternMultiplySyncer)
                .numbersInt(1, Integer.MAX_VALUE)
                .scrollValues(1, 4, 64, 256)
                .setTextAlignment(Alignment.Center)
                .setTextColor(Color.WHITE.main)
                .tooltipBuilder(
                    tooltip -> tooltip.addLine(
                        IKey.dynamic(
                            () -> StatCollector
                                .translateToLocalFormatted("GT5U.gui.text.rangedvalue", 1, Integer.MAX_VALUE))))
                .tooltipShowUpTimer(TOOLTIP_DELAY)
                .pos(15, 40)
                .size(70, 18));
        return panel;
    }

    private IWidget createPatternOutputsWidget(PanelSyncManager syncManager) {
        DynamicLinkedSyncHandler<?> outputsWidgetSyncer = syncManager
            .findSyncHandler(PATTERN_OUTPUTS_WIDGET_SYNC_KEY, DynamicLinkedSyncHandler.class);
        return new DynamicSyncedWidget<>().syncHandler(outputsWidgetSyncer)
            .initialChild(createPatternOutputsWidget(Collections.emptyList()))
            .fullWidth();
    }

    private IWidget createPatternOutputsWidget(List<IAEItemStack> cachedOutputItems) {
        return new ListWidget<>().fullWidth()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .children(createPatternOutputRows(cachedOutputItems));
    }

    private List<IWidget> createPatternOutputRows(List<IAEItemStack> cachedOutputItems) {
        if (cachedOutputItems == null || cachedOutputItems.isEmpty()) {
            return Collections.emptyList();
        }

        Reference2LongMap<IAEItemStack> nameToAmount = new Reference2LongOpenHashMap<>();
        for (IAEItemStack item : cachedOutputItems) {
            if (item == null || item.getStackSize() <= 0) continue;
            nameToAmount.merge(item, item.getStackSize(), Long::sum);
        }

        List<Reference2LongMap.Entry<IAEItemStack>> sortedMap = new ObjectArrayList<>(
            nameToAmount.reference2LongEntrySet());
        sortedMap.sort(
            ((Comparator<Reference2LongMap.Entry<IAEItemStack>> & Serializable) (left, right) -> Long
                .compare(right.getLongValue(), left.getLongValue())));

        return sortedMap.stream()
            .map(this::createPatternOutputRow)
            .collect(Collectors.toList());
    }

    private IWidget createPatternOutputRow(Reference2LongMap.Entry<IAEItemStack> entry) {
        ItemStack stack = entry.getKey()
            .getItemStack()
            .copy();
        long itemCount = entry.getLongValue();
        String itemName = stack.getDisplayName();
        String itemAmountString = EnumChatFormatting.WHITE + " x "
            + EnumChatFormatting.GOLD
            + GTUtility.formatShortenedLong(itemCount)
            + EnumChatFormatting.WHITE
            + GTUtility.appendRate(false, itemCount, true, multiblock.mMaxProgresstime);
        String lineText = EnumChatFormatting.AQUA + GTUtility.truncateText(itemName, 40 - itemAmountString.length())
            + itemAmountString;
        String lineTooltip = EnumChatFormatting.AQUA + itemName
            + "\n"
            + GTUtility.appendRate(false, itemCount, false, multiblock.mMaxProgresstime);

        return Flow.row()
            .height(10)
            .fullWidth()
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(
                new ItemDisplayWidget().item(stack)
                    .displayAmount(false)
                    .size(8, 8))
            .child(
                new TextWidget<>(IKey.str(lineText)).textAlign(Alignment.CenterLeft)
                    .size(170, 10)
                    .marginLeft(2)
                    .tooltipBuilder(tooltip -> tooltip.addLine(lineTooltip)));
    }

    private static final class PatternOutputsSyncHandler extends GenericListSyncHandler<IAEItemStack> {

        private PatternOutputsSyncHandler(AssemblerMatrix machine) {
            super(
                machine::getCachedPatternOutputsForGui,
                machine::setCachedPatternOutputsFromGui,
                PatternOutputsSyncHandler::readStack,
                PatternOutputsSyncHandler::writeStack,
                PatternOutputsSyncHandler::stacksEqual,
                PatternOutputsSyncHandler::copyStack);
        }

        private static IAEItemStack readStack(PacketBuffer buffer) {
            try {
                return AEItemStack.loadItemStackFromPacket(buffer);
            } catch (IOException exception) {
                return AEItemStack.create(new ItemStack(Blocks.fire));
            }
        }

        private static void writeStack(PacketBuffer buffer, IAEItemStack stack) {
            try {
                stack.writeToPacket(buffer);
            } catch (IOException ignored) {}
        }

        private static boolean stacksEqual(IAEItemStack left, IAEItemStack right) {
            if (left == right) return true;
            if (left == null || right == null) return false;
            return left.equals(right) && left.getStackSize() == right.getStackSize();
        }

        private static IAEItemStack copyStack(IAEItemStack stack) {
            return stack == null ? AEItemStack.create(new ItemStack(Blocks.fire)) : stack.copy();
        }
    }
}
