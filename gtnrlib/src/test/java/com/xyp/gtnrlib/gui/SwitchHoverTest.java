package com.xyp.gtnrlib.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.xyp.gtnrlib.gui.theme.ae2.AE2Switches.SwitchHover;

public class SwitchHoverTest {

    @Test
    public void flippedSwitchStaysPlainUntilThePointerLeaves() {
        SwitchHover hover = new SwitchHover();
        assertTrue(hover.lit(true, false), "a switch under the pointer shows its hover look");
        assertFalse(hover.lit(true, true), "a switch flipped under the pointer drops its hover look");
        assertFalse(hover.lit(true, true), "the plain look stays while the pointer stays");
        assertFalse(hover.lit(false, true), "a switch away from the pointer is not hovered");
        assertTrue(hover.lit(true, true), "coming back under the pointer shows the hover look again");
    }
}
