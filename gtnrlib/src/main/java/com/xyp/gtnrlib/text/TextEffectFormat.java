package com.xyp.gtnrlib.text;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.text.TextEffectStyle.Roll;
import com.xyp.gtnrlib.text.TextEffectStyle.Shake;
import com.xyp.gtnrlib.text.TextEffectStyle.Typewriter;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/** Client-independent syntax and native formatting rules for effect text. */
public class TextEffectFormat {

    public static final String INLINE_OPEN = "\u00a7{";
    public static final String AMP_INLINE_OPEN = "&{";
    public static final int MAX_HEADER_LENGTH = 512;
    public static final String TYPEWRITER = "typewriter";
    public static final float DEFAULT_TYPING_RATE = 20;
    public static final float DEFAULT_PAUSE_SECONDS = 0.5f;
    public static final float MAX_PAUSE_SECONDS = 10;
    public static final String ROLL = "roll";
    public static final float DEFAULT_ROLL_SECONDS = 1;
    public static final String SLOT = "slot";
    public static final String NO_WAIT = "nowait";
    public static final String BLUR_PARAMETER = "blur";
    private static final String ROLL_PARAMETER = ROLL + "=";
    public static final String SHAKE = "shake";
    public static final int DEFAULT_SHAKE = 1;
    private static final String SHAKE_PARAMETER = SHAKE + "=";
    private static final Pattern SHAKE_AMOUNT = Pattern.compile("\\d");
    public static final String NEON_FLICKER = "neon_flicker";
    public static final String FAULT = "fault";
    public static final String ARCANE_CIPHER = "arcane_cipher";
    public static final String GRAVITY_LENS = "gravity_lens";
    public static final String MELTING = "melting";
    public static final String CONSTELLATION = "constellation";
    public static final String CORRUPTION = "corruption";
    public static final String HOLD = "hold";
    public static final String HIDE = "hide";
    public static final String SWEEP = "sweep";
    public static final String FLIP = "flip";
    public static final String SHARDS = "shards";
    public static final String FLICK = "flick";
    private static final String SECONDS = "(\\d{1,2}(?:\\.\\d{1,3})?|\\.\\d{1,3})";
    private static final Pattern PAUSE = Pattern
        .compile("\\s*(?:p|pause)\\s*(?:;\\s*(?:(?:duration|d)\\s*=\\s*)?" + SECONDS + "\\s*)?");
    private static final Pattern ENTRANCE = Pattern.compile("([a-z]+)(?::" + SECONDS + "(?::(\\d(?:\\.\\d{1,3})?))?)?");
    private static final Pattern BLUR = Pattern.compile("\\d{1,3}");
    private static final Pattern ROLL_SECONDS = Pattern.compile(SECONDS);
    private static final Map<String, String> ALIASES = new Object2ObjectOpenHashMap<>(
        Map.ofEntries(
            Map.entry("ir", "infernum_red_rarity"),
            Map.entry("gc", "genesis_component_rarity_shader"),
            Map.entry("pc", "pulse_circle"),
            Map.entry("nb", "nameless_boss_bar_shader"),
            Map.entry("pu", "pulse_upwards"),
            Map.entry("cr", "calamity_red"),
            Map.entry("er", "exotic_rainbow"),
            Map.entry("sb", "superboss_rarity"),
            Map.entry("is", "infernum_spark_rarity"),
            Map.entry("kcs", "infernum_cyan_spark"),
            Map.entry("ba", "burnished_auric"),
            Map.entry("ec", "evercold_cyan"),
            Map.entry("ss", "starsilver_rarity"),
            Map.entry("nr", "nebula_rift"),
            Map.entry("ps", "prismatic_scan"),
            Map.entry("qg", "quantum_glitch"),
            Map.entry("mc", "molten_core"),
            Map.entry("px", "pixel_reassembly"),
            Map.entry("rp", "refracted_prism"),
            Map.entry("re", "rune_etching"),
            Map.entry("ca", "circuit_awakening"),
            Map.entry("ge", "ghost_echo"),
            Map.entry("pf", "paper_fold"),
            Map.entry("fc", "frost_crystal"),
            Map.entry("nf", NEON_FLICKER),
            Map.entry("ac", ARCANE_CIPHER),
            Map.entry("gl", GRAVITY_LENS),
            Map.entry("mt", MELTING),
            Map.entry("cn", CONSTELLATION),
            Map.entry("tc", CORRUPTION),
            Map.entry("tw", TYPEWRITER)));
    private static final Set<String> LEGACY_NAMESPACES = new ObjectOpenHashSet<>();
    private static final Map<String, Map<String, TextEffectOption>> OPTIONS = new Object2ObjectOpenHashMap<>();
    private static final Set<String> RESERVED_PARAMETERS = Set
        .of("colors", "color", "c", "speed", "s", "type", "t", "reveal", "r", ROLL, SHAKE, BLUR_PARAMETER);

