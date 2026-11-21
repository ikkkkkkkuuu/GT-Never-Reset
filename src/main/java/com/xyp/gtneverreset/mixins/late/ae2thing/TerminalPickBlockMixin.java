package com.xyp.gtneverreset.mixins.late.ae2thing;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.common.terminal.WirelessTerminalSession;

@Pseudo
@Mixin(targets = "com.asdflj.ae2thing.loader.KeybindLoader", remap = false)
public abstract class TerminalPickBlockMixin {

    @Inject(method = "getTargetBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private static void gtnr$preferDualTerminal(World world, EntityPlayer player,
        CallbackInfoReturnable<ItemStack> cir) {
        if (WirelessTerminalSession.findDualInterfaceTerminal(player) >= 0) cir.setReturnValue(null);
    }
}
