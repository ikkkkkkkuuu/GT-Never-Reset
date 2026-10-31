package com.xyp.gtneverreset.mixins.late.matterManipulator;

import java.util.Map;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer.IBlockApplyContext;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;
import com.xyp.gtneverreset.common.wireless.IWirelessCopiedLink;
import com.xyp.gtneverreset.common.wireless.WirelessCopiedLinks;
import com.xyp.gtneverreset.common.wireless.WirelessCopiedLinks.CopiedLink;

@Mixin(value = PendingBlock.class, remap = false)
public abstract class MixinPendingBlockWireless implements IWirelessCopiedLink {

    @Unique
    private transient Map<ForgeDirection, CopiedLink> gtnr$copiedLinks;

    @Override
    public Map<ForgeDirection, CopiedLink> gtnr$copiedLinks() {
        return gtnr$copiedLinks;
    }

    @Override
    public void gtnr$setCopiedLinks(Map<ForgeDirection, CopiedLink> links) {
        gtnr$copiedLinks = links;
    }

    @Inject(method = "analyze", at = @At("RETURN"))
    private void gtnr$captureLinks(TileEntity tile, int flags, CallbackInfoReturnable<PendingBlock> cir) {
        gtnr$copiedLinks = WirelessCopiedLinks.capture(tile);
    }

    @Inject(method = "reset", at = @At("HEAD"))
    private void gtnr$resetLinks(CallbackInfoReturnable<PendingBlock> cir) {
        gtnr$copiedLinks = null;
    }

    @Inject(
        method = "clone()Lcom/recursive_pineapple/matter_manipulator/common/building/PendingBlock;",
        at = @At("RETURN"))
    private void gtnr$cloneLinks(CallbackInfoReturnable<PendingBlock> cir) {
        ((IWirelessCopiedLink) cir.getReturnValue()).gtnr$setCopiedLinks(gtnr$copiedLinks);
    }

    @Inject(
        method = "transform(Lcom/recursive_pineapple/matter_manipulator/common/items/manipulator/Transform;)V",
        at = @At("RETURN"))
    private void gtnr$rotateLinks(Transform transform, CallbackInfo ci) {
        gtnr$copiedLinks = WirelessCopiedLinks.rotate(gtnr$copiedLinks, transform::apply);
    }

    @Inject(method = "apply", at = @At("RETURN"))
    private void gtnr$restoreLinks(IBlockApplyContext context, World world, CallbackInfoReturnable<Boolean> cir) {
        WirelessCopiedLinks.pasted(context.getRealPlayer(), context.getTileEntity(), gtnr$copiedLinks);
    }
}
