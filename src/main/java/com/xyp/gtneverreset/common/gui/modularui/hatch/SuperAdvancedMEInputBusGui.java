// Adapted from GT5-Unofficial 5.09.54.183, LGPL-3.0. See META-INF/super-storage-input-port/.
package com.xyp.gtneverreset.common.gui.modularui.hatch;

import static com.xyp.gtneverreset.common.machine.hatch.SuperAdvancedMEInputBus.SLOT_COUNT;
import static gregtech.api.modularui2.GTGuis.createPopUpPanel;

import java.text.MessageFormat;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.utils.serialization.ByteBufAdapters;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.common.machine.hatch.SuperAdvancedMEInputBus;
import com.xyp.gtneverreset.common.machine.hatch.SuperAdvancedMEInputBus.ItemPolicy;

import appeng.core.localization.WailaText;
import appeng.me.GridAccessException;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.api.util.GTDataUtils;
import gregtech.api.util.GTUtility;
import gregtech.common.gui.modularui.adapter.MTEHatchInputBusMESlotAdapter;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;
import gregtech.common.gui.modularui.util.StockingSlot;
import gregtech.common.tileentities.machines.MTEHatchInputBusME.Slot;

public class SuperAdvancedMEInputBusGui extends MTEHatchBaseGui<SuperAdvancedMEInputBus> {

    private static final String FILTER_INV_NAME = "filter_inv";
    private static final String STOCK_INV_NAME = "stock_inv";
    private static final int FILTER_SLOT_ROW = 100;
    private static final int FILTER_SLOT_PER_ROW = 9;
    private static final int STOCK_SLOT_ROW = 100;
    private static final int STOCK_SLOT_PER_ROW = 9;
    private final Slot[] slots;
    private final VerticalScrollData itemScroll = new VerticalScrollData();
    private ItemStack selectedItem;
    private final GenericSyncValue<ItemStack, ?> selection = GenericSyncValue.builder(ItemStack.class)
        .getter(() -> selectedItem)
        .setter(value -> selectedItem = GTUtility.copyAmount(1, value))
        .adapter(ByteBufAdapters.ITEM_STACK)
        .nullable()
        .build()
        .allowC2S();
    private IPanelHandler policyPanel;

    public SuperAdvancedMEInputBusGui(SuperAdvancedMEInputBus hatch, Slot[] slots) {
        super(hatch);
        this.slots = slots;
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        syncManager.syncValue("selectedItem", selection);
        policyPanel = syncManager
            .syncedPanel("itemPolicy", true, (manager, handler) -> createPolicyPanel(panel, manager));
        BooleanSyncValue isAutoPullSyncer = new BooleanSyncValue(
            machine::isAutoPullItemList,
            machine::setAutoPullItemList).allowC2S();

        IItemHandlerModifiable configItemHandler = new IItemHandlerModifiable() {

            @Override
            public int getSlots() {
                return SLOT_COUNT * 2;
            }

            @Override
            public ItemStack getStackInSlot(int slotIndex) {
                boolean forConfig = slotIndex < SLOT_COUNT;
                slotIndex %= SLOT_COUNT;

                Slot slot = GTDataUtils.getIndexSafe(slots, slotIndex);

                if (slot == null) return null;

                return forConfig ? slot.config : GTUtility.copyAmountUnsafe(slot.extractedAmount, slot.extracted);
            }

            @Override
            public @Nullable ItemStack insertItem(int slot, @Nullable ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public @Nullable ItemStack extractItem(int slot, int amount, boolean simulate) {
                return null;
            }

            @Override
            public int getSlotLimit(int slot) {
                return Integer.MAX_VALUE;
            }

            @Override
            public void setStackInSlot(int slotIndex, ItemStack stack) {
                if (slotIndex < 0 || slotIndex >= SLOT_COUNT
                    || !baseMetaTileEntity.isServerSide()
                    || machine.isAutoPullItemList()) return;

                machine.setSlotConfig(slotIndex, GTUtility.copyAmount(1, stack));

                if (baseMetaTileEntity.isServerSide()) {
                    try {
                        machine.updateInformationSlot(slotIndex);
                    } catch (GridAccessException ignored) {}
                }
            }
        };

        Flow mainRow = Flow.row()
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.START);

        Flow left = Flow.col()
            .coverChildren()
            .childPadding(3);
        // #tr gtnr.super_storage_bus.marks
        // # Item marks
        // # zh_CN 物品标记
        left.child(
            IKey.lang("gtnr.super_storage_bus.marks")
                .asWidget());
        left.child(createFilterSlots(syncManager, configItemHandler, isAutoPullSyncer));
        mainRow.child(left);
        mainRow.child(createMiddleColumn(syncManager, panel, isAutoPullSyncer));
        Flow right = Flow.col()
            .coverChildren()
            .childPadding(3);
        // #tr gtnr.super_storage_bus.available
        // # Available to recipes
        // # zh_CN 配方可用数量
        right.child(
            IKey.lang("gtnr.super_storage_bus.available")
                .asWidget());
        right.child(createStockSlots(syncManager, configItemHandler));
        mainRow.child(right);

        Flow content = Flow.col()
            .coverChildren()
            .childPadding(4);
        content.child(mainRow);
        content.child(createOreFilter(syncManager, isAutoPullSyncer));
        return super.createContentSection(panel, syncManager).child(content);
    }

