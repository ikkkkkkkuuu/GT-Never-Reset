package com.xyp.gtneverreset.client.radialmenu;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ActionType {

    public final String id;

    public final String labelKey;

    public final List<ActionField> fields;

    public final boolean chain;

    public ActionType(String id, String labelKey, boolean chain, ActionField... fields) {
        this.id = id;
        this.labelKey = labelKey;
        this.chain = chain;
        this.fields = Collections.unmodifiableList(Arrays.asList(fields));
    }

    public String tooltipKey() {
        switch (labelKey) {
            // #tr gtnr.radialmenu.action.keybind
            // # Keybind
            // # zh_CN 按键绑定
            case "gtnr.radialmenu.action.keybind":
                // #tr gtnr.radialmenu.action.keybind.tooltip
                // # Trigger a keybinding, including bindings with no assigned key
                // # zh_CN 触发按键绑定，也支持未分配按键的绑定
                return "gtnr.radialmenu.action.keybind.tooltip";
            // #tr gtnr.radialmenu.action.profile_switch
            // # Profile
            // # zh_CN 配置
            case "gtnr.radialmenu.action.profile_switch":
                // #tr gtnr.radialmenu.action.profile_switch.tooltip
                // # Switch profiles. Leave blank to cycle to the next profile
                // # zh_CN 切换配置方案；留空则切换到下一个
                return "gtnr.radialmenu.action.profile_switch.tooltip";
            // #tr gtnr.radialmenu.action.command
            // # Command
            // # zh_CN 命令
            case "gtnr.radialmenu.action.command":
                // #tr gtnr.radialmenu.action.command.tooltip
                // # Send chat messages or slash commands
                // # zh_CN 发送聊天消息或斜杠命令
                return "gtnr.radialmenu.action.command.tooltip";
            // #tr gtnr.radialmenu.action.submenu
            // # Submenu
            // # zh_CN 子菜单
            case "gtnr.radialmenu.action.submenu":
                // #tr gtnr.radialmenu.action.submenu.tooltip
                // # Open a submenu. Shift-click a slot in it to edit its action
                // # zh_CN 打开子菜单；Shift+点击其中的槽位以编辑动作
                return "gtnr.radialmenu.action.submenu.tooltip";
            // #tr gtnr.radialmenu.action.sequence
            // # Chain
            // # zh_CN 操作链
            case "gtnr.radialmenu.action.sequence":
                // #tr gtnr.radialmenu.action.sequence.tooltip
                // # Run the configured steps in order
                // # zh_CN 按顺序执行设定的步骤
                return "gtnr.radialmenu.action.sequence.tooltip";
            // #tr gtnr.radialmenu.action.script
            // # Script
            // # zh_CN 脚本
            case "gtnr.radialmenu.action.script":
                // #tr gtnr.radialmenu.action.script.tooltip
                // # Run Lua scripts to read server replies and create menus. See the scripting guide
                // # zh_CN 运行 Lua 脚本，读取服务器回复并创建菜单；详见脚本指南
                return "gtnr.radialmenu.action.script.tooltip";
            default:
                return labelKey + ".tooltip";
        }
    }

    public ActionField field(String key) {
        for (ActionField field : fields) {
            if (field.key.equals(key)) {
                return field;
            }
        }
        return null;
    }

    public void applyDefaults(ActionSpec spec) {
        for (ActionField field : fields) {
            if (spec.getString(field.key, null) == null) {
                spec.set(field.key, field.defaultValue);
            }
        }
    }

    public boolean isComplete(ActionSpec spec) {
        for (ActionField field : fields) {
            if (field.isMissingFrom(spec)) {
                return false;
            }
        }
        return true;
    }

    public ActionSpec newSpec() {
        ActionSpec spec = new ActionSpec(id);
        applyDefaults(spec);
        return spec;
    }
}
