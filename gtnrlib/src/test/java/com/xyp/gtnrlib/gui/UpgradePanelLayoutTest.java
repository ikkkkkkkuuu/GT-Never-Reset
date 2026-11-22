package com.xyp.gtnrlib.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.ae2.AE2UpgradePainter;
import com.xyp.gtnrlib.gui.widget.UpgradesPanel;

public class UpgradePanelLayoutTest {

    @Test
    public void defaultColumnsMatchAe2() {
        assertEquals(23, AE2UpgradePainter.upgradePanelWidth(3), "one column is 23 pixels wide, as in AE2");
        assertEquals(66, AE2UpgradePainter.upgradePanelHeight(3), "three slots make an 18n + 12 high panel");
        assertEquals(41, AE2UpgradePainter.upgradePanelWidth(9), "a ninth slot opens a second column");
        assertEquals(18, AE2UpgradePainter.upgradeSlotX(8), "the ninth slot starts the second column");
        assertEquals(5, AE2UpgradePainter.upgradeSlotY(8), "the ninth slot sits at the top of its column");
    }

    @Test
    public void columnLength() {
        assertEquals(
            59,
            AE2UpgradePainter.upgradePanelWidth(3, 1),
            "one slot per column puts three slots side by side");
        assertEquals(30, AE2UpgradePainter.upgradePanelHeight(3, 1), "one slot per column makes a one-row panel");
        assertEquals(36, AE2UpgradePainter.upgradeSlotX(2, 1), "the third slot is two columns to the right");
        assertEquals(5, AE2UpgradePainter.upgradeSlotY(2, 1), "every slot of a one-row panel sits on the top row");
        assertEquals(
            23,
            AE2UpgradePainter.upgradeSlotY(3, 2),
            "the fourth slot of two-slot columns is on the second row");
        assertEquals(
            AE2UpgradePainter.upgradePanelWidth(5),
            AE2UpgradePainter.upgradePanelWidth(5, AE2Theme.UPGRADE_SLOTS_PER_COLUMN),
            "the default methods use AE2's column length");
    }

    @Test
    public void neiExclusion() {
        UpgradesPanel panel = new UpgradesPanel();
        assertFalse(panel.isExcludeAreaInRecipeViewer(), "an empty panel asks NEI for no space, as AE2's");
        ItemSlot hidden = new ItemSlot();
        hidden.setEnabled(false);
        panel.slot(hidden);
        assertFalse(panel.isExcludeAreaInRecipeViewer(), "a slot that is turned off takes no space either");
        panel.slot(new ItemSlot());
        assertTrue(panel.isExcludeAreaInRecipeViewer(), "a shown slot makes the panel ask NEI to keep clear");
    }
}
