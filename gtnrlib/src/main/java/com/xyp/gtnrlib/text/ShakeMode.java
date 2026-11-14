package com.xyp.gtnrlib.text;

/** Whether shaking text moves glyph by glyph ({@link #EACH}) or all together ({@link #SYNC}). */
public enum ShakeMode {

    EACH("each"),
    SYNC("sync");

    public final String id;

    ShakeMode(String id) {
        this.id = id;
    }

    /** Returns the mode written as {@code id}, or null when the name is unknown. */
    public static ShakeMode byId(String id) {
        for (ShakeMode mode : values()) {
            if (mode.id.equals(id)) return mode;
        }
        return null;
    }
}
