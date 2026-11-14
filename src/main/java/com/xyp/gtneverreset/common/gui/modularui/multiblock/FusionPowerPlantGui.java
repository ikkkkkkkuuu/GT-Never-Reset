package com.xyp.gtneverreset.common.gui.modularui.multiblock;

import java.util.Locale;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.common.machine.multiblock.FusionPowerPlant;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public final class FusionPowerPlantGui extends MTEMultiBlockBaseGui<FusionPowerPlant> {

    private static final int LABEL = 0xFFAAAAAA, VALUE = 0xFFFFFFFF, OK = 0xFF55FF55;
    private static final int WARN = 0xFFFFAA00, BAD = 0xFFFF5555, EDGE = 0xFF4A4D55, TRACK = 0xFF1E2026;
    private IntSyncValue temperature;
    private IntSyncValue generation;
    private IntSyncValue export;
    private IntSyncValue cooling;
    private IntSyncValue demand;
    private IntSyncValue target;
    private IntSyncValue deuterium;
    private IntSyncValue tritium;
    private IntSyncValue decay;
    private LongSyncValue ignition;
    private StringSyncValue status;
    private boolean info;

    public FusionPowerPlantGui(FusionPowerPlant machine) {
        super(machine);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager sync) {
        super.registerSyncValues(sync);
        temperature = new IntSyncValue(() -> (int) Math.round(multiblock.getTemperature()));
        generation = new IntSyncValue(multiblock::getGeneratedEUt);
        export = new IntSyncValue(multiblock::getExportedEUt);
        cooling = new IntSyncValue(multiblock::getCoolantUsed);
        demand = new IntSyncValue(multiblock::getCoolantDemand);
        target = new IntSyncValue(multiblock::getTargetPower, multiblock::setTargetPower).allowC2S();
        deuterium = new IntSyncValue(multiblock::getDeuteriumStored);
        tritium = new IntSyncValue(multiblock::getTritiumStored);
        decay = new IntSyncValue(() -> (int) Math.round(multiblock.getDecayHeat() * 1000));
        ignition = new LongSyncValue(multiblock::getIgnitionEnergy);
        status = new StringSyncValue(multiblock::getStatusKey);
        sync.syncValue("fusionTemperature", temperature);
        sync.syncValue("fusionGeneration", generation);
        sync.syncValue("fusionExport", export);
        sync.syncValue("fusionCooling", cooling);
        sync.syncValue("fusionDemand", demand);
        sync.syncValue("fusionTarget", target);
        sync.syncValue("fusionDeuterium", deuterium);
        sync.syncValue("fusionTritium", tritium);
        sync.syncValue("fusionDecay", decay);
        sync.syncValue("fusionIgnition", ignition);
        sync.syncValue("fusionStatus", status);
        sync.syncValue(
            "fusionStart",
            new InteractionSyncHandler().setOnMousePressed(
                mouse -> { if (!mouse.isClient() && mouse.mouseButton == 0) multiblock.requestStart(); }));
        sync.syncValue(
            "fusionStop",
            new InteractionSyncHandler().setOnMousePressed(
                mouse -> { if (!mouse.isClient() && mouse.mouseButton == 0) multiblock.requestStop(); }));
    }

    @Override
    protected int getBasePanelHeight() {
        return 227;
    }

    @Override
    protected int getTerminalRowHeight() {
        return 118;
    }

    @Override
    protected ModularPanel getBasePanel(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return super.getBasePanel(data, sync, settings).topRel(0.5F, 7, 0.5F);
    }

    @Override
    protected ParentWidget<?> createTerminalParentWidget(ModularPanel panel, PanelSyncManager sync) {
        return new ParentWidget<>().size(getTerminalWidgetWidth(), getTerminalWidgetHeight())
            .widgetTheme(GTWidgetThemes.BACKGROUND_TERMINAL)
            .child(
                new InstrumentWidget().pos(4, 4)
                    .size(180, 110)
                    .tooltip(t -> {
                        t.addLine(StatCollector.translateToLocal(status.getStringValue()));
                        // #tr gtnr.fusion.gui.temperature_tip
                        // # Warning %s K; shutdown %s K; explosion %s K.
                        // # zh_CN 警戒 %s K；停堆 %s K；爆炸 %s K。
                        t.addLine(
                            tr(
                                "gtnr.fusion.gui.temperature_tip",
                                FusionPowerPlant.WARNING_K,
                                FusionPowerPlant.SCRAM_K,
                                FusionPowerPlant.EXPLOSION_K));
                        // #tr gtnr.fusion.gui.coolant_tip
                        // # Coolant flow: %s / %s L/s required. Hot coolant must be able to leave.
                        // # zh_CN 冷却流量：%s / 需求 %s L/s；热冷却液必须能够排出。
                        t.addLine(tr("gtnr.fusion.gui.coolant_tip", cooling.getIntValue(), demand.getIntValue()));
                    })
                    .tooltipAutoUpdate(true));
    }

    @Override
    protected Flow createLeftPanelGapRow(ModularPanel panel, PanelSyncManager sync) {
        // #tr gtnr.fusion.gui.target
        // # Power
        // # zh_CN 功率
        return Flow.row()
            .coverChildrenWidth()
            .fullHeight()
            .childPadding(1)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(
                IKey.lang("gtnr.fusion.gui.target")
                    .color(0xFF404040)
                    .asWidget()
                    .marginRight(2)
                    .heightRel(1))
            .child(stepButton(-5))
            .child(
                new TextFieldWidget().numbersLong(0, 100)
                    .syncHandler("fusionTarget")
                    .setTextAlignment(Alignment.Center)
                    .size(32, 14))
            .child(stepButton(5))
            .child(
                IKey.str("%")
                    .color(0xFF404040)
                    .asWidget()
                    .marginLeft(2)
                    .heightRel(1));
    }

    private IWidget stepButton(int step) {
        return new ButtonWidget<>().size(9, 14)
            .overlay(IKey.str(step < 0 ? "-" : "+"))
            .onMousePressed(button -> {
                target.setIntValue(Math.max(0, Math.min(100, target.getIntValue() + step * (button == 1 ? 5 : 1))));
                return true;
            });
    }

    @Override
    protected Flow createRightPanelGapRow(ModularPanel panel, PanelSyncManager sync) {
        // #tr gtnr.fusion.gui.info
        // # Operating limits and fuel / coolant requirements
        // # zh_CN 查看运行限制与燃料、冷却要求
        return super.createRightPanelGapRow(panel, sync).child(
            new ButtonWidget<>().size(18, 18)
                .overlay(
                    IKey.str("i")
                        .color(0xFF00AAFF))
                .onMousePressed(button -> {
                    info = !info;
                    return true;
                })
                .tooltip(t -> t.addLine(IKey.lang("gtnr.fusion.gui.info"))));
    }

    @Override
    protected Flow createButtonColumn(ModularPanel panel, PanelSyncManager sync) {
        // #tr gtnr.fusion.gui.start
        // # Ignite (requires a formed, cool reactor)
        // # zh_CN 启动（要求结构成型且温度低于警戒值）
        // #tr gtnr.fusion.gui.stop
        // # Shutdown (keep coolant flowing)
        // # zh_CN 停堆（仍需持续供应冷却液）
        return Flow.column()
            .width(18)
            .right(2)
            .childPadding(0)
            .child(
                new ButtonWidget<>().size(18, 18)
                    .syncHandler("fusionStart")
                    .overlay(GTGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_ON)
                    .tooltip(t -> t.addLine(IKey.lang("gtnr.fusion.gui.start"))))
            .child(createStructureUpdateButton(sync))
            .child(
                new ButtonWidget<>().size(18, 18)
                    .syncHandler("fusionStop")
                    .overlay(GTGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_OFF)
                    .tooltip(t -> t.addLine(IKey.lang("gtnr.fusion.gui.stop"))))
            .child(
                new ItemSlot()
                    .slot(
                        new ModularSlot(multiblock.inventoryHandler, multiblock.getControllerSlotIndex())
                            .singletonSlotGroup())
                    .marginTop(4));
    }

    private static String tr(String key, Object... arguments) {
        return StatCollector.translateToLocalFormatted(key, arguments);
    }

    private final class InstrumentWidget extends Widget<InstrumentWidget> {

        @Override
        @SideOnly(Side.CLIENT)
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            super.draw(context, theme);
            drawHeader();
            drawTemperature();
            if (info) {
                drawInfo();
                return;
            }
            // #tr gtnr.fusion.gui.cooling
            // # Coolant
            // # zh_CN 冷却
            drawBar(
                26,
                tr("gtnr.fusion.gui.cooling"),
                cooling.getIntValue() / 400.0,
                cooling.getIntValue() + " L/s",
                0xFF2F6B8F,
                0xFF7CD8FF,
                demand.getIntValue() / 400.0);
            // #tr gtnr.fusion.gui.ignition
            // # Ignition
            // # zh_CN 点火
            drawBar(
                36,
                tr("gtnr.fusion.gui.ignition"),
                ignition.getLongValue() / (double) FusionPowerPlant.IGNITION_EU,
                ignition.getLongValue() * 100 / FusionPowerPlant.IGNITION_EU + "%",
                0xFF7A3A10,
                0xFFFFA040,
                -1);
            // #tr gtnr.fusion.gui.decay
            // # Residual
            // # zh_CN 余热
            stat(
                46,
                tr("gtnr.fusion.gui.decay"),
                String.format(Locale.ROOT, "%.3f K/t", decay.getIntValue() / 1000.0),
                0xFF55FFFF);
            // #tr gtnr.fusion.gui.power
            // # Power
            // # zh_CN 发电
            stat(56, tr("gtnr.fusion.gui.power"), generation.getIntValue() + " EU/t", OK);
            // #tr gtnr.fusion.gui.export
            // # Wireless
            // # zh_CN 无线上网
            stat(66, tr("gtnr.fusion.gui.export"), export.getIntValue() + " EU/t", OK);
            // #tr gtnr.fusion.gui.fuel
            // # D / T
            // # zh_CN 氘 / 氚
            stat(76, tr("gtnr.fusion.gui.fuel"), deuterium.getIntValue() + " / " + tritium.getIntValue() + " L", VALUE);
            // #tr gtnr.fusion.gui.limits
            // # Warn / stop
            // # zh_CN 警戒 / 停堆
            stat(
                86,
                tr("gtnr.fusion.gui.limits"),
                FusionPowerPlant.WARNING_K + " / " + FusionPowerPlant.SCRAM_K + " K",
                LABEL);
            text(
                cut(StatCollector.translateToLocal(status.getStringValue()), 180),
                0,
                101,
                generation.getIntValue() > 0 && cooling.getIntValue() >= demand.getIntValue() ? LABEL : WARN);
        }

        @SideOnly(Side.CLIENT)
        private void drawHeader() {
            String badge;
            int fill;
            String state = status.getStringValue();
            if (state.endsWith(".structure") || state.endsWith(".trip")) {
                // #tr gtnr.fusion.gui.badge.fault
                // # FAULT
                // # zh_CN 故障
                badge = tr("gtnr.fusion.gui.badge.fault");
                fill = 0xFFD23C3C;
            } else if (generation.getIntValue() > 0) {
                // #tr gtnr.fusion.gui.badge.running
                // # RUNNING
                // # zh_CN 运行
                badge = tr("gtnr.fusion.gui.badge.running");
                fill = 0xFF55C85A;
            } else if (state.endsWith(".stopped")) {
                // #tr gtnr.fusion.gui.badge.cooling
                // # COOLING
                // # zh_CN 冷却
                badge = tr("gtnr.fusion.gui.badge.cooling");
                fill = 0xFF48C9C9;
            } else {
                // #tr gtnr.fusion.gui.badge.waiting
                // # WAITING
                // # zh_CN 等待
                badge = tr("gtnr.fusion.gui.badge.waiting");
                fill = 0xFFE8DA50;
            }
            int width = width(badge) + 5;
            box(0, 0, width, 11, EDGE, fill);
            text(badge, 3, 2, 0xFF101010);
            // #tr gtnr.fusion.gui.subject
            // # D-T Fusion
            // # zh_CN 氘氚聚变
            text(tr("gtnr.fusion.gui.subject"), width + 4, 2, VALUE);
            right(target.getIntValue() + "%", 180, 2, LABEL);
            GuiDraw.drawRect(0, 13, 180, 1, 0xFF2A2D35);
        }

        @SideOnly(Side.CLIENT)
        private void drawTemperature() {
            // #tr gtnr.fusion.gui.temperature
            // # Temp
            // # zh_CN 温度
            text(tr("gtnr.fusion.gui.temperature"), 0, 17, VALUE);
            int x = 27, y = 17, width = 105;
            box(x, y, width, 7, EDGE, 0xFF4A1414);
            int warning = temperatureX(FusionPowerPlant.WARNING_K, width);
            int scram = temperatureX(FusionPowerPlant.SCRAM_K, width);
            GuiDraw.drawRect(x + 1, y + 1, warning, 5, 0xFF1F6B3A);
            GuiDraw.drawRect(x + 1 + warning, y + 1, scram - warning, 5, 0xFF6B6020);
            int marker = x + 1 + temperatureX(temperature.getIntValue(), width);
            GuiDraw.drawRect(marker - 2, y - 1, 4, 9, 0xFF000000);
            GuiDraw.drawRect(marker - 1, y - 1, 2, 9, VALUE);
            right(
                temperature.getIntValue() + " K",
                180,
                17,
                temperature.getIntValue() >= FusionPowerPlant.SCRAM_K ? BAD
                    : temperature.getIntValue() >= FusionPowerPlant.WARNING_K ? WARN : OK);
        }

        private int temperatureX(int kelvin, int width) {
            return Math.max(
                0,
                Math.min(
                    width - 2,
                    (kelvin - FusionPowerPlant.AMBIENT_K) * (width - 2)
                        / (FusionPowerPlant.EXPLOSION_K - FusionPowerPlant.AMBIENT_K)));
        }

        @SideOnly(Side.CLIENT)
        private void drawInfo() {
            stat(26, tr("gtnr.fusion.gui.ignition"), FusionPowerPlant.IGNITION_EU + " EU", VALUE);
            stat(36, tr("gtnr.fusion.gui.fuel"), "1 / 1 L/s", VALUE);
            stat(
                46,
                tr("gtnr.fusion.gui.cooling"),
                "IC2 / " + FusionPowerPlant.MAX_COOLANT_PER_TICK * 20 + " L/s",
                VALUE);
            stat(
                56,
                tr("gtnr.fusion.gui.limits"),
                FusionPowerPlant.WARNING_K + " / " + FusionPowerPlant.SCRAM_K + " K",
                WARN);
            // #tr gtnr.fusion.gui.explosion
            // # Explosion
            // # zh_CN 爆炸
            stat(66, tr("gtnr.fusion.gui.explosion"), FusionPowerPlant.EXPLOSION_K + " K", BAD);
            // #tr gtnr.fusion.gui.keep_cooling
            // # Keep cooling after shutdown!
            // # zh_CN 停堆后仍需持续冷却！
            text(tr("gtnr.fusion.gui.keep_cooling"), 0, 81, WARN);
            // #tr gtnr.fusion.gui.owner_network
            // # EU goes to the owner's grid.
            // # zh_CN 电力直接存入放置者的无线电网。
            text(tr("gtnr.fusion.gui.owner_network"), 0, 96, LABEL);
        }

        @SideOnly(Side.CLIENT)
        private void drawBar(int y, String label, double fraction, String value, int from, int to, double tick) {
            text(label, 0, y + 1, LABEL);
            int x = Math.max(52, width(label) + 3);
            int width = Math.max(24, Math.min(132, 180 - width(value) - 3) - x);
            box(x, y + 1, width, 7, EDGE, TRACK);
            float fill = (float) ((width - 2) * Math.max(0, Math.min(1, fraction)));
            if (fill > 0) GuiDraw.drawRect(x + 1, y + 2, fill, 5, from, to, from, to);
            if (tick >= 0) GuiDraw.drawRect(x + 1 + (int) ((width - 2) * Math.min(1, tick)), y, 1, 9, 0xFFFFFF55);
            right(value, 180, y + 1, VALUE);
        }

        @SideOnly(Side.CLIENT)
        private void stat(int y, String label, String value, int color) {
            text(label, 0, y + 1, LABEL);
            right(value, 180, y + 1, color);
        }

        @SideOnly(Side.CLIENT)
        private void box(int x, int y, int width, int height, int edge, int fill) {
            GuiDraw.drawRect(x, y, width, height, edge);
            GuiDraw.drawRect(x + 1, y + 1, width - 2, height - 2, fill);
        }

        @SideOnly(Side.CLIENT)
        private int width(String text) {
            return TextRenderer.getFontRenderer()
                .getStringWidth(text);
        }

        @SideOnly(Side.CLIENT)
        private String cut(String text, int width) {
            return width(text) <= width ? text
                : TextRenderer.getFontRenderer()
                    .trimStringToWidth(text, width - 9) + "...";
        }

        @SideOnly(Side.CLIENT)
        private void right(String text, int edge, int y, int color) {
            text(text, edge - width(text), y, color);
        }

        @SideOnly(Side.CLIENT)
        private void text(String text, int x, int y, int color) {
            Platform.setupDrawFont();
            TextRenderer.getFontRenderer()
                .drawString(text, x, y, color, false);
        }
    }
}
