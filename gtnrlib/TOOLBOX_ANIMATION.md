# GTNH 工具盒手臂动画

目标：为 GTNH 已有工具盒提供第一人称提握、开盖和翻找动作，全部实现在 GTNRLib。

## 分批实现

- [x] 核实工具盒物品身份与打开入口，建立可选模组适配。
- [x] 添加带提手的工具盒模型与手臂提握姿势。
- [x] 打开工具盒时播放开盖、探手翻找动作，并处理关闭、换物与离开世界。
- [x] 在独立后台客户端中自动验证并截图，记录构建与回归结果。

保留工具盒原有存储和交互行为；可选模组适配放在 `src/optional/java/`。此文档随实现更新。

## 行为

适配 GregTech `ItemGTToolbox`（`ItemList.ToolBox`，工程师工具盒）。右手握住立起的提手，箱体跟随现有第一人称移动惯性。外形沿用原物品的深红箱体、橙色面板、中央竖向银色扣件和灰色方形提手；正面直接读取当前资源包的工具盒图标，搭配立体包角、阶梯顶盖、铰链和分格工具盘。

右键打开原有库存界面或打开 GT 选工具轮盘后，左手先托住箱底，右手通过提手抬盖，再伸进工具盘左右翻找。关闭界面时先收手，再合盖并回到提握。工具盘展示已同步内容中最多六件工具，优先调用物品自身的装备渲染器，保留 GT 材质和叠层，不显示 GT 库存图标上的耐久或电量条。

选出工具后回到 GT 原有工具渲染；工具损坏提示也沿用原行为。动画不修改库存、选择、耐久、充电、服务器包或 GUI 操作。关闭 GTNRLib 第一人称动画开关立即恢复原版持物。

本次范围为主手第一人称；第三人称、背手模组副手、IC2 工具盒不在此适配范围。库存面板和 NEI 仍绘制在手臂前面，可能遮挡部分动作；轮盘右侧和无界面提握可直接看到模型。

## 验证入口

```powershell
.\gradlew.bat spotlessApply spotlessCheck compileJava :gtnrlib:checkLibraryContracts
.\gradlew.bat --no-configuration-cache -I gtnrlib/scripts/animation-qa.init.gradle -PanimationQaDefaults -PanimationQaStandaloneMods -PanimationQaToolbox :runClient25
```

客户端使用独立静音、透明、点击穿透且无任务栏按钮的实例。测试夹具通过原有服务端右键入口打开库存，再使用 GT 原有轮盘工厂与选工具包；无需桌面键鼠操作。

输出位于 `build/animation-qa/library-mods-defaults-toolbox/`，`result.txt` 记录断言，`screenshots/` 保存真实 GUI 和隔离手臂绘制截图。纯状态测试覆盖不同帧率、重复采样、支撑与开盖顺序、收手合盖顺序及重置。

2026-11-14 验证：`spotlessCheck`、`compileJava`、`:gtnrlib:build`、库契约测试和上述独立客户端夹具通过。客户端确认未加载 GT-Never-Reset；真实库存打开、轮盘打开、选工具包、动画开关和换槽检查通过。有工具与空盘的隔离绘制产生 827 个不同像素，确认箱内工具实际可见。

实机截图：[提握](docs/toolbox-animation/carry.png)、[开盖翻找](docs/toolbox-animation/search.png)、[工具盘细节](docs/toolbox-animation/tray.png)。截图来自游戏帧缓冲，未使用图像生成或后期绘制。
