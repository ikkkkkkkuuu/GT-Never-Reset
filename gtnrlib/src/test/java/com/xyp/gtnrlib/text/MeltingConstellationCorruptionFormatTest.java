package com.xyp.gtnrlib.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class MeltingConstellationCorruptionFormatTest {

    @Test
    public void theAliasesAndColorsAreRead() {
        assertEquals(TextEffects.MELTING, TextEffectFormat.readInline("mt"));
        assertEquals(TextEffects.CONSTELLATION, TextEffectFormat.readInline("cn"));
        assertEquals(TextEffects.CORRUPTION, TextEffectFormat.readInline("tc"));
        assertEquals(TextEffects.CORRUPTION, TextEffectFormat.readInline("corruption"));
        assertEquals(
            TextEffects.CORRUPTION.withColors(0x221133, 0xCC66FF, 0x551188, 0xFF55DD),
            TextEffectFormat.readInline("tc;#213,#c6f,#518,#f5d"),
            "the corruption takes a fourth flux color");
        assertNull(TextEffectFormat.readInline("mt;fault=20"), "the new effects have no options of their own");
    }
}
