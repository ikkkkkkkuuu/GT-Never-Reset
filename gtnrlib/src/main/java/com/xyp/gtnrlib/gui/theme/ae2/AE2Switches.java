package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.INACTIVE_ALPHA;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.LABEL_HEIGHT;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.LABEL_PADDING;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.RADIO_LABEL_X;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.RADIO_SIZE;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.SWITCH_HEIGHT;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.SWITCH_LABEL_X;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.SWITCH_LABEL_Y;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.SWITCH_WIDTH;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.widget.Widget;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/**
 * AE2 switches and radio buttons: drawables, the sprite of a list-row switch, the label layout of AE2's check boxes and
 * AE2's hover rule after a click.
 */
public class AE2Switches {

    private static final int CHECKBOX_LINES_Y = 1;

    private AE2Switches() {}

    /**
     * Creates a radio-indicator drawable with a label, without installing input handlers.
     * <p>
     * The AE2 indicator is 14 by 14 pixels and the label starts at {@link AE2Theme#RADIO_LABEL_X}. Other themes use a
     * MUI2
     * checkbox sprite. The label is trimmed to the supplied drawing width. The caller owns selection and input.
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
            ITheme panel = AE2Draw.themeOf(widget, context);
            Variant variant = AE2Look.of(panel)
                .variant();
            GlStateManager.color(1, 1, 1, usable ? 1 : INACTIVE_ALPHA);
            if (variant == null) {
                (checked ? GuiTextures.CHECK_BOX_FULL : GuiTextures.CHECK_BOX_EMPTY).draw(x, y, RADIO_SIZE, RADIO_SIZE);
                GlStateManager.color(1, 1, 1, 1);
                String fitted = Minecraft.getMinecraft().fontRenderer
                    .trimStringToWidth(label.get(), Math.max(0, width - RADIO_LABEL_X));
                AE2Draw.plainText(panel, fitted, x + RADIO_LABEL_X, y + (RADIO_SIZE - LABEL_HEIGHT) / 2);
                return;
            }
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
     * Draws the label of a switch or radio button as AE2's check boxes do: wrapped at the given width, a single line
     * {@link AE2Theme#SWITCH_LABEL_Y} pixels below the top of the sprite and several lines from one pixel below it, one
     * font
     * line apart. Client only.
     *
     * @param label  text to draw
     * @param x      left edge of the text
     * @param top    top edge of the switch or radio sprite
     * @param width  width the text may use, in pixels; the label wraps at word boundaries to fit it
     * @param color  ARGB text color
     * @param shadow whether to draw a text shadow
     */
    public static void checkboxLabel(String label, int x, int top, int width, int color, boolean shadow) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        List<?> lines = font.listFormattedStringToWidth(label, Math.max(1, width));
        int lineY = top + (lines.size() <= 1 ? SWITCH_LABEL_Y : CHECKBOX_LINES_Y);
        for (Object line : lines) {
            AE2Draw.text(String.valueOf(line), x, lineY, color, shadow);
            lineY += font.FONT_HEIGHT;
        }
    }

    /**
     * Measures an untrimmed radio indicator and label using the current font. Client only: it reads the client font,
     * so call it while drawing or laying out on the client, not while a GUI is built on the server.
     *
     * @param label display text
     * @return required width in pixels
     */
    public static int radioWidth(String label) {
        return RADIO_LABEL_X + Minecraft.getMinecraft().fontRenderer.getStringWidth(label);
    }

    /**
     * Draws an on/off switch: AE2's switch sprite under AE2 themes, MUI2's check box otherwise. List rows and
     * {@link #toggleSwitch} both use it, so a switch looks the same everywhere in a theme.
     *
     * @param variant active AE2 variant, or {@code null} outside the AE2 themes
     * @param x       left edge
     * @param y       top edge
     * @param on      switch value
     * @param hovered whether the mouse is over the switch; only AE2 sprites show it
     * @return width of the drawn switch: {@link AE2Theme#SWITCH_WIDTH} under AE2 themes, {@link AE2Theme#SWITCH_HEIGHT}
     *         otherwise
     */
    public static int drawSwitch(@Nullable Variant variant, int x, int y, boolean on, boolean hovered) {
        if (variant == null) {
            (on ? GuiTextures.CHECK_BOX_FULL : GuiTextures.CHECK_BOX_EMPTY).draw(x, y, SWITCH_HEIGHT, SWITCH_HEIGHT);
            return SWITCH_HEIGHT;
        }
        variant.switchSprite(on, hovered)
            .draw(x, y, SWITCH_WIDTH, SWITCH_HEIGHT);
        return SWITCH_WIDTH;
    }

    /**
     * Creates a switch drawable with a width-trimmed label, without installing input handlers.
     * <p>
     * AE2 switches use a 22-by-12-pixel sprite and label offset {@link AE2Theme#SWITCH_LABEL_X}. Other themes use a
     * MUI2
     * checkbox sprite. The caller owns the switch value and input handling.
     *
     * @param widget    widget supplying hover state
     * @param on        supplier of the switch value
     * @param label     supplier of non-null display text
     * @param available supplier of the visual enabled state
     * @return combined switch and label drawable
     */
    public static IDrawable toggleSwitch(Widget<?> widget, BooleanSupplier on, Supplier<String> label,
        BooleanSupplier available) {
        SwitchHover hover = new SwitchHover();
        return (context, x, y, width, height, theme) -> {
            boolean usable = available.getAsBoolean(), checked = on.getAsBoolean(),
                lit = hover.lit(widget.isHovering(), checked) && usable;
            ITheme panel = AE2Draw.themeOf(widget, context);
            Variant variant = AE2Look.of(panel)
                .variant();
            GlStateManager.color(1, 1, 1, usable ? 1 : INACTIVE_ALPHA);
            if (variant == null) {
                drawSwitch(null, x, y, checked, lit);
                GlStateManager.color(1, 1, 1, 1);
                String fitted = Minecraft.getMinecraft().fontRenderer
                    .trimStringToWidth(label.get(), Math.max(0, width - SWITCH_LABEL_X));
                AE2Draw.plainText(
                    panel,
                    fitted,
                    x + SWITCH_HEIGHT + LABEL_PADDING * 2,
                    y + (SWITCH_HEIGHT - LABEL_HEIGHT) / 2);
                return;
            }
            drawSwitch(variant, x, y, checked, lit);
            GlStateManager.color(1, 1, 1, 1);
            checkboxLabel(
                label.get(),
                x + SWITCH_LABEL_X,
                y,
                width - SWITCH_LABEL_X,
                usable ? variant.colors.text() : variant.colors.muted(),
                false);
        };
    }

    /**
     * AE2's hover rule for switches: once a switch flips under the pointer it shows its plain sprite until the pointer
     * leaves it, as AE2's check box does while it keeps the focus of the click. Radio buttons always show hover in AE2.
     * Keep one instance per switch and ask it every frame.
     */
    public static class SwitchHover {

        private @Nullable Boolean shown;
        private boolean quiet;

        /**
         * Decides whether the switch is drawn hovered this frame.
         *
         * @param hovering whether the pointer is over the switch
         * @param on       the switch's current value
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
     * Measures an untrimmed AE2 switch and label using the current font. Client only: it reads the client font, so
     * call it while drawing or laying out on the client, not while a GUI is built on the server.
     *
     * @param label display text
     * @return required width in pixels
     */
    public static int switchWidth(String label) {
        return SWITCH_LABEL_X + Minecraft.getMinecraft().fontRenderer.getStringWidth(label);
    }
}
