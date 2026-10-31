# GT-Never-Reset 开发规则

- Minecraft 1.7.10 / Forge / GTNH addon；根包保持 `com.xyp.gtneverreset`，Mod ID 保持 `gtneverreset`。
- 组织方式以 GTNotLeisure/GT-Not-Leisure PR #663 最终提交 `fdf3f3d0c4ca7a57d18b1421e84b12e8986c11ef` 为基准（2026-10-31）。参考：https://github.com/GTNotLeisure/GT-Not-Leisure/pull/663 。保留本项目根包、功能和兼容标识；后续上游更新先核实实际职责变化。

## 包和文件归位

先按类型职责归位，再组织业务领域。方块、物品、机器、GUI、容器、网络包、配置、配方注册和 loader 保持各自入口；领域内部的状态、持久化、调度和算法放在 `common.crafting`、`common.wireless`、`common.me`、`common.terminal`、`common.flux` 等实际所属系统。允许业务领域包，但不能把该系统的所有入口、GUI 和注册也一起塞进去。

以下路径均位于 `src/main/java/com/xyp/gtneverreset/`，按实际需要创建，不预建空目录：

| 职责 | 默认位置 |
| --- | --- |
| 模组入口、代理 | 根包；保持简短，委托 loader 或对应实现 |
| 配置 | `config.XxxConfig` 独立注解注册，顶层配置字段为静态字段；`ConfigRegistry` 维护分类，`ConfigMigration` 迁移旧文件 |
| 加载和注册编排 | `loader.ContentLoaders` 按固定顺序调度 `IContentLoader`；机器注册细分 `loader.machine` |
| 方块、对应 ItemBlock、TileEntity | 分别为 `common.block.blocks`、其下的 `item`、`tile` |
| 普通物品 | `common.item.items`；物品基类及确有共用职责的基础类型放 `common.item` |
| 单方块机器、多方块、仓室、机器基类 | 分别为 `common.machine.basicMachine`、`multiblock`、`hatch`、`multiMachineBase` |
| 配方实现 | `common.recipe`，按实际目标配方池组织类；统一由 recipe loader 调用 |
| GT 机器、仓室的 ModularUI 界面 | `common.gui.modularui.multiblock`、`hatch`、`singleblock`；控制器保留 UI 入口 |
| 普通方块、物品、覆盖板及 AE 部件的 GUI | `common.gui`；方块、物品和部件保留 UI 入口 |
| 容器 | `common.gui.container`，必要时按界面领域细分 |
| 客户端屏幕、输入、渲染 | `client` 下对应领域，如 `client.wireless`、`client.quantum`、`client.radialmenu`；独立屏幕可放 `client.gui.<领域>`，保持端隔离 |
| 可选模组兼容、NEI / Waila / BQ | `compat.<目标模组>`，可选模组检查后才调用；配方页布局放 `common.gui.recipe` |
| 网络数据包、命令、实体、世界逻辑 | 分别为 `common.packet`、`common.command`、`common.entity`、`common.world` |
| 扩展接口和稳定定义 | `api` 下相应领域；跨领域 Mixin 接口放 `api.mixinHelper`，领域内部接口随所属系统 |
| 状态、算法及辅助实现 | 优先所属业务目录；只有跨系统共用实现保留 `utils`，不依赖 GTNR / GregTech 的通用框架放 GTNRLib |
| 多方块提示生成 | `common.machine.tooltip`；结构资源路径和缓存留在 `utils.structure.MultiblockStructureLoader` |
| 公共枚举 | 现有 `utils.enums` |

