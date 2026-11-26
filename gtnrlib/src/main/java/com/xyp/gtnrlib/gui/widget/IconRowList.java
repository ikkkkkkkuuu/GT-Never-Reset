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
import com.xyp.gtnrlib.gui.theme.ae2.AE2ListRowPainter;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;
import com.xyp.gtnrlib.gui.theme.ae2.AE2PanelPainter;
import com.xyp.gtnrlib.gui.theme.ae2.PlainLook;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * A scrolling list whose rows are drawn directly instead of being widgets, so it stays fast with hundreds of rows. The
 * rows are read from a supplier every tick. A row can carry an item icon, a right-aligned detail, a check box, a
 * dropdown whose options open as indented rows under it, or a number field. Number fields are {@link NumberEntryWidget}
 * children placed over the visible number rows, so only those rows cost a widget.
 * <p>
 * Under an AE2 theme the list draws as an AE2 inset with AE2 list-row highlights and check boxes, and AE2's scroll bar
 * beside the inset; otherwise it draws as Minecraft's sunken slot box with MUI2's check boxes and a thin bar, in the
 * colors of {@link PlainLook}. Rows are drawn with {@link AE2ListRowPainter}, like {@link ListRow}'s. The scroll bar is
 * a {@link Scrollbar} child: the wheel moves one row, as in AE2's network status and crafting CPU screens. The wheel
 * scrolls the list everywhere, also over number fields, unless {@link #numberWheel(boolean) numberWheel(true)} lets it
 * step the field under the pointer instead.
 */
public class IconRowList extends ParentWidget<IconRowList> implements Interactable, RecipeViewerIngredientProvider {

    /**
     * Standard row height, width of the plain look's thin scroll bar, indent of a dropdown's options, width kept for a
     * dropdown's caret and standard width of a number field, in pixels.
     */
    public static final int ROW = AE2ListRowPainter.ROW_HEIGHT, SCROLLBAR = 3, INDENT = 10, CARET = 9,
        NUMBER_WIDTH = 64;
    /**
     * Row colors that stand for the look's own colors: a row in {@code TEXT} uses the look's list text color, and
     * header rows carry {@code HEADER}.
     */
    public static final int HEADER = runtime(0xFFA0A0A8), TEXT = runtime(0xFFE8E8E8);
    /**
     * Row colors that stand for the look's good and bad colors; they are the plain look's own. Pass these fields rather
     * than their values: the values may change between GTNRLib versions.
     */
    public static final int GOOD = runtime(PlainLook.GOOD), BAD = runtime(PlainLook.BAD);
    /**
     * Width taken by the scroll bar right of the inset under AE2 themes, like AE2's lists: the groove sits on the panel
     * background, eight pixels from the inset's edge, as in AE2's network status and crafting CPU screens.
     */
    public static final int AE2_SCROLL_COLUMN = AE2Theme.BIG_SCROLLER_WIDTH + 5;
    private static final int AE2_GUTTER = AE2_SCROLL_COLUMN + 1, PLAIN_GUTTER = SCROLLBAR + AE2Look.WELL_EDGE,
        ICON = 16, MIN_ROW = ICON, MIN_NUMBER_WIDTH = 32;

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
     * One row, made by one of the factory methods and adjusted with the {@code with...} methods, each of which returns
     * a changed copy. A row is a header, a plain or clickable item, a check box, a dropdown or a number field. An
     * unavailable row is drawn muted with a faded check box, does not highlight and ignores clicks; its number field
     * looks disabled.
     */
    public record Row(ItemStack icon, String text, String detail, int color, boolean selected, boolean header,
        Runnable click, List<Row> options, Boolean toggle, Numeric number, Runnable secondary, int detailColor,
        boolean available, Object key) {

        public Row(ItemStack icon, String text, String detail, int color, boolean selected, boolean header,
            Runnable click, List<Row> options, Boolean toggle, Numeric number, Runnable secondary, int detailColor,
            boolean available) {
            this(
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
                secondary,
                detailColor,
                available,
                null);
        }

        public Row withKey(Object key) {
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
                secondary,
                detailColor,
                available,
                key);
        }

        public static Row checkbox(String text, boolean on, Runnable click) {
            return toggle(text, on, click);
        }

        public Row(ItemStack icon, String text, String detail, int color, boolean selected, boolean header,
            Runnable click, List<Row> options, Boolean toggle, Numeric number, Runnable secondary, int detailColor) {
            this(
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
                secondary,
                detailColor,
                true);
        }

        public static Row header(String text) {
            return new Row(null, text, "", HEADER, false, true, null, null, null, null, null, 0, true);
        }

        public static Row item(ItemStack icon, String text, String detail, boolean selected, Runnable click) {
            return new Row(icon, text, detail, TEXT, selected, false, click, null, null, null, null, 0, true);
        }

        public static Row colored(ItemStack icon, String text, String detail, int color) {
            return new Row(icon, text, detail, color, false, false, null, null, null, null, null, 0, true);
        }

        public static Row toggle(String text, boolean on, Runnable click) {
            return new Row(null, text, "", TEXT, false, false, click, null, on, null, null, 0, true);
        }

        public static Row dropdown(ItemStack icon, String text, String detail, boolean selected, List<Row> options) {
            return new Row(icon, text, detail, TEXT, selected, false, null, options, null, null, null, 0, true);
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
                0,
                true);
        }

        /**
         * Returns a copy of this row that can or cannot be used.
         *
         * @param available whether the row reacts to the mouse
         * @return the new row
         */
        public Row withAvailable(boolean available) {
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
                secondary,
                detailColor,
                available,
                key);
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
                detailColor,
                available,
                key);
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
                color,
                available,
                key);
        }
    }

    private record Occurrence(String text, int index) {}

    private final Supplier<List<Row>> source;
    private final List<NumberEntryWidget> entries = new ArrayList<>();
    private final List<Row> entryRows = new ArrayList<>();
    private final IntList entryTops = new IntArrayList();
    private List<Row> rows = Collections.emptyList();
    private boolean[] nested = new boolean[0];
    private Object[] identities = new Object[0];
    private Object expanded;
    private int scroll, edge, gutter = SCROLLBAR, rowHeight = ROW, numberWidth = NUMBER_WIDTH;
    private boolean revealSelected, numberWheel;
    private final Scrollbar scrollbar = new Scrollbar(() -> scroll, this::maxScroll, row -> scroll = row)
        .plainBar(PlainLook.THIN_SCROLL_BAR, this::visibleRows);

    /**
     * Creates a list over a supplier of rows. The caller sets its size.
     *
     * @param source supplier of the rows, read every tick while the list is shown; dropdown options are added by the
     *               list while a dropdown is open
     */
    public IconRowList(Supplier<List<Row>> source) {
        this.source = source;
        child(
            scrollbar.right(0)
                .top(0)
                .heightRel(1f)
                .width(SCROLLBAR));
    }

    /**
     * Scrolls back to the first row.
     */
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

    /**
     * Sets the height of every row; the standard is {@link #ROW}. Text, icons, check boxes, carets and number fields
     * stay centred in the row. Make the list's inner height a multiple of it, or a strip stays empty at the bottom.
     *
     * @param rowHeight row height in pixels, at least 16 so a 16-pixel item icon fits
     * @return this list
     */
    public IconRowList rowHeight(int rowHeight) {
        this.rowHeight = Math.max(MIN_ROW, rowHeight);
        resizeEntries();
        return this;
    }

    /**
     * Sets the width of the number fields of number rows; the standard is {@link #NUMBER_WIDTH}.
     *
     * @param numberWidth field width in pixels, at least 32 so the arrows and a few digits fit
     * @return this list
     */
    public IconRowList numberWidth(int numberWidth) {
        this.numberWidth = Math.max(MIN_NUMBER_WIDTH, numberWidth);
        resizeEntries();
        return this;
    }

    /**
     * Sets whether the mouse wheel over a number row's field steps its value, as
     * {@link NumberEntryWidget#wheel(boolean)} does. Off by default, so the wheel scrolls the list there too.
     *
     * @param numberWheel true to let the wheel step number fields
     * @return this list
     */
    public IconRowList numberWheel(boolean numberWheel) {
        this.numberWheel = numberWheel;
        for (NumberEntryWidget entry : entries) entry.wheel(numberWheel);
        return this;
    }

    private void resizeEntries() {
        for (NumberEntryWidget entry : entries) entry.size(numberWidth, rowHeight - 2);
        entryTops.replaceAll(top -> -1);
        scheduleResize();
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

    private boolean isExpanded(Object identity) {
        return identity != null && identity.equals(expanded);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        // MUI2 updates hidden widgets too; a hidden list has nothing to rebuild.
        if (!isEnabled()) return;
        List<Row> flat = new ArrayList<>();
        List<Boolean> inside = new ArrayList<>();
        List<Object> seen = new ArrayList<>();
        Object2IntOpenHashMap<String> counts = new Object2IntOpenHashMap<>();
        for (Row row : source.get()) {
            int count = counts.addTo(row.text, 1);
            Object identity = row.key != null ? row.key : new Occurrence(row.text, count);
            flat.add(row);
            inside.add(false);
            seen.add(identity);
            if (row.options == null || !isExpanded(identity)) continue;
            if (!row.available) {
                expanded = null;
                continue;
            }
            for (Row option : row.options) {
                flat.add(option);
                inside.add(true);
                seen.add(null);
            }
        }
        rows = flat;
        nested = new boolean[flat.size()];
        for (int i = 0; i < nested.length; i++) nested[i] = inside.get(i);
        identities = seen.toArray();
        if (revealSelected && getArea().height >= rowHeight) {
            revealSelected = false;
            for (int i = 0; i < flat.size(); i++) {
                if (!flat.get(i).selected) continue;
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
            if (row.number == null) continue;
            if (used == entries.size()) {
                addEntry();
                moved = true;
            }
            NumberEntryWidget entry = entries.get(used);
            int top = edge + i * rowHeight + 1;
            if (entryTops.getInt(used) != top) {
                entry.top(top);
                entryTops.set(used, top);
                moved = true;
            }
            entryRows.set(used, row);
            entry.setEnabled(true);
            Numeric number = row.number;
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
        NumberEntryWidget entry = new NumberEntryWidget().right(gutter + 2)
            .top(0)
            .size(numberWidth, rowHeight - 2)
            .wheel(numberWheel)
            .available(() -> {
                Row row = entryRows.get(index);
                return row == null || row.available;
            })
            .onChange(value -> {
                Row row = entryRows.get(index);
                if (row != null && row.number != null) row.number.set()
                    .accept(value);
            });
        entries.add(entry);
        entryRows.add(null);
        entryTops.add(-1);
        addChild(entry, -1);
    }

    private void layout(boolean themed) {
        int nextGutter = themed ? AE2_GUTTER : PLAIN_GUTTER;
        if (edge == AE2Look.WELL_EDGE && nextGutter == gutter) return;
        edge = AE2Look.WELL_EDGE;
        gutter = nextGutter;
        scrollbar.width(themed ? AE2Theme.BIG_SCROLLER_WIDTH : SCROLLBAR)
            .right(themed ? 0 : AE2Look.WELL_EDGE);
        for (NumberEntryWidget entry : entries) entry.right(gutter + 2);
        entryTops.replaceAll(top -> -1);
        placeEntries();
        scheduleResize();
    }

    private int visibleRows() {
        return Math.max(1, (getArea().height - 2 * edge) / rowHeight);
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - visibleRows());
    }

    private boolean inGutter() {
        return getContext().getMouseX() >= getArea().width - gutter;
    }

    private int hoveredRow() {
        if (!isHovering() || inGutter()) return -1;
        int mouseY = getContext().getMouseY() - edge;
        int row = scroll + Math.floorDiv(mouseY, rowHeight);
        return mouseY >= 0 && row < rows.size() && mouseY < visibleRows() * rowHeight ? row : -1;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        AE2Look look = AE2Look.of(this);
        layout(look.isAE2());
        int width = getArea().width, height = getArea().height;
        look.drawWell(0, 0, look.isAE2() ? width - AE2_SCROLL_COLUMN : width, height);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int hovered = hoveredRow(), visible = visibleRows(), textTop = AE2ListRowPainter.textTop(rowHeight);
        int rowRight = width - gutter, textRight = rowRight - 2;
        for (int i = 0; i < visible && scroll + i < rows.size(); i++) {
            int index = scroll + i;
            Row row = rows.get(index);
            int top = edge + i * rowHeight, left = edge + (nested[index] ? INDENT : 0);
            boolean usable = row.available, lit = usable && index == hovered, shadow = usable && look.listShadow();
            int textColor = usable ? color(look, row.color, look.listText()) : look.listMuted();
            int detailColor = usable
                ? color(look, row.detailColor != 0 ? row.detailColor : row.color, look.listDetail())
                : look.listMuted();
            if (nested[index]) GuiDraw.drawRect(edge, top, rowRight - edge, rowHeight, look.nested());
            if (row.selected) {
                look.drawSelectedRow(left, top, rowRight - left, rowHeight);
                textColor = detailColor = look.selectedText();
            }
            if (lit && !row.header && !row.selected)
                GuiDraw.drawRect(left, top, rowRight - left, rowHeight, look.hover());
            int textLeft = left + 3;
            if (row.icon != null) {
                drawItemIcon(context, widgetTheme, row, left + 1, top + (rowHeight - ICON) / 2);
                textLeft = left + 19;
            }
            int right = textRight;
            if (row.number != null) right -= numberWidth + 2;
            if (row.options != null) {
                AE2PanelPainter.drawCaret(
                    textRight - CARET + 2,
                    top + (rowHeight - AE2Theme.CARET_HEIGHT) / 2,
                    isExpanded(identities[index]),
                    textColor);
                right -= CARET;
            }
            if (row.toggle != null)
                right -= AE2ListRowPainter.drawCheckbox(look, textRight, top, rowHeight, row.toggle, lit, usable) + 3;
            String detail = row.detail == null ? "" : row.detail;
            int detailWidth = detail.isEmpty() ? 0 : font.getStringWidth(detail) + 3, textY = top + textTop;
            if (!detail.isEmpty()) font.drawString(detail, right - detailWidth + 3, textY, detailColor, shadow);
            String text = font.trimStringToWidth(row.text, Math.max(0, right - detailWidth - textLeft));
            if (row.header) AE2ListRowPainter.drawHeader(look, text, textLeft, top, rowHeight, rowRight);
            else font.drawString(text, textLeft, textY, textColor, shadow);
        }
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    protected void drawItemIcon(ModularGuiContext context, WidgetThemeEntry<?> theme, Row row, int x, int y) {
        new ItemDrawable(row.icon()).draw(context, x, y, ICON, ICON, theme.getTheme());
    }

    // Set through a call so that other mods read the marker colors at run time instead of compiling their values in.
    private static int runtime(int color) {
        return color;
    }

    private static int color(AE2Look look, int color, int plain) {
        if (color == TEXT) return plain;
        return color == GOOD ? look.good() : color == BAD ? look.bad() : color;
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        int index = hoveredRow();
        if (index < 0) return Result.ACCEPT;
        Row row = rows.get(index);
        if (!row.available) return Result.STOP;
        if (mouseButton == 1) {
            if (row.secondary == null) return Result.ACCEPT;
            row.secondary.run();
            Interactable.playButtonClickSound();
            return Result.SUCCESS;
        }
        if (mouseButton != 0) return Result.ACCEPT;
        if (row.options != null) {
            expanded = isExpanded(identities[index]) ? null : identities[index];
            Interactable.playButtonClickSound();
        } else if (row.click != null) {
            row.click.run();
            if (nested[index]) expanded = null;
            Interactable.playButtonClickSound();
        }
        return Result.SUCCESS;
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        scrollbar.scroll(scrollDirection);
        return true;
    }

    @Override
    public ItemStack getStackForRecipeViewer() {
        int index = hoveredRow();
        return index < 0 ? null : rows.get(index).icon;
    }

    @SideOnly(Side.CLIENT)
    public static int drawSwitch(@Nullable Variant variant, int right, int top, int rowHeight, boolean on,
        boolean hovered) {
        return AE2ListRowPainter.drawCheckbox(AE2Look.of(variant), right, top, rowHeight, on, hovered, true);
    }

    @SideOnly(Side.CLIENT)
    public static void drawHeader(@Nullable Variant variant, FontRenderer font, String text, int left, int top,
        int right) {
        AE2ListRowPainter.drawHeader(AE2Look.of(variant), text, left, top, ROW, right);
    }

    @SideOnly(Side.CLIENT)
    public static int drawSwitch(@Nullable Variant variant, int right, int top, boolean on, boolean hovered) {
        return AE2ListRowPainter.drawCheckbox(AE2Look.of(variant), right, top, ROW, on, hovered, true);
    }

    @SideOnly(Side.CLIENT)
    public static void drawHeader(@Nullable Variant variant, FontRenderer font, String text, int left, int top,
        int rowHeight, int right) {
        AE2ListRowPainter.drawHeader(AE2Look.of(variant), text, left, top, rowHeight, right);
    }
}
