package com.xyp.gtneverreset.mixins.late.enderio;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.common.item.items.ItemUniversalWand;

import crazypants.enderio.teleport.TravelController;

@Pseudo
@Mixin(targets = "crazypants.enderio.teleport.packet.PacketLongDistanceTravelEvent", remap = false)
public abstract class UniversalWandLongDistanceTravelMixin {

    @Inject(method = "validate", at = @At("RETURN"), cancellable = true)
    private static void gtneverreset$acceptWand(EntityPlayerMP player,
        crazypants.enderio.api.teleport.TravelSource source, boolean conserveMotion, boolean alsoDoTeleport, int x,
        int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || source != crazypants.enderio.api.teleport.TravelSource.TELEPORT_STAFF) return;
        ItemStack stack = player.getHeldItem();
        if (stack == null || !(stack.getItem() instanceof ItemUniversalWand)
            || !ItemUniversalWand.isTeleportEnabled(stack)) return;
        // Preserve validation of the client-supplied fallback destination in combined mode.
        if (alsoDoTeleport
            && TravelController.instance.validatePacketTravelEvent(player, x, y, z, 0, conserveMotion, source) != null)
            return;
        cir.setReturnValue(true);
    }
}
