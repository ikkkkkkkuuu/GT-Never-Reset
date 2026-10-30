package com.xyp.gtneverreset.common.gui;

import java.util.function.Supplier;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.item.InvWrapper;
import com.cleanroommc.modularui.utils.serialization.ByteBufAdapters;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.xyp.gtneverreset.api.packaged.PackagedTarget;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityOverloadedProvider;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityOverloadedProvider.Lane;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuiThemes;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.common.gui.modularui.util.PatternSlot;

public final class OverloadedProviderGui {

    private static final int PAGE_SIZE = 5;

    private OverloadedProviderGui() {}

    public static ModularPanel build(TileEntityOverloadedProvider tile, PanelSyncManager sync, UISettings settings) {
        settings.useTheme(GTGuiThemes.STANDARD.getId());
        ModularPanel panel = ModularPanel.defaultPanel("overloaded_provider", 176, 184);
        InvWrapper inventory = new InvWrapper(tile);
        sync.registerSlotGroup("patterns", 9);
        // #tr gui.overloaded.title
        // # Overloaded Provider
        // # zh_CN 过载无线样板供应器
        panel.child(
            new ParentWidget<>().pos(0, -15)
                .size(148, 15)
                .widgetTheme(GTWidgetThemes.BACKGROUND_TITLE)
                .excludeAreaInRecipeViewer()
                .child(
                    IKey.dynamic(
                        () -> tile.hasCustomName() ? tile.getCustomName()
                            : StatCollector.translateToLocal("gui.overloaded.title"))
                        .asWidget()
                        .pos(5, 5)
                        .size(138, 9)
                        .widgetTheme(GTWidgetThemes.TEXT_TITLE)));
        for (int slot = 0; slot < TileEntityOverloadedProvider.PATTERNS; slot++) {
            panel.child(
                new PatternSlot().slot(
                    new ModularSlot(inventory, slot).slotGroup("patterns")
                        .filter(stack -> tile.isItemValidForSlot(0, stack)))
                    .pos(7 + slot % 9 * 18, 6 + slot / 9 * 18));
        }
        BooleanSyncValue optimize = new BooleanSyncValue(tile::allowsPatternOptimization);
        BooleanSyncValue visible = new BooleanSyncValue(tile::shouldDisplay);
        BooleanSyncValue auto = new BooleanSyncValue(tile::getAutoReturn);
        sync.syncValue("optimize", optimize);
        sync.syncValue("visible", visible);
        sync.syncValue("autoReturn", auto);
        panel.child(
            control(
                7,
                GTGuiTextures.OVERLAY_BUTTON_PATTERN_OPTIMIZE,
                () -> tr(
                    "GT5U.infodata.hatch.crafting_input_me.optimize_pattern."
                        + (optimize.getBoolValue() ? "enable" : "disabled"))).syncHandler(
                            new InteractionSyncHandler().setOnMousePressed(
                                mouse -> {
                                    if (!mouse.isClient() && tile.isUseableByPlayer(sync.getPlayer()))
                                        tile.togglePatternOptimization();
                                })));
        IDrawable visibilityIcon = (context, x, y, w, h,
            theme) -> (visible.getBoolValue() ? GTGuiTextures.OVERLAY_BUTTON_WHITELIST
                : GTGuiTextures.OVERLAY_BUTTON_BLACKLIST).draw(context, x, y, w, h, theme);
        panel.child(
            control(
                25,
                visibilityIcon,
                () -> tr(
                    "GT5U.infodata.hatch.crafting_input_me.show_pattern."
                        + (visible.getBoolValue() ? "enable" : "disabled"))).syncHandler(
                            new InteractionSyncHandler().setOnMousePressed(
                                mouse -> {
                                    if (!mouse.isClient() && tile.isUseableByPlayer(sync.getPlayer()))
                                        tile.toggleTerminalVisibility();
                                })));
        panel.child(
            control(43, GTGuiTextures.OVERLAY_BUTTON_X2, () -> tr("gui.tooltips.appliedenergistics2.DoublePatterns"))
                .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                    if (!mouse.isClient() && mouse.mouseButton <= 1 && tile.isUseableByPlayer(sync.getPlayer()))
                        tile.multiplyPatterns(mouse.shift, mouse.mouseButton == 1);
                })));
        var machines = sync.syncedPanel("machines", true, (manager, handler) -> machines(tile, manager, settings));
        // #tr gui.overloaded.machines
        // # Connected machines
        // # zh_CN 已连接机器
        panel.child(
            control(115, PatternProviderGuiTextures.icon("frequency_select"), () -> tr("gui.overloaded.machines"))
                .name("machines")
                .onMousePressed(button -> {
                    if (button != 0) return false;
                    machines.openPanel();
                    settings.getRecipeViewerSettings()
                        .disable();
                    panel.setEnabled(false);
                    return true;
                }));
        var returns = sync.syncedPanel("returns", true, (manager, handler) -> returns(tile, manager));
        IDrawable returnIcon = (context, x, y, w, h, theme) -> PatternProviderGuiTextures
            .icon(auto.getBoolValue() ? "auto_input_on" : "auto_input_off")
            .draw(context, x, y, w, h, theme);
        panel.child(
            control(
                133,
                returnIcon,
                () -> tr(auto.getBoolValue() ? "gui.overloaded.return_on" : "gui.overloaded.return_off"))
                    // #tr gui.overloaded.return_access
                    // # Left-click: toggle auto return. Right-click: view output buffer.
                    // # zh_CN 左键切换自动回收，右键查看产物缓存。
                    .tooltip(t -> t.addLine(IKey.lang("gui.overloaded.return_access")))
                    .onMousePressed(mouse -> {
                        if (mouse == 1) {
                            returns.openPanel();
                            return true;
                        }
                        return false;
                    })
                    .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                        if (!mouse.isClient() && mouse.mouseButton == 0 && tile.isUseableByPlayer(sync.getPlayer()))
                            tile.toggleAutoReturn();
                    })));
        panel.child(
            SlotGroupWidget.playerInventory(true)
                .pos(7, 100));
        return panel;
    }

    private static ButtonWidget<?> control(int x, IDrawable icon, Supplier<String> tooltip) {
        return new ButtonWidget<>().pos(x, 80)
            .size(18, 18)
            .overlay(icon)
            .tooltip(t -> t.addLine(IKey.dynamic(tooltip)));
    }

    private static ModularPanel returns(TileEntityOverloadedProvider tile, PanelSyncManager sync) {
        ModularPanel panel = GTGuis.createPopUpPanel("overloaded_returns")
            .size(176, 46);
        InvWrapper inventory = new InvWrapper(tile);
        sync.registerSlotGroup("returns", 9);
        // #tr gui.overloaded.return_buffer
        // # Return buffer (kept when ME storage is full)
        // # zh_CN 回收缓存（ME 存储已满时保留产物）
        panel.child(
            IKey.lang("gui.packaged.returns")
                .asWidget()
                .pos(7, 6));
        for (int slot = TileEntityOverloadedProvider.PATTERNS; slot < tile.getSizeInventory(); slot++) {
            panel.child(
                new ItemSlot().slot(
                    new ModularSlot(inventory, slot).slotGroup("returns")
                        .canPut(false))
                    .pos(7 + (slot - TileEntityOverloadedProvider.PATTERNS) * 18, 21)
                    .tooltip(t -> t.addLine(IKey.lang("gui.overloaded.return_buffer"))));
        }
        panel.child(ButtonWidget.panelCloseButton());
        return panel;
    }

    private static ModularPanel machines(TileEntityOverloadedProvider tile, PanelSyncManager sync,
        UISettings settings) {
        ModularPanel panel = ModularPanel.defaultPanel("overloaded_machines", 420, 234)
            .background(WirelessCardGuiTextures.BACKGROUND);
        int[] page = { 0 };
        TargetActions actions = new TargetActions(tile, sync);
        sync.syncValue("targetActions", actions);
        IntSyncValue count = new IntSyncValue(
            () -> tile.connections()
                .size());
        IntSyncValue pageValue = new IntSyncValue(() -> page[0]);
        BooleanSyncValue distribution = new BooleanSyncValue(tile::getRoundRobin);
        BooleanSyncValue autoReturn = new BooleanSyncValue(tile::getAutoReturn);
        sync.syncValue("count", count);
        sync.syncValue("page", pageValue);
        sync.syncValue("distribution", distribution);
        sync.syncValue("autoReturn", autoReturn);
        panel.child(
            IKey.dynamic(
                () -> tile.hasCustomName() ? tile.getCustomName()
                    : StatCollector.translateToLocal(tile.getInventoryName()))
                .asWidget()
                .pos(10, 8));
        // #tr gui.overloaded.count
        // # Machines: %s / %s
        // # zh_CN 机器：%s / %s 台
        panel.child(
            IKey.dynamic(
                () -> StatCollector.translateToLocalFormatted(
                    "gui.overloaded.count",
                    count.getIntValue(),
                    TileEntityOverloadedProvider.MAX_TARGETS))
                .asWidget()
                .pos(10, 29));
        // #tr gui.overloaded.round_robin
        // # Mode: round robin
        // # zh_CN 模式：轮询逐台
        // #tr gui.overloaded.fill_first
        // # Mode: fill first
        // # zh_CN 模式：优先填满
        panel.child(
            button(
                216,
                22,
                194,
                () -> tr(distribution.getBoolValue() ? "gui.overloaded.round_robin" : "gui.overloaded.fill_first"),
                sync,
                tile,
                tile::toggleDistribution));
        for (int row = 0; row < PAGE_SIZE; row++) {
            final int index = row;
            StringSyncValue title = new StringSyncValue(() -> {
                page[0] = Math.max(
                    0,
                    Math.min(
                        page[0],
                        Math.max(
                            0,
                            (tile.connections()
                                .size() - 1) / PAGE_SIZE)));
                int selected = page[0] * PAGE_SIZE + index;
                if (selected >= tile.connections()
                    .size()) {

                    return "";
                }
                Lane lane = tile.connections()
                    .get(selected);

                TileEntity target = lane.target.resolve(tile.getWorldObj());
                String name = target instanceof IInventory inv ? inv.getInventoryName() : "?";
                return name + "|"
                    + lane.target.x
                    + ", "
                    + lane.target.y
                    + ", "
                    + lane.target.z
                    + "|"
                    + lane.target.face;
            });
            StringSyncValue state = new StringSyncValue(() -> {
                int selected = page[0] * PAGE_SIZE + index;
                if (selected >= tile.connections()
                    .size()) return "";
                Lane lane = tile.connections()
                    .get(selected);
                int status = lane.target.resolve(tile.getWorldObj()) == null ? 0
                    : !tile.getProxy()
                        .isActive() ? 1 : lane.pendingKinds() > 0 ? 2 : 3;
                return status + ":" + lane.pendingKinds();
            });
            sync.syncValue("title" + row, title);
            sync.syncValue("state" + row, state);
            StringSyncValue identity = new StringSyncValue(() -> {
                int selected = page[0] * PAGE_SIZE + index;
                if (selected >= tile.connections()
                    .size()) return "";
                PackagedTarget target = tile.connections()
                    .get(selected).target;
                return target.x + "," + target.y + "," + target.z + "," + target.face;
            });
            sync.syncValue("identity" + row, identity);
            var icon = GenericSyncValue.builder(ItemStack.class)
                .getter(() -> {
                    int selected = page[0] * PAGE_SIZE + index;
                    if (selected >= tile.connections()
                        .size()) return null;
                    TileEntity target = tile.connections()
                        .get(selected).target.resolve(tile.getWorldObj());
                    if (target == null) return null;
                    if (target instanceof gregtech.api.interfaces.tileentity.IGregTechTileEntity gt
                        && gt.getMetaTileEntity() != null)
                        return gt.getMetaTileEntity()
                            .getStackForm(1);
                    return new ItemStack(target.getBlockType(), 1, target.getBlockMetadata());
                })
                .adapter(ByteBufAdapters.ITEM_STACK)
                .nullable()
                .build();
            sync.syncValue("icon" + row, icon);
            int y = 50 + row * 29;
            ItemDrawable item = new ItemDrawable();
            IDrawable drawable = (context, x, py, w, h, theme) -> item.setItem(icon.getValue())
                .draw(context, x, py, w, h, theme);
            panel.child(
                drawable.asWidget()
                    .pos(10, y)
                    .size(20, 20));
            panel.child(
                IKey.dynamic(() -> title(title.getValue()))
                    .asWidget()
                    .pos(36, y)
                    .size(252, 10));
            panel.child(
                IKey.dynamic(() -> status(state.getValue()))
                    .asWidget()
                    .pos(36, y + 12)
                    .size(252, 10));
            // #tr gui.overloaded.retry
            // # Retry
            // # zh_CN 重试
            ParentWidget<?> rowActions = new ParentWidget<>().name("row_actions_" + row)
                .pos(296, y)
                .size(114, 20)
                .setEnabledIf(
                    widget -> !identity.getValue()
                        .isEmpty());
            panel.child(rowActions);
            rowActions.child(
                new ButtonWidget<>().pos(0, 0)
                    .size(52, 20)
                    .background(WirelessCardGuiTextures.BUTTONS[1])
                    .hoverBackground(WirelessCardGuiTextures.BUTTONS[2])
                    .overlay(IKey.lang("gui.overloaded.retry"))
                    .onMousePressed(mouse -> {
                        if (mouse == 0) actions.request(0, identity.getValue());
                        return true;
                    })
                    .setEnabledIf(
                        widget -> !title.getValue()
                            .isEmpty()));
            // #tr gui.overloaded.unlink
            // # Unlink
            // # zh_CN 解绑
            rowActions.child(
                new ButtonWidget<>().name("unlink_" + row)
                    .pos(58, 0)
                    .size(56, 20)
                    .background(WirelessCardGuiTextures.BUTTONS[1])
                    .hoverBackground(WirelessCardGuiTextures.BUTTONS[2])
                    .overlay(IKey.lang("gui.overloaded.unlink"))
                    .onMousePressed(mouse -> {
                        if (mouse == 0) actions.request(1, identity.getValue());
                        return true;
                    })
                    .setEnabledIf(
                        widget -> !title.getValue()
                            .isEmpty())
                    // #tr gui.overloaded.unlink_pending
                    // # Deliver pending inputs before unlinking.
                    // # zh_CN 有待送材料时不能解绑，请先完成投料。
                    .tooltip(t -> t.addLine(IKey.lang("gui.overloaded.unlink_pending"))));
        }
        panel.child(button(10, 203, 28, () -> "<", sync, tile, () -> page[0] = Math.max(0, page[0] - 1)));
        panel.child(
            IKey.dynamic(
                () -> (pageValue.getIntValue() + 1) + " / "
                    + Math.max(1, (count.getIntValue() + PAGE_SIZE - 1) / PAGE_SIZE))
                .asWidget()
                .pos(45, 209));
        panel.child(
            button(
                104,
                203,
                28,
                () -> ">",
                sync,
                tile,
                () -> page[0] = Math.min(
                    Math.max(
                        0,
                        (tile.connections()
                            .size() - 1) / PAGE_SIZE),
                    page[0] + 1)));
        // #tr gui.overloaded.return_on
        // # Auto return: ON
        // # zh_CN 自动回收：开
        // #tr gui.overloaded.return_off
        // # Auto return: OFF
        // # zh_CN 自动回收：关
        panel.child(
            button(
                140,
                203,
                146,
                () -> tr(autoReturn.getBoolValue() ? "gui.overloaded.return_on" : "gui.overloaded.return_off"),
                sync,
                tile,
                tile::toggleAutoReturn));
        // #tr gui.overloaded.back
        // # Back
        // # zh_CN 返回
        panel.child(
            new ButtonWidget<>().pos(296, 203)
                .size(114, 20)
                .background(WirelessCardGuiTextures.BUTTONS[1])
                .hoverBackground(WirelessCardGuiTextures.BUTTONS[2])
                .overlay(IKey.lang("gui.overloaded.back"))
                .onMousePressed(mouse -> {
                    panel.closeIfOpen();
                    return true;
                }));
        panel.onCloseAction(() -> {
            panel.getScreen()
                .getMainPanel()
                .setEnabled(true);
            settings.getRecipeViewerSettings()
                .defaultState();
        });
        return panel;
    }

    private static ButtonWidget<?> button(int x, int y, int width, Supplier<String> label, PanelSyncManager sync,
        TileEntityOverloadedProvider tile, Runnable action) {
        return new ButtonWidget<>().pos(x, y)
            .size(width, 20)
            .background(WirelessCardGuiTextures.BUTTONS[1])
            .hoverBackground(WirelessCardGuiTextures.BUTTONS[2])
            .overlay(IKey.dynamic(label))
            .syncHandler(
                new InteractionSyncHandler().setOnMousePressed(
                    mouse -> {
                        if (!mouse.isClient() && mouse.mouseButton == 0 && tile.isUseableByPlayer(sync.getPlayer()))
                            action.run();
                    }));
    }

    private static String title(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 3) return "";
        return tr(parts[0]) + " (" + parts[1] + ") " + tr("gui.overloaded.face." + parts[2]);
    }

    private static String status(String value) {
        if (value.isEmpty()) return "";
        String[] parts = value.split(":");
        // #tr gui.overloaded.status.0
        // # Unloaded or missing
        // # zh_CN 区块未加载或机器已移除
        // #tr gui.overloaded.status.1
        // # ME network offline
        // # zh_CN ME 网络未就绪
        // #tr gui.overloaded.status.2
        // # Waiting for input space
        // # zh_CN 等待输入空间
        // #tr gui.overloaded.status.3
        // # Ready
        // # zh_CN 就绪
        // #tr gui.overloaded.pending
        // # %s | Pending: %s types
        // # zh_CN %s | 待送 %s 种
        return StatCollector
            .translateToLocalFormatted("gui.overloaded.pending", tr("gui.overloaded.status." + parts[0]), parts[1]);
    }

    private static final class TargetActions extends SyncHandler {

        private final TileEntityOverloadedProvider tile;
        private final PanelSyncManager sync;

        private TargetActions(TileEntityOverloadedProvider tile, PanelSyncManager sync) {
            this.tile = tile;
            this.sync = sync;
            allowC2S();
        }

        private void request(int action, String identity) {
            if (sync.isClient() && !identity.isEmpty())
                syncToServer(action, buffer -> buffer.writeStringToBuffer(identity));
        }

        @Override
        public void detectAndSendChanges(boolean init) {}

        @Override
        public void readOnClient(int id, PacketBuffer buffer) {}

        @Override
        public void readOnServer(int id, PacketBuffer buffer) {
            if (id != 0 && id != 1) return;
            final int x, y, z, face;
            try {
                String[] parts = buffer.readStringFromBuffer(64)
                    .split(",");
                if (parts.length != 4) return;
                x = Integer.parseInt(parts[0]);
                y = Integer.parseInt(parts[1]);
                z = Integer.parseInt(parts[2]);
                face = Integer.parseInt(parts[3]);
            } catch (java.io.IOException | RuntimeException malformed) {
                return;
            }
            ServerThreadUtil.addScheduledTask(() -> {
                if (!isValid() || !tile.isUseableByPlayer(sync.getPlayer())) return;
                PackagedTarget target = new PackagedTarget(tile.getWorldObj().provider.dimensionId, x, y, z, face);
                if (id == 0) tile.retry(target);
                else tile.unlink(target);
            });
        }
    }

    private static String tr(String key) {
        // #tr gui.overloaded.face.0
        // # Bottom
        // # zh_CN 底
        // #tr gui.overloaded.face.1
        // # Top
        // # zh_CN 顶
        // #tr gui.overloaded.face.2
        // # North
        // # zh_CN 北
        // #tr gui.overloaded.face.3
        // # South
        // # zh_CN 南
        // #tr gui.overloaded.face.4
        // # West
        // # zh_CN 西
        // #tr gui.overloaded.face.5
        // # East
        // # zh_CN 东
        return StatCollector.translateToLocal(key);
    }
}
