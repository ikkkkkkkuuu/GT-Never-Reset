package com.xyp.gtnrlib.gui.widget;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.scroll.ScrollData;
import com.cleanroommc.modularui.widgets.ListWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class GTNRListWidget<I extends IWidget, W extends ListWidget<I, W>> extends ListWidget<I, W> {

    private int initialScrollY;

    public GTNRListWidget() {
        this(0);
    }

    public GTNRListWidget(int initialScrollY) {
        super();
        this.initialScrollY = initialScrollY;
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
