package com.xyp.gtneverreset.common.gui.modularui.hatch;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.serialization.ByteBufAdapters;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.sizer.Unit;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.me.CircuitSharedSupplies;
import com.xyp.gtneverreset.compat.ae2fc.FluidDropCompat;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.util.GTUtility;
import gregtech.common.modularui2.widget.SlotLikeButtonWidget;

public class CircuitMEPatternBufferGui extends SuperMTEHatchCraftingInputMEGui {

    private static final int PANEL_WIDTH = 188;
    private static final String PANEL = "shared_supplies";
    private final CircuitSharedSupplies supplies;

    public CircuitMEPatternBufferGui(CircuitMEPatternBuffer hatch) {
        super(hatch);
        supplies = hatch.getSharedSupplies();
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel parent, PanelSyncManager sync) {
        var popup = sync.syncedPanel(PANEL, true, (manager, handler) -> supplyPanel(parent, manager));
        // #tr gtnr.gui.circuit_supplies.title
        // # Shared consumable supplies
        // # zh_CN 共享可消耗补货
        return super.createBottomLeftCornerFlow(parent, sync).child(
            new ButtonWidget<>().name(PANEL)
                .overlay(GTGuiTextures.OVERLAY_BUTTON_IMPORT)
                .addTooltipLine(IKey.lang("gtnr.gui.circuit_supplies.title"))
                .onMousePressed(button -> {
                    if (button != 0) return false;
                    popup.openPanel();
                    return true;
                }));
    }

    private ModularPanel supplyPanel(ModularPanel parent, PanelSyncManager sync) {
        var panel = GTGuis.createPopUpPanel(PANEL)
            .background(GTGuiTextures.BACKGROUND_POPUP_STANDARD)
            .size(PANEL_WIDTH, 184)
            .relative(parent)
            .left(() -> Math.max(-PANEL_WIDTH - 3, -parent.getArea().x), Unit.Measure.PIXEL)
            .top(0);
        panel.child(
            IKey.lang("gtnr.gui.circuit_supplies.title")
                .asWidget()
                .pos(6, 5));
        // #tr gtnr.gui.circuit_supplies.items
        // # Items
        // # zh_CN 物品
        panel.child(
            IKey.lang("gtnr.gui.circuit_supplies.items")
                .asWidget()
                .pos(6, 21));
        // #tr gtnr.gui.circuit_supplies.fluids
        // # Fluids (mB)
        // # zh_CN 流体（mB）
        panel.child(
            IKey.lang("gtnr.gui.circuit_supplies.fluids")
                .asWidget()
                .pos(6, 70));
        for (int slot = 0; slot < 2 * CircuitSharedSupplies.SLOTS_PER_CHANNEL; slot++) {
            int row = slot % CircuitSharedSupplies.SLOTS_PER_CHANNEL / 8;
            int y = slot < CircuitSharedSupplies.SLOTS_PER_CHANNEL ? 32 : 81;
            panel.child(sampleSlot(sync, slot).pos(6 + slot % 8 * 18, y + row * 18));
        }
        // #tr gtnr.gui.circuit_supplies.interval
        // # Interval (s, 0 = off)
        // # zh_CN 定时拉取（秒，0 为关闭）
        panel.child(
            IKey.lang("gtnr.gui.circuit_supplies.interval")
                .asWidget()
                .pos(6, 125));
        var interval = new IntSyncValue(null, null, supplies::interval, supplies::setInterval).allowC2S();
        panel.child(
            new TextFieldWidget().name("pull_interval")
                .value(interval)
                .numbersInt(0, CircuitSharedSupplies.MAX_INTERVAL)
                .setMaxLength(5)
                .size(48, 16)
                .pos(134, 121));
        // #tr gtnr.gui.circuit_supplies.on_check
        // # Pull on recipe check
        // # zh_CN 配方启动时拉取
        panel.child(
            IKey.lang("gtnr.gui.circuit_supplies.on_check")
                .asWidget()
                .pos(6, 145));
        var onCheck = new BooleanSyncValue(null, null, supplies::onRecipeCheck, supplies::setOnRecipeCheck).allowC2S();
        panel.child(
            new ToggleButton().name("pull_on_check")
                .value(onCheck)
                .size(48, 16)
                .pos(134, 141)
                .overlay(true, IKey.lang("options.on"))
                .overlay(false, IKey.lang("options.off")));
        var left = new IntSyncValue(supplies::remainingSeconds);
        sync.syncValue("seconds_left", left);
        // #tr gtnr.gui.circuit_supplies.next
        // # Next pull in %s s
        // # zh_CN 下次定时拉取：%s 秒
        panel.child(
            IKey.dynamic(
                () -> StatCollector.translateToLocalFormatted("gtnr.gui.circuit_supplies.next", left.getIntValue()))
                .asWidget()
                .pos(6, 165)
                .setEnabledIf(w -> interval.getIntValue() > 0));
        return panel;
    }

