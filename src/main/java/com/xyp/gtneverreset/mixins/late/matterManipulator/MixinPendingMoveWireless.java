// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtneverreset.mixins.late.matterManipulator;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.recursive_pineapple.matter_manipulator.common.building.PendingMove;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessConstructionSites;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessConstructionSites.Site;

@Mixin(value = PendingMove.class, remap = false)
public abstract class MixinPendingMoveWireless {

    @Shadow
    private List<?> moves;

    @Unique
    private Site gtnr$source;

    @Unique
    private Site gtnr$target;

    @Unique
    private Object gtnr$task;

    @Inject(method = "tryPlaceBlocks", at = @At("RETURN"))
    private void gtnr$moving(ItemStack stack, EntityPlayer player, CallbackInfo ci) {
        if (gtnr$task == null) {
            gtnr$task = new Object();
            PendingMove move = (PendingMove) (Object) this;
            gtnr$source = new Site(
                player.worldObj.provider.dimensionId,
                move.getSrcMinX(),
                move.getSrcMinY(),
                move.getSrcMinZ(),
                move.getSrcMaxX(),
                move.getSrcMaxY(),
                move.getSrcMaxZ());
            gtnr$target = gtnr$source.offset(move.getMoveOffsetX(), move.getMoveOffsetY(), move.getMoveOffsetZ());
        }
        WirelessConstructionSites.update(gtnr$task, moves != null && !moves.isEmpty(), gtnr$source, gtnr$target);
    }
}
