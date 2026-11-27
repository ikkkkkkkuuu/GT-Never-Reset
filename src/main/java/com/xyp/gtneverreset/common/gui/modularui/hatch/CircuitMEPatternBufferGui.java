package com.xyp.gtneverreset.common.gui.modularui.hatch;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerGhostIngredientSlot;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.utils.fluid.FluidInteractions;
import com.cleanroommc.modularui.utils.serialization.ByteBufAdapters;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.sizer.Unit;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.me.CircuitSharedSupplies;
import com.xyp.gtneverreset.compat.ae2fc.FluidDropCompat;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.ReadableNumberConverter;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.util.GTUtility;
import gregtech.common.modularui2.widget.SlotLikeButtonWidget;

public class CircuitMEPatternBufferGui extends SuperMTEHatchCraftingInputMEGui {

    private static final int PANEL_WIDTH = 188;
    private static final String PANEL = "shared_supplies";
    private static final Pattern FLUID_LIMIT_PATTERN = Pattern.compile("[0-9]*");
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
            .size(PANEL_WIDTH, 224)
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
        addSupplyMode(
            panel,
            165,
            "direct_items",
            // #tr gtnr.gui.circuit_supplies.direct_items
            // # Draw items from ME on use
            // # zh_CN 物品按需从 ME 扣除
            IKey.lang("gtnr.gui.circuit_supplies.direct_items"),
            // #tr gtnr.gui.circuit_supplies.direct_items_hint
            // # On: use network items when a recipe starts. Off: refill the hatch on the configured schedule.
            // # zh_CN 开启时，配方启动才扣除 ME 物品；关闭时，按补货设置预存到总成。
            IKey.lang("gtnr.gui.circuit_supplies.direct_items_hint"),
            new BooleanSyncValue(null, null, supplies::directItems, supplies::setDirectItems).allowC2S());
        addSupplyMode(
            panel,
            185,
            "direct_fluids",
            // #tr gtnr.gui.circuit_supplies.direct_fluids
            // # Draw fluid from ME on use
            // # zh_CN 流体按需从 ME 扣除
            IKey.lang("gtnr.gui.circuit_supplies.direct_fluids"),
            // #tr gtnr.gui.circuit_supplies.direct_fluids_hint
            // # On: use network fluid when a recipe starts. Off: refill the hatch on the configured schedule.
            // # zh_CN 开启时，配方启动才扣除 ME 流体；关闭时，按补货设置预存到总成。
            IKey.lang("gtnr.gui.circuit_supplies.direct_fluids_hint"),
            new BooleanSyncValue(null, null, supplies::directFluids, supplies::setDirectFluids).allowC2S());
        var left = new IntSyncValue(supplies::remainingSeconds);
        sync.syncValue("seconds_left", left);
        // #tr gtnr.gui.circuit_supplies.next
        // # Next pull in %s s
        // # zh_CN 下次定时拉取：%s 秒
        panel.child(
            IKey.dynamic(
                () -> StatCollector.translateToLocalFormatted("gtnr.gui.circuit_supplies.next", left.getIntValue()))
                .asWidget()
                .pos(6, 205)
                .setEnabledIf(w -> interval.getIntValue() > 0 && left.getIntValue() > 0));
        return panel;
    }

    private static void addSupplyMode(ModularPanel panel, int y, String name, IKey label, IKey hint,
        BooleanSyncValue mode) {
        panel.child(
            label.asWidget()
                .pos(6, y));
        panel.child(
            new ToggleButton().name(name)
                .value(mode)
                .size(48, 16)
                .pos(134, y - 4)
                .overlay(true, IKey.lang("options.on"))
                .overlay(false, IKey.lang("options.off"))
                .addTooltipLine(hint));
    }

    private SlotLikeButtonWidget sampleSlot(PanelSyncManager sync, int slot) {
        boolean itemSlot = slot < CircuitSharedSupplies.SLOTS_PER_CHANNEL;
        var icon = GenericSyncValue.builder(ItemStack.class)
            .getter(() -> display(supplies.target(slot)))
            .adapter(ByteBufAdapters.ITEM_STACK)
            .nullable()
            .build();
        sync.syncValue("sample", slot, icon);
        var amount = new LongSyncValue(() -> amount(slot));
        sync.syncValue("amount", slot, amount);
        String name = "supply_limit_" + slot;
        var editor = sync.syncedPanel(name, true, (manager, handler) -> amountPanel(name, slot));
        String drop = "supply_drop_" + slot;
        sync.registerSyncedAction(drop, false, true, buffer -> {
            ItemStack stack = NetworkUtils.readItemStack(buffer);
            if (stack != null) markSample(slot, stack);
        });
        var click = new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (mouse.mouseButton == 1 || mouse.mouseButton == 2) {
                if (mouse.isClient() && icon.getValue() != null) editor.openPanel();
            } else if (!mouse.isClient() && mouse.mouseButton == 0) {
                ItemStack cursor = sync.getPlayer().inventory.getItemStack();
                if (cursor == null) supplies.setTarget(slot, null);
                else markSample(slot, cursor);
            }
        });
        // #tr gtnr.gui.circuit_supplies.sample
        // # Drag from NEI or click with a sample. Empty-hand left-click clears. Right-click sets the limit.
        // # zh_CN 从 NEI 拖入或手持样品左键标记，空手左键清除，右键设置补货上限。
        return new SupplySampleSlot(
            icon::getValue,
            itemSlot,
            amount,
            stack -> sync.callSyncedAction(drop, buffer -> NetworkUtils.writeItemStack(buffer, stack))).size(18)
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
                                    + (itemSlot ? Long.toString(amount.getLongValue())
                                        : String.format("%,d mB", amount.getLongValue()))))
                        .addLine(IKey.lang("gtnr.gui.circuit_supplies.sample")));
    }

    private void markSample(int slot, ItemStack stack) {
        if (slot < CircuitSharedSupplies.SLOTS_PER_CHANNEL) {
            supplies.setTarget(slot, AEItemStack.create(stack));
        } else {
            FluidStack fluid = sampleFluid(stack);
            if (fluid != null) supplies.setTarget(slot, AEFluidStack.create(fluid));
        }
    }

    private static FluidStack sampleFluid(ItemStack stack) {
        FluidStack fluid = ItemFluidPacket.getFluidStack(stack);
        if (fluid == null && FluidDropCompat.isFluidDrop(stack)) fluid = FluidDropCompat.getFluidStack(stack);
        if (fluid == null) fluid = GTUtility.getFluidFromContainerOrFluidDisplay(stack);
        return fluid == null ? FluidInteractions.getFluidForItem(stack) : fluid;
    }

    private static final class SupplySampleSlot extends SlotLikeButtonWidget
        implements RecipeViewerGhostIngredientSlot<ItemStack> {

        private final boolean itemSlot;
        private final LongSyncValue amount;
        private final Consumer<ItemStack> drop;

        private SupplySampleSlot(Supplier<ItemStack> icon, boolean itemSlot, LongSyncValue amount,
            Consumer<ItemStack> drop) {
            super(icon);
            this.itemSlot = itemSlot;
            this.amount = amount;
            this.drop = drop;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            super.draw(context, theme);
            long count = amount.getLongValue();
            if (count <= 0) return;

            // Match GT's fluid display label within the centered 16-pixel inventory icon.
            var font = Minecraft.getMinecraft().fontRenderer;
            String text = itemSlot
                ? count < 10000 ? Long.toString(count) : ReadableNumberConverter.INSTANCE.toWideReadableForm(count)
                : NumberFormatUtil.formatFluidCompact(count);
            float scale = Math.min(font.getUnicodeFlag() ? 0.75f : 0.5f, 16f / font.getStringWidth(text));
            GlStateManager.pushMatrix();
            GlStateManager.translate((getArea().width - 16) / 2, (getArea().height - 16) / 2, 0);
            GlStateManager.scale(scale, scale, 1);
            font.drawString(text, 0, (int) (16 / scale) - font.FONT_HEIGHT + 1, 0xFFFFFF, true);
            GlStateManager.popMatrix();
        }

        @Override
        public boolean handleDragAndDrop(ItemStack stack, int button) {
            if (!areAncestorsEnabled() || stack == null
                || stack.getItem() == null
                || (!itemSlot && sampleFluid(stack) == null)) return false;
            drop.accept(stack);
            stack.stackSize = 0;
            return true;
        }
    }

    private long amount(int slot) {
        var target = supplies.target(slot);
        return target == null ? 0 : target.getStackSize();
    }

    private static ItemStack display(IAEStack<?> target) {
        if (target instanceof IAEFluidStack fluid)
            return GTUtility.getFluidDisplayStack(fluid.getFluidStack(), false, true);
        if (target instanceof IAEItemStack item) return GTUtility.copyAmount(1, item.getItemStack());
        return null;
    }

    private ModularPanel amountPanel(String name, int slot) {
        var limit = new LongSyncValue(null, null, () -> amount(slot), value -> {
            var target = supplies.target(slot);
            if (target != null) supplies.setTarget(slot, target.setStackSize(value));
        }).allowC2S();
        // #tr gtnr.gui.circuit_supplies.item_limit
        // # Item limit
        // # zh_CN 物品用量上限（个）
        // #tr gtnr.gui.circuit_supplies.fluid_limit
        // # Fluid limit (mB)
        // # zh_CN 流体用量上限（mB）
        boolean itemSlot = slot < CircuitSharedSupplies.SLOTS_PER_CHANNEL;
        String key = itemSlot ? "gtnr.gui.circuit_supplies.item_limit" : "gtnr.gui.circuit_supplies.fluid_limit";
        var field = new TextFieldWidget().name("supply_limit")
            .value(limit)
            .size(138, 16)
            .pos(6, 23);
        if (itemSlot) {
            field.numbersInt(1, Integer.MAX_VALUE)
                .setMaxLength(10);
        } else {
            field.setPattern(FLUID_LIMIT_PATTERN)
                .setValidator(CircuitMEPatternBufferGui::validateFluidLimit)
                .setMaxLength(19);
        }
        return GTGuis.createPopUpPanel(name)
            .background(GTGuiTextures.BACKGROUND_POPUP_STANDARD)
            .size(152, 48)
            .child(
                IKey.lang(key)
                    .asWidget()
                    .pos(6, 6))
            .child(field);
    }

    private static String validateFluidLimit(String text) {
        if (text.isEmpty()) return "1";
        try {
            return Long.toString(Math.max(1, Long.parseLong(text)));
        } catch (NumberFormatException ignored) {
            return Long.toString(Long.MAX_VALUE);
        }
    }
}
