package com.xyp.gtnrlib.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Upgrades;

public class UpgradePanelLayoutTest {

    @Test
    public void defaultColumnsMatchAe2() {
        assertEquals(23, AE2Upgrades.upgradePanelWidth(3), "one column is 23 pixels wide, as in AE2");
        assertEquals(66, AE2Upgrades.upgradePanelHeight(3), "three slots make an 18n + 12 high panel");
        assertEquals(41, AE2Upgrades.upgradePanelWidth(9), "a ninth slot opens a second column");
        assertEquals(18, AE2Upgrades.upgradeSlotX(8), "the ninth slot starts the second column");
        assertEquals(5, AE2Upgrades.upgradeSlotY(8), "the ninth slot sits at the top of its column");
    }

    @Test
    public void chosenColumnLengthLaysSlotsOut() {
        assertEquals(59, AE2Upgrades.upgradePanelWidth(3, 1), "one slot per column puts three slots side by side");
        assertEquals(30, AE2Upgrades.upgradePanelHeight(3, 1), "one slot per column makes a one-row panel");
        assertEquals(36, AE2Upgrades.upgradeSlotX(2, 1), "the third slot is two columns to the right");
        assertEquals(5, AE2Upgrades.upgradeSlotY(2, 1), "every slot of a one-row panel sits on the top row");
        assertEquals(23, AE2Upgrades.upgradeSlotY(3, 2), "the fourth slot of two-slot columns is on the second row");
        assertEquals(
            AE2Upgrades.upgradePanelWidth(5),
            AE2Upgrades.upgradePanelWidth(5, AE2Theme.UPGRADE_ROWS),
            "the default methods use AE2's column length");
    }
}
