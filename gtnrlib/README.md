# GTNRLib

Minecraft 1.7.10 的通用支持库，Mod ID 为 `gtnrlib`，Java 包为 `com.xyp.gtnrlib`。GT-Never-Reset 通过 Gradle 项目依赖使用本模块；库不依赖 GT-Never-Reset 或 GregTech。

- `text`：文字标记解析、样式、原版格式码处理、保留颜色和样式的提示行折行（可指定宽度）。
- `client/text`：文字布局、遮罩缓存、渲染器、着色器和可选 Angelica 对接。
- `client/gui`：ModularUI2 像素字体屏幕，以及颜色选择、确认框、文本输入、多行编辑、按键选择屏幕。
- `client/input`：按键查询、LWJGL 键盘缓冲访问、原版视角和流畅镜头按键效果。
- `client/render/icon`：原版药水图标的注册名、索引和纹理坐标。
- `gui/wheel`：轮盘几何、弧形排布、槽位布局、配色继承与展开动画计算。
- `gui/widget`：共享面板绘制、滚动屏幕、列表、按钮和拖动排序控件，均保留客户端隔离。
- `config/json`：枚举及多行字符串映射的 Gson 适配器。
- `world`：按世界与区块管理的方块变化监听，以及保留客户端隔离的区块坐标、朝向和世界时间换算。
- `mod`：模组 ID、名称、加载检测和资源地址。
- `utils`：颜色混合与透明度计算、饱和长整数运算、无中间加法溢出的向上整除、Java 8 有序只读集合快照、MBS1 结构解码。
- `mixins`：库自己的字体注入和加载入口，同时识别旧 GTNL 与独立 GTNLLib 的 Angelica 补丁。
- `assets/gtnrlib/shaders/text`：库自己的着色器资源。

`GTNRLib` 自行注册 `WorldListener.INSTANCE`、初始化渲染器和资源重载监听；使用方无需重复注册。文字协议的不可见标记以及历史样式 ID `gtneverreset:exotic_rainbow` 保持不变，宿主原有着色器资源路径保留供兼容使用。

轮盘动作、配置文件、界面导航栈及业务编辑器仍在宿主。宿主的 `RadialUiScreen` 以及五种通用弹窗入口仅为库屏幕接回 `RadialGuiStack`；其他使用方可以覆写 `onCancel()` 指定返回行为。无线电网 HUD 的数据、等级和网络请求留在宿主。`MultiblockStructureLoader` 继续拥有原资源路径和缓存，只将二进制解码委托给 `StructureFileCodec`。既有注册 ID、配置字段、JSON 枚举值及 `.mbs` 资源保持不变。

主模组的传送执行复用 `TeleporterUtils`，安全落点搜索和传送权限仍由宿主管理；轮盘配色复用 `Colors`；合成数量、计数器和无线入口预算的整数计算复用 `LongMath`。保留宿主的非法需求过滤等业务规则，不在库中加入 AE 或机器策略。

继续留在宿主的边界：`KeyInjector` 的借键配置及 Mixin 协作、Lua 编辑器的轮盘专用词表、用户头像和图集的配置目录/重载策略、AE 合成图与无线网络状态。它们虽包含辅助计算，但并非独立通用实现。

使用 `./gradlew.bat :gtnrlib:build` 独立编译、检查和打包，产物在 `gtnrlib/build/libs`。根项目 `assemble` 将库的正式包、开发包和源码包收集到根目录 `build/libs`。实际安装需要 GTNRLib、GTNHLib 和 ModularUI2（以及其运行依赖 Baubles Expanded）；GTNH 开发基准已有这些依赖。Angelica 是可选对接。

启动任务使用完整路径：主模组为 `:runClient25` / `:runServer25`，独立库为 `:gtnrlib:runClient25` / `:gtnrlib:runServer25`。不要使用未限定的同名启动任务，以免同时启动两个项目。

`checkLibraryContracts` 在不包含宿主和 GregTech 的测试路径中验证文字协议、格式边界、轮盘命中、动画、JSON 兼容、长整数边界、损坏结构文件拒绝、监听失效及源码依赖方向。新增通用代码必须继续保持这个边界。

