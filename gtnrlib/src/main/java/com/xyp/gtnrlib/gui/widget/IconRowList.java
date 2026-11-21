package com.xyp.gtnrlib.gui.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerIngredientProvider;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * A scrolling list whose rows are drawn directly instead of being widgets, so it stays fast with hundreds of rows.
 * The rows are read from a supplier every tick. A row can carry an item icon, a right-aligned detail, an on/off
 * switch, a dropdown whose options open as indented rows under it, or a number field. Number fields are
 * {@link NumberEntry} children placed over the visible number rows, so only those rows cost a widget.
 * <p>
 * Under an AE2 theme the list draws as an AE2 inset with AE2 list-row highlights, switches and a small scroll bar;
 * otherwise it keeps its own dark look with MUI2's check boxes.
 */
public class IconRowList extends ParentWidget<IconRowList> implements Interactable, RecipeViewerIngredientProvider {

    public static final int ROW = 18, SCROLLBAR = 3, INDENT = 10, CARET = 9, NUMBER_WIDTH = 64;
    public static final int BACKGROUND = 0xFF1C1C22, NESTED = 0xFF26262E, HOVER = 0x30FFFFFF, SELECTED = 0x5040A0FF,
        BAR = 0xFF808090, HEADER = 0xFFA0A0A8, TEXT = 0xFFE8E8E8, ACCENT = 0xFFFFD040;
    /** Row colors that AE2 themes replace with their own good and bad colors. */
    public static final int GOOD = 0xFF60E060, BAD = 0xFFFF6060;
    /**
     * Width taken by the scroll bar right of the inset under AE2 themes, like AE2's lists: the groove sits on the panel
     * background, seven pixels from the inset's edge.
     */
    public static final int AE2_SCROLL_COLUMN = AE2Theme.BIG_SCROLLER_WIDTH + 4;
    private static final int AE2_GUTTER = AE2_SCROLL_COLUMN + 1, LIGHT_SHADE = 0x18000000, DARK_SHADE = 0x18FFFFFF,
        HEADER_LINE_ALPHA = 0x40000000;

    /**
     * The number field of a row.
     *
     * @param value shown value; it replaces the text whenever the player is not typing
     * @param min   inclusive minimum
     * @param max   inclusive maximum
     * @param reset value set by right-clicking the field, clamped to the bounds
     * @param set   receives every valid value the player enters
     */
    public record Numeric(long value, long min, long max, long reset, LongConsumer set) {}

    /**
     * One row. Only one of click, options, toggle and number is normally set; options turn the row into a
     * dropdown, a non-null toggle draws a switch whose state is that value, and a number puts a {@link NumberEntry}
     * on the right of the row. A right click runs {@code secondary} when it is set. A nonzero {@code detailColor}
     * colors only the detail; zero uses {@code color} for both.
     */
    public record Row(ItemStack icon, String text, String detail, int color, boolean selected, boolean header,
        Runnable click, List<Row> options, Boolean toggle, Numeric number, Runnable secondary, int detailColor) {

        public static Row header(String text) {
            return new Row(null, text, "", HEADER, false, true, null, null, null, null, null, 0);
        }

        public static Row item(ItemStack icon, String text, String detail, boolean selected, Runnable click) {
            return new Row(icon, text, detail, TEXT, selected, false, click, null, null, null, null, 0);
        }

        public static Row colored(ItemStack icon, String text, String detail, int color) {
            return new Row(icon, text, detail, color, false, false, null, null, null, null, null, 0);
        }

        public static Row toggle(String text, boolean on, Runnable click) {
            return new Row(null, text, "", TEXT, false, false, click, null, on, null, null, 0);
        }

        public static Row dropdown(ItemStack icon, String text, String detail, boolean selected, List<Row> options) {
            return new Row(icon, text, detail, TEXT, selected, false, null, options, null, null, null, 0);
        }

        public static Row number(String text, long value, long min, long max, long reset, LongConsumer set) {
            return new Row(
                null,
                text,
                "",
                TEXT,
                false,
                false,
                null,
                null,
                null,
                new Numeric(value, min, max, reset, set),
                null,
                0);
        }

        /**
         * Returns a copy of this row that runs the action on a right click.
         *
         * @param action action for the right mouse button
         * @return the new row
         */
        public Row onRightClick(Runnable action) {
            return new Row(
                icon,
                text,
                detail,
                color,
                selected,
                header,
                click,
                options,
                toggle,
                number,
                action,
                detailColor);
        }

        /**
         * Returns a copy of this row whose detail alone is drawn in another color; {@link #GOOD} and {@link #BAD}
         * follow the AE2 theme like row colors do.
         *
         * @param color ARGB color of the detail
         * @return the new row
         */
        public Row withDetailColor(int color) {
            return new Row(
                icon,
                text,
                detail,
                this.color,
                selected,
                header,
                click,
                options,
                toggle,
                number,
                secondary,
                color);
        }
    }

