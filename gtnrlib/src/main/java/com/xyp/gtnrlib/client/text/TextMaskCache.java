package com.xyp.gtnrlib.client.text;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.util.font.IFontParameters;
import com.xyp.gtnrlib.client.text.EffectTextLayout.Glyph;
import com.xyp.gtnrlib.client.text.EffectTextLayout.Layout;
import com.xyp.gtnrlib.client.text.compat.AngelicaTextAdapter;
import com.xyp.gtnrlib.client.text.compat.AngelicaTextAdapter.FontSettings;
import com.xyp.gtnrlib.client.text.compat.GlyphTextureMetrics;
import com.xyp.gtnrlib.text.TextEffectColors;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/** Render-thread-owned, bounded cache of native font coverage masks. */
public class TextMaskCache {

    private static final int MAX_ENTRIES = 256;
    private static final long MAX_PIXELS = 4L * 1024 * 1024;
    private static final int BITMAP_RESOLUTION = 2;
    private static final int CUSTOM_FONT_RESOLUTION = 4;
    private final Map<Key, Mask> masks = new LinkedHashMap<>(32, 0.75f, true);
    private final Object2ObjectOpenHashMap<Key, Object> liveStates = new Object2ObjectOpenHashMap<>();
    private long pixels;

    public Mask get(FontRenderer font, String text, float width, float height, float padding) {
        return obtain(font, text, text, width, height, padding, null, (target, resolution) -> drawWhite(font, text));
    }

    /**
     * Captures a rolling number: every layer is drawn in the run's {@code formatting}, cut to the line and to the
     * run's left edge before any effect sees it, so glow and outlines still spread past the cut. With {@code fade},
     * digits sliding past the line fade out over its last {@code fadeUnits} instead of ending sharply. The mask is
     * redrawn in place whenever the frame changes, so a roll keeps one framebuffer.
     */
    public Mask getRolled(FontRenderer font, String formatting, String metricsText, NumberRoll.Frame frame, float width,
        float height, float padding, float fadeUnits) {
        String key = "\u0002" + formatting + metricsText + '#' + fadeUnits;
        return obtain(font, key, metricsText, width, height, padding, frame.layers(), (target, resolution) -> {
            int left = Math.round(padding * resolution);
            int lineHeight = Math.round(height * resolution);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(
                left,
                target.framebufferHeight - left - lineHeight,
                Math.max(0, target.framebufferWidth - left),
                lineHeight);
            paintLayers(font, formatting, frame, height, true);
            if (fadeUnits > 0) fadeLineEdges(width, height, padding, fadeUnits);
            paintLayers(font, formatting, frame, height, false);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            return Math.round(width);
        });
    }

    /**
     * Captures text whose glyphs tremble on their own: glyph {@code i} is drawn {@code dx[i]}, {@code dy[i]} away from
     * where the font would put it, each glyph starting where the previous one ended. The mask is redrawn in place
     * whenever the offsets change, so a shaking span keeps one framebuffer.
     */
    public Mask getShaken(FontRenderer font, String formatting, String metricsText, String[] glyphs, float[] dx,
        float[] dy, float width, float height, float padding) {
        String key = "\u0003" + formatting + metricsText;
        List<FloatList> offsets = List.of(FloatArrayList.wrap(dx), FloatArrayList.wrap(dy));
        return obtain(font, key, metricsText, width, height, padding, offsets, (target, resolution) -> {
            int pen = 0;
            for (int i = 0; i < glyphs.length; i++)
                pen += drawWhite(font, formatting + glyphs[i], pen + dx[i], dy[i], 1);
            return pen;
        });
    }

    private static void paintLayers(FontRenderer font, String formatting, NumberRoll.Frame frame, float height,
        boolean moving) {
        for (NumberRoll.Layer layer : frame.layers()) {
            if (layer.moving() == moving && layer.alpha() * 255 >= EffectTextRenderer.MIN_FONT_ALPHA)
                drawWhite(font, formatting + layer.text(), layer.x(), layer.shift() * height, layer.alpha());
        }
    }

    private static int drawWhite(FontRenderer font, String text) {
        return drawWhite(font, text, 1);
    }

    private static int drawWhite(FontRenderer font, String text, float alpha) {
        int white = TextEffectColors.opaqueWhite.getColor();
        return font.drawString(text, 0, 0, Math.round((white >>> 24) * alpha) << 24 | white & 0xFFFFFF, false);
    }

