package com.xyp.gtneverreset.mixins.early.minecraft;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.client.projector.ProjectorClient;
import com.xyp.gtneverreset.config.ToolsConfig;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClientProjector {

    @Inject(method = "handleChunkData", at = @At("TAIL"))
    private void gtneverreset$projectorChunk(S21PacketChunkData packet, CallbackInfo ci) {
        if (!ToolsConfig.enableMultiblockProjector) return;
        ProjectorClient.chunkReceived(packet.func_149273_e(), packet.func_149271_f());
    }

    @Inject(method = "handleMapChunkBulk", at = @At("TAIL"))
    private void gtneverreset$projectorChunks(S26PacketMapChunkBulk packet, CallbackInfo ci) {
        if (!ToolsConfig.enableMultiblockProjector) return;
        for (int i = 0; i < packet.func_149254_d(); i++) {
            ProjectorClient.chunkReceived(packet.func_149255_a(i), packet.func_149253_b(i));
        }
    }
}
