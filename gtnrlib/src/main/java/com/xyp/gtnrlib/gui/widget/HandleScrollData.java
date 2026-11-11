package com.xyp.gtnrlib.gui.widget;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.widget.scroll.ScrollArea;
import com.cleanroommc.modularui.widget.scroll.ScrollData;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HandleScrollData extends VerticalScrollData implements IPersistentScrollbar {

    private final @Nullable Variant variant;
    private final boolean small;
    private int rowHeight;
    private int dragMouse, dragScroll;

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
    public int getScrollBarStart(ScrollArea area, int scrollBarLength, int fullVisibleSize) {
        return (int) ((long) (fullVisibleSize - scrollBarLength) * getScroll() / (getScrollSize() - fullVisibleSize));
    }

    @Override
    public boolean onMouseClicked(ScrollArea area, int mainAxisPos, int crossAxisPos, int button) {
        int track = mainAxisPos - trackTop(area);
        if (!isScrollBarActive(area) || !super.onMouseClicked(area, track, crossAxisPos, button)) return false;
        int length = getScrollBarLength(area), visible = getFullVisibleSize(area);
        int start = getScrollBarStart(area, length, visible);
        dragMouse = track;
        dragScroll = track < start || track > start + length ? offsetAt(track - clickOffset, visible, length)
            : getScroll();
        return true;
    }

    @Override
    public float getProgress(ScrollArea area, int mainAxisPos, int crossAxisPos) {
        int visible = getFullVisibleSize(area);
        int range = getScrollSize() - visible;
        if (range <= 0) return 0;
        int length = getScrollBarLength(area), travel = visible - length, track = mainAxisPos - trackTop(area);
        int handle = track - clickOffset;
        long offset;
        // A stationary press keeps its exact offset; a moved thumb at either end uses the track endpoint.
        if (travel <= 0) offset = getScroll();
        else if (isDragging() && (track == dragMouse || handle > 0 && handle < travel)) offset = Math
            .max(0, Math.min(range, Math.round(dragScroll + (double) (track - dragMouse) * range / travel)));
        else offset = offsetAt(handle, visible, length);
        // ScrollArea.drag multiplies by the range plus the track thickness and truncates; the half pixel survives both.
        return (float) ((offset + 0.5D) / ((double) range + getThickness()));
    }

    private int offsetAt(int handle, int visible, int length) {
        int range = getScrollSize() - visible, travel = visible - length;
        if (travel <= 0) return getScroll();
        return (int) Math.max(0, Math.min(range, Math.round((double) handle * range / travel)));
    }

    @Override
    public boolean scrollTo(ScrollArea area, int x) {
        int maxScroll = Math.max(0, getScrollSize() - getFullVisibleSize(area));
        if (rowHeight > 0 && x > 0 && x < maxScroll) {
            x = (int) Math.min(Math.round(x / (double) rowHeight) * rowHeight, maxScroll);
        }
        return super.scrollTo(area, x);
    }

    @Override
    public boolean scrollBy(ScrollArea area, int x) {
        long target = (long) getScroll() + x;
        return scrollTo(area, (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, target)));
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

    @Override
    public void drawTrack(ScrollArea area) {
        boolean active = isScrollBarActive(area);
        int handleY = active ? getScrollBarStart(area, getScrollBarLength(area), false) : 0;
        int x = area.w() - getThickness();
        int top = trackTop(area);
        int height = getFullVisibleSize(area);
        // The groove border sits one pixel outside the scroll area so the handle can travel the full height.
        if (small) AE2Theme.drawSmallScrollbar(variant, x, top - 1, height + 2, handleY, active);
        else AE2Theme.drawScrollbar(variant, x, top - 1, height + 2, handleY, active);
    }

    private int trackTop(ScrollArea area) {
        ScrollData horizontal = getOtherScrollData(area);
        return horizontal != null && horizontal.isScrollBarActive(area) && horizontal.isOnAxisStart()
            ? horizontal.getThickness()
            : 0;
    }
}
