package com.xyp.gtnrlib.gui.widget;

import java.util.function.Supplier;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.ae2.AE2ButtonPainter;
import com.xyp.gtnrlib.gui.theme.ae2.AE2SettingIcon;
import com.xyp.gtnrlib.gui.theme.ae2.IconSource;

/**
 * AE2's framed tab button ({@code TabButton}): {@link AE2ButtonPainter#tabButton}, {@link AE2Theme#TAB_BUTTON_SIZE}
 * pixels square, drawn by the theme of its panel. AE2 uses it for the button that returns from a sub-screen and places
 * it five pixels above the panel's top edge ({@link #back(Runnable)}). Other themes draw a plain button with the icon.
 * AE2 lights its tab button only while it has keyboard focus, which 1.7.10 buttons do not get, so this one lights up
 * under the mouse instead, in the same size. It reacts to the left mouse button; see {@link ThemedButton} for the
 * available state. Like AE2's, it has no selected look, so {@link #selected} changes nothing.
 */
public class TabButton extends ThemedButton<TabButton> {

    /** Offset of AE2's back button from its panel's top edge, and its distance from the panel's right edge. */
    public static final int BACK_TOP = -5, BACK_RIGHT = 4;

    /**
     * Creates a tab button.
     *
     * @param icon   supplier of the icon, read every frame
     * @param action run on a left click while the button is available
     */
    public TabButton(Supplier<? extends IconSource> icon, Runnable action) {
        IDrawable look = AE2ButtonPainter.tabButton(this, icon, this::isAvailable);
        background(look).hoverBackground(look)
            .size(AE2Theme.TAB_BUTTON_SIZE, AE2Theme.TAB_BUTTON_SIZE)
            .action(action);
    }

    /**
     * Creates AE2's back button, the way AE2 1.21's sub-screens and dialogs return to the screen they came from: a tab
     * button with AE2's back arrow. AE2 places it {@link #BACK_TOP} pixels above its panel's top edge and
     * {@link #BACK_RIGHT} pixels in from the right edge, so at {@code pos(panelWidth - TAB_BUTTON_SIZE - BACK_RIGHT,
     * BACK_TOP)}; add it after the panel's other children.
     *
     * @param action run on a left click, such as closing the dialog
     * @return the back button
     */
    public static TabButton back(Runnable action) {
        return new TabButton(() -> AE2SettingIcon.BACK, action);
    }
}
