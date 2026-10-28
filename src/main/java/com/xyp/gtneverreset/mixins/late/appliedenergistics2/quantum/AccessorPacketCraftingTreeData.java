package com.xyp.gtneverreset.mixins.late.appliedenergistics2.quantum;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import appeng.core.sync.packets.PacketCraftingTreeData;
import io.netty.buffer.ByteBuf;

@Mixin(value = PacketCraftingTreeData.class, remap = false)
public interface AccessorPacketCraftingTreeData {

    @Invoker("<init>")
    static PacketCraftingTreeData gtnr$create(ByteBuf chunkData, int chunkId, int totalChunks) {
        throw new AssertionError();
    }
}
