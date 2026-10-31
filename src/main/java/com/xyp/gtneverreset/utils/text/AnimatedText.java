package com.xyp.gtneverreset.utils.text;

import java.util.function.Supplier;

import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;
import com.xyp.gtnrlib.text.TextEffects;

public final class AnimatedText {

    private static final String MOD_NAME = TextEffects.apply("GT Never Reset");

    public static final Supplier<String> GT_NEVER_RESET = () -> {
        // #tr gtnr.tooltip.adder
        // # Added by:
        // # zh_CN 添加者：
        return StatCollector.translateToLocal("gtnr.tooltip.adder") + " " + MOD_NAME + AnimatedTooltipHandler.RESET;
    };

}
