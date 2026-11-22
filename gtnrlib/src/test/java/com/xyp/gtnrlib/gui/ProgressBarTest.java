package com.xyp.gtnrlib.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.cleanroommc.modularui.widgets.ProgressWidget.Direction;
import com.xyp.gtnrlib.gui.widget.ProgressBar;

public class ProgressBarTest {

    @Test
    public void straightOnly() {
        ProgressBar bar = new ProgressBar();
        assertEquals(Direction.UP, bar.getDirection(), "a new bar fills upwards like AE2's inscriber bar");
        bar.direction(Direction.RIGHT);
        assertEquals(Direction.RIGHT, bar.getDirection(), "straight directions are kept");
        assertThrows(
            IllegalArgumentException.class,
            () -> bar.direction(Direction.CIRCULAR_CW),
            "a circular bar needs a quartered texture the AE2 skins do not have");
    }
}