    static {
        registerOptions(NEON_FLICKER, new TextEffectOption(FAULT, 0, 100, true));
        registerOptions(
            ARCANE_CIPHER,
            new TextEffectOption(HOLD, 0, 30, false),
            new TextEffectOption(HIDE, 0.5f, 30, false),
            new TextEffectOption(SWEEP, 0.2f, 10, false),
            new TextEffectOption(FLIP, 0, 30, false),
            new TextEffectOption(SHARDS, 0, 8, true),
            new TextEffectOption(FLICK, 0, 100, true));
    }

    /**
     * Declares the named numbers an effect accepts, written as {@code name=value} in its declaration. Names the
     * declaration already uses, such as {@code c}, {@code t} or {@code roll}, are refused.
     */
    public static void registerOptions(String rendererId, TextEffectOption... options) {
        Map<String, TextEffectOption> known = OPTIONS
            .computeIfAbsent(qualify(rendererId), id -> new Object2ObjectOpenHashMap<>());
        for (TextEffectOption option : options) {
            if (RESERVED_PARAMETERS.contains(option.name()))
                throw new IllegalArgumentException("Option name is a declaration parameter: " + option.name());
            if (known.putIfAbsent(option.name(), option) != null)
                throw new IllegalArgumentException("Option already registered: " + option.name());
        }
    }

    public static TextEffectOption option(String rendererId, String name) {
        Map<String, TextEffectOption> known = OPTIONS.get(rendererId);
        return known == null ? null : known.get(name);
    }

    public static void registerAlias(String alias, String rendererId) {
        if (alias == null || !alias.matches("[a-z][a-z0-9_]{0,15}"))
            throw new IllegalArgumentException("Invalid text effect alias");
        new TextEffectStyle(rendererId, IntList.of(), 1);
        if (ALIASES.putIfAbsent(alias, rendererId) != null)
            throw new IllegalArgumentException("Text effect alias already registered: " + alias);
    }

    public static void legacyNamespace(String namespace) {
        LEGACY_NAMESPACES.add(namespace);
    }

    public static String qualify(String id) {
        int colon = id.indexOf(':');
        if (colon < 0) return GTNRLib.MODID + ":" + id;
        return LEGACY_NAMESPACES.contains(id.substring(0, colon)) ? GTNRLib.MODID + id.substring(colon) : id;
    }

    public static String aliasFor(String rendererId) {
        String shortest = "";
        for (Entry<String, String> entry : ALIASES.entrySet()) {
            String target = entry.getValue();
            target = qualify(target);
            String alias = entry.getKey();
            if (target.equals(rendererId) && (shortest.isEmpty() || alias.length() < shortest.length()
                || alias.length() == shortest.length() && alias.compareTo(shortest) < 0)) shortest = alias;
        }
        return shortest;
    }

    public static String normalizeAmpersand(String token) {
        if (token.length() < 2 || token.charAt(0) != '&') return token;
        char code = Character.toLowerCase(token.charAt(1));
        if (token.length() == 8 && code == '#') return hexToken(token.substring(2));
        if (token.length() == 18 && code == 'g')
            return "\u00a7g" + hexToken(token.substring(4, 10)) + hexToken(token.substring(12));
        if (token.length() == 10 && code == 'u') return "\u00a7u" + hexToken(token.substring(4));
        if (token.length() == 14 && code == 'x') return token.replace('&', '\u00a7');
        if (token.length() == 2 && "0123456789abcdefklmnor".indexOf(code) >= 0) return "\u00a7" + code;
        return token;
    }

