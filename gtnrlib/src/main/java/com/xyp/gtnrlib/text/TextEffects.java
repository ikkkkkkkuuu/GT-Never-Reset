package com.xyp.gtnrlib.text;

import java.util.Objects;

import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.text.TextEffectStyle.Typewriter;

import it.unimi.dsi.fastutil.ints.IntList;

public class TextEffects {

    public static final TextEffectStyle INFERNUM_RED_RARITY = preset("infernum_red_rarity");
    public static final TextEffectStyle GENESIS_COMPONENT_RARITY_SHADER = preset("genesis_component_rarity_shader");
    public static final TextEffectStyle PULSE_CIRCLE = preset("pulse_circle");
    public static final TextEffectStyle NAMELESS_BOSS_BAR_SHADER = preset("nameless_boss_bar_shader");
    public static final TextEffectStyle PULSE_UPWARDS = preset("pulse_upwards");
    public static final TextEffectStyle CALAMITY_RED = preset("calamity_red");
    public static final TextEffectStyle EXOTIC_RAINBOW = new TextEffectStyle(
        "gtneverreset:exotic_rainbow",
        IntList.of(),
        1);
    public static final TextEffectStyle SUPERBOSS_RARITY = preset("superboss_rarity");
    public static final TextEffectStyle INFERNUM_SPARK_RARITY = preset("infernum_spark_rarity");
    public static final TextEffectStyle INFERNUM_CYAN_SPARK = preset("infernum_cyan_spark");
    public static final TextEffectStyle BURNISHED_AURIC = preset("burnished_auric");
    public static final TextEffectStyle EVERCOLD_CYAN = preset("evercold_cyan");
    public static final TextEffectStyle STARSILVER_RARITY = preset("starsilver_rarity");
    public static final TextEffectStyle NEBULA_RIFT = preset("nebula_rift");
    public static final TextEffectStyle PRISMATIC_SCAN = preset("prismatic_scan");
    public static final TextEffectStyle QUANTUM_GLITCH = preset("quantum_glitch");
    public static final TextEffectStyle MOLTEN_CORE = preset("molten_core");
    public static final TextEffectStyle PIXEL_REASSEMBLY = preset("pixel_reassembly");
    public static final TextEffectStyle REFRACTED_PRISM = preset("refracted_prism");
    public static final TextEffectStyle RUNE_ETCHING = preset("rune_etching");
    public static final TextEffectStyle CIRCUIT_AWAKENING = preset("circuit_awakening");
    public static final TextEffectStyle GHOST_ECHO = preset("ghost_echo");
    public static final TextEffectStyle PAPER_FOLD = preset("paper_fold");
    public static final TextEffectStyle FROST_CRYSTAL = preset("frost_crystal");
    /** A glowing tube sign: the whole sign dips every few seconds and random glyphs sputter; see {@code withFault}. */
    public static final TextEffectStyle NEON_FLICKER = preset(TextEffectFormat.NEON_FLICKER);
    /**
     * Text written in shifting runes; a wave reads it to plain text and back. Options: {@code hold}, {@code hide},
     * {@code sweep} (seconds), {@code flip} (rune changes per second), {@code shards} and {@code flick} (percent).
     */
    public static final TextEffectStyle ARCANE_CIPHER = preset(TextEffectFormat.ARCANE_CIPHER);
    /** An unseen black hole crosses the line and back every eight seconds, bending the text around it. */
    public static final TextEffectStyle GRAVITY_LENS = preset(TextEffectFormat.GRAVITY_LENS);
    /** Glowing molten metal: beads swell under the strokes, snap off with sparks and cool to a crust as they fall. */
    public static final TextEffectStyle MELTING = preset(TextEffectFormat.MELTING);
    /** Every six seconds stars light up on the strokes and lines join them into a new constellation. */
    public static final TextEffectStyle CONSTELLATION = preset(TextEffectFormat.CONSTELLATION);
    /** Pitted dark strokes with writhing tendrils, crossed by flux waves and torn by rifts; four colors. */
    public static final TextEffectStyle CORRUPTION = preset(TextEffectFormat.CORRUPTION);
    /** Plain text typed one glyph at a time; other effects type the same way through {@code withTyping}. */
    public static final TextEffectStyle TYPEWRITER = preset(TextEffectFormat.TYPEWRITER)
        .withTyping(TextEffectFormat.DEFAULT_TYPING_RATE);
    /** Plain text whose numbers count up when they appear; any effect rolls the same way through {@code withRoll}. */
    public static final TextEffectStyle ROLL = preset(TextEffectFormat.ROLL)
        .withRoll(TextEffectFormat.DEFAULT_ROLL_SECONDS);
    /** Plain text that trembles; any effect trembles the same way through {@code withShake}. */
    public static final TextEffectStyle SHAKE = preset(TextEffectFormat.SHAKE)
        .withShake(TextEffectFormat.DEFAULT_SHAKE);