已完整移植上游 `refactor-trial/gtnllib`（`6dffa94077cd8dbe319f84ac72884a42c55ea1c0`）的通用框架，并与上述本地 API 合并：

| 包 | 能力 |
| --- | --- |
| `client/hud` | HUD 声明、注册、布局保存、拖动编辑、设置、撤销重做 |
| `client/fx`、`client/shader` | 缓动、2D 图形、模糊、GLSL、世界电弧、冲击波、粒子与可见性管理 |
| `gui/theme`、`gui/layout` | 配色、实时主题、金属皮肤、尺寸与布局计算 |
| `gui/widget`、`gui/settings`、`gui/wheel` | 滑块、开关、卡片、虚拟物品槽、设置表单、坐标输入和通用轮盘 |
| `client/text`、`text` | 24 种文字特效、参数化格式、别名、预览、输入框与换行支持 |
| `client/tooltip` | Shift/Ctrl 提示与动态文字、物品替身解析 |
| `registry`、`network` | 物品方块注册器、双向数据包基类；频道与包 ID 仍由使用方管理 |
| `command`、`utils` | 物品信息、实体选择器、传送、数学、向量、MBS1 编解码等 |

库客户端配置位于 `config/GTNeverReset/GTNRLib/gtnrlib.cfg`，HUD 布局位于同目录 `hud.json`。从 Mods → GTNRLib → Config → Edit HUD 打开编辑器；编辑器只列出使用方注册的 HUD 元素，不自动接管宿主已有 HUD。

命令为 `/gtnr_texteffects`、`/gtnr_iteminfo`、`/gtnr_suicide`，不注册上游的同名短命令。主题键、绘制类型、资源、语言键和 Mixin 成员使用 GTNRLib 命名空间。字体补丁兼容旧 GTNL 和独立 GTNLLib；已存在上游 Angelica 抗锯齿重定向时复用它，避免重复重定向。上游 `gtnllib:` 显式文字格式交由其自身处理。

新翻译维护在 Java 使用处的 `#tr` 注释，由本模块 `preprocessLangInJavaFiles` 生成。库的 `checkLibraryContracts` 同时验证 HUD、主题、布局、特效、注册器和原有本地 API。

内部实现统一由以下入口维护，原有公开调用入口仍然可用：

- `Colors`：共享 RGB 通道、混白、亮度、HSL、对比度、插值及透明度计算。`ColorMath` 保留客户端 GL/Tessellator 方法和数值 API 的兼容转发；公共端无需加载它。
- `MathUtils`：轮盘和朝向共用角度归一化，动画共用单位区间限制；保留负零及 NaN/无穷值行为。
- `FxEasing.smoothStep`：屏幕/世界特效与青色文字火花共用缓动实现。
- `ImmutableCollections`：HUD 选项和元素使用同一套只读快照；注册表的实时只读视图仍保留原语义。

相似接口的差异有意保留：`Colors.scale` 是百分比亮度（超过 100 时混白），`scaleRgb` 是通道乘法；`blendRgb` 保留源 alpha 并按加权公式舍入，`lerpRgb` 使用原上游差值公式且只返回 RGB；`fadeAlpha` 截断，`scaleAlpha` 四舍五入。双精度角度轮盘与单精度弧度轮盘、HSV 选择器与 HSL 主题、字符单元提示折行与像素文字排版也分别保留。结构读取的流所有权、文字协议及 shader 状态恢复不变。

`SharedImplementationContracts` 使用合并前保存的结果指纹比较 10,000 组颜色及边界输入，并覆盖舍入差异和 HUD 快照隔离；不能因重构而更新这些预期结果。

本次拆分的来源记录在仓库根目录 `licenses/gtnrlib/`，构建时随 JAR 分发。

样板通配符界面复用库的 `ScrollingDropdown`、`ItemStackSlot` 和 `ModernGuiSkin`：下拉框保留静默赋值、浮层关闭与长文字滚动；物品槽支持只读预览、动态物品提示和保留 NBT 的单个物品副本回调；皮肤由调用方提供资源命名空间和目录。原有现代皮肤贴图仍在宿主，布局、材料／流体转换、样板预览、NEI 配方导入和服务端保存也留在宿主。`GhostItemSlot` 的无 NBT 物品标识语义和 `CycleDropdown` 的滚轮切换语义保持不变。
