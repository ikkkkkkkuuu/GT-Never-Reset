package com.xyp.gtnrlib.gui.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.menu.AbstractMenuButton;
import com.cleanroommc.modularui.widgets.menu.Menu;

/** A floating string selector with silent programmatic updates and scrolling single-line captions. */
public class ScrollingDropdown extends AbstractMenuButton<ScrollingDropdown> {

    /** MUI caches panel handlers by name for the screen lifetime, including removed filter rows. */
    private static final AtomicLong NEXT_MENU_ID = new AtomicLong();
    private static final IDrawable SELECTED = new Rectangle().color(0xFFFFFFFF)
        .hollow(1);
    private final List<String> candidates = new ArrayList<>();
    private String value = "";
    private Consumer<String> onChanged = ignored -> {};
    private int maxCount = 5;

    public ScrollingDropdown(String name, IDrawable button, List<String> candidates) {
        super(name + "_menu_" + NEXT_MENU_ID.getAndIncrement());
        name(name);
        openOnHover = false;
        size(80, 15);
        padding(0);
        background(button);
        disableHoverBackground();
        overlay(new ScrollingTextDrawable(() -> value, 0xFF444444, false));
        tooltip().setAutoUpdate(true)
            .tooltipBuilder(tooltip -> { if (!value.isEmpty()) tooltip.addLine(IKey.str(value)); });
        setCandidates(candidates);
    }

    /** Replaces options and invalidates the popup; never selects or writes a model value implicitly. */
    public ScrollingDropdown setCandidates(List<String> values) {
        List<String> copy = new ArrayList<>(Objects.requireNonNull(values));
        copy.forEach(Objects::requireNonNull);
        if (isOpen()) closeMenu(false);
        candidates.clear();
        candidates.addAll(copy);
        setMenu(null);
        return this;
    }

    public List<String> getCandidates() {
        return Collections.unmodifiableList(candidates);
    }

    public ScrollingDropdown setValue(String value) {
        this.value = Objects.requireNonNull(value);
        return this;
    }

    public String getSelectedValue() {
        return value;
    }

    public ScrollingDropdown setOnChanged(Consumer<String> callback) {
        onChanged = Objects.requireNonNull(callback);
        return this;
    }

    public ScrollingDropdown setMaxCount(int count) {
        if (count < 1) throw new IllegalArgumentException("Positive row count required");
        maxCount = count;
        setCandidates(new ArrayList<>(candidates));
        return this;
    }

    @Override
    public void openMenu(boolean soft) {
        if (isValid() && areAncestorsEnabled() && !candidates.isEmpty()) super.openMenu(soft);
    }

    /**
     * A menu may still receive close events while its panel is finishing its closing animation.
     * Never walk a removed source's parent chain; also retire menus whose source page is hidden.
     */
    @Override
    protected void checkClose(boolean soft, boolean requireNoHover) {
        if (!isValid() || !areAncestorsEnabled()) {
            closeMenu(false);
            return;
        }
        super.checkClose(soft, requireNoHover);
    }

    /** Closes and drops the floating menu before MUI invalidates this row's parent and context. */
    @Override
    public void dispose() {
        try {
            closeMenu(false);
            setMenu(null);
        } finally {
            super.dispose();
        }
    }

    @Override
    protected Menu<?> createMenu() {
        ListWidget<IWidget, ?> list = new ListWidget<>().widthRel(1f)
            .maxSize(maxCount * 15)
            .scrollDirection(new VerticalScrollData(false, 4).texture(new Rectangle().color(0xFFFFFFFF)));
        for (String candidate : candidates) {
            list.child(
                new ButtonWidget<>().widthRel(1f)
                    .height(15)
                    .padding(0)
                    .background(new DynamicDrawable(() -> candidate.equals(value) ? SELECTED : null))
                    .disableHoverBackground()
                    .overlay(new ScrollingTextDrawable(() -> candidate, 0xFFFFFFFF, true))
                    .tooltipBuilder(tooltip -> tooltip.addLine(IKey.str(candidate)))
                    .onMousePressed(mouse -> {
                        if (mouse != 0) return false;
                        if (!isValid() || !areAncestorsEnabled()) {
                            closeMenu(false);
                            return true;
                        }
                        if (candidates.contains(candidate)) {
                            setValue(candidate);
                            closeMenu(false);
                            onChanged.accept(candidate);
                        }
                        return true;
                    }));
        }
        return new Menu<>().widthRel(1f)
            .coverChildrenHeight()
            .padding(0)
            .background(new Rectangle().color(0xAA000000))
            .child(list);
    }

    /**
     * Single-line selector caption: short text is centered, long text scrolls between its ends without wrapping.
     * Uses MUI's clipping implementation so floating menus and scroll-list transforms remain aligned.
     * Each caption owns its renderer and timing; changing its value resets the initial reading pause.
     */
    private static final class ScrollingTextDrawable implements IDrawable {

        private final Supplier<String> text;
        private final TextRenderer renderer = new TextRenderer();
        private String previous;
        private long started;

        private ScrollingTextDrawable(Supplier<String> text, int color, boolean shadow) {
            this.text = Objects.requireNonNull(text);
            renderer.setColor(color);
            renderer.setShadow(shadow);
            renderer.setHardWrapOnBorder(false);
        }

        @Override
        public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme theme) {
            if (width <= 4 || height <= 0) return;
            String supplied = text.get();
            String value = supplied == null ? ""
                : supplied.replace('\n', ' ')
                    .replace('\r', ' ');
            long now = Minecraft.getSystemTime();
            if (!value.equals(previous)) {
                previous = value;
                started = now;
            }
            int available = width - 4;
            TextRenderer.Line line = renderer.line(value);
            renderer.setPos(x + 2, y);
            boolean overflow = line.getWidth() > available;
            renderer.setAlignment(overflow ? Alignment.CenterLeft : Alignment.Center, available, height);
            if (overflow) {
                long travel = Math.max(1000, (long) ((line.getWidth() - available) * 50));
                long phase = Math.max(0, now - started) % (travel * 2 + 1600);
                float progress = phase < 800 ? 0
                    : phase < 800 + travel ? (phase - 800) / (float) travel
                        : phase < 1600 + travel ? 1 : 1 - (phase - 1600 - travel) / (float) travel;
                renderer.drawScrolling(line, progress, null, context);
            } else {
                renderer.drawSimple(value);
            }
        }
    }
}
