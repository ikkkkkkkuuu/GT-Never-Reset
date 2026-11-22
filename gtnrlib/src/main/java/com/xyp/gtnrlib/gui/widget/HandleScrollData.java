package com.xyp.gtnrlib.gui.widget;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Mouse;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.widget.scroll.ScrollArea;
import com.cleanroommc.modularui.widget.scroll.ScrollData;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;

/**
 * Vertical scroll data with a fixed-size AE2 or vanilla handle. It can be built on either side, so it also works in
 * GUIs the server opens.
 * <p>
 * The handle follows the look of the panel the list is in: AE2's groove and handle under AE2 themes, the vanilla
 * creative-inventory scroll bar otherwise, and the width follows with it. {@link GTNRListWidget} hands it its panel's
 * look before every layout; other scroll widgets call {@link #look(AE2Look)} themselves, or keep the plain look.
 * <p>
 * {@link GTNRListWidget} reserves the track width and draws the disabled handle when the content fits. Other scroll
 * widgets must reserve {@link #getThickness()} pixels on the right and draw the inactive track themselves. The
 * available vertical track must be at least {@link AE2Theme#SCROLLER_HEIGHT} pixels tall.
 * <p>
 * Scrolling is smooth by default. {@link #rows(int)} enables immediate scrolling snapped to row boundaries. Scroll
 * shadows are never drawn. A press on the track beside the handle moves the handle's centre to the pointer at once, and
 * the same press keeps dragging from there. {@link #pageOnTrackPress(boolean) pageOnTrackPress(true)} pages like AE2
 * instead: the press moves one wheel step (MUI2's scroll speed, one row after {@link #rows(int)}) towards the pointer,
 * and inside a {@link GTNRListWidget} holding the button keeps paging, first after {@value #FIRST_REPEAT_MILLIS} ms and
 * then every {@value #REPEAT_MILLIS} ms.
 */
public class HandleScrollData extends VerticalScrollData implements IPersistentScrollbar {

    /** Delay before a held track press pages again, and the interval between later pages, in milliseconds. */
    public static final int FIRST_REPEAT_MILLIS = 250, REPEAT_MILLIS = 150;
    private final boolean small;
    private AE2Look look;
    private int rowHeight;
    private int dragMouse, dragScroll, pageDirection;
    private long nextPage;
    private boolean pages;

    /** Creates scroll data with a large handle. */
    public HandleScrollData() {
        this(false);
    }

    /**
     * Creates scroll data with the requested handle style.
     *
     * @param small whether to use the 7-pixel-wide scroll bar instead of the large one; outside AE2 themes it is
     *              Minecraft's handle narrowed to fit
     */
    public HandleScrollData(boolean small) {
        this(null, small);
    }

    /**
     * Creates scroll data with a large handle that starts with a given AE2 skin.
     *
     * @param variant skin used until the list hands over its panel's look, or {@code null} for the plain look
     * @deprecated use {@link #HandleScrollData()}; the handle follows the panel's theme
     */
    @Deprecated
    public HandleScrollData(@Nullable Variant variant) {
        this(variant, false);
    }

    /**
     * Creates scroll data with the requested handle style that starts with a given AE2 skin.
     *
     * @param variant skin used until the list hands over its panel's look, or {@code null} for the plain look
     * @param small   whether to use the 7-pixel-wide AE2 handle instead of the 12-pixel-wide one
     * @deprecated use {@link #HandleScrollData(boolean)}; the handle follows the panel's theme
     */
    @Deprecated
    public HandleScrollData(@Nullable Variant variant, boolean small) {
        super(
            false,
            AE2Look.of(variant)
                .scrollbarWidth());
        this.small = small;
        look = AE2Look.of(variant);
    }

    /**
     * Sets the look the handle is drawn with, which also decides the track width. {@link GTNRListWidget} calls it with
     * its panel's look before every layout.
     *
     * @param look look of the panel the scroll widget is in
     * @return this scroll data
     */
    public HandleScrollData look(AE2Look look) {
        this.look = look;
        return this;
    }

    /**
     * Returns the track width for the current look: 7 pixels for the small scroll bar in every look, otherwise 12 for
     * AE2's large handle and 14 for the vanilla scroll bar.
     *
     * @return track width in pixels
     */
    @Override
    public int getThickness() {
        return small ? AE2Theme.SMALL_SCROLLER_WIDTH : look.scrollbarWidth();
    }

    /**
     * Configures row snapping and the wheel step.
     * <p>
     * Positive values disable animation and set the wheel step to one row. Both content endpoints remain reachable when
     * the scroll range is not divisible by the row height. Align content, viewport, padding and separators to the row
     * height when partial rows must not be visible.
     *
     * @param rowHeight row height in pixels; nonpositive values disable snapping without changing the wheel step
     * @return this scroll data
     */
    public HandleScrollData rows(int rowHeight) {
        this.rowHeight = Math.max(0, rowHeight);
        if (rowHeight > 0) setScrollSpeed(rowHeight);
        return this;
    }