    private Flow createOreFilter(PanelSyncManager sync, BooleanSyncValue autoPull) {
        StringSyncValue expression = new StringSyncValue(machine::getOreFilter, machine::setOreFilter).allowC2S();
        BooleanSyncValue valid = new BooleanSyncValue(machine::isOreFilterValid);
        sync.syncValue("oreFilterValid", valid);
        Flow content = Flow.col()
            .width(350)
            .coverChildrenHeight()
            .childPadding(3);
        content.child(IKey.dynamic(() -> {
            if (!valid.getBoolValue()) {
                // #tr gtnr.super_storage_bus.filter_invalid
                // # Invalid regex: automatic selection is blocked
                // # zh_CN 正则无效：自动拉取已阻止
                return EnumChatFormatting.RED + IKey.lang("gtnr.super_storage_bus.filter_invalid")
                    .get();
            }
            if (!autoPull.getBoolValue()) {
                // #tr gtnr.super_storage_bus.filter_manual
                // # Ore regex (inactive in manual mode)
                // # zh_CN 矿词正则（手动模式下不生效）
                return IKey.lang("gtnr.super_storage_bus.filter_manual")
                    .get();
            }
            if (expression.getStringValue()
                .isEmpty()) {
                // #tr gtnr.super_storage_bus.filter_all
                // # Ore regex (empty: all items)
                // # zh_CN 矿词正则（留空：接收全部物品）
                return IKey.lang("gtnr.super_storage_bus.filter_all")
                    .get();
            }
            // #tr gtnr.super_storage_bus.filter_active
            // # Ore regex (filter active)
            // # zh_CN 矿词正则（过滤已启用）
            return EnumChatFormatting.DARK_GREEN + IKey.lang("gtnr.super_storage_bus.filter_active")
                .get();
        })
            .asWidget()
            .maxWidth(350));
        // #tr gtnr.super_storage_bus.filter_help
        // # Matches any ore name; case sensitive. Empty: all items. Example: ^dust(Impure|Pure).*
        // # zh_CN 匹配任一矿词，区分大小写；留空不限。例如：^dust(Impure|Pure).*
        content.child(
            new TextFieldWidget().value(expression)
                .setMaxLength(SuperAdvancedMEInputBus.MAX_FILTER_LENGTH)
                .width(350)
                .height(16)
                .addTooltipLine(IKey.lang("gtnr.super_storage_bus.filter_help")));
        return content;
    }

    private Grid createFilterSlots(PanelSyncManager syncManager, IItemHandlerModifiable handler,
        BooleanSyncValue autoPull) {
        syncManager.registerSlotGroup(FILTER_INV_NAME, FILTER_SLOT_PER_ROW);
        return new Grid().gridOfWidthHeight(FILTER_SLOT_PER_ROW, FILTER_SLOT_ROW, (x, y, index) -> {
            StockingSlot widget = new StockingSlot(autoPull) {

                @Override
                public @NotNull Result onMousePressed(int button) {
                    if (button == 1 && slots[index] != null) {
                        selection.setValue(slots[index].config);
                        policyPanel.openPanel();
                        return Result.SUCCESS;
                    }
                    return super.onMousePressed(button);
                }
            };
            // #tr gtnr.super_storage_bus.configure_slot
            // # Right-click: configure amounts. Empty-hand left-click: clear manual mark
            // # zh_CN 右键设置数量；空手左键清除手动标记
            widget.itemTooltip()
                .tooltipBuilder(t -> t.addLine(IKey.lang("gtnr.super_storage_bus.configure_slot")));
            return widget.slot(
                new ModularSlot(handler, index).slotGroup(FILTER_INV_NAME)
                    .filter(stack -> !autoPull.getBoolValue()));
        })
            .size(9 * SLOT_SIZE + 4, 4 * SLOT_SIZE)
            .scrollable(itemScroll);
    }

