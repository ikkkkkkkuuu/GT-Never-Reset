package com.xyp.gtneverreset.mixins.late.wtct;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.asdflj.wtct.client.event.BlockPickerKeyHandler;
import com.xyp.gtneverreset.common.terminal.WirelessTerminalSession;

@Mixin(value = BlockPickerKeyHandler.class, remap = false)
public abstract class BlockPickerKeyHandlerMixin {

    @Inject(method = "carries", at = @At("HEAD"), cancellable = true, require = 0)
    private static void gtnr$skipCardRequest(Minecraft mc, ItemStack picked, CallbackInfoReturnable<Boolean> cir) {
        if (WirelessTerminalSession.findDualInterfaceTerminal(mc.thePlayer) >= 0) cir.setReturnValue(true);
    }
}
