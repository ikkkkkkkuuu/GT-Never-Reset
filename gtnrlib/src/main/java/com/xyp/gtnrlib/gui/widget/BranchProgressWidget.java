package com.xyp.gtnrlib.gui.widget;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.drawable.Stencil;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A complete branching connection, revealed by one bottom-to-top progress boundary. */
public final class BranchProgressWidget extends Widget<BranchProgressWidget> {

    private static final int TEXTURE_HEIGHT = 18;
    private static final int LINE_LEADING_EXTENT = 2;
    private static final int EMPTY_LINE_TRAILING_EXTENT = 1;
    private static final int FILLED_LINE_TRAILING_EXTENT = 2;
    private DoubleSupplier value = () -> 0;
    private BooleanSupplier completed = () -> false;
    private UITexture emptyLine, fullLine, completedLine, emptyArrow, fullArrow, completedArrow;
    private int[] sourceXs = new int[0];
    private int targetX;
    private int junctionY;

    /** Coordinates are in the parent canvas; all sources share the same Y coordinate. */
    public BranchProgressWidget layout(int targetX, int targetY, int junctionY, int sourceY, int... sourceXs) {
        if (sourceXs.length == 0 || sourceY <= targetY) throw new IllegalArgumentException("Invalid branch endpoints");
        int[] sorted = sourceXs.clone();
        Arrays.sort(sorted);
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i] == sorted[i - 1]) throw new IllegalArgumentException("Duplicate branch endpoint");
        }
        if (targetX < sorted[0] || targetX > sorted[sorted.length - 1]
            || sorted.length > 1 && (junctionY <= targetY || junctionY >= sourceY))
            throw new IllegalArgumentException("Junction must lie between sources and target");
        int originX = sorted[0] - TEXTURE_HEIGHT / 2;
        int width = sorted[sorted.length - 1] - sorted[0] + TEXTURE_HEIGHT;
        this.targetX = targetX - originX;
        this.junctionY = junctionY - targetY;
        for (int i = 0; i < sorted.length; i++) sorted[i] -= originX;
        this.sourceXs = sorted;
        pos(originX, targetY).size(width, sourceY - targetY);
        return this;
    }

    public BranchProgressWidget value(DoubleSupplier value) {
        this.value = Objects.requireNonNull(value);
        return this;
    }

    public BranchProgressWidget completed(BooleanSupplier completed) {
        this.completed = Objects.requireNonNull(completed);
        return this;
    }

    /** Line and arrow textures point right; the caller supplies their visual style. */
    public BranchProgressWidget textures(UITexture emptyLine, UITexture fullLine, UITexture emptyArrow,
        UITexture fullArrow) {
        this.emptyLine = Objects.requireNonNull(emptyLine);
        this.fullLine = Objects.requireNonNull(fullLine);
        this.emptyArrow = Objects.requireNonNull(emptyArrow);
        this.fullArrow = Objects.requireNonNull(fullArrow);
        return this;
    }

    public BranchProgressWidget completedTextures(UITexture line, UITexture arrow) {
        completedLine = Objects.requireNonNull(line);
        completedArrow = Objects.requireNonNull(arrow);
        return this;
    }

    public double getCurrentProgress() {
        double progress = value.getAsDouble();
        return Double.isNaN(progress) ? 0 : Math.max(0, Math.min(1, progress));
    }

    public int getFillTop() {
        return getArea().height - (int) Math.round(getArea().height * getCurrentProgress());
    }

    public boolean isComplete() {
        return completed.getAsBoolean();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> entry) {
        if (sourceXs.length == 0 || emptyLine == null) return;
        WidgetTheme theme = getActiveWidgetTheme(entry, isHovering());
        drawShape(context, theme, false);
        int top = getFillTop();
        if (top >= getArea().height) return;
        Stencil.apply(0, top, getArea().width, getArea().height - top, context);
        try {
            drawShape(context, theme, true);
        } finally {
            Stencil.remove();
        }
    }

    @SideOnly(Side.CLIENT)
    private void drawShape(ModularGuiContext context, WidgetTheme theme, boolean filled) {
        boolean complete = filled && isComplete();
        UITexture line = !filled ? emptyLine : complete && completedLine != null ? completedLine : fullLine;
        UITexture arrow = !filled ? emptyArrow : complete && completedArrow != null ? completedArrow : fullArrow;
        int bottom = getArea().height;
        if (sourceXs.length == 1) {
            drawTexture(context, theme, arrow, targetX, bottom, bottom, -90);
            return;
        }
        int left = sourceXs[0] - LINE_LEADING_EXTENT;
        int right = sourceXs[sourceXs.length - 1] + (filled ? FILLED_LINE_TRAILING_EXTENT : EMPTY_LINE_TRAILING_EXTENT);
        // Match the rotated shaft's visible bounds: three pixels empty, four pixels filled.
        drawTexture(context, theme, line, left, junctionY, right - left, 0);
        // Draw both incoming and outgoing strokes over the bar, opening its border at every junction.
        for (int x : sourceXs) drawTexture(context, theme, line, x, bottom, bottom - junctionY, -90);
        drawTexture(context, theme, arrow, targetX, junctionY, junctionY, -90);
    }

    @SideOnly(Side.CLIENT)
    private void drawTexture(ModularGuiContext context, WidgetTheme theme, UITexture texture, int x, int y, int length,
        float angle) {
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x, y, 0);
            GL11.glRotatef(angle, 0, 0, 1);
            texture.draw(context, 0, -TEXTURE_HEIGHT / 2, length, TEXTURE_HEIGHT, theme);
        } finally {
            GL11.glPopMatrix();
        }
    }
}
