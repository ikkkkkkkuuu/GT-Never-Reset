# GT-Never-Reset

面向 Minecraft 1.7.10 / Forge 的 GT New Horizons 附属模组。

## 📥 下载与兼容性

| GTNH 版本 | 模组版本 | 现状 | 下载 |
| --- | --- | --- | --- |
| `2.9.0-RC-2`（开发基准） | `0.1.0` | 🚧 开发中，尚未发布 | [![Download Releases](https://img.shields.io/badge/Download-Releases-blue)](https://github.com/ikkkkkkkuuu/GT-Never-Reset/releases) |

当前尚无正式发行包，发布后可在 [Releases](https://github.com/ikkkkkkkuuu/GT-Never-Reset/releases) 下载。构建状态与日志可在 [Actions](https://github.com/ikkkkkkkuuu/GT-Never-Reset/actions) 查看。

开发环境使用 Minecraft `1.7.10`、Forge `10.13.4.1614`，以 GTNH `2.9.0-RC-2` 的依赖清单为基准；其他 GTNH 版本的兼容性尚待验证。

## 📜 制作人员与署名

本项目作者：[xyp](https://github.com/ikkkkkkkuuu)。感谢以下开源项目及贡献者提供的代码、资源和实现参考：

| 项目 | 作者 / 贡献者 | 许可 |
| --- | --- | --- |
| [GT-Not-Good](https://github.com/ikkkkkkkuuu/GT-Not-Good) | GT-Not-Good 贡献者 | [MIT](src/main/resources/META-INF/compact-ae-port/GT-Not-Good-MIT.txt)，第三方派生部分保留原许可 |
| [GT-Not-Leisure](https://github.com/ABKQPO/GT-Not-Leisure) | GT-Not-Leisure 贡献者；自动提示相关贡献者 LineBroke | [LGPL-3.0-only](src/main/resources/META-INF/compact-ae-port/GT-Not-Leisure-LGPL-3.0.txt) |
| [GregTech / GT5-Unofficial](https://github.com/GTNewHorizons/GT5-Unofficial) | GTNewHorizons、GregTech / GT++ 贡献者 | [LGPL-3.0 / LGPL-3.0-or-later](src/main/resources/META-INF/super-storage-input-port/NOTICE.md)，以对应文件声明为准 |
| [Twist Space Technology](https://github.com/Nxer/Twist-Space-Technology-Mod) | Nxer 及贡献者 | [GPL-3.0-only](src/main/resources/META-INF/multiblock-base-port/GPL-3.0.txt) |
| [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) / [GTNH 分支](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial) | AlgorithmX2、TeamAppliedEnergistics、Ridanisaurus Rid 及贡献者 | 代码：[LGPL-3.0-or-later](src/main/resources/META-INF/advancedio-port/NOTICE.md)；视觉素材：[CC BY-NC-SA 3.0](src/main/resources/META-INF/wireless-terminal-port/AE2-CC-BY-NC-SA-3.0.txt)；部分旧版 GUI 贴图：[LGPL-3.0](src/main/resources/META-INF/wildcard-pattern-port/NOTICE.md) |
| [AdvancedAE](https://github.com/pedroksl/AdvancedAE) | pedroksl 及贡献者 | [LGPL-3.0](src/main/resources/META-INF/advancedio-port/LICENSE-LGPL-3.0.txt) |
| [AE2AddonLib](https://github.com/pedroksl/AE2AddonLib) | pedroksl 及贡献者 | [GPL-3.0-only](src/main/resources/META-INF/advancedio-port/AE2AddonLib-LICENSE.txt) |
| [Flux Networks](https://github.com/SonarSonic/Flux-Networks) | Ollie Lansdell / SonarSonic、BloCamLimb 及贡献者 | 代码与方块资源：[MIT](src/main/resources/META-INF/licenses/Flux-Networks-MIT.txt)；GUI 设计与资源：[CC BY-NC-SA 4.0](src/main/resources/META-INF/licenses/Flux-Networks-GUI-CC-BY-NC-SA-4.0.txt) |
| [Modernity GTNH UI](https://github.com/ModernityGTNH/Modernity-GTNH-UI) / [Dark UI](https://github.com/ModernityGTNH/Modernity-GTNH-Dark-UI) | ModernityGTNH 贡献者 | [CC BY-NC-SA 4.0](src/main/resources/META-INF/wireless-terminal-port/Modernity-CC-BY-NC-SA-4.0.txt) |
| [AE2 Lightning Tech Reborn](https://github.com/AE2-Lightning-Tech-Reborn/AE2-Lightning-Tech-Reborn) 无线频率卡贴图 | MOAKIEE、CystrySU、gjmhmm8、\_leng、TedXenon、MHanHanBing 等贡献者 | [CC BY-NC-SA 3.0](src/main/resources/META-INF/wireless-frequency-card/LICENSE-CC-BY-NC-SA-3.0.txt) |
| [GTNH Beyond the Stars](https://github.com/v3rysp3d/GTNH-Beyond-the-Stars) 贴图 | V3rySp3d | [MIT](src/main/resources/META-INF/vault-port/LICENSE-Beyond-the-Stars-MIT.txt) |
| Wildcard-Pattern | LeoDreamer | [MIT](src/main/resources/META-INF/wildcard-pattern-port/Wildcard-Pattern-MIT.txt) |
| LDLib | LDLib 贡献者 | [LGPL-3.0](src/main/resources/META-INF/wildcard-pattern-port/LDLib-LGPL-3.0.txt) |
| [GregTechCEu / GregTech Modern](https://github.com/GregTechCEu/GregTech-Modern) 资源 | GregTechCEu 贡献者 | [LGPL-3.0](src/main/resources/META-INF/wildcard-pattern-port/GTCEu-LGPL-3.0.txt) |
| GTL-Enhancedcore 大型熔炉 | S_Fast | [LGPL-3.0](src/main/resources/META-INF/large-furnace-port/LICENSE-LGPL-3.0.txt) |

本项目原创部分采用 [MIT](LICENSE) 许可；上表所列第三方代码和资源保留各自的许可。

完整署名、来源版本、文件清单、修改说明及许可证文本见 [`src/main/resources/META-INF`](src/main/resources/META-INF)。翻译构建逻辑的来源说明见 [`scripts/translation-NOTICE`](scripts/translation-NOTICE)。
