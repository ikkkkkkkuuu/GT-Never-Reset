package com.xyp.gtnrlib.mixins.early.texteffect;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnrlib.client.text.EffectTextFieldView;
import com.xyp.gtnrlib.client.text.EffectTextLayout.Layout;
import com.xyp.gtnrlib.client.text.EffectTextRenderer;

/** Retains formatting while vanilla scrolls and draws either side of an input cursor. */
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextFieldTextEffects {

    @Shadow
    @Final
    private FontRenderer field_146211_a;

    @Shadow
    private String text;

    @Shadow
    private int lineScrollOffset;

    @Shadow
    private int cursorPosition;

    @Unique
    private EffectTextFieldView gtnr$effectView;

    @Unique
    private EffectTextFieldView gtnr$view() {
        if (!EffectTextRenderer.handles(text)) {
            gtnr$effectView = null;
            return null;
        }
        Layout layout = EffectTextRenderer.INSTANCE.layout(field_146211_a, text);
        if (gtnr$effectView == null || !gtnr$effectView.matches(text, layout))
            gtnr$effectView = new EffectTextFieldView(text, layout);
        return gtnr$effectView;
    }

    @WrapOperation(
        method = { "drawTextBox", "mouseClicked", "setSelectionPos" },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;trimStringToWidth(Ljava/lang/String;I)Ljava/lang/String;"))
    private String gtnr$trimVisibleRange(FontRenderer font, String value, int width, Operation<String> original) {
        EffectTextFieldView view = gtnr$view();
        return view == null ? original.call(font, value, width)
            : view.trim(lineScrollOffset, lineScrollOffset + value.length(), width, false);
    }

    @WrapOperation(
        method = "setSelectionPos",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;"))
    private String gtnr$trimScrollRange(FontRenderer font, String value, int width, boolean reverse,
        Operation<String> original) {
        EffectTextFieldView view = gtnr$view();
        return view == null ? original.call(font, value, width, reverse) : view.trim(0, value.length(), width, reverse);
    }

    @WrapOperation(
        method = "drawTextBox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I",
            ordinal = 0))
    private int gtnr$drawBeforeCursor(FontRenderer font, String value, int x, int y, int color,
        Operation<Integer> original) {
        EffectTextFieldView view = gtnr$view();
        return original.call(
            font,
            view == null ? value : view.slice(lineScrollOffset, lineScrollOffset + value.length()),
            x,
            y,
            color);
    }

    @WrapOperation(
        method = "drawTextBox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I",
            ordinal = 1))
    private int gtnr$drawAfterCursor(FontRenderer font, String value, int x, int y, int color,
        Operation<Integer> original) {
        EffectTextFieldView view = gtnr$view();
        return original.call(
            font,
            view == null ? value : view.slice(cursorPosition, cursorPosition + value.length()),
            x,
            y,
            color);
    }

    @WrapOperation(
        method = "drawTextBox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"))
    private int gtnr$measureSelection(FontRenderer font, String value, Operation<Integer> original) {
        EffectTextFieldView view = gtnr$view();
        return view == null ? original.call(font, value)
            : view.width(lineScrollOffset, lineScrollOffset + value.length());
    }
}
