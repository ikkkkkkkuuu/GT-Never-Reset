package com.xyp.gtnrlib.client.input;

public final class KeyModifiers {

    public static final KeyModifiers NONE = new KeyModifiers(false, false, false);
    public final boolean shift;
    public final boolean ctrl;
    public final boolean alt;

    public KeyModifiers(boolean shift, boolean ctrl, boolean alt) {
        this.shift = shift;
        this.ctrl = ctrl;
        this.alt = alt;
    }

    public boolean any() {
        return shift || ctrl || alt;
    }

    @Override
    public String toString() {
        if (!any()) {
            return "none";
        }
        StringBuilder text = new StringBuilder();
        if (shift) {
            text.append("shift");
        }
        if (ctrl) {
            text.append(text.length() > 0 ? "+ctrl" : "ctrl");
        }
        if (alt) {
            text.append(text.length() > 0 ? "+alt" : "alt");
        }
        return text.toString();
    }
}
