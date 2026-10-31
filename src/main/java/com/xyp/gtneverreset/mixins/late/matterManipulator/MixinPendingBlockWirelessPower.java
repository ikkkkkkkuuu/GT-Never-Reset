package com.xyp.gtneverreset.mixins.late.matterManipulator;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer.IBlockApplyContext;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.xyp.gtneverreset.common.wireless.WirelessPowerTransfer;

@Mixin(value = PendingBlock.class, remap = false)
public abstract class MixinPendingBlockWirelessPower {

    @Unique
    private transient NBTTagCompound gtnr$copiedPower;

    @Inject(method = "analyze", at = @At("RETURN"))
    private void gtnr$capturePower(TileEntity tile, int flags, CallbackInfoReturnable<PendingBlock> cir) {
        gtnr$copiedPower = WirelessPowerTransfer.copySettings(tile);
    }

    @Inject(method = "reset", at = @At("HEAD"))
    private void gtnr$resetPower(CallbackInfoReturnable<PendingBlock> cir) {
        gtnr$copiedPower = null;
    }

    @Inject(
        method = "clone()Lcom/recursive_pineapple/matter_manipulator/common/building/PendingBlock;",
        at = @At("RETURN"))
    private void gtnr$clonePower(CallbackInfoReturnable<PendingBlock> cir) {
        ((MixinPendingBlockWirelessPower) (Object) cir.getReturnValue()).gtnr$copiedPower = gtnr$copiedPower == null
            ? null
            : (NBTTagCompound) gtnr$copiedPower.copy();
    }

    @Inject(method = "apply", at = @At("RETURN"))
    private void gtnr$restorePower(IBlockApplyContext context, World world, CallbackInfoReturnable<Boolean> cir) {
        // MM's result means a property changed, not whether a matching destination exists.
        WirelessPowerTransfer.pasteSettings(context.getRealPlayer(), context.getTileEntity(), gtnr$copiedPower);
    }
}
