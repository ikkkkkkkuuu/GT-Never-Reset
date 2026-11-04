# GTNRLib 动画 API 0.1.22

## 在 1.7.10 运行 Hold My Items 5.1.1

下载 [HMI 5.1.1（Minecraft 1.21.11，KZm1qTcU）](https://modrinth.com/mod/hold-my-items/version/KZm1qTcU)，将原始 JAR 放在测试实例的 `resourcepacks` 目录。打开 GTNRLib 客户端配置，在“仅导入动画的资源包”中填写完整文件名（含 `.jar`），进游戏按 F3+T。清空该项并重载即可退出 HMI 模式。GTNRLib 会校验原包 SHA-256。这个模式无需解包或安装 Fabric。

GTNRLib 用独立的 LuaJ 环境运行原包的四段程序。每次绘制先采样玩家输入与挥动，再分别求主手、物品和辅助手的矩阵。握持、挖掘、攻击、使用和收回共用脚本的持续状态。显示辅助手不占用额外的物品槽。原包模式优先于已选的 Punchy 资源包，显式绑定到具体物品的第三方动画仍可接管该物品。退出、重新加载和切换世界都会重置相应状态。

- 空手、普通物品、剑、镐、斧、锄、铲、方块、弓、钓竿、雪球、食物、药水与地图均使用 1.7.10 的输入。走动、下落、攀爬、潜行、接触水、视角变化和换物会进入 Lua 的对应分支。按住 J 可检视。
- 5.1.1 原包的 35 个物品模型入口均可按第一人称上下文读取。船、运输船、竹筏、桶、花瓣及末地水晶使用原包的模型、显示变换和纹理。液面、桶中生物、运输船的箱盖由原包零件脚本驱动。喝奶按已使用时长选择模型，美西螈桶按变体选择外观。
- Et Futurum 的同名船、筏、末地水晶以及灯笼、灵魂灯笼、灵魂火把、黏液块和蜂蜜块会匹配对应的上游物品。火把、灯笼和岩浆桶可显示脚本生成的附着光晕。挥动粉红花瓣、野花或枯叶时，适配器按脚本生成屏幕空间粒子，并执行其逐帧回调。
- Forge 自定义物品和地图默认沿用提供者的渲染路径。使用标准手持贴图坐标的自定义渲染器可用 `HmiItemBindings.registerTool` 显式接入 HMI 握姿。适配器按物品栈和渲染器筛选，可在 `pose` 提供物品局部坐标下的腕部变换，手和工具会跟随同一变换。Forge 仍负责调用原渲染器绘制完整工具。
- 启用 HMI 后，Forge 矿物词典登记的锭（如 `ingotCopper`、`ingotHotTitanium`）沿用原版金锭、铁锭的握姿。识别按手中的物品和元数据进行，各模组仍绘制自己的纹理、材质颜色和叠层。Punchy 资源包可通过 `GTNRLIB_FLAT_ITEM` 为这些锭设置握姿。锭形模具、金属板和粒按各自的矿词分类处理。
- HMI 和 Punchy 共用工具类型识别。原版工具基类、Forge 采掘类型和 craftingTool 矿词可选择剑、镐、斧、铲、锄或普通手工具的握持与挥动；实际堆栈仍交给原渲染器。安装 GT-Never-Reset 0.1.21 后，还会识别 GT 元工具、匠魂近战/采掘工具和 IC2 电动工具。例如电钻沿用镐、电锯沿用斧，锉刀等使用普通手工具动作。电机、链条、充能等专属动作留待后续。没有可识别类型的自定义物品继续使用原路径。
- 同时安装 GT-Never-Reset 0.1.21 并启用本地 HMI 原包后，普通 GT 手动扳手沿用默认手持姿势。服务端确认扳手转动后只通知操作者，连续拧动会从上一段运动淡入。材质由 GT 绘制。拧腕曲线可用资源包覆盖 `assets/gtneverreset/animations/wrench_turn.json`，按 F3+T 重载。电动扳手使用通用工具挥动。工具箱仍由原模组处理。
- 同时安装 GT-Never-Reset 0.1.22 并启用本地 HMI 原包后，普通 GT 手动剪线钳由双手各握一柄。左键向前试剪，右键由 GT 机器或电缆确认剪线钳操作后播放。连续挥动最多预留下一剪。资源包可覆盖 `assets/gtneverreset/animations/wire_cutter.json` 的前伸位移和钳口转角。铰点按 GT 标准图标布局定位，电动剪线器和折叠工具沿用通用动作。
- 现代物品在 1.7.10 中需要由其他模组提供实物。弩、三叉戟、长矛、重锤、副手装备槽、现代游泳及匍匐还缺对应的使用状态。通用外部 HMI addon 和动作音效尚未接入。原包未附 `firefly.png`，引用它的粒子会跳过绘制。1.7.10 的图标、皮肤、方块网格和光照也会影响最终画面。

第三方工具需要明确持握类型时，可在客户端初始化时调用 `ToolGrip.registerFamily(MyDrill.class, stack -> ToolGrip.PICKAXE)`，也可按元数据或 NBT 返回不同的类型。子类的回调优先于父类，返回 `null` 继续匹配。

第三方 Mod 可在客户端初始化时调用 `com.xyp.gtnrlib.client.animation.HmiItemBindings.register`。下例把模组自己的物品绑定到蓝色美西螈桶，其他颜色可通过同一个回调按物品 NBT 或损伤值选择。回调在渲染线程读取当前 `ItemStack`，建议复用已创建的 `Profile`。返回 `null` 时按物品自身身份解析，`unregister(item)` 可撤销绑定。

```java
var blueBucket = new HmiItemBindings.Profile("minecraft:axolotl_bucket", "blue");
HmiItemBindings.register(myBucket, stack -> blueBucket);
```

开发者可在下文的客户端夹具与 `:gtnrlib:checkLibraryContracts` 命令中追加 `-PhmiRelease=<原版 JAR 的绝对路径>` 复现 HMI 验证。无原包时仍执行公共库及 Punchy 测试。

## 试用第一人称示例

将本仓库 `gtnrlib/examples/first-person` 整个目录放入游戏实例的 `resourcepacks`，在资源包列表中选中后，可以空手走动、拿起铁锭或放置圆石。取消选择并重载即可恢复。示例是本项目编写的独立资源包，不随库 JAR 激活。

示例中，空手、持剑、放方块和用斧头都只显示右手。拿圆石右键放置，可以看到方块从右下随手伸出、回到待机位置。用铁斧持续砍木头，可以看到抬斧、下劈和收回。姿势使用玩家皮肤和 1.7.10 原版手臂网格。第三方动画可用 `dual_handed` 明确要求副手参与。修改资源包后按 F3+T 重载，更换库 JAR 后重启客户端。

现代包可以直接使用原始 ZIP 或目录。在 GTNRLib 客户端配置的“仅导入动画的资源包”填入其在 `resourcepacks` 中的文件名，按 F3+T 重载。这个显式导入项位于已选包之上，不要求 1.7.10 理解现代 pack.mcmeta。ZIP 的根应直接包含 assets。

在动作片段的时间轴第 0 秒添加 `gtnrlib:replace_swing;` 可让关键帧控制本次第一人称挥动。一次性动作结束时会淡回当前待机或行走姿势；松开挖掘后，已被接管的原版挥动会自然结束，其尾段继续隐藏。成功放置方块时，这个标记也让持物高度在播放期间保持稳定。普通换物仍走原有装备过渡。这个标记是 GTNRLib 扩展，省略时保留原版挥动叠加。

空手、普通平面物品和方块的基础片段在时间轴第 0 秒声明 `gtnrlib:hand_motion;`。它为手和物品共用的握点叠加呼吸、转头滞后、相对前进与横移的惯性，以及跳跃和下落时的高度变化。地面步态按实际行进距离推进，起步和停步时渐入渐出，跟随客户端的视角摇晃开关。换物时旧物品保留自己的握姿直到原版装备槽切换，再抬起新物品。运动每游戏 tick 更新一次，通过 partial ticks 采样。基于 `GTNRLIB_FLAT_ITEM` 的规则匹配普通非工具分类中无耐久、非 full-3D 的物品，并兼容已有的 `ITEM` 后备规则。特殊物品仍可按注册名覆盖。

本轮以 Hold My Items 原版 5.1.1 的资源脚本及 Reforged 2.0 的 Forge 渲染实现为行为参照。示例单独编写了空手出拳、普通持物挥动和圆石放置轨迹。宽屏夹具会测量方块前伸时的位置与尺寸，并检查连续两次成功放置后的收回。两个上游的发布版本、核对位置及校验值见[来源记录](../licenses/gtnrlib/SOURCE.md#first-person-hand-motion)。

连续搭建时，方块和前臂使用同一个腕部轨迹。示例先向前送出，再抬腕，约 0.12 秒到达最远点，约 0.4 秒收稳。下一个原版右键可以在 0.2 秒时成功放置，此时手仍在收回。放置片段在第 0 秒同时声明 `gtnrlib:replace_swing;` 和 `gtnrlib:chain_placement;` 后，重复触发同一片段会从当前握姿与速度衔接至新片段，过渡最长 75 毫秒。实际方块大小保持恒定，画面里的缩小来自前伸产生的透视。

示例中的空手右臂垂在画面右下，站立时随呼吸轻微转腕，走路时随步幅摆动。空手左键点空气或轻点方块共用约 0.30 秒的出拳动作。手腕先向内抬起、向前送出，再从右侧回到待机。轻点方块即使立刻松开，也会收完这一拳。在 `emptyHand` 的 `attack` 和 `mining` 规则中复用同一片段，并在第 0 秒同时声明 `gtnrlib:replace_swing;` 与 `gtnrlib:empty_punch;`，即可在原版接受下一拳时从当前腕部姿势和速度衔接。点击判定与挖掘仍由游戏处理。

### 从 Hold My Items 转换持握姿势

示例的持剑、镐、斧、锄、铲及铁锭握姿来自 Hold My Items 5.1.1 的静止主手采样。转换时保留原手臂的三维朝向和物品的侧转，用 1.7.10 的纹理坐标选取握点。长柄沿前臂的横向穿过拳心，剑单独选在护手下方的短柄。手臂和物品一起跟随 `right_arm` 运动。待机高度按本示例的短前臂构图设置。

开发者可在仓库根运行以下命令。需要 Python 3、NumPy、Java 21 或以上，以及[作者发布的 5.1.1 / MC 1.21.11 JAR](https://modrinth.com/mod/hold-my-items/version/KZm1qTcU)。工具校验发布包，从临时目录运行包内的 LuaJ、缓动函数和三个原版脚本，在固定视角、主手静止的条件下以 60 Hz 预热四秒。命令输出各物品的姿势参数，加 `--check` 可与示例包比较。

```powershell
python gtnrlib/scripts/hmi-grip/convert.py "C:/mods/HMI 5.1.1 (1.21.11).jar" --check
```

此转换器覆盖上述物品的主手持握基准。连击、蓄力、双手交互及完整 Lua 状态机需要另外适配。

## 第三方调用

`com.xyp.gtnrlib.api.animation` 独立于游戏客户端。每个目标持有自己的播放器，clip 和 rig 可以共享。注册、模拟和采样均在调用方的 owner thread 运行。下例中的 reader、日志和标记回调由宿主提供。

```java
AnimationLibrary library = new AnimationLibrary();
library.registerProvider("example:doors", out -> {
    try (Reader reader = openDoorResource()) {
        NativeAnimationResource.load(reader, out);
    }
});
if (library.reload(logger::warn)) {
    AnimationClip clip = library.find("example:door");
    AnimationPlayer player = new AnimationPlayer(marker -> onDoorMarker(marker.name()));
    AnimationPlayer.Playback playback = player.play(clip, 0.1);
    player.advance(0.05); // once per simulation tick
    AnimationClip.Vec angle = player.sample(
        new AnimationClip.Channel("hinge", AnimationClip.Property.ROTATION), 0.0125);
    playback.speed(0.5);
    playback.stop(0.15);
}
```

`NativeAnimationResource` 位于 `com.xyp.gtnrlib.animation.resource`。原生 v1 资源的最小示例：

```json
{
  "version": 1,
  "clips": {
    "example:door": {
      "duration": 1.0, "end": "hold",
      "tracks": [{
        "bone": "hinge", "property": "rotation",
        "keys": [{"time": 0, "value": [0, 0, 0]}, {"time": 1, "value": [0, 90, 0]}]
      }],
      "markers": [{"time": 0.5, "name": "half_open"}]
    }
  }
}
```

- Provider 按注册顺序加载，后者覆盖同名 clip，重复 provider ID 报错，失败保留先前快照。调用 `removeProvider` 后重载即可卸载。
- `AnimationRig` 按父节点在前的顺序定义骨架，合成静止姿势和父子变换到调用方提供的列主序矩阵数组。位置使用模型单位，旋转使用度。编辑器可以 `clip.sample` 定点采样或 `Playback.seek` 静默跳转。
- `AnimationPlayer.play(clip, fade, priority, blend, mask)` 支持优先级、通道遮罩、覆盖、加法位移/欧拉角和乘法缩放。对一次性片段调用 `Playback.fadeOutAtEnd(片段秒数)`，可在结尾平滑淡回下层当前姿势，默认时长为零。事件在模拟结束后派发，相同帧的多次采样只读取姿势。极长暂停后每次最多补发 64 个完整循环的事件以及末尾部分。
- `AnimationStates<T>` 按声明顺序选取首个满足谓词且资源可用的状态。同名状态重载到新片段时保留播放位置。每 tick 先 update 再推进共享播放器。
- 客户端第一人称入口是 `FirstPersonAnimations.INSTANCE`。用 `bind("mymod:tools", (held, action) -> ...)` 返回共享 clip 或 null，用 trigger 请求动作，用 addListener/removeListener 订阅标记，用 unbind 释放绑定。绑定按注册顺序选首个匹配，之后再查询资源包。第一人称适配器在重载后重新选取当前持物状态，正在播放的一次性动作会结束。

## Punchy! 兼容矩阵

| 部分 | 本版本行为 |
| --- | --- |
| 发现与覆盖 | 扫描已选本地 ZIP/目录的 `assets/minecraft/punchy/{animations,compat}/` 与 `assets/punchy/{animations,compat}/`。同名 clip 整段覆盖。规则按目标及动作类型覆盖，姿势按目标、arm/item 和左右侧整体覆盖。同路径低优先级文件仍参与。 |
| 关键帧 | 秒、常量数组、时间键、position/rotation/scale、pre/post/vector、线性、Catmull-Rom、V2 SLERP/SQUAD、30 种 easing、once/loop/hold。legacy 线性旋转取短弧。1.7.10 原版模型使用近似手部锚点，尚未验证不同上游骨架版本的支点及逐帧外观等价。 |
| 时间轴 | 支持字符串或数组、分号标记、初始布局/双手以及常规速度 token。变速按经过的时间段积分。常规速度限制为 0.5–5.5。特定动作的特殊速度、动态副手窗口、完整 blend 指令和声音派发尚未覆盖。 |
| 表达式 | 有界算术表达式、动画时间、度制三角和常用数值函数。未知名字取零。每条最多 256 字符。 |
| 第一人称 | 原版工具分类、注册名、emptyHand、default_animations、站立、行走、疾跑、单段挖掘、攻击变体、ItemBlock 成功放置。支持 transforms/armTransforms 的单侧偏移、旋转、缩放和物品支点。移动惯性仅在选中姿势时应用。 |
| 特殊渲染 | 映射右臂、左臂及 item_right/itemgrip_right，保留 Forge 的实际物品绘制、pass、地图和覆盖层。自定义 EQUIPPED_FIRST_PERSON renderer 默认走原渲染路径。游戏逻辑和第三人称绘制由原入口处理。 |
| 局限 | 自定义几何、材质、粒子、物品物理和模型动画、现代标签/副手、复杂组与交互过滤、三段 mining、完整优先级和 Mix the Packs 来源选择、Punchy! Moves 尚未完整支持。未知字段可能被忽略，模型动画和缺失引用会产生诊断。 |
| 重载与限制 | 损坏的片段或规则文件会隔离。I/O 失败保留上一快照。每包上限 4096 个 JSON，单文件 2 MiB，总计 32 MiB，嵌套深度 48。虚拟包可由第三方 provider 主动提供。 |

格式参照 [Punchy! 动画规范](https://wiki.punchymod.com/punchy/animations/reference)、[资源覆盖规则](https://wiki.punchymod.com/punchy/resource-packs) 和 [骨架说明](https://wiki.punchymod.com/punchy/player-rig)。实现和示例均为独立编写。

## 复现验证

在仓库根运行：

```powershell
.\gradlew.bat spotlessCheck :gtnrlib:build :gtnrlib:checkLibraryContracts
.\gradlew.bat --no-configuration-cache -I gtnrlib/scripts/animation-qa.init.gradle :gtnrlib:runClient25
.\gradlew.bat --no-configuration-cache -I gtnrlib/scripts/animation-qa.init.gradle -PanimationQaHost :runClient25
.\gradlew.bat --no-configuration-cache -I gtnrlib/scripts/animation-server-qa.init.gradle :gtnrlib:runServer25
```

Windows 客户端夹具在库 run 目录内启动独立静音实例，守护脚本隐藏窗口并设置点击穿透。它通过集成服务器和游戏帧缓冲操作，结果与 PNG 写入库 build 目录的 `animation-qa/{library,host}`。其中 `screenshots/world-grip.png` 和 `world-empty.png` 来自正常游戏帧。隔离绘制使用陆地上的 70° 手部投影，并临时在测试皮肤的腕部与袖口加上不同颜色的标记。夹具分别检查单手和双手姿势，要求两处标记可见且按对应方向分开，并核对禁用后的持剑、空手画面与原版逐像素一致。

独立库使用 640×480 窗口，宿主模式使用 854×480 窗口和项目声明的开发依赖。专服夹具使用独立的 `animation-qa/server`，监听环回地址和临时端口，需要该测试实例已有的 EULA 设置。测试结束自动停服。
