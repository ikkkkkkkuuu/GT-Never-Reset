package com.xyp.gtnrlib.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.xyp.gtnrlib.text.EffectTextParser.Cursor;
import com.xyp.gtnrlib.text.EffectTextParser.Token;

public class TypewriterFormatTest {

    @Test
    public void typingRateIsReadFromInlineDeclarations() {
        assertEquals(
            20,
            TextEffectFormat.readInline("ba;t=20")
                .typing());
        assertEquals(
            "gtnrlib:burnished_auric",
            TextEffectFormat.readInline("ba;#fc0;2;type=7.5")
                .rendererId());
        assertEquals(
            7.5f,
            TextEffectFormat.readInline("ba;#fc0;2;type=7.5")
                .typing());
        assertEquals(
            0,
            TextEffectFormat.readInline("ba")
                .typing());
        assertNull(TextEffectFormat.readInline("ba;t=2000"), "rates above the limit reject the declaration");
        assertNull(TextEffectFormat.readInline("ba;t=1;t=2"), "a repeated rate rejects the declaration");
    }

    @Test
    public void plainTypewriterReadsItsRate() {
        assertEquals(TextEffects.TYPEWRITER, TextEffectFormat.readInline("tw"));
        assertEquals(
            40,
            TextEffectFormat.readInline("tw;40")
                .typing());
        assertEquals(
            5,
            TextEffectFormat.readInline("typewriter;t=5")
                .typing());
        assertEquals(
            1,
            TextEffectFormat.readInline("tw;40")
                .speed());
    }

    @Test
    public void pauseMarkersHoldWithoutStyle() {
        assertEquals(0.5f, TextEffectFormat.readPause("p"));
        assertEquals(0.8f, TextEffectFormat.readPause("p;800"));
        assertEquals(0.1f, TextEffectFormat.readPause("pause;d=100"));
        assertEquals(-1, TextEffectFormat.readPause("p;10001"));
        assertEquals(-1, TextEffectFormat.readPause("pu"));
        assertEquals("§{p}", TextEffects.pause(500));
        assertEquals("警告……", TextEffects.plainText("&{tw}警告&{p;800}……"));
        Cursor cursor = new Cursor("&{ba}A&{p;250}B");
        assertEquals(
            "A",
            cursor.next()
                .text());
        Token pause = cursor.next();
        assertEquals("", pause.text());
        assertEquals(0.25f, pause.pause());
        assertEquals(
            "gtnrlib:burnished_auric",
            pause.style()
                .rendererId(),
            "a pause keeps the active effect");
        assertEquals(
            "B",
            cursor.next()
                .text());
    }

    @Test
    public void bothWrittenFormsKeepTheRate() {
        TextEffectStyle disabled = TextEffects.TYPEWRITER.withTyping(0);
        String disabledText = TextEffects.format(disabled);
        assertEquals(disabled, TextEffectFormat.readInline(disabledText.substring(2, disabledText.length() - 1)));
        TextEffectStyle style = TextEffects.BURNISHED_AURIC.withColors(0xFFCC00)
            .withTyping(12);
        assertEquals(
            style,
            TextEffectFormat.readInline(
                TextEffects.format(style)
                    .substring(
                        2,
                        TextEffects.format(style)
                            .length() - 1)));
        Cursor scoped = new Cursor(TextEffects.apply("ab", style));
        assertEquals(
            style,
            scoped.next()
                .style());
        Cursor legacy = new Cursor(EffectTextParser.OPEN + "gtnrlib:burnished_auric;1.0;" + EffectTextParser.END + "a");
        assertEquals(
            0,
            legacy.next()
                .style()
                .typing(),
            "scoped text written before typing existed still parses");
    }
}
