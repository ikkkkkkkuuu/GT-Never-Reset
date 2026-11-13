package com.xyp.gtnrlib.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.xyp.gtnrlib.text.EffectTextParser.Cursor;

public class ArcaneCipherFormatTest {

    @Test
    public void optionNamesCannotShadowDeclarationParameters() {
        for (String name : new String[] { "c", "t", "r", "roll", "shake" }) assertThrows(
            IllegalArgumentException.class,
            () -> TextEffectFormat.registerOptions("test_reserved", new TextEffectOption(name, 0, 1, true)),
            name);
    }

    @Test
    public void optionsAreReadForTheArcaneCipherOnly() {
        TextEffectStyle plain = TextEffectFormat.readInline("ac");
        assertEquals(TextEffects.ARCANE_CIPHER, plain);
        assertTrue(
            plain.options()
                .isEmpty(),
            "without options the effect keeps its own defaults");
        TextEffectStyle tuned = TextEffectFormat
            .readInline("ac;#7cf;hold=3.5;hide=1;sweep=.8;flip=0;shards=8;flick=40");
        assertEquals(3.5f, tuned.option(TextEffectFormat.HOLD, -1));
        assertEquals(1f, tuned.option(TextEffectFormat.HIDE, -1));
        assertEquals(0.8f, tuned.option(TextEffectFormat.SWEEP, -1));
        assertEquals(0f, tuned.option(TextEffectFormat.FLIP, -1));
        assertEquals(8f, tuned.option(TextEffectFormat.SHARDS, -1));
        assertEquals(40f, tuned.option(TextEffectFormat.FLICK, -1));
        assertEquals(
            0f,
            TextEffectFormat.readInline("ac;hold=0")
                .option(TextEffectFormat.HOLD, -1),
            "zero hold keeps the line in runes");
        assertNull(TextEffectFormat.readInline("ac;hide=0.4"), "hide is at least half a second");
        assertNull(TextEffectFormat.readInline("ac;sweep=11"), "sweep is at most ten seconds");
        assertNull(TextEffectFormat.readInline("ac;shards=9"), "at most eight shards");
        assertNull(TextEffectFormat.readInline("ac;shards=2.5"), "shards are whole");
        assertNull(TextEffectFormat.readInline("ac;flick=101"), "flick is a percent");
        assertNull(TextEffectFormat.readInline("ac;hold=1;hold=2"), "a repeated option rejects the declaration");
        assertNull(TextEffectFormat.readInline("ba;hold=2"), "other effects have no cipher options");
        assertNull(TextEffectFormat.readInline("ac;fault=10"), "the neon fault is not a cipher option");
    }

    @Test
    public void bothWrittenFormsKeepTheOptions() {
        TextEffectStyle style = TextEffects.ARCANE_CIPHER.withColors(0x77CCFF)
            .withOption(TextEffectFormat.HOLD, 4)
            .withOption(TextEffectFormat.SWEEP, 0.75f)
            .withOption(TextEffectFormat.SHARDS, 2);
        String inline = TextEffects.format(style);
        assertEquals(style, TextEffectFormat.readInline(inline.substring(2, inline.length() - 1)));
        assertEquals(
            style,
            new Cursor(TextEffects.apply("Arcane", style)).next()
                .style(),
            "options without typing, entrance, roll or shake still survive the scoped form");
        TextEffectStyle withShake = style.withShake(1);
        assertEquals(
            withShake,
            new Cursor(TextEffects.apply("Arcane", withShake)).next()
                .style());
        assertEquals(
            TextEffects.ARCANE_CIPHER.withOption(TextEffectFormat.HOLD, 0.3f),
            TextEffects.ARCANE_CIPHER.withOption(TextEffectFormat.HOLD, 0.1f + 0.2f),
            "option values round to the millisecond, so float noise does not split equal styles");
    }

    @Test
    public void everyParameterAndOptionFitInOneDeclaration() {
        TextEffectStyle style = TextEffects.ARCANE_CIPHER.withColors(0x77CCFF)
            .withSpeed(1.5f)
            .withTyping(20)
            .withEntrance(TextEntrance.SLAM)
            .withRoll(2, RollMode.FADE, SlotOrder.LEFT_FIRST)
            .withRollBlur(40)
            .withShake(2);
        for (String name : new String[] { TextEffectFormat.HOLD, TextEffectFormat.HIDE, TextEffectFormat.SWEEP,
            TextEffectFormat.FLIP, TextEffectFormat.SHARDS, TextEffectFormat.FLICK }) style = style.withOption(name, 2);
        String inline = TextEffects.format(style);
        assertEquals(style, TextEffectFormat.readInline(inline.substring(2, inline.length() - 1)), inline);
        assertEquals(
            style,
            new Cursor(TextEffects.apply("12", style)).next()
                .style());
    }
}
