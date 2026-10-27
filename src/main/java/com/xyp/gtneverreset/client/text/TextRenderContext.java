package com.xyp.gtneverreset.client.text;

import com.xyp.gtneverreset.client.text.TextMaskCache.Mask;
import com.xyp.gtneverreset.utils.text.effect.TextEffectStyle;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public record TextRenderContext(Mask mask, TextEffectStyle style, float x, float y, int color, double seconds,
    boolean shadow) {}
