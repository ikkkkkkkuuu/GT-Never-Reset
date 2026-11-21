package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.DISABLED_TEXT;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.ICON_SIZE;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.LABEL_HEIGHT;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.LABEL_PADDING;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Stencil;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.widget.Widget;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.ButtonText;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable.State;

/**
 * AE2 text buttons, toolbar icon buttons, tabs and page tabs as drawables, resolved against the theme of the widget's
 * panel when drawn, with MUI2's button look outside AE2 themes.
 */
public class AE2Buttons {

    private static final double MARQUEE_SECONDS_PER_PIXEL = 0.5, MARQUEE_MIN_PERIOD = 3;

    private AE2Buttons() {}

    /**
     * Creates a button background resolved against the theme and supplied state at draw time.
     *
     * @param widget    widget supplying hover state
     * @param available supplier of the visual enabled state; input handling remains the caller's responsibility
     * @param selected  supplier of the selected state, which uses the highlighted background
     * @return background drawable suitable for both normal and hover backgrounds
     * @see #label(Widget, Supplier, BooleanSupplier, BooleanSupplier)
     */
    public static IDrawable button(Widget<?> widget, BooleanSupplier available, BooleanSupplier selected) {
        return (context, x, y, width, height, theme) -> {
            State state = State.of(widget, available.getAsBoolean(), selected.getAsBoolean());
            ITheme panel = AE2Draw.themeOf(widget, context);
            Variant variant = AE2Look.of(panel)
                .variant();
            if (variant != null) AE2ButtonDrawable.draw(variant.folder, state, x, y, width, height);
            else AE2Draw.themeButton(context, panel, state, x, y, width, height);
        };
    }

    /**
     * Resolves the text style for a button in the screen's theme.
     *
     * @param context current drawing context
     * @param state   visual button state
     * @param height  button height in pixels
     * @return text color, vertical offset and shadow setting
     * @see #buttonText(IWidget, State, int)
     */
    public static ButtonText buttonText(GuiContext context, State state, int height) {
        return buttonText(AE2Draw.screenTheme(context), state, height);
    }

    /**
     * Resolves the text style for a button in the theme of the panel a widget belongs to.
     *
     * @param widget widget in an open panel
     * @param state  visual button state
     * @param height button height in pixels
     * @return text color, vertical offset and shadow setting
     */
    public static ButtonText buttonText(IWidget widget, State state, int height) {
        return buttonText(AE2Draw.themeOf(widget), state, height);
    }

    private static ButtonText buttonText(@Nullable ITheme theme, State state, int height) {
        Variant variant = AE2Look.of(theme)
            .variant();
        if (variant != null)
            return new ButtonText(variant.colors.button(state), AE2ButtonDrawable.textY(state, height), false);
        WidgetTheme look = AE2Draw.buttonLook(theme, state);
        int color = state == State.DISABLED ? DISABLED_TEXT : look == null ? Color.WHITE.main : look.getTextColor();
        return new ButtonText(color, (height - LABEL_HEIGHT) / 2, look == null || look.getTextShadow());
    }

