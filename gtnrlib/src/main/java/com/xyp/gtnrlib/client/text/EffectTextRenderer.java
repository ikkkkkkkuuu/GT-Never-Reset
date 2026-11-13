package com.xyp.gtnrlib.client.text;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.gtnewhorizon.gtnhlib.util.font.FontRendering;
import com.gtnewhorizon.gtnhlib.util.font.IFontParameters;
import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.client.text.EffectTextLayout.DrawRun;
import com.xyp.gtnrlib.client.text.EffectTextLayout.Glyph;
import com.xyp.gtnrlib.client.text.EffectTextLayout.Layout;
import com.xyp.gtnrlib.client.text.TextMaskCache.Mask;
import com.xyp.gtnrlib.client.text.TypewriterTimeline.GlyphEntrance;
import com.xyp.gtnrlib.client.text.TypewriterTimeline.Typing;
import com.xyp.gtnrlib.client.text.compat.AngelicaTextAdapter;
import com.xyp.gtnrlib.client.text.compat.AngelicaTextAdapter.FontSettings;
import com.xyp.gtnrlib.client.text.compat.FontBatchBridge;
import com.xyp.gtnrlib.client.text.effect.ShaderTextEffect;
import com.xyp.gtnrlib.text.EffectTextParser;
import com.xyp.gtnrlib.text.RollMode;
import com.xyp.gtnrlib.text.ShakeMode;
import com.xyp.gtnrlib.text.TextEffectColors;
import com.xyp.gtnrlib.text.TextEffectFormat;
import com.xyp.gtnrlib.text.TextEffectStyle;
import com.xyp.gtnrlib.text.TextEffectStyle.Roll;
import com.xyp.gtnrlib.text.TextEffectStyle.Shake;
import com.xyp.gtnrlib.text.TextEntrance;
import com.xyp.gtnrlib.utils.ColorMath;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/** Draws marked spans while leaving glyph generation to the current font. */
public class EffectTextRenderer implements IResourceManagerReloadListener {

    public static final EffectTextRenderer INSTANCE = new EffectTextRenderer();
    private static final Logger LOGGER = LogManager.getLogger("GTNRTextEffects");
    private static final long START_TIME = System.nanoTime();
    private static final String CARET = "_";
    private static final String PLAIN_MASK_ID = GTNRLib.MODID + ":plain_mask";
    private static final TextEffectStyle PLAIN_MASK_STYLE = new TextEffectStyle(PLAIN_MASK_ID, IntList.of(), 1);
    private static final float DECODE_SETTLES_AT = 0.75f;
    private static final double DECODE_FLICKER_HZ = 20;
    private static final String DECODE_LATIN = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ#$%&*?";
    // A small fixed set keeps the stand-ins' effect masks cached instead of new framebuffers every flicker.
    private static final String DECODE_CJK = "数据接入网络核心能量矩阵量子超频电压并行线圈熔炉光谱信号频道样板";
    private static final char CJK_START = '⺀';
    private static final float DROP_HEIGHT = 4;
    public static final int MIN_FONT_ALPHA = 4;
    private static final float SLAM_FADE_IN = 3;
    private static final float SLAM_FLASH_FADE = 1.6f;
    private static final float SLAM_FLASH_LIGHTEN = 0.5f;
    private static final float ROLL_FADE_UNITS = 3;
    private static final int SHAKE_GRID = 2;
    private static int captureDepth;
    private static BooleanSupplier enabled = () -> true;
    private static int nativeDepth;
    private final TextMaskCache masks = new TextMaskCache();
    private final Map<LayoutKey, Layout> layouts = new LinkedHashMap<>(32, 0.75f, true);
    private final Set<String> failed = new ObjectOpenHashSet<>();
    private final ShaderTextEffect plainMask = new ShaderTextEffect(
        new ResourceLocation(GTNRLib.MODID, "shaders/text/plain_mask.frag.glsl"),
        TextEffectColors.rgb(TextEffectColors.effectWhite));

    public static void enabledWith(BooleanSupplier setting) {
        enabled = setting;
    }

    public static boolean isCapturing() {
        return captureDepth > 0;
    }

    public static void beginCapture() {
        captureDepth++;
    }

    public static void endCapture() {
        captureDepth--;
    }

