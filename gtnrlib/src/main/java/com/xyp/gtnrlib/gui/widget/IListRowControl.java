package com.xyp.gtnrlib.gui.widget;

/**
 * A control that draws differently as the control of a {@link ListRow}, such as text in the list's colors instead of
 * the panel's.
 */
public interface IListRowControl {

    /**
     * Tells the control that it is the control of a list row. {@link ListRow#control} calls it once, when the control
     * is placed in the row.
     */
    void placedInListRow();
}
