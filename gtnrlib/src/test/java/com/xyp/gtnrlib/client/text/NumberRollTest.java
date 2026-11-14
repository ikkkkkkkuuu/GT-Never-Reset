package com.xyp.gtnrlib.client.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.xyp.gtnrlib.client.text.EffectTextLayout.Glyph;
import com.xyp.gtnrlib.client.text.NumberRoll.Frame;
import com.xyp.gtnrlib.client.text.NumberRoll.Layer;
import com.xyp.gtnrlib.text.SlotOrder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class NumberRollTest {

    private static List<Glyph> glyphs(String text) {
        List<Glyph> glyphs = new ObjectArrayList<>();
        for (char character : text.toCharArray()) glyphs.add(new Glyph(String.valueOf(character), "", null, 6));
        return glyphs;
    }

    @Test
    public void numbersCountUpAndGrowFromTheLeft() {
        List<Glyph> number = glyphs("1,048,576");
        assertEquals(
            "0",
            NumberRoll.frame(number, 0, 2, true, SlotOrder.NONE, 0)
                .jumpText(),
            "a number starts at zero");
        assertEquals(
            "1,048,576",
            NumberRoll.frame(number, 1, 2, true, SlotOrder.NONE, 0)
                .jumpText());
        Frame early = NumberRoll.frame(number, 0.2f, 2, true, SlotOrder.NONE, 0);
        assertTrue(early.hidden() > 0, "wheels the value has not reached take no room");
        Layer first = early.layers()
            .get(0);
        assertEquals(0f, first.x(), 0.01f, "the shown digits start at the left edge");
    }

    @Test
    public void jumpingNumbersStepAndSlidingNumbersTurn() {
        List<Glyph> number = glyphs("2048");
        assertEquals(
            NumberRoll.frame(number, 0.201f, 2, true, SlotOrder.NONE, 0)
                .jumpText(),
            NumberRoll.frame(number, 0.224f, 2, true, SlotOrder.NONE, 0)
                .jumpText(),
            "a jumping number holds between steps");
        Frame sliding = NumberRoll.frame(number, 0.3f, 2, false, SlotOrder.NONE, 0);
        assertTrue(
            sliding.layers()
                .stream()
                .anyMatch(Layer::moving),
            "a sliding number has a digit turning past the line");
        assertTrue(
            NumberRoll.frame(number, 0.3f, 2, true, SlotOrder.NONE, 0)
                .layers()
                .stream()
                .noneMatch(Layer::moving),
            "a jumping number never shows half a digit");
    }

    @Test
    public void separateNumbersRollOnTheirOwn() {
        for (float progress : new float[] { 0.3f, 0.6f }) {
            assertEquals(
                jumpText("32", progress) + " / " + jumpText("64", progress),
                jumpText("32 / 64", progress),
                "a slash splits two numbers");
            assertEquals(
                jumpText("3", progress) + "x " + jumpText("128", progress),
                jumpText("3x 128", progress),
                "a letter and a space split two numbers");
        }
        Layer prefix = NumberRoll.frame(glyphs("x 128"), 0.1f, 2, true, SlotOrder.NONE, 0)
            .layers()
            .get(0);
        assertEquals("x", prefix.text());
        assertEquals(0f, prefix.x(), 0.01f, "text before a number keeps its place while the number grows");
    }

    @Test
    public void leadingZerosAndDecimalsShowFromTheStart() {
        assertEquals("0.00", jumpText("0.85", 0), "a decimal keeps the digit before the point and its decimals");
        assertTrue(jumpText("0.85", 0.5f).startsWith("0."), "and still reads as a decimal half way");
        assertEquals("0.0", jumpText("12.5", 0), "the integer part grows in front of the point");
        assertEquals("0.0", jumpText("1,048.5", 0), "a comma before a decimal point still groups thousands");
        assertEquals("000", jumpText("007", 0), "a number written with leading zeros keeps all its wheels");
        assertEquals("0", jumpText("1.048.576", 0), "repeated points with three digits after them group thousands");
        assertEquals("0", jumpText("12.345", 0), "a lone point with three digits after it reads as thousands");
    }

    @Test
    public void longNumbersLandExactly() {
        for (String number : new String[] { "9223372036854775807", "123,456,789,012,345,678,901" }) {
            assertEquals(number, jumpText(number, 1), "every wheel lands on its digit");
            assertEquals(
                number,
                NumberRoll.frame(glyphs(number), 1, 2, false, SlotOrder.NONE, 0)
                    .jumpText(),
                "a sliding number lands too");
        }
    }

    @Test
    public void slotsShowEveryWheelAndStopOneAfterAnother() {
        List<Glyph> number = glyphs("1,048,576");
        assertEquals(
            9,
            NumberRoll.frame(number, 0, 2, true, SlotOrder.RIGHT_FIRST, 0)
                .jumpText()
                .length(),
            "every wheel and comma shows from the start");
        for (SlotOrder order : new SlotOrder[] { SlotOrder.RIGHT_FIRST, SlotOrder.LEFT_FIRST }) {
            assertEquals(
                "1,048,576",
                NumberRoll.frame(number, 1, 2, false, order, 0)
                    .jumpText(),
                "every wheel lands on its digit");
        }
        Frame rightFirst = NumberRoll.frame(number, 0.5f, 2, false, SlotOrder.RIGHT_FIRST, 0);
        assertTrue(stopped(rightFirst, 8, "6"), "the ones wheel stops first");
        assertTrue(spinning(rightFirst, 0), "while the highest wheel still spins");
        Frame leftFirst = NumberRoll.frame(number, 0.5f, 2, false, SlotOrder.LEFT_FIRST, 0);
        assertTrue(stopped(leftFirst, 0, "1"), "from the left the highest wheel stops first");
        assertTrue(spinning(leftFirst, 8), "while the ones wheel still spins");
    }

    @Test
    public void spinningSlotsSmearAndStoppedOnesDoNot() {
        List<Glyph> number = glyphs("1,048,576");
        Frame frame = NumberRoll.frame(number, 0.5f, 2, false, SlotOrder.RIGHT_FIRST, 100);
        assertTrue(
            layersAt(frame, 0).stream()
                .anyMatch(layer -> layer.alpha() < 1),
            "a spinning wheel trails faint copies");
        assertTrue(stopped(frame, 8, "6"), "a stopped wheel shows its digit alone");
        assertTrue(
            NumberRoll.frame(number, 0.5f, 2, false, SlotOrder.RIGHT_FIRST, 0)
                .layers()
                .stream()
                .allMatch(layer -> layer.alpha() == 1),
            "blur 0 turns the smear off");
        assertTrue(
            NumberRoll.frame(number, 0.5f, 2, true, SlotOrder.RIGHT_FIRST, 100)
                .layers()
                .stream()
                .allMatch(layer -> layer.alpha() == 1),
            "a jumping number never smears");
    }

    private static boolean stopped(Frame frame, int glyph, String digit) {
        List<Layer> layers = layersAt(frame, glyph);
        return layers.size() == 1 && layers.get(0)
            .text()
            .equals(digit)
            && !layers.get(0)
                .moving();
    }

    private static boolean spinning(Frame frame, int glyph) {
        return layersAt(frame, glyph).stream()
            .anyMatch(Layer::moving);
    }

    private static List<Layer> layersAt(Frame frame, int glyph) {
        return frame.layers()
            .stream()
            .filter(layer -> Math.abs(layer.x() - glyph * 6) < 0.01f)
            .toList();
    }

    private static String jumpText(String number, float progress) {
        return NumberRoll.frame(glyphs(number), progress, 2, true, SlotOrder.NONE, 0)
            .jumpText();
    }

    @Test
    public void separatorsFollowTheDigitOnTheirLeft() {
        List<Glyph> number = glyphs("1,000");
        assertEquals(
            "1,000",
            NumberRoll.frame(number, 1, 1, true, SlotOrder.NONE, 0)
                .jumpText());
        String early = NumberRoll.frame(number, 0.3f, 1, true, SlotOrder.NONE, 0)
            .jumpText();
        assertEquals(3, early.length(), "below a thousand only three wheels show");
        assertFalse(early.contains(","), "the comma waits for the thousands digit");
    }
}
