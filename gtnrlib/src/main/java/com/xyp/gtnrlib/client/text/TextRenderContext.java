package com.xyp.gtnrlib.client.text;

import com.xyp.gtnrlib.client.text.TextMaskCache.Mask;
import com.xyp.gtnrlib.text.TextEffectStyle;

public record TextRenderContext(Mask mask, TextEffectStyle style, float x, float y, int color, double seconds,
    boolean shadow) {}
