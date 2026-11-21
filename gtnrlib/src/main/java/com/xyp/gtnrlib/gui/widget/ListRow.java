package com.xyp.gtnrlib.gui.widget;

import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.sizer.Unit.Measure;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Draw;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;

/**
 * One list row made of widgets and drawn like an {@link IconRowList} row: a one-line label on the left and, against
 * the right end, an optional control widget or on/off switch. Rows with a click action highlight under the mouse. A
 * label cut to fit shows in full as the tooltip unless a tooltip builder replaces it.
 * <p>
 * The row is meant to sit inside a list inset, like {@link IconRowList}'s rows: a row as wide as that list's inset
 * places its label {@link #TEXT_INSET} pixels and its control {@link #END_INSET} pixels from the inset's edges, the
 * same as the list's own rows. Use it where a list mixes MUI2 controls such as {@link NumberEntry} and
 * {@link CycleDropdown} with plain rows. The row is {@link IconRowList#ROW} pixels high unless its height is set; the
 * label, switch and control stay centred in any height. It can be built on either side, so it also works in GUIs the
 * server opens.
 */
public class ListRow extends ParentWidget<ListRow> implements Interactable {

    /** Pixels from the row's left edge to the label and from its right edge to the control or switch. */
    public static final int TEXT_INSET = 4, END_INSET = 3;
    private static final int EDGE = 1, GAP = 4;
    private static final float INACTIVE_ALPHA = 0.5F;
    private final Supplier<String> label;
    private final boolean header;
    private IntPredicate click;
    private BooleanSupplier toggle, available = () -> true;
    private Widget<?> control;
    private int controlWidth;
    private boolean cut;

    /**
     * Creates a row with a label.
     *
     * @param label supplier of the label, read every frame
     */
    public ListRow(Supplier<String> label) {
        this(label, false);
    }

    private ListRow(Supplier<String> label, boolean header) {
        this.label = label;
        this.header = header;
        height(IconRowList.ROW);
        tooltipAutoUpdate(true);
        tooltipBuilder(tooltip -> { if (cut) tooltip.addLine(label.get()); });
    }

    /**
     * Creates a section header row: muted text and, under AE2 themes, a divider line.
     *
     * @param label supplier of the header text, read every frame
     * @return the new row
     */
    public static ListRow header(Supplier<String> label) {
        return new ListRow(label, true);
    }

    /**
     * Places a control against the row's right end, centred vertically. While the control is disabled, the label is
     * drawn muted.
     *
     * @param control control widget; its size is set here
     * @param width   control width in pixels
     * @param height  control height in pixels, at most the row's height
     * @return this row
     */
    public ListRow control(Widget<?> control, int width, int height) {
        this.control = control;
        controlWidth = width;
        child(
            control.size(width, height)
                .right(END_INSET)
                .top(() -> (getArea().height - height) / 2, Measure.PIXEL));
        return this;
    }

    /**
     * Shows an on/off switch against the row's right end; a left click anywhere on the row flips it.
     *
     * @param on   supplier of the switch state, read every frame
     * @param flip run on a left click
     * @return this row
     */
    public ListRow toggle(BooleanSupplier on, Runnable flip) {
        toggle = on;
        return onClick(mouse -> {
            if (mouse != 0) return false;
            flip.run();
            return true;
        });
    }

    /**
     * Makes the row clickable; it then highlights under the mouse.
     *
     * @param click receives the pressed mouse button (0 left, 1 right) and returns whether it handled it
     * @return this row
     */
    public ListRow onClick(IntPredicate click) {
        this.click = click;
        return this;
    }

    /**
     * Sets when the row can be used. An unavailable row draws its label muted and its switch faded, does not
     * highlight, and swallows clicks without running its action.
     *
     * @param available supplier read every frame and on every click
     * @return this row
     */
    public ListRow available(BooleanSupplier available) {
        this.available = available;
        return this;
    }

    // MUI2 lets the mouse fall through a parent widget without a theme background; this one draws its own.
    @Override
    public boolean canHover() {
        return true;
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        if (click == null) return Result.IGNORE;
        if (!available.getAsBoolean()) return Result.STOP;
        if (!click.test(mouseButton)) return Result.IGNORE;
        Interactable.playButtonClickSound();
        return Result.SUCCESS;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        Variant variant = AE2Look.of(this)
            .variant();
        int width = getArea().width, height = getArea().height;
        boolean usable = available.getAsBoolean(), hovered = usable && click != null && isHovering();
        if (hovered) GuiDraw.drawRect(EDGE, 0, width - 2 * EDGE, height, IconRowList.hoverColor(variant));
        int right = width - END_INSET;
        if (toggle != null) {
            GlStateManager.color(1, 1, 1, usable ? 1 : INACTIVE_ALPHA);
            right -= IconRowList.drawSwitch(variant, right, 0, height, toggle.getAsBoolean(), hovered) + GAP;
            GlStateManager.color(1, 1, 1, 1);
        }
        if (control != null) right -= controlWidth + GAP;
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String full = label.get();
        String shown = font.trimStringToWidth(full, Math.max(0, right - TEXT_INSET));
        cut = shown.length() < full.length();
        if (header) IconRowList.drawHeader(variant, font, shown, TEXT_INSET, 0, height, width - EDGE);
        else {
            boolean muted = !usable || control != null && !control.isEnabled();
            int color = variant == null ? muted ? IconRowList.HEADER : IconRowList.TEXT
                : muted ? variant.colors.muted() : variant.colors.text();
            AE2Draw.text(shown, TEXT_INSET, IconRowList.textTop(height), color, false);
        }
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}
