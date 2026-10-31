package com.xyp.gtneverreset.common.packet;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.compat.betterquesting.QuestProgress;
import com.xyp.gtnrlib.mod.ModList;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public final class QuestSkipPacket implements IMessage {

    private UUID questId;

    public QuestSkipPacket() {}

    public QuestSkipPacket(UUID questId) {
        this.questId = questId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (buf.readableBytes() != 16) throw new IllegalArgumentException("Invalid quest skip payload");
        questId = new UUID(buf.readLong(), buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(questId.getMostSignificantBits());
        buf.writeLong(questId.getLeastSignificantBits());
    }

    public void handle(EntityPlayerMP player) {
        if (ModList.BetterQuesting.isModLoaded()) QuestProgress.skip(player, questId);
    }
}