    private final Supplier<List<Row>> source;
    private final List<NumberEntry> entries = new ArrayList<>();
    private final List<Row> entryRows = new ArrayList<>();
    private final IntList entryTops = new IntArrayList();
    private List<Row> rows = Collections.emptyList();
    private boolean[] nested = new boolean[0];
    private String expanded;
    private int scroll, edge, gutter = SCROLLBAR, grab = -1;
    private boolean ae2, revealSelected;

    public IconRowList(Supplier<List<Row>> source) {
        this.source = source;
    }

    public void scrollToTop() {
        scroll = 0;
    }

    /**
     * Scrolls, once the list has been laid out, so that the first selected row sits in the middle of the view.
     *
     * @return this list
     */
    public IconRowList revealSelected() {
        revealSelected = true;
        return this;
    }

    // MUI2 lets the mouse fall through a parent widget without a theme background; this one draws its own.
    @Override
    public boolean canHover() {
        return true;
    }

    @Override
    public boolean canClickThrough() {
        return false;
    }

    @Override
    public boolean canHoverThrough() {
        return false;
    }

    /**
     * Collapses an open dropdown.
     */
    public void collapse() {
        expanded = null;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        // MUI2 updates hidden widgets too; a hidden list has nothing to rebuild.
        if (!isEnabled()) return;
        List<Row> flat = new ArrayList<>();
        List<Boolean> inside = new ArrayList<>();
        for (Row row : source.get()) {
            flat.add(row);
            inside.add(false);
            if (row.options() == null || !row.text()
                .equals(expanded)) continue;
            for (Row option : row.options()) {
                flat.add(option);
                inside.add(true);
            }
        }
        rows = flat;
        nested = new boolean[flat.size()];
        for (int i = 0; i < nested.length; i++) nested[i] = inside.get(i);
        if (revealSelected && getArea().height >= ROW) {
            revealSelected = false;
            for (int i = 0; i < flat.size(); i++) {
                if (!flat.get(i)
                    .selected()) continue;
                scroll = i - visibleRows() / 2;
                break;
            }
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        placeEntries();
    }

    private void placeEntries() {
        int used = 0;
        boolean moved = false;
        for (int i = 0; i < visibleRows() && scroll + i < rows.size(); i++) {
            Row row = rows.get(scroll + i);
            if (row.number() == null) continue;
            if (used == entries.size()) {
                addEntry();
                moved = true;
            }
            NumberEntry entry = entries.get(used);
            int top = edge + i * ROW + 1;
            if (entryTops.getInt(used) != top) {
                entry.top(top);
                entryTops.set(used, top);
                moved = true;
            }
            entryRows.set(used, row);
            entry.setEnabled(true);
            Numeric number = row.number();
            entry.bounds(number.min(), number.max())
                .resetValue(number.reset());
            if (!entry.isEditing() && entry.value()
                .orElse(Long.MIN_VALUE) != number.value()) entry.value(number.value());
            used++;
        }
        for (int i = used; i < entries.size(); i++) {
            entries.get(i)
                .setEnabled(false);
            entryRows.set(i, null);
        }
        if (moved) scheduleResize();
    }

    private void addEntry() {
        int index = entries.size();
        NumberEntry entry = new NumberEntry(null).right(gutter + 2)
            .top(0)
            .size(NUMBER_WIDTH, ROW - 2)
            .onChange(value -> {
                Row row = entryRows.get(index);
                if (row != null && row.number() != null) row.number()
                    .set()
                    .accept(value);
            });
        entries.add(entry);
        entryRows.add(null);
        entryTops.add(-1);
        addChild(entry, -1);
    }

    private void layout(boolean themed) {
        int nextEdge = themed ? 1 : 0, nextGutter = themed ? AE2_GUTTER : SCROLLBAR;
        ae2 = themed;
        if (nextEdge == edge && nextGutter == gutter) return;
        edge = nextEdge;
        gutter = nextGutter;
        for (NumberEntry entry : entries) entry.right(gutter + 2);
        entryTops.replaceAll(top -> -1);
        placeEntries();
        scheduleResize();
    }

    private int visibleRows() {
        return Math.max(1, (getArea().height - 2 * edge) / ROW);
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - visibleRows());
    }

