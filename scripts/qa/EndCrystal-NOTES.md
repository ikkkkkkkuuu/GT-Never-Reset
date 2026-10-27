# 末晶谐振器与稳定末影水晶验证

来源、版本、许可证和贴图校验值见
`src/main/resources/META-INF/crystal-resonator-port/`。

机器保留 GTNG 的 LV–MAX 共 14 档、原装配机配方及无线发电参数：
LV 为额定电压的 32 倍 EU/t，其余档位为 128 倍，每 20 tick 存入机主的 GT 无线电网。
本移植只识别正上方的本模组稳定末影水晶，不消耗水晶。

水晶由 7 个玻璃、1 个末影之眼和 1 个恶魂之泪合成。
右键坚固顶面放置，上方需要两格空气；生存模式打破后回收。
独立实体不继承原版末影水晶，不会爆炸或在末地点火。
客户端引用原版水晶模型和贴图，构造模型时禁用底座。

## 重跑

在项目根目录执行；只对本次 PowerShell 会话设置临时 JVM 参数：

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp'
./gradlew.bat -I scripts/crystal-client-qa.init.gradle spotlessApply spotlessCheck compileJava processResources runClient17 --no-configuration-cache --console=plain
git diff --check
```

测试使用 `build/crystal-isolated` 独立构建目录，并启动静音、透明、穿透且隐藏任务栏的专用客户端。
结果位于 `build/crystal-client-qa/client-result.txt`，截图位于其 `screenshots/` 子目录。
测试类和临时世界不进入发布 JAR。

2026-10-27 实测通过：14 档注册、装配机配方、顶面放置、侧面/遮挡/重复放置拒绝、
生存扣物与创造不扣物、无线电量精确结算、暂停发电、未结算电量及实体 NBT 恢复、
移除水晶后停止发电、无爆炸回收、火焰与爆炸伤害免疫、末地不生成火焰、客户端实体同步。
人工检查两帧实际游戏截图：水晶保留旋转与浮动，上半部分正常显示，无基岩底座。