- 包名沿用参考项目的职责词汇和目标项目已有大小写；`basicMachine`、`multiMachineBase` 等已有风格不另造一套。保留本项目根包，不复制 `com.science.gtnl`。
- 配方类按实际写入的配方池归并，例如装配机配方统一放在 `AssemblerRecipes`、工作台配方统一放在 `CraftingTableRecipes`；不得按产物所属功能或移植项目拆成 `ModuleHatchRecipes`、`PackagedRecipes` 等注册类。不同配方池通常分别组织；共用同一转换流程的 Modern 无单元配方集中在 `ModernNonCellRecipes`，分别写入对应配方池，不为每个池建立仅转发调用的类。跨配方池共享转换算法放在 `utils.recipes`；专属后端随所属配方池，机器专属处理逻辑随机器，样板与 AE 业务归 `common.me`。配方池稳定定义 `GTNRRecipeMaps` 放在 `common.recipe`，不放 loader。
- 配方子包按目标配方池归属划分：写入 GT 原有配方池的配方放在 `common.recipe.gregtech`，本模组自定义配方池的配方注册类放在 `common.recipe.gtneverreset`；同一功能同时涉及两类配方时拆开放置，不能仅因使用 GT API 或 GT 机器就全部归入 `gregtech`。
- 对照新版 GTNL：GT 多方块、仓室、小机器 GUI 分别归 `common.gui.modularui.multiblock`、`hatch`、`singleblock`。普通方块、物品、覆盖板及 AE 部件 GUI 放在 `common.gui`，复杂领域可细分；共享纹理按使用范围就近放置。使用 ModularUI2 本身不是归入 `modularui` 的依据。
- 参考 GTNL 的局部内部类型做法：只服务一个 GUI 的槽位适配器、控件、同步处理器、预览/选择面板及视图状态，优先写在所属 GUI 中；简短且单次调用的注册包装直接并回调用处，重复布局提取所属 GUI 的私有方法。同一流程的方法及相关内部类型放在一起，不为类别统一拆出 `handler`、`widget`、`sync`、`model` 目录。
- 跨 GUI 复用或确有独立职责的复杂控件、同步实现才保留独立文件，按实际职责和 GTNL 的对应位置安排；不为减少文件数合并无关 GUI 或业务逻辑。Forge `IGuiHandler` 路由类与普通 GUI 入口同放 `common.gui`；跨 GUI、网络和会话共用的 GUI 类型枚举归 `utils.enums`。
- 客户端 GUI 同样按实际使用范围收拢：专用按钮、面板、弹窗、视图状态及界面契约优先放回所属客户端主 GUI，相关生命周期和辅助方法就近排列；独立复杂视图模型按领域保留，被客户端 mixin 引用本身不等于通用库职责。确实被不同界面使用的屏幕基类、按钮和纹理处理保留独立；不要为了合并文件把客户端实现移入公共端，或去掉原有 `@SideOnly(Side.CLIENT)` 隔离。物品专用提示文字及其小型辅助方法归所属物品的客户端提示入口。
- 类名使用领域名加职责名，如 `XxxController`、`XxxGui`、`XxxPacket`、`XxxState`。一个文件一个主要顶层类型，文件名与类型名一致。
- 方块沿用 `BlockXxx`、对应 ItemBlock 沿用 `ItemBlockXxx`，TileEntity 类（含基类）统一使用 `TileEntityXxx` 前缀，放在 `common.block.blocks.tile`。目录或类名调整时同步更新引用，但保留已有注册 ID、存档键和网络标识。
- 多方块形状统一使用 GTNL 的 `MBS1` 二进制 `.mbs`，放在 `src/main/resources/assets/gtneverreset/multiblock/`，通过 `utils.structure.MultiblockStructureLoader` 加载；不再在 Java 类中嵌入形状字符串数组。Java 保留元素映射、结构 piece 名、控制器偏移和成型规则；迁移时逐字符核对空格、层顺序及转置方式。`.mbs` 必须按二进制提交，避免换行转换损坏。
- 不把功能专属逻辑仅改名为 `Utils` 后堆入工具包；不为很小的逻辑创建没有独立职责的 Manager、接口或包装类。小型且只服务外部类的数据类型可用内部类。

## 类与方法的位置

