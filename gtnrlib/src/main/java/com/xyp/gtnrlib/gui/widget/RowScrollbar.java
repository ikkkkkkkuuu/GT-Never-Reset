package com.xyp.gtnrlib.gui.widget;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** @deprecated Use {@link Scrollbar}. */
@Deprecated
public class RowScrollbar extends Scrollbar {

    public RowScrollbar(IntSupplier position, IntSupplier maxPosition, IntConsumer scrollTo) {
        super(position, maxPosition, scrollTo);
    }

    @Override
    public RowScrollbar step(int step) {
        super.step(step);
        return this;
    }

    @Override
    public RowScrollbar pageOnTrackPress(boolean pages) {
        super.pageOnTrackPress(pages);
        return this;
    }

    @Override
    public RowScrollbar small(boolean small) {
        super.small(small);
        return this;
    }

    @Override
    public RowScrollbar plainBar(int color, IntSupplier visible) {
        super.plainBar(color, visible);
        return this;
    }
}
