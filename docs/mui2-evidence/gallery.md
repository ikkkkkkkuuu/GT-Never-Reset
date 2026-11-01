# MUI2 实测截图索引

2026-11-02；ModularUI2 2.3.94-1.7.10；1100×740 游戏帧缓冲原图。关联 [开发指南](../mui2-gui-guide.md) 和 [自动化记录](report.txt)。

## 人工复核结果

- 已逐张通过联系表检查全部 33 张截图，并原尺寸检查关键控制台、实体、结构、同步页。
- 图表、富文本、菜单、列表、文字输入、视图变换和机器页正常显示；动画前后帧有变化。
- `buildWorldSchemaUI` 仅显示面板及 ALL 按钮，没有结构；自动化面板断言不覆盖这一失败。
- `entity-direct-cow` 可以显示实体，但模型超出预留区域、遮挡标题，需要进一步缩放/裁剪后才能交付生产界面。
- 控制台缩放 1、2、3 边界通过；缩放 3 图表较矮，生产界面仍建议提供详情布局。
- 原生同步方块两种标签均正常显示；本次未覆盖背包资源移动、所有子面板和 NEI 拖放操作。
- 上游 SpriteAndEntity 示例构造失败，没有对应成功截图，错误保留在 report.txt。

## 原图

| 场景 / 阶段 | 截图 |
| --- | --- |
| `00-diamond-menu` | [查看原图](../images/mui2/00-diamond-menu.png) |
| `buildAspectRatioUI` | [查看原图](../images/mui2/buildAspectRatioUI.png) |
| `buildCollapseDisabledChildrenUI` | [查看原图](../images/mui2/buildCollapseDisabledChildrenUI.png) |
| `buildColorTheoryUI` | [查看原图](../images/mui2/buildColorTheoryUI.png) |
| `buildContextMenu-opened` | [查看原图](../images/mui2/buildContextMenu-opened.png) |
| `buildContextMenu` | [查看原图](../images/mui2/buildContextMenu.png) |
| `buildGraphUI` | [查看原图](../images/mui2/buildGraphUI.png) |
| `buildMachineLikeUI` | [查看原图](../images/mui2/buildMachineLikeUI.png) |
| `buildPendulumAnimationUI-after` | [查看原图](../images/mui2/buildPendulumAnimationUI-after.png) |
| `buildPendulumAnimationUI` | [查看原图](../images/mui2/buildPendulumAnimationUI.png) |
| `buildPostTheLogAnimationUI-after` | [查看原图](../images/mui2/buildPostTheLogAnimationUI-after.png) |
| `buildPostTheLogAnimationUI` | [查看原图](../images/mui2/buildPostTheLogAnimationUI.png) |
| `buildRichTextUI` | [查看原图](../images/mui2/buildRichTextUI.png) |
| `buildSearchTest-filtered` | [查看原图](../images/mui2/buildSearchTest-filtered.png) |
| `buildSearchTest` | [查看原图](../images/mui2/buildSearchTest.png) |
| `buildTextFieldUI-after` | [查看原图](../images/mui2/buildTextFieldUI-after.png) |
| `buildTextFieldUI` | [查看原图](../images/mui2/buildTextFieldUI.png) |
| `buildToggleGridListUI` | [查看原图](../images/mui2/buildToggleGridListUI.png) |
| `buildViewportTransformUI` | [查看原图](../images/mui2/buildViewportTransformUI.png) |
| `buildWorldSchemaUI` | [查看原图](../images/mui2/buildWorldSchemaUI.png) |
| `buildWrappedFlowUI` | [查看原图](../images/mui2/buildWrappedFlowUI.png) |
| `dashboard-scale-1-after` | [查看原图](../images/mui2/dashboard-scale-1-after.png) |
| `dashboard-scale-1` | [查看原图](../images/mui2/dashboard-scale-1.png) |
| `dashboard-scale-2-after` | [查看原图](../images/mui2/dashboard-scale-2-after.png) |
| `dashboard-scale-2` | [查看原图](../images/mui2/dashboard-scale-2.png) |
| `dashboard-scale-3-after` | [查看原图](../images/mui2/dashboard-scale-3-after.png) |
| `dashboard-scale-3` | [查看原图](../images/mui2/dashboard-scale-3.png) |
| `entity-direct-cow` | [查看原图](../images/mui2/entity-direct-cow.png) |
| `sortable-list` | [查看原图](../images/mui2/sortable-list.png) |
| `synced-tile-after` | [查看原图](../images/mui2/synced-tile-after.png) |
| `synced-tile-before` | [查看原图](../images/mui2/synced-tile-before.png) |
| `synced-tile-second-tab` | [查看原图](../images/mui2/synced-tile-second-tab.png) |
| `synced-tile` | [查看原图](../images/mui2/synced-tile.png) |
