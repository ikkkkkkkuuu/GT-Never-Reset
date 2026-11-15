package com.xyp.gtneverreset.common.gui.modularui.multiblock;

import net.minecraft.entity.player.EntityPlayer;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.xyp.gtneverreset.common.machine.multiblock.BottleInChaos;

import gregtech.api.modularui2.GTGuiTextures;

public final class BottleInChaosGui extends GTNRMultiBlockBaseGui<BottleInChaos> {

    public BottleInChaosGui(BottleInChaos machine) {
        super(machine);
    }

    @Override
    public ModularPanel build(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        ModularPanel panel = new ModularPanel("bottle_in_chaos").size(420, 334)
            .background(GTGuiTextures.BACKGROUND_STANDARD);
        panel.child(
            IKey.lang("gtnr.machine.bottle_in_chaos.name")
                .asWidget()
                .pos(10, 8));
        // #tr gtnr.chaos.gui.power
        // # Power
        // # zh_CN 运行开关
        panel.child(button("gtnr.chaos.gui.power", 336, 4, 74, () -> {
            if (multiblock.getBaseMetaTileEntity()
                .isAllowedToWork())
                multiblock.getBaseMetaTileEntity()
                    .disableWorking();
            else multiblock.getBaseMetaTileEntity()
                .enableWorking();
        }));
        for (int i = 0; i < BottleInChaos.GROUP_COUNT; i++) {
            final int group = i;
            StringSyncValue status = new StringSyncValue(() -> multiblock.groupText(group));
            sync.syncValue("chaos_group_" + i, status);
            StringSyncValue details = new StringSyncValue(() -> multiblock.groupDetails(group));
            sync.syncValue("chaos_details_" + i, details);
            panel.child(
                IKey.dynamic(status::getStringValue)
                    .asWidget()
                    .pos(10, 30 + i * 25)
                    .size(140, 23)
                    .tooltipDynamic(
                        tooltip -> {
                            for (String line : details.getStringValue()
                                .split("\n")) tooltip.addLine(line);
                        }));
            // #tr gtnr.chaos.gui.toggle
            // # I/O
            // # zh_CN 启停
            panel.child(button("gtnr.chaos.gui.toggle", 152, 30 + i * 25, 24, () -> multiblock.toggleGroup(group)));
            // #tr gtnr.chaos.gui.export
            // # Out
            // # zh_CN 导出
            panel.child(button("gtnr.chaos.gui.export", 178, 30 + i * 25, 24, () -> multiblock.exportGroup(group)));
        }
        // #tr gtnr.chaos.gui.modules
        // # Installed structure modules
        // # zh_CN 已安装结构模组
        panel.child(
            IKey.lang("gtnr.chaos.gui.modules")
                .asWidget()
                .pos(214, 28));
        sync.registerSlotGroup("chaos_modules", 2);
        for (int i = 0; i < BottleInChaos.MODULE_SLOTS; i++)
            addSlot(panel, i, 214 + i % 8 * 18, 42 + i / 8 * 18, "chaos_modules");
        // #tr gtnr.chaos.gui.workbench
        // # Credential | Module + machine + 8 casings
        // # zh_CN 凭证注入 | 空模组＋机器＋8 外壳
        panel.child(
            IKey.lang("gtnr.chaos.gui.workbench")
                .asWidget()
                .pos(214, 82));
        sync.registerSlotGroup("chaos_workbench", 1);
        for (int i = BottleInChaos.CREDENTIAL_INPUT; i <= BottleInChaos.MODULE_OUTPUT; i++)
            addSlot(panel, i, 214 + (i - BottleInChaos.CREDENTIAL_INPUT) * 27, 96, "chaos_workbench");
        StringSyncValue preset = new StringSyncValue(multiblock::presetName);
        sync.syncValue("chaos_preset", preset);
        panel.child(
            new ButtonWidget<>().overlay(IKey.dynamic(preset::getStringValue))
                .pos(214, 118)
                .size(190, 18)
                .syncHandler(
                    new InteractionSyncHandler()
                        .setOnMousePressed(mouse -> { if (!mouse.isClient()) multiblock.nextPreset(); })));
        // #tr gtnr.chaos.gui.preset
        // # Bind preset
        // # zh_CN 注入预设
        panel.child(button("gtnr.chaos.gui.preset", 214, 138, 60, () -> multiblock.bindCredential(false)));
        // #tr gtnr.chaos.gui.custom
        // # Bind AE
        // # zh_CN 注入 AE
        panel.child(button("gtnr.chaos.gui.custom", 279, 138, 60, () -> multiblock.bindCredential(true)));
        // #tr gtnr.chaos.gui.package
        // # Package
        // # zh_CN 封装
        panel.child(button("gtnr.chaos.gui.package", 344, 138, 60, multiblock::packageModule));
        // #tr gtnr.chaos.gui.patterns
        // # AE processing patterns, in order (not consumed)
        // # zh_CN AE 加工样板，按顺序执行（不消耗）
        panel.child(
            IKey.lang("gtnr.chaos.gui.patterns")
                .asWidget()
                .pos(214, 162));
        sync.registerSlotGroup("chaos_patterns", 4);
        for (int i = BottleInChaos.PATTERN_START; i < BottleInChaos.INVENTORY_SIZE; i++) {
            int slot = i - BottleInChaos.PATTERN_START;
            addSlot(panel, i, 214 + slot % 8 * 18, 176 + slot / 8 * 18, "chaos_patterns");
        }
        StringSyncValue feedback = new StringSyncValue(multiblock::feedback);
        sync.syncValue("chaos_feedback", feedback);
        panel.child(
            IKey.dynamic(feedback::getStringValue)
                .asWidget()
                .pos(10, 234)
                .size(192, 16));
        panel.child(
            SlotGroupWidget.playerInventory(true)
                .pos(129, 252));
        return panel;
    }

    private ButtonWidget<?> button(String key, int x, int y, int width, Runnable action) {
        return new ButtonWidget<>().overlay(IKey.lang(key))
            .pos(x, y)
            .size(width, 18)
            .syncHandler(
                new InteractionSyncHandler().setOnMousePressed(mouse -> { if (!mouse.isClient()) action.run(); }));
    }

    private void addSlot(ModularPanel panel, int index, int x, int y, String group) {
        ModularSlot slot = new ModularSlot(multiblock.workbench(), index) {

            @Override
            public boolean canTakeStack(EntityPlayer player) {
                return (index >= BottleInChaos.MODULE_SLOTS || !multiblock.hasWork()) && super.canTakeStack(player);
            }
        }.slotGroup(group);
        if (index == BottleInChaos.CREDENTIAL_OUTPUT || index == BottleInChaos.MODULE_OUTPUT) slot.canPut(false);
        panel.child(
            new ItemSlot().slot(slot)
                .pos(x, y));
    }
}
