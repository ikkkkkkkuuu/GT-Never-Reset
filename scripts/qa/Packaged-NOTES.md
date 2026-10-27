# 封包供应器移植验证

来源为相邻 GT-Not-Good 的现有实现，保留供应器、连接器、七种专用核心、原 GUI 与材质。
代码和资源来源清单位于 `src/main/resources/META-INF/ae2lt-port/`。

## 操作

供应器连接有电且有可用频道的 ME 网络，装入专用核心和加工样板。
手持连接器普通右键选择供应器，再 Shift 右键对应机器绑定；空中右键清除选择。
启用自动回收后从 ME 下单。绑定仅接受同维度已加载区块，所有者须在线并具备相应权限和研究。
基础核心用于合成专用核心，本身不执行加工。含流体的装配线配方使用终极编码样板。
供应器有 720 个样板槽、九个回收槽和一个核心槽，任务进行中不能更换核心。

## 可重复检查

```powershell
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:/gtnr-unix-socket-fallback-missing'
./gradlew.bat -I scripts/packaged-client-qa.init.gradle runClient17 --no-configuration-cache
$packagedSave = (Get-Content build/packaged-client-qa/client-run/packaged-qa-resume.txt -Raw).Trim()
./gradlew.bat -I scripts/packaged-client-qa.init.gradle runClient17 "-PpackagedResume=$packagedSave" --no-configuration-cache
```

测试创建隔离存档，客户端静音并隐藏。第一轮应返回 `SAVED`，第二轮应返回 `PASS`；
结果文件在 `build/packaged-client-qa/client-run/packaged-qa-result.txt`，截图在同目录的 `screenshots/`。
测试配方仅存在于测试客户端，不进入正式 JAR。

2026-10-27 两轮客户端检查通过：绑定与去重、研究/基座拒绝、真实注魔、重复派单拒绝、
活动核心保护、红石及产物锁、旧库存迁移、任务存档恢复、精确回收、ME 背压和一次入库，
以及 720 格滚动、末尾样板、目标列表操作、优先级、终端可见性和样板优化同步。
已检查主界面与目标列表截图，资源路径正常；按用户要求移除界面文字阴影，调整为深灰字。

本轮没有逐一运行其他六种核心的完整机器流程，也没有进行专用服务器或大型多人服压力测试。
