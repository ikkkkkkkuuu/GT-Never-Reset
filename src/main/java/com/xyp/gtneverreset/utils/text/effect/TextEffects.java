package com.xyp.gtneverreset.utils.text.effect;

import java.util.List;
import java.util.Objects;

import com.xyp.gtneverreset.GTNeverReset;

public final class TextEffects {

    public static final TextEffectStyle EXOTIC_RAINBOW = new TextEffectStyle(
        GTNeverReset.MODID + ":exotic_rainbow",
        List.of(),
        1);

    public static String apply(String text) {
        Objects.requireNonNull(text, "text");
        return text.isEmpty() ? text : opening() + text + EffectTextParser.CLOSE;
    }

    public static String opening() {
        return EffectTextParser.OPEN;
    }

    public static String escapeLiteral(String text) {
        return Objects.requireNonNull(text, "text")
            .replace("\u2063", "\u2063\u2063");
    }

    public static String plainText(String text) {
        return EffectTextParser.parse(text)
            .plainText();
    }
}
