package com.xyp.gtnrlib.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cleanroommc.modularui.api.UpOrDown;
import com.xyp.gtnrlib.gui.widget.RowScrollbar;

public class RowScrollbarTest {

    private int firstRow, lastRow;
    private final List<Integer> changes = new ArrayList<>();

    private RowScrollbar bar() {
        return new RowScrollbar(() -> firstRow, () -> lastRow, row -> {
            changes.add(row);
            firstRow = row;
        });
    }

    @Test
    public void wheelMovesOneStepAndStopsAtTheEnds() {
        lastRow = 5;
        RowScrollbar bar = bar().step(2);
        assertTrue(bar.scroll(UpOrDown.DOWN), "a notch down must scroll a bar with a range");
        assertEquals(2, firstRow, "a notch moves one step");
        bar.scroll(UpOrDown.DOWN);
        bar.scroll(UpOrDown.DOWN);
        assertEquals(5, firstRow, "the last position stops the bar");
        bar.scroll(UpOrDown.DOWN);
        assertEquals(Arrays.asList(2, 4, 5), changes, "a notch at the end must not report the same position again");
        bar.scroll(UpOrDown.UP);
        assertEquals(3, firstRow, "a notch up moves towards position zero");
    }

    @Test
    public void barWithoutRangeLeavesTheWheel() {
        lastRow = 0;
        RowScrollbar bar = bar();
        assertFalse(bar.scroll(UpOrDown.DOWN), "a bar whose content fits must leave the wheel to others");
        assertTrue(changes.isEmpty(), "a bar whose content fits must not move");
    }

    @Test
    public void positionIsClampedToTheRange() {
        lastRow = 4;
        firstRow = 9;
        assertEquals(4, bar().position(), "an owner position past the end reads as the last position");
        firstRow = -3;
        assertEquals(0, bar().position(), "a negative owner position reads as position zero");
    }
}