    private SlotLikeButtonWidget sampleSlot(PanelSyncManager sync, int slot) {
        boolean itemSlot = slot < CircuitSharedSupplies.SLOTS_PER_CHANNEL;
        var icon = GenericSyncValue.builder(ItemStack.class)
            .getter(() -> display(supplies.target(slot)))
            .adapter(ByteBufAdapters.ITEM_STACK)
            .nullable()
            .build();
        sync.syncValue("sample", slot, icon);
        var amount = new IntSyncValue(() -> amount(slot));
        sync.syncValue("amount", slot, amount);
        String name = "supply_limit_" + slot;
        var editor = sync.syncedPanel(name, true, (manager, handler) -> amountPanel(name, slot));
        var click = new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (mouse.mouseButton == 1 || mouse.mouseButton == 2) {
                if (mouse.isClient() && icon.getValue() != null) editor.openPanel();
            } else if (!mouse.isClient() && mouse.mouseButton == 0) {
                ItemStack cursor = sync.getPlayer().inventory.getItemStack();
                if (cursor == null) supplies.setTarget(slot, null);
                else if (itemSlot) supplies.setTarget(slot, AEItemStack.create(cursor));
                else {
                    FluidStack fluid = ItemFluidPacket.getFluidStack(cursor);
                    if (fluid == null && FluidDropCompat.isFluidDrop(cursor))
                        fluid = FluidDropCompat.getFluidStack(cursor);
                    if (fluid == null) fluid = GTUtility.getFluidFromContainerOrFluidDisplay(cursor);
                    if (fluid != null) supplies.setTarget(slot, AEFluidStack.create(fluid));
                }
            }
        });
        // #tr gtnr.gui.circuit_supplies.sample
        // # Left-click with a sample to mark, or with an empty hand to clear. Right-click to set the limit.
        // # zh_CN 左键用手持样品标记，空手左键清除；右键设置补货上限。
        return new SlotLikeButtonWidget(icon::getValue).size(18)
            .widgetTheme(itemSlot ? IThemeApi.ITEM_SLOT : IThemeApi.FLUID_SLOT)
            .backgroundOverlay(
                itemSlot ? GuiTextures.SLOT_ITEM : GTGuiTextures.SLOT_FLUID_DARK,
                GTGuiTextures.OVERLAY_SLOT_ARROW_ME)
            .name("supply_sample_" + slot)
            .syncHandler(click)
            .tooltip(
                t -> t.addLine(
                    IKey.dynamic(
                        () -> icon.getValue() == null ? ""
                            : icon.getValue()
                                .getDisplayName() + " × "
                                + amount.getIntValue()))
                    .addLine(IKey.lang("gtnr.gui.circuit_supplies.sample")));
    }

    private int amount(int slot) {
        var target = supplies.target(slot);
        return target == null ? 0 : (int) target.getStackSize();
    }

    private static ItemStack display(IAEStack<?> target) {
        if (target instanceof IAEFluidStack fluid) return GTUtility.getFluidDisplayStack(fluid.getFluidStack(), true);
        if (target instanceof IAEItemStack item) return GTUtility.copyAmount(1, item.getItemStack());
        return null;
    }

    private ModularPanel amountPanel(String name, int slot) {
        var limit = new IntSyncValue(null, null, () -> amount(slot), value -> {
            var target = supplies.target(slot);
            if (target != null) supplies.setTarget(slot, target.setStackSize(value));
        }).allowC2S();
        // #tr gtnr.gui.circuit_supplies.item_limit
        // # Target item count
        // # zh_CN 物品补货上限（个）
        // #tr gtnr.gui.circuit_supplies.fluid_limit
        // # Target fluid amount (mB)
        // # zh_CN 流体补货上限（mB）
        String key = slot < CircuitSharedSupplies.SLOTS_PER_CHANNEL ? "gtnr.gui.circuit_supplies.item_limit"
            : "gtnr.gui.circuit_supplies.fluid_limit";
        return GTGuis.createPopUpPanel(name)
            .background(GTGuiTextures.BACKGROUND_POPUP_STANDARD)
            .size(152, 48)
            .child(
                IKey.lang(key)
                    .asWidget()
                    .pos(6, 6))
            .child(
                new TextFieldWidget().name("supply_limit")
                    .value(limit)
                    .numbersInt(1, Integer.MAX_VALUE)
                    .setMaxLength(10)
                    .size(138, 16)
                    .pos(6, 23));
    }
}