    private Flow createMiddleColumn(PanelSyncManager syncManager, ModularPanel panel,
        BooleanSyncValue isAutoPullSyncer) {
        Flow mainColumn = Flow.col()
            .width(18)
            .marginTop(12)
            .mainAxisAlignment(Alignment.MainAxis.START)
            .coverChildrenHeight();

        // toggle button for config panel
        IPanelHandler settingsPanel = syncManager
            .syncedPanel("configPanel", true, (manager, handler) -> createStackSizeConfigurationPanel(panel));
        mainColumn.child(new ToggleButton() {

            @Override
            public @NotNull Result onMousePressed(int mouseButton) {
                switch (mouseButton) {
                    case 0:
                        next();
                        playClickSound();
                        return Result.SUCCESS;
                    case 1:
                        if (!settingsPanel.isPanelOpen()) settingsPanel.openPanel();
                        else settingsPanel.closePanel();
                        playClickSound();
                        return Result.SUCCESS;
                }
                return Result.IGNORE;
            }
        }.value(isAutoPullSyncer)
            .size(16)
            .margin(1)
            .setEnabledIf(b -> machine.autoPullAvailable)
            .overlay(true, GTGuiTextures.OVERLAY_BUTTON_AUTOPULL_ME)
            .overlay(false, GTGuiTextures.OVERLAY_BUTTON_AUTOPULL_ME_DISABLED)
            .tooltip(t -> {
                t.addLine(IKey.lang("GT5U.machines.stocking_bus.auto_pull.tooltip.1"));
                t.addLine(IKey.lang("GT5U.machines.stocking_bus.auto_pull.tooltip.2"));
            }));

        BooleanSyncValue limited = new BooleanSyncValue(machine::isLimitedMode, machine::setLimitedMode).allowC2S();
        BooleanSyncValue fixed = new BooleanSyncValue(machine::isFixedMode, machine::setFixedMode).allowC2S();
        // #tr gtnr.super_storage_bus.limit_mode
        // # Reserve mode: keep each item's configured reserve in ME
        // # zh_CN 限制模式：在ME中保留每种物品设定的数量
        mainColumn.child(
            new ToggleButton().value(limited)
                .size(16)
                .margin(1)
                .overlay(true, GTGuiTextures.OVERLAY_BUTTON_LOCKED)
                .overlay(false, GTGuiTextures.OVERLAY_BUTTON_LOCK)
                .addTooltipLine(IKey.lang("gtnr.super_storage_bus.limit_mode")));
        // #tr gtnr.super_storage_bus.fixed_mode
        // # Fixed mode: offer each item's configured quantity per recipe check
        // # zh_CN 固定模式：每次配方检查提供各物品设定的数量
        mainColumn.child(
            new ToggleButton().value(fixed)
                .size(16)
                .margin(1)
                .overlay(true, GTGuiTextures.OVERLAY_BUTTON_CHECKMARK)
                .overlay(false, GTGuiTextures.OVERLAY_BUTTON_CROSS)
                .addTooltipLine(IKey.lang("gtnr.super_storage_bus.fixed_mode")));
        // #tr gtnr.super_storage_bus.pull_settings
        // # Configure minimum stock and refresh interval
        // # zh_CN 设置最低库存与刷新间隔
        mainColumn.child(
            new ButtonWidget<>().size(16)
                .margin(1)
                .overlay(IKey.str("..."))
                .addTooltipLine(IKey.lang("gtnr.super_storage_bus.pull_settings"))
                .onMousePressed(button -> {
                    settingsPanel.openPanel();
                    return true;
                }));
        return mainColumn;
    }

