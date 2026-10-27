# GT-Never-Reset

面向 Minecraft 1.7.10 / Forge 的 GT New Horizons 附属模组。

## 📥 下载与兼容性

| GTNH 版本 | 模组版本 | 现状 | 下载 |
| --- | --- | --- | --- |
| `2.9.0-RC-2`（开发基准） | `0.1.0` | 🚧 开发中，尚未发布 | [![Download Releases](https://img.shields.io/badge/Download-Releases-blue)](https://github.com/ikkkkkkkuuu/GT-Never-Reset/releases) |

当前尚无正式发行包，发布后可在 [Releases](https://github.com/ikkkkkkkuuu/GT-Never-Reset/releases) 下载。构建状态与日志可在 [Actions](https://github.com/ikkkkkkkuuu/GT-Never-Reset/actions) 查看。

开发环境使用 Minecraft `1.7.10`、Forge `10.13.4.1614`，以 GTNH `2.9.0-RC-2` 的依赖清单为基准；其他 GTNH 版本的兼容性尚待验证。

## 🧩 功能介绍

以下介绍对应当前开发分支。合成配方可在游戏内 NEI 中查询，多方块结构与详细参数可查看控制器提示。

### ME 无线二合一接口终端

将 ME 接口管理与样板编码放在同一界面中，可切换到无线合成终端，方便集中管理自动化配方。

- 支持从 NEI 导入配方、编辑输入输出并编码样板，`Alt + 点击` 可编码并上传到目标接口。
- 支持接口搜索、隐藏已满样板行，以及按电路等不消耗物品的编号辅助识别接口。
- `Ctrl + 点击` 接口定位按钮可传送到接口附近。
- 可通过快捷键打开终端或将手持物品存入 ME；`Shift / Ctrl + 中键` 点击 NEI 物品可从 ME 提取到物品栏。
- 支持浅色、深色与自动匹配的按钮主题；快捷键可在游戏控制设置中调整。

### 无线频率卡

将整个物理 ME 设备簇无线接入选中的 ME 控制器网络，并提供连接管理和网络可视化。

- `Shift + 右键` ME 控制器保存并选中发射源；右键 ME 设备可连接或断开其整个物理簇。
- 右键空气打开管理界面，可切换已保存的发射源、查看频道占用、筛选设备、添加备注、暂停连接、高亮或传送到设备附近。
- 可开启自动连接，支持佩戴于饰品栏；手持或佩戴时可查看节点、频道、P2P 与合成输入代理等网络连线。
- 普通入口最多承载 8 个频道，致密入口最多 32 个；频道分配仍受源控制器网络预算限制。
- `Shift + 右键` 空气移除当前发射源，已有无线连接和其他已保存的发射源会保留。

### ME 高级 IO 总线

同时管理相邻机器的物品与流体库存，适合需要持续备料和自动回收的生产设备。

- 放入物品或流体样本，中键设置目标库存；自动补足已标记资源，并将未标记资源送回 ME。
- 可切换超量回收，传输遵循机器连接面的访问限制。
- 默认 18 个样本槽，容量卡可扩展至 63 个；提供 8 个升级槽，支持加速卡、容量卡与红石卡。
- 基础吞吐量等同一张超级光速卡，可继续通过加速卡提升。

### 库存 IO 接口（ME）

让受支持的 GT 机器直接读取 ME 中的配方原料与燃料，按实际消耗扣除库存。

- 提供方块与 AE 部件两种形式，连接目标机器后可在界面中标记资源、设置数量与目标方向。
- 物品与流体各支持 900 个标记，可手动配置，也可自动标记 ME 中的资源。
- 支持保留指定 ME 库存，以及为每次配方或燃料检查提供固定可用量。
- 可将机器的物品与流体输出回收至 ME；工作时需要有效的 ME 供电与频道。

### 通配样板符

用一份配方模板为多种材料生成样板，减少逐种编码相似配方的操作。

- 右键打开配置界面，可点击 NEI 配方的 `＋` 自动填写，调整后保存。
- 输入输出支持材料前缀、流体与固定物品，可按材料属性、标签或名称过滤。
- 提供展开预览与材料黑名单，便于检查实际生成的样板。
- 可在支持通配样板的 ME 接口、样板输入仓室与装配矩阵中使用。

### 超级样板输入总成（ME）

为 GT 多方块机器接收 ME 合成任务的原料，并按样板分别缓存。

- 提供 540 个样板槽，支持物品与流体原料；流体配方使用终极编码样板。
- 不同样板的原料互不混用，手动物品槽由所有样板共用。
- 样板配方使用本仓缓存的原料，可配合样板输入镜像供其他机器共用。
- 需要 ME 供电和一个频道；剪线钳右键可切换仅正面或所有面的 ME 连接。

### 电路样板输入总成（ME）

在样板输入总成的基础上自动提供电路与受支持的不消耗模具，减少合成任务对辅助物品的索取。

- 提供 540 个样板槽，自动展开通配样板。
- 每份缓存最多容纳 64 种物品与 64 种流体，每种容量上限为 `2^63 - 1`。
- 每次配方检查每种原料最多提供 `2^31 - 1`，剩余原料继续保留在缓存中。
- 可连接电路样板输入镜像或超级样板输入镜像；通配样板槽需清空当前缓存后才切换展开配方。

### 样板输入镜像（ME）

让多台多方块机器共用一套样板输入主仓，集中维护样板、原料和产物输出缓存。

- 手持数据棒左键点击主仓，再右键点击同维度的镜像完成连接。
- 超级样板输入镜像可共用兼容的样板输入总成或总线。
- 电路样板输入镜像专用于电路样板输入总成；连接时主仓需要已加载且类型匹配。

### 量子计算机

接入 ME 网络后按合成任务动态分配 AE 合成 CPU，用于集中承接大量自动合成请求。

- 提供最高合成存储容量与协处理器数量，无需逐级升级。
- 不消耗 EU 或 AE 合成能量，仍需保持结构成型及 ME 网络连接有效。
- 界面可查看合成存储、协处理器的总量与占用，并为计算机重命名。

### 量子合成模式

为玩家发起的 AE 合成请求提供支持循环样板的合成规划方式，由量子 CPU 执行任务。

- 可通过合成界面的量子合成模式按钮切换，网络中需要有可用的量子计算机。
- 支持分析循环样板的净产出与外部原料需求；缺料时可尝试替代样板。
- 与轻量模式互斥；关闭后使用 AE2 的合成规划算法。
- 可配置计算时限、替代样板尝试时限，以及计算步骤和性能日志。

### 循环样板诊断

帮助排查自动合成中的递归依赖，定位形成循环的样板与机器。

- 合成确认界面与合成树可显示循环信息，区分有净产出和无净产出的循环。
- 支持环形查看循环关系，以及展开或收起循环所需的外部原料。
- 可列出递归样板并高亮存放这些样板的机器，方便回到产线检查。

### 装配矩阵

集中执行 AE 合成样板的多方块机器，适合承接大量工作台合成任务。

- 最多容纳 576 个样板，支持通配样板与样板倍率调整。
- 以 1 tick 执行合成，无需升级，不消耗 EU 或 AE 合成能量。
- 可配置输入、输出与运行模式，并控制是否在接口终端中显示。

### 保险库

集中存储物品与流体的多方块仓库，通过保险库接口接入 ME 网络。

- 物品与流体共用字节容量，各自另有种类与单种存储上限。
- 结构必须且只能安装一个保险库接口，成型时自动绑定；接口可从任意面连接 ME，需要一个频道和 ME 供电。
- 通过接口存取物资，不支持输出总线或输出仓；保险库本身不消耗 EU。
- 可查看容量与存储占用，使用扫描仪查看已存物品索引，螺丝刀右键切换输入溢出销毁。

### 大型矿石处理机

将矿石及选矿中间产物加工为矿粉、宝石和副产物，集中完成矿物处理。

- 基础支持 64 并行与 10 个跨配方线程，并具备无损超频。
- 支持参数模块，可调整并行、速度、能耗与超频行为。
- 具体投入、产出及副产物可在 NEI 的矿石处理配方中查询。

### 大型熔炉

批量执行熔炉配方，可同时处理多种冶炼任务。

- LV 下提供 64 并行与 6 个线程；每提升一档输入电压，两者翻倍。
- 基础处理速度为 2 倍，耗电系数为 0.8，支持安装参数模块进一步调整。
- 结构需要 56 格静止岩浆，钢机械方块位置可替换为兼容仓室。

### 参数控制模块

为兼容的多方块机器调整处理参数，包含并行、速度、能耗与超频四类模块。

- 并行与速度模块提供静态、动态版本；动态模块可右键设置参数。
- 超频模块提供低速无损、无损与奇点无损超频等选择。
- 每台机器的每类模块限装一个，参数修改对新配方任务生效。
- 各级模块的参数范围与倍率可查看物品提示及模块界面。

### 独立流体槽单方块机器

提供 GTM 风格的小型搅拌机、小型电解机与小型化学反应釜，覆盖 LV 至 UV。

- 使用独立流体槽处理对应的 GTNH 配方，无需借助流体单元安排配方输入输出。
- 不同机型具有对应的物品槽、流体槽与容量配置。
- 可在机器界面管理原料与产物，并在 NEI 中查询对应配方。

### 碎片世界采集器

通过维度展示方块与电路选择资源，在任意维度运行采集配方。

- 维度展示方块作为选择条件，不会被消耗。
- 支持物品与流体采集；流体采集需要钻头，钻头有概率消耗。
- 工作耗电为 8 EU/t，耗时随配方变化，资源与条件可在 NEI 中查询。

### 通量插头与通量点

通过 GTNH 无线电网在设备之间传输 GT EU，共用放置者所属 GT 无线团队的电量。

- 通量插头将输入的 EU 存入无线电网，通量点从无线电网取电并输出 EU。
- 放置时自动识别电压，无法识别时使用 32 V；默认 1 A，可右键调整。
- 传输上限为电压 × 安培数 EU/t，输出遵循 GT 的过压与线缆安培数限制。
- 界面支持传输开关、红石控制、区块加载与网络统计。

### 多方块自动提示

从结构定义和机器参数生成多方块提示，便于在搭建前了解结构要求与处理能力。

- 显示方块、仓室、数量与结构等级等要求。
- 根据机器提供的信息显示并行、速度、能耗及超频等参数。
- 配合控制器的功能说明，为结构搭建与模块配置提供参考。

## 📜 制作人员与署名

本项目作者：[xyp](https://github.com/ikkkkkkkuuu)。感谢以下开源项目及贡献者提供的代码、资源和实现参考：

| 项目 | 作者 / 贡献者 | 许可 |
| --- | --- | --- |
| [GT-Not-Good](https://github.com/ikkkkkkkuuu/GT-Not-Good) | GT-Not-Good 贡献者 | [MIT](src/main/resources/META-INF/compact-ae-port/GT-Not-Good-MIT.txt)，第三方派生部分保留原许可 |
| [GT-Not-Leisure](https://github.com/ABKQPO/GT-Not-Leisure) | GT-Not-Leisure 贡献者；自动提示相关贡献者 LineBroke | [LGPL-3.0-only](src/main/resources/META-INF/compact-ae-port/GT-Not-Leisure-LGPL-3.0.txt) |
| [GregTech / GT5-Unofficial](https://github.com/GTNewHorizons/GT5-Unofficial) | GTNewHorizons、GregTech / GT++ 贡献者 | [LGPL-3.0 / LGPL-3.0-or-later](src/main/resources/META-INF/super-storage-input-port/NOTICE.md)，以对应文件声明为准 |
| [Twist Space Technology](https://github.com/Nxer/Twist-Space-Technology-Mod) | Nxer 及贡献者 | [GPL-3.0-only](src/main/resources/META-INF/multiblock-base-port/GPL-3.0.txt) |
| [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) / [GTNH 分支](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial) | AlgorithmX2、TeamAppliedEnergistics、Ridanisaurus Rid 及贡献者 | 代码：[LGPL-3.0-or-later](src/main/resources/META-INF/advancedio-port/NOTICE.md)；视觉素材：[CC BY-NC-SA 3.0](src/main/resources/META-INF/wireless-terminal-port/AE2-CC-BY-NC-SA-3.0.txt)；部分旧版 GUI 贴图：[LGPL-3.0](src/main/resources/META-INF/wildcard-pattern-port/NOTICE.md) |
| [AdvancedAE](https://github.com/pedroksl/AdvancedAE) | pedroksl 及贡献者 | [LGPL-3.0](src/main/resources/META-INF/advancedio-port/LICENSE-LGPL-3.0.txt) |
| [AE2AddonLib](https://github.com/pedroksl/AE2AddonLib) | pedroksl 及贡献者 | [GPL-3.0-only](src/main/resources/META-INF/advancedio-port/AE2AddonLib-LICENSE.txt) |
| [Flux Networks](https://github.com/SonarSonic/Flux-Networks) | Ollie Lansdell / SonarSonic、BloCamLimb 及贡献者 | 代码与方块资源：[MIT](src/main/resources/META-INF/licenses/Flux-Networks-MIT.txt)；GUI 设计与资源：[CC BY-NC-SA 4.0](src/main/resources/META-INF/licenses/Flux-Networks-GUI-CC-BY-NC-SA-4.0.txt) |
| [Modernity GTNH UI](https://github.com/ModernityGTNH/Modernity-GTNH-UI) / [Dark UI](https://github.com/ModernityGTNH/Modernity-GTNH-Dark-UI) | ModernityGTNH 贡献者 | [CC BY-NC-SA 4.0](src/main/resources/META-INF/wireless-terminal-port/Modernity-CC-BY-NC-SA-4.0.txt) |
| [AE2 Lightning Tech Reborn](https://github.com/AE2-Lightning-Tech-Reborn/AE2-Lightning-Tech-Reborn) 无线频率卡贴图 | MOAKIEE、CystrySU、gjmhmm8、\_leng、TedXenon、MHanHanBing 等贡献者 | [CC BY-NC-SA 3.0](src/main/resources/META-INF/wireless-frequency-card/LICENSE-CC-BY-NC-SA-3.0.txt) |
| [GTNH Beyond the Stars](https://github.com/v3rysp3d/GTNH-Beyond-the-Stars) 贴图 | V3rySp3d | [MIT](src/main/resources/META-INF/vault-port/LICENSE-Beyond-the-Stars-MIT.txt) |
| Wildcard-Pattern | LeoDreamer | [MIT](src/main/resources/META-INF/wildcard-pattern-port/Wildcard-Pattern-MIT.txt) |
| LDLib | LDLib 贡献者 | [LGPL-3.0](src/main/resources/META-INF/wildcard-pattern-port/LDLib-LGPL-3.0.txt) |
| [GregTechCEu / GregTech Modern](https://github.com/GregTechCEu/GregTech-Modern) 资源 | GregTechCEu 贡献者 | [LGPL-3.0](src/main/resources/META-INF/wildcard-pattern-port/GTCEu-LGPL-3.0.txt) |
| GTL-Enhancedcore 大型熔炉 | S_Fast | [LGPL-3.0](src/main/resources/META-INF/large-furnace-port/LICENSE-LGPL-3.0.txt) |

本项目原创部分采用 [MIT](LICENSE) 许可；上表所列第三方代码和资源保留各自的许可。

完整署名、来源版本、文件清单、修改说明及许可证文本见 [`src/main/resources/META-INF`](src/main/resources/META-INF)。翻译构建逻辑的来源说明见 [`scripts/translation-NOTICE`](scripts/translation-NOTICE)。
