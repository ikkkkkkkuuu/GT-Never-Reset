package com.xyp.gtnrlib.text;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A named number an effect reads from its declaration, such as {@code fault=40}. A {@code whole} option takes a whole
 * number; the others take up to three decimals. A value outside {@code min} to {@code max} rejects the declaration.
 */
public record TextEffectOption(String name, float min, float max, boolean whole) {

    private static final Pattern NAME = Pattern.compile("[a-z]{1,16}");
    private static final Pattern WHOLE = Pattern.compile("\\d{1,3}");
    private static final Pattern DECIMAL = Pattern.compile("\\d{1,3}(?:\\.\\d{1,3})?|\\.\\d{1,3}");

    public TextEffectOption {
        Objects.requireNonNull(name, "name");
        if (!NAME.matcher(name)
            .matches()) throw new IllegalArgumentException("Option names are one to sixteen lowercase letters");
        if (!Float.isFinite(min) || !Float.isFinite(max) || min > max)
            throw new IllegalArgumentException("Option range must be finite and ordered");
    }

    /** Reads a written value, rejecting text that is not a number of the right kind or falls outside the range. */
    public float read(String text) {
        if (!(whole ? WHOLE : DECIMAL).matcher(text)
            .matches()) throw new IllegalArgumentException("Invalid value for option " + name);
        float value = Float.parseFloat(text);
        if (value < min || value > max) throw new IllegalArgumentException("Option " + name + " is out of range");
        return value;
    }

    public String write(float value) {
        return whole ? String.valueOf(Math.round(value)) : TextEffectFormat.writeDecimal(value);
    }
}
