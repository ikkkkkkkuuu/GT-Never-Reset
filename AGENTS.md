# GT-Never-Reset 开发规则

- Minecraft 1.7.10 / Forge / GTNH addon；根包保持 `com.xyp.gtneverreset`，Mod ID 保持 `gtneverreset`。
- 用户指定以相邻的 `../GT-Not-Leisure-dev-290` 为代码组织和格式参考。以下规则适用于今后新增和修改的代码；已有功能的大规模迁移另按实际任务处理。

## 包和文件归位

先按职责确定包，再在该职责下按需要细分对象或领域。一个新功能可以跨多个职责包，通过一致的名称关联，不能把它的方块、物品、机器、GUI、网络、数据、配方和注册全部塞进 `common.<功能名>`、`common.item.<功能名>` 或 `common.block.<功能名>`。

以下路径均位于 `src/main/java/com/xyp/gtneverreset/`，按实际需要创建，不预建空目录：

| 职责 | 默认位置 |
| --- | --- |
| 模组入口、代理 | 根包；保持简短，委托 loader 或对应实现 |
| 配置 | `config.MainConfig`；对照 GTNL，用 GTNHLib 注解和内部分类集中声明 |
| 加载和注册编排 | `loader` |
| 方块、对应 ItemBlock、TileEntity | 分别为 `common.block.blocks`、其下的 `item`、`tile` |
| 普通物品 | `common.item` |
| 单方块机器、多方块、仓室、机器基类 | 分别为 `common.machine.basicMachine`、`multiblock`、`hatch`、`multiMachineBase` |
| 配方实现 | `common.recipe`，按配方所属系统细分；统一由 recipe loader 调用 |
| GT 机器、仓室的 ModularUI 界面 | `common.gui.modularui`；控制器保留 UI 入口 |
| 普通方块、物品、覆盖板及 AE 部件的 GUI | `common.gui`；方块、物品和部件保留 UI 入口 |
| 原版容器 | `container` |
| 客户端屏幕、渲染、NEI | `client` 下对应职责包；客户端类型不得由公共端直接加载 |
| 网络数据包、命令、实体、世界逻辑 | 分别为 `common.packet`、`common.command`、`common.entity`、`common.world` |
| 扩展接口和稳定定义 | `api` 下相应领域；不要把内部实现都当 API |
| 状态、算法及复用支持逻辑 | 复用现有对应职责包，必要时在 `utils.world`、`utils.machine`、`utils.recipes` 等下细分 |
| 公共枚举 | 现有 `utils.enums` |

- 包名沿用参考项目的职责词汇和目标项目已有大小写；`basicMachine`、`multiMachineBase` 等已有风格不另造一套。保留本项目根包，不复制 `com.science.gtnl`。
- 配方子包按目标配方池归属划分：写入 GT 原有配方池的配方放在 `common.recipe.gregtech`，本模组自定义配方池的配方及专用后端放在 `common.recipe.gtneverreset`；同一功能同时涉及两类配方时拆开放置，不能仅因使用 GT API 或 GT 机器就全部归入 `gregtech`。
- 对照 GTNL 的实际分类：`common.gui.modularui` 根层放完整的 GT 机器、仓室 GUI 类；普通方块、物品、覆盖板及 AE 部件 GUI 放在 `common.gui` 根层，共享 GUI 纹理定义也放在这一层。使用 ModularUI2 本身不是归入 `modularui` 的依据。
- 参考 GTNL 的局部内部类型做法：只服务一个 GUI 的槽位适配器、控件、同步处理器、预览/选择面板及视图状态，优先写在所属 GUI 中；简短且单次调用的注册包装直接并回调用处，重复布局提取所属 GUI 的私有方法。同一流程的方法及相关内部类型放在一起，不为类别统一拆出 `handler`、`widget`、`sync`、`model` 目录。
- 跨 GUI 复用或确有独立职责的复杂控件、同步实现才保留独立文件，按实际职责和 GTNL 的对应位置安排；不为减少文件数合并无关 GUI 或业务逻辑。Forge `IGuiHandler` 路由类与普通 GUI 入口同放 `common.gui`；跨 GUI、网络和会话共用的 GUI 类型枚举归 `utils.enums`。
- 客户端 GUI 同样按实际使用范围收拢：专用按钮、面板、弹窗、视图状态及界面契约优先放回所属客户端主 GUI，相关生命周期和辅助方法就近排列；被客户端 mixin 引用本身不等于跨界面复用。确实被不同界面使用的屏幕基类、按钮和纹理处理保留独立；不要为了合并文件把客户端实现移入公共端，或去掉原有 `@SideOnly(Side.CLIENT)` 隔离。物品专用提示文字及其小型辅助方法归所属物品的客户端提示入口。
- 类名使用领域名加职责名，如 `XxxController`、`XxxGui`、`XxxPacket`、`XxxState`。一个文件一个主要顶层类型，文件名与类型名一致。
- 不把功能专属逻辑仅改名为 `Utils` 后堆入工具包；不为很小的逻辑创建没有独立职责的 Manager、接口或包装类。小型且只服务外部类的数据类型可用内部类。

