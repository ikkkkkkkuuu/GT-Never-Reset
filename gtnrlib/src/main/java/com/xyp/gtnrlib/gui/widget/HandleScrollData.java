package com.xyp.gtnrlib.gui.widget;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.widget.scroll.ScrollArea;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HandleScrollData extends VerticalScrollData {

    private final @Nullable Variant variant;
    private final boolean small;
    private int rowHeight;

    public HandleScrollData(@Nullable Variant variant) {
        this(variant, false);
    }

    public HandleScrollData(@Nullable Variant variant, boolean small) {
        super(false, small && variant != null ? AE2Theme.SCROLLBAR_WIDTH : AE2Theme.scrollbarWidth(variant));
        this.variant = variant;
        this.small = small && variant != null;
    }

    public HandleScrollData rows(int rowHeight) {
        this.rowHeight = Math.max(0, rowHeight);
        if (rowHeight > 0) setScrollSpeed(rowHeight);
        return this;
    }

    @Override
    public int getScrollBarLength(ScrollArea area) {
        return Math.min(AE2Theme.SCROLLER_HEIGHT, getFullVisibleSize(area));
    }

    @Override
    public boolean scrollTo(ScrollArea area, int x) {
        return super.scrollTo(area, rowHeight > 0 ? Math.round(x / (float) rowHeight) * rowHeight : x);
    }

    @Override
    public void animateTo(ScrollArea area, int x) {
        if (rowHeight > 0) scrollTo(area, x);
        else super.animateTo(area, x);
    }

    @Override
    public void drawScrollbar(ScrollArea area, ModularGuiContext context, WidgetTheme widgetTheme, IDrawable texture) {
        drawTrack(area);
    }

    @Override
    public void drawScrollShadow(ScrollArea area, ModularGuiContext context) {}

    public void drawTrack(ScrollArea area) {
        boolean active = isScrollBarActive(area);
        int handleY = active ? getScrollBarStart(area, getScrollBarLength(area), false) : 0;
        int x = area.w() - getThickness();
        // The groove border sits one pixel outside the scroll area so the handle can travel the full height.
        if (small) AE2Theme.drawSmallScrollbar(variant, x, -1, area.h() + 2, handleY, active);
        else AE2Theme.drawScrollbar(variant, x, -1, area.h() + 2, handleY, active);
    }
}
