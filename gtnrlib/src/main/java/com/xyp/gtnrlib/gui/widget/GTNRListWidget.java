package com.xyp.gtnrlib.gui.widget;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.scroll.ScrollData;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * List widget with an initial vertical offset and support for persistent scroll bars.
 * <p>
 * Scroll data implementing {@link IPersistentScrollbar} reserves its thickness during layout even when the content
 * fits. Its inactive track is drawn after the content. Scroll offsets are clamped after each resize. A
 * {@link HandleScrollData} gets the look of the list's panel before each layout, so its handle and width follow the
 * panel's theme. Like AE2's lists, it does not darken its top and bottom edges while there is more to scroll; call
 * {@code showScrollShadows(true)} for MUI2's shading.
 *
 * @param <I> child widget type
 * @param <W> fluent widget type
 */
public class GTNRListWidget<I extends IWidget, W extends ListWidget<I, W>> extends ListWidget<I, W> {

    private int initialScrollY;

    /** Creates a list positioned at the start of its content. */
    public GTNRListWidget() {
        this(0);
    }

    /**
     * Creates a list with a vertical offset applied after its first resize.
     *
     * @param initialScrollY initial vertical offset in pixels; nonpositive values leave the offset unchanged
     */
    public GTNRListWidget(int initialScrollY) {
        super();
        this.initialScrollY = initialScrollY;
        showScrollShadows(false);
    }

    @Override
    public boolean layoutWidgets() {
        if (!hasChildren()) getScrollData().setScrollSize(0);
        return super.layoutWidgets();
    }

    @Override
    public void beforeResize(boolean onOpen) {
        super.beforeResize(onOpen);
        ScrollData data = getScrollData();
        if (data instanceof HandleScrollData handle) handle.look(AE2Look.of(this));
        if (data instanceof IPersistentScrollbar) {
            getScrollArea().getScrollPadding()
                .scrollPadding(
                    data.getAxis()
                        .getOther(),
                    data.isOnAxisStart(),
                    data.getThickness());
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (getScrollData() instanceof HandleScrollData handle) handle.repeatPaging(getScrollArea());
    }

    // MUI2 treats every mouse button on the scroll bar as a left press; AE2 scroll bars react to the left one only.
    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        ModularGuiContext context = getContext();
        if (mouseButton != 0 && getScrollArea().isInsideScrollbarArea(context.getMouseX(), context.getMouseY()))
            return Result.IGNORE;
        return super.onMousePressed(mouseButton);
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        if (getScrollData() instanceof HandleScrollData handle) handle.stopPaging();
        return super.onMouseRelease(mouseButton);
    }

    @Override
    public void postResize() {
        super.postResize();
        getScrollData().clamp(getScrollArea());
        if (initialScrollY > 0 && getScrollArea().getScrollY() != null) {
            getScrollArea().getScrollY()
                .scrollTo(getScrollArea(), initialScrollY);
            initialScrollY = 0;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        if (hasChildren()) super.draw(context, widgetTheme);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void postDraw(ModularGuiContext context, boolean transformed) {
        super.postDraw(context, transformed);
        if (!transformed && getScrollData() instanceof IPersistentScrollbar scrollbar
            && !getScrollData().isScrollBarActive(getScrollArea())) scrollbar.drawTrack(getScrollArea());
    }
}
