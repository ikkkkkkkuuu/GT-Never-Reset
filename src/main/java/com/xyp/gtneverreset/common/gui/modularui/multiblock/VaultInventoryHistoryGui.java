package com.xyp.gtneverreset.common.gui.modularui.multiblock;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.sizer.Unit;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.xyp.gtneverreset.common.me.vault.VaultInventoryHistory;
import com.xyp.gtneverreset.common.me.vault.VaultInventoryHistory.Sample;
import com.xyp.gtneverreset.config.ScreenThemeConfig;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

final class VaultInventoryHistoryGui {

    static final String NAME = "vault_inventory_history";
    private static final String TEXT = "gtnr.gui.singularity_data_hub.";

    private VaultInventoryHistoryGui() {}

    static ModularPanel build(ModularPanel parent, PanelSyncManager sync, GenericSyncValue<NBTTagCompound, ?> identity,
        Supplier<Sample> sampleSource, Runnable stopSampling) {
        GenericSyncValue<Sample, ?> sample = GenericSyncValue.builder(Sample.class)
            .getter(sampleSource)
            .serializer((buffer, value) -> {
                buffer.writeLong(value.seconds());
                buffer.writeLong(value.amount());
                buffer.writeBoolean(value.available());
            })
            .deserializer(buffer -> new Sample(buffer.readLong(), buffer.readLong(), buffer.readBoolean()))
            .equals((first, second) -> first == second)
            .copyImmutable()
            .build();

        sync.syncValue("vaultHistoryIdentity", identity);
        sync.syncValue("vaultHistorySample", sample);
        HistoryChart chart = new HistoryChart();
        if (sync.isClient()) {
            identity.setChangeListener(() -> chart.setIdentity(identity.getValue()));
            sample.setChangeListener(() -> chart.addSample(sample.getValue()));
        }
        sync.addCloseListener(player -> {
            if (sync.isClient()) {
                identity.setValue(new NBTTagCompound(), false, false);
                sample.setValue(Sample.WAITING, false, false);
                chart.clear();
            } else {
                stopSampling.run();
            }
        });

        ModularPanel panel = new Dialog<>(NAME).size(344, 188);
        if (sync.isClient()) {
            panel.themeOverride(ScreenThemeConfig.vault.themeId);
            panel.width(
                () -> Math.min(
                    344,
                    parent.getArea()
                        .w() - 16),
                Unit.Measure.PIXEL);
            panel.height(
                () -> Math.min(
                    188,
                    parent.getArea()
                        .h() - 16),
                Unit.Measure.PIXEL);
        }
        panel.child(ButtonWidget.panelCloseButton());
        // #tr gtnr.gui.singularity_data_hub.history_title
        // # ME Inventory History
        // # zh_CN ME 库存变化
        panel.child(
            IKey.lang(TEXT + "history_title")
                .asWidget()
                .left(10)
                .right(30)
                .top(8)
                .height(12));
        panel.child(
            IKey.dynamic(chart::selectionName)
                .asWidget()
                .left(10)
                .right(10)
                .top(24)
                .height(12));
        panel.child(
            chart.left(9)
                .right(9)
                .top(41)
                .bottom(28));
        panel.child(
            IKey.dynamic(chart::currentAmount)
                .asWidget()
                .left(10)
                .right(10)
                .bottom(9)
                .height(12));
        return panel;
    }

    private static final class HistoryChart extends Widget<HistoryChart> {

        private static final int AXIS_LEFT = 83;
        private static final int AXIS_TOP = 17;
        private static final int AXIS_RIGHT = 12;
        private static final int AXIS_BOTTOM = 29;
        private static final int MARKS = 4;
        private static final int TIME_MARKS = 6;
        private static final int MIN_AXIS_SECONDS = 30;

        private final Deque<Sample> samples = new ArrayDeque<>();
        private Sample latest = Sample.WAITING;
        private String selectionName = "";
        private boolean fluid;
        private long start;
        private long end = MIN_AXIS_SECONDS;
        private long minimum;
        private long maximum = MARKS;

        private HistoryChart() {
            name("vault_inventory_chart");
            tooltip(
                t -> t.setAutoUpdate(true)
                    .tooltipBuilder(lines -> {
                        Sample point = hoveredSample();
                        if (point == null) return;
                        // #tr gtnr.gui.singularity_data_hub.history_sample_time
                        // # Time %s s
                        // # zh_CN 时间 %s 秒
                        lines.addLine(IKey.lang(TEXT + "history_sample_time", point.seconds()));
                        lines.addLine(IKey.str(formatAmount(point.amount())));
                    }));
        }

