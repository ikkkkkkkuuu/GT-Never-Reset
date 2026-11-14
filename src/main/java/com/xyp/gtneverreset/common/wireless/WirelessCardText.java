package com.xyp.gtneverreset.common.wireless;

import net.minecraft.util.StatCollector;

public final class WirelessCardText {

    public static String tr(String suffix, Object... args) {
        String key = switch (suffix) {
            // #tr gtnr.wireless.gui.source_delete
            // # Delete
            // # zh_CN 删除
            case "source_delete" -> "gtnr.wireless.gui.source_delete";
            // #tr gtnr.wireless.gui.source_delete_hint
            // # Remove this saved source from the card; existing wireless links remain.
            // # zh_CN 从卡片移除此发射源；已有无线连接保持不变。
            case "source_delete_hint" -> "gtnr.wireless.gui.source_delete_hint";
            // #tr gtnr.wireless.gui.sources_title
            // # Wireless Frequency Card · Sources
            // # zh_CN 无线频率卡 · 发射源
            case "sources_title" -> "gtnr.wireless.gui.sources_title";
            // #tr gtnr.wireless.gui.sources_hint
            // # Sneak-right-click an ME Controller to add a source.
            // # zh_CN 潜行右键 ME 控制器可添加发射源。
            case "sources_hint" -> "gtnr.wireless.gui.sources_hint";
            // #tr gtnr.wireless.gui.sources_auto_hint
            // # Auto-connect uses the selected source; existing links stay unchanged.
            // # zh_CN 自动连接使用当前选中的源；已有连接保持不变。
            case "sources_auto_hint" -> "gtnr.wireless.gui.sources_auto_hint";
            // #tr gtnr.wireless.gui.sources_open
            // # Click to select a saved source network.
            // # zh_CN 点击选择已保存的源网络。
            case "sources_open" -> "gtnr.wireless.gui.sources_open";
            // #tr gtnr.wireless.gui.source_loaded
            // # Loaded
            // # zh_CN 已加载
            case "source_loaded" -> "gtnr.wireless.gui.source_loaded";
            // #tr gtnr.wireless.gui.source_unloaded
            // # Unloaded or unavailable
            // # zh_CN 未加载或不可用
            case "source_unloaded" -> "gtnr.wireless.gui.source_unloaded";
            // #tr gtnr.wireless.gui.source_selected
            // # Selected
            // # zh_CN 已选中
            case "source_selected" -> "gtnr.wireless.gui.source_selected";
            // #tr gtnr.wireless.gui.source_select
            // # Select
            // # zh_CN 选择
            case "source_select" -> "gtnr.wireless.gui.source_select";
            // #tr gtnr.wireless.gui.title
            // # Wireless Frequency Card · Network
            // # zh_CN 无线频率卡 · 网络管理
            case "title" -> "gtnr.wireless.gui.title";
            // #tr gtnr.wireless.gui.shared
            // # Shared: %s
            // # zh_CN 共用：%s
            case "shared" -> "gtnr.wireless.gui.shared";
            // #tr gtnr.wireless.gui.budget_compact
            // # Controller channels: %s / %s
            // # zh_CN 控制器频道：%s / %s
            case "budget_compact" -> "gtnr.wireless.gui.budget_compact";
            // #tr gtnr.wireless.gui.online_metric
            // # Online %s/%s
            // # zh_CN 在线 %s/%s
            case "online_metric" -> "gtnr.wireless.gui.online_metric";
            // #tr gtnr.wireless.gui.missing_metric
            // # Missing channels %s
            // # zh_CN 缺少频道 %s
            case "missing_metric" -> "gtnr.wireless.gui.missing_metric";
            // #tr gtnr.wireless.gui.online_compact
            // # %s/%s
            // # zh_CN 在线 %s/%s
            case "online_compact" -> "gtnr.wireless.gui.online_compact";
            // #tr gtnr.wireless.gui.missing_compact
            // # Miss %s
            // # zh_CN 缺少 %s
            case "missing_compact" -> "gtnr.wireless.gui.missing_compact";
            // #tr gtnr.wireless.gui.entrances_compact
            // # Links %s
            // # zh_CN 接入点 %s
            case "entrances_compact" -> "gtnr.wireless.gui.entrances_compact";
            // #tr gtnr.wireless.gui.short_state.active
            // # Linked
            // # zh_CN 已连接
            case "short_state.active" -> "gtnr.wireless.gui.short_state.active";
            // #tr gtnr.wireless.gui.short_state.waiting
            // # Waiting
            // # zh_CN 等待恢复
            case "short_state.waiting" -> "gtnr.wireless.gui.short_state.waiting";
            // #tr gtnr.wireless.gui.short_state.conflict
            // # Conflict
            // # zh_CN 频率冲突
            case "short_state.conflict" -> "gtnr.wireless.gui.short_state.conflict";
            // #tr gtnr.wireless.gui.short_state.paused
            // # Paused
            // # zh_CN 已暂停
            case "short_state.paused" -> "gtnr.wireless.gui.short_state.paused";
            // #tr gtnr.wireless.gui.short_state.limited
            // # Limited
            // # zh_CN 扫描受限
            case "short_state.limited" -> "gtnr.wireless.gui.short_state.limited";
            // #tr gtnr.wireless.gui.copy
            // # Copy
            // # zh_CN 复制
            case "copy" -> "gtnr.wireless.gui.copy";
            // #tr gtnr.wireless.gui.highlight
            // # Mark
            // # zh_CN 高亮
            case "highlight" -> "gtnr.wireless.gui.highlight";
            // #tr gtnr.wireless.gui.teleport
            // # TP
            // # zh_CN 传送
            case "teleport" -> "gtnr.wireless.gui.teleport";
            // #tr gtnr.wireless.gui.teleport_unavailable
            // # The target device or source is not loaded. Teleport is unavailable.
            // # zh_CN 目标设备或发射端尚未加载，暂时无法传送。
            case "teleport_unavailable" -> "gtnr.wireless.gui.teleport_unavailable";
            // #tr gtnr.wireless.gui.teleport_no_space
            // # No safe landing space near the device. Clear ground and two blocks of headroom.
            // # zh_CN 设备附近没有安全落脚点，请留出实心地面和两格头顶空间。
            case "teleport_no_space" -> "gtnr.wireless.gui.teleport_no_space";
            // #tr gtnr.wireless.gui.teleport_failed
            // # Unable to teleport right now.
            // # zh_CN 当前无法完成传送。
            case "teleport_failed" -> "gtnr.wireless.gui.teleport_failed";
            // #tr gtnr.wireless.gui.teleport_done
            // # Teleported near the device: D%s %s, %s, %s.
            // # zh_CN 已传送至设备附近：D%s %s, %s, %s。
            case "teleport_done" -> "gtnr.wireless.gui.teleport_done";
            // #tr gtnr.wireless.gui.copied
            // # Copied dimension, coordinates and part side to the clipboard.
            // # zh_CN 已复制目标维度、坐标和部件方向到剪贴板。
            case "copied" -> "gtnr.wireless.gui.copied";
            // #tr gtnr.wireless.gui.save_name
            // # Save name
            // # zh_CN 保存备注
            case "save_name" -> "gtnr.wireless.gui.save_name";
            // #tr gtnr.wireless.gui.name_label
            // # Name
            // # zh_CN 备注
            case "name_label" -> "gtnr.wireless.gui.name_label";
            // #tr gtnr.wireless.gui.search
            // # Find
            // # zh_CN 查找
            case "search" -> "gtnr.wireless.gui.search";
            // #tr gtnr.wireless.gui.search_placeholder
            // # Name / coordinates
            // # zh_CN 名称 / 坐标
            case "search_placeholder" -> "gtnr.wireless.gui.search_placeholder";
            // #tr gtnr.wireless.gui.filter.0
            // # All
            // # zh_CN 全部
            case "filter.0" -> "gtnr.wireless.gui.filter.0";
            // #tr gtnr.wireless.gui.filter.1
            // # Paused
            // # zh_CN 暂停
            case "filter.1" -> "gtnr.wireless.gui.filter.1";
            // #tr gtnr.wireless.gui.filter.2
            // # Conflict
            // # zh_CN 冲突
            case "filter.2" -> "gtnr.wireless.gui.filter.2";
            // #tr gtnr.wireless.gui.filter.3
            // # Linked
            // # zh_CN 已连接
            case "filter.3" -> "gtnr.wireless.gui.filter.3";
            // #tr gtnr.wireless.gui.filter.4
            // # Waiting
            // # zh_CN 等待
            case "filter.4" -> "gtnr.wireless.gui.filter.4";
            // #tr gtnr.wireless.gui.filter_invalid
            // # Invalid filter: enter an integer dimension ID or leave it blank.
            // # zh_CN 筛选无效：维度应填写整数或留空。
            case "filter_invalid" -> "gtnr.wireless.gui.filter_invalid";
            // #tr gtnr.wireless.gui.no_matches
            // # No matching loaded clusters. Clear the search and dimension, then select All states.
            // # zh_CN 没有匹配的已加载簇。清空搜索、维度并选择全部状态可恢复列表。
            case "no_matches" -> "gtnr.wireless.gui.no_matches";
            // #tr gtnr.wireless.gui.highlight_dimension
            // # Target is in dimension %s. Travel there before highlighting it.
            // # zh_CN 目标在维度 %s，请前往该维度后再高亮。
            case "highlight_dimension" -> "gtnr.wireless.gui.highlight_dimension";
            // #tr gtnr.wireless.gui.highlight_started
            // # Marked %s positions for 5 seconds (loaded targets within 256 blocks; max 512).
            // # zh_CN 已标记 %s 个位置，持续 5 秒（仅显示 256 格内已加载目标，最多 512 个）。
            case "highlight_started" -> "gtnr.wireless.gui.highlight_started";
            // #tr gtnr.wireless.gui.detail_title
            // # Wireless Entrances
            // # zh_CN 无线入口明细
            case "detail_title" -> "gtnr.wireless.gui.detail_title";
            // #tr gtnr.wireless.gui.matched
            // # Matches: %s
            // # zh_CN 匹配簇：%s
            case "matched" -> "gtnr.wireless.gui.matched";
            // #tr gtnr.wireless.gui.auto_on
            // # Auto-connect: ON
            // # zh_CN 自动连接：开启
            case "auto_on" -> "gtnr.wireless.gui.auto_on";
            // #tr gtnr.wireless.gui.auto_off
            // # Auto-connect: OFF
            // # zh_CN 自动连接：关闭
            case "auto_off" -> "gtnr.wireless.gui.auto_off";
            // #tr gtnr.wireless.gui.visualisation
            // # Network view: %s
            // # zh_CN 网络可视化：%s
            case "visualisation" -> "gtnr.wireless.gui.visualisation";
            // #tr gtnr.wireless.gui.visualisation_button
            // # View: %s
            // # zh_CN 显示：%s
            case "visualisation_button" -> "gtnr.wireless.gui.visualisation_button";
            // #tr gtnr.wireless.gui.back_compact
            // # Back
            // # zh_CN 返回
            case "back_compact" -> "gtnr.wireless.gui.back_compact";
            // #tr gtnr.wireless.gui.visualisation.full.short
            // # All
            // # zh_CN 全部
            case "visualisation.full.short" -> "gtnr.wireless.gui.visualisation.full.short";
            // #tr gtnr.wireless.gui.visualisation.nodes.short
            // # Nodes
            // # zh_CN 节点
            case "visualisation.nodes.short" -> "gtnr.wireless.gui.visualisation.nodes.short";
            // #tr gtnr.wireless.gui.visualisation.channels.short
            // # Channels
            // # zh_CN 频道
            case "visualisation.channels.short" -> "gtnr.wireless.gui.visualisation.channels.short";
            // #tr gtnr.wireless.gui.visualisation.nonum.short
            // # Nodes+links
            // # zh_CN 节点+连线
            case "visualisation.nonum.short" -> "gtnr.wireless.gui.visualisation.nonum.short";
            // #tr gtnr.wireless.gui.visualisation.nodes_one_channel.short
            // # N.+source
            // # zh_CN 节点+源连线
            case "visualisation.nodes_one_channel.short" -> "gtnr.wireless.gui.visualisation.nodes_one_channel.short";
            // #tr gtnr.wireless.gui.visualisation.one_channel.short
            // # Source
            // # zh_CN 源连线
            case "visualisation.one_channel.short" -> "gtnr.wireless.gui.visualisation.one_channel.short";
            // #tr gtnr.wireless.gui.visualisation.p2p.short
            // # P2P
            // # zh_CN P2P
            case "visualisation.p2p.short" -> "gtnr.wireless.gui.visualisation.p2p.short";
            // #tr gtnr.wireless.gui.visualisation.proxy.short
            // # Proxies
            // # zh_CN 合成代理
            case "visualisation.proxy.short" -> "gtnr.wireless.gui.visualisation.proxy.short";
            // #tr gtnr.wireless.gui.visualisation_off
            // # Off
            // # zh_CN 关闭
            case "visualisation_off" -> "gtnr.wireless.gui.visualisation_off";
            // #tr gtnr.wireless.gui.visualisation.full
            // # Everything (with channel counts)
            // # zh_CN 全部（含频道数）
            case "visualisation.full" -> "gtnr.wireless.gui.visualisation.full";
            // #tr gtnr.wireless.gui.visualisation.nodes
            // # Nodes
            // # zh_CN 节点
            case "visualisation.nodes" -> "gtnr.wireless.gui.visualisation.nodes";
            // #tr gtnr.wireless.gui.visualisation.channels
            // # Channel links
            // # zh_CN 频道连线
            case "visualisation.channels" -> "gtnr.wireless.gui.visualisation.channels";
            // #tr gtnr.wireless.gui.visualisation.nonum
            // # Nodes and links (no numbers)
            // # zh_CN 节点与连线（无数字）
            case "visualisation.nonum" -> "gtnr.wireless.gui.visualisation.nonum";
            // #tr gtnr.wireless.gui.visualisation.nodes_one_channel
            // # Nodes and remote source links
            // # zh_CN 节点与源节点远程连线
            case "visualisation.nodes_one_channel" -> "gtnr.wireless.gui.visualisation.nodes_one_channel";
            // #tr gtnr.wireless.gui.visualisation.one_channel
            // # Remote source links
            // # zh_CN 源节点远程连线
            case "visualisation.one_channel" -> "gtnr.wireless.gui.visualisation.one_channel";
            // #tr gtnr.wireless.gui.visualisation.p2p
            // # P2P links
            // # zh_CN P2P 连线
            case "visualisation.p2p" -> "gtnr.wireless.gui.visualisation.p2p";
            // #tr gtnr.wireless.gui.visualisation.proxy
            // # Crafting input proxy links
            // # zh_CN 合成输入代理连线
            case "visualisation.proxy" -> "gtnr.wireless.gui.visualisation.proxy";
            // #tr gtnr.wireless.gui.visualisation_hint
            // # Hold or wear the card to view the network. Change views here or with the hotkey.
            // # zh_CN 手持或佩戴此卡查看网络，在此处或用快捷键切换视图。
            case "visualisation_hint" -> "gtnr.wireless.gui.visualisation_hint";
            // #tr gtnr.wireless.gui.refresh
            // # Refresh
            // # zh_CN 刷新
            case "refresh" -> "gtnr.wireless.gui.refresh";
            // #tr gtnr.wireless.gui.source
            // # Controller: %s
            // # zh_CN 控制器：%s
            case "source" -> "gtnr.wireless.gui.source";
            // #tr gtnr.wireless.gui.unbound
            // # Frequency card is unbound
            // # zh_CN 频率卡尚未绑定
            case "unbound" -> "gtnr.wireless.gui.unbound";
            // #tr gtnr.wireless.gui.bind_hint
            // # Sneak-use an ME Controller to bind
            // # zh_CN 先潜行右击 ME 控制器绑定频率
            case "bind_hint" -> "gtnr.wireless.gui.bind_hint";
            // #tr gtnr.wireless.gui.source_waiting
            // # Source unloaded; waiting for recovery
            // # zh_CN 发射端未加载，等待恢复
            case "source_waiting" -> "gtnr.wireless.gui.source_waiting";
            // #tr gtnr.wireless.gui.unlimited
            // # AE2 channel limits are disabled
            // # zh_CN AE2 频道限制已关闭
            case "unlimited" -> "gtnr.wireless.gui.unlimited";
            // #tr gtnr.wireless.gui.empty
            // # No loaded clusters; unloaded records are retained
            // # zh_CN 没有已加载的簇；未加载区块的记录仍保留
            case "empty" -> "gtnr.wireless.gui.empty";
            // #tr gtnr.wireless.gui.loaded
            // # Loaded groups: %s
            // # zh_CN 已加载 %s 组
            case "loaded" -> "gtnr.wireless.gui.loaded";
            // #tr gtnr.wireless.gui.initializing
            // # Identifying multiblocks... The list will appear when their startup checks finish.
            // # zh_CN 正在识别多方块… 启动检查完成后将显示列表。
            case "initializing" -> "gtnr.wireless.gui.initializing";
            // #tr gtnr.wireless.gui.entrances
            // # Links %s
            // # zh_CN 接入点 %s
            case "entrances" -> "gtnr.wireless.gui.entrances";
            // #tr gtnr.wireless.gui.usage
            // # Entrance channels: %s
            // # zh_CN 当前入口频道占用：%s
            case "usage" -> "gtnr.wireless.gui.usage";
            // #tr gtnr.wireless.gui.usage_waiting
            // # Channels: waiting for pathing
            // # zh_CN 频道占用：等待网络稳定
            case "usage_waiting" -> "gtnr.wireless.gui.usage_waiting";
            // #tr gtnr.wireless.gui.usage_disabled
            // # Channel limits disabled
            // # zh_CN 已关闭频道限制
            case "usage_disabled" -> "gtnr.wireless.gui.usage_disabled";
            // #tr gtnr.wireless.gui.no_runtime
            // # No runtime entrance
            // # zh_CN 当前无运行时入口
            case "no_runtime" -> "gtnr.wireless.gui.no_runtime";
            // #tr gtnr.wireless.gui.denied
            // # Cannot change: target changed, AE permission denied, or source unloaded.
            // # zh_CN 无法修改：目标已变化、AE 权限不足或发射源未加载。
            case "denied" -> "gtnr.wireless.gui.denied";
            // #tr gtnr.wireless.gui.stale
            // # Physical cluster changed. Retry after refreshing.
            // # zh_CN 物理簇已变化，请在刷新后重新操作。
            case "stale" -> "gtnr.wireless.gui.stale";
            // #tr gtnr.wireless.gui.filter.5
            // # No channel
            // # zh_CN 缺频道
            case "filter.5" -> "gtnr.wireless.gui.filter.5";
            // #tr gtnr.wireless.gui.chip_all_tip
            // # Show every device group
            // # zh_CN 显示全部设备组
            case "chip_all_tip" -> "gtnr.wireless.gui.chip_all_tip";
            // #tr gtnr.wireless.gui.chip_tip
            // # Show only "%s" device groups
            // # zh_CN 只看“%s”的设备组
            case "chip_tip" -> "gtnr.wireless.gui.chip_tip";
            // #tr gtnr.wireless.gui.chip_count
            // # %s groups
            // # zh_CN %s 组
            case "chip_count" -> "gtnr.wireless.gui.chip_count";
            // #tr gtnr.wireless.gui.dimension_all
            // # All dimensions
            // # zh_CN 所有维度
            case "dimension_all" -> "gtnr.wireless.gui.dimension_all";
            // #tr gtnr.wireless.gui.dimension_option
            // # %s D%s · %s
            // # zh_CN %s D%s · %s
            case "dimension_option" -> "gtnr.wireless.gui.dimension_option";
            // #tr gtnr.wireless.gui.merge_types
            // # Merge types
            // # zh_CN 合并同类
            case "merge_types" -> "gtnr.wireless.gui.merge_types";
            default -> "gtnr.wireless.gui." + suffix;
        };
        return StatCollector.translateToLocalFormatted(key, args);
    }
}
