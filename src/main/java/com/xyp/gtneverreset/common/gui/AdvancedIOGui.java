package com.xyp.gtneverreset.common.gui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.utils.item.InvWrapper;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.widget.sizer.Unit;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.glodblock.github.util.Util;
import com.xyp.gtneverreset.common.part.PartAdvancedIOBus;
import com.xyp.gtneverreset.compat.ae2fc.FluidDropCompat;
import com.xyp.gtneverreset.config.ScreenThemeConfig;
import com.xyp.gtnrlib.gui.theme.ButtonPurpose;
import com.xyp.gtnrlib.mod.ModList;

import appeng.api.config.RedstoneMode;
import appeng.api.config.SchedulingMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.util.GTUtility;

/** Mixed item and fluid stock filters and their server-backed amount editor. */
public final class AdvancedIOGui {

    public static final Factory FACTORY = new Factory();

    private static final UITexture REDSTONE_HIGH = icon("states", 1, 0), REDSTONE_LOW = icon("states", 0, 0),
        REDSTONE_IGNORE = icon("states", 3, 0), BACK = icon("states", 6, 1);
    private static final UITexture[] SCHEDULING = { icon("states", 0, 15), icon("states", 1, 15),
        icon("states", 2, 15) };
    private static final UITexture REGULATE_ON = icon("advanced_states", 2, 2),
        REGULATE_OFF = icon("advanced_states", 3, 2), EXPORT_ON = icon("advanced_states", 3, 1),
        EXPORT_OFF = icon("advanced_states", 5, 0);