    public static boolean handles(String text) {
        return !isBypassingEffects() && ownsText(text);
    }

    private static boolean ownsText(String text) {
        if (text == null) return false;
        if (text.contains(EffectTextParser.PREFIX) || text.contains(EffectTextParser.CLOSE)) return true;
        if (!EffectTextParser.containsMarkers(text)) return false;
        EffectTextParser.Cursor cursor = new EffectTextParser.Cursor(
            text,
            java.util.function.UnaryOperator.identity(),
            false,
            EffectTextRenderer::ownsRenderer);
        for (EffectTextParser.Token token; (token = cursor.next()) != null;) {
            if (token.pause() > 0) return true;
            if (token.style() != null && ownsRenderer(
                token.style()
                    .rendererId())) return true;
        }
        return false;
    }

    private static boolean ownsRenderer(String identifier) {
        return TextEffectRegistry.get(identifier) != null || TextEffectFormat.isPlainRenderer(identifier);
    }

    public static boolean isBypassingEffects() {
        return isCapturing() || nativeDepth > 0;
    }

    // The part of string between offset and offset + length, or null when it has no effect markers to draw.
    public static String markedText(CharSequence string, int offset, int length) {
        if (isBypassingEffects() || string == null) return null;
        int start = Math.max(0, Math.min(offset, string.length()));
        int end = start + Math.max(0, Math.min(length, string.length() - start));
        if (!ownsText(
            string.subSequence(start, end)
                .toString()))
            return null;
        return start == 0 && end == string.length() ? string.toString()
            : string.subSequence(start, end)
                .toString();
    }

    public float drawWithUnicodeFlag(FontRenderer font, String text, float x, float y, int color, boolean shadow,
        boolean unicodeFlag) {
        boolean previousUnicode = font.getUnicodeFlag();
        font.setUnicodeFlag(unicodeFlag);
        try {
            draw(font, text, x, y, color, shadow);
            return x + layout(font, text).width();
        } finally {
            font.setUnicodeFlag(previousUnicode);
        }
    }

    public Layout layout(FontRenderer font, String text) {
        IFontParameters parameters = (IFontParameters) font;
        LayoutKey key = new LayoutKey(
            font,
            text,
            font.getUnicodeFlag(),
            font.FONT_HEIGHT,
            parameters.getGlyphScaleX(),
            parameters.getGlyphScaleY(),
            parameters.getGlyphSpacing(),
            parameters.getWhitespaceScale(),
            FontRendering.preprocessText("&q&z&v"),
            FontRendering.hexColorResetsStyles(),
            AngelicaTextAdapter.fontSettings());
        Layout layout = layouts.get(key);
        if (layout == null) {
            layout = EffectTextLayout.create(font, text);
            if (text.length() < 16384) {
                layouts.put(key, layout);
                if (layouts.size() > 256) layouts.remove(
                    layouts.keySet()
                        .iterator()
                        .next());
            }
        }
        return layout;
    }

    public void drawStyledLine(FontRenderer font, String text, TextEffectStyle style, float x, float y) {
        if (!enabled.getAsBoolean()) {
            drawPlain(font, text, x, y, 0xFFFFFFFF, true);
            return;
        }
        FontBatchBridge bridge = AngelicaTextAdapter.bridge(font);
        int depth = bridge == null ? 0 : bridge.gtnr$suspendBatch();
        try {
            AngelicaTextAdapter.beginForeignDraw();
            drawRun(
                font,
                text,
                style,
                x,
                y,
                font.getStringWidth(text),
                EffectTextLayout.fontHeight(font),
                0xFFFFFFFF,
                true);
        } finally {
            try {
                AngelicaTextAdapter.endForeignDraw();
            } finally {
                if (bridge != null) bridge.gtnr$resumeBatch(depth);
            }
        }
    }

