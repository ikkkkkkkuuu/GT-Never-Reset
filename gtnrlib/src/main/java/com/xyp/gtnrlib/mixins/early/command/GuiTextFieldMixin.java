package com.xyp.gtnrlib.mixins.early.command;

import java.util.function.BiFunction;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnrlib.client.command.GuiTextFieldExtras;
import com.xyp.gtnrlib.client.text.EffectTextRenderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mixin(GuiTextField.class)
@SideOnly(Side.CLIENT)
public abstract class GuiTextFieldMixin implements GuiTextFieldExtras {

    @Shadow
    private String text;
    @Shadow
    private FontRenderer field_146211_a;
    @Shadow
    public int xPosition;
    @Shadow
    public int yPosition;
    @Shadow
    private int width;
    @Shadow
    private int height;
    @Shadow
    private int cursorPosition;
    @Shadow
    private int lineScrollOffset;
    @Shadow
    private int selectionEnd;
    @Shadow
    private boolean isFocused;
    @Shadow
    private int cursorCounter;
    @Shadow
    private boolean isEnabled;
    @Shadow
    private int enabledColor;
    @Shadow
    private int disabledColor;
    @Shadow
    private boolean enableBackgroundDrawing;

    @Unique
    private String gtnrlib$currentSuggestion;
    @Unique
    private boolean gtnrlib$chatInput;
    @Unique
    private BiFunction<String, Integer, String> gtnrlib$textFormatter = new BiFunction<String, Integer, String>() {

        @Override
        public String apply(String text, Integer pos) {
            return text;
        }
    };

    @Shadow
    public abstract boolean getVisible();

    @Shadow
    public abstract boolean getEnableBackgroundDrawing();

    @Shadow
    public abstract int getMaxStringLength();

    @Shadow
    protected abstract void drawCursorVertical(int startX, int startY, int endX, int endY);

    @Inject(method = "drawTextBox", at = @At("HEAD"), cancellable = true)
    private void renderCustomTextBox(CallbackInfo ci) {
        if (!this.gtnrlib$chatInput || EffectTextRenderer.handles(this.text)) return;
        if (!this.getVisible()) {
            ci.cancel();
            return;
        }

        this.gtnrlib$renderBackground();

        int textColor = this.isEnabled ? this.enabledColor : this.disabledColor;
        int cursorPos = this.cursorPosition - this.lineScrollOffset;
        int selEnd = this.selectionEnd - this.lineScrollOffset;
        int innerWidth = this.enableBackgroundDrawing ? this.width - 8 : this.width;
        String visibleText = this.field_146211_a
            .trimStringToWidth(this.text.substring(this.lineScrollOffset), innerWidth);
        boolean shouldShowCursor = cursorPos >= 0 && cursorPos <= visibleText.length();
        boolean showBlinkingCursor = this.isFocused && this.cursorCounter / 6 % 2 == 0 && shouldShowCursor;
        int textX = this.enableBackgroundDrawing ? this.xPosition + 4 : this.xPosition;
        int textY = this.enableBackgroundDrawing ? this.yPosition + (this.height - 8) / 2 : this.yPosition;
        int renderX = textX;
        selEnd = Math.min(selEnd, visibleText.length());

        if (!visibleText.isEmpty()) {
            String textBeforeCursor = shouldShowCursor ? visibleText.substring(0, cursorPos) : visibleText;
            renderX = this.field_146211_a.drawStringWithShadow(
                this.gtnrlib$textFormatter.apply(textBeforeCursor, this.lineScrollOffset),
                textX,
                textY,
                textColor);
        }

        boolean isAtEnd = this.cursorPosition < this.text.length() || this.text.length() >= this.getMaxStringLength();
        int cursorX = shouldShowCursor ? (isAtEnd ? renderX - 1 : renderX)
            : (cursorPos > 0 ? textX + this.width : textX);

        if (!visibleText.isEmpty() && shouldShowCursor && cursorPos < visibleText.length()) {
            this.field_146211_a.drawStringWithShadow(
                this.gtnrlib$textFormatter.apply(visibleText.substring(cursorPos), this.cursorPosition),
                renderX,
                textY,
                textColor);
        }

        if (!isAtEnd && this.gtnrlib$currentSuggestion != null) {
            this.field_146211_a.drawStringWithShadow(this.gtnrlib$currentSuggestion, cursorX - 1, textY, -8355712);
        }

        if (showBlinkingCursor) {
            if (isAtEnd) {
                Gui.drawRect(cursorX, textY - 1, cursorX + 1, textY + 1 + this.field_146211_a.FONT_HEIGHT, -3092272);
            } else {
                this.field_146211_a.drawStringWithShadow("_", cursorX, textY, textColor);
            }
        }

        if (selEnd != cursorPos) {
            int selectionX = textX + this.field_146211_a.getStringWidth(visibleText.substring(0, selEnd));
            this.drawCursorVertical(cursorX, textY - 1, selectionX - 1, textY + 1 + this.field_146211_a.FONT_HEIGHT);
        }

        ci.cancel();
    }

    @Unique
    private void gtnrlib$renderBackground() {
        if (this.getEnableBackgroundDrawing()) {
            Gui.drawRect(
                this.xPosition - 1,
                this.yPosition - 1,
                this.xPosition + this.width + 1,
                this.yPosition + this.height + 1,
                -6250336);
            Gui.drawRect(
                this.xPosition,
                this.yPosition,
                this.xPosition + this.width,
                this.yPosition + this.height,
                -16777216);
        }
    }

    @Unique
    @Override
    public void gtnrlib$suggestion(@Nullable String suggestion) {
        this.gtnrlib$currentSuggestion = suggestion;
    }

    @Unique
    @Override
    public void gtnrlib$chatInput(boolean enabled) {
        this.gtnrlib$chatInput = enabled;
    }

    @Unique
    @Override
    public void gtnrlib$textFormatter(BiFunction<String, Integer, String> formatter) {
        this.gtnrlib$textFormatter = formatter;
    }

    @Unique
    @Override
    public int gtnrlib$screenX(int position) {
        return position > this.text.length() ? this.xPosition
            : this.xPosition + this.field_146211_a.getStringWidth(this.text.substring(0, position));
    }
}
