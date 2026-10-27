// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtneverreset.mixins.late.gregtech.wireless;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.utils.machine.wireless.WirelessControllerContext;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessControllerVersion;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessHatchOwner;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

@Mixin(value = MTEHatch.class, remap = false)
public abstract class MixinWirelessHatchOwner implements WirelessHatchOwner {

    @Unique
    private Map<MTEMultiBlockBase, Long> gtnr$owners;

    @Override
    public void wirelessFrequency$setController(MTEMultiBlockBase controller) {
        if (controller == null || !(controller instanceof WirelessControllerVersion versioned)) return;
        if (gtnr$owners == null) gtnr$owners = new WeakHashMap<>();
        gtnr$owners.put(controller, versioned.wirelessFrequency$version());
    }

    @Override
    public Set<MTEMultiBlockBase> wirelessFrequency$getControllers() {
        if (gtnr$owners == null || gtnr$owners.isEmpty()) return Collections.emptySet();
        MTEHatch hatch = (MTEHatch) (Object) this;
        IGregTechTileEntity hatchBase = hatch.getBaseMetaTileEntity();
        if (hatchBase == null || !hatch.isValid() || hatchBase.getMetaTileEntity() != hatch) {
            gtnr$owners.clear();
            return Collections.emptySet();
        }
        gtnr$owners.entrySet()
            .removeIf(entry -> {
                MTEMultiBlockBase controller = entry.getKey();
                if (controller == null || !controller.isValid()
                    || !(controller instanceof WirelessControllerVersion versioned)
                    || entry.getValue() != versioned.wirelessFrequency$version()) return true;
                IGregTechTileEntity base = controller.getBaseMetaTileEntity();
                return base == null || base.getMetaTileEntity() != controller
                    || base.getWorld() != hatchBase.getWorld();
            });
        return Collections.unmodifiableSet(new HashSet<>(gtnr$owners.keySet()));
    }

    @Inject(method = "updateCraftingIcon(Lnet/minecraft/item/ItemStack;)V", at = @At("TAIL"))
    private void gtnr$recordHatchOwner(ItemStack icon, CallbackInfo ci) {
        if (icon != null) wirelessFrequency$setController(WirelessControllerContext.current());
    }

    @Inject(method = "updateTexture", at = @At("HEAD"))
    private void gtnr$recordHatchOwnerFromTexture(int id, CallbackInfo ci) {
        wirelessFrequency$setController(WirelessControllerContext.current());
    }
}
