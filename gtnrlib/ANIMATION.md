# GTNRLib 动画 API 0.1.26

## 第一人称默认动作

在已有 GTNH 常规前置（GTNHLib、ModularUI2、Baubles）的 1.7.10 实例中，只需安装 GTNRLib。首次进入世界即启用内置动作，不需要 GT-Never-Reset、HMI JAR、Punchy! 资源包或手动重载。装有 GregTech 时，库会启用 GT 手动扳手、剪线钳和螺丝刀。多人游戏的服务端也装 GTNRLib 后，会在操作成功时通知玩家播放。可在客户端配置中关闭 `builtInFirstPerson`。

空手出拳、剑的地面连击和空中挥砍、镐和其他采掘工具、双手拉弓与松弦、进食、饮用和换物都使用同一个播放器。它分别求主手、物品和辅助手的姿势，再按当前握点合成呼吸、步态、转头滞后和垂直惯性。起跳后仍能挥剑、挖掘或拉弓，收招时回到角色当时的移动姿势。同一帧重复绘制不会重复推进运动。副手可参与持弓及双手工具动作，物品仍使用 1.7.10 的主手槽。

- 普通 GT 手动扳手在服务端确认右键操作成功后向准星探出。扳口就位后，手带着扳柄绕扳口转过约 55°，再略微抽开、回到握持位置。单次动作长 0.45 秒，连续成功操作会衔接下一次拧动。
- 普通 GT 手动螺丝刀操作机器、管道（含覆盖板）、中继器或比较器时，服务端确认后播放 `gtnrlib:screwdriver_turn`。右手前送刀尖，沿刀杆拧约 80°，再抽回，整段 0.48 秒。刀头和握柄沿用物品实际材质。
- 普通 GT 手动剪线钳由双手分握两柄。左键向前试剪，右键在服务端确认操作后开合，单次约 0.30 秒。张开时每片钳刃向外转 17°，左右握柄分别带动对侧钳刃，中央铰点单独绘制。两刃相接时由 GT 直接绘制完整的闭合工具。电动和折叠型号沿用通用握姿。更改标准 GT 图标铰点的材质包需要单独适配。
- 工具类型由原版基类、采掘类型和 Forge 矿词判定。库按已安装的模组补充 GT 元工具、匠魂工具和 IC2 电动工具类型。安装 GregTech 后，按物品和元数据识别其材料形态，包括板、粉、杆、箔、齿轮及材料方块。普通材料使用平面握姿，材料方块使用方块握姿，贴图、材质颜色与专用渲染仍取自实际物品。独立安装时仍按 Forge 矿词识别锭。普通 Forge 专用渲染器由提供者绘制；已知工具、显式绑定的动作或模型可进入第一人称播放器。

### 剑和工具如何握在手里