    private int handleLength() {
        if (ae2) return AE2Theme.SCROLLER_HEIGHT;
        return Math.max(8, getArea().height * visibleRows() / Math.max(1, rows.size()));
    }

    private int handleRange() {
        return getArea().height - handleLength() - (ae2 ? 2 : 0);
    }

    private int handleTop() {
        return (ae2 ? 1 : 0) + handleRange() * scroll / Math.max(1, maxScroll());
    }

    private boolean overScrollbar() {
        return getContext().getMouseX() >= getArea().width - gutter;
    }

    private void dragTo(int mouseY) {
        int max = maxScroll(), range = handleRange();
        if (max == 0 || range <= 0) return;
        int position = mouseY - grab - (ae2 ? 1 : 0);
        scroll = Math.max(0, Math.min(max, Math.round((float) position * max / range)));
    }

    private int hoveredRow() {
        if (!isHovering() || overScrollbar()) return -1;
        int mouseY = getContext().getMouseY() - edge;
        int row = scroll + Math.floorDiv(mouseY, ROW);
        return mouseY >= 0 && row < rows.size() && mouseY < visibleRows() * ROW ? row : -1;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        Variant variant = AE2Theme.active(context);
        layout(variant != null);
        int width = getArea().width, height = getArea().height;
        AE2Theme.drawWell(variant, 0, 0, variant == null ? width : width - AE2_SCROLL_COLUMN, height, BACKGROUND);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int hovered = hoveredRow(), visible = visibleRows();
        int rowRight = width - gutter, textRight = rowRight - 2;
        int shade = hoverColor(variant);
        for (int i = 0; i < visible && scroll + i < rows.size(); i++) {
            int index = scroll + i;
            Row row = rows.get(index);
            int top = edge + i * ROW, left = edge + (nested[index] ? INDENT : 0);
            int textColor = color(variant, row.color(), variant == null ? TEXT : variant.colors.text());
            int detailColor = color(
                variant,
                row.detailColor() != 0 ? row.detailColor() : row.color(),
                variant == null ? TEXT : variant.colors.secondary());
            if (nested[index]) GuiDraw.drawRect(edge, top, rowRight - edge, ROW, variant == null ? NESTED : shade);
            if (row.selected()) {
                if (variant == null) GuiDraw.drawRect(left, top, rowRight - left, ROW, SELECTED);
                else {
                    variant.listRowSelected.draw(left, top, rowRight - left, ROW);
                    textColor = detailColor = variant.highlightText;
                }
            }
            if (index == hovered && !row.header() && !row.selected())
                GuiDraw.drawRect(left, top, rowRight - left, ROW, shade);
            int textLeft = left + 3;
            if (row.icon() != null) {
                new ItemDrawable(row.icon()).draw(context, left + 1, top + 1, 16, 16, widgetTheme.getTheme());
                textLeft = left + 19;
            }
            int right = textRight;
            if (row.number() != null) right -= NUMBER_WIDTH + 2;
            if (row.options() != null) {
                AE2Theme.drawCaret(
                    textRight - CARET + 2,
                    top + 7,
                    row.text()
                        .equals(expanded),
                    variant == null ? ACCENT : textColor);
                right -= CARET;
            }
            if (row.toggle() != null) right -= drawSwitch(variant, textRight, top, row.toggle(), index == hovered) + 3;
            String detail = row.detail() == null ? "" : row.detail();
            int detailWidth = detail.isEmpty() ? 0 : font.getStringWidth(detail) + 3;
            if (!detail.isEmpty()) font.drawString(detail, right - detailWidth + 3, top + 5, detailColor);
            String text = font.trimStringToWidth(row.text(), Math.max(0, right - detailWidth - textLeft));
            if (row.header()) drawHeader(variant, font, text, textLeft, top, rowRight);
            else font.drawString(text, textLeft, top + 5, textColor);
        }
        scrollbar(variant, width, height, visible);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private static int color(Variant variant, int color, int plain) {
        if (color == TEXT) return plain;
        if (variant == null) return color;
        return color == GOOD ? variant.colors.good() : color == BAD ? variant.colors.bad() : color;
    }

    /**
     * Returns the color laid over a row under the mouse.
     *
     * @param variant active AE2 variant, or {@code null} for the list's own dark look
     * @return ARGB color
     */
    public static int hoverColor(@Nullable Variant variant) {
        return variant == null ? HOVER : variant == Variant.LIGHT ? LIGHT_SHADE : DARK_SHADE;
    }

    /**
     * Draws a row's on/off switch against the row's right end, centered in the row; see
     * {@link AE2Theme#drawSwitch}.
     *
     * @param variant active AE2 variant, or {@code null} outside the AE2 themes
     * @param right   pixel the switch ends at
     * @param top     top of the {@link #ROW}-high row
     * @param on      whether the switch is on
     * @param hovered whether the row is under the mouse
     * @return width of the drawn switch
     */
    public static int drawSwitch(@Nullable Variant variant, int right, int top, boolean on, boolean hovered) {
        int width = variant == null ? AE2Theme.SWITCH_HEIGHT : AE2Theme.SWITCH_WIDTH;
        return AE2Theme.drawSwitch(variant, right - width, top + 3, on, hovered);
    }

    /**
     * Draws a section header: muted text followed, under AE2 themes, by a divider line up to the row's end.
     *
     * @param variant active AE2 variant, or {@code null} for the list's own dark look
     * @param font    font to draw with
     * @param text    header text, already cut to the available width
     * @param left    left edge of the text
     * @param top     top of the {@link #ROW}-high row
     * @param right   right end of the row
     */
    public static void drawHeader(@Nullable Variant variant, FontRenderer font, String text, int left, int top,
        int right) {
        AE2Theme.text(text, left, top + 5, variant == null ? HEADER : variant.colors.muted(), false);
        if (variant == null) return;
        int start = left + font.getStringWidth(text) + 4, width = right - 4 - start;
        if (width <= 0) return;
        GuiDraw.drawRect(start, top + 9, width, 1, variant.colors.line());
        GuiDraw.drawRect(start, top + 10, width, 1, HEADER_LINE_ALPHA | variant.colors.edge() & 0xFFFFFF);
    }

    private void scrollbar(Variant variant, int width, int height, int visible) {
        boolean scrolls = rows.size() > visible;
        if (variant == null) {
            if (scrolls) GuiDraw.drawRect(width - SCROLLBAR, handleTop(), SCROLLBAR, handleLength(), BAR);
            return;
        }
        AE2Theme.drawScrollbar(variant, width - AE2Theme.BIG_SCROLLER_WIDTH, 0, height, handleTop() - 1, scrolls);
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        grab = -1;
        if (overScrollbar()) {
            if (mouseButton != 0 || maxScroll() == 0) return Result.SUCCESS;
            int mouseY = getContext().getMouseY(), top = handleTop(), length = handleLength();
            grab = mouseY >= top && mouseY < top + length ? mouseY - top : length / 2;
            dragTo(mouseY);
            return Result.SUCCESS;
        }
        int index = hoveredRow();
        if (index < 0) return Result.ACCEPT;
        Row row = rows.get(index);
        if (mouseButton == 1) {
            if (row.secondary() == null) return Result.ACCEPT;
            row.secondary()
                .run();
            Interactable.playButtonClickSound();
            return Result.SUCCESS;
        }
        if (mouseButton != 0) return Result.ACCEPT;
        if (row.options() != null) {
            expanded = row.text()
                .equals(expanded) ? null : row.text();
            Interactable.playButtonClickSound();
        } else if (row.click() != null) {
            row.click()
                .run();
            if (nested[index]) expanded = null;
            Interactable.playButtonClickSound();
        }
        return Result.SUCCESS;
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - scrollDirection.modifier));
        return true;
    }

    @Override
    public void onMouseDrag(int mouseButton, long timeSinceClick) {
        if (grab >= 0) dragTo(getContext().getMouseY());
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        boolean dragging = grab >= 0;
        grab = -1;
        return dragging;
    }

    @Override
    public ItemStack getStackForRecipeViewer() {
        int index = hoveredRow();
        return index < 0 ? null
            : rows.get(index)
                .icon();
    }
}
