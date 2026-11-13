package com.xyp.gtnrlib.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class GravityLensFormatTest {

    @Test
    public void theAliasAndColorsAreRead() {
        assertEquals(TextEffects.GRAVITY_LENS, TextEffectFormat.readInline("gl"));
        assertEquals(TextEffects.GRAVITY_LENS, TextEffectFormat.readInline("gravity_lens"));
        assertEquals(
            TextEffects.GRAVITY_LENS.withColors(0xFFFFFF, 0xFF8800)
                .withSpeed(2),
            TextEffectFormat.readInline("gl;#fff,#f80;2"));
        assertNull(TextEffectFormat.readInline("gl;hold=2"), "the lens has no options of its own");
    }
}