    public int draw(FontRenderer font, String text, float x, float y, int color, boolean shadow) {
        if (text == null) return 0;
        if ((color & 0xFC000000) == 0) color |= 0xFF000000;
        Layout layout = layout(font, text);
        boolean styled = false;
        for (DrawRun run : layout.runs()) {
            if (run.style() != null) {
                styled = true;
                break;
            }
        }
        if (!styled || !enabled.getAsBoolean()) {
            // Plain rendering keeps parsed text visible without touching deferred font or model batches.
            drawRuns(
                font,
                layout,
                TypewriterTimeline.INSTANCE.plan(font, text, layout, x, y, seconds()),
                x,
                y,
                color,
                shadow,
                false);
            return endX(font, layout, x, shadow);
        }
        if (!DeferredTextEffects.isFlushing() && AngelicaTextAdapter.shouldDeferEffects()) {
            DeferredTextEffects.enqueue(font, text, x, y, color, shadow);
            return endX(font, layout, x, shadow);
        }
        FontBatchBridge bridge = AngelicaTextAdapter.bridge(font);
        int depth = bridge == null ? 0 : bridge.gtnr$suspendBatch();
        try {
            AngelicaTextAdapter.beginForeignDraw();
            return drawLayout(
                font,
                layout,
                TypewriterTimeline.INSTANCE.plan(font, text, layout, x, y, seconds()),
                x,
                y,
                color,
                shadow);
        } finally {
            try {
                AngelicaTextAdapter.endForeignDraw();
            } finally {
                if (bridge != null) bridge.gtnr$resumeBatch(depth);
            }
        }
    }

    private int drawLayout(FontRenderer font, Layout layout, Typing typing, float x, float y, int color,
        boolean shadow) {
        try (TextRenderState ignored = new TextRenderState()) {
            drawRuns(font, layout, typing, x, y, color, shadow, true);
        }
        return endX(font, layout, x, shadow);
    }

    private void drawRuns(FontRenderer font, Layout layout, Typing typing, float x, float y, int color, boolean shadow,
        boolean effects) {
        float[] advances = new float[layout.runs()
            .size()];
        for (int i = 0; i < advances.length; i++) {
            DrawRun run = layout.runs()
                .get(i);
            int count = typing == null ? -1 : typing.count(i);
            if (count == 0) continue;
            float runX = x + run.x();
            float runY = y + run.y();
            if (typing != null && typing.roll(i) >= 0) {
                drawRolling(font, run, typing.roll(i), runX, runY, layout.height(), color, shadow, effects);
                continue;
            }
            GlyphEntrance[] entering = typing == null ? null : typing.entrances(i);
            int settled = entering != null ? entering[0].index() : count;
            // A partly typed run uses a mask of its typed glyphs alone, so hidden glyphs cannot bleed outlines in.
            float advance = settled == 0 ? 0
                : paintRun(font, run, settled, runX, runY, layout.height(), color, shadow, effects);
            if (entering != null) for (GlyphEntrance entrance : entering) advance += drawEntrance(
                font,
                run,
                entrance,
                runX + advance,
                runY,
                layout.height(),
                color,
                shadow,
                effects);
            advances[i] = advance;
        }
        drawCaret(font, layout, typing, advances, x, y, color, shadow);
    }

    private float paint(FontRenderer font, String text, TextEffectStyle style, float x, float y, float width,
        float height, int color, boolean shadow, boolean effects) {
        return effects ? drawRun(font, text, style, x, y, width, height, color, shadow)
            : drawPlain(font, text, x, y, color, shadow);
    }

