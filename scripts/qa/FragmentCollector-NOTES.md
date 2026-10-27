# 碎片世界采集器验证

GTNG 来源：`048fd1963de8c6458ef6f800c411be0087cbafdb`。机器 ID 为 `29453`，
机器、GUI、配方和 NEI 按 GTNR 职责包归位。配方数据、六面纹理及工作动画保持原字节；
来源与许可见 `src/main/resources/META-INF/fragment-world-collector-port/`。

## 运行

```powershell
.\scripts\dev\Invoke-Gradle.ps1 spotlessApply spotlessCheck compileJava processResources runClient17 --init-script scripts/fragment-collector-client-qa.init.gradle --offline --no-configuration-cache --console=plain
```

QA 使用独立世界和目录 `build/fragment-collector-client-qa`，静音并隐藏代理启动的客户端。
测试运行时额外加载固定版本 Galacticraft、GalaxySpace 及隔壁已有的 Et Futurum dev JAR；
可通过 `-PfragmentCollectorEtFuturumJar=<路径>` 指定该 JAR。
正式依赖不因 QA 增加。报告在 `result.txt`，逐条配方在 `recipes.txt`，截图在 `screenshots/`。

## 2026-10-27 实测

- 隔离客户端运行成功，`result.txt=PASS`；239 条已注册配方覆盖 16 个维度，全部通过真实控制器的 `checkRecipe`。
- 验证催化维度方块保留、缺少选择器拒绝、输出堵塞、配方缓存切换、MAX 流体再加 1 的溢出保护及失败时钻头保留。
- 3000 次钢钻头配方消耗 33 个钻头；2000 次 5% 岩浆配方命中 103 次。基础耗电 8 EU/t 和原耗时保留。
- 完整 TileEntity NBT 恢复正在进行的任务，真实 `onPostTick` 恰好完成一次并生成已掷定产物。
- 验证向箱子和 LV 流体输入仓自动输出、禁用开关、满箱保护及设置持久化。
- 原生自动输出开关和 Shift 输出面选择器通过控件交互及同步到达服务端，重开界面保留设置。
- GUI 比例 1/2/3、12 个输出槽、4 个 NEI 页面和运行中的机器共 9 张截图已人工查看，无缺纹理或裁切。
- 16 条维度展示块获取配方与源 MixerRecipes 逐项对比，火箭等级、原电路号、圆石数量、LV 电压及 5 秒耗时一致。
- 7 个资源文件及配方 JSON 的 SHA-256 与来源清单一致；Spotless、`compileJava`、`processResources` 通过。

此客户端运行使用集成服务端；GUI 检查通过控件回调驱动，未重放物理鼠标点击或独立远端联机。
可选模组未提供的产物沿用源配方的可用性过滤。

客户端通过后，共享目录中的几次整包复查遇到并行编辑及构建覆盖，失败日志已保留。
改用独立构建目录 `build/fragment-collector-final` 后，最终 `spotlessCheck`、`compileJava`、
`processResources`、`checkTranslationFormatValidator`（21 项）和 `assemble` 全部通过，
包括字节码降级和重混淆；完整日志为 `build/fragment-collector-isolated-final.log`。
正式 JAR 核对包含机器、GUI、配方、7 个贴图/动画文件及许可证，基础类为 Java 8 字节码，未混入 QA 类。
未改动或回退其他聊天的并行实现。
