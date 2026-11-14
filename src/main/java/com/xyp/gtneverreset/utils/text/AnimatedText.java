package com.xyp.gtneverreset.utils.text;

import java.util.function.Supplier;

import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;

public final class AnimatedText {

    private static final String MOD_NAME = "§{ac;2;roll=0.1;hold=3.5}§6§l§o§nGT Never Reset";

    public static final Supplier<String> GT_NEVER_RESET = () -> {
        // #tr gtnr.tooltip.adder
        // # Added by:
        // # zh_CN 添加者：
        return StatCollector.translateToLocal("gtnr.tooltip.adder") + " " + MOD_NAME + AnimatedTooltipHandler.RESET;
    };

}
