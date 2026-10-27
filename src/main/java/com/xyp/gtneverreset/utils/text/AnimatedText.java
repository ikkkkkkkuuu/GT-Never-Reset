package com.xyp.gtneverreset.utils.text;

import java.util.function.Supplier;

import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;

public final class AnimatedText {

    private static final Supplier<String> MOD_NAME = AnimatedTooltipHandler.animatedText(
        "GT Never Reset",
        1,
        100,
        AnimatedTooltipHandler.RED + AnimatedTooltipHandler.BOLD,
        AnimatedTooltipHandler.GOLD + AnimatedTooltipHandler.BOLD,
        AnimatedTooltipHandler.YELLOW + AnimatedTooltipHandler.BOLD,
        AnimatedTooltipHandler.GREEN + AnimatedTooltipHandler.BOLD,
        AnimatedTooltipHandler.AQUA + AnimatedTooltipHandler.BOLD,
        AnimatedTooltipHandler.BLUE + AnimatedTooltipHandler.BOLD,
        AnimatedTooltipHandler.LIGHT_PURPLE + AnimatedTooltipHandler.BOLD);

    public static final Supplier<String> GT_NEVER_RESET = () -> {
        // #tr gtnr.tooltip.adder
        // # Added by:
        // # zh_CN 添加者：
        return StatCollector.translateToLocal("gtnr.tooltip.adder") + " "
            + MOD_NAME.get()
            + AnimatedTooltipHandler.RESET;
    };

    private AnimatedText() {}
}
