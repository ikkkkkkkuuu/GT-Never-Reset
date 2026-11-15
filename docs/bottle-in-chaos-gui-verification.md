# 混沌中瓶 GUI 原版运行对照

2026-11-15，使用 TST `a4b6e17bb02802edfd5c449248e20881faee25b8` 的实际控制器和 GUI，在同一个独立客户端中与 GTNR 控制器先后打开比较。不是网页重绘、效果图或仅凭源码判断。

## 环境和参考构建

- Minecraft 1.7.10、GTNH 2.9.0 依赖清单、GT5 5.09.54.265、ModularUI2 2.3.94-1.7.10、Java 21、中文、1100×740、GUI 缩放 2。
- 原版和移植版均实际成型，15 个加工组、48 个结构模组槽；选择同一预设，装入相同的 12 种原生机器来源、合计 48 个模组。原版使用实体凭证，GTNR 通过免凭证接口选择同一产线。
- 两边的配方池页显式定位到同一页、同一行，分别比较未安装和已安装来源，避免异步初始化造成初始选中项不同。
- 原版从固定提交源码编译。为复用本机已缓存构建环境，仅调整构建工具为 Gradle 9.3.1、GTNH convention 2.0.20、Elytra 1.2.1，依赖对齐清单，使用原仓库已生成且与 GUI 声明一致的语言资源，编译为 Java 21。缺少的编译依赖从原 Maven 仓库取得。
- 原版 GUI、皮肤、文字布局等 7 个相关 Java 文件与下载的提交归档逐字节相同，[SHA-256 记录](images/bottle-in-chaos/comparison/reference-source-hashes.json)。测试用 TST JAR 只进入对照客户端，不进入 GTNR 正式依赖或发行包。
- 测试窗口透明、点击穿透、静音、无任务栏按钮。游戏内夹具通过 GUI API 切页、滚动和操作弹窗；没有操作系统键鼠输入。用户可见预览独立运行。

## 尺寸、位置、间距

从实际构建后的控件树取得逻辑坐标，完成 [217 项布局比较](images/bottle-in-chaos/comparison/layout-comparison.json)。以下位置相对主窗口；像素缩放前的尺寸相同。

| 控件 | 位置与尺寸 | 结果 |
| --- | --- | --- |
| 主窗口 | 448×335 | 一致 |
| 四个标签 | 起点 (8,8)，80×16，横向步长 80 | 一致 |
| 终端区域 | (8,28)，432×183 | 一致 |
| 15 个加工组按钮 | 起点 (14,35)，80×31，横向步长 84、纵向步长 35 | 一致 |
| 五个加工组操作位置 | 起点 (172,148)，48×18，横向步长 53 | 一致 |
| 背包、封装和模组物品槽 | 比较各状态的全部 ItemSlot 坐标与大小 | 一致 |
| 缓存上限弹窗 | 居中，164×78；输入框 148×18 | 一致 |

## 字体、颜色及运行截图

保留原始截图，逐像素比较；[完整像素统计](images/bottle-in-chaos/comparison/pixels.json)同时保留未屏蔽区域的差异数，没有将整张图宣称为完全相同。

| 比较区域 | 比较像素数 | 不同像素数 |
| --- | ---: | ---: |
| 运行列表滚动到底的内容区 | 302,152 | 0 |
| 配方池未安装来源内容区 | 302,152 | 0 |
| 配方池已安装来源内容区 | 302,152 | 0 |
| 模组列表顶部内容区 | 302,152 | 0 |
| 模组列表底部内容区 | 302,152 | 0 |
| 缓存上限弹窗 | 51,168 | 0 |
| 封装槽位及其标签，每个对照状态 | 32,536 | 0 |
| 背包及机器控制区，排除玩家背包中附魔魔杖的动画槽位 | 71,648 | 0 |

