# 大型蜜蜂杂交机验证

使用固定 GTNH 依赖，运行独立构建目录、存档和静音隐藏客户端：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/Invoke-Gradle.ps1 spotlessApply compileJava processResources spotlessCheck checkTranslationFormatValidator runClient17 --init-script scripts/bee-breeder-client-qa.init.gradle --project-cache-dir .gradle/bee-breeder-qa --offline --no-configuration-cache --no-daemon --max-workers=2 --console=plain
```

结果在 `build/bee-breeder-client-qa/client-result.txt`，截图在同目录的 `screenshots/`。

夹具验证林业实际物种的多步路线、缺失亲本、组装配方注册、输入总线耗料、重复扫描、断电不结算、公主蜂待输出数量、NBT 恢复和完成结算。GUI 验证运行/物种档案/可杂交到蜂/杂交路线四页、蜂种搜索、选择只修改草稿、非法目标反馈、NEI 蜜蜂拖放、应用后服务端更新和本地化名称解析。

可杂交列表验证直接后代和多步后代、排除已有档案、空档案、缺少亲本，以及每个候选的完整路线。通过实际输入总线补入新蜂种，验证已存档蜂种从打开的可杂交列表中消失。物种档案与可杂交列表使用纯灰背景、深色文字和标准 GT 按钮。

使用实际 LV/MV/HV 能源仓及参数模块，验证 LV 2 种/20 秒、MV 4 种/10 秒、HV 8 种/5 秒，以及多个 LV 能源仓不会升压。以可控分支路线检查每个并行位属于不同的就绪蜂种，未解锁亲本的后继蜂种不会提前执行。验证动态速度/并行、能耗和超频模块、运行中改参数只影响新周期、重复模块结构错误、1 tick 下限与整数并行上限，并核对客户端同步后的 LV/MV 信息。

运行页进度验证包括当前批次蜂种的 NBT 恢复、完成后清空、服务端计时同步、多蜂种翻页，以及调速后继续使用本轮原定时长。为稳定核对截图，未成型夹具中的实际批次计时字段分别固定为 50/200 和 100/200 tick；停止后检查列表和计时均清空。对应截图为 `bee-active-progress-25.png`、`bee-active-progress-50.png`、`bee-active-progress-page-2.png` 和 `bee-active-progress-stopped.png`。

夹具使用实际控制器、输入总线、能源仓和模块，但不自动搭建整座多方块；完整成型后的长期运行不属于该夹具的覆盖范围。