    static ModularPanel build(PartAdvancedIOBus part, PanelSyncManager sync) {
        var enabled = new IntSyncValue(part::availableSlots);
        var regulate = new BooleanSyncValue(part::regulate);
        var exportOnly = new BooleanSyncValue(part::exportOnly);
        var hasRedstone = new BooleanSyncValue(() -> part.getInstalledUpgrades(Upgrades.REDSTONE) > 0);
        var redstone = new IntSyncValue(
            () -> part.getRSMode()
                .ordinal());
        var scheduling = new IntSyncValue(
            () -> ((SchedulingMode) part.getConfigManager()
                .getSetting(Settings.SCHEDULING_MODE)).ordinal());
        sync.syncValue("enabled", enabled);
        sync.syncValue("regulate", regulate);
        sync.syncValue("exportOnly", exportOnly);
        sync.syncValue("hasRedstone", hasRedstone);
        sync.syncValue("redstone", redstone);
        sync.syncValue("scheduling", scheduling);
        var samples = new SampleItemHandler(part);
        var panel = ModularPanel.defaultPanel("advanced_io", 224, 244);
        panel.child(
            IKey.lang("item.advanced_io_bus.name")
                .asWidget()
                .pos(8, 6));
        // #tr gui.advancedio.amount_hint
        // # Middle-click a sample to set target stock.
        // # zh_CN 中键样本，设置目标库存。
        panel.child(
            IKey.lang("gui.advancedio.amount_hint")
                .scale(0.8f)
                .asWidget()
                .pos(7, 17));
        for (int i = 0; i < PartAdvancedIOBus.CONFIG_SLOTS; i++) {
            final int index = i;
            var amount = new IntSyncValue(
                () -> part.filter(index) == null ? 0
                    : (int) part.filter(index)
                        .getStackSize());
            sync.syncValue("amount_" + i, amount);
            var fluid = new BooleanSyncValue(
                () -> part.filter(index) != null && part.filter(index)
                    .isFluid());
            sync.syncValue("fluid_" + i, fluid);
            var handler = new SampleSyncHandler(new ModularSlot(samples, i).singletonSlotGroup(), part, index);
            sync.syncValue("sample", i, handler);
            int xSlot = 7 + i % 9 * 18;
            int ySlot = 27 + i / 9 * 18;
            if (i >= 18) {
                panel.child(
                    new Rectangle().color(0x30000000)
                        .asWidget()
                        .pos(xSlot + 1, ySlot + 1)
                        .size(16)
                        .setEnabledIf(widget -> index >= enabled.getIntValue()));
            }
            panel.child(
                new PhantomItemSlot().syncHandler(handler)
                    .overlay((context, x, y, w, h, theme) -> {
                        if (amount.getIntValue() <= 0) return;
                        String text = shortAmount(amount.getIntValue());
                        var font = Minecraft.getMinecraft().fontRenderer;
                        GL11.glPushMatrix();
                        GL11.glScalef(0.666f, 0.666f, 1);
                        font.drawStringWithShadow(
                            text,
                            (int) ((x + 18) / 0.666f - font.getStringWidth(text)),
                            (int) ((y + 16) / 0.666f - 5),
                            0xffffff);
                        GL11.glPopMatrix();
                    })
                    .tooltip(
                        t -> t.addLine(IKey.dynamic(() -> amount.getIntValue() + (fluid.getBoolValue() ? " L" : "")))
                            .addLine(IKey.lang("gui.advancedio.amount_hint")))
                    .setEnabledIf(widget -> index < enabled.getIntValue())
                    .pos(xSlot, ySlot));
        }
        // #tr gui.advancedio.upgrades
        // # Upgrades
        // # zh_CN 升级卡
        panel.child(
            IKey.lang("gui.advancedio.upgrades")
                .asWidget()
                .pos(176, 16));
        var upgrades = new InvWrapper(part.getInventoryByName("upgrades"));
        for (int i = 0; i < upgrades.getSlots(); i++) {
            panel.child(
                new ItemSlot().slot(new ModularSlot(upgrades, i).singletonSlotGroup())
                    .pos(176 + i % 2 * 18, 27 + i / 2 * 18));
        }
        // #tr gui.advancedio.controls
        // # Modes
        // # zh_CN 工作模式
        panel.child(
            IKey.lang("gui.advancedio.controls")
                .asWidget()
                .pos(176, 106));
        panel.child(
            IKey.lang("container.inventory")
                .asWidget()
                .pos(8, 158));
        for (int i = 0; i < 36; i++) {
            int column = i < 9 ? i : (i - 9) % 9;
            int row = i < 9 ? 3 : (i - 9) / 9;
            panel.child(
                new ItemSlot().syncHandler("player", i)
                    .pos(7 + column * 18, 167 + row * 18 + (i < 9 ? 2 : 0)));
        }
        panel.child(
            toolbarButton(
                sync,
                "redstoneButton",
                () -> redstone.getIntValue() == RedstoneMode.HIGH_SIGNAL.ordinal() ? REDSTONE_HIGH
                    : redstone.getIntValue() == RedstoneMode.LOW_SIGNAL.ordinal() ? REDSTONE_LOW : REDSTONE_IGNORE,
                () -> redstoneLabel(redstone.getIntValue()),
                mouse -> {
                    if (part.getInstalledUpgrades(Upgrades.REDSTONE) == 0) return;
                    RedstoneMode[] modes = { RedstoneMode.IGNORE, RedstoneMode.HIGH_SIGNAL, RedstoneMode.LOW_SIGNAL };
                    int old = part.getRSMode() == RedstoneMode.IGNORE ? 0
                        : part.getRSMode() == RedstoneMode.HIGH_SIGNAL ? 1 : 2;
                    part.getConfigManager()
                        .putSetting(Settings.REDSTONE_CONTROLLED, modes[(old + (mouse.mouseButton == 1 ? 2 : 1)) % 3]);
                    part.changed();
                }).pos(196, 139)
                    .setEnabledIf(w -> hasRedstone.getBoolValue()));
        panel.child(
            toolbarButton(
                sync,
                "schedulingButton",
                () -> SCHEDULING[scheduling.getIntValue()],
                () -> schedulingLabel(scheduling.getIntValue()),
                mouse -> {
                    int old = ((SchedulingMode) part.getConfigManager()
                        .getSetting(Settings.SCHEDULING_MODE)).ordinal();
                    part.getConfigManager()
                        .putSetting(
                            Settings.SCHEDULING_MODE,
                            SchedulingMode.values()[(old + (mouse.mouseButton == 1 ? 2 : 1)) % 3]);
                    part.changed();
                }).pos(176, 117));
        // #tr gui.advancedio.regulate
        // # Return stock above the target to ME
        // # zh_CN 将超出目标的库存送回 ME
        panel.child(
            toolbarButton(
                sync,
                "regulateButton",
                () -> regulate.getBoolValue() ? REGULATE_ON : REGULATE_OFF,
                () -> IKey.lang("gui.advancedio.regulate")
                    .get() + ": "
                    + yesNo(regulate.getBoolValue()),
                mouse -> part.setRegulate(!part.regulate())).pos(196, 117));
        // #tr gui.advancedio.export_only
        // # Only supply the machine (no product or excess stock recovery)
        // # zh_CN 仅向机器输入（不回收产物或超额库存）
        panel.child(
            toolbarButton(
                sync,
                "exportOnlyButton",
                () -> exportOnly.getBoolValue() ? EXPORT_ON : EXPORT_OFF,
                () -> IKey.lang("gui.advancedio.export_only")
                    .get() + ": "
                    + yesNo(exportOnly.getBoolValue()),
                mouse -> part.setExportOnly(!part.exportOnly())).pos(176, 139));
        return panel;
    }