这些区域包含实际文字、图标、按钮边框和背景，验证了本次环境下的字体大小、文字布局与颜色。终端外框按时间移动的光带、玩家魔杖附魔闪光在不同截图时刻不同，不作为静态差异。它们的原始差异仍保留在统计中。

| 状态 | TST 原版 | GTNR 移植版 |
| --- | --- | --- |
| 运行信息 | [原版](images/bottle-in-chaos/comparison/original-runtime.png) | [移植版](images/bottle-in-chaos/comparison/port-runtime.png) |
| 运行列表底部 | [原版](images/bottle-in-chaos/comparison/original-runtime-scrolled.png) | [移植版](images/bottle-in-chaos/comparison/port-runtime-scrolled.png) |
| 加工组 | [原版](images/bottle-in-chaos/comparison/original-groups.png) | [移植版](images/bottle-in-chaos/comparison/port-groups.png) |
| 配方池，未安装 | [原版](images/bottle-in-chaos/comparison/original-pools.png) | [移植版](images/bottle-in-chaos/comparison/port-pools.png) |
| 配方池，已安装 | [原版](images/bottle-in-chaos/comparison/original-pools-installed.png) | [移植版](images/bottle-in-chaos/comparison/port-pools-installed.png) |
| 模组列表顶部 | [原版](images/bottle-in-chaos/comparison/original-modules.png) | [移植版](images/bottle-in-chaos/comparison/port-modules.png) |
| 模组列表底部 | [原版](images/bottle-in-chaos/comparison/original-modules-scrolled.png) | [移植版](images/bottle-in-chaos/comparison/port-modules-scrolled.png) |
| 缓存上限弹窗 | [原版](images/bottle-in-chaos/comparison/original-cache-limit.png) | [移植版](images/bottle-in-chaos/comparison/port-cache-limit.png) |

## 保留的需求差异和修正

机器名为“混沌中瓶”。免凭证版将“凭证管理／凭证仓”改为“加工组管理／加工组”，将取出凭证入口改为选择产线；下方标题为“结构封装／拆封”。运行页和加工组页对应文字的像素差异是这些需求导致的。AE 样板编辑与免凭证产线选择是新增窗口，没有对应的原版窗口可作一对一比较。

修正了移植时遗留的实体凭证按钮条件：没有选中产线时，已安装的加工组仍可打开产线选择。夹具先在服务端清空选线，再验证按钮启用、GUI 命中检测、按下、松开和同步弹窗打开；[空组状态](images/bottle-in-chaos/comparison/port-empty-group.png)、[选线弹窗](images/bottle-in-chaos/comparison/port-empty-group-routes.png)及[断言记录](images/bottle-in-chaos/comparison/report.txt)。材料用量不匹配时还增加了完整悬停说明，不改变原有工作区布局。

独立、不加载 TST 的原有验证另外通过 629 项断言，新增工业高炉准确材料清单和多放、少放机壳的回归检查。[独立验证记录](images/bottle-in-chaos/report.txt)。

本记录覆盖上述语言、分辨率、缩放及已列出的状态；不将有限状态的结果称作所有分辨率、语言、输入组合和未来 TST 版本的“完美复刻”。原版布局与用户明确要求的免凭证、独立运行改动分别记录。

## 复现

先构建固定提交的 TST 开发 JAR，再执行：

```powershell
./gradlew.bat '-PchaosReferenceJar=C:/path/TwistSpaceTechnology-dev.jar' -I src/qa/resources/bottle-in-chaos-qa.init.gradle :runClient17 --no-configuration-cache
powershell.exe -NoProfile -File src/qa/resources/compare-bottle-in-chaos.ps1 -Screenshots "$PWD/build/bottle-in-chaos-visual/run/screenshots" -Report "$PWD/build/chaos-pixel-comparison.json"
```

比较夹具位于 `src/qa/java/com/xyp/gtneverreset/qa/BottleInChaosVisualChecks.java`。截图和原始控件坐标输出到 `build/bottle-in-chaos-visual/run`，`result.txt` 必须为 `PASS`。
