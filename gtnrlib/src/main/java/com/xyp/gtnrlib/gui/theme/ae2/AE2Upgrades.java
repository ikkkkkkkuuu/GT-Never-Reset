package com.xyp.gtnrlib.gui.theme.ae2;

import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/** @deprecated Use {@link AE2UpgradePainter} and {@link AE2Look} for new screens. */
@Deprecated
public class AE2Upgrades {

    private AE2Upgrades() {}

    public static int upgradePanelWidth(int slots) {
        return AE2UpgradePainter.upgradePanelWidth(slots);
    }

    public static int upgradePanelWidth(int slots, int perColumn) {
        return AE2UpgradePainter.upgradePanelWidth(slots, perColumn);
    }

    public static int upgradeSlotX(int index) {
        return AE2UpgradePainter.upgradeSlotX(index);
    }

    public static int upgradeSlotX(int index, int perColumn) {
        return AE2UpgradePainter.upgradeSlotX(index, perColumn);
    }

    public static int upgradeSlotY(int index) {
        return AE2UpgradePainter.upgradeSlotY(index);
    }

    public static int upgradeSlotY(int index, int perColumn) {
        return AE2UpgradePainter.upgradeSlotY(index, perColumn);
    }

    public static int upgradePanelHeight(int slots) {
        return AE2UpgradePainter.upgradePanelHeight(slots);
    }

    public static int upgradePanelHeight(int slots, int perColumn) {
        return AE2UpgradePainter.upgradePanelHeight(slots, perColumn);
    }

    public static void drawUpgradePanel(Variant variant, int x, int y, int slots) {
        AE2UpgradePainter.drawUpgradePanel(variant, x, y, slots);
    }

    public static void drawUpgradePanel(Variant variant, int x, int y, int slots, int perColumn) {
        AE2UpgradePainter.drawUpgradePanel(variant, x, y, slots, perColumn);
    }
}
