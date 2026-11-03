package com.xyp.gtneverreset.client.wireless;

import static com.xyp.gtneverreset.client.wireless.WirelessCardPanel.ROW_HEIGHT;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.api.widget.IGuiAction;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.scroll.ScrollArea;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.widget.HandleScrollData;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class WirelessCardScrollbar extends Widget<WirelessCardScrollbar> implements Interactable {

    private final ScrollArea area = new ScrollArea();
    private final HandleScrollData scroll;
    private final Supplier<NBTTagCompound> view;
    private final BooleanSupplier ready;
    private final IntConsumer scrollTo;
    private int dragMouseY, dragOffset;

    WirelessCardScrollbar(AE2Theme.Variant variant, Supplier<NBTTagCompound> view, BooleanSupplier ready,
        IntConsumer scrollTo) {
        this.view = view;
        this.ready = ready;
        this.scrollTo = scrollTo;
        scroll = new HandleScrollData(variant);
        area.setScrollDataY(scroll);
        listenGuiAction((IGuiAction.MouseReleased) button -> {
            area.mouseReleased(0, 0);
            return false;
        });
    }

    @Override
    public ScrollArea getArea() {
        return area;
    }

    @Override
    public boolean canHover() {
        return true;
    }

    private void sync() {
        scroll.setScrollSize(
            view.get()
                .getInteger("rowCount") * ROW_HEIGHT + 2);
        scroll.scrollTo(
            area,
            view.get()
                .getInteger("scroll") * ROW_HEIGHT);
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        sync();
        scroll.drawTrack(area);
    }

    @Override
    public Result onMousePressed(int button) {
        if (button != 0 || !ready.getAsBoolean()) return Result.STOP;
        sync();
        int x = getContext().getAbsMouseX() - area.x, y = getContext().getAbsMouseY() - area.y;
        if (!area.mouseClicked(x, y)) return Result.STOP;
        int length = scroll.getScrollBarLength(area), start = scroll.getScrollBarStart(area, length, false);
        if (y < start || y > start + length) {
            int max = view.get()
                .getInteger("maxScroll");
            dragOffset = Math.max(0, Math.min(max, Math.round(scroll.getProgress(area, y, x) * max)));
            scrollTo.accept(dragOffset);
        } else dragOffset = view.get()
            .getInteger("scroll");
        // Anchor to the exact row offset; the rendered thumb's integer position loses precision on long lists.
        dragMouseY = getContext().getAbsMouseY();
        return Result.SUCCESS;
    }

    @Override
    public void onMouseDrag(int button, long timeSinceClick) {
        if (button != 0 || !ready.getAsBoolean() || !scroll.isDragging()) return;
        sync();
        if (!scroll.isScrollBarActive(area)) return;
        int travel = scroll.getFullVisibleSize(area) - scroll.getScrollBarLength(area);
        if (travel <= 0) return;
        int delta = getContext().getAbsMouseY() - dragMouseY;
        scrollTo.accept(
            dragOffset + Math.round(
                (float) delta * view.get()
                    .getInteger("maxScroll") / travel));
    }

    @Override
    public boolean onMouseRelease(int button) {
        area.mouseReleased(0, 0);
        return true;
    }
}