## 类与方法的位置

- loader 和代理负责调用与注册；业务规则归机器、TileEntity 或相应管理类；GUI 负责界面与同步；包处理器负责接收和委托。注册方法里不展开整个功能实现。
- 网络数据包统一在 `common.packet.NetWorkHandler#register` 注册，由代理调用；不另设功能专属的注册入口或通道。保留已有通道名、接收侧和包 ID，新增包追加到统一注册列表。
- 配置沿用 GTNL 的 `MainConfig`、静态分类实例和内部分类类结构，使用 GTNHLib 的 `@Config`、默认值、说明及约束注解；由 `loader.ConfigLoader` 统一注册和保存。只创建已有功能需要的分类，纯客户端设置不标记 `@Sync`，不另用 NEI 配置标签保存同一设置。
- 新类默认按“常量和字段 → 构造方法 → 生命周期及主要对外行为 → 辅助方法 → 局部内部类型”排列；同一流程的方法、重载及紧密相关的辅助方法相邻。修改已有类时先沿用同类附近的顺序，不机械按字母或可见性重排。
- 类和接口用大驼峰，普通方法与字段用小驼峰，常量用大写下划线。已有 API、覆盖方法和兼容性名称按原约定保留。
- 该成员顺序是本项目后续生成代码的默认约定；参考项目存在历史差异，不应声称其全仓采用严格统一的顺序。

## 格式与验证

- 格式以本项目 `.editorconfig` 和 Gradle 提供的 Spotless 配置为准：Java 使用 4 空格、UTF-8、LF、末尾换行；导入、花括号及长调用换行交给 formatter。参考项目为 120 列，不为模仿外观替换本项目构建配置。
- 每次执行 Git 提交前，先运行 `./gradlew.bat spotlessApply`，再运行 `spotlessCheck` 和本次修改对应的构建验证；检查格式化产生的改动后再暂存、提交。
- 用户要求“提交”时只创建本地 Git 提交；只有用户明确要求“推送”时才推送到远程。
- 新代码先确定每个类的职责和文件落点，再实现；交付时核对是否仍有 UI、状态、注册等不同职责挤在同一功能包或大类中。
- Java 修改运行项目 Spotless 检查及 `compileJava`；资源修改补充 `processResources`。Git 可用时检查 `git diff --check`，当前目录没有 Git 元数据时如实说明。

## 语言键与翻译

- 沿用 GTNG 的 `#tr` 注释流程，在 Java 中使用语言键的位置紧邻上方声明译文，不直接编辑生成的 `.lang` 文件，不集中堆在类头。每条译文保持单行，避免 formatter 折行破坏提取。

```java
// #tr gui.example.key
// # English text
// # zh_CN 中文文本
```

- 未标语言的译文默认为 `en_US`；可用 `{\\RED}`、`{\\RESET}`、`{\\SPACE}` 等占位符表示 Minecraft 格式码和空格。
- `preprocessLangInJavaFiles` 从主 Java 源集重建 `src/main/resources/assets/gtneverreset/lang/*.lang`，已声明语言缺译时回退英文；`processResources` 和 `sourcesJar` 自动先运行生成任务。
- 自定义构建逻辑放 `addon.gradle`；格式参数校验在生成时运行，`checkTranslationFormatValidator` 已接入 `check`。