        private void setIdentity(NBTTagCompound selection) {
            fluid = selection.getBoolean("fluid");
            var icon = selection.getCompoundTag("icon");
            if (fluid) {
                FluidStack stack = FluidStack.loadFluidStackFromNBT(icon);
                selectionName = stack == null ? "" : stack.getLocalizedName();
            } else {
                ItemStack stack = ItemStack.loadItemStackFromNBT(icon);
                selectionName = stack == null ? "" : stack.getDisplayName();
            }
        }

        private void clear() {
            samples.clear();
            latest = Sample.WAITING;
            selectionName = "";
            fluid = false;
            start = 0;
            end = MIN_AXIS_SECONDS;
            minimum = 0;
            maximum = MARKS;
        }

        private void addSample(Sample point) {
            if (point.seconds() < 0 || point.seconds() <= latest.seconds()) return;
            latest = point;
            start = Math.max(0, point.seconds() - VaultInventoryHistory.WINDOW_SECONDS);
            end = start > 0 ? point.seconds()
                : Math.max(MIN_AXIS_SECONDS, (point.seconds() + TIME_MARKS - 1) / TIME_MARKS * TIME_MARKS);
            samples.addLast(point);
            while (samples.getFirst()
                .seconds() < start) samples.removeFirst();

            long low = Long.MAX_VALUE;
            long high = 0;
            boolean hasPoints = false;
            for (Sample sample : samples) {
                if (!sample.available()) continue;
                hasPoints = true;
                low = Math.min(low, sample.amount());
                high = Math.max(high, sample.amount());
            }
            minimum = hasPoints ? Math.min(Math.max(0, low - 1), Long.MAX_VALUE - MARKS) : 0;
            maximum = Math.max(minimum + MARKS, high == Long.MAX_VALUE ? high : high + 1);
        }

        private String selectionName() {
            return selectionName;
        }

        private String currentAmount() {
            if (selectionName.isEmpty() || latest.seconds() >= 0 && !latest.available()) {
                return StatCollector.translateToLocal(TEXT + "network_unavailable");
            }
            // #tr gtnr.gui.singularity_data_hub.history_waiting
            // # Waiting for first sample
            // # zh_CN 等待首次采样
            if (latest.seconds() < 0) return StatCollector.translateToLocal(TEXT + "history_waiting");
            // #tr gtnr.gui.singularity_data_hub.history_current
            // # Current inventory %s
            // # zh_CN 当前库存 %s
            return StatCollector.translateToLocalFormatted(TEXT + "history_current", formatAmount(latest.amount()));
        }

        private String formatAmount(long value) {
            return String.format(Locale.ROOT, "%,d", value) + (fluid ? " mB" : "");
        }

        private Sample hoveredSample() {
            int x = getContext().getAbsMouseX() - getArea().x;
            int y = getContext().getAbsMouseY() - getArea().y;
            int width = getArea().w() - AXIS_RIGHT - AXIS_LEFT;
            int height = getArea().h() - AXIS_BOTTOM - AXIS_TOP;
            if (width <= 0 || height <= 0
                || x < AXIS_LEFT
                || x > AXIS_LEFT + width
                || y < AXIS_TOP
                || y > AXIS_TOP + height) return null;
            Sample result = null;
            int distance = 8;
            for (Sample sample : samples) {
                if (!sample.available()) continue;
                int dx = Math.abs(x - xAt(sample.seconds(), width));
                if (dx >= distance) continue;
                distance = dx;
                result = sample;
            }
            return result;
        }

        private int xAt(long seconds, int width) {
            return AXIS_LEFT + (int) Math.round((seconds - start) / (double) (end - start) * width);
        }