    private static int drawWhite(FontRenderer font, String text, float x, float y, float alpha) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        try {
            return drawWhite(font, text, alpha);
        } finally {
            GL11.glPopMatrix();
        }
    }

    // Multiplies what is already drawn by a ramp near the line's top and bottom; only moving digits are drawn yet.
    private static void fadeLineEdges(float width, float height, float padding, float fadeUnits) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.glBlendFunc(GL11.GL_ZERO, GL11.GL_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_SRC_ALPHA);
        float left = -padding;
        float right = width + padding;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(1, 1, 1, 0);
        GL11.glVertex2f(left, 0);
        GL11.glVertex2f(right, 0);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glVertex2f(right, fadeUnits);
        GL11.glVertex2f(left, fadeUnits);
        GL11.glVertex2f(left, height - fadeUnits);
        GL11.glVertex2f(right, height - fadeUnits);
        GL11.glColor4f(1, 1, 1, 0);
        GL11.glVertex2f(right, height);
        GL11.glVertex2f(left, height);
        GL11.glEnd();
        GL11.glColor4f(1, 1, 1, 1);
        OpenGlHelper
            .glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private Mask obtain(FontRenderer font, String keyText, String metricsText, float width, float height, float padding,
        Object state, Painter painter) {
        IFontParameters parameters = (IFontParameters) font;
        boolean smooth = AngelicaTextAdapter.usesCustomFont(font);
        int resolution = smooth ? CUSTOM_FONT_RESOLUTION : BITMAP_RESOLUTION;
        Key key = new Key(
            font,
            keyText,
            width,
            height,
            padding,
            font.getUnicodeFlag(),
            parameters.getGlyphScaleX(),
            parameters.getGlyphScaleY(),
            parameters.getGlyphSpacing(),
            parameters.getShadowOffset(),
            smooth,
            AngelicaTextAdapter.fontSettings());
        Mask present = masks.get(key);
        if (present != null) {
            boolean stale = state != null && !state.equals(liveStates.get(key));
            if (!stale && !hasAnimatedGlyphs(metricsText)) return present;
            Mask redrawn = capture(
                font,
                metricsText,
                width,
                height,
                padding,
                present.target(),
                resolution,
                smooth,
                painter);
            masks.put(key, redrawn);
            if (state != null) liveStates.put(key, state);
            return redrawn;
        }
        int pixelWidth = Math.max(1, (int) Math.ceil((width + padding * 2) * resolution));
        int pixelHeight = Math.max(1, (int) Math.ceil((height + padding * 2) * resolution));
        int maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        if (pixelWidth > maxTextureSize || pixelHeight > maxTextureSize || (long) pixelWidth * pixelHeight > MAX_PIXELS)
            return null;
        Mask mask;
        try (TextRenderState ignored = new TextRenderState()) {
            Framebuffer target = new Framebuffer(pixelWidth, pixelHeight, false);
            try {
                mask = capture(font, metricsText, width, height, padding, target, resolution, smooth, painter);
            } catch (RuntimeException exception) {
                target.deleteFramebuffer();
                throw exception;
            }
        }
        masks.put(key, mask);
        if (state != null) liveStates.put(key, state);
        pixels += mask.pixels();
        Iterator<Entry<Key, Mask>> iterator = masks.entrySet()
            .iterator();
        if (masks.size() > MAX_ENTRIES || pixels > MAX_PIXELS) {
            try (TextRenderState ignored = new TextRenderState()) {
                while (masks.size() > MAX_ENTRIES || pixels > MAX_PIXELS) {
                    var entry = iterator.next();
                    Mask removed = entry.getValue();
                    liveStates.remove(entry.getKey());
                    iterator.remove();
                    pixels -= removed.pixels();
                    removed.target()
                        .deleteFramebuffer();
                }
            }
        }
        return mask;
    }

    private static boolean hasAnimatedGlyphs(String text) {
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) == '\u00a7') {
                char code = Character.toLowerCase(text.charAt(++i));
                if ("0123456789abcdeflmnorgx".indexOf(code) < 0) return true;
            }
        }
        return false;
    }

    private Mask capture(FontRenderer font, String text, float width, float height, float padding, Framebuffer target,
        int resolution, boolean smooth, Painter painter) {
        try (TextRenderState ignored = new TextRenderState()) {
            target.setFramebufferColor(0, 0, 0, 0);
            target.setFramebufferFilter(smooth ? GL11.GL_LINEAR : GL11.GL_NEAREST);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            target.framebufferClear();
            target.bindFramebuffer(true);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(
                0,
                target.framebufferWidth / (float) resolution,
                target.framebufferHeight / (float) resolution,
                0,
                -100,
                100);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glTranslatef(padding, padding, 0);
            GL20.glUseProgram(0);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper
                .glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            int advance;
            Layout layout = EffectTextLayout.create(font, text);
            int[] starts;
            EffectTextRenderer.beginCapture();
            try {
                advance = painter.paint(target, resolution);
                starts = drawnStarts(font, layout);
            } finally {
                EffectTextRenderer.endCapture();
            }
            writeGlyphTextureMetrics(font, layout, starts, width, height, padding, resolution);
            return new Mask(target, width, height, padding, resolution, advance);
        }
    }

    // Vanilla measures unicode glyphs wider than it draws them, so glyph columns follow where each glyph was drawn.
    // Each glyph is drawn alone with color writes off; vanilla truncates every glyph's advance, so the sum matches.
    private static int[] drawnStarts(FontRenderer font, Layout layout) {
        int[] starts = new int[layout.glyphs()
            .size() + 1];
        GL11.glColorMask(false, false, false, false);
        try {
            int pen = 0;
            for (int i = 0; i < layout.glyphs()
                .size(); i++) {
                Glyph glyph = layout.glyphs()
                    .get(i);
                starts[i] = pen;
                pen += drawWhite(font, glyph.formatting() + glyph.text());
            }
            starts[starts.length - 1] = pen;
        } finally {
            GL11.glColorMask(true, true, true, true);
        }
        return starts;
    }

    private static void writeGlyphTextureMetrics(FontRenderer font, Layout layout, int[] starts, float width,
        float height, float padding, int resolution) {
        GlyphTextureMetrics[] metrics = new GlyphTextureMetrics[layout.glyphs()
            .size()];
        for (int i = 0; i < metrics.length; i++) {
            metrics[i] = GlyphTextureMetrics.of(
                font,
                layout.glyphs()
                    .get(i)
                    .text()
                    .charAt(0));
        }
        GL20.glUseProgram(0);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_FOG);
        // RGB carries the atlas interval and horizontal glyph center; alpha retains font coverage.
        GL11.glColorMask(true, true, true, false);
        int index = 0;
        GL11.glBegin(GL11.GL_QUADS);
        for (Glyph glyph : layout.glyphs()) {
            float cursor = starts[index];
            float drawn = glyph.width() > 0 ? Math.max(0, starts[index + 1] - cursor - layout.spacing()) : 0;
            float left = index == 0 ? -padding : cursor - layout.spacing() * 0.5f;
            float right = cursor + drawn + layout.spacing() * 0.5f;
            if (index == layout.glyphs()
                .size() - 1) right = width + padding;
            float center = (cursor + drawn * 0.5f) / Math.max(1, width);
            GL11.glColor3f(metrics[index].vStart(), metrics[index].vSpan(), center);
            GL11.glVertex2f(left, -padding);
            GL11.glVertex2f(left, height + padding);
            GL11.glVertex2f(right, height + padding);
            GL11.glVertex2f(right, -padding);
            // The top metadata row also retains glyph starts for source renderers using left-edge phase.
            GL11.glColor3f(metrics[index].vStart(), metrics[index].vSpan(), cursor / Math.max(1, width));
            GL11.glVertex2f(left, -padding);
            GL11.glVertex2f(left, -padding + 1f / resolution);
            GL11.glVertex2f(right, -padding + 1f / resolution);
            GL11.glVertex2f(right, -padding);
            // The second row keeps each glyph's drawn width over 16 units and its kind: blank, punctuation or letter.
            GL11.glColor3f(Math.min(1, drawn / 16), 0, glyphKind(glyph, drawn));
            GL11.glVertex2f(left, -padding + 1f / resolution);
            GL11.glVertex2f(left, -padding + 2f / resolution);
            GL11.glVertex2f(right, -padding + 2f / resolution);
            GL11.glVertex2f(right, -padding + 1f / resolution);
            index++;
        }
        GL11.glEnd();
    }

    private static float glyphKind(Glyph glyph, float drawn) {
        if (glyph.text()
            .isBlank() || drawn <= 0) return 0;
        return isPunctuation(
            glyph.text()
                .codePointAt(0)) ? 0.5f : 1;
    }

    private static boolean isPunctuation(int codePoint) {
        return switch (Character.getType(codePoint)) {
            case Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION, Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION, Character.OTHER_PUNCTUATION -> true;
            default -> false;
        };
    }

    public void clear() {
        if (!masks.isEmpty()) {
            try (TextRenderState ignored = new TextRenderState()) {
                for (Mask mask : masks.values()) mask.target()
                    .deleteFramebuffer();
            }
        }
        masks.clear();
        liveStates.clear();
        pixels = 0;
    }

    private interface Painter {

        /**
         * Draws a mask's coverage into the bound framebuffer, already positioned at the text origin; returns its
         * advance.
         */
        int paint(Framebuffer target, int resolution);
    }

    public record Key(FontRenderer font, String text, float width, float height, float padding, boolean unicode,
        float scaleX, float scaleY, float spacing, float shadowOffset, boolean smooth, FontSettings fontSettings) {

        public Key(FontRenderer font, String text, float width, float height, float padding, boolean unicode,
            float scaleX, float scaleY, float spacing, float shadowOffset) {
            this(font, text, width, height, padding, unicode, scaleX, scaleY, spacing, shadowOffset, false, null);
        }
    }

    /** {@code advance} is how far the font moved its pen while drawing the text, which may differ from its measure. */
    public record Mask(Framebuffer target, float width, float height, float padding, int resolution, float advance) {

        public Mask(Framebuffer target, float width, float height, float padding, int resolution) {
            this(target, width, height, padding, resolution, width);
        }

        public long pixels() {
            return (long) target.framebufferTextureWidth * target.framebufferTextureHeight;
        }

        public float textureWidth() {
            return target.framebufferTextureWidth / (float) resolution;
        }

        public float textureHeight() {
            return target.framebufferTextureHeight / (float) resolution;
        }
    }
}
