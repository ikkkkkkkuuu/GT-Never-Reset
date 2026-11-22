package com.xyp.gtnrlib.gui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.EnumValue;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.value.LongValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.gtnewhorizon.gtnhlib.config.Config;
import com.xyp.gtnrlib.gui.layout.Layout;
import com.xyp.gtnrlib.gui.settings.BlockPosFields;
import com.xyp.gtnrlib.gui.settings.SettingControls;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Icon;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.theme.ScreenTheme;
import com.xyp.gtnrlib.gui.theme.ae2.AE2ButtonPainter;
import com.xyp.gtnrlib.gui.theme.ae2.AE2CheckboxPainter;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Legacy;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;
import com.xyp.gtnrlib.gui.theme.ae2.AE2PanelPainter;
import com.xyp.gtnrlib.gui.theme.ae2.AE2SettingIcon;
import com.xyp.gtnrlib.gui.theme.ae2.IconSource;
import com.xyp.gtnrlib.gui.wheel.WheelStyle;
import com.xyp.gtnrlib.gui.widget.AE2Button;
import com.xyp.gtnrlib.gui.widget.AECheckbox;
import com.xyp.gtnrlib.gui.widget.Breadcrumb;
import com.xyp.gtnrlib.gui.widget.CloseButton;
import com.xyp.gtnrlib.gui.widget.CycleDropdown;
import com.xyp.gtnrlib.gui.widget.Divider;
import com.xyp.gtnrlib.gui.widget.FadingNotice;
import com.xyp.gtnrlib.gui.widget.FillBar;
import com.xyp.gtnrlib.gui.widget.GTNRListWidget;
import com.xyp.gtnrlib.gui.widget.HandleScrollData;
import com.xyp.gtnrlib.gui.widget.IconButton;
import com.xyp.gtnrlib.gui.widget.IconRowList;
import com.xyp.gtnrlib.gui.widget.LayoutTheme;
import com.xyp.gtnrlib.gui.widget.ListRow;
import com.xyp.gtnrlib.gui.widget.NumberEntryWidget;
import com.xyp.gtnrlib.gui.widget.PageTabs;
import com.xyp.gtnrlib.gui.widget.ProgressBar;
import com.xyp.gtnrlib.gui.widget.Scrollbar;
import com.xyp.gtnrlib.gui.widget.SettingToggleButton;
import com.xyp.gtnrlib.gui.widget.SlideSwitch;
import com.xyp.gtnrlib.gui.widget.StepSlider;
import com.xyp.gtnrlib.gui.widget.TabButton;
import com.xyp.gtnrlib.gui.widget.UpgradesPanel;
import com.xyp.gtnrlib.gui.widget.ValueStepper;
import com.xyp.gtnrlib.gui.widget.VerticalButtonBar;

/**
 * Builds the AE2 widgets the way a GUI opened by the server builds them on the server, without drawing anything.
 * {@link ServerSideLoadingTest} runs it inside a {@link ServerClassLoader}.
 */
public class ServerWidgetScenario implements Runnable {

