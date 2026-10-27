# 自动结构提示接入与验证

结构读取由 `utils.structure.StructureTooltipReader` 完成，显示由
`AutoMultiblockTooltipBuilder` 完成，机器通过
`api.machine.MultiblockTooltipSource` 提供纯计数规则和材料信息。
现有 `GTNRMultiBlockBase` 已实现该接口，旧机器不需要修改接口调用。

## 结构等级与重复层

读取器使用独立的无世界绑定控制器，在投影物品数量 1–64 的范围内调用
`construct(trigger, true)`，记录实际的 `buildPiece` 名称和偏移。
`construct` 中的选级、层数约束与正常投影共用同一逻辑即可。
所有等级的 piece 应登记在同一份稳定的 `IStructureDefinition` 中，
由 `construct` 选择名称和偏移；读取器通过这些调用识别结构变化，
不依赖按等级字段替换同名 piece 的定义。

- 固定结构显示一份尺寸、仓室与材料表。
- 在已有结构上增加部件的等级显示基础结构与后续等级新增的方块。
- 替换为另一套结构的等级各自显示尺寸、控制器位置、仓室与材料表。
- 至少三个不同尺寸沿同一轴等距增长、每次增加相同元素数量时，可显示基础结构与每层材料表。
  带移动顶层或末端的塔、生产线使用这种形式；高度、长度和宽度分别选择对应的投影说明。

多个 `buildPiece` 重叠的坐标只计一次。投影数量被 clamp 到相同结构时不会重复显示该等级。
不要在 `construct` 里放置方块、访问网络、注册机器或执行其他只属于真实结构检查的动作。

累加结构的高等级材料列表仅列新增方块，该等级的仓室要求与外壳总数要求则描述完整结构。
例如二级增加输入仓位置且要求总共两个输入仓，二级段显示完整结构的“两只输入仓”，
不会把它解释为必须在新增部件中额外安装两只仓室。
独立等级在各自的结构详情中显示完整仓室要求。

## 纯计数规则

原有接口继续使用：

```java
void collectTooltipStructureRequirements(List<StructureError> errors)
```

`GTNRMultiBlockBase` 的旧接口仍会调用机器的 `checkMachineHatches` 并保留模块数量校验。
固定的 `checkCasingMin`、`checkHatchMin`、`checkHatchMax`、`checkHatchExact` 等规则继续放在这条纯计数路径里。

等级或层数需要额外上下文时，覆写新增重载：

```java
void collectTooltipStructureRequirements(List<StructureError> errors, int projectorStackSize)
```

默认实现直接委托旧接口。`projectorStackSize` 是首次构造该尺寸的真实投影数量，
不是从 1 开始的结构等级编号，也不是已经 clamp 完成的层数。
例如 `construct` 将数量约束到 3–12 层时，最小结构的参数是 1，下个尺寸的参数是 4。
必须与 `construct` 共用选级或层数计算函数。

若机器的旧纯计数方法读取已有的 `structureTier` 字段，可以在新增重载中准备这个字段后委托旧接口：

```java
@Override
public void collectTooltipStructureRequirements(List<StructureError> errors, int projectorStackSize) {
    int previousTier = structureTier;
    structureTier = tierFromProjectorSize(projectorStackSize);
    try {
        collectTooltipStructureRequirements(errors);
    } finally {
        structureTier = previousTier;
    }
}
```

这里的 `structureTier` 和 `tierFromProjectorSize` 属于机器自身，
与其 `construct`、真实成型检查共用。提示读取只操作独立实例，
不调用完整 `checkMachine`，纯计数路径不得访问世界、放水、连接 AE 网络或登记运行状态。

## 材料等级与投影频道

同一结构存在多个材料版本时，提供三个方法：

```java
@Override
public int getStructureTooltipMaterialTiers() {
    return 2;
}

@Override
public String getStructureTooltipMaterialTierHeader(int tier) {
    // #tr gtnr.example.material_tier
    // # Material tier %s
    // # zh_CN 材料等级 %s
    return StatCollector.translateToLocalFormatted("gtnr.example.material_tier", tier);
}

@Override
public IStructureChannels getStructureTooltipMaterialTierChannel() {
    return GTStructureChannels.BOROGLASS;
}
```

该例表示机器按硼硅玻璃子频道选择两个材料等级。结构元素应使用相同的
`withChannel(GTStructureChannels.BOROGLASS.get(), ...)`。
自定义材料频道返回机器已有的 `IStructureChannels` 对象。
未指定子频道时，材料等级沿用投影主频道数量。
等级数量为 0 表示不按材料等级拆表；等级标题返回 null 会使用通用标题。
只有随材料等级变化的方块进入材料分表，固定材料仍只显示一次。

## 跨材料或随层数增加的外壳最低数

单一外壳的最低数继续从 `checkCasingMin` 自动推断。
若同一个计数器合计多种外壳，或最低数随结构长度变化，提供单独说明：

```java
@Override
public List<String> getStructureTooltipCasingRequirements(int minimum) {
    // #tr gtnr.example.casings.total
    // # At least %s casings in total
    // # zh_CN 各种外壳合计至少 %s 个
    return List.of(StatCollector.translateToLocalFormatted("gtnr.example.casings.total", minimum));
}
```

`minimum` 来自纯计数路径实际传给 `checkCasingMin` 的值，
不再复制一份独立的最低数常量。非空返回值将这条要求单独显示，
避免把跨材料总数错误地写成某一种外壳的最低数。
层数相关的说明还应写清增长公式，并与真实规则复用常量。
空列表保持单材料自动推断；多个无明确归属的计数器不能交给单材料自动推断。

## 每层仓室数量

在机器已有的逐层纯计数检查入口加入记录分支：

```java
if (StructureTooltipHooks.onCheckLayerHatches(this, MIN_LAYERS, 1, HatchElement.OutputHatch)) {
    return;
}
// 下方保留机器已有的逐层输出仓检查。
```

`MIN_LAYERS` 是真实成型允许的最低层数，第二个数值是每层要求的仓室数量。
提示录制时返回 true，并记录“每层 1 个”；正常成型时返回 false，继续原有检查。
这个 hook 负责记录说明，不会替代真实逐层检查。
仅检查仓室总数不能证明每一层都装有仓室，真实规则必须继续核对各层位置。
多个 `IHatchElement` 参数表示这些仓室类型共同满足同一条数量要求。

## 验证入口

`StructureTooltipChecks` 包含固定结构、独立等级、材料等级、累加等级、
纵向与横向重复层、共享外壳总数、异常清理和现有机器回归样例。
`StructureTooltipClientChecks` 在客户端显示真实提示并保存中英文截图。

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp'
.\gradlew.bat -I scripts/structure-tooltip-qa.init.gradle runServer17 --no-configuration-cache
.\gradlew.bat -I scripts/structure-tooltip-qa.init.gradle -Pstructure-tooltipQaClient runClient17 --no-configuration-cache
```

测试结果和截图位于 `build/structure-tooltip-port/qa`；测试类仅由临时 Gradle 入口加载，
不注册到正式机器列表，也不加入发布 JAR。