    private ModularPanel createStackSizeConfigurationPanel(ModularPanel parent) {
        IntSyncValue minAutoPullStackSizeSyncer = new IntSyncValue(
            machine::getMinAutoPullStackSize,
            machine::setMinAutoPullStackSize).allowC2S();
        IntSyncValue autoPullRefreshTimeSyncer = new IntSyncValue(
            machine::getAutoPullRefreshTime,
            machine::setAutoPullRefreshTime).allowC2S();

        Flow mainColumn = Flow.col()
            .width(144)
            .coverChildrenHeight()
            .marginTop(15)
            .childPadding(3);

        // #tr gtnr.super_storage_bus.minimum_stock
        // # Minimum stock (items)
        // # zh_CN 最低库存（个）
        mainColumn.child(
            IKey.lang("gtnr.super_storage_bus.minimum_stock")
                .asWidget());

        mainColumn.child(
            new TextFieldWidget().value(minAutoPullStackSizeSyncer)
                .numbersInt(1, Integer.MAX_VALUE)
                .formatAsInteger(true)
                .setMaxLength(10)
                .setTextAlignment(Alignment.CENTER)
                .width(144));

        // #tr gtnr.super_storage_bus.refresh_ticks
        // # Refresh interval (ticks)
        // # zh_CN 刷新间隔（tick）
        mainColumn.child(
            IKey.lang("gtnr.super_storage_bus.refresh_ticks")
                .asWidget()
                .maxWidth(144)
                .textAlign(Alignment.Center));

        mainColumn.child(
            new TextFieldWidget().value(autoPullRefreshTimeSyncer)
                .numbersInt(1, Integer.MAX_VALUE)
                .formatAsInteger(true)
                .setMaxLength(10)
                .setTextAlignment(Alignment.CENTER)
                .width(144));

        return createPopUpPanel("configPanel").size(160, 112)
            .relative(parent)
            .center()
            .padding(8)
            .child(mainColumn);
    }

    private Grid createStockSlots(PanelSyncManager syncManager, IItemHandlerModifiable handler) {
        syncManager.registerSlotGroup(STOCK_INV_NAME, STOCK_SLOT_PER_ROW);
        return new Grid()
            .gridOfWidthHeight(
                STOCK_SLOT_PER_ROW,
                STOCK_SLOT_ROW,
                (x, y, index) -> new ItemSlot()
                    .slot(
                        new ModularSlot(handler, SLOT_COUNT + index).slotGroup(STOCK_INV_NAME)
                            .accessibility(false, false))
                    .backgroundOverlay(GTGuiTextures.SLOT_ITEM_DARK))
            .size(9 * SLOT_SIZE + 4, 4 * SLOT_SIZE)
            .scrollable(itemScroll);
    }

    @Override
    protected Flow createBottomRightCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return Flow.row()
            .coverChildren()
            .childPadding(2)
            .verticalCenter()
            .rightRel(0)
            .child(createCircuitSlot(syncManager))
            .child(
                new ItemSlot().slot(
                    new ModularSlot(machine.inventoryHandler, machine.getManualSlot()).slotGroup("item_inv")
                        .changeListener((stack, amountOnly, client, init) -> {
                            if (!client) {
                                baseMetaTileEntity.enableTicking();
                                machine.onContentsChanged(machine.getManualSlot());
                            }
                        }))
                    .addTooltipLine(IKey.lang("GT5U.machines.stocking_bus.manual_slot.tooltip.1")))
            .child(makeLogoWidget());
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        BooleanSyncValue isActiveSyncer = new BooleanSyncValue(machine::isActive);
        BooleanSyncValue isPoweredSyncer = new BooleanSyncValue(machine::isPowered);
        BooleanSyncValue isBootingSyncer = new BooleanSyncValue(machine::isBooting);
        BooleanSyncValue isAllowedToWorkSyncer = new BooleanSyncValue(machine::isAllowedToWork);

        syncManager.syncValue("isActive", isActiveSyncer);
        syncManager.syncValue("isPowered", isPoweredSyncer);
        syncManager.syncValue("isBooting", isBootingSyncer);
        syncManager.syncValue("isAllowedToWork", isAllowedToWorkSyncer);

