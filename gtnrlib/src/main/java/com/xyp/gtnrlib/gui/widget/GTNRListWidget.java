package com.xyp.gtnrlib.gui.widget;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
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
    public void postResize() {
        super.postResize();
        if (initialScrollY > 0 && getScrollArea().getScrollY() != null) {
            getScrollArea().getScrollY()
                .scrollTo(getScrollArea(), initialScrollY);
            initialScrollY = 0;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void postDraw(ModularGuiContext context, boolean transformed) {
        super.postDraw(context, transformed);
        if (!transformed && getScrollData() instanceof HandleScrollData handle
            && !handle.isScrollBarActive(getScrollArea())) handle.drawTrack(getScrollArea());
    }
}
