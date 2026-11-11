package com.xyp.gtnrlib;

import static com.xyp.gtnrlib.testing.ContractAssertions.check;
import static com.xyp.gtnrlib.testing.ThemeAssertions.reloadThemes;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.drawable.DrawableSerialization;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.widget.scroll.HorizontalScrollData;
import com.cleanroommc.modularui.widget.scroll.ScrollArea;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.widget.HandleScrollData;

final class AE2ThemeContracts {

    static void run() throws Exception {
        for (int reload = 0; reload < 2; reload++) {
            AE2Theme.register();
            reloadThemes(AE2Theme.LIGHT_ID);
            for (Variant variant : Variant.values()) {
                ITheme menu = IThemeApi.get()
                    .getTheme(variant.menuId);
                check(
                    menu.getId()
                        .equals(variant.menuId),
                    "AE2 menu survives reload");
                check(
                    DrawableSerialization.serialize(
                        menu.getButtonTheme()
                            .getTheme()
                            .getBackground())
                        .equals(DrawableSerialization.serialize(new Rectangle().color(0))),
                    "menu rows remain transparent after JSON loading instead of inheriting framed buttons");
                check(
                    DrawableSerialization.serialize(
                        menu.getButtonTheme()
                            .getHoverTheme()
                            .getBackground())
                        .equals(DrawableSerialization.serialize(variant.listRowSelected)),
                    "menu hover keeps the variant's selected-row sprite");
                check(
                    DrawableSerialization.serialize(
                        menu.getWidgetTheme(IThemeApi.TOGGLE_BUTTON)
                            .getTheme()
                            .getBackground())
                        .equals(DrawableSerialization.serialize(new Rectangle().color(0))),
                    "toggle menu rows remain flat after JSON loading");
            }
        }
        check(Variant.LIGHT.highlight == Variant.DARK.highlight, "original selection palettes remain compatible");
        check(Variant.DARK_PURPLE.highlightText == 0xFFFFFFFF, "purple selections retain readable white labels");
        for (Variant variant : new Variant[] { null, Variant.LIGHT, Variant.DARK_PURPLE }) {
            for (boolean small : new boolean[] { false, true }) {
                for (int height : new int[] { 72, 120, 216 }) {
                    for (int row : new int[] { 0, 24, 32 }) {
                        for (int horizontal : new int[] { 0, 9 }) {
                            scrolling(variant, small, height, row, horizontal);
                        }
                    }
                }
            }
        }
    }

    private static void scrolling(Variant variant, boolean small, int height, int row, int horizontal) {
        ScrollArea area = new ScrollArea(0, 0, 180, height);
        HandleScrollData data = new HandleScrollData(variant, small).rows(row);
        area.setScrollDataY(data);
        if (horizontal > 0) {
            HorizontalScrollData other = new HorizontalScrollData(true, horizontal);
            other.setScrollSize(500);
            area.setScrollDataX(other);
        }
        data.setScrollSize(320003);
        int range = data.getScrollSize() - data.getFullVisibleSize(area);
        int x = area.w() - data.getThickness() / 2;
        for (int index = 0; index <= 256; index++) {
            data.scrollTo(area, (int) ((long) range * index / 256));
            int before = data.getScroll();
            int y = horizontal + data.getScrollBarStart(area, data.getScrollBarLength(area), false) + 7;
            check(area.mouseClicked(x, y), "active scrollbar starts dragging");
            area.drag(x, y);
            check(data.getScroll() == before, "stationary handle preserves exact long-list offset");
            area.drag(x, y + 3);
            area.drag(x, y);
            check(data.getScroll() == before, "returning the pointer restores its original scroll offset");
            area.mouseReleased(x, y);
            check(!area.isDragging(), "release ends the drag");
        }
        data.scrollTo(area, range / 2);
        check(area.mouseClicked(x, horizontal + 1), "track click starts dragging");
        area.drag(x, horizontal + 1);
        check(data.getScroll() == 0, "track click near the top reaches the first row");
        area.drag(x, height + 20);
        check(data.getScroll() == range, "drag reaches the last partial row");
        area.mouseReleased(x, height);
        data.scrollBy(area, Integer.MAX_VALUE);
        check(data.getScroll() == range, "large forward delta cannot overflow to the top");
        data.scrollBy(area, Integer.MIN_VALUE);
        check(data.getScroll() == 0, "large reverse delta clamps to the top");
        data.setScrollSize(0);
        check(!data.onMouseClicked(area, horizontal + 1, x, 0), "disabled scrollbar does not start a drag");
    }
}