    /**
     * Chooses what a press on the track beside the handle does.
     *
     * @param pages {@code true} to page one wheel step towards the pointer like AE2; {@code false}, the default, to
     *              move the handle's centre to the pointer and keep dragging
     * @return this scroll data
     */
    public HandleScrollData pageOnTrackPress(boolean pages) {
        this.pages = pages;
        return this;
    }

    @Override
    public int getScrollBarLength(ScrollArea area) {
        return Math.min(AE2Theme.SCROLLER_HEIGHT, getFullVisibleSize(area));
    }

    /**
     * Calculates the handle offset along an active track.
     *
     * @param area            owning scroll area
     * @param scrollBarLength handle length in pixels
     * @param fullVisibleSize visible track length in pixels, smaller than the content length
     * @return handle offset from the track's start in pixels
     */
    @Override
    public int getScrollBarStart(ScrollArea area, int scrollBarLength, int fullVisibleSize) {
        return (int) ((long) (fullVisibleSize - scrollBarLength) * getScroll() / (getScrollSize() - fullVisibleSize));
    }

    /**
     * Handles a press on an active track: a press on the handle starts a drag anchored to the current scroll offset,
     * and a press beside the handle scrolls at once to the offset centring the handle on the pointer and drags from
     * there, or, with paging turned on, pages towards the pointer instead.
     *
     * @param area         owning scroll area
     * @param mainAxisPos  mouse Y coordinate relative to the scroll area
     * @param crossAxisPos mouse X coordinate relative to the scroll area
     * @param button       mouse button
     * @return whether the press landed on the track
     */
    @Override
    public boolean onMouseClicked(ScrollArea area, int mainAxisPos, int crossAxisPos, int button) {
        int track = mainAxisPos - trackTop(area);
        pageDirection = 0;
        if (!isScrollBarActive(area) || !super.onMouseClicked(area, track, crossAxisPos, button)) return false;
        int length = getScrollBarLength(area), visible = getFullVisibleSize(area);
        int start = getScrollBarStart(area, length, visible);
        boolean beside = track < start || track > start + length;
        if (beside && pages) {
            dragging = false;
            pageDirection = track < start ? -1 : 1;
            nextPage = System.currentTimeMillis() + FIRST_REPEAT_MILLIS;
            page(area);
            return true;
        }
        // MUI2 moves the content only once the pointer moves, so a still press would not jump.
        if (beside) scrollTo(area, offsetAt(track - clickOffset, visible, length));
        dragMouse = track;
        dragScroll = getScroll();
        return true;
    }

    /**
     * Pages again while a track press is held, at most once per {@link #REPEAT_MILLIS}. {@link GTNRListWidget} calls it
     * every client tick; other scroll widgets page once per press.
     *
     * @param area owning scroll area
     */
    public void repeatPaging(ScrollArea area) {
        if (pageDirection == 0) return;
        if (!Mouse.isButtonDown(0)) {
            pageDirection = 0;
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextPage) return;
        nextPage = now + REPEAT_MILLIS;
        page(area);
    }

    /** Stops paging after the mouse button is released. */
    public void stopPaging() {
        pageDirection = 0;
    }

    private void page(ScrollArea area) {
        int step = Math.max(1, getScrollSpeed());
        int max = Math.max(0, getScrollSize() - getFullVisibleSize(area)),
            from = isAnimating() ? getAnimatingTo() : getScroll();
        animateTo(area, Math.max(0, Math.min(max, from + pageDirection * step)));
    }

    /**
     * Converts the pointer position to the progress consumed by {@link ScrollArea#drag(int, int)}.
     * <p>
     * During a drag, the pointer's movement since the press is added to the offset recorded at the press, so a pointer
     * that has not moved keeps the content in place regardless of the content length.
     *
     * @param area         owning scroll area
     * @param mainAxisPos  mouse Y coordinate relative to the scroll area
     * @param crossAxisPos mouse X coordinate relative to the scroll area
     * @return progress adjusted for the content range used by MUI2 dragging
     */
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

    /**
     * Draws the groove and enabled or disabled handle in scroll-area coordinates.
     * <p>
     * The groove extends one pixel above and below the available vertical track. An active horizontal scroll bar is
     * excluded from that track. This method does not reserve layout space or process input.
     *
     * @param area owning scroll area
     */
    @Override
    public void drawTrack(ScrollArea area) {
        boolean active = isScrollBarActive(area);
        int handleY = active ? getScrollBarStart(area, getScrollBarLength(area), false) : 0;
        int x = area.w() - getThickness();
        int top = trackTop(area);
        int height = getFullVisibleSize(area);
        // The groove border sits one pixel outside the scroll area so the handle can travel the full height.
        if (small) look.drawSmallScrollbar(x, top - 1, height + 2, handleY, active);
        else look.drawScrollbar(x, top - 1, height + 2, handleY, active);
    }

    private int trackTop(ScrollArea area) {
        ScrollData horizontal = getOtherScrollData(area);
        return horizontal != null && horizontal.isScrollBarActive(area) && horizontal.isOnAxisStart()
            ? horizontal.getThickness()
            : 0;
    }
}
