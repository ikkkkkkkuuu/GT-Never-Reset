package com.xyp.gtnrlib.text;

/**
 * How a rolling number changes. {@link #CUT} slides each digit like an odometer and cuts it at the line,
 * {@link #FADE} slides it and fades it out near the line, {@link #JUMP} shows whole numbers that step forward.
 * {@link #AUTO} jumps inside an effect, whose outline would also trace a sliding digit's cut, and cuts plain text.
 */
public enum RollMode {

    AUTO(""),
    CUT("cut"),
    FADE("fade"),
    JUMP("jump");

    public final String id;

    RollMode(String id) {
        this.id = id;
    }

    /** Returns the mode written as {@code id}, or null when the name is unknown or empty. */
    public static RollMode byId(String id) {
        for (RollMode mode : values()) {
            if (mode != AUTO && mode.id.equals(id)) return mode;
        }
        return null;
    }
}
