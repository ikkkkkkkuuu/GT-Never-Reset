package com.xyp.gtneverreset.compat.betterquesting;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.QuestSkipPacket;

import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.IQuest;
import betterquesting.api2.client.gui.context.IQuestContextMenuEntry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class QuestSkipMenu implements IQuestContextMenuEntry {

    @Override
    public String getLabel(UUID questId, IQuest quest) {
        // #tr gtneverreset.quest.skip
        // # Skip
        // # zh_CN 跳过
        return StatCollector.translateToLocal("gtneverreset.quest.skip");
    }

    @Override
    public Runnable getAction(UUID questId, IQuest quest) {
        return () -> {
            var player = Minecraft.getMinecraft().thePlayer;
            if (player != null && !quest.isComplete(QuestingAPI.getQuestingUUID(player))) {
                NetWorkHandler.sendToServer(new QuestSkipPacket(questId));
            }
        };
    }
}
