package com.xyp.gtnrlib.client.text;

import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.client.gui.FontRenderer;

import com.xyp.gtnrlib.client.text.EffectTextLayout.DrawRun;
import com.xyp.gtnrlib.client.text.EffectTextLayout.Glyph;
import com.xyp.gtnrlib.client.text.EffectTextLayout.Layout;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Render-thread-owned typing clock. A marked string starts typing when it appears and starts over after it has not
 * been drawn for a frame. Lines inside one string type one after another, and so do strings drawn lower at the same x
 * in the same frame, such as the lines of a tooltip.
 */
public class TypewriterTimeline {

    public static final TypewriterTimeline INSTANCE = new TypewriterTimeline();
    private static final double CARET_BLINK_SECONDS = 0.4;
    private static final long FORGET_AFTER_FRAMES = 1;
    private static final long SWEEP_INTERVAL_FRAMES = 120;
    private static BooleanSupplier enabled = () -> true;
    private final Object2ObjectOpenHashMap<String, Appearance> appearances = new Object2ObjectOpenHashMap<>();
    private final Chain chain = new Chain();
    private long frame;
    private int stillDepth;

    public static void enabledWith(BooleanSupplier setting) {
        enabled = setting;
    }

    /** Text being edited, such as a text field, shows in full until the matching {@link #endStill()}. */
    public void beginStill() {
        stillDepth++;
    }

    public void endStill() {
        stillDepth--;
    }

    public boolean isStill() {
        return stillDepth > 0;
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (event.phase == Phase.START) nextFrame();
    }

    public void nextFrame() {
        frame++;
        if (frame % SWEEP_INTERVAL_FRAMES == 0) appearances.values()
            .removeIf(appearance -> appearance.lastFrame < frame - FORGET_AFTER_FRAMES);
    }

    public void clear() {
        appearances.clear();
        chain.frame = -1;
    }

    /** Returns null when the text shows in full: it has no typing, typing is off, or it has finished typing. */
    public Typing plan(FontRenderer font, String text, Layout layout, float x, float y, double seconds) {
        if (stillDepth > 0 || !enabled.getAsBoolean() || !typed(layout)) return null;
        Appearance appearance = appearances.get(text);
        if (appearance == null) {
            appearance = new Appearance();
            appearances.put(text, appearance);
        }
        if (appearance.lastFrame < frame - FORGET_AFTER_FRAMES) appearance.firstSeen = seconds;
        appearance.lastFrame = frame;
        double start = appearance.firstSeen;
        if (chain.continues(frame, font, x, y)) start = Math.max(start, chain.end);
        Typing typing = schedule(layout, seconds - start, seconds);
        chain.extend(frame, font, x, y, start + typing.duration());
        return seconds - start >= typing.duration() ? null : typing;
    }

    private static boolean typed(Layout layout) {
        for (Glyph glyph : layout.glyphs()) {
            if (glyph.pause() > 0 || glyph.style() != null && glyph.style()
                .typing() > 0) return true;
        }
        return false;
    }

    private static Typing schedule(Layout layout, double elapsed, double seconds) {
        List<DrawRun> runs = layout.runs();
        int[] counts = new int[runs.size()];
        List<int[]> lines = lines(runs);
        double lineStart = 0;
        int caretRun = -1;
        boolean caretPausing = false;
        boolean anyCaret = false;
        int waitingRun = -1;
        boolean previousPausing = false;
        for (int[] line : lines) {
            double time = elapsed - lineStart;
            double clock = 0;
            boolean blocked = false;
            boolean pausing = false;
            for (int index = line[0]; index < line[1]; index++) {
                DrawRun run = runs.get(index);
                float rate = run.style() == null ? 0
                    : run.style()
                        .typing();
                List<Glyph> glyphs = run.glyphs();
                int count = -1;
                for (int i = 0; i < glyphs.size(); i++) {
                    Glyph glyph = glyphs.get(i);
                    if (glyph.pause() > 0) {
                        if (!blocked && time >= clock && time < clock + glyph.pause()) pausing = true;
                        clock += glyph.pause();
                        continue;
                    }
                    if (glyph.width() <= 0) continue;
                    if (blocked || time < clock) {
                        if (!blocked && time >= 0) {
                            caretRun = index;
                            caretPausing = pausing;
                            anyCaret = true;
                        }
                        if (count < 0) count = i;
                        blocked = true;
                    }
                    if (rate > 0) clock += 1 / rate;
                }
                counts[index] = count;
            }
            if (!anyCaret && waitingRun < 0 && time < 0 && hasVisibleGlyph(runs, line)) {
                waitingRun = line[0];
                caretPausing = previousPausing;
            }
            previousPausing = pausing && !blocked;
            lineStart += clock;
        }
        if (!anyCaret && waitingRun >= 0) caretRun = waitingRun;
        boolean caretVisible = caretRun >= 0
            && (!caretPausing || Math.floorMod((long) Math.floor(seconds / CARET_BLINK_SECONDS), 2) == 0);
        return new Typing(counts, caretVisible ? caretRun : -1, lineStart);
    }

    private static List<int[]> lines(List<DrawRun> runs) {
        List<int[]> lines = new ObjectArrayList<>();
        for (int index = 0; index < runs.size(); index++) {
            int[] last = lines.isEmpty() ? null : lines.get(lines.size() - 1);
            if (last != null && runs.get(last[0])
                .y()
                == runs.get(index)
                    .y())
                last[1] = index + 1;
            else lines.add(new int[] { index, index + 1 });
        }
        return lines;
    }

    private static boolean hasVisibleGlyph(List<DrawRun> runs, int[] line) {
        for (int index = line[0]; index < line[1]; index++) {
            for (Glyph glyph : runs.get(index)
                .glyphs()) {
                if (glyph.width() > 0) return true;
            }
        }
        return false;
    }

    /**
     * {@code counts[i]} is how many glyphs of run {@code i} show, or -1 when all of them do. {@code caretRun} is the
     * run whose next glyph slot holds the visible caret, or -1 when no caret shows.
     */
    public record Typing(int[] counts, int caretRun, double duration) {

        public int count(int run) {
            return counts[run];
        }
    }

    private static class Appearance {

        private double firstSeen;
        private long lastFrame = Long.MIN_VALUE / 2;
    }

    private static class Chain {

        private long frame = -1;
        private FontRenderer font;
        private float x;
        private float y;
        private double end;

        private boolean continues(long currentFrame, FontRenderer currentFont, float currentX, float currentY) {
            return frame == currentFrame && font == currentFont && Math.abs(x - currentX) < 0.5f && currentY > y;
        }

        private void extend(long currentFrame, FontRenderer currentFont, float currentX, float currentY,
            double typedUntil) {
            frame = currentFrame;
            font = currentFont;
            x = currentX;
            y = currentY;
            end = typedUntil;
        }
    }
}
