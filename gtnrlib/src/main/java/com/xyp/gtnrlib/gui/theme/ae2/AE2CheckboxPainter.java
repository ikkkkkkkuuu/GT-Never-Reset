package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.CHECKBOX_LABEL_X;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.CHECKBOX_LABEL_Y;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.INACTIVE_ALPHA;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.RADIO_LABEL_X;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.RADIO_SIZE;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.widget.Widget;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/**
 * AE2 check boxes and radio buttons as drawables, the label layout of AE2's check boxes and AE2's hover rule after a
 * click.
 */
public class AE2CheckboxPainter {

    private static final int CHECKBOX_LINES_Y = 1;

    private AE2CheckboxPainter() {}

    /**
     * Creates a radio-indicator drawable with a label, without installing input handlers.
     * <p>
     * The AE2 indicator is 14 by 14 pixels and the label starts at {@link AE2Theme#RADIO_LABEL_X}. Other themes use a
     * MUI2 checkbox sprite. The label is trimmed to the supplied drawing width. The caller owns selection and input.
     *
     * @param widget    widget supplying hover state
     * @param selected  supplier of the selected state
     * @param label     supplier of non-null display text
     * @param available supplier of the visual enabled state
     * @return the indicator and label drawable
     */
    public static IDrawable radio(Widget<?> widget, BooleanSupplier selected, Supplier<String> label,
        BooleanSupplier available) {
        return (context, x, y, width, height, theme) -> {
            boolean usable = available.getAsBoolean(), lit = usable && widget.isHovering(),
                checked = selected.getAsBoolean();
            AE2Look look = AE2Look.of(widget);
            PlainLook plain = look.plainLook();
            if (plain != null) {
                plain.drawCheckbox(label.get(), x, y, width, RADIO_SIZE, RADIO_LABEL_X, checked, usable);
                return;
            }
            Variant variant = look.variant();
            GlStateManager.color(1, 1, 1, usable ? 1 : INACTIVE_ALPHA);
            variant.radioSprite(checked, lit)
                .draw(x, y, RADIO_SIZE, RADIO_SIZE);
            GlStateManager.color(1, 1, 1, 1);
            checkboxLabel(
                label.get(),
                x + RADIO_LABEL_X,
                y,
                width - RADIO_LABEL_X,
                usable ? variant.colors.text() : variant.colors.muted(),
                false);
        };
    }

    /**
     * Draws the label of a check box or radio button as AE2's check boxes do: wrapped at the given width, a single line
     * {@link AE2Theme#CHECKBOX_LABEL_Y} pixels below the top of the sprite and several lines from one pixel below it,
     * one font line apart. Client only.
     *
     * @param label  text to draw
     * @param x      left edge of the text
     * @param top    top edge of the check box or radio sprite
     * @param width  width the text may use, in pixels; the label wraps at word boundaries to fit it
     * @param color  ARGB text color
     * @param shadow whether to draw a text shadow
     */
    public static void checkboxLabel(String label, int x, int top, int width, int color, boolean shadow) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        List<?> lines = font.listFormattedStringToWidth(label, Math.max(1, width));
        int lineY = top + (lines.size() <= 1 ? CHECKBOX_LABEL_Y : CHECKBOX_LINES_Y);
        for (Object line : lines) {
            AE2Draw.text(String.valueOf(line), x, lineY, color, shadow);
            lineY += font.FONT_HEIGHT;
        }
    }

    /**
     * Measures an untrimmed radio indicator and label using the current font. Client only: it reads the client font, so
     * call it while drawing or laying out on the client, not while a GUI is built on the server.
     *
     * @param label display text
     * @return required width in pixels
     */
    public static int radioWidth(String label) {
        return RADIO_LABEL_X + Minecraft.getMinecraft().fontRenderer.getStringWidth(label);
    }

    /**
     * Creates a check box drawable with a label, without installing input handlers.
     * <p>
     * AE2's check box is a 22-by-12-pixel sprite with its label {@link AE2Theme#CHECKBOX_LABEL_X} pixels from its left
     * edge, wrapped at the drawing width. Other themes draw MUI2's check box with a one-line label cut to the width
     * ({@link PlainLook#drawCheckbox(String, int, int, int, int, int, boolean, boolean)}). The box itself is drawn by
     * {@link AE2Look#drawCheckbox}, as in list rows. The caller owns the value and input handling.
     *
     * @param widget    widget supplying hover state
     * @param on        supplier of the checked state
     * @param label     supplier of non-null display text
     * @param available supplier of the visual enabled state
     * @return combined check box and label drawable
     */
    public static IDrawable checkbox(Widget<?> widget, BooleanSupplier on, Supplier<String> label,
        BooleanSupplier available) {
        CheckboxHover hover = new CheckboxHover();
        return (context, x, y, width, height, theme) -> {
            boolean usable = available.getAsBoolean(), checked = on.getAsBoolean(),
                lit = hover.lit(widget.isHovering(), checked) && usable;
            AE2Look look = AE2Look.of(widget);
            PlainLook plain = look.plainLook();
            if (plain != null) {
                plain.drawCheckbox(
                    label.get(),
                    x,
                    y,
                    width,
                    PlainLook.CHECKBOX_SIZE,
                    PlainLook.CHECKBOX_LABEL_X,
                    checked,
                    usable);
                return;
            }
            Variant variant = look.variant();
            GlStateManager.color(1, 1, 1, usable ? 1 : INACTIVE_ALPHA);
            look.drawCheckbox(x, y, checked, lit);
            GlStateManager.color(1, 1, 1, 1);
            checkboxLabel(
                label.get(),
                x + CHECKBOX_LABEL_X,
                y,
                width - CHECKBOX_LABEL_X,
                usable ? variant.colors.text() : variant.colors.muted(),
                false);
        };
    }

    /**
     * AE2's hover rule for check boxes: once a check box flips under the pointer it shows its plain sprite until the
     * pointer leaves it, as AE2's check box does while it keeps the focus of the click. Radio buttons always show hover
     * in AE2. Keep one instance per check box and ask it every frame.
     */
    public static class CheckboxHover {

        private @Nullable Boolean shown;
        private boolean quiet;

        /**
         * Decides whether the check box is drawn hovered this frame.
         *
         * @param hovering whether the pointer is over the check box
         * @param on       whether the box is checked
         * @return whether to draw the hovered sprite
         */
        public boolean lit(boolean hovering, boolean on) {
            if (!hovering) quiet = false;
            else if (shown != null && shown != on) quiet = true;
            shown = on;
            return hovering && !quiet;
        }
    }

    /**
     * Measures an untrimmed AE2 check box and label using the current font. Client only: it reads the client font, so
     * call it while drawing or laying out on the client, not while a GUI is built on the server.
     *
     * @param label display text
     * @return required width in pixels
     */
    public static int checkboxWidth(String label) {
        return CHECKBOX_LABEL_X + Minecraft.getMinecraft().fontRenderer.getStringWidth(label);
    }

    /**
     * Measures a check box and label as {@link #checkbox} draws them in a look: AE2's 22-pixel sprite under an AE2
     * skin, MUI2's smaller box in the plain look. Client only, like {@link #checkboxWidth(String)}.
     *
     * @param look  look the check box is drawn in
     * @param label display text
     * @return required width in pixels
     */
    public static int checkboxWidth(AE2Look look, String label) {
        return (look.isAE2() ? CHECKBOX_LABEL_X : PlainLook.CHECKBOX_LABEL_X)
            + Minecraft.getMinecraft().fontRenderer.getStringWidth(label);
    }
}
