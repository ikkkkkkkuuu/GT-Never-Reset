package com.xyp.gtneverreset.mixins.early.texteffect.angelica;

import net.minecraft.client.gui.FontRenderer;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnewhorizons.angelica.client.font.BatchingFontRenderer;
import com.gtnewhorizons.angelica.config.FontConfig;
import com.xyp.gtneverreset.client.text.EffectTextRenderer;
import com.xyp.gtneverreset.client.text.compat.AngelicaTextAdapter;

/** Supplies the exclusive field redirects only when GTNL does not supply its own font patch. */
@Mixin(value = BatchingFontRenderer.class, remap = false)
public abstract class MixinFontBatchAntialiasing {

    @Shadow
    protected FontRenderer underlying;

    @Redirect(
        method = { "flushBatchInner", "setupFontDrawState" },
        at = @At(
            value = "FIELD",
            target = "Lcom/gtnewhorizons/angelica/config/FontConfig;fontAAMode:I",
            opcode = Opcodes.GETSTATIC),
        remap = false,
        require = 3)
    private int gtnr$sharpMaskCoverage() {
        // Keep the comparison, cached mode and uniform consistent across both Angelica method layouts.
        return EffectTextRenderer.isCapturing() && !AngelicaTextAdapter.usesCustomFont(underlying) ? 0
            : FontConfig.fontAAMode;
    }
}
