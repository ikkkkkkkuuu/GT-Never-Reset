# GTNRLib 动画 API 0.1.17

## 试用第一人称示例

将本仓库 `gtnrlib/examples/first-person` 整个目录放入游戏实例的 `resourcepacks`，在资源包列表中选中后拿起原版剑。取消选择并重载即可恢复。示例是本项目编写的独立资源包，不随库 JAR 激活。

现代包可以直接使用原始 ZIP 或目录。在 GTNRLib 客户端配置的“仅导入动画的资源包”填入其在 `resourcepacks` 中的文件名，按 F3+T 重载。这个显式导入项位于已选包之上，不要求 1.7.10 理解现代 pack.mcmeta。ZIP 的根应直接包含 assets。

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
- `AnimationPlayer.play(clip, fade, priority, blend, mask)` 支持优先级、通道遮罩、覆盖、加法位移/欧拉角和乘法缩放。事件在模拟结束后派发，相同帧的多次采样只读取姿势。极长暂停后每次最多补发 64 个完整循环的事件以及末尾部分。
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

Windows 客户端夹具在库 run 目录内启动独立静音实例，守护脚本隐藏窗口并设置点击穿透。它通过集成服务器和游戏帧缓冲操作，结果与 PNG 写入库 build 目录的 `animation-qa/{library,host}`。宿主模式使用项目声明的开发依赖。专服夹具使用独立的 `animation-qa/server`，监听环回地址和临时端口，需要该测试实例已有的 EULA 设置。测试结束自动停服。
