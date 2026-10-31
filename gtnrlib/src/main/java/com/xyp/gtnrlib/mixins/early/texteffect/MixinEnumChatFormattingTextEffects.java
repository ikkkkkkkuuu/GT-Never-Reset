package com.xyp.gtnrlib.mixins.early.texteffect;

import net.minecraft.util.EnumChatFormatting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.xyp.gtnrlib.text.EffectTextParser;
import com.xyp.gtnrlib.text.TextEffects;

/** Removes effect declarations wherever vanilla explicitly requests unformatted text. */
@Mixin(EnumChatFormatting.class)
public abstract class MixinEnumChatFormattingTextEffects {

    @ModifyVariable(method = "getTextWithoutFormattingCodes", at = @At("HEAD"), argsOnly = true)
    private static String gtnr$stripEffects(String text) {
        return EffectTextParser.containsMarkers(text) ? TextEffects.plainText(text) : text;
    }
}
