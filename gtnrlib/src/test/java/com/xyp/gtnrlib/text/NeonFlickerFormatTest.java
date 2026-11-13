package com.xyp.gtnrlib.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.xyp.gtnrlib.text.EffectTextParser.Cursor;

public class NeonFlickerFormatTest {

    @Test
    public void faultChanceIsReadForTheNeonFlickerOnly() {
        TextEffectStyle neon = TextEffectFormat.readInline("nf");
        assertEquals(TextEffects.NEON_FLICKER, neon);
        assertEquals(-1f, neon.option(TextEffectFormat.FAULT, -1), "without fault= the effect keeps its own default");
        assertEquals(
            35f,
            TextEffectFormat.readInline("nf;#4ff,#08f;fault=35")
                .option(TextEffectFormat.FAULT, -1));
        assertEquals(
            0f,
            TextEffectFormat.readInline("neon_flicker;fault=0")
                .option(TextEffectFormat.FAULT, -1),
            "zero means no glyph fails on its own");
        assertNull(TextEffectFormat.readInline("nf;fault=101"), "chances above 100 percent are rejected");
        assertNull(TextEffectFormat.readInline("nf;fault="), "an empty chance is rejected");
        assertNull(TextEffectFormat.readInline("nf;fault=2.5"), "the chance is a whole percent");
        assertNull(TextEffectFormat.readInline("ba;fault=10"), "other effects have no failing glyphs");
        assertNull(TextEffectFormat.readInline("nf;fault=10;fault=20"), "a repeated chance rejects the declaration");
    }

    @Test
    public void bothWrittenFormsKeepTheFaultChance() {
        TextEffectStyle style = TextEffects.NEON_FLICKER.withColors(0x44FFFF)
            .withFault(60);
        String inline = TextEffects.format(style);
        assertEquals(style, TextEffectFormat.readInline(inline.substring(2, inline.length() - 1)));
        assertEquals(
            style,
            new Cursor(TextEffects.apply("OPEN", style)).next()
                .style(),
            "a fault without typing, entrance, roll or shake still survives the scoped form");
        TextEffectStyle shaking = style.withShake(1);
        assertEquals(
            shaking,
            new Cursor(TextEffects.apply("OPEN", shaking)).next()
                .style());
    }
}
