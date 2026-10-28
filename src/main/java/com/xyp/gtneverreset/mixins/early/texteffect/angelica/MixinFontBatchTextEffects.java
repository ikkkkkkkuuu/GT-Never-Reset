package com.xyp.gtneverreset.mixins.early.texteffect.angelica;

import net.minecraft.client.gui.FontRenderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizons.angelica.client.font.BatchingFontRenderer;
import com.gtnewhorizons.angelica.config.FontConfig;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.xyp.gtneverreset.client.text.DeferredTextEffects;
import com.xyp.gtneverreset.client.text.EffectTextRenderer;
import com.xyp.gtneverreset.client.text.compat.FontBatchBridge;
import com.xyp.gtneverreset.utils.text.effect.EffectTextParser;

/** Flushes the current batch before temporary render-target changes. */
@Mixin(value = BatchingFontRenderer.class, remap = false)
public abstract class MixinFontBatchTextEffects implements FontBatchBridge {

    @Shadow
    protected FontRenderer underlying;

    @Shadow
    private int batchDepth;

    @Shadow
    private static BatchingFontRenderer arenaOwner;

    @Shadow
    @Final
    private int fontShaderId;

    @Shadow
    @Final
    private int AAMode;

    @Shadow
    private int fontAAModeLast;

    @Shadow
    private void flushBatch() {}

    @Shadow
    public static void flushDeferredText() {}

    @Inject(method = "drawString", at = @At("HEAD"), cancellable = true, order = 850)
    private void gtnr$directEffectText(float x, float y, int color, boolean shadow, boolean unicode,
        CharSequence source, int offset, int length, CallbackInfoReturnable<Float> cir) {
        if (EffectTextRenderer.isBypassingEffects() || source == null) return;
        int start = Math.max(0, Math.min(offset, source.length()));
        int end = start + Math.max(0, Math.min(length, source.length() - start));
        if (!EffectTextParser.containsMarkers(source, start, end)) return;
        String text = start == 0 && end == source.length() ? source.toString()
            : source.subSequence(start, end)
                .toString();
        if (!EffectTextRenderer.handles(text)) return;
        boolean previousUnicode = underlying.getUnicodeFlag();
        underlying.setUnicodeFlag(unicode);
        try {
            EffectTextRenderer.INSTANCE.draw(underlying, text, x, y, color, shadow);
            cir.setReturnValue(
                x + EffectTextRenderer.INSTANCE.layout(underlying, text)
                    .width() + (shadow ? ((BatchingFontRenderer) (Object) this).getShadowOffset() : 0));
        } finally {
            underlying.setUnicodeFlag(previousUnicode);
        }
    }

    @Override
    public int gtnr$suspendBatch() {
        flushDeferredText();
        if (arenaOwner instanceof FontBatchBridge owner) owner.gtnr$flushBatch();
        flushBatch();
        int depth = batchDepth;
        batchDepth = 0;
        return depth;
    }

    @Inject(method = "flushDeferredText", at = @At("RETURN"))
    private static void gtnr$drawDeferredEffects(CallbackInfo ci) {
        DeferredTextEffects.flush();
    }

    @Override
    public void gtnr$resumeBatch(int depth) {
        batchDepth = depth;
    }

    @Override
    public void gtnr$flushBatch() {
        flushBatch();
    }

    @Inject(method = "shouldDeferNow", at = @At("HEAD"), cancellable = true)
    private static void gtnr$immediateMask(CallbackInfoReturnable<Boolean> cir) {
        if (EffectTextRenderer.isCapturing()) cir.setReturnValue(false);
    }

    @Inject(method = "shouldDrawThroughPipeline", at = @At("HEAD"), cancellable = true)
    private void gtnr$localMaskTarget(CallbackInfoReturnable<Boolean> cir) {
        if (EffectTextRenderer.isCapturing()) cir.setReturnValue(false);
    }

    @ModifyArg(
        method = { "flushBatchInner", "setupFontDrawState" },
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/GLStateManager;tryBlendFuncSeparate(IIII)V",
            ordinal = 0),
        index = 3,
        remap = false,
        require = 1)
    private int gtnr$compositeMaskCoverage(int destinationAlpha) {
        // Bold copies must accumulate coverage instead of erasing strokes with their translucent edges.
        return EffectTextRenderer.isCapturing() ? GL11.GL_ONE_MINUS_SRC_ALPHA : destinationAlpha;
    }

    @Inject(method = "flushBatch", at = @At("RETURN"))
    private void gtnr$restoreFontAntialiasing(CallbackInfo ci) {
        int configuredMode = FontConfig.fontAAMode;
        if (!EffectTextRenderer.isCapturing() || fontAAModeLast == configuredMode) return;
        // The shader is shared by all font instances, while Angelica caches its mode per instance.
        int previousProgram = GLStateManager.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GLStateManager.glUseProgram(fontShaderId);
        try {
            GLStateManager.glUniform1i(AAMode, configuredMode);
            fontAAModeLast = configuredMode;
        } finally {
            GLStateManager.glUseProgram(previousProgram);
        }
    }
}
