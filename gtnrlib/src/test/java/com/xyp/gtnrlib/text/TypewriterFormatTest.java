package com.xyp.gtnrlib.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                .typewriter()
                .rate());
        assertEquals(
            "gtnrlib:burnished_auric",
            TextEffectFormat.readInline("ba;#fc0;2;type=7.5")
                .rendererId());
        assertEquals(
            7.5f,
            TextEffectFormat.readInline("ba;#fc0;2;type=7.5")
                .typewriter()
                .rate());
        assertEquals(
            0,
            TextEffectFormat.readInline("ba")
                .typewriter()
                .rate());
        assertNull(TextEffectFormat.readInline("ba;t=2000"), "rates above the limit reject the declaration");
        assertNull(TextEffectFormat.readInline("ba;t=1;t=2"), "a repeated rate rejects the declaration");
    }

    @Test
    public void plainTypewriterReadsItsRate() {
        assertEquals(TextEffects.TYPEWRITER, TextEffectFormat.readInline("tw"));
        assertEquals(
            40,
            TextEffectFormat.readInline("tw;t=40")
                .typewriter()
                .rate());
        assertEquals(
            5,
            TextEffectFormat.readInline("typewriter;t=5")
                .typewriter()
                .rate());
        assertNull(TextEffectFormat.readInline("tw;40"), "the typing rate is always named");
        assertNull(TextEffectFormat.readInline("roll;1.5"), "the roll time is always named");
    }

    @Test
    public void pauseMarkersHoldWithoutStyle() {
        assertEquals(0.5f, TextEffectFormat.readPause("p"));
        assertEquals(0.8f, TextEffectFormat.readPause("p;0.8"));
        assertEquals(0.1f, TextEffectFormat.readPause("pause;d=0.1"));
        assertEquals(-1, TextEffectFormat.readPause("p;10.5"));
        assertEquals(-1, TextEffectFormat.readPause("pu"));
        assertEquals(-1, TextEffectFormat.readPause("p;800"), "times are seconds, so 800 is out of range");
        assertEquals(-1, TextEffectFormat.readPause("p;0.0001"), "times stop at milliseconds");
        assertEquals(0.25f, TextEffectFormat.readPause("p;.25"));
        assertEquals("§{p}", TextEffects.pause(0.5f));
        assertEquals("§{p;1.25}", TextEffects.pause(1.25f));
        assertEquals("2", TextEffectFormat.writeDecimal(2));
        assertEquals("0.05", TextEffectFormat.writeDecimal(0.05f));
        assertEquals("警告……", TextEffects.plainText("&{tw}警告&{p;0.8}……"));
        Cursor cursor = new Cursor("&{ba}A&{p;0.25}B");
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
    public void entrancesAndRollsAreRead() {
        TextEffectStyle decode = TextEffectFormat.readInline("tw;t=24;r=decode:0.6");
        assertEquals(
            TextEntrance.DECODE,
            decode.typewriter()
                .entrance());
        assertEquals(
            0.6f,
            decode.typewriter()
                .entranceSeconds());
        assertEquals(
            0.6f,
            decode.typewriter()
                .entranceDuration());
        assertEquals(
            0.25f,
            TextEffectFormat.readInline("tw;r=fade")
                .typewriter()
                .entranceDuration(),
            "an entrance without a time uses its default");
        assertNull(TextEffectFormat.readInline("tw;r=spin"), "an unknown entrance rejects the declaration");
        assertNull(TextEffectFormat.readInline("tw;r=fade:3"), "entrance times above the limit are rejected");
        TextEffectStyle rolled = TextEffectFormat.readInline("ba;roll=2.5:jump");
        assertEquals(
            2.5f,
            rolled.roll()
                .seconds());
        assertEquals(
            RollMode.JUMP,
            rolled.roll()
                .mode());
        assertEquals(
            RollMode.AUTO,
            TextEffectFormat.readInline("nr;roll=2")
                .roll()
                .mode());
        assertNull(TextEffectFormat.readInline("ba;roll=2:spin"), "an unknown roll mode rejects the declaration");
        assertNull(TextEffectFormat.readInline("ba;roll=11"), "roll times above the limit are rejected");
        assertEquals(TextEffects.ROLL, TextEffectFormat.readInline("roll"));
        assertEquals(
            1.5f,
            TextEffectFormat.readInline("roll=1.5")
                .roll()
                .seconds());
        TextEffectStyle modeOnly = TextEffectFormat.readInline("roll=fade");
        assertEquals(
            RollMode.FADE,
            modeOnly.roll()
                .mode());
        assertEquals(
            1f,
            modeOnly.roll()
                .seconds(),
            "a mode alone keeps the default time");
        TextEffectStyle plainJump = TextEffectFormat.readInline("roll=2:jump");
        assertEquals(
            2f,
            plainJump.roll()
                .seconds());
        assertEquals(
            RollMode.JUMP,
            plainJump.roll()
                .mode());
    }

    @Test
    public void slotRollsAreReadInAnyOrderAndWrittenBack() {
        assertEquals(
            TextEffects.ROLL.withRoll(2, RollMode.AUTO, SlotOrder.RIGHT_FIRST),
            TextEffectFormat.readInline("roll=2:slot"),
            "a slot stops from the right unless told otherwise");
        assertEquals(
            TextEffects.ROLL.withRoll(2, RollMode.FADE, SlotOrder.LEFT_FIRST),
            TextEffectFormat.readInline("roll=2:left:fade:slot"),
            "the words may come in any order");
        assertEquals(TextEffectFormat.readInline("roll=2:slot"), TextEffectFormat.readInline("roll=2:slot:right"));
        assertNull(TextEffectFormat.readInline("roll=2:left"), "a stop order needs slot");
        assertNull(TextEffectFormat.readInline("roll=2:slot:slot"), "each word at most once");
        assertNull(TextEffectFormat.readInline("roll=2:slot:left:right"), "only one stop order");
        TextEffectStyle leftFirst = TextEffects.BURNISHED_AURIC.withRoll(1.5f, RollMode.CUT, SlotOrder.LEFT_FIRST);
        String inline = TextEffects.format(leftFirst);
        assertEquals(
            leftFirst,
            TextEffectFormat.readInline(inline.substring(2, inline.length() - 1)),
            "the inline form");
        assertEquals(
            leftFirst,
            new Cursor(TextEffects.apply("9", leftFirst)).next()
                .style(),
            "the scoped form");
    }

    @Test
    public void shakesAreRead() {
        assertEquals(TextEffects.SHAKE, TextEffectFormat.readInline("shake"));
        TextEffectStyle together = TextEffectFormat.readInline("shake=2:sync:jump");
        assertEquals(
            2,
            together.shake()
                .pixels());
        assertEquals(
            ShakeMode.SYNC,
            together.shake()
                .mode());
        assertEquals(
            ShakeMotion.JUMP,
            together.shake()
                .motion());
        assertEquals(together, TextEffectFormat.readInline("shake=2:jump:sync"), "modes may come in any order");
        TextEffectStyle modeOnly = TextEffectFormat.readInline("shake=jump");
        assertEquals(
            1,
            modeOnly.shake()
                .pixels(),
            "a mode alone keeps the default amount");
        assertEquals(
            ShakeMotion.JUMP,
            modeOnly.shake()
                .motion());
        TextEffectStyle effect = TextEffectFormat.readInline("cr;shake=1");
        assertEquals("gtnrlib:calamity_red", effect.rendererId());
        assertEquals(
            ShakeMode.EACH,
            effect.shake()
                .mode());
        assertEquals(
            ShakeMotion.SMOOTH,
            effect.shake()
                .motion());
        assertNull(TextEffectFormat.readInline("shake=1:jump:jump"), "a repeated mode rejects the declaration");
        assertNull(TextEffectFormat.readInline("shake=4"), "shakes above three pixels are rejected");
        assertNull(TextEffectFormat.readInline("shake;2"), "the shake amount is always named");
    }

    @Test
    public void bothWrittenFormsKeepShakes() {
        TextEffectStyle style = TextEffects.CALAMITY_RED.withShake(2, ShakeMode.SYNC, ShakeMotion.JUMP);
        String inline = TextEffects.format(style);
        assertEquals(style, TextEffectFormat.readInline(inline.substring(2, inline.length() - 1)));
        assertEquals(
            style,
            new Cursor(TextEffects.apply("A", style)).next()
                .style());
    }

    @Test
    public void bothWrittenFormsKeepEntrancesAndRolls() {
        TextEffectStyle style = TextEffects.BURNISHED_AURIC.withTyping(12)
            .withEntrance(TextEntrance.DROP, 0.3f)
            .withRoll(2.5f, RollMode.FADE);
        String inline = TextEffects.format(style);
        assertEquals(style, TextEffectFormat.readInline(inline.substring(2, inline.length() - 1)));
        assertEquals(
            style,
            new Cursor(TextEffects.apply("12", style)).next()
                .style());
        TextEffectStyle rollOnly = TextEffects.NEBULA_RIFT.withRoll(1.234f);
        assertEquals(
            rollOnly,
            new Cursor(TextEffects.apply("9", rollOnly)).next()
                .style(),
            "a roll without typing still survives the scoped form");
        assertEquals(
            TextEffects.ROLL.withRoll(0.3f),
            TextEffects.ROLL.withRoll(0.1f + 0.2f),
            "times are rounded to the millisecond, so float noise does not split equal styles");
    }

    @Test
    public void slamsBlursAndNoWaitAreReadAndWrittenBack() {
        TextEffectStyle slam = TextEffectFormat.readInline("tw;t=20;r=slam:0.3:4");
        assertEquals(
            TextEntrance.SLAM,
            slam.typewriter()
                .entrance());
        assertEquals(
            4,
            slam.typewriter()
                .slamSize(),
            0.001f);
        assertEquals(
            TextEffectStyle.DEFAULT_SLAM_SIZE,
            TextEffectFormat.readInline("tw;t=20;r=slam")
                .typewriter()
                .slamSize(),
            0.001f,
            "a slam without a size starts at the default size");
        assertNull(TextEffectFormat.readInline("tw;t=20;r=slam:0.3:7"), "a slam starts at most six times its size");
        assertNull(TextEffectFormat.readInline("tw;t=20;r=drop:0.3:2"), "only a slam takes a size");
        TextEffectStyle roll = TextEffectFormat.readInline("ba;roll=1:slot:nowait;blur=40");
        assertEquals(
            40,
            roll.roll()
                .blur());
        assertFalse(
            roll.roll()
                .waits());
        assertEquals(roll, TextEffectFormat.readInline("ba;roll=1:nowait:blur=40:slot"), "blur is also a roll word");
        assertNull(TextEffectFormat.readInline("ba;blur=40"), "blur needs a roll");
        assertNull(TextEffectFormat.readInline("ba;roll=1:slot:blur=40;blur=50"), "blur is written at most once");
        assertNull(TextEffectFormat.readInline("ba;roll=1:slot;blur=201"), "blur is at most 200 percent");
        TextEffectStyle style = TextEffects.BURNISHED_AURIC.withTyping(12)
            .withEntrance(TextEntrance.SLAM, 0, 2.5f)
            .withRoll(1.5f, RollMode.AUTO, SlotOrder.LEFT_FIRST)
            .withRollBlur(0)
            .withRollWait(false);
        String inline = TextEffects.format(style);
        assertEquals(style, TextEffectFormat.readInline(inline.substring(2, inline.length() - 1)), "the inline form");
        assertEquals(
            style,
            new Cursor(TextEffects.apply("12", style)).next()
                .style(),
            "the scoped form");
    }

    @Test
    public void bothWrittenFormsKeepTheRate() {
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
                .typewriter()
                .rate(),
            "scoped text written before typing existed still parses");
    }
}