- loader 和代理负责调用与注册；业务规则归机器、TileEntity 或相应管理类；GUI 负责界面与同步；包处理器负责接收和委托。注册方法里不展开整个功能实现。
- 代理类先集中排列初始化和服务端启停生命周期，再放侧别入口及相关辅助方法；同一流程重复取得的注册器使用局部变量。客户端回包处理归对应包的客户端处理入口，保留客户端隔离、主线程调度及连接有效性检查，不在代理中堆叠功能专属回包实现；继续保留 loader 委托和 `common.gui` 中的 GUI 路由。
- 网络数据包集中在 `common.packet`，统一在 `NetWorkHandler#register` 注册，由 `NetworkLoader` 委托；不另设功能专属的注册入口或通道。保留已有通道名、接收侧和包 ID，新增包追加到统一注册列表。
- 配置不再通过 `MainConfig` 聚合实例。各 `config.XxxConfig` 使用 GTNHLib `@Config`、默认值、说明及约束注解，顶层字段静态化，小型子分类保留实例与内部类。`ConfigRegistry` 列出公共端和客户端分类，`ConfigLoader` 处理加载时机。配置文件为 `config/GTNeverReset/{general,item,machine,recipe,client}.cfg`；同一文件可以有多个分类，禁止注册时剔除其他分类。纯客户端分类仅客户端注册、不标记 `@Sync`；客户端保存只保存客户端分类。不另用 NEI 配置标签保存同一设置。旧 `GTNeverReset.cfg` 按类别迁移，现有新文件优先，保留旧文件和字段名、默认值、约束、同步语义及已有语言键。
- 公共库 `gtnrlib` 是独立 Gradle 子模块和 Forge 模组，根包为 `com.xyp.gtnrlib`，Mod ID 为 `gtnrlib`。只放不依赖 GTNR 业务和 GregTech 的通用实现；主模组单向依赖公共库。库拥有自己的资源、Mixin 配置、refmap 和初始化入口，不能从宿主重复注册。主模组生命周期由 `loader.ContentLoaders` 和 `IContentLoader` 统一编排，代理保留侧别入口。有静态注册字段的 loader 用内部 `Card` 延迟初始化，不能因构造登记表提前创建物品。保持原生命周期阶段和注册顺序，不照抄上游的机器注册阶段。
- `MachineLoader` 只作机器注册总入口，依次调用 `loader.machine.HatchLoader`、`MultiblockLoader`、`BasicMachineLoader`；Modern 小机器随 BasicMachineLoader，模块仓随 HatchLoader。不为单台机器建立只转发的 loader。
- Early coremod 入口放 `asm`，late mixin 加载入口放 `mixins`；移动入口时同步构建配置、manifest 和资源中的类路径。空的默认 mixin 配置不声明覆盖加载器的根包，实际 mixin 配置只声明 `mixins.early` / `mixins.late`，避免普通加载器被当作 mixin 类禁止加载。
- 已抽出的轮盘几何、配色和动画复用库的 `gui.wheel`，共享控件和滚动屏幕复用 `gui.widget`，模组检测复用 `mod.ModList`，方块变化监听复用 `world.WorldListener`。业务枚举仍放主模组 `utils.enums`；轮盘动作、脚本、配置模型与存储归主模组 `client.radialmenu`，屏幕和导航栈归 `client.gui.radialmenu`；无线网络数据归 `common.wireless`。结构路径与缓存仍由 `MultiblockStructureLoader` 管理，MBS1 解码委托库的 `utils.StructureFileCodec`。
- 图集解析、文件图标及玩家头像缓存复用 GTNRLib `client.render.icon`，每个调用方持有自己的缓存实例；资源命名空间、目录和日志由主模组传入。轮盘动作与配置留在主模组，按键模拟、修饰键和持续按键状态复用库的 `client.input`，主模组负责 tick、离开世界和切换配置时的释放。`KeyBindingAccessor` 仅由库的客户端 early mixin 注册。Lua 词法分析复用 `text.syntax.LuaSyntax`，主模组 `ScriptSyntax` 只声明自己的 API 词表，脚本执行和权限留在主模组。
- 新类默认按“常量和字段 → 构造方法 → 生命周期及主要对外行为 → 辅助方法 → 局部内部类型”排列；同一流程的方法、重载及紧密相关的辅助方法相邻。修改已有类时先沿用同类附近的顺序，不机械按字母或可见性重排。
- 多方块控制器及公共机器基类统一以 `common.machine.multiMachineBase.MachineBaseLayoutTemplate` 为排布模板，优先于上面的通用成员顺序：静态常量与静态工具方法 → 持久化字段 → 运行时字段 → 构造器 → 生命周期 → 结构检查与参数准备 → 配方与处理主流程 → 输入、输出、仓室与贴图 → 信息展示 → UI → NBT 与客户端同步 → 能力开关与策略 Getter → 底层辅助方法与内部类型。同一职责连续排列，覆写方法按职责归位；辅助方法和重载跟随所属流程，未使用区域可以省略。调整字段位置必须核对初始化依赖。模板仅作排布参考，不注册、不承载业务，也不要求机器继承它。
- 类和接口用大驼峰，普通方法与字段用小驼峰，常量用大写下划线。已有 API、覆盖方法和兼容性名称按原约定保留。
- 该成员顺序是本项目后续生成代码的默认约定；参考项目存在历史差异，不应声称其全仓采用严格统一的顺序。
- 成员顺序通过实际排列体现，不添加“配方与处理主流程”等分区标题注释；连续字段不逐条插入空行，仅在必要的逻辑分组、方法之间保留空行。

