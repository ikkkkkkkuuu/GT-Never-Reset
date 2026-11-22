package com.xyp.gtneverreset.mixins.late.wtct;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.asdflj.wtct.loader.KeybindLoader;
import com.xyp.gtneverreset.common.terminal.WirelessTerminalSession;

@Mixin(value = KeybindLoader.class, remap = false)
public abstract class KeybindLoaderMixin {

    @Inject(method = "getTargetBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private static void gtnr$preferDualTerminal(CallbackInfoReturnable<ItemStack> cir) {
        if (WirelessTerminalSession.findDualInterfaceTerminal(Minecraft.getMinecraft().thePlayer) >= 0) {
            cir.setReturnValue(null);
        }
    }
}
