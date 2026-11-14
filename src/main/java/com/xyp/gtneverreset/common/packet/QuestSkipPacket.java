package com.xyp.gtneverreset.common.packet;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.compat.betterquesting.QuestProgress;
import com.xyp.gtnrlib.mod.ModList;
import com.xyp.gtnrlib.network.ServerboundPacket;

import io.netty.buffer.ByteBuf;

public final class QuestSkipPacket extends ServerboundPacket {

    private UUID questId;

    public QuestSkipPacket() {}

    public QuestSkipPacket(UUID questId) {
        this.questId = questId;
    }

    @Override
    protected void read(ByteBuf buf) {
        if (buf.readableBytes() != 16) throw new IllegalArgumentException("Invalid quest skip payload");
        questId = new UUID(buf.readLong(), buf.readLong());
    }

    @Override
    protected void write(ByteBuf buf) {
        buf.writeLong(questId.getMostSignificantBits());
        buf.writeLong(questId.getLeastSignificantBits());
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        if (ModList.BetterQuesting.isModLoaded()) QuestProgress.skip(player, questId);
    }
}
