package com.xyp.gtnrlib.client.text;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.xyp.gtnrlib.client.text.EffectTextLayout.Glyph;
import com.xyp.gtnrlib.client.text.EffectTextLayout.Layout;
import com.xyp.gtnrlib.client.text.TypewriterTimeline.Typing;
import com.xyp.gtnrlib.text.TextEffectStyle;
import com.xyp.gtnrlib.text.TextEffects;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TypewriterTimelineTest {

    private static final TextEffectStyle TEN_PER_SECOND = TextEffects.TYPEWRITER.withTyping(10);
    private static final char ONE_SECOND_PAUSE = '|';

    private static Layout layout(String text) {
        List<Glyph> glyphs = new ObjectArrayList<>();
        int lines = 1;
        for (char character : text.toCharArray()) {
            if (character == ONE_SECOND_PAUSE) glyphs.add(new Glyph("", "", TEN_PER_SECOND, 0, -1, -1, 1));
            else if (character == '\n') {
                glyphs.add(new Glyph("\n", "", TEN_PER_SECOND, 0));
                lines++;
            } else glyphs.add(new Glyph(String.valueOf(character), "", TEN_PER_SECOND, 6));
        }
        return new Layout(glyphs, 60, 9, 0, lines);
    }

    @Test
    public void linesOfOneStringTypeInTurn() {
        TypewriterTimeline timeline = new TypewriterTimeline();
        Layout layout = layout("AB\nCD");
        timeline.plan(null, "AB\nCD", layout, 0, 0, 0);
        Typing early = timeline.plan(null, "AB\nCD", layout, 0, 0, 0.05);
        assertArrayEquals(new int[] { 1, 0 }, early.counts(), "the second line waits while the first types");
        Typing late = timeline.plan(null, "AB\nCD", layout, 0, 0, 0.25);
        assertArrayEquals(new int[] { -1, 1 }, late.counts(), "hidden glyphs still take their time");
        assertNull(timeline.plan(null, "AB\nCD", layout, 0, 0, 0.45), "finished text shows in full");
    }

    @Test
    public void linesDrawnBelowInOneFrameWaitForTheLineAbove() {
        TypewriterTimeline timeline = new TypewriterTimeline();
        Layout first = layout("ABC");
        Layout second = layout("DE");
        timeline.plan(null, "ABC", first, 4, 10, 0);
        Typing waiting = timeline.plan(null, "DE", second, 4, 22, 0);
        assertArrayEquals(new int[] { 0 }, waiting.counts());
        timeline.nextFrame();
        timeline.plan(null, "ABC", first, 4, 10, 0.32);
        Typing typing = timeline.plan(null, "DE", second, 4, 22, 0.32);
        assertArrayEquals(new int[] { 1 }, typing.counts(), "the lower line starts once the upper one is typed");
        Typing elsewhere = timeline.plan(null, "XY", layout("XY"), 50, 40, 0.32);
        assertArrayEquals(new int[] { 1 }, elsewhere.counts(), "text at another x starts on its own");
    }

    @Test
    public void pausesHoldTheClockAndBlinkTheCaret() {
        TypewriterTimeline timeline = new TypewriterTimeline();
        Layout layout = layout("A|B");
        timeline.plan(null, "A|B", layout, 0, 0, 0);
        Typing held = timeline.plan(null, "A|B", layout, 0, 0, 0.5);
        assertArrayEquals(new int[] { 2 }, held.counts(), "the pause glyph is passed but B waits");
        assertEquals(-1, held.caretRun(), "the caret blinks off during the pause");
        Typing shown = timeline.plan(null, "A|B", layout, 0, 0, 0.85);
        assertEquals(0, shown.caretRun(), "and on again");
        assertArrayEquals(new int[] { 2 }, shown.counts());
        assertNull(timeline.plan(null, "A|B", layout, 0, 0, 1.25), "B follows the one-second hold");
    }

    @Test
    public void textThatReappearsTypesAgain() {
        TypewriterTimeline timeline = new TypewriterTimeline();
        Layout layout = layout("ABCD");
        timeline.plan(null, "ABCD", layout, 0, 0, 0);
        assertNull(timeline.plan(null, "ABCD", layout, 0, 0, 1));
        for (int i = 0; i < 2; i++) timeline.nextFrame();
        Typing again = timeline.plan(null, "ABCD", layout, 0, 0, 2);
        assertNotNull(again, "text hidden for a few frames starts over");
        assertArrayEquals(new int[] { 1 }, again.counts());
    }

    @Test
    public void plainTextIsNotTimed() {
        TypewriterTimeline timeline = new TypewriterTimeline();
        Layout layout = new Layout(List.of(new Glyph("A", "", null, 6)), 6, 9, 0, 1);
        assertNull(timeline.plan(null, "A", layout, 0, 0, 0));
        timeline.beginStill();
        assertNull(timeline.plan(null, "AB", layout("AB"), 0, 0, 0), "text fields show in full");
        timeline.endStill();
    }
}
