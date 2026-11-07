# 样板配方池与跨类型输入总成

快捷编码终端从 GT 的 NEI 配方页导入处理配方时，将该页的 `RecipeMap.unlocalizedName`
写入样板的 `gtnrPatternRecipeMap` 字段。此字段独立于“写入编程电路和模具信息”开关。
重新编辑、倍增和通配符展开保留配方池；终端修改输入或输出的物品、流体类型后，不再沿用旧配方池。
手动填写且没有导入来源的配方不猜测配方池。

“超级跨配方类型样板输入总成 (ME)”继承电路样板总成的独立缓存、虚拟电路和模具。
只有新总成的库存启用按样板选择配方池；普通及电路总成仍使用控制器当前模式，单方块推料不参与跨池路由。
未携带配方池的样板按控制器当前模式加工，未知、损坏或机器不支持的配方池保留原料，不能退回其他池误加工。
相同输入输出但配方池不同的通配符展开结果拥有不同标识。

新总成顶部提供固定宽度的配方池下拉栏，选项取自所连接控制器的全部可用配方池。
它只选择 AE 接口终端中的名称，不改变加工模式；显式选择会替换已有的手动命名，选择结果随存档保存。
未连接控制器时不显示过期选项，重新连接后恢复仍有效的选择。
下拉栏直接复用 GTNRLib 的 `ScrollingDropdown`，与样板通配符界面一致。每个选项沿用顶部按钮的主题材质，
带完整边框和悬停效果；选项之间有细分隔线，超长名称在固定栏位内横向滚动。

工具栏的“自动分配名字”按钮在服务端为已连接控制器上的跨配方池总成批量选择显示名称。
总成按坐标排序，按控制器声明的配方池顺序循环分配，例如两个配方池、四个总成依次分为 A、B、A、B。
同一主仓的多个镜像只计算一次，镜像沿用主仓名称；共享主仓只命名一次，其他控制器继续分配尚未覆盖的名称。
普通总成、电路总成、过期关联不参与；不会沿其他总成的连接继续扩展到无关控制器。
批量命名会替换参与总成已有的手动名称，保存配方池 ID，由客户端按当前语言显示；加工模式和缓存内容保持原样。

“超级跨配方类型样板输入镜像 (ME)”为新总成的专用镜像，共享主仓的样板、原料缓存和产物输出缓存。
只有镜像接入控制器时，总成的下拉栏也能读取其配方池；多个连接提供的配方池会去重合并。
镜像由电路样板输入镜像、2 个 LV 电路、2 块铝板在装配机中以 24 号编程电路升级，耗时 5 秒、32 EU/t。

普通、电路、跨配方池三类输入各自使用对应镜像，不再跨类型绑定。
这只限制镜像与主仓之间的绑定，不限制不同类型的总成或镜像安装在同一台多方块上。
数据棒、复制粘贴、NBT 恢复及已缓存的连接都检查镜像类型；失败的重新绑定不覆盖已有合法连接。
旧存档中的跨类型镜像连接不再生效，需要更换对应镜像；主仓内的样板和原料不因连接失效而删除。

![新总成的配方池下拉栏与选项分隔线](images/pattern-recipe-routing/selector.png)

跨池处理使用机器的 `getAvailableRecipeMaps`、`nextMachineMode` 和 `setMachineMode`，
不维护按名称匹配的机器白名单。检查目标配方前重新运行原生参数准备，保留目标模式的电压、并行、速度、
耗电及配方校验。完成或失败后恢复原模式及处理参数。配方锁定期间不自动切换到其他模式。
电路装配线还执行目标模式的印刻和长度前置检查。

路由在机器入口以 GT `ProcessingLogic` 类型调用样板缓存、加工和清理时进入，随后仍调用机器的原有虚方法。
因此，第三方重写这些方法、或在调用父类加工前读取模式参数，也能共用同一条路由。
调用桥只按 GT 方法签名匹配，类加载时处理一次；没有模组版本表、机器白名单、逐 tick 反射或私有字段探测。
ASM API 在初始化时按当前运行环境选择，兼容 Java 8 Forge 的 ASM 5 和现代启动器的新版 ASM。
原有基类钩子保留，供直接调用继承方法的代码使用；嵌套进入父类不会重复路由。

适配范围是通过 GT `ProcessingLogic` API 加工、并通过模式方法读写状态的机器，并非任意声明多个配方池的控制器。
模式可以由机器自己的字段保存，只要 `getMachineMode`、`nextMachineMode`、`setMachineMode` 与
`getRecipeMap` 一致；切换失败或抛出异常后仍恢复原模式。普通输入、普通及电路总成继续调用原逻辑。
研究站的专用研究流程、由输入电路选择内部流程的机器，以及绕过上述接口的第三方实现，
不能仅凭配方池列表接入。机器进入处理流程前执行的原有前置检查仍然生效；
例如电路装配线在当前模式下的印刻或长度要求也需要满足。
完全隐藏的模式状态、独立加工引擎，以及加工流程外依赖模式的特殊前后处理不作自动兼容保证。

