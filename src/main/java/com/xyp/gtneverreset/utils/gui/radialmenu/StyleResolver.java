package com.xyp.gtneverreset.utils.gui.radialmenu;

public final class StyleResolver {

    public static WheelColors resolve(WheelColors defaults, MenuStyle profile, MenuStyle menu) {
        return apply(apply(defaults, profile), menu);
    }

    private static WheelColors apply(WheelColors base, MenuStyle style) {
        if (style == null) {
            return base;
        }
        return new WheelColors(
            Colors.over(style.ringColor, base.ring),
            Colors.over(style.highlightColor, base.highlight),
            Colors.over(style.borderColor, base.border),
            Colors.over(style.highlightBorderColor, base.highlightBorder),
            Colors.over(style.backgroundColor, base.background),
            Colors.over(style.iconColor, base.icon));
    }
}
