# GTM 小型流体机器移植与验证

以 GregTech CEu Modern `v7.4.0-1.20.1` 为外观、槽位及液槽容量依据，
新增 LV–UV 的小型搅拌机、小型电解机、小型化学反应釜和小型离心机，共 32 台。
贴图、动画、各档外壳及许可证来源记录在
`src/main/resources/META-INF/modern-basic-machines-port/`，其中的资源清单记录原始路径和 SHA-256。

| 机器 | 物品输入/输出 | 流体输入/输出 | 单槽容量 |
| --- | --- | --- | --- |
| 小型搅拌机 | 6 / 1 | 2 / 1 | LV 8,000 L；MV 12,000 L；HV–UV 16,000 L |
| 小型电解机 | 2 / 6 | 1 / 6 | LV 32,000 L；MV 48,000 L；HV–UV 64,000 L |
| 小型化学反应釜 | 2 / 2 | 3 / 2 | 各档均为 16,000 L |
| 小型离心机 | 2 / 6 | 1 / 6 | LV 32,000 L；MV 48,000 L；HV–UV 64,000 L |

容量来自 GTM 的 `hvCappedTankSizeFunction`、`largeTankSizeFunction` 和化反固定容量函数。
运行与空闲贴图、原版动画元数据及 LV–UV 外壳均使用上游资源。

四类机器共用 `common.machine.basicMachine.ModernFluidMachine` 和
`common.gui.modularui.ModernFluidMachineGui`；注册由 `loader.MachineLoader` 编排，
配方表集中在 `loader.GTNRRecipeMaps`，自定义配方适配位于
`common.recipe.gtneverreset.ModernFluidMachineRecipes`，装配机升级配方位于
`common.recipe.gregtech.AssemblerRecipes`。
机器复用 GTNH 的能量、普通超频、进度、无尘室接口和现有 `StockIORecipeBridge`。
多流体槽、待产出流体及输出侧配置保存到 NBT；物品和流体自动输出分别开关，使用同一输出面。

装配机升级配方为同档原版小机器、2 个同档电动泵、配置电路 24 和 144 L 熔融锡，
耗时 10 秒，使用对应档位的配方电压。原版小机器及其制作门槛仍是前置条件。

在加载完成阶段，仅复制 GTNH 对应普通小机器的配方并转换流体容器，
不引入大型化反或其他多方块的专属配方。转换保留电压、时长、环境条件、元数据、
物品和流体概率、矿辞替代及不消耗的催化剂；检查容器平衡，跳过实际消耗容器的配方、
明确禁止转换的配方和没有对应流体的 IC2 电解水单元。
确定性同类流体可以合并；随机流体保留各自概率。

专用服务端回归已通过 327 项断言；当前真实转换配方数量依次为搅拌 415、电解 413、化反 1,071、离心 1,177，共 3,076 条。
客户端回归已取得 PASS，包括全部槽位同步、贴图动画、两种自动输出开关，以及 Shift 输出侧弹窗的服务器和客户端朝向同步。

## 验证命令

在项目根目录使用 PowerShell。为运行时和 Gradle 指定可用的临时目录：

```powershell
$env:TEMP = 'C:/Windows/Temp'
$env:TMP = 'C:/Windows/Temp'
.\gradlew.bat spotlessApply --no-configuration-cache
.\gradlew.bat spotlessCheck compileJava processResources checkTranslationFormatValidator --no-configuration-cache
git diff --check
.\gradlew.bat -I scripts/modern-fluid-machine-qa.init.gradle runServer17 --no-configuration-cache
.\gradlew.bat -I scripts/modern-fluid-machine-client-qa.init.gradle runClient17 --no-configuration-cache
```

QA 入口使用 `build/` 下的独立运行目录和隔离世界，测试类不进入发布 JAR。
服务端结果写入 `build/modern-fluid-machine-qa/result.txt`；客户端结果和截图写入
`build/modern-fluid-machine-client-qa/`。客户端截图覆盖四类 GUI、LV–UV 外壳、
空闲与运行状态动画，以及物品和流体的 Shift 输出侧弹窗。

## NEI 多流体布局回归

四张配方表使用 GT 原生 `LargeNEIFrontend` 的三列布局。电解机和离心机的六个流体输出槽排成两行，
全部槽位保持在 170×82 的配方背景内，并避开 GT 标志和进度条。

客户端回归覆盖四种真实配方页、四表最大槽位边界与重叠检查、实际缓存配方的槽位坐标，
以及电解机和离心机同时占满六个物品和六个流体输出槽的独立显示样例。显示样例不写入正式配方表。

```powershell
.\gradlew.bat -I scripts/modern-fluid-machine-nei-client-qa.init.gradle runClient17 --no-configuration-cache
```

结果和六张配方页截图位于 `build/modern-fluid-machine-nei-client-qa/`。