    private static String hexToken(String hex) {
        StringBuilder result = new StringBuilder("\u00a7x");
        for (int i = 0; i < hex.length(); i++) result.append('\u00a7')
            .append(hex.charAt(i));
        return result.toString();
    }

    /** Returns the hold in seconds of a pause marker header such as {@code p;0.8}, or -1 for any other header. */
    public static float readPause(String header) {
        Matcher matcher = PAUSE.matcher(header);
        if (!matcher.matches()) return -1;
        float seconds = matcher.group(1) == null ? DEFAULT_PAUSE_SECONDS : Float.parseFloat(matcher.group(1));
        return seconds <= MAX_PAUSE_SECONDS ? seconds : -1;
    }

    /** Writes a time or option value rounded to three decimals without trailing zeros, for example {@code 2.5}. */
    public static String writeDecimal(float value) {
        return BigDecimal.valueOf(Math.round(value * 1000), 3)
            .stripTrailingZeros()
            .toPlainString();
    }

    public static String pauseMarker(float seconds) {
        if (!Float.isFinite(seconds) || seconds < 0 || seconds > MAX_PAUSE_SECONDS)
            throw new IllegalArgumentException("Pause must be between zero and " + MAX_PAUSE_SECONDS + " s");
        String written = writeDecimal(seconds);
        return INLINE_OPEN + "p" + (written.equals(writeDecimal(DEFAULT_PAUSE_SECONDS)) ? "" : ";" + written) + "}";
    }

    public static boolean isTypewriter(String rendererId) {
        return rendererId.equals(qualify(TYPEWRITER));
    }

    public static boolean isRoll(String rendererId) {
        return rendererId.equals(qualify(ROLL));
    }

    public static boolean isShake(String rendererId) {
        return rendererId.equals(qualify(SHAKE));
    }

