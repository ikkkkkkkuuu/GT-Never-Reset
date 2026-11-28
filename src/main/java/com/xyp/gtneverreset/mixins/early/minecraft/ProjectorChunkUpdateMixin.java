package com.xyp.gtneverreset.mixins.early.minecraft;

import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.ChunkCoordIntPair;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xyp.gtneverreset.common.projector.ProjectorPlacementUpdates;

@Mixin(targets = "net.minecraft.server.management.PlayerManager$PlayerInstance")
public abstract class ProjectorChunkUpdateMixin {

    @Shadow
    @Final
    private ChunkCoordIntPair chunkLocation;

    @Shadow(remap = false)
    @Final
    private PlayerManager this$0;

    @ModifyExpressionValue(
        method = "sendChunkUpdate",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraftforge/common/ForgeModContainer;clumpingThreshold:I",
            remap = false),
        require = 1)
    private int gtnr$preserveProjectedBlockVisibility(int threshold) {
        // A full section packet would reveal real blocks that belong to a later animation band.
        return ProjectorPlacementUpdates
            .hasHeldBlocksInChunk(this$0.getWorldServer(), chunkLocation.chunkXPos, chunkLocation.chunkZPos)
                ? Integer.MAX_VALUE
                : threshold;
    }
}
