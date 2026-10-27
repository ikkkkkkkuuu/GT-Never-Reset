# 自动性能提示接入与验证

性能采集由 `utils.structure.MultiblockPerformanceReader` 负责，结果为
`PerformanceSummary`，`AutoMultiblockTooltipBuilder` 负责显示。
机器继续实现 `api.machine.MultiblockTooltipSource`；现有四字段
`TooltipStats(parallels, speed, energyUsage, perfectOverclock)` 保持兼容。

## 复用真实公式

`GTNRMultiBlockBase` 的默认统计读取 `getBaseParallelRecipes`、
`getBaseSpeedMultiplier`、`getBaseEUtMultiplier` 与 `hasPerfectOverclock`。
它们也参与真实处理逻辑，因此新机器应复用这些公式，而不是再写一套说明数值。
若已经覆写 `getTooltipStats`，原覆盖继续生效。

部件等级计算用 `getTooltipTier(part, runtimeValue)` 读取：

```java
@Override
protected int getBaseParallelRecipes() {
    int voltageTier = Math.max(1, getTooltipTier(TieredPart.VOLTAGE, GTUtility.getTier(getMaxInputEu())));
    return (int) Math.min(Integer.MAX_VALUE, 64L << Math.min(30, voltageTier - 1));
}

@Override
protected double getBaseSpeedMultiplier() {
    int coilTier = getTooltipTier(TieredPart.COIL, installedCoilTier);
    return Math.pow(2, coilTier);
}
```

正常运行时它返回机器的实际字段值；提示采集时只替换被探测的部件，
其余部件等级为 0。需要最低有效等级的公式应和正常计算共用同一限幅逻辑。
`VOLTAGE` 探测同时设置 `getMaxInputVoltage` 和 `getMaxInputEu` 的返回值，
避免依赖总 EU 的公式与依赖电压的公式读到不同状态。
嵌套与异常通过 `finally` 恢复作用域；直接探测已绑定 TileEntity 的控制器会被拒绝。
`MultiblockPerformanceReader` 对每次采样创建独立实例，实际建在世界中的机器不被修改。

无线或自定义能源机器若覆写能源 getter，应保留基类的探测分支：

```java
@Override
public long getMaxInputEu() {
    if (isTooltipProbeActive()) return super.getMaxInputEu();
    return readActualWirelessPower();
}

@Override
public long getMaxInputVoltage() {
    if (isTooltipProbeActive()) return super.getMaxInputVoltage();
    return readActualWirelessVoltage();
}
```

`readActualWirelessPower` 与 `readActualWirelessVoltage` 代表机器原有的实际取值方法。
不要为了探测改动真实无线网络、电流上限或能源规则。
## 部件、范围与加成

可选部件为 `VOLTAGE`、`COIL`、`GLASS`、`CASING`、
`HIGH_PRESSURE` 和 `PARALLEL_CONTROLLER`。
普通基类机器只需在真实公式中读取对应探测等级。
不继承 `GTNRMultiBlockBase` 的机器可实现 `getTooltipStatsAtTier(part, tier)`，
返回某一部件等级对应的真实统计；不支持的部件返回 null。

通过 `getTooltipTieredParts` 限定需要探测的部件；
通过 `getTooltipTierRange` 提供含首尾的有效等级范围。
读取器验证整个范围的相邻差值或倍率，不把只在低等级成立的关系写成全等级加成。
没有稳定差值或倍率时不生成固定的每级数字。
速度倍率不呈线性变化时，还会检查其倒数，即配方时间系数。
`HIGH_PRESSURE` 按一次切换比较，不当作无限重复的等级。

完美超频门槛来自统计的布尔值：只有从某一级到有效最高级都保持开启，
才会显示“该等级及以上”的门槛。
单独某一级开启、下一等级关闭的情况不会被错误描述为持续门槛。
NaN 或 Infinity 表示不能用这一静态数值描述该机器，显示时省略相应数字。

## 从真实超频计算器读取温度规则

`GTNRMultiBlockBase` 使用机器实际的 `RecipeThreadProcessingLogic` 工厂，
经 `createTooltipOverclockCalculator(recipe)` 调用同一个
`createOverclockCalculator(recipe)`。
只读取计算器配置，不搜寻配方、不消耗输入、不计算进度。
提示不执行会访问世界的 setupProcessingLogic 链；只放在该链中的自定义参数，
应提取为与真实 calculator 工厂共用的 getter/配置方法，或显式覆写 Source 的规则接口。
只读 mixin 取得温度超频、温度折扣、折扣因子、机器温度、配方温度及电压越级限制。

例如机器原本具有如下热处理逻辑时，提示可自动识别：

```java
@Override
protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
    return new RecipeThreadProcessingLogic() {
        @Override
        protected OverclockCalculator createOverclockCalculator(GTRecipe recipe) {
            return super.createOverclockCalculator(recipe)
                .setMachineHeat(readMachineHeat())
                .setRecipeHeat(recipe.mSpecialValue)
                .setHeatOC(true)
                .setHeatDiscount(true);
        }
    };
}
```

`readMachineHeat` 在此代表机器已有的真实温度方法，不能为提示另外写假温度。
两份具有不同 `mSpecialValue` 的测试配方用于判断计算器是否实际读取配方温度。
固定机器温度与固定配方温度产生的免费完美超频按真实差值计算。
GT 的温度折扣步长为 900K，温度完美超频步长为 1800K；
折扣倍率读取计算器配置，允许机器真实使用不同于 0.95 的倍率。
已全程完美超频的机器保留温度折扣说明，省略重复的温度超频说明。

低于配方温度时能否运行、是否惩罚，属于机器真实配方检查与计算器的行为。
普通 GT 计算器不会自动把负温差折扣次数夹到 0，提示接口不会改写这个行为。
只实现提示接口不会给机器启用新的温度折扣或超频能力。

## 电压升温与越级

只有真实温度公式已经使用相同规则时，才覆写
`getVoltageHeatPerTier` 与 `getVoltageHeatFromTier`。
`coilAndVoltageHeat(installedCoil)` 可供实际温度方法与提示共用，
它使用真实线圈热量、总输入 EU 的电压等级和同一组每级升温常量。

`getTooltipTierSkips` 默认从真实计算器读取。
普通 GT 的高一级规则保持隐式；零级、其他固定级差与不限等级会显示对应说明。
不使用这套 ProcessingLogic 的机器可自行提供 `HeatRules` 和 `OptionalInt`。

## 验证入口

```powershell
New-Item -ItemType Directory -Path 'D:/gtnr-tmp' -Force | Out-Null
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=D:/gtnr-tmp -Djava.io.tmpdir=D:/gtnr-tmp'
.\gradlew.bat -I scripts/multiblock-performance-qa.init.gradle runServer17 --no-configuration-cache
.\gradlew.bat -I scripts/multiblock-performance-qa.init.gradle -Pperformance-tooltipQaClient runClient17 --no-configuration-cache
```

测试包含真实 GT 900K/1800K行为、静态统计兼容、探测作用域恢复、
全范围加成拟合、完美超频门槛、实际处理工厂的温度及越级配置，
并重跑 `StructureTooltipChecks` 的结构回归。
客户端保存中英文性能页面；测试类与临时世界不进入发布 JAR。
