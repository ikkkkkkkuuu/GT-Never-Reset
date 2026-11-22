package com.xyp.gtnrlib.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.ae2.AE2ButtonPainter;
import com.xyp.gtnrlib.gui.theme.ae2.IconSource;

/**
 * AE2's page tabs, the mode tabs of its pattern encoding terminal, as one widget: 22-by-22 tabs stacked
 * {@link AE2Theme#PAGE_TAB_PITCH} apart down the right edge of a panel, one per page. The owner keeps the open page;
 * the tabs read it every frame and hand every choice back, so code may also switch pages itself. It can be built on
 * either side, so it also works in GUIs the server opens.
 * <p>
 * Place the widget at {@code panelRight - AE2Theme.PAGE_TAB_INSET} and add it after the panel's other children: the
 * open tab then covers the panel border and joins the panel, and the others stand out. {@link #page(int, Widget)} shows
 * a page widget only while its tab is open. Other themes draw the tabs as buttons, the open one hovered. A tab made
 * unavailable with {@link #available(BooleanSupplier)} shows no hover, draws its icon faded and swallows clicks.
 * {@link #tabSize(int, int)} changes the size of the tabs from AE2's 22 by 22 pixels. The tabs stand outside the panel,
 * so they tell NEI to keep its panels clear of them, as AE2's do.
 */
public class PageTabs extends ParentWidget<PageTabs> {

    private static final int MIN_WIDTH = AE2Theme.PAGE_TAB_INSET + AE2Theme.ICON_SIZE + 2,
        MIN_HEIGHT = AE2Theme.PAGE_TAB_SIZE;

    private final IntSupplier open;
    private final IntConsumer select;
    private final List<BooleanSupplier[]> usable = new ArrayList<>();
    private final List<ThemedButton<?>> buttons = new ArrayList<>();
    private int tabWidth = AE2Theme.PAGE_TAB_SIZE, tabHeight = AE2Theme.PAGE_TAB_SIZE;

    /**
     * Creates an empty column of tabs.
     *
     * @param open   supplier of the open page's index, read every frame
     * @param select receives the index of a tab the player clicks while another page is open
     */
    public PageTabs(IntSupplier open, IntConsumer select) {
        this.open = open;
        this.select = select;
        size(AE2Theme.PAGE_TAB_SIZE, 0).excludeAreaInRecipeViewer();
    }

    /**
     * Sets the size of every tab, also of tabs added before; AE2's are {@link AE2Theme#PAGE_TAB_SIZE} pixels square.
     * Neighbouring tabs share one outline row, so tabs start {@code height - 1} pixels apart. The widget becomes as
     * wide as a tab.
     *
     * @param width  tab width in pixels, at least 21 so a 16-pixel icon fits beside the covered panel border
     * @param height tab height in pixels, at least 22 so a 16-pixel icon fits between the frame's top and bottom
     * @return these tabs
     */
    public PageTabs tabSize(int width, int height) {
        tabWidth = Math.max(MIN_WIDTH, width);
        tabHeight = Math.max(MIN_HEIGHT, height);
        for (int i = 0; i < buttons.size(); i++) buttons.get(i)
            .pos(0, i * pitch())
            .size(tabWidth, tabHeight);
        resize();
        return this;
    }

    private int pitch() {
        return tabHeight - 1;
    }

    private void resize() {
        size(tabWidth, buttons.isEmpty() ? 0 : buttons.size() * pitch() + 1);
    }

    /**
     * Adds a tab below the existing ones; the first tab is page zero.
     *
     * @param icon    supplier of the icon, read every frame: an {@code Icon} or {@code AE2SettingIcon}, or any
     *                texture or item through {@link IconSource#of}
     * @param tooltip supplier of the tooltip line, such as the page name, read every frame
     * @return these tabs
     */
    public PageTabs tab(Supplier<? extends IconSource> icon, Supplier<String> tooltip) {
        int index = usable.size();
        BooleanSupplier[] available = { () -> true };
        usable.add(available);
        ThemedButton<?> tab = new ThemedButton<>();
        IDrawable look = AE2ButtonPainter
            .pageTab(tab, icon, () -> open.getAsInt() == index, () -> available[0].getAsBoolean());
        tab.available(() -> available[0].getAsBoolean())
            .background(look)
            .hoverBackground(look)
            .disableHoverThemeBackground(true)
            .pos(0, index * pitch())
            .size(tabWidth, tabHeight)
            .tooltipAutoUpdate(true)
            .tooltipBuilder(lines -> lines.addLine(tooltip.get()))
            .onMousePressed(mouse -> {
                if (mouse != 0) return false;
                if (open.getAsInt() != index) select.accept(index);
                return true;
            });
        buttons.add(tab);
        child(tab);
        resize();
        return this;
    }

    /**
     * Sets when the tab added last can be opened.
     *
     * @param available supplier read every frame and on every click
     * @return these tabs
     * @throws IllegalStateException if no tab was added yet
     */
    public PageTabs available(BooleanSupplier available) {
        if (usable.isEmpty()) throw new IllegalStateException("Add a tab before setting when it is available");
        usable.get(usable.size() - 1)[0] = available;
        return this;
    }

    /**
     * Shows a page widget only while its tab is open, by setting the widget's enabled condition.
     *
     * @param index page index, the position of its tab
     * @param page  widget holding the page
     * @param <W>   widget type
     * @return the same widget
     */
    public <W extends Widget<?>> W page(int index, W page) {
        page.setEnabledIf(widget -> open.getAsInt() == index);
        return page;
    }
}
