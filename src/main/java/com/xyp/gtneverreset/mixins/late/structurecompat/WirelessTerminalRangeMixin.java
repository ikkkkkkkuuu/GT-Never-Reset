package com.xyp.gtneverreset.mixins.late.structurecompat;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;

@Pseudo
@Mixin(targets = "net.glease.structurecompat.WirelessTerminalStackExtractor", remap = false)
public abstract class WirelessTerminalRangeMixin {

    @Inject(method = "rangeCheck", at = @At("RETURN"), cancellable = true)
    private void gtnr$checkInfiniteRange(ItemStack terminal, EntityPlayerMP player, IGrid grid,
        CallbackInfoReturnable<Boolean> result) {
        if (result.getReturnValueZ()) return;
        var wireless = AEApi.instance()
            .registries()
            .wireless();
        var handler = wireless.getWirelessTerminalHandler(terminal);
        if (handler != null && handler.hasInfinityRange(terminal) && wireless.checkRange(terminal, player)) {
            result.setReturnValue(true);
        }
    }
}