        private int yAt(long amount, int height) {
            return AXIS_TOP + height - (int) Math.round((amount - minimum) / (double) (maximum - minimum) * height);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            int width = getArea().w();
            int height = getArea().h();
            int plotWidth = width - AXIS_LEFT - AXIS_RIGHT;
            int plotHeight = height - AXIS_TOP - AXIS_BOTTOM;
            if (plotWidth <= 0 || plotHeight <= 0) return;

            AE2Look look = AE2Look.of(this);
            look.drawWell(0, 0, width, height);
            int axisColor = look.listText();
            int gridColor = look.variant() == null ? 0xFF727272 : look.variant().colors.line();
            long span = maximum - minimum;
            int bottom = AXIS_TOP + plotHeight;
            for (int i = 0; i <= MARKS; i++) {
                long amount = minimum + span / MARKS * i + span % MARKS * i / MARKS;
                int y = yAt(amount, plotHeight);
                GuiDraw.drawRect(AXIS_LEFT - 2, y, plotWidth + 2, 1, gridColor);
                text(
                    context,
                    theme,
                    Long.toString(amount),
                    4,
                    y - 5,
                    AXIS_LEFT - 9,
                    axisColor,
                    Alignment.CenterRight,
                    0.65f);
            }
            for (int i = 0; i <= TIME_MARKS; i++) {
                long seconds = start + (end - start) * i / TIME_MARKS;
                int x = xAt(seconds, plotWidth);
                GuiDraw.drawRect(x, AXIS_TOP, 1, plotHeight + 3, gridColor);
                text(
                    context,
                    theme,
                    Long.toString(seconds),
                    x - 11,
                    bottom + 5,
                    22,
                    axisColor,
                    Alignment.Center,
                    0.75f);
            }

            GuiDraw.drawRect(AXIS_LEFT, AXIS_TOP, 1, plotHeight + 1, axisColor);
            GuiDraw.drawRect(AXIS_LEFT, bottom, plotWidth + 1, 1, axisColor);
            // #tr gtnr.gui.singularity_data_hub.history_quantity_axis
            // # Inventory
            // # zh_CN 库存量
            String quantity = StatCollector.translateToLocal(TEXT + "history_quantity_axis") + (fluid ? " (mB)" : "");
            text(context, theme, quantity, 5, 3, AXIS_LEFT - 6, axisColor, Alignment.CenterLeft, 0.8f);
            // #tr gtnr.gui.singularity_data_hub.history_time_axis
            // # Elapsed time (s)
            // # zh_CN 已用时间（秒）
            text(
                context,
                theme,
                StatCollector.translateToLocal(TEXT + "history_time_axis"),
                AXIS_LEFT,
                bottom + 16,
                plotWidth,
                axisColor,
                Alignment.Center,
                0.8f);

            AE2Theme.Variant variant = look.variant();
            int line = variant == null ? 0xFF21528B
                : variant == AE2Theme.Variant.LIGHT ? variant.highlightShadow : variant.highlightTop;
            drawSamples(plotWidth, plotHeight, line);
            Sample hovered = isHovering() ? hoveredSample() : null;
            if (hovered != null) {
                int x = xAt(hovered.seconds(), plotWidth);
                int y = yAt(hovered.amount(), plotHeight);
                GuiDraw.drawRect(x - 2, y - 2, 5, 5, line);
            }
        }

        @SideOnly(Side.CLIENT)
        private void drawSamples(int width, int height, int color) {
            if (samples.isEmpty()) return;
            Platform.setupDrawColor();
            Platform.startDrawing(Platform.DrawMode.QUADS, Platform.VertexFormat.POS_COLOR, buffer -> {
                Sample previous = null;
                for (Sample sample : samples) {
                    if (!sample.available()) {
                        previous = null;
                        continue;
                    }
                    int x = xAt(sample.seconds(), width);
                    int y = yAt(sample.amount(), height);
                    GuiDraw.drawRectRaw(buffer, x, y, x + 2, y + 2, color);
                    if (previous != null && sample.seconds() - previous.seconds() <= 2) {
                        int beforeX = xAt(previous.seconds(), width);
                        int beforeY = yAt(previous.amount(), height);
                        GuiDraw.drawRectRaw(buffer, beforeX, beforeY, x + 1, beforeY + 1, color);
                        GuiDraw.drawRectRaw(buffer, x, Math.min(y, beforeY), x + 1, Math.max(y, beforeY) + 1, color);
                    }
                    previous = sample;
                }
            });
            Platform.endDrawColor();
        }

        @SideOnly(Side.CLIENT)
        private static void text(ModularGuiContext context, WidgetThemeEntry<?> theme, String text, int x, int y,
            int width, int color, Alignment alignment, float scale) {
            IKey.str(text)
                .color(color)
                .alignment(alignment)
                .scale(scale)
                .draw(context, x, y, width, 10, theme.getTheme());
        }
    }
}
