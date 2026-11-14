package com.xyp.gtnrlib.client.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.xyp.gtnrlib.text.ShakeMode;
import com.xyp.gtnrlib.text.ShakeMotion;
import com.xyp.gtnrlib.text.TextEffectStyle.Shake;

public class TextShakeTest {

    @Test
    public void jumpingShakesHoldWholePixelsForEachStep() {
        float first = TextShake.offset(3, 1, 0.01, 2, true);
        assertEquals(first, Math.round(first), "a jumping shake stays on whole pixels");
        assertEquals(first, TextShake.offset(3, 1, 0.08, 2, true), "and holds until the next step");
        for (int step = 0; step < 50; step++) {
            float offset = TextShake.offset(1, 0, step / TextShake.STEPS_PER_SECOND, 2, true);
            assertTrue(Math.abs(offset) <= 2, "within the amplitude");
        }
    }

    @Test
    public void smoothShakesGlideBetweenTheTargets() {
        double step = 1 / TextShake.STEPS_PER_SECOND;
        float start = TextShake.offset(5, 0, 4 * step, 3, false);
        float end = TextShake.offset(5, 0, 5 * step, 3, false);
        float middle = TextShake.offset(5, 0, 4.5 * step, 3, false);
        assertEquals((start + end) / 2, middle, 0.001f, "half way between two steps sits half way between targets");
        float justAfter = TextShake.offset(5, 0, 4 * step + 0.001, 3, false);
        assertEquals(start, justAfter, 0.01f, "the glide eases out of each target instead of jumping");
    }

    @Test
    public void aShakeStyleMovesGlyphsTogetherOrOnTheirOwn() {
        Shake together = new Shake(2, ShakeMode.SYNC, ShakeMotion.JUMP);
        assertEquals(
            TextShake.offset(together, 0, 0, 0.3),
            TextShake.offset(together, 7, 0, 0.3),
            "glyphs move as one");
        Shake each = new Shake(2, ShakeMode.EACH, ShakeMotion.JUMP);
        assertEquals(TextShake.offset(4, 0, 0.3, 1, true), TextShake.offset(each, 4, 0, 0.3), "half as far sideways");
        assertEquals(TextShake.offset(4, 1, 0.3, 2, true), TextShake.offset(each, 4, 1, 0.3), "full height down");
        assertEquals(0, TextShake.offset(Shake.NONE, 4, 1, 0.3), "no shake stays still");
    }

    @Test
    public void glyphsOnTheirOwnMoveHalfAsFarSideways() {
        assertEquals(0, TextShake.sideways(1));
        assertEquals(1, TextShake.sideways(2));
        assertEquals(1, TextShake.sideways(3));
        assertEquals(0.5f, TextShake.toGrid(0.3f, 2));
    }
}
