package com.xyp.gtnrlib.text;

/**
 * Whether a rolling number counts up like an odometer ({@link #NONE}) or spins every digit on its own like a slot
 * machine and stops them one by one, starting at the right with the ones digit ({@link #RIGHT_FIRST}) or at the left
 * with the highest digit ({@link #LEFT_FIRST}). {@code id} names the side that stops first, as in {@code slot:left}.
 */
public enum SlotOrder {

    NONE(""),
    RIGHT_FIRST("right"),
    LEFT_FIRST("left");

    public final String id;

    SlotOrder(String id) {
        this.id = id;
    }

    /** Returns the order written as {@code id}, or null when the name is unknown or empty. */
    public static SlotOrder byId(String id) {
        for (SlotOrder order : values()) {
            if (order != NONE && order.id.equals(id)) return order;
        }
        return null;
    }
}
