package com.xyp.gtnrlib.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.LocatedWidget;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.menu.Menu;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.theme.ScreenTheme;
import com.xyp.gtnrlib.gui.widget.CycleDropdown;
import com.xyp.gtnrlib.gui.widget.GTNRListWidget;
import com.xyp.gtnrlib.gui.widget.HandleScrollData;
import com.xyp.gtnrlib.gui.widget.NumberEntry;
import com.xyp.gtnrlib.gui.widget.StepSlider;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(modid = "ae2themeqa", name = "AE2 Theme QA", version = "1", dependencies = "after:" + GTNRLib.MODID)
public final class AE2ThemeClientChecks {

    private boolean launched, finished;
    private int ticks, themeIndex, stage, frames;
    private ModularScreen screen;
    private ListWidget<IWidget, ?> list;
    private ListWidget<IWidget, ?> empty;
    private NumberEntry number;
    private MenuProbe dropdown;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnrlib.ae2theme.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings != null) mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
        try {
            require(++ticks < 3600, "client timeout");
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "ae2-theme-" + System.currentTimeMillis(),
                    "AE2 Theme QA",
                    new WorldSettings(16, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (mc.thePlayer != null && screen == null) open();
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void open() {
        ScreenTheme theme = ScreenTheme.values()[themeIndex];
        Variant variant = AE2Theme.variant(theme.themeId);
        Minecraft.getMinecraft().gameSettings.guiScale = themeIndex == 2 ? 3 : 2;
        ModularPanel panel = new ModularPanel("ae2-theme-qa").size(350, 225)
            .padding(10);
        panel.child(
            IKey.str("AE2 Theme QA / " + theme.name())
                .asWidget()
                .pos(8, 8));
        list = new GTNRListWidget<>().scrollDirection(new HandleScrollData(variant).rows(24))
            .pos(8, 26)
            .size(155, 144);
        for (int i = 0; i < 1000; i++) {
            int index = i;
            list.child(
                new ButtonWidget<>().widthRel(1)
                    .height(24)
                    .background(AE2Theme.listRow(() -> index == 0))
                    .overlay(IKey.str("Node / 节点 " + (i + 1))));
        }
        panel.child(list);
        empty = new GTNRListWidget<>().scrollDirection(new HandleScrollData(variant, true))
            .pos(8, 184)
            .size(155, 25);
        panel.child(empty);
        number = new NumberEntry(variant).bounds(1, Long.MAX_VALUE)
            .value(64)
            .pos(180, 26);
        panel.child(number);
        panel.child(
            new StepSlider().range(0, 100, 20)
                .value(() -> 65, ignored -> {})
                .pos(180, 94)
                .size(138, 26));
        dropdown = new MenuProbe();
        dropdown.choices(List.of("All / 全部", "Nodes / 节点", "Controllers / 控制器"))
            .value(new StringValue("All / 全部"))
            .pos(180, 156)
            .size(138, 20);
        panel.child(dropdown);
        ButtonWidget<?> selected = new ButtonWidget<>().pos(180, 128)
            .size(138, 20);
        selected.background(AE2Theme.radio(selected, () -> true, () -> "Selected / 已选中", () -> true));
        panel.child(selected);
        AE2Theme.useMenuTheme(theme.themeId, GTNRLib.MODID, panel.getName(), "qa_options");
        screen = new ModularScreen(GTNRLib.MODID, panel);
        if (theme.themeId != null) screen.useTheme(theme.themeId);
        ClientGUI.open(screen);
        frames = stage = 0;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished
            || screen == null
            || ModularScreen.getCurrent() != screen
            || ++frames < 20) return;
        try {
            ScreenTheme theme = ScreenTheme.values()[themeIndex];
            if (stage == 0) {
                numbers();
                scroll();
                screenshot(theme.name() + "-controls.png");
                dropdown.openMenu(false);
                stage++;
            } else {
                Menu<?> menu = dropdown.inspectMenu();
                var padding = menu.getArea()
                    .getPadding();
                boolean ae2 = theme != ScreenTheme.DEFAULT;
                require(
                    padding.getLeft() == (ae2 ? 2 : 1) && padding.getRight() == (ae2 ? 2 : 1)
                        && padding.getTop() == (ae2 ? 2 : 1)
                        && padding.getBottom() == (ae2 ? 4 : 1),
                    "menu options stay inside the panel border");
                require(
                    menu.getChildren()
                        .get(0)
                        .getChildren()
                        .size() == 3,
                    "all menu options are visible");
                ButtonWidget<?> second = (ButtonWidget<?>) menu.getChildren()
                    .get(0)
                    .getChildren()
                    .get(1);
                var transform = LocatedWidget.of(second)
                    .getTransformationMatrix();
                int mouseX = transform.transformX(
                    second.getArea()
                        .w() / 2,
                    second.getArea()
                        .h() / 2);
                int mouseY = transform.transformY(
                    second.getArea()
                        .w() / 2,
                    second.getArea()
                        .h() / 2);
                screen.getContext()
                    .reset();
                screen.getContext()
                    .updateState(mouseX, mouseY, 0);
                screen.onFrameUpdate();
                log("hover at " + mouseX + "," + mouseY + ": " + second.isHovering());
                require(second.isHovering(), "menu option hover is resolved through the real widget tree");
                Minecraft.getMinecraft().entityRenderer.setupOverlayRendering();
                screen.drawScreen();
                screenshot(theme.name() + "-menu.png");
                second.onMousePressed(0);
                require(
                    "Nodes / 节点".equals(
                        dropdown.getValue()
                            .getValue()),
                    "menu selection updates the value");
                require(!dropdown.isOpen(), "selection closes the dropdown");
                log(theme + " PASS: inputs, scrollbar, empty list, menu padding and selection");
                Minecraft.getMinecraft()
                    .displayGuiScreen(null);
                screen = null;
                if (++themeIndex == ScreenTheme.values().length) finish(null);
            }
            frames = 0;
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void numbers() {
        AtomicInteger changes = new AtomicInteger();
        number.onChange(value -> changes.incrementAndGet())
            .bounds(Long.MIN_VALUE, Long.MAX_VALUE);
        TextFieldWidget field = (TextFieldWidget) number.getChildren()
            .stream()
            .filter(widget -> widget instanceof TextFieldWidget)
            .findFirst()
            .orElseThrow();
        number.value(Long.MAX_VALUE);
        require(
            number.value()
                .orElseThrow() == Long.MAX_VALUE && changes.get() == 0,
            "exact long maximum without callback");
        enter(field, Long.toString(Long.MIN_VALUE));
        require(
            number.value()
                .orElseThrow() == Long.MIN_VALUE && changes.get() == 1,
            "exact long minimum");
        enter(field, "1 / 0");
        require(
            !number.isInputValid() && !number.isValueValid()
                && number.value()
                    .isEmpty(),
            "invalid arithmetic stays editable");
        enter(field, "9223372036854775808");
        require(!number.isInputValid(), "integer overflow rejected");
        enter(field, "1.5");
        require(!number.isInputValid(), "fraction rejected");
        enter(field, "2 * 32");
        require(
            number.value()
                .orElseThrow() == 64 && changes.get() == 2,
            "expression recovers valid value once");
        enter(field, "64");
        require(changes.get() == 2, "equivalent text and blur do not duplicate callbacks");
        number.bounds(1, 64)
            .value(64);
        ((ButtonWidget<?>) number.getChildren()
            .get(0)).onMousePressed(0);
        require(
            number.value()
                .orElseThrow() == 64 && changes.get() == 2,
            "step against bound does not notify");
        number.value(1);
        ((ButtonWidget<?>) number.getChildren()
            .get(2)).onMousePressed(0);
        require(
            number.value()
                .orElseThrow() == 10 && changes.get() == 3,
            "step from one selects ten");
        number.value(64);
    }

    private void enter(TextFieldWidget field, String text) {
        field.setText(text);
        field.onRemoveFocus(screen.getContext());
    }

    private void scroll() {
        var area = list.getScrollArea();
        var data = list.getScrollData();
        data.scrollTo(area, 12345);
        int before = data.getScroll();
        int x = area.w() - data.getThickness() / 2;
        int y = data.getScrollBarStart(area, data.getScrollBarLength(area), false) + 7;
        require(area.mouseClicked(x, y), "real list scrollbar accepts press");
        area.drag(x, y);
        require(data.getScroll() == before, "real thousand-row list does not jump");
        area.drag(x, area.h() + 20);
        require(data.getScroll() == data.getScrollSize() - data.getFullVisibleSize(area), "last row is reachable");
        area.mouseReleased(x, y);
        data.scrollTo(area, 0);
        empty.getScrollData()
            .setScrollSize(1000);
        empty.getScrollData()
            .scrollTo(empty.getScrollArea(), 500);
        empty.layoutWidgets();
        empty.postResize();
        require(
            empty.getScrollData()
                .getScrollSize() == 0
                && empty.getScrollData()
                    .getScroll() == 0,
            "empty list clears stale scroll bounds");
        require(
            !empty.getScrollData()
                .isScrollBarActive(empty.getScrollArea()),
            "empty scrollbar remains disabled");
    }

    private static File output() {
        return new File(System.getProperty("gtnrlib.ae2theme.qa.output"));
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void log(String message) throws Exception {
        Files.writeString(
            new File(output(), "structure.log").toPath(),
            message + "\n",
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
    }

    private void finish(Throwable failure) {
        if (finished) return;
        finished = true;
        try {
            StringWriter trace = new StringWriter();
            if (failure != null) failure.printStackTrace(new PrintWriter(trace));
            Files.writeString(new File(output(), "result.txt").toPath(), failure == null ? "PASS\n" : "FAIL\n" + trace);
            if (failure != null) failure.printStackTrace();
        } catch (Exception writeFailure) {
            writeFailure.printStackTrace();
        }
        Minecraft.getMinecraft()
            .shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class MenuProbe extends CycleDropdown<String> {

        private MenuProbe() {
            super("qa_options", String.class);
        }

        private Menu<?> inspectMenu() {
            return getMenu();
        }
    }
}