        // status label
        TextWidget<?> status = IKey.dynamic(() -> {
            boolean isActive = isActiveSyncer.getBoolValue();
            boolean isPowered = isPoweredSyncer.getBoolValue();
            boolean isBooting = isBootingSyncer.getBoolValue();

            String state = WailaText.getPowerState(isActive, isPowered, isBooting);

            if (isActive && isPowered) {
                return MessageFormat.format(
                    "{0} ({1})",
                    EnumChatFormatting.GREEN + state + EnumChatFormatting.RESET,
                    IKey.lang(
                        isAllowedToWorkSyncer.getBoolValue() ? "GT5U.gui.text.enabled" : "GT5U.gui.text.disabled"));
            } else {
                return EnumChatFormatting.DARK_RED + state + EnumChatFormatting.RESET;
            }
        })
            .asWidget()
            .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE);

        return super.createBottomLeftCornerFlow(panel, syncManager).child(status);
    }

    @Override
    public void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.registerSlotGroup("item_inv", 1);

        GenericListSyncHandler<Slot> slotSyncHandler = GenericListSyncHandler.<Slot>builder()
            .getter(() -> GTDataUtils.mapToList(slots, slot -> {
                if (slot == null) return null;
                Slot copy = new Slot(slot.config.copy());
                copy.extracted = slot.extracted == null ? null : slot.extracted.copy();
                copy.extractedAmount = slot.extractedAmount;
                return copy;
            }))
            .setter(slots2 -> System.arraycopy(slots2.toArray(new Slot[0]), 0, slots, 0, SLOT_COUNT))
            .adapter(new MTEHatchInputBusMESlotAdapter())
            .build();
        syncManager.syncValue("slots", slotSyncHandler);
    }

    @Override
    protected int getBasePanelHeight() {
        return 238;
    }

    @Override
    protected boolean supportsBottomRowOverlap() {
        return false;
    }

    @Override
    protected int getBasePanelWidth() {
        return 366;
    }

    private ModularPanel createPolicyPanel(ModularPanel parent, PanelSyncManager sync) {
        LongSyncValue reserve = new LongSyncValue(() -> {
            ItemPolicy policy = getSelectedPolicy();
            return policy == null ? 0 : policy.reserve;
        }, value -> {
            ItemPolicy policy = getSelectedPolicy();
            if (baseMetaTileEntity.isServerSide() && policy != null) {
                machine.setItemReserve(getSelectedSlot(), value);
            }
        }).allowC2S();
        IntSyncValue batch = new IntSyncValue(() -> {
            ItemPolicy policy = getSelectedPolicy();
            return policy == null ? 1 : policy.batch;
        }, value -> {
            ItemPolicy policy = getSelectedPolicy();
            if (baseMetaTileEntity.isServerSide() && policy != null) {
                machine.setItemBatch(getSelectedSlot(), value);
            }
        }).allowC2S();
        Flow content = Flow.col()
            .coverChildren()
            .childPadding(4)
            .marginTop(16);
        content.child(
            IKey.dynamic(() -> selectedItem == null ? "" : selectedItem.getDisplayName())
                .asWidget()
                .maxWidth(200));
        // #tr gtnr.super_storage_bus.reserve_amount
        // # Keep in ME (items)
        // # zh_CN ME网络保留量（个）
        content.child(
            IKey.lang("gtnr.super_storage_bus.reserve_amount")
                .asWidget());
        content.child(
            new TextFieldWidget().value(reserve)
                .numbersLong(0, Long.MAX_VALUE)
                .formatAsInteger(true)
                .setMaxLength(19)
                .width(180)
                .setEnabledIf(widget -> getSelectedSlot() >= 0));
        // #tr gtnr.super_storage_bus.batch_amount
        // # Available per recipe check (items)
        // # zh_CN 每次配方检查可用量（个）
        content.child(
            IKey.lang("gtnr.super_storage_bus.batch_amount")
                .asWidget());
        content.child(
            new TextFieldWidget().value(batch)
                .numbersInt(1, Integer.MAX_VALUE)
                .formatAsInteger(true)
                .setMaxLength(10)
                .width(180)
                .setEnabledIf(widget -> getSelectedSlot() >= 0));
        // #tr gtnr.super_storage_bus.batch_help
        // # Waits for the full amount; consumes only what the recipe needs
        // # zh_CN 不足设定量则等待，实际只扣除配方消耗
        content.child(
            IKey.lang("gtnr.super_storage_bus.batch_help")
                .asWidget()
                .maxWidth(200));
        return createPopUpPanel("itemPolicy").size(224, 160)
            .relative(parent)
            .center()
            .padding(8)
            .child(content);
    }

    private ItemPolicy getSelectedPolicy() {
        if (!baseMetaTileEntity.isServerSide()) return null;
        int index = getSelectedSlot();
        return index < 0 ? null : machine.getPolicy(index);
    }

    private int getSelectedSlot() {
        if (selectedItem == null) return -1;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (slots[i] != null && GTUtility.areStacksEqual(slots[i].config, selectedItem)) return i;
        }
        return -1;
    }

}