HMI 5.1.1 在第一人称画面中按 `thirdperson_righthand` 提交物品。0.1.23 误用了 `firstperson_righthand`。剑、镐、斧、锄、铲的模型绕 Z 轴的角度因此少了 30°，工具头偏向玩家。0.1.24 使用 1.21.11 对应物品的显示变换，右臂和左臂分别在肩部加上原版的 +0.1 和 −0.1 弧度侧转。弓、钓竿和普通平面物品各自使用源模型的朝向与比例。[1.21.11 显示上下文映射](https://maven.fabricmc.net/docs/yarn-1.21.11%2Bbuild.4/net/minecraft/item/ItemDisplayContext.html#THIRD_PERSON_RIGHT_HAND)

在 60 Hz 下用水平视角静止四秒，测量从握柄指向剑尖或镐头的向量与屏幕平面的夹角。旧版的剑和镐均朝玩家倾斜约 41.1°，HMI 原包同一采样约为 11.1°。工具材质、当前动作和视角会影响屏幕上的轮廓。

为了让旧版贴图的柄穿过拳心，沿柄线横向和厚度方向补了固定的小位移。剑用护手下方的柄，采掘工具用木柄。位移在物品本地坐标中应用，步态、转头、起落和挥动时沿用同一握点。带专属挂点的 GT 扳手和双手剪线钳使用各自的工具模型。

无外部资源包的宿主客户端在同一帧读取手臂模型和 Forge 物品绘制矩阵。拳心取经典手臂末端 4×4×4 皮肤立方体的中心。按 16×16 物品纹理计算，静止时剑（钻石）、镐、斧、铲、锄（铁）的柄轴距拳心分别约为 0.006、0.002、0.015、0.016、0.002 个纹理像素。角度另按 HMI 使用的模型轴核对，并复测了行走中挥剑、挖掘与跳跃。更换柄形状的材质包及第三方自绘的非标准模型需单独核对。

### 用 JSON 给物品增加动作

模组资源随 JAR 自动发现。开发时可在资源包中用相同路径覆盖，F3+T 可重读修改。下面的绑定只改变紫色染料的攻击，由集成服夹具在正常接受挥动的入口验证。把注册名、元数据和动作换成自己的即可。

`assets/example/animations/first_person.json`

```json
{
  "bindings": [
    {"item": "minecraft:dye", "damage": 13, "trigger": "attack", "clip": "example:twist"}
  ]
}
```

`assets/example/animations/twist.json`

```json
{
  "version": 1,
  "clips": {
    "example:twist": {
      "duration": 0.30,
      "tracks": [
        {"bone": "wrist", "property": "rotation", "keys": [
          {"time": 0, "value": [0, 0, 0]},
          {"time": 0.14, "value": [15, 10, -40]},
          {"time": 0.30, "value": [0, 0, 0]}
        ]}
      ]
    }
  }
}
```

`damage` 省略时匹配该注册名的全部元数据。精确元数据优先，然后查同物品的通配绑定。一次只替换所选触发行为，未绑定的行为继续使用默认姿势。可用触发有 `attack`（点击空气或攻击实体）、`mining`（对准方块挥动）、`place`（ItemBlock 成功放置）、`interact`（右键交互产生挥动）、`use`（物品开始持续使用）。`use` 支持持续片段，松开时淡出。已有的模组专用触发同样可写进 JSON，例如 `gtneverreset:wrench_turn`、`gtneverreset:wire_cutter_snip`。全新的业务事件需由其提供者发出一次 `FirstPersonItemActions.INSTANCE.trigger("域:事件")`。

`wrist` 同时带动主手和物品；`right_arm`、`left_arm`、`item` 可分别添加局部变换。位移使用游戏单位，角度使用度，时间使用秒。切换堆栈或卸载世界时清理播放状态。库内的 [扳手绑定](src/main/resources/assets/gtneverreset/animations/first_person.json) 和 [拧腕曲线](src/main/resources/assets/gtneverreset/animations/wrench_turn.json) 是实际发行的例子。剪线钳的双柄、铰点与握点由库内的 GT 模型提供，其前伸和开合幅度在 [剪线钳曲线](src/main/resources/assets/gtneverreset/animations/wire_cutter.json) 内调整。这三份 JSON 随 GTNRLib 发行，保留原来的 `assets/gtneverreset/animations/` 路径和动作 ID，以便旧资源包继续覆盖。新的内置动作在 `gtnrlib/src/main/resources/assets/gtnrlib/animations/` 维护。

### 与 HMI 5.1.1 默认程序的逐帧对照

开发工具从锁定的发布包采样局部动作、双手和各运动支点。发行库读取压缩数值曲线，并根据实际玩家输入连续计算移动。对照程序以同一组输入分别运行原包 Lua 和内置播放器，记录主手、物品、副手原点之间的最大距离及各自三个轴中最大的夹角。以下为每组连续 2.5 秒内的峰值，包含松开或收回后的帧。位置使用 1.7.10 渲染坐标中的格数。

| 输入 | 采样频率 | 物品位置最大差 | 物品朝向最大差 |
| --- | --- | ---: | ---: |
| 原地转头、走动起停、连续起跳和落地，分别对照 | 8 档，30–240 Hz | < 0.0001 | ≤ 0.03° |
| 空手出拳 | 60 Hz | 0.0032 | 0.33° |
| 持剑边走边挥 | 8 档，30–240 Hz | 0.0023 | 0.35° |
| 持镐边走边挖 | 60 Hz | 0.0022 | 0.42° |
| 拉弓、短拉即松 | 60 Hz | 0.0002 | 0.03° |
| 边走边拉弓 | 8 档，30–240 Hz | 0.0008 | 0.03° |
| 空中挥剑 / 拉弓 | 60 Hz | 0.0022 / 0.0007 | 0.33° / 0.03° |
| 行走中同时转头、挥剑 / 拉弓 | 60 Hz | 0.0022 / 0.0008 | 0.33° / 0.03° |
| 快速连剑（间隔 6 tick） / 较慢连剑 | 60 Hz | 0.0020 / 0.0023 | 0.30° / 0.35° |
| 喝药 | 60 Hz | 0.0153 | 1.31° |

采样频率分别为 30、45、60、75、90、120、144 和 240 Hz。共 95 组、18,305 个有效采样帧，包含下方 35 项的逐项对照。角度和距离按上表精度取整。另测了持剑使用、进食、换物和持物高度。游戏侧在空资源包目录启动独立客户端，记录真实前进、两次起跳、下落与触地，并在空中分别挥剑、拉弓。数值参考用的是运行在适配器中的 5.1.1 原包程序；尚未做现代 1.21.11 客户端和 1.7.10 客户端的逐像素对照。玩家皮肤、网格、物品纹理、照明和视场都会影响最终画面。

### 35 个特殊物品模型入口

原包的 `assets/minecraft/items/*.json` 共 35 项。剑、镐、弓、空手和玩家运动归上面的共用姿势程序。内置模型为 1.7.10 单独制作，采样握姿并沿用本机方块图集。实机检查每项的解析目标、独立模型可见像素、静止与挥动的手部矩阵和帧缓冲变化。原包程序另逐项对照 60 Hz 的持握、挥动与收回；其中物品原点的最大差为 0.0156 格（末地水晶），朝向最大差为 1.03°（花叶篮）。开发宿主有 22 项可直接拿在手里（原版 5 项、Et Futurum 17 项）。下表的“映射”表示该实例缺少对应物品，夹具通过绑定堆栈绘制同一入口。它验证模型与握姿，缺失物品的原生使用交互仍无实物可测。

| 5.1.1 入口 | 1.7.10 实例 | 核对项目 |
| --- | --- | --- |
| `acacia_boat` | 实物 `etfuturum:acacia_boat` | 握持、挥动通过 |
| `acacia_chest_boat` | 实物 `etfuturum:acacia_chest_boat` | 握持、挥动通过 |
| `axolotl_bucket` | 映射，实例缺实物 | 握持、挥动通过 |
| `bamboo_chest_raft` | 实物 `etfuturum:bamboo_chest_raft` | 握持、挥动通过 |
| `bamboo_raft` | 实物 `etfuturum:bamboo_raft` | 握持、挥动通过 |
| `birch_boat` | 实物 `etfuturum:birch_boat` | 握持、挥动通过 |
| `birch_chest_boat` | 实物 `etfuturum:birch_chest_boat` | 握持、挥动通过 |
| `bucket` | 实物 `minecraft:bucket` | 握持、挥动通过 |
| `cherry_boat` | 实物 `etfuturum:cherry_boat` | 握持、挥动通过 |
| `cherry_chest_boat` | 实物 `etfuturum:cherry_chest_boat` | 握持、挥动通过 |
| `cod_bucket` | 映射，实例缺实物 | 握持、挥动通过 |
| `dark_oak_boat` | 实物 `etfuturum:dark_oak_boat` | 握持、挥动通过 |
| `dark_oak_chest_boat` | 实物 `etfuturum:dark_oak_chest_boat` | 握持、挥动通过 |
| `end_crystal` | 实物 `etfuturum:end_crystal` | 握持、挥动通过 |
| `jungle_boat` | 实物 `etfuturum:jungle_boat` | 握持、挥动通过 |
| `jungle_chest_boat` | 实物 `etfuturum:jungle_chest_boat` | 握持、挥动通过 |
| `lava_bucket` | 实物 `minecraft:lava_bucket` | 握持、挥动通过 |
| `leaf_litter` | 映射，实例缺实物 | 握持、挥动通过 |
| `mangrove_boat` | 映射，实例缺实物 | 握持、挥动通过 |
| `mangrove_chest_boat` | 映射，实例缺实物 | 握持、挥动通过 |
| `milk_bucket` | 实物 `minecraft:milk_bucket` | 握持、挥动通过 |
| `oak_boat` | 实物 `minecraft:boat` | 握持、挥动通过 |
| `oak_chest_boat` | 实物 `etfuturum:oak_chest_boat` | 握持、挥动通过 |
| `pale_oak_boat` | 映射，实例缺实物 | 握持、挥动通过 |
| `pale_oak_chest_boat` | 映射，实例缺实物 | 握持、挥动通过 |
| `pink_petals` | 实物 `etfuturum:pink_petals` | 握持、挥动通过 |
| `powder_snow_bucket` | 映射，实例缺实物 | 握持、挥动通过 |
| `pufferfish_bucket` | 映射，实例缺实物 | 握持、挥动通过 |
| `salmon_bucket` | 映射，实例缺实物 | 握持、挥动通过 |
| `spruce_boat` | 实物 `etfuturum:spruce_boat` | 握持、挥动通过 |
| `spruce_chest_boat` | 实物 `etfuturum:spruce_chest_boat` | 握持、挥动通过 |
| `tadpole_bucket` | 映射，实例缺实物 | 握持、挥动通过 |
| `tropical_fish_bucket` | 映射，实例缺实物 | 握持、挥动通过 |
| `water_bucket` | 实物 `minecraft:water_bucket` | 握持、挥动通过 |
| `wildflowers` | 映射，实例缺实物 | 握持、挥动通过 |

船／筏有独立船体与桨，运输型带活动箱盖；液面、饮奶液位、桶中生物、花叶粒子和水晶可动。美西螈桶另测了同一堆栈切换五种外观。内置几何、纹理和装饰性粒子保留 1.7.10 的绘制方式，不能据这 35 个通过项判定与上游逐像素相同。1.7.10 原版也缺少现代副手物品槽、弩、三叉戟、长矛、重锤以及现代游泳、匍匐等状态。地图随俯视变焦、攀梯时双臂交替和检视动作的逐帧等价性尚未验证。完整 GTNH 整合包、第三方光影和通用 HMI addon 尚未回归。

## 可选的本地原包对照

需要对照原包资源时，可把 HMI 5.1.1（MC 1.21.11，版本 KZm1qTcU）的原始 JAR 放在独立实例 `resourcepacks` 内，将完整文件名填入客户端的显式导入项后按 F3+T。GTNRLib 校验 SHA-256，运行四段原包程序并读取该包自己的模型和纹理。清空导入项并重载后回到内置默认。已选 Punchy! 包可给匹配物品提供替代姿势，显式加载 HMI 原包时优先使用原包。动作音效和通用 addon 尚未接入。[HMI 5.1.1 发布页](https://modrinth.com/mod/hold-my-items/version/KZm1qTcU)

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

该转换器只用于上述 Punchy 示例的静止握姿。内置播放器的动态采样由 `CapturePoses.java` 完成。

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

在仓库根运行。带 `-PanimationQaDefaults` 的客户端命令要求测试资源包目录为空。

```powershell
.\gradlew.bat spotlessCheck build :gtnrlib:checkLibraryContracts
.\gradlew.bat --no-configuration-cache -I gtnrlib/scripts/animation-qa.init.gradle -PanimationQaDefaults :gtnrlib:runClient25
.\gradlew.bat --no-configuration-cache -I gtnrlib/scripts/animation-qa.init.gradle -PanimationQaDefaults -PanimationQaStandaloneMods :runClient25
.\gradlew.bat --no-configuration-cache -I gtnrlib/scripts/animation-qa.init.gradle -PanimationQaDefaults -PanimationQaHost :runClient25
.\gradlew.bat --no-configuration-cache -I gtnrlib/scripts/animation-qa.init.gradle :gtnrlib:runClient25
.\gradlew.bat --no-configuration-cache -I gtnrlib/scripts/animation-qa.init.gradle -PanimationQaHost :runClient25
.\gradlew.bat --no-configuration-cache -I gtnrlib/scripts/animation-server-qa.init.gradle :gtnrlib:runServer25
```

默认动作结果及截图写入库 build 的 `animation-qa/{library-defaults,library-mods-defaults,host-defaults}`，每次启动从空资源包目录直接进入独立世界。附加的旧示例／可选原包测试使用另外的实例。开发者可给 `:gtnrlib:checkLibraryContracts` 传入 `-PhmiRelease=<原包绝对路径>` 运行逐帧对照。要重新生成内置数值资源，使用同一参数和 `-I gtnrlib/scripts/animation-qa.init.gradle :gtnrlib:captureDefaultPoses`，输出在库 build 的 `animation-qa/default-poses.gz`。

Windows 客户端夹具在库 run 目录内启动独立静音实例，守护脚本隐藏窗口并设置点击穿透。它通过集成服务器和游戏帧缓冲操作，结果与 PNG 写入库 build 目录的 `animation-qa/{library,host}`。其中 `screenshots/world-grip.png` 和 `world-empty.png` 来自正常游戏帧。隔离绘制使用陆地上的 70° 手部投影，并临时在测试皮肤的腕部与袖口加上不同颜色的标记。夹具分别检查单手和双手姿势，要求两处标记可见且按对应方向分开，并核对禁用后的持剑、空手画面与原版逐像素一致。

独立库使用 640×480 窗口，带可选模组的模式使用 854×480 窗口。`-PanimationQaStandaloneMods` 从运行类路径排除 GT-Never-Reset，并核对已加载模组；它使用实际 GregTech 工具检查客户端渲染和服务端确认。专服夹具使用独立的 `animation-qa/server`，监听环回地址和临时端口，需要该测试实例已有的 EULA 设置。测试结束自动停服。
