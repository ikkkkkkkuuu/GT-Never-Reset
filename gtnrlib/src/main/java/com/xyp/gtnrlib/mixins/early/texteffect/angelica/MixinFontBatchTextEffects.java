package com.xyp.gtnrlib.mixins.early.texteffect.angelica;

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
import com.xyp.gtnrlib.client.text.DeferredTextEffects;
import com.xyp.gtnrlib.client.text.EffectTextRenderer;
import com.xyp.gtnrlib.client.text.compat.FontBatchBridge;

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
    private void gtnr$directEffectText(float anchorX, float anchorY, int inputColor, boolean enableShadow,
        boolean unicodeFlag, CharSequence string, int stringOffset, int stringLength,
        CallbackInfoReturnable<Float> cir) {
        String text = EffectTextRenderer.markedText(string, stringOffset, stringLength);
        if (text == null) return;
        float width = EffectTextRenderer.INSTANCE
            .drawWithUnicodeFlag(underlying, text, anchorX, anchorY, inputColor, enableShadow, unicodeFlag);
        cir.setReturnValue(width + (enableShadow ? ((BatchingFontRenderer) (Object) this).getShadowOffset() : 0));
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

    // The shader is shared by all font instances, while Angelica caches its mode per instance.
    @Inject(method = "flushBatch", at = @At("RETURN"))
    private void gtnr$restoreFontAntialiasing(CallbackInfo ci) {
        int configuredMode = FontConfig.fontAAMode;
        if (!EffectTextRenderer.isCapturing() || fontAAModeLast == configuredMode) return;
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
