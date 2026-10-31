package com.xyp.gtnrlib.client.hud;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.RecipeViewerSettingsImpl;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.gui.layout.Layout;
import com.xyp.gtnrlib.gui.layout.LayoutMath;
import com.xyp.gtnrlib.gui.layout.Size;
import com.xyp.gtnrlib.gui.theme.ButtonPurpose;
import com.xyp.gtnrlib.gui.widget.LayoutTheme;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@SideOnly(Side.CLIENT)
public class HudEditor extends ModularScreen {

    private static final String LANG = "gtnrlib.hud.editor.", UNAVAILABLE = "§7";
    // #tr gtnrlib.hud.editor.open
    // # Edit HUD
    // # zh_CN 编辑 HUD
    public static final String OPEN_LANG = LANG + "open";
    private static final int HELP_Y = 6, TEXT_HEIGHT = 10, TOOLBAR_Y = 20, MARGIN = 6, BUTTON_MIN = 52;
    private static final float HELP_WIDTH = 0.96F;
    private static boolean pending;

    private final HudCanvas canvas;
    private final HudSettingsCard card;

    private HudEditor(HudCanvas canvas, LayoutTheme style) {
        this(canvas, new HudSettingsCard(canvas, style, TOOLBAR_Y + style.buttonHeight + MARGIN, MARGIN), style);
    }

    private HudEditor(HudCanvas canvas, HudSettingsCard card, LayoutTheme style) {
        super(GTNRLib.MODID, panel(canvas, card, style));
        this.canvas = canvas;
        this.card = card;
        String theme = HudColor.theme();
        if (theme != null) useTheme(theme);
        drawDarkBackground(false);
    }

    public static void open() {
        pending = true;
    }

    public static void openPending() {
        if (!pending) return;
        pending = false;
        String theme = HudColor.theme();
        LayoutTheme style = theme == null ? LayoutTheme.DEFAULT
            : LayoutTheme.of(
                IThemeApi.get()
                    .getTheme(theme));
        RecipeViewerSettingsImpl recipeViewer = new RecipeViewerSettingsImpl();
        recipeViewer.disable();
        ClientGUI.open(new HudEditor(new HudCanvas(), style), recipeViewer);
    }

    public static boolean isOpen() {
        return ModularScreen.getCurrent() instanceof HudEditor;
    }

    private static ModularPanel panel(HudCanvas canvas, HudSettingsCard card, LayoutTheme style) {
        ModularPanel panel = new ModularPanel("hud_editor") {

            @Override
            public boolean shouldAnimate() {
                return false;
            }
        };
        panel.full()
            .disableThemeBackground(true)
            .disableHoverBackground();
        panel.child(
            canvas.reserveTop(TOOLBAR_Y + style.buttonHeight + 2)
                .full());
        panel.child(
        // @formatter:off
            // #tr gtnrlib.hud.editor.help
            // # Click to edit · drag to move (hold Alt to skip snapping) · scroll to scale · right-click to show or hide · arrow keys nudge (Shift ×10) · Ctrl+Z undo, Ctrl+Y redo
            // # zh_CN 点选元素修改设置 · 拖动移动（按住 Alt 不吸附）· 滚轮缩放 · 右键显示/隐藏 · 方向键微调（按住 Shift 每次 10 格）· Ctrl+Z 撤销 · Ctrl+Y 重做
            // @formatter:on
            Layout.label(IKey.lang(LANG + "help"), style)
                .alignment(Alignment.Center)
                .leftRel(0.5F)
                .top(HELP_Y)
                .widthRel(HELP_WIDTH)
                .height(TEXT_HEIGHT));
        List<ButtonWidget<?>> buttons = new ObjectArrayList<>();
        IntArrayList widths = new IntArrayList();
        add(
            buttons,
            widths,
            style,
            ButtonPurpose.NORMAL,
            // #tr gtnrlib.hud.editor.show_all
            // # Show All
            // # zh_CN 显示全部
            // #tr gtnrlib.hud.editor.in_use_only
            // # In Use Only
            // # zh_CN 只显示在用
            () -> tr(canvas.showAll() ? "in_use_only" : "show_all"),
            canvas::toggleShowAll,
            "show_all",
            "in_use_only");
        add(
            buttons,
            widths,
            style,
            ButtonPurpose.NORMAL,
            // #tr gtnrlib.hud.editor.undo
            // # Undo
            // # zh_CN 撤销
            () -> (canvas.canUndo() ? "" : UNAVAILABLE) + tr("undo"),
            canvas::undo,
            "undo");
        add(
            buttons,
            widths,
            style,
            ButtonPurpose.NORMAL,
            // #tr gtnrlib.hud.editor.redo
            // # Redo
            // # zh_CN 重做
            () -> (canvas.canRedo() ? "" : UNAVAILABLE) + tr("redo"),
            canvas::redo,
            "redo");
        // #tr gtnrlib.hud.editor.reset
        // # Reset All
        // # zh_CN 重置全部
        add(buttons, widths, style, ButtonPurpose.DANGER, () -> tr("reset"), canvas::resetAll, "reset");
        add(
            buttons,
            widths,
            style,
            ButtonPurpose.PRIMARY,
            // #tr gtnrlib.hud.editor.done
            // # Done
            // # zh_CN 完成
            () -> tr("done"),
            () -> canvas.getPanel()
                .closeIfOpen(),
            "done");
        int[] sizes = widths.toIntArray();
        Flow toolbar = Flow.row()
            .size(LayoutMath.total(sizes, style.spacingMedium), style.buttonHeight)
            .childPadding(style.spacingMedium)
            .leftRel(0.5F)
            .top(TOOLBAR_Y);
        for (int i = 0; i < buttons.size(); i++) toolbar.child(
            buttons.get(i)
                .width(sizes[i]));
        panel.child(toolbar);
        panel.child(card);
        return panel;
    }

    private static void add(List<ButtonWidget<?>> buttons, IntList widths, LayoutTheme style, ButtonPurpose purpose,
        Supplier<String> label, Runnable action, String... keys) {
        int widest = 0;
        for (String key : keys) widest = Math.max(widest, Layout.textWidth(tr(key), style));
        widths.add(
            Size.content(widest, style.padding)
                .atLeast(BUTTON_MIN)
                .resolve(0));
        ButtonWidget<?> button = new ButtonWidget<>();
        buttons.add(
            purpose.apply(button)
                .height(style.buttonHeight)
                .overlay(Layout.caption(IKey.dynamic(label), style))
                .onMousePressed(mouse -> {
                    if (mouse != 0) return false;
                    action.run();
                    return true;
                }));
    }

    private static String tr(String key) {
        return StatCollector.translateToLocal(LANG + key);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (canvas.takeDirty()) card.refresh();
    }

    @Override
    public boolean onKeyPressed(char typedChar, int keyCode) {
        return canvas.key(keyCode) || super.onKeyPressed(typedChar, keyCode);
    }

    @Override
    public void onClose() {
        HudLayout.save();
    }
}