## 第三方许可

- 许可文本和来源记录统一放在仓库根目录的 `licenses/` 下，不在 Java 源码中添加许可或来源注释，不放在 `src/main/resources` 中。需要随发行包分发的声明由构建任务从 `licenses/` 加入 JAR。

## 格式与验证

- 格式以本项目 `.editorconfig` 和 Gradle 提供的 Spotless 配置为准：Java 使用 4 空格、UTF-8、LF、末尾换行；导入、花括号及长调用换行交给 formatter。参考项目为 120 列，不为模仿外观替换本项目构建配置。
- 每次执行 Git 提交前，先运行 `./gradlew.bat spotlessApply`，再运行 `spotlessCheck` 和本次修改对应的构建验证；检查格式化产生的改动后再暂存、提交。
- 用户要求“提交”时只创建本地 Git 提交；只有用户明确要求“推送”时才推送到远程。
- 新代码先确定每个类的职责和文件落点，再实现；交付时核对是否仍有 UI、状态、注册等不同职责挤在同一功能包或大类中。
- Java 修改运行项目 Spotless 检查及 `compileJava`；资源修改补充 `processResources`。Git 可用时检查 `git diff --check`，当前目录没有 Git 元数据时如实说明。

## 语言键与翻译

- 多方块的通用参数、等级加成和结构信息由 `AutoMultiblockTooltipBuilder` 自动生成；特殊说明维护在 `src/main/resources/assets/gtneverreset/lang/<语言>/tooltip/<机器名>.md`；结构说明不另建 Markdown，模块数量等规则与成型校验共用数据。通过 `MultiblockTooltipSource` 声明文档路径和 `{var:name}` 数值，不在 Markdown 重复维护能够自动读取的参数或结构数量。这些 Markdown 文案直接维护中英文，不走下方的 Java `#tr` 生成流程；其他语言键继续遵守原规则。

- 沿用 GTNG 的 `#tr` 注释流程，在 Java 中使用语言键的位置紧邻上方声明译文，不直接编辑生成的 `.lang` 文件，不集中堆在类头。每条译文保持单行，避免 formatter 折行破坏提取。

```java
// #tr gui.example.key
// # English text
// # zh_CN 中文文本
```

- 未标语言的译文默认为 `en_US`；可用 `{\\RED}`、`{\\RESET}`、`{\\SPACE}` 等占位符表示 Minecraft 格式码和空格。
- `preprocessLangInJavaFiles` 从主 Java 源集重建 `src/main/resources/assets/gtneverreset/lang/*.lang`，已声明语言缺译时回退英文；`processResources` 和 `sourcesJar` 自动先运行生成任务。
- 自定义构建逻辑放 `addon.gradle`；格式参数校验在生成时运行，`checkTranslationFormatValidator` 已接入 `check`。