    private float paintRun(FontRenderer font, DrawRun run, int count, float x, float y, float height, int color,
        boolean shadow, boolean effects) {
        TextEffectStyle style = run.style();
        String text = count < 0 ? run.text() : run.typedText(count);
        Shake shake = shakeOf(style);
        if (shake.pixels() == 0 || !effects)
            return paint(font, text, style, x, y, run.width(), height, color, shadow, effects);
        if (shake.mode() == ShakeMode.SYNC) return paint(
            font,
            text,
            style,
            x + TextShake.offset(shake, 0, 0, seconds()),
            y + TextShake.offset(shake, 0, 1, seconds()),
            run.width(),
            height,
            color,
            shadow,
            effects);
        List<Glyph> glyphs = run.glyphs()
            .subList(
                0,
                count < 0 ? run.glyphs()
                    .size() : count);
        int visible = 0;
        for (Glyph glyph : glyphs) if (!glyph.text()
            .isEmpty()) visible++;
        String[] texts = new String[visible];
        float[] dx = new float[visible];
        float[] dy = new float[visible];
        for (int i = 0, slot = 0; i < glyphs.size(); i++) {
            Glyph glyph = glyphs.get(i);
            if (glyph.text()
                .isEmpty()) continue;
            texts[slot] = glyph.text();
            dx[slot] = shakeStep(shake, i, 0);
            dy[slot] = shakeStep(shake, i, 1);
            slot++;
        }
        String formatting = run.formatting();
        TextEffect effect = TextEffectRegistry.get(style.rendererId());
        if (effect != null && supportsMasks(font) && !failed.contains(style.rendererId())) {
            try (TextRenderState ignored = new TextRenderState()) {
                float padding = Math.max(height, effect.padding(run.width(), height));
                Mask mask = masks.getShaken(font, formatting, text, texts, dx, dy, run.width(), height, padding);
                if (mask != null) return renderMask(font, effect, mask, style, x, y, color, shadow);
            } catch (RuntimeException exception) {
                markFailed(style.rendererId(), exception);
            }
        }
        int plainColor = fallbackColor(style, effect, color);
        float pen = 0;
        for (int i = 0; i < texts.length; i++)
            pen += drawPlain(font, formatting + texts[i], x + pen + dx[i], y + dy[i], plainColor, shadow);
        return pen;
    }

    private static Shake shakeOf(TextEffectStyle style) {
        return style == null ? Shake.NONE : style.shake();
    }

    private static float shakeStep(Shake shake, int glyph, int axis) {
        return TextShake.toGrid(TextShake.offset(shake, glyph, axis, seconds()), SHAKE_GRID);
    }

