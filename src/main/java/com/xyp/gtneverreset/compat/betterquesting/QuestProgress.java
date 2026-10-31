package com.xyp.gtneverreset.compat.betterquesting;

import java.util.Collections;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.config.QuestsConfig;

import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.IQuest;
import betterquesting.network.handlers.NetQuestEdit;
import betterquesting.questing.QuestDatabase;

public final class QuestProgress {

    private QuestProgress() {}

    public static void skip(EntityPlayerMP player, UUID questId) {
        if (!QuestsConfig.enableSkipping || questId == null) return;
        IQuest quest = QuestDatabase.INSTANCE.get(questId);
        UUID playerId = QuestingAPI.getQuestingUUID(player);
        // Completing an already completed quest through BQ's admin API would mark its rewards claimed.
        if (quest == null || quest.isComplete(playerId)) return;
        NetQuestEdit.completeQuests(Collections.singletonList(questId), true, playerId);
    }
}