    /** Retains SetAmountScreen's separate 176x107 page; changes only commit when Set/Enter is pressed. */
    static ModularPanel amountPanel(PartAdvancedIOBus part, PanelSyncManager sync, int index) {
        int[] draft = { part.filter(index) == null ? 1
            : (int) part.filter(index)
                .getStackSize() };
        var quantity = new IntSyncValue(() -> draft[0], value -> draft[0] = Math.max(0, value)).allowC2S();
        sync.syncValue("quantity", quantity);
        var fluidAmount = new BooleanSyncValue(
            () -> part.filter(index) != null && part.filter(index)
                .isFluid());
        sync.syncValue("fluidAmount", fluidAmount);
        var displayQuantity = new DoubleSyncValue(
            () -> (double) draft[0],
            value -> {
                if (Double.isFinite(value))
                    draft[0] = (int) Math.max(0, Math.min(Integer.MAX_VALUE - 1L, Math.round(value)));
            }) {

            @Override
            public String getStringValue() {
                return BigDecimal.valueOf(getDoubleValue())
                    .stripTrailingZeros()
                    .toPlainString();
            }
        }.allowC2S();
        sync.syncValue("displayQuantity", displayQuantity);
        var panel = ModularPanel.defaultPanel("advanced_io_amount", 176, 107);
        fluidAmount.setChangeListener(panel::scheduleResize);
        // #tr gui.advancedio.set_amount
        // # Set Amount
        // # zh_CN 设置数量
        panel.child(
            IKey.lang("gui.advancedio.set_amount")
                .asWidget()
                .pos(8, 6));
        var samples = new SampleItemHandler(part);
        panel.child(
            new ItemSlot().slot(
                new ModularSlot(samples, index).singletonSlotGroup()
                    .canPut(false)
                    .canTake(false))
                .pos(22, 52));
        var confirm = new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (mouse.isClient()) return;
            if (index < part.availableSlots() && part.filter(index) != null) {
                part.setFilter(
                    index,
                    draft[0] == 0 ? null
                        : part.filter(index)
                            .copy()
                            .setStackSize(draft[0]));
            }
            AdvancedIOGui.FACTORY.open(sync.getPlayer(), part);
        });
        sync.syncValue("confirm", confirm);
        panel.child(new TextFieldWidget() {

            @Override
            public void setText(@Nonnull String text) {
                // DoubleSyncValue's change notification uses Double.toString even for integer amounts.
                super.setText(text.endsWith(".0") ? text.substring(0, text.length() - 2) : text);
            }

            @Override
            @Nonnull
            public Result onKeyPressed(char character, int keyCode) {
                Result result = super.onKeyPressed(character, keyCode);
                if (result == Result.SUCCESS && (keyCode == 28 || keyCode == 156)) confirm.onMousePressed(0);
                return result;
            }
        }.value(displayQuantity)
            .numbersDouble(() -> 0, () -> Integer.MAX_VALUE - 1.0)
            .autoUpdateOnChange(true)
            .setFocusOnGuiOpen(true)
            .pos(43, 53)
            .height(16)
            .width(() -> fluidAmount.getBoolValue() ? 62 : 73, Unit.Measure.PIXEL));
        panel.child(
            IKey.dynamic(() -> fluidAmount.getBoolValue() ? "L" : "")
                .asWidget()
                .pos(108, 57));
        int[] xs = { 20, 48, 82, 120 };
        int[] widths = { 22, 28, 32, 38 };
        int[] decimal = { 1, 10, 100, 1000 };
        int[] stacks = { 1, 16, 32, 64 };
        for (int i = 0; i < 8; i++) {
            final int step = i % 4;
            final int sign = i < 4 ? 1 : -1;
            var action = new InteractionSyncHandler().setOnMousePressed(mouse -> {
                if (!mouse.isClient()) quantity.setIntValue(
                    (int) Math.max(
                        0,
                        Math.min(
                            Integer.MAX_VALUE - 1L,
                            (long) draft[0] + (long) sign * (mouse.shift || mouse.ctrl ? stacks[step] : decimal[step])
                                - (sign > 0 && step > 0 && draft[0] == 1 ? 1 : 0))),
                    true,
                    true);
            });
            sync.syncValue("step", i, action);
            panel.child(
                textButton(() -> (sign > 0 ? "+" : "-") + (shiftOrControl() ? stacks[step] : decimal[step]))
                    .syncHandler(action)
                    .pos(xs[step], i < 4 ? 30 : 72)
                    .size(widths[step], 20));
        }
        // #tr gui.advancedio.set
        // # Set
        // # zh_CN 设置
        panel.child(
            textButton(
                () -> IKey.lang("gui.advancedio.set")
                    .get()).syncHandler(confirm)
                        .pos(120, 51)
                        .size(38, 20));
        var back = new InteractionSyncHandler()
            .setOnMousePressed(mouse -> { if (!mouse.isClient()) AdvancedIOGui.FACTORY.open(sync.getPlayer(), part); });
        sync.syncValue("back", back);
        panel.child(
            ButtonPurpose.ICON.apply(new ButtonWidget<>())
                .overlay(
                    BACK.asIcon()
                        .size(16))
                .syncHandler(back)
                // #tr gui.advancedio.back
                // # Back
                // # zh_CN 返回
                .tooltip(t -> t.addLine(IKey.lang("gui.advancedio.back")))
                .pos(150, 5)
                .size(20));
        return panel;
    }

    private static UITexture icon(String name, int column, int row) {
        return UITexture.builder()
            .location(ModList.ModIds.GT_NEVER_RESET, "gui/advancedio/" + name)
            .imageSize(256, 256)
            .subAreaXYWH(column * 16, row * 16, 16, 16)
            .nonOpaque()
            .build();
    }

    @SideOnly(Side.CLIENT)
    private static boolean shiftOrControl() {
        return GuiScreen.isShiftKeyDown() || GuiScreen.isCtrlKeyDown();
    }

    private static ButtonWidget<?> textButton(Supplier<String> label) {
        return new ButtonWidget<>().overlay(IKey.dynamic(label));
    }

    /** Slot labels abbreviate item counts or GTNH liters with compact magnitudes. */
    private static String shortAmount(int amount) {
        double value = amount;
        String suffix = "";
        if (value >= 1000000) {
            value /= 1000000;
            suffix = "M";
        } else if (value >= 1000) {
            value /= 1000;
            suffix = "k";
        }
        return BigDecimal.valueOf(value)
            .setScale(value < 1 ? 3 : value < 10 ? 2 : value < 100 ? 1 : 0, RoundingMode.DOWN)
            .stripTrailingZeros()
            .toPlainString() + suffix;
    }

    private static ButtonWidget<?> toolbarButton(PanelSyncManager sync, String name, Supplier<IDrawable> icon,
        Supplier<String> tooltip, Consumer<MouseData> action) {
        var handler = new InteractionSyncHandler().setOnMousePressed(
            mouse -> {
                if (!mouse.isClient() && (mouse.mouseButton == 0 || mouse.mouseButton == 1)) action.accept(mouse);
            });
        sync.syncValue(name, handler);
        return ButtonPurpose.ICON.apply(new ButtonWidget<>())
            .overlay(
                (context, x, y, w, h, theme) -> icon.get()
                    .draw(context, x + 1, y + 2, 16, 16, theme))
            .syncHandler(handler)
            .tooltip(t -> t.addLine(IKey.dynamic(tooltip)))
            .size(18, 20)
            .excludeAreaInRecipeViewer();
    }

    private static String yesNo(boolean value) {
        // #tr gui.advancedio.yes
        // # On
        // # zh_CN 开
        if (value) return IKey.lang("gui.advancedio.yes")
            .get();
        // #tr gui.advancedio.no
        // # Off
        // # zh_CN 关
        return IKey.lang("gui.advancedio.no")
            .get();
    }

    private static String schedulingLabel(int value) {
        // #tr gui.advancedio.scheduling_default
        // # Scheduling: first to last
        // # zh_CN 调度模式：从前往后
        if (value == 0) return IKey.lang("gui.advancedio.scheduling_default")
            .get();
        // #tr gui.advancedio.scheduling_roundrobin
        // # Scheduling: round-robin
        // # zh_CN 调度模式：轮询
        if (value == 1) return IKey.lang("gui.advancedio.scheduling_roundrobin")
            .get();
        // #tr gui.advancedio.scheduling_random
        // # Scheduling: random
        // # zh_CN 调度模式：随机
        return IKey.lang("gui.advancedio.scheduling_random")
            .get();
    }

    private static String redstoneLabel(int value) {
        // #tr gui.advancedio.redstone_high
        // # Redstone: run with signal
        // # zh_CN 红石：有信号运行
        if (value == RedstoneMode.HIGH_SIGNAL.ordinal()) return IKey.lang("gui.advancedio.redstone_high")
            .get();
        // #tr gui.advancedio.redstone_low
        // # Redstone: run without signal
        // # zh_CN 红石：无信号运行
        if (value == RedstoneMode.LOW_SIGNAL.ordinal()) return IKey.lang("gui.advancedio.redstone_low")
            .get();
        // #tr gui.advancedio.redstone_ignore
        // # Redstone: ignore signal
        // # zh_CN 红石：忽略信号
        return IKey.lang("gui.advancedio.redstone_ignore")
            .get();
    }

    /** The custom server handler owns changes; client copies hold only the synchronized display item. */
    private static final class SampleItemHandler implements IItemHandlerModifiable {

        private final PartAdvancedIOBus part;
        private final ItemStack[] client = new ItemStack[PartAdvancedIOBus.CONFIG_SLOTS];

        private SampleItemHandler(PartAdvancedIOBus part) {
            this.part = part;
        }

        public int getSlots() {
            return client.length;
        }

        public int getSlotLimit(int slot) {
            return 1;
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return null;
        }

        public void setStackInSlot(int slot, ItemStack stack) {
            if (part.getTile()
                .getWorldObj().isRemote) client[slot] = stack;
        }

        public ItemStack getStackInSlot(int slot) {
            if (part.getTile()
                .getWorldObj().isRemote) return client[slot];
            var key = part.filter(slot);
            if (key instanceof IAEFluidStack fluid) {
                // Render a fluid marker, without the real packet's delivery/unpacking tooltip.
                ItemStack display = ItemFluidPacket.newDisplayStack(fluid.getFluidStack());
                ItemFluidPacket.setFluidAmount(display, fluid.getStackSize());
                return display;
            }
            if (key instanceof IAEItemStack item) {
                ItemStack icon = item.getItemStack();
                icon.stackSize = 1;
                return icon;
            }
            return null;
        }
    }

    /** Ghost filters never consume cursor items; the middle button opens the native-sized amount page. */
    private static final class SampleSyncHandler extends PhantomItemSlotSH {

        private final PartAdvancedIOBus part;
        private final int index;

        private SampleSyncHandler(ModularSlot slot, PartAdvancedIOBus part, int index) {
            super(slot);
            this.part = part;
            this.index = index;
        }

        /**
         * NEI tests ghost eligibility before sending its sample packet. The display inventory deliberately rejects
         * real insertion, so its simulated insertion result cannot validate a ghost. Capacity is checked again
         * by {@link #phantomClick(MouseData, ItemStack)} on the server.
         *
         * @param stack NEI's dragged sample, never a real inventory transfer
         * @return whether the sample contains an item to mark
         */
        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack != null && stack.stackSize > 0;
        }

        @Override
        protected void phantomClick(MouseData mouse, ItemStack cursor) {
            if (index >= part.availableSlots()) return;
            if (mouse.mouseButton == 2) {
                if (part.filter(index) != null) AdvancedIOGui.FACTORY.open(getSyncManager().getPlayer(), part, index);
            } else if (mouse.shift || cursor == null) {
                part.setFilter(index, null);
            } else {
                FluidStack fluid = ItemFluidPacket.getFluidStack(cursor);
                if (fluid == null) fluid = GTUtility.getFluidFromDisplayStack(cursor);
                if (fluid == null && FluidDropCompat.isFluidDrop(cursor)) fluid = FluidDropCompat.getFluidStack(cursor);
                if (fluid == null) fluid = FluidContainerRegistry.getFluidForFilledItem(cursor);
                if (fluid == null && cursor.getItem() instanceof IFluidContainerItem container)
                    fluid = container.getFluid(cursor);
                IAEStack<?> sample = fluid == null ? AEItemStack.create(cursor) : AEFluidStack.create(fluid);
                if (sample != null && part.filter(index) != null
                    && part.filter(index)
                        .isSameType(sample)) {
                    long amount = part.filter(index)
                        .getStackSize() + (mouse.mouseButton == 1 ? -1 : 1) * sample.getStackSize();
                    sample = amount <= 0 ? null : sample.setStackSize(amount);
                }
                part.setFilter(index, sample);
            }
        }

        @Override
        protected void phantomScroll(MouseData mouse) {
            if (index >= part.availableSlots() || part.filter(index) == null) return;
            long amount = part.filter(index)
                .getStackSize() + (mouse.ctrl ? 64L : mouse.shift ? 16L : 1L) * mouse.mouseButton;
            part.setFilter(
                index,
                amount <= 0 ? null
                    : part.filter(index)
                        .copy()
                        .setStackSize(amount));
        }
    }

    /** Position-and-face UI locator with distance, part identity and AE security checks on the server. */
    public static final class Factory extends AbstractUIFactory<Factory.Data> {

        private Factory() {
            super(ModList.GTNeverReset.getResourcePath("advanced_io"));
        }

        public void open(EntityPlayer player, PartAdvancedIOBus part) {
            open(player, part, -1);
        }

        /** Opens the upstream amount sub-screen only for an enabled, populated ghost slot. */
        public void open(EntityPlayer player, PartAdvancedIOBus part, int amountSlot) {
            if (!(player instanceof EntityPlayerMP serverPlayer) || player instanceof FakePlayer) return;
            if (amountSlot >= 0 && (amountSlot >= part.availableSlots() || part.filter(amountSlot) == null)) return;
            var tile = part.getTile();
            var data = new Data(
                player,
                tile.xCoord,
                tile.yCoord,
                tile.zCoord,
                part.getSide()
                    .ordinal(),
                amountSlot);
            if (canInteractWith(player, data)) GuiManager.open(this, data, serverPlayer);
        }

        @Override
        public @Nonnull IGuiHolder<Data> getGuiHolder(Data data) {
            if (data.part == null) throw new IllegalStateException("Advanced IO bus no longer exists");
            return (guiData, sync, settings) -> {
                if (guiData.isClient()) settings.useTheme(ScreenThemeConfig.advancedIOBus.themeId);
                return data.amountSlot < 0 ? AdvancedIOGui.build(data.part, sync)
                    : AdvancedIOGui.amountPanel(data.part, sync, data.amountSlot);
            };
        }

        @Override
        @SideOnly(Side.CLIENT)
        public ModularScreen createScreen(Data data, ModularPanel panel) {
            return new ModularScreen(ModList.GTNeverReset.getID(), panel);
        }

        @Override
        public boolean canInteractWith(EntityPlayer player, Data data) {
            return super.canInteractWith(player, data) && data.getSquaredDistance(player) <= 64
                && data.part != null
                && data.resolve() == data.part
                && (player.worldObj.isRemote
                    || Util.hasPermission(player, SecurityPermissions.BUILD, (IPart) data.part));
        }

        @Override
        public void writeGuiData(Data data, PacketBuffer buffer) {
            buffer.writeInt(data.getX());
            buffer.writeInt(data.getY());
            buffer.writeInt(data.getZ());
            buffer.writeByte(data.side);
            buffer.writeInt(data.amountSlot);
        }

        @Override
        public @Nonnull Data readGuiData(EntityPlayer player, PacketBuffer buffer) {
            return new Data(
                player,
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readUnsignedByte(),
                buffer.readInt());
        }

        /** Captures the original part so replacing a cable cannot redirect an already open container. */
        public static final class Data extends PosGuiData {

            private final int side;
            private final PartAdvancedIOBus part;
            private final int amountSlot;

            Data(EntityPlayer player, int x, int y, int z, int side, int amountSlot) {
                super(player, x, y, z);
                this.side = side;
                this.amountSlot = amountSlot >= 0 && amountSlot < PartAdvancedIOBus.CONFIG_SLOTS ? amountSlot : -1;
                this.part = resolve();
            }

            private PartAdvancedIOBus resolve() {
                if (side < 0 || side >= 6 || !getWorld().blockExists(getX(), getY(), getZ())) return null;
                if (getTileEntity() instanceof IPartHost host
                    && host.getPart(ForgeDirection.getOrientation(side)) instanceof PartAdvancedIOBus bus) return bus;
                return null;
            }
        }
    }
}