## 验证

在 Windows 开发环境运行：

```powershell
.\gradlew.bat -I src/qa/resources/circuit-pattern-qa.init.gradle :runClient17 --no-configuration-cache --console=plain
.\gradlew.bat -I src/qa/resources/cross-recipe-selector-qa.init.gradle :runClient17 --no-configuration-cache --console=plain
```

夹具使用 `build/circuit-pattern-qa/run` 下的独立游戏实例，通过游戏内事件执行断言。
启动脚本静音，并将测试窗口透明、点击穿透和隐藏任务栏；不对正常游戏实例进行操作。
结果为该目录下的 `result.txt`，通过时保存 `screenshots/circuit-pattern.png` 和 `screenshots/cross-recipe-mirror.png`。
`src/qa` 不进入发行包。

覆盖快捷终端编码和重新编辑、网络载荷往返、通配符展开及倍增、电路模具和单方块推料回归，
以及真实原生控制器的打包/拆包连续加工、缓冲区存档重载、错误配方池不消耗原料、配方锁定、
精密装配机结构等级校验、巨型蒸馏塔目标模式速度与耗电对照。
镜像测试覆盖三类主仓与镜像的 3×3 绑定矩阵、数据棒与复制粘贴、存档及旧连接恢复，
并验证原生打包机同时使用普通总成、电路总成及跨配方池镜像，连续完成四份配方。
用专门的结构夹具覆盖六种总成/镜像的 36 对组合，确认 GT 和 TT 公共结构入口允许不同类型共存；
它验证公共校验流程，不替代每台机器完整结构的逐一搭建测试。
另遍历测试实例已注册的多模式控制器检查配方池选择和模式恢复；该遍历不是所有机器完整结构加工测试。
覆盖使用私有模式字段、会修改状态的 `nextMachineMode`、加工前读取模式参数、完全重写缓存/加工/清理方法的夹具。
核对自动切换与手动切换的耗时、EU/t、耗料、产出，以及清理和异常后的模式、参数恢复。

第二个夹具使用独立的客户端和服务端总成实例打开真实 GUI，通过控件事件选择选项并验证 C2S/S2C 同步。
覆盖候选数量、接口终端命名、保存重载、控制器更换与断开、重新打开、长文字裁剪及 GUI 缩放 1/2/3。
自动命名通过真实按钮事件验证多总成同步，覆盖重复镜像、共享主仓、无关控制器、普通及电路总成排除、
坐标排序、循环分配、名称持久化和重复点击；另对 133 个总成连续分配 32 次并核对每个显示名。
结果及截图位于 `build/cross-recipe-selector-qa/run`。

压力测试使用同一隔离启动脚本，加上 `-PpatternStressTest=true`：

```powershell
.\gradlew.bat -I src/qa/resources/circuit-pattern-qa.init.gradle -PpatternStressTest=true :runClient17 --no-configuration-cache --console=plain
```

覆盖 8 类原生控制器的 16 组机器/配方池组合，24 个消费者共用 8 个满装 540 张样板的总成。
每组运行 6 轮重复投料、交替配方池、双镜像共享和中途 NBT 重载，另验证超过 32 位数量的缓存。
输出在 `build/pattern-recipe-stress-qa/run`：`result.txt`、`stress-summary.txt`、`stress-metrics.csv`、
`stress-operations.csv` 及 `screenshots/pattern-stress.png`。
实测数据、峰值来源和覆盖边界见[压力测试报告](pattern-recipe-stress.md)。

真实附属模组测试复用该压力夹具，添加 `-PpatternAddonTest=true`：

```powershell
.\gradlew.bat -I src/qa/resources/circuit-pattern-qa.init.gradle -PpatternAddonTest=true :runClient17 --no-configuration-cache --console=plain
```

先在 `build/addon-pattern-qa/dependencies` 放入 GTNL、TST 和 GTNLLib 的开发版 JAR（文件名以 `-dev.jar` 结尾）。
本次测试样本为 GTNL `0.2.7-rc2.1`、TST `0.8.0`、GTNLLib `1.1.3`；版本只用于记录可复现环境，
生产适配代码不识别这些版本。启动脚本为测试实例补充运行依赖，不改变主项目发布依赖。

夹具覆盖 `LargePacker`、`LargeBrewer`、`GT_TileEntity_MagneticDrivePressureFormer`、
`TST_ManufacturingCenter`，调用真实控制器的完整 `checkProcessing` 入口。
每个配方池均单独比较手动模式和自动模式的耗时、EU/t、并行、扣料和输出；另检查制造中心的核心等级校验。
仍使用专用测试配方和预置结构参数，不代替完整多方块搭建、逐 tick 完工和 AE 网络回收测试。
报告和截图位于 `build/addon-pattern-qa/run`，文件名与原生压力测试相同。