    @Override
    public void run() {
        for (ScreenTheme theme : ScreenTheme.values()) AE2Look.of(AE2Theme.variant(theme.themeId));
        AE2Look.skin(AE2Look.KEY.getFullName());
        new AE2Look.Marker.Builder().skin(Variant.DARK.folder);
        Variant light = AE2Theme.variant(AE2Theme.LIGHT_ID);
        ButtonWidget<?> plain = new ButtonWidget<>();
        plain.background(AE2ButtonPainter.button(plain, () -> true, () -> false))
            .overlay(AE2ButtonPainter.label(plain, () -> "Apply", () -> true, () -> false));
        AE2ButtonPainter.iconButton(plain, () -> Icon.ADD, () -> true, () -> false);
        AE2ButtonPainter.tabButton(plain, () -> Icon.GEAR, () -> true);
        AE2ButtonPainter.pageTab(plain, () -> Icon.FILE, () -> true);
        AE2CheckboxPainter.checkbox(plain, () -> true, () -> "Auto", () -> true);
        AE2CheckboxPainter.radio(plain, () -> true, () -> "Tier", () -> true);
        AE2PanelPainter.listRow(plain, () -> false);
        AE2PanelPainter.well(plain, 60);
        AE2PanelPainter.panelBackground(plain, 0, 0);
        new AE2Button(() -> "Apply", () -> {}).available(() -> false)
            .selected(() -> false);
        new IconButton(() -> Icon.REFRESH, () -> {});
        new NumberEntryWidget().bounds(1, 64)
            .value(3)
            .available(() -> false);
        new NumberEntryWidget().size(64, 16);
        new ListRow(() -> "Row").checkbox(() -> true, () -> {})
            .available(() -> false);
        ListRow.header(() -> "Header");
        new VerticalButtonBar().button(() -> Icon.GEAR, () -> "Settings", () -> {})
            .toggle(() -> Icon.VISIBLE, () -> "Show", () -> true, () -> {});
        new ValueStepper(() -> "LV", step -> {});
        new GTNRListWidget<>().scrollDirection(
            new HandleScrollData(true).look(AE2Look.of(light))
                .rows(IconRowList.ROW));
        new HandleScrollData(null).look(AE2Look.plain());
        new CycleDropdown<>("menu", String.class).choices(Arrays.asList("a", "b"));
        List<IconRowList.Row> rows = Collections.singletonList(
            IconRowList.Row.checkbox("Auto", true, () -> {})
                .withAvailable(false));
        new IconRowList(() -> rows);
        new SlideSwitch().value(() -> true)
            .available(() -> false);
        new StepSlider().range(0, 1, 10)
            .vertical(true)
            .available(() -> false);
        new ValueStepper(() -> "MV", step -> {}).available(() -> false);
        new CycleDropdown<>("menu_off", String.class).available(() -> false);
        new VerticalButtonBar().button(() -> IconSource.of(IDrawable.EMPTY), () -> "Custom", () -> {})
            .available(() -> false);
        int[] openPage = { 0 };
        PageTabs tabs = new PageTabs(() -> openPage[0], page -> openPage[0] = page)
            .tab(() -> Icon.GEAR, () -> "Settings")
            .tab(() -> Icon.FILE, () -> "Plan")
            .available(() -> false);
        tabs.page(1, new ParentWidget<>());
        tabs.tabSize(30, 26);
        new VerticalButtonBar().button(() -> Icon.ADD, () -> "Add", () -> {})
            .buttonSize(24, 24);
        new IconRowList(() -> rows).rowHeight(24)
            .numberWidth(48);
        new ListRow(() -> "Tall row").height(26)
            .control(new NumberEntryWidget(), 64, 16);
        int[] firstRow = { 0 };
        new Scrollbar(() -> firstRow[0], () -> 5, row -> firstRow[0] = row).step(2)
            .small(true)
            .plainBar(0xFF808090, () -> 3);
        new TabButton(() -> Icon.GEAR, () -> {});
        new SettingToggleButton().value(new EnumValue<>(ScreenTheme.class, ScreenTheme.AE2_LIGHT))
            .title(IKey.str("Theme"))
            .state(ScreenTheme.AE2_LIGHT, Icon.VISIBLE, IKey.str("Light"))
            .available(() -> false);
        new SettingToggleButton().value(new BoolValue(true))
            .state(true, Icon.VISIBLE, IKey.str("Shown"))
            .state(false, Icon.INVISIBLE, IKey.str("Hidden"));
        new SettingToggleButton().value(new IntValue(0))
            .stateCount(3)
            .state(2, Icon.ADD);
        new ProgressBar().value(new DoubleValue(0.4));
        AE2Theme.sliderHandles(new ParentWidget<>().child(new SliderWidget()), AE2Theme.LIGHT_ID);
        AE2Theme.useMenuTheme(AE2Theme.DARK_ID, "gtnrlib", "server_scenario", "menu");
        AE2Legacy.legacyStates(AE2Look.of(light));
        AE2Legacy.legacyTerminal(AE2Look.plain());
        WheelStyle.builder()
            .skin(light);
        AE2Look.plain()
            .scrollbarWidth();
        new ListRow(() -> "Stepper row").control(new ValueStepper(() -> "HV", step -> {}), 84, 16);
        new FillBar().value(() -> 0.5);
        new Divider().color(0xFF808080);
        new CloseButton();
        new Breadcrumb().segments(() -> Arrays.asList("Root", "Page"))
            .onSelect(depth -> {})
            .colors(0xFF404040, 0xFF1C4CA6, 0xFF777777);
        new FadingNotice().duration(2000)
            .background(0x80000000);
        Layout settings = Layout.rows(LayoutTheme.DEFAULT, 200);
        SettingControls.slider(null, Options.class, "volume", 0.1, volume -> String.valueOf(volume))
            .widget(settings, LayoutTheme.DEFAULT, 80, 24, () -> {});
        SettingControls.toggle(null, Options.class, "shown")
            .widget(settings, LayoutTheme.DEFAULT, 80, 24, () -> {});
        new BlockPosFields(0.5, 64, -3.5, 1000, 0, 255).x();
        new VerticalButtonBar().add(
            new SettingToggleButton().value(new BoolValue(true))
                .state(true, AE2SettingIcon.AUTO_EXPORT_ON, IKey.str("On"))
                .state(false, AE2SettingIcon.AUTO_EXPORT_OFF, IKey.str("Off")));
        new NumberEntryWidget().value(new LongValue(3));
        new AECheckbox(() -> "Auto", new BoolValue(true)).available(() -> false);
        AECheckbox.radio(() -> "First", new IntValue(0), 0);
        new Scrollbar(() -> 0, () -> 4, row -> {}).height(40);
        TabButton.back(() -> {})
            .pos(0, TabButton.BACK_TOP);
        new CloseButton().action(() -> {});
        new UpgradesPanel().slot(new ItemSlot())
            .slot(new ItemSlot())
            .perColumn(1);

        ModularPanel panel = ModularPanel.defaultPanel("server_scenario");
        panel.child(new AE2Button(() -> "Apply", () -> {}).size(40, 20))
            .child(new NumberEntryWidget())
            .child(new ListRow(() -> "Row").checkbox(() -> false, () -> {}))
            .child(new VerticalButtonBar().button(() -> Icon.ADD, () -> "Add", () -> {}))
            .child(new ValueStepper(() -> "LV", step -> {}).size(84, 16))
            .child(new CycleDropdown<>("server_scenario_menu", String.class).choices(Arrays.asList("a", "b")))
            .child(new IconRowList(() -> rows))
            .child(new Scrollbar(() -> 0, () -> 0, row -> {}).size(AE2Theme.BIG_SCROLLER_WIDTH, 40))
            .child(new PageTabs(() -> 0, page -> {}).tab(() -> Icon.GEAR, () -> "Settings"))
            .child(new StepSlider().size(60, 12))
            .child(new GTNRListWidget<>().scrollDirection(new HandleScrollData()));
        WidgetTree.collectSyncValues(new PanelSyncManager(new ModularSyncManager(false), true), panel);
    }

    public static class Options {

        @Config.LangKey("gtnrlib.test.volume")
        @Config.RangeDouble(min = 0, max = 1)
        @Config.DefaultDouble(0.5)
        public static double volume = 0.5;

        @Config.LangKey("gtnrlib.test.shown")
        @Config.DefaultBoolean(true)
        public static boolean shown = true;
    }
}
