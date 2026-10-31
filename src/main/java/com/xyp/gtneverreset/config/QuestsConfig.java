package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.Sync;

@Sync
public class QuestsConfig {

    @Comment("允许玩家通过任务右键菜单跳过任务，标记完成并保留未领取的奖励；由服务端控制")
    @DefaultBoolean(true)
    public boolean enableSkipping = true;
}
