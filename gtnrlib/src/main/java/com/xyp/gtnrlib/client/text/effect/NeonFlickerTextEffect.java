package com.xyp.gtnrlib.client.text.effect;

import net.minecraft.util.ResourceLocation;

import com.xyp.gtnrlib.client.text.TextRenderContext;
import com.xyp.gtnrlib.text.TextEffectFormat;

/** A neon sign whose glyphs fail at random; the style's fault chance picks how many sputter in each round. */
public class NeonFlickerTextEffect extends ShaderTextEffect {

    public static final int DEFAULT_FAULT = 20;

    public NeonFlickerTextEffect(ResourceLocation fragment, int... colors) {
        super(fragment, colors);
    }

    @Override
    protected void configureUniforms(TextShader shader, TextRenderContext context) {
        shader.uniform1f(
            "faultShare",
            context.style()
                .option(TextEffectFormat.FAULT, DEFAULT_FAULT) / 100f);
    }
}
