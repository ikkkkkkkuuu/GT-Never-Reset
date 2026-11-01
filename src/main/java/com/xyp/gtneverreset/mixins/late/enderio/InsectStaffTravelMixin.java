package com.xyp.gtneverreset.mixins.late.enderio;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.common.item.items.InsectStaff;

import crazypants.enderio.api.teleport.TravelSource;
import crazypants.enderio.teleport.TravelController;

@Mixin(value = TravelController.class, remap = false)
public abstract class InsectStaffTravelMixin {

    @Inject(method = "getTravelSource", at = @At("HEAD"), cancellable = true)
    private void gtnr$travelSource(EntityPlayer player, ItemStack stack, CallbackInfoReturnable<TravelSource> cir) {
        if (stack != null && stack.getItem() instanceof InsectStaff staff && staff.isActive(player, stack)) {
            cir.setReturnValue(TravelSource.TELEPORT_STAFF);
        }
    }

    @Inject(method = "validatePacketTravelEvent", at = @At("RETURN"), cancellable = true)
    private void gtnr$validateStaff(EntityPlayerMP player, int x, int y, int z, int power, boolean conserve,
        TravelSource source, CallbackInfoReturnable<String> cir) {
        ItemStack stack = player.getCurrentEquippedItem();
        // Only replace the final item-type rejection; retain upstream distance and power validation.
        if ((source == TravelSource.TELEPORT_STAFF || source == TravelSource.TELEPORT_STAFF_BLINK)
            && "not staff".equals(cir.getReturnValue())
            && stack != null
            && stack.getItem() instanceof InsectStaff staff
            && staff.isActive(player, stack)) cir.setReturnValue(null);
    }
}
