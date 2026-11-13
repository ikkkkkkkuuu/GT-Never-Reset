package com.xyp.gtnrlib.client.text.effect;

import net.minecraft.util.ResourceLocation;

import com.xyp.gtnrlib.client.text.TextRenderContext;
import com.xyp.gtnrlib.text.TextEffectFormat;
import com.xyp.gtnrlib.text.TextEffectStyle;

/**
 * Text written in shifting runes that a wave reads to plain text and back. The style's options set how long the line
 * stays text and runes, how long a pass of the wave takes, how often runes change, and the motes and flickers.
 */
public class ArcaneCipherTextEffect extends ShaderTextEffect {

    public static final float DEFAULT_HOLD = 4;
    public static final float DEFAULT_HIDE = 3;
    public static final float DEFAULT_SWEEP = 1.5f;
    public static final float DEFAULT_FLIP = 5;
    public static final int DEFAULT_SHARDS = 4;
    public static final int DEFAULT_FLICK = 12;

    public ArcaneCipherTextEffect(ResourceLocation fragment, int... colors) {
        super(fragment, colors);
    }

    @Override
    protected void configureUniforms(TextShader shader, TextRenderContext context) {
        TextEffectStyle style = context.style();
        shader.uniform1f("holdSeconds", style.option(TextEffectFormat.HOLD, DEFAULT_HOLD));
        shader.uniform1f("hideSeconds", style.option(TextEffectFormat.HIDE, DEFAULT_HIDE));
        shader.uniform1f("sweepSeconds", style.option(TextEffectFormat.SWEEP, DEFAULT_SWEEP));
        shader.uniform1f("flipRate", style.option(TextEffectFormat.FLIP, DEFAULT_FLIP));
        shader.uniform1f("shardCount", style.option(TextEffectFormat.SHARDS, DEFAULT_SHARDS));
        shader.uniform1f("flickChance", style.option(TextEffectFormat.FLICK, DEFAULT_FLICK) / 100f);
    }
}
