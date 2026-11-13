package com.xyp.gtnrlib.text;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;

/**
 * Serializable visual parameters independent of client rendering classes. {@code typewriter} types the span one glyph
 * at a time, {@code roll} counts its numbers up when they appear and {@code shake} makes it tremble; each has a
 * {@code NONE} value that leaves the span as it is. {@code options} holds the named numbers an effect declares for
 * itself, such as the neon flicker's {@code fault} or the arcane cipher's {@code hold}; a missing option keeps the
 * effect's own default. Every time is in seconds, rounded to the millisecond.
 */
public record TextEffectStyle(String rendererId, IntList colors, float speed, Typewriter typewriter, Roll roll,
    Shake shake, SortedMap<String, Float> options) {

    public static final float MAX_TYPING_RATE = 1000;
    public static final float MAX_ENTRANCE_SECONDS = 2;
    public static final float MAX_ROLL_SECONDS = 10;
    public static final int MAX_SHAKE = 3;
    public static final float MIN_SLAM_SIZE = 1.5f;
    public static final float MAX_SLAM_SIZE = 6;
    public static final float DEFAULT_SLAM_SIZE = 3.5f;
    public static final int DEFAULT_ROLL_BLUR = 100;
    public static final int MAX_ROLL_BLUR = 200;
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public TextEffectStyle(String rendererId, java.util.List<Integer> colors, float speed) {
        this(rendererId, new IntArrayList(colors), speed);
    }

    public TextEffectStyle {
        Objects.requireNonNull(rendererId, "rendererId");
        if (rendererId.length() > 128 || !IDENTIFIER.matcher(rendererId)
            .matches()) throw new IllegalArgumentException("Invalid effect identifier");
        colors = IntLists.unmodifiable(new IntArrayList(colors));
        if (colors.size() > 8) throw new IllegalArgumentException("At most eight palette colors are supported");
        for (int i = 0; i < colors.size(); i++) {
            if ((colors.getInt(i) & 0xFF000000) != 0)
                throw new IllegalArgumentException("Palette colors must be 24-bit RGB");
        }
        if (!Float.isFinite(speed) || speed < 0)
            throw new IllegalArgumentException("Speed must be finite and nonnegative");
        Objects.requireNonNull(typewriter, "typewriter");
        Objects.requireNonNull(roll, "roll");
        Objects.requireNonNull(shake, "shake");
        SortedMap<String, Float> rounded = new TreeMap<>();
        for (Map.Entry<String, Float> option : options.entrySet()) {
            Float value = option.getValue();
            if (value == null || !Float.isFinite(value))
                throw new IllegalArgumentException("Option " + option.getKey() + " must be a finite number");
            rounded.put(Objects.requireNonNull(option.getKey(), "option name"), toThousandths(value));
        }
        options = Collections.unmodifiableSortedMap(rounded);
    }

    public TextEffectStyle(String rendererId, IntList colors, float speed) {
        this(rendererId, colors, speed, Typewriter.NONE, Roll.NONE, Shake.NONE, new TreeMap<>());
    }

    public TextEffectStyle withColors(int... palette) {
        return new TextEffectStyle(rendererId, IntList.of(palette), speed, typewriter, roll, shake, options);
    }

    public TextEffectStyle withSpeed(float value) {
        return new TextEffectStyle(rendererId, colors, value, typewriter, roll, shake, options);
    }

    public TextEffectStyle withTypewriter(Typewriter value) {
        return new TextEffectStyle(rendererId, colors, speed, value, roll, shake, options);
    }

    public TextEffectStyle withRoll(Roll value) {
        return new TextEffectStyle(rendererId, colors, speed, typewriter, value, shake, options);
    }

    public TextEffectStyle withShake(Shake value) {
        return new TextEffectStyle(rendererId, colors, speed, typewriter, roll, value, options);
    }

    /** Reveals the span one glyph at a time at {@code glyphsPerSecond}; zero turns typing off. */
    public TextEffectStyle withTyping(float glyphsPerSecond) {
        return withTypewriter(
            new Typewriter(
                glyphsPerSecond,
                typewriter.entrance(),
                typewriter.entranceSeconds(),
                typewriter.entranceSize()));
    }

    /** Each typed glyph appears through {@code value} over its default time. */
    public TextEffectStyle withEntrance(TextEntrance value) {
        return withEntrance(value, 0);
    }

    /** Each typed glyph appears through {@code value} over {@code seconds}; zero uses the entrance's default. */
    public TextEffectStyle withEntrance(TextEntrance value, float seconds) {
        return withEntrance(value, seconds, 0);
    }

    /**
     * Each typed glyph appears through {@code value} over {@code seconds}, a slam starting at {@code size} times the
     * glyph's size ({@link #MIN_SLAM_SIZE} to {@link #MAX_SLAM_SIZE}); zero uses the entrance's defaults.
     */
    public TextEffectStyle withEntrance(TextEntrance value, float seconds, float size) {
        return withTypewriter(new Typewriter(typewriter.rate(), value, seconds, size));
    }

    /** Numbers in the span count up from zero over {@code seconds} when they appear; zero turns rolling off. */
    public TextEffectStyle withRoll(float seconds) {
        return withRoll(seconds, roll.mode());
    }

    public TextEffectStyle withRoll(float seconds, RollMode mode) {
        return withRoll(seconds, mode, roll.slot());
    }

    /** Numbers spin like a slot machine and stop digit by digit in {@code slot} order, or count up for {@code NONE}. */
    public TextEffectStyle withRoll(float seconds, RollMode mode, SlotOrder slot) {
        return withRoll(new Roll(seconds, mode, slot, roll.blur(), roll.waits()));
    }

    /** How strongly a slot machine's spinning digits smear, in percent: zero is sharp, 100 the default. */
    public TextEffectStyle withRollBlur(int percent) {
        return withRoll(new Roll(roll.seconds(), roll.mode(), roll.slot(), percent, roll.waits()));
    }

    /** With {@code false} the text after a rolling number types on while it rolls instead of waiting for it. */
    public TextEffectStyle withRollWait(boolean wait) {
        return withRoll(new Roll(roll.seconds(), roll.mode(), roll.slot(), roll.blur(), wait));
    }

    /** The span trembles by {@code pixels} (at most {@link #MAX_SHAKE}), keeping its mode and motion; zero stops it. */
    public TextEffectStyle withShake(int pixels) {
        return withShake(pixels, shake.mode(), shake.motion());
    }

    public TextEffectStyle withShake(int pixels, ShakeMode mode) {
        return withShake(pixels, mode, shake.motion());
    }

    public TextEffectStyle withShake(int pixels, ShakeMode mode, ShakeMotion motion) {
        return withShake(new Shake(pixels, mode, motion));
    }

    /** Sets one of the effect's own options; the declaration form checks names and ranges, this only stores it. */
    public TextEffectStyle withOption(String name, float value) {
        SortedMap<String, Float> changed = new TreeMap<>(options);
        changed.put(name, value);
        return new TextEffectStyle(rendererId, colors, speed, typewriter, roll, shake, changed);
    }

    /** Each neon glyph fails in a round with {@code percent} chance. */
    public TextEffectStyle withFault(int percent) {
        return withOption(TextEffectFormat.FAULT, percent);
    }

    /** The option's value, or {@code fallback} when the declaration leaves it out. */
    public float option(String name, float fallback) {
        Float value = options.get(name);
        return value == null ? fallback : value;
    }

    private static float toThousandths(float value) {
        return Math.round(value * 1000) / 1000f;
    }

    private static float checkedSeconds(float seconds, float max, String what) {
        if (!Float.isFinite(seconds) || seconds < 0 || seconds > max)
            throw new IllegalArgumentException(what + " must be between zero and " + max + " s");
        return toThousandths(seconds);
    }

    /**
     * Types the span at {@code rate} glyphs per second, zero showing it at once. {@code entrance} is how each typed
     * glyph appears and {@code entranceSeconds} how long that takes, zero meaning the entrance's own default (which is
     * kept as zero, so both spellings of it are equal).
     * {@code entranceSize} is how many times its size a slammed glyph starts at, zero meaning
     * {@link TextEffectStyle#DEFAULT_SLAM_SIZE}; only the slam takes one.
     */
    public record Typewriter(float rate, TextEntrance entrance, float entranceSeconds, float entranceSize) {

        public static final Typewriter NONE = new Typewriter(0, TextEntrance.NONE, 0, 0);

        public Typewriter {
            if (!Float.isFinite(rate) || rate < 0 || rate > MAX_TYPING_RATE)
                throw new IllegalArgumentException("Typing rate must be between zero and " + MAX_TYPING_RATE);
            Objects.requireNonNull(entrance, "entrance");
            entranceSeconds = checkedSeconds(entranceSeconds, MAX_ENTRANCE_SECONDS, "Entrance time");
            if (entrance == TextEntrance.NONE || entranceSeconds == entrance.defaultSeconds) entranceSeconds = 0;
            if (!Float.isFinite(entranceSize) || entranceSize != 0
                && (entrance != TextEntrance.SLAM || entranceSize < MIN_SLAM_SIZE || entranceSize > MAX_SLAM_SIZE))
                throw new IllegalArgumentException(
                    "Only a slam takes a size, between " + MIN_SLAM_SIZE + " and " + MAX_SLAM_SIZE);
            entranceSize = toThousandths(entranceSize) == DEFAULT_SLAM_SIZE ? 0 : toThousandths(entranceSize);
        }

        /** How long each typed glyph's entrance lasts, falling back to the entrance's default. */
        public float entranceDuration() {
            return entranceSeconds > 0 ? entranceSeconds : entrance.defaultSeconds;
        }

        /** How many times its size a slammed glyph starts at, falling back to the default. */
        public float slamSize() {
            return entranceSize > 0 ? entranceSize : DEFAULT_SLAM_SIZE;
        }
    }

    /**
     * Numbers reach their value over {@code seconds} when they appear, zero meaning they do not roll; {@code mode} is
     * how a moving digit is cut and {@code slot} whether they count up or spin and stop digit by digit. {@code blur}
     * is how strongly spinning slot digits smear, in percent from zero to {@link TextEffectStyle#MAX_ROLL_BLUR}, and
     * {@code waits} whether the text after the number waits for it to finish rolling.
     */
    public record Roll(float seconds, RollMode mode, SlotOrder slot, int blur, boolean waits) {

        public static final Roll NONE = new Roll(0, RollMode.AUTO, SlotOrder.NONE, DEFAULT_ROLL_BLUR, true);

        public Roll {
            seconds = checkedSeconds(seconds, MAX_ROLL_SECONDS, "Roll time");
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(slot, "slot");
            if (blur < 0 || blur > MAX_ROLL_BLUR)
                throw new IllegalArgumentException("Blur must be between zero and " + MAX_ROLL_BLUR + " percent");
            if (seconds == 0) {
                mode = RollMode.AUTO;
                slot = SlotOrder.NONE;
                blur = DEFAULT_ROLL_BLUR;
                waits = true;
            }
        }
    }

    /** The span trembles by {@code pixels}, zero meaning it stays still; mode and motion say how. */
    public record Shake(int pixels, ShakeMode mode, ShakeMotion motion) {

        public static final Shake NONE = new Shake(0, ShakeMode.EACH, ShakeMotion.SMOOTH);

        public Shake {
            if (pixels < 0 || pixels > MAX_SHAKE)
                throw new IllegalArgumentException("Shake must be between zero and " + MAX_SHAKE + " pixels");
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(motion, "motion");
            if (pixels == 0) {
                mode = ShakeMode.EACH;
                motion = ShakeMotion.SMOOTH;
            }
        }
    }
}