    /**
     * Creates a centered button label matching {@link #button}. Under AE2 themes a label that fits starts half its
     * width left of the button's middle, both halves rounded down as AE2 rounds them, and a label wider than the
     * button scrolls back and forth inside it, as AE2 buttons do, and stays at the height of a hovered button's label
     * in every state; other themes trim it to the width.
     *
     * @param widget    widget supplying hover state
     * @param text      supplier of non-null display text
     * @param available supplier of the visual enabled state
     * @param selected  supplier of the selected state
     * @return label drawable suitable for a button overlay
     */
    public static IDrawable label(Widget<?> widget, Supplier<String> text, BooleanSupplier available,
        BooleanSupplier selected) {
        return (context, x, y, width, height, theme) -> {
            State state = State.of(widget, available.getAsBoolean(), selected.getAsBoolean());
            ITheme panel = AE2Draw.themeOf(widget, context);
            ButtonText style = buttonText(panel, state, height);
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            String full = text.get();
            int room = Math.max(0, width - LABEL_PADDING * 2), overflow = font.getStringWidth(full) - room;
            boolean ae2 = AE2Look.of(panel)
                .isAE2();
            if (overflow <= 0 || !ae2) {
                String fitted = font.trimStringToWidth(full, room);
                int fittedWidth = font.getStringWidth(fitted);
                int left = ae2 ? x + width / 2 - fittedWidth / 2 : x + (width - fittedWidth) / 2;
                AE2Draw.text(fitted, left, y + style.y(), style.color(), style.shadow());
                return;
            }
            // Why: AE2Button draws a scrolling label at the hovered label's height whatever the state; only a
            // centred label sinks and rises with the face.
            int scrollY = AE2ButtonDrawable.textY(State.HOVER, height);
            Stencil.apply(x + LABEL_PADDING, y, room, height, context);
            AE2Draw.text(full, x + LABEL_PADDING - marquee(overflow), y + scrollY, style.color(), style.shadow());
            Stencil.remove();
        };
    }

    private static int marquee(int overflow) {
        double seconds = Minecraft.getSystemTime() / 1000D,
            period = Math.max(overflow * MARQUEE_SECONDS_PER_PIXEL, MARQUEE_MIN_PERIOD);
        double phase = Math.sin(Math.PI / 2 * Math.cos(Math.PI * 2 * seconds / period)) / 2 + 0.5;
        return (int) (phase * overflow);
    }

    /**
     * Creates a toolbar-button background and icon, sized by the drawing area.
     *
     * @param widget    widget supplying hover state
     * @param icon      supplier of the displayed icon
     * @param available supplier of the visual enabled state
     * @return combined background and icon drawable; the standard size is 18 by 20 pixels
     */
    public static IDrawable iconButton(Widget<?> widget, Supplier<? extends IconSource> icon,
        BooleanSupplier available) {
        return iconButton(widget, icon, available, () -> false);
    }

    /**
     * Creates a toolbar-button background and icon that shows AE2's selected frame while it is selected, such as the
     * button of the open page. Hovering still sinks it like any toolbar button.
     *
     * @param widget    widget supplying hover state
     * @param icon      supplier of the displayed icon
     * @param available supplier of the visual enabled state
     * @param selected  supplier of the selected state
     * @return combined background and icon drawable; the standard size is 18 by 20 pixels
     * @see #iconButton(Widget, Supplier, BooleanSupplier)
     */
    public static IDrawable iconButton(Widget<?> widget, Supplier<? extends IconSource> icon, BooleanSupplier available,
        BooleanSupplier selected) {
        return (context, x, y, width, height, theme) -> {
            boolean on = available.getAsBoolean(), lit = on && widget.isHovering(), chosen = selected.getAsBoolean();
            ITheme panel = AE2Draw.themeOf(widget, context);
            Variant variant = AE2Look.of(panel)
                .variant();
            IconSource shown = icon.get();
            if (variant == null) {
                drawPlainIconButton(context, panel, theme, shown, lit || chosen, x, y, width, height, on);
                return;
            }
            int sink = lit ? 1 : 0;
            GlStateManager.color(1, 1, 1, 1);
            (lit ? variant.iconButtonHover : chosen ? variant.iconButtonSelected : variant.iconButton)
                .draw(x, y + sink, width, height);
            shown.drawIcon(
                context,
                theme,
                variant,
                x + (width - ICON_SIZE) / 2,
                y + Math.max(0, (height - ICON_SIZE) / 2 - 1) + sink,
                on);
        };
    }