    private float drawEntrance(FontRenderer font, DrawRun run, GlyphEntrance entrance, float x, float y, float height,
        int color, boolean shadow, boolean effects) {
        Glyph glyph = run.glyphs()
            .get(entrance.index());
        float progress = entrance.progress();
        TextEntrance kind = entrance.kind();
        boolean slam = kind == TextEntrance.SLAM;
        boolean standIn = kind == TextEntrance.DECODE && progress < DECODE_SETTLES_AT;
        String shown = standIn ? decodeGlyph(glyph.text(), entrance.index()) : glyph.text();
        float dy = kind == TextEntrance.DROP ? -DROP_HEIGHT * (1 - progress) * (1 - progress) : 0;
        float opacity = kind == TextEntrance.FADE ? progress : slam ? Math.min(1, progress * SLAM_FADE_IN) : 1;
        int alpha = Math.round((color >>> 24) * opacity);
        int flashAlpha = slam ? Math.round((color >>> 24) * Math.max(0, 1 - progress * SLAM_FLASH_FADE)) : 0;
        // Vanilla draws any alpha below 4 fully opaque, so the faintest frames are skipped instead.
        if (alpha < MIN_FONT_ALPHA && flashAlpha < MIN_FONT_ALPHA) return glyph.width();
        TextEffectStyle style = run.style();
        float glyphX = x + (effects ? shakeStep(shakeOf(style), entrance.index(), 0) : 0);
        float glyphY = y + dy + (effects ? shakeStep(shakeOf(style), entrance.index(), 1) : 0);
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_ENABLE_BIT);
        try {
            if (slam) {
                float landed = 1 - (1 - progress) * (1 - progress) * (1 - progress);
                float grow = 1 + (entrance.size() - 1) * (1 - landed);
                float centerX = glyphX + glyph.width() / 2f;
                float centerY = glyphY + height / 2f;
                GL11.glTranslatef(centerX, centerY, 0);
                GL11.glScalef(grow, grow, 1);
                GL11.glTranslatef(-centerX, -centerY, 0);
            }
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper
                .glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            float drawn = alpha < MIN_FONT_ALPHA ? glyph.width()
                : paint(
                    font,
                    glyph.formatting() + shown,
                    style,
                    glyphX,
                    glyphY,
                    glyph.width(),
                    height,
                    alpha << 24 | color & 0xFFFFFF,
                    shadow,
                    effects);
            if (flashAlpha >= MIN_FONT_ALPHA) {
                TextEffect effect = style == null ? null : TextEffectRegistry.get(style.rendererId());
                int own = effects ? fallbackColor(style, effect, color) & 0xFFFFFF
                    : plainColor(glyph.formatting(), color);
                drawPlain(
                    font,
                    withoutColors(glyph.formatting()) + shown,
                    glyphX,
                    glyphY,
                    flashAlpha << 24 | ColorMath.mixWhite(own, SLAM_FLASH_LIGHTEN),
                    false);
            }
            return standIn || slam ? glyph.width() : drawn;
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private static String withoutColors(String formatting) {
        StringBuilder kept = new StringBuilder();
        for (int i = 0; i < formatting.length();) {
            int length = Math.max(1, TextEffectFormat.nativeLength(formatting, i));
            boolean color = length == 14 || length == 30
                || length == 2 && Character.digit(formatting.charAt(i + 1), 16) >= 0;
            if (!color) kept.append(formatting, i, i + length);
            i += length;
        }
        return kept.toString();
    }

    private static String decodeGlyph(String text, int index) {
        long step = (long) Math.floor(seconds() * DECODE_FLICKER_HZ);
        String pool = !text.isEmpty() && text.charAt(0) >= CJK_START ? DECODE_CJK : DECODE_LATIN;
        return String.valueOf(pool.charAt((int) Math.floorMod(step * 7 + index * 13L, (long) pool.length())));
    }

    private void drawRolling(FontRenderer font, DrawRun run, float progress, float x, float y, float height, int color,
        boolean shadow, boolean effects) {
        TextEffectStyle style = run.style();
        Roll roll = style.roll();
        TextEffect effect = TextEffectRegistry.get(style.rendererId());
        RollMode mode = roll.mode() == RollMode.AUTO ? effect != null ? RollMode.JUMP : RollMode.CUT : roll.mode();
        String formatting = run.formatting();
        boolean slides = mode != RollMode.JUMP && effects && supportsMasks(font) && !failed.contains(PLAIN_MASK_ID);
        NumberRoll.Frame frame = NumberRoll
            .frame(run.glyphs(), progress, roll.seconds(), !slides, roll.slot(), roll.blur());
        if (!slides) {
            paint(font, formatting + frame.jumpText(), style, x, y, run.width(), height, color, shadow, effects);
            return;
        }
        TextEffect painter = effect != null && !failed.contains(style.rendererId()) ? effect : plainMask;
        TextEffectStyle painted = painter == effect ? style
            : PLAIN_MASK_STYLE.withColors(plainColor(formatting, color));
        try (TextRenderState ignored = new TextRenderState()) {
            float padding = Math.max(height, painter.padding(run.width(), height));
            Mask mask = masks.getRolled(
                font,
                formatting,
                run.text(),
                frame,
                run.width(),
                height,
                padding,
                mode == RollMode.FADE ? ROLL_FADE_UNITS : 0);
            if (mask == null) {
                paint(font, formatting + frame.jumpText(), style, x, y, run.width(), height, color, shadow, effects);
                return;
            }
            renderMask(font, painter, mask, painted, x, y, color, shadow);
        } catch (RuntimeException exception) {
            markFailed(painted.rendererId(), exception);
            paint(font, formatting + frame.jumpText(), style, x, y, run.width(), height, color, shadow, effects);
        }
    }

    private static boolean supportsMasks(FontRenderer font) {
        return AngelicaTextAdapter.supportsEffects(font) && OpenGlHelper.isFramebufferEnabled()
            && GLContext.getCapabilities().OpenGL20;
    }

    private static int plainColor(String formatting, int color) {
        TextEffectStyle current = PLAIN_MASK_STYLE;
        for (int i = 0; i < formatting.length();) {
            int length = Math.max(1, TextEffectFormat.nativeLength(formatting, i));
            if (length > 1) current = TextEffectFormat
                .applyColor(current == null ? PLAIN_MASK_STYLE : current, formatting.substring(i, i + length));
            i += length;
        }
        return current == null || current.colors()
            .isEmpty() ? color & 0xFFFFFF
                : current.colors()
                    .getInt(0);
    }

    // Vanilla measures unicode glyphs wider than it draws them, so the caret uses the drawn advance of the typed text.
    private void drawCaret(FontRenderer font, Layout layout, Typing typing, float[] advances, float x, float y,
        int color, boolean shadow) {
        if (typing == null || typing.caretRun() < 0) return;
        DrawRun run = layout.runs()
            .get(typing.caretRun());
        String formatting = run.formatting()
            .replace("§k", "")
            .replace("§K", "");
        String caret = formatting + CARET;
        float slot = run.x() + advances[typing.caretRun()];
        if (slot + layout(font, caret).width() > layout.width()) return;
        TextEffect effect = run.style() == null ? null
            : TextEffectRegistry.get(
                run.style()
                    .rendererId());
        drawPlain(font, caret, x + slot, y + run.y(), fallbackColor(run.style(), effect, color), shadow);
    }

    private static int endX(FontRenderer font, Layout layout, float x, boolean shadow) {
        return (int) Math.ceil(x + layout.width() + (shadow ? ((IFontParameters) font).getShadowOffset() : 0));
    }

    private float drawRun(FontRenderer font, String text, TextEffectStyle style, float x, float y, float width,
        float height, int color, boolean shadow) {
        TextEffect effect = style == null ? null : TextEffectRegistry.get(style.rendererId());
        if (effect == null || width <= 0 || failed.contains(style.rendererId()) || !supportsMasks(font)) {
            return drawPlain(font, text, x, y, fallbackColor(style, effect, color), shadow);
        }
        try (TextRenderState ignored = new TextRenderState()) {
            float padding = Math.max(height, effect.padding(width, height));
            Mask mask = masks.get(font, text, width, height, padding);
            if (mask == null) return drawPlain(font, text, x, y, fallbackColor(style, effect, color), shadow);
            return renderMask(font, effect, mask, style, x, y, color, shadow);
        } catch (RuntimeException exception) {
            markFailed(style.rendererId(), exception);
            return drawPlain(font, text, x, y, fallbackColor(style, effect, color), shadow);
        }
    }

    private static float renderMask(FontRenderer font, TextEffect effect, Mask mask, TextEffectStyle style, float x,
        float y, int color, boolean shadow) {
        double seconds = seconds();
        if (shadow) {
            float offset = ((IFontParameters) font).getShadowOffset();
            effect.render(new TextRenderContext(mask, style, x + offset, y + offset, color, seconds, true));
        }
        effect.render(new TextRenderContext(mask, style, x, y, color, seconds, false));
        return mask.advance();
    }

    private void markFailed(String rendererId, RuntimeException exception) {
        if (failed.add(rendererId))
            LOGGER.warn("Text effect unavailable until resource reload: {}", rendererId, exception);
    }

    private static double seconds() {
        return (System.nanoTime() - START_TIME) * 1e-9;
    }

    private static int fallbackColor(TextEffectStyle style, TextEffect effect, int color) {
        if (style == null) return color;
        int rgb = !style.colors()
            .isEmpty() ? style.colors()
                .getInt(0) : effect == null ? color & 0xFFFFFF : effect.fallbackColor();
        return color & 0xFF000000 | rgb;
    }

    private static float drawPlain(FontRenderer font, String text, float x, float y, int color, boolean shadow) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        nativeDepth++;
        try {
            int end = font.drawString(text, 0, 0, color, shadow);
            // Vanilla returns the end of whichever pass reaches further, which is the shadow pass.
            return Math.max(0, end - (shadow ? ((IFontParameters) font).getShadowOffset() : 0));
        } finally {
            nativeDepth--;
            GL11.glPopMatrix();
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        DeferredTextEffects.clear();
        TypewriterTimeline.INSTANCE.clear();
        layouts.clear();
        failed.clear();
        if (GLContext.getCapabilities().OpenGL20) {
            try (TextRenderState ignored = new TextRenderState()) {
                masks.clear();
                TextEffectRegistry.reload();
                plainMask.close();
            }
        }
    }

    public record LayoutKey(FontRenderer font, String text, boolean unicode, int fontHeight, float scaleX, float scaleY,
        float spacing, float whitespace, String preprocessing, boolean hexResetsStyles, FontSettings fontSettings) {

        public LayoutKey(FontRenderer font, String text, boolean unicode, int fontHeight, float scaleX, float scaleY,
            float spacing, float whitespace, String preprocessing, boolean hexResetsStyles) {
            this(
                font,
                text,
                unicode,
                fontHeight,
                scaleX,
                scaleY,
                spacing,
                whitespace,
                preprocessing,
                hexResetsStyles,
                null);
        }
    }
}
