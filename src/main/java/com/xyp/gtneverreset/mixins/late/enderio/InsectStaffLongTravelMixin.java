package com.xyp.gtneverreset.mixins.late.enderio;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.common.item.items.InsectStaff;

import crazypants.enderio.api.teleport.TravelSource;
import crazypants.enderio.teleport.TravelController;
import crazypants.enderio.teleport.packet.PacketLongDistanceTravelEvent;

@Mixin(value = PacketLongDistanceTravelEvent.class, remap = false)
public abstract class InsectStaffLongTravelMixin {

    @Inject(method = "validate", at = @At("RETURN"), cancellable = true)
    private static void gtnr$validateStaff(EntityPlayerMP player, TravelSource source, boolean conserve,
        boolean alsoTeleport, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = player.getCurrentEquippedItem();
        if (source != TravelSource.TELEPORT_STAFF || stack == null
            || !(stack.getItem() instanceof InsectStaff staff)
            || !staff.isActive(player, stack)) return;
        cir.setReturnValue(
            !alsoTeleport
                || TravelController.instance.validatePacketTravelEvent(player, x, y, z, 0, conserve, source) == null);
    }
}