    /** Writes the effect's options as {@code name=value} joined by {@code separator}, in name order. */
    public static String optionsValue(TextEffectStyle style, String separator) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Float> entry : style.options()
            .entrySet()) {
            TextEffectOption option = option(style.rendererId(), entry.getKey());
            if (result.length() > 0) result.append(separator);
            result.append(entry.getKey())
                .append('=')
                .append(option == null ? writeDecimal(entry.getValue()) : option.write(entry.getValue()));
        }
        return result.toString();
    }

    /** Reads {@code name=value} pairs joined by commas, as the scoped form writes them. */
    public static TextEffectStyle readOptions(TextEffectStyle style, String value) {
        for (String pair : value.split(",", -1)) {
            int separator = pair.indexOf('=');
            if (separator < 0) throw new IllegalArgumentException("Invalid option");
            style = readOption(style, pair.substring(0, separator), pair.substring(separator + 1));
        }
        return style;
    }

    private static TextEffectStyle readOption(TextEffectStyle style, String name, String value) {
        TextEffectOption option = option(style.rendererId(), name);
        if (option == null || style.options()
            .containsKey(name)) throw new IllegalArgumentException("Unknown or repeated option " + name);
        return style.withOption(name, option.read(value));
    }

    /** Text-only renderers that need no registered shader: the typewriter, the number roll and the shake. */
    public static boolean isPlainRenderer(String rendererId) {
        return isTypewriter(rendererId) || isRoll(rendererId) || isShake(rendererId);
    }

    /** Writes the shake as text, for example {@code 2:sync:jump}, or an empty string when the span stays still. */
    public static String shakeValue(Shake shake) {
        if (shake.pixels() == 0) return "";
        return shake.pixels() + (shake.mode() == ShakeMode.EACH ? "" : ":" + shake.mode().id)
            + (shake.motion() == ShakeMotion.SMOOTH ? "" : ":" + shake.motion().id);
    }

    /** Reads a pixel amount followed by any of {@code each|sync} and {@code smooth|jump}, each group at most once. */
    public static TextEffectStyle readShake(TextEffectStyle style, String value) {
        String[] parts = value.split(":", -1);
        int start = 0;
        int pixels = DEFAULT_SHAKE;
        if (SHAKE_AMOUNT.matcher(parts[0])
            .matches()) {
            pixels = Integer.parseInt(parts[0]);
            start = 1;
        }
        ShakeMode mode = null;
        ShakeMotion motion = null;
        for (int i = start; i < parts.length; i++) {
            ShakeMode readMode = ShakeMode.byId(parts[i]);
            ShakeMotion readMotion = ShakeMotion.byId(parts[i]);
            if (readMode != null && mode == null) mode = readMode;
            else if (readMotion != null && motion == null) motion = readMotion;
            else throw new IllegalArgumentException("Invalid shake");
        }
        return style
            .withShake(pixels, mode == null ? ShakeMode.EACH : mode, motion == null ? ShakeMotion.SMOOTH : motion);
    }

    /**
     * Writes the entrance as text, for example {@code decode:0.6} or {@code slam:0.3:4}, or an empty string when there
     * is none. A slam's size needs the time before it, so the default time is written out then.
     */
    public static String entranceValue(Typewriter typewriter) {
        if (typewriter.entrance() == TextEntrance.NONE) return "";
        boolean sized = typewriter.entranceSize() > 0;
        return typewriter.entrance().id
            + (typewriter.entranceSeconds() > 0 || sized ? ":" + writeDecimal(typewriter.entranceDuration()) : "")
            + (sized ? ":" + writeDecimal(typewriter.entranceSize()) : "");
    }

    /**
     * Writes the roll as text, for example {@code 2.5:jump:slot:nowait}, or an empty string when the span does not
     * roll. The blur is written apart, as {@code blur=} in the inline form and {@link #blurWord} in the scoped one.
     */
    public static String rollValue(Roll roll) {
        if (roll.seconds() == 0) return "";
        return writeDecimal(roll.seconds()) + (roll.mode() == RollMode.AUTO ? "" : ":" + roll.mode().id)
            + (roll.slot() == SlotOrder.NONE ? "" : ":" + SLOT)
            + (roll.slot() == SlotOrder.LEFT_FIRST ? ":" + SlotOrder.LEFT_FIRST.id : "")
            + (roll.waits() ? "" : ":" + NO_WAIT);
    }

    /** The blur as a roll word such as {@code :blur=40}, or an empty string for the default. */
    public static String blurWord(Roll roll) {
        return roll.blur() == TextEffectStyle.DEFAULT_ROLL_BLUR ? "" : ":" + BLUR_PARAMETER + "=" + roll.blur();
    }

    public static TextEffectStyle readEntrance(TextEffectStyle style, String value) {
        Matcher matcher = ENTRANCE.matcher(value);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid entrance");
        TextEntrance entrance = TextEntrance.byId(matcher.group(1));
        if (entrance == null) throw new IllegalArgumentException("Unknown entrance");
        return style.withEntrance(
            entrance,
            matcher.group(2) == null ? 0 : Float.parseFloat(matcher.group(2)),
            matcher.group(3) == null ? 0 : Float.parseFloat(matcher.group(3)));
    }

    /**
     * Reads a time followed by any of {@code cut|fade|jump}, {@code slot} and, with {@code slot}, {@code right|left},
     * {@code nowait} and {@code blur=} with a percent, each at most once and in any order, as in
     * {@code 2.5:slot:left:nowait}.
     */
    public static TextEffectStyle readRoll(TextEffectStyle style, String value) {
        String[] parts = value.split(":", -1);
        int start = 0;
        float seconds = DEFAULT_ROLL_SECONDS;
        if (ROLL_SECONDS.matcher(parts[0])
            .matches()) {
            seconds = Float.parseFloat(parts[0]);
            start = 1;
        }
        RollMode mode = null;
        boolean slot = false;
        SlotOrder order = null;
        boolean waits = true;
        int blur = -1;
        String blurPrefix = BLUR_PARAMETER + "=";
        for (int i = start; i < parts.length; i++) {
            RollMode readMode = RollMode.byId(parts[i]);
            SlotOrder readOrder = SlotOrder.byId(parts[i]);
            if (readMode != null && mode == null) mode = readMode;
            else if (parts[i].equals(SLOT) && !slot) slot = true;
            else if (readOrder != null && order == null) order = readOrder;
            else if (parts[i].equals(NO_WAIT) && waits) waits = false;
            else if (parts[i].startsWith(blurPrefix) && blur < 0)
                blur = readBlur(parts[i].substring(blurPrefix.length()));
            else throw new IllegalArgumentException("Invalid roll");
        }
        if (order != null && !slot) throw new IllegalArgumentException("A stop order needs slot");
        SlotOrder written = slot ? order == null ? SlotOrder.RIGHT_FIRST : order : SlotOrder.NONE;
        return style.withRoll(
            new Roll(
                seconds,
                mode == null ? RollMode.AUTO : mode,
                written,
                blur < 0 ? TextEffectStyle.DEFAULT_ROLL_BLUR : blur,
                waits));
    }

    private static int readBlur(String value) {
        if (!BLUR.matcher(value)
            .matches()) throw new IllegalArgumentException("Invalid blur");
        return Integer.parseInt(value);
    }

    /**
     * Reads the inside of {@code &{...}}. Every parameter but colors and the effect's own animation speed is named:
     * {@code t=} typing rate in glyphs per second, {@code r=} entrance with an optional time (and a slam's size),
     * {@code roll=} roll time with optional words, {@code blur=} a slot roll's smear in percent, {@code shake=}
     * pixels with optional modes, and the options an effect registers for
     * itself, such as {@code fault=} for the neon flicker. Times are in seconds. Plain text may be
     * written {@code &{roll=1.5}} or {@code &{shake=2:sync}}.
     */
    public static TextEffectStyle readInline(String header) {
        String[] fields = header.split(";", -1);
        String id = fields[0].trim();
        String roll = null;
        String shake = null;
        String blur = null;
        Map<String, String> options = new Object2ObjectLinkedOpenHashMap<>();
        if (id.startsWith(ROLL_PARAMETER)) {
            roll = id.substring(ROLL_PARAMETER.length())
                .trim();
            id = ROLL;
        } else if (id.startsWith(SHAKE_PARAMETER)) {
            shake = id.substring(SHAKE_PARAMETER.length())
                .trim();
            id = SHAKE;
        }
        id = ALIASES.getOrDefault(id, id);
        id = qualify(id);
        IntList colors = IntList.of();
        float speed = 1;
        float typing = 0;
        String entrance = null;
        boolean hasColors = false;
        boolean hasSpeed = false;
        boolean hasTyping = false;
        try {
            for (int i = 1; i < fields.length; i++) {
                String field = fields[i].trim();
                int separator = field.indexOf('=');
                if (separator < 0) {
                    if (field.startsWith("#") && !hasColors) {
                        colors = readColors(field);
                        hasColors = true;
                    } else if (!field.startsWith("#") && !hasSpeed) {
                        speed = Float.parseFloat(field);
                        hasSpeed = true;
                    } else return null;
                    continue;
                }
                String name = field.substring(0, separator)
                    .trim();
                String value = field.substring(separator + 1)
                    .trim();
                if ((name.equals("colors") || name.equals("color") || name.equals("c")) && !hasColors) {
                    colors = readColors(value);
                    hasColors = true;
                } else if ((name.equals("speed") || name.equals("s")) && !hasSpeed) {
                    speed = Float.parseFloat(value);
                    hasSpeed = true;
                } else if ((name.equals("type") || name.equals("t")) && !hasTyping) {
                    typing = Float.parseFloat(value);
                    hasTyping = true;
                } else if ((name.equals("reveal") || name.equals("r")) && entrance == null) {
                    entrance = value;
                } else if (name.equals("roll") && roll == null) {
                    roll = value;
                } else if (name.equals("shake") && shake == null) {
                    shake = value;
                } else if (name.equals(BLUR_PARAMETER) && blur == null) {
                    blur = value;
                } else if (option(id, name) != null && !options.containsKey(name)) {
                    options.put(name, value);
                } else return null;
            }
            // The typewriter, roll and shake have no animation of their own, so an unnamed speed would be a mistake.
            if (isPlainRenderer(id) && hasSpeed) return null;
            if (isTypewriter(id) && !hasTyping) typing = DEFAULT_TYPING_RATE;
            if (isRoll(id)) {
                if (roll == null) roll = writeDecimal(DEFAULT_ROLL_SECONDS);
                typing = 0;
                entrance = null;
            }
            if (isShake(id) && shake == null) shake = String.valueOf(DEFAULT_SHAKE);
            TextEffectStyle style = new TextEffectStyle(id, colors, speed).withTyping(typing);
            if (entrance != null) style = readEntrance(style, entrance);
            if (roll != null) style = readRoll(style, roll);
            if (blur != null && (roll == null || roll.contains(BLUR_PARAMETER + "="))) return null;
            if (blur != null) style = style.withRollBlur(readBlur(blur));
            if (shake != null) style = readShake(style, shake);
            for (Map.Entry<String, String> option : options.entrySet())
                style = readOption(style, option.getKey(), option.getValue());
            return style;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static IntList readColors(String value) {
        String[] entries = value.split(",", -1);
        if (entries.length > 8) throw new IllegalArgumentException("At most eight colors are supported");
        IntArrayList colors = new IntArrayList(entries.length);
        for (String entry : entries) {
            String hex = entry.trim();
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.length() != 3 && hex.length() != 6)
                throw new IllegalArgumentException("Colors must contain three or six hexadecimal digits");
            for (int i = 0; i < hex.length(); i++) {
                if (Character.digit(hex.charAt(i), 16) < 0) throw new IllegalArgumentException("Invalid RGB color");
            }
            int color = Integer.parseInt(hex, 16);
            if (hex.length() == 3)
                color = (color >> 8 & 15) * 0x110000 | (color >> 4 & 15) * 0x1100 | (color & 15) * 0x11;
            colors.add(color);
        }
        return colors;
    }

    public static int nativeLength(String text, int offset) {
        if (offset + 1 >= text.length() || text.charAt(offset) != '\u00a7') return 0;
        char code = Character.toLowerCase(text.charAt(offset + 1));
        if (code == 'g' && hexColor(text, offset + 2) >= 0 && hexColor(text, offset + 16) >= 0) return 30;
        if (code == 'u' && hexColor(text, offset + 2) >= 0) return 16;
        if (code == 'x' && hexColor(text, offset) >= 0) return 14;
        return 2;
    }

    public static int hexColor(String text, int offset) {
        if (offset + 14 > text.length() || text.charAt(offset) != '\u00a7'
            || Character.toLowerCase(text.charAt(offset + 1)) != 'x') return -1;
        int color = 0;
        for (int i = offset + 2; i < offset + 14; i += 2) {
            int digit = Character.digit(text.charAt(i + 1), 16);
            if (text.charAt(i) != '\u00a7' || digit < 0) return -1;
            color = color << 4 | digit;
        }
        return color;
    }

    public static TextEffectStyle applyColor(TextEffectStyle style, String token) {
        char code = Character.toLowerCase(token.charAt(1));
        if (code == 'r') return null;
        if (style == null) return null;
        int color = "0123456789abcdef".indexOf(code);
        if (color >= 0) return style.withColors(TextEffectColors.vanillaColor(color));
        if (token.length() == 14) return style.withColors(hexColor(token, 0));
        if (token.length() == 30) return style.withColors(hexColor(token, 2), hexColor(token, 16));
        return style;
    }

    public static String applyFormatting(String formatting, String token, boolean hexResetsStyles) {
        char code = Character.toLowerCase(token.charAt(1));
        if (code == 'r') return "";
        if ("0123456789abcdef".indexOf(code) >= 0) return token + retain(formatting, "zvu", true);
        if (token.length() == 14 || token.length() == 30) {
            return token + retain(formatting, hexResetsStyles && code == 'x' ? "zvu" : "klmnozvu", true);
        }
        if (code == 'u' && token.length() == 16) return retain(formatting, "u", false) + token;
        if ("zvu".indexOf(code) >= 0) {
            String remaining = retain(formatting, String.valueOf(code), false);
            return remaining.length() == formatting.length() ? formatting + token : remaining;
        }
        if (code == 'q') return retain(formatting, "gxq", false) + token;
        // Unknown extension codes may toggle registered Angelica effects, so retain every occurrence.
        return "klmno".indexOf(code) >= 0 && formatting.contains(token) ? formatting : formatting + token;
    }

    private static String retain(String formatting, String codes, boolean matching) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < formatting.length();) {
            int length = nativeLength(formatting, i);
            if (length == 0) break;
            boolean match = codes.indexOf(Character.toLowerCase(formatting.charAt(i + 1))) >= 0;
            if (match == matching) result.append(formatting, i, i + length);
            i += length;
        }
        return result.toString();
    }
}