    /**
     * Creates a tab-button background and icon, drawn at the widget's size; AE2's tabs are
     * {@link AE2Theme#TAB_BUTTON_SIZE}
     * pixels square, and other sizes stretch the tab's fill while keeping its frame. The icon stays 16 by 16, centred.
     * This drawable does not install input handlers.
     *
     * @param widget    widget supplying hover state
     * @param icon      supplier of the displayed icon
     * @param available supplier of the visual enabled state
     * @return combined tab background and icon drawable
     */
    public static IDrawable tabButton(Widget<?> widget, Supplier<? extends IconSource> icon,
        BooleanSupplier available) {
        return (context, x, y, width, height, theme) -> {
            boolean on = available.getAsBoolean(), lit = on && widget.isHovering();
            ITheme panel = AE2Draw.themeOf(widget, context);
            Variant variant = AE2Look.of(panel)
                .variant();
            IconSource shown = icon.get();
            if (variant == null) {
                drawPlainIconButton(context, panel, theme, shown, lit, x, y, width, height, on);
                return;
            }
            GlStateManager.color(1, 1, 1, 1);
            (lit ? variant.tabButtonHover : variant.tabButton).draw(x, y, width, height);
            shown.drawIcon(
                context,
                theme,
                variant,
                x + (width - ICON_SIZE) / 2,
                y + Math.max(0, (height - ICON_SIZE) / 2 - 1),
                on);
        };
    }

    /**
     * Creates a page-tab background and icon, as AE2 draws the mode tabs of its pattern encoding terminal.
     * <p>
     * AE2 tabs are 22 by 22 pixels, stacked {@link AE2Theme#PAGE_TAB_PITCH} apart and placed
     * {@link AE2Theme#PAGE_TAB_INSET} pixels
     * inside the panel's right edge, after the panel: the selected tab then covers the panel border and joins the
     * panel, the others stand out as separate tabs. The tab is drawn at the widget's size: other sizes stretch its fill
     * and keep its frame and the three columns that cover the panel border; the icon stays 16 by 16, centred on the
     * part outside the panel. Other themes use the button look, hovered while selected.
     *
     * @param widget   widget supplying hover state
     * @param icon     supplier of the displayed icon
     * @param selected supplier of the selected state
     * @return combined tab background and icon drawable
     */
    public static IDrawable pageTab(Widget<?> widget, Supplier<? extends IconSource> icon, BooleanSupplier selected) {
        return pageTab(widget, icon, selected, () -> true);
    }

    /**
     * Creates a page-tab background and icon like {@link #pageTab(Widget, Supplier, BooleanSupplier)} that can also be
     * unavailable: an unavailable tab shows no hover and draws its icon faded.
     *
     * @param widget    widget supplying hover state
     * @param icon      supplier of the displayed icon
     * @param selected  supplier of the selected state
     * @param available supplier of the visual enabled state
     * @return combined tab background and icon drawable
     */
    public static IDrawable pageTab(Widget<?> widget, Supplier<? extends IconSource> icon, BooleanSupplier selected,
        BooleanSupplier available) {
        return (context, x, y, width, height, theme) -> {
            boolean chosen = selected.getAsBoolean(), on = available.getAsBoolean(), lit = on && widget.isHovering();
            ITheme panel = AE2Draw.themeOf(widget, context);
            Variant variant = AE2Look.of(panel)
                .variant();
            IconSource shown = icon.get();
            if (variant == null) {
                drawPlainIconButton(context, panel, theme, shown, lit || chosen, x, y, width, height, on);
                return;
            }
            GlStateManager.color(1, 1, 1, 1);
            (chosen ? variant.pageTabSelected : lit ? variant.pageTabHover : variant.pageTab).draw(x, y, width, height);
            shown.drawIcon(
                context,
                theme,
                variant,
                x + (width + 1 - ICON_SIZE) / 2,
                y + Math.max(0, (height - ICON_SIZE) / 2 - 1),
                on);
        };
    }

    private static void drawPlainIconButton(GuiContext context, @Nullable ITheme panel, WidgetTheme theme,
        IconSource icon, boolean lit, int x, int y, int width, int height, boolean enabled) {
        AE2Draw.themeButton(context, panel, lit ? State.HOVER : State.NORMAL, x, y, width, height);
        icon.drawIcon(context, theme, null, x + (width - ICON_SIZE) / 2, y + (height - ICON_SIZE) / 2, enabled);
    }
}
