package com.xyp.gtneverreset.common.gui.modularui.hatch;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.serialization.ByteBufAdapters;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.BottledChaosHatchControls;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.ChaosTextLayout;
import com.xyp.gtneverreset.common.machine.hatch.ChaosCredentialHatch;
import com.xyp.gtneverreset.common.machine.multiMachineBase.BottleInChaosMachineBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;

public final class ChaosCredentialHatchGui {

    private ChaosCredentialHatchGui() {}

    public static ModularPanel build(ChaosCredentialHatch hatch, PosGuiData guiData, PanelSyncManager syncManager,
        UISettings uiSettings) {
        GenericSyncValue<NBTTagCompound, ?> information = GenericSyncValue.builder(NBTTagCompound.class)
            .getter(() -> hatch.getInformation(false))
            .adapter(ByteBufAdapters.NBT)
            .copy(tag -> (NBTTagCompound) tag.copy())
            .build();
        syncManager.syncValue("bottledChaosHatchInformation", information);
        ModularPanel panel = GTGuis.mteTemplatePanelBuilder(hatch, guiData, syncManager, uiSettings)
            .build();
        List<ButtonWidget<?>> controls = BottledChaosHatchControls.create(
            panel,
            syncManager,
            "bottledChaosHatch",
            () -> hatch,
            () -> information.getValue() != null && information.getValue()
                .getBoolean("enabled"),
            null,
            null,
            BottledChaosHatchControls.Action.ROUTES,
            BottledChaosHatchControls.Action.EXECUTION,
            BottledChaosHatchControls.Action.LIMIT,
            BottledChaosHatchControls.Action.EXPORT,
            BottledChaosHatchControls.Action.CLEAR);
        for (int i = 0; i < controls.size(); i++) panel.child(
            controls.get(i)
                .pos(26 + 26 * i, 62));
        var patterns = syncManager.syncedPanel("chaosPatterns", true, (sync, handler) -> {
            sync.registerSlotGroup(
                new com.cleanroommc.modularui.widgets.slot.SlotGroup("chaosPatterns", 8).setAllowSorting(false));
            var popup = GTGuis.createPopUpPanel("chaosPatterns")
                .relative(panel)
                .leftRel(0.5f)
                .topRel(0.5f)
                .size(176, 132);
            popup.child(
                // #tr gtnr.chaos.patterns.title
                // # AE custom chain (left to right)
                // # zh_CN AE 自定义链（从左到右）
                IKey.lang("gtnr.chaos.patterns.title")
                    .asWidget()
                    .pos(8, 18)
                    .size(160, 16));
            for (int index = 0; index < 32; index++) {
                popup.child(
                    new com.cleanroommc.modularui.widgets.slot.ItemSlot().pos(16 + index % 8 * 18, 38 + index / 8 * 18)
                        .slot(
                            new com.cleanroommc.modularui.widgets.slot.ModularSlot(hatch.inventoryHandler, index + 1)
                                .slotGroup("chaosPatterns")
                                .filter(
                                    stack -> stack != null && stack
                                        .getItem() instanceof appeng.api.implementations.ICraftingPatternItem)));
            }
            return popup.child(
                // #tr gtnr.chaos.patterns.help
                // # Remove all patterns to use presets
                // # zh_CN 清空样板后使用预设产线
                IKey.lang("gtnr.chaos.patterns.help")
                    .asWidget()
                    .pos(8, 112)
                    .size(160, 14));
        });
        panel.child(
            new ButtonWidget<>().name("chaos_patterns_button")
                .pos(64, 34)
                .size(48, 18)
                .overlay(IKey.str("AE"))
                .onMousePressed(button -> {
                    if (button != 0) return false;
                    if (patterns.isPanelOpen()) patterns.closePanel();
                    else patterns.openPanel();
                    return true;
                }));
        return panel.child(
            IKey.dynamic(() -> getFittedHatchDisplayText(information.getValue()))
                .asWidget()
                .color(Color.WHITE.main)
                .textAlign(Alignment.Center)
                .background(GTGuiTextures.BACKGROUND_TEXT_FIELD)
                .tooltipBuilder(
                    tooltip -> tooltip.addLine(
                        information.getValue() != null && information.getValue()
                            .getBoolean("connected")
                                ? BottleInChaosMachineBase.getCredentialHatchTitle(information.getValue())
                                : StatCollector
                                    .translateToLocal("gtnr.machine.BottleInChaosRecipeTokenHatch.info.disconnected")))
                .tooltipAutoUpdate(true)
                .pos(14, 8)
                .size(148, 18));
    }

    @SideOnly(Side.CLIENT)
    private static String getFittedHatchDisplayText(NBTTagCompound data) {
        return ChaosTextLayout.ellipsize(
            BottleInChaosMachineBase.getCredentialHatchTitle(data),
            140,
            TextRenderer.getFontRenderer()::getStringWidth);
    }

}