    private static TextEffectStyle preset(String name) {
        return new TextEffectStyle(GTNRLib.MODID + ":" + name, IntList.of(), 1);
    }

    /**
     * Opens an inline effect until the next declaration or {@code §r}. Native color codes replace its palette;
     * native style codes retain their usual meaning. The same codes also accept an ampersand prefix. Example:
     * {@code §{pulse_upwards;colors=#FFD700;speed=1.2}§oText§r}. Compact input accepts aliases such as
     * {@code &{pu}}, {@code &{ba;#fc0;2}}, and the parameter names {@code c} and {@code s}. Every other parameter is
     * named and every time is in seconds: {@code t=20} types the span at twenty glyphs per second,
     * {@code r=decode:0.6} gives each typed glyph an entrance, {@code roll=2.5:jump} counts its numbers up and
     * {@code shake=2:sync} makes it tremble. {@code &{tw;t=24}}, {@code &{roll=1.5}} and {@code &{shake}} do the same
     * for plain text.
     */
    public static String format(TextEffectStyle style) {
        Objects.requireNonNull(style, "style");
        String id = style.rendererId();
        String namespace = GTNRLib.MODID + ":";
        if (id.startsWith(namespace)) id = id.substring(namespace.length());
        StringBuilder result = new StringBuilder(TextEffectFormat.INLINE_OPEN).append(id);
        if (!style.colors()
            .isEmpty()) {
            result.append(";colors=");
            for (int i = 0; i < style.colors()
                .size(); i++) {
                if (i > 0) result.append(',');
                String hex = Integer.toHexString(
                    style.colors()
                        .getInt(i));
                result.append('#')
                    .append("000000", 0, 6 - hex.length())
                    .append(hex);
            }
        }
        if (style.speed() != 1) result.append(";speed=")
            .append(style.speed());
        Typewriter typewriter = style.typewriter();
        if (typewriter.rate() != 0) result.append(";type=")
            .append(typewriter.rate());
        appendNamed(result, ";r=", TextEffectFormat.entranceValue(typewriter));
        appendNamed(result, ";roll=", TextEffectFormat.rollValue(style.roll()));
        appendNamed(
            result,
            ";" + TextEffectFormat.BLUR_PARAMETER + "=",
            style.roll()
                .blur() == TextEffectStyle.DEFAULT_ROLL_BLUR ? ""
                    : String.valueOf(
                        style.roll()
                            .blur()));
        appendNamed(result, ";shake=", TextEffectFormat.shakeValue(style.shake()));
        if (!style.options()
            .isEmpty())
            result.append(';')
                .append(TextEffectFormat.optionsValue(style, ";"));
        return result.append('}')
            .toString();
    }

    private static void appendNamed(StringBuilder result, String name, String value) {
        if (!value.isEmpty()) result.append(name)
            .append(value);
    }

    public static String opening() {
        return EffectTextParser.OPEN;
    }

    public static String apply(String text) {
        Objects.requireNonNull(text, "text");
        return text.isEmpty() ? text : opening() + text + EffectTextParser.CLOSE;
    }

    public static String apply(String text, TextEffectStyle style) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(style, "style");
        return text.isEmpty() ? text : opening(style) + text + EffectTextParser.CLOSE;
    }

    public static String opening(TextEffectStyle style) {
        StringBuilder result = new StringBuilder(EffectTextParser.PREFIX).append(style.rendererId())
            .append(';')
            .append(style.speed())
            .append(';');
        for (int i = 0; i < style.colors()
            .size(); i++) {
            if (i > 0) result.append(',');
            result.append(
                Integer.toHexString(
                    style.colors()
                        .getInt(i)));
        }
        Typewriter typewriter = style.typewriter();
        String[] fields = { String.valueOf(typewriter.rate()), TextEffectFormat.entranceValue(typewriter),
            TextEffectFormat.rollValue(style.roll()) + TextEffectFormat.blurWord(style.roll()),
            TextEffectFormat.shakeValue(style.shake()), TextEffectFormat.optionsValue(style, ",") };
        int used = fields.length;
        while (used > 1 && fields[used - 1].isEmpty()) used--;
        if (used == 1 && typewriter.rate() == 0) used = 0;
        for (int i = 0; i < used; i++) result.append(';')
            .append(fields[i]);
        return result.append(EffectTextParser.END)
            .toString();
    }

    /**
     * Holds a typewriter for {@code seconds} (at most {@link TextEffectFormat#MAX_PAUSE_SECONDS}) before the next
     * glyph.
     * The marker changes no style and is written {@code &{p;0.8}} in typed text; {@code &{p}} holds half a second.
     */
    public static String pause(float seconds) {
        return TextEffectFormat.pauseMarker(seconds);
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
