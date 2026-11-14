package com.xyp.gtneverreset.client.radialmenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.xyp.gtnrlib.gui.wheel.SlotLayout;

public final class ActionTypes {

    public static final String KEYBIND = "keybind";

    public static final String PROFILE_SWITCH = "profileSwitch";

    public static final String SEQUENCE = "sequence";

    public static final String COMMAND = "command";

    public static final String SUBMENU = "submenu";

    public static final String SCRIPT = "script";

    public static final String SCRIPT_RESUME = "scriptResume";

    public static final String PARAM_BINDING = "binding";

    public static final String PARAM_CATEGORY = "category";
    public static final String PARAM_MODE = "mode";

    public static final String PARAM_SHIFT = "shift";
    public static final String PARAM_CTRL = "ctrl";
    public static final String PARAM_ALT = "alt";

    public static final String PARAM_SNEAK = "sneak";
    public static final String PARAM_HOLD_TICKS = "holdTicks";
    public static final String PARAM_PROFILE = "profile";
    public static final String PARAM_COMMAND = "command";
    public static final String PARAM_DELAY_TICKS = "delayTicks";
    public static final String PARAM_CYCLE = "cycle";

    public static final String PARAM_SCRIPT = "script";

    public static final String PARAM_TIMEOUT_TICKS = "timeoutTicks";

    public static final String PARAM_TOKEN = "token";

    public static final String PARAM_CHOICE = "choice";

    public static final String PARAM_OPENING = "opening";
    public static final String PARAM_SLOT_MODE = "slotMode";
    public static final String PARAM_SLOT_COUNT = "slots";
    public static final String PARAM_ACCENT = "accent";
    public static final String PARAM_RING_COLOR = "ringColor";
    public static final String PARAM_HIGHLIGHT_COLOR = "highlightColor";
    public static final String PARAM_BORDER_COLOR = "borderColor";

    public static final String PARAM_ICON_COLOR = "iconColor";
    public static final String PARAM_HIGHLIGHT_BORDER_COLOR = "highlightBorderColor";
    public static final String PARAM_BACKGROUND_COLOR = "backgroundColor";

    private static final Map<String, ActionType> TYPES = new LinkedHashMap<>();

    public static void register(ActionType type) {
        TYPES.put(type.id, type);
    }

    public static ActionType get(String id) {
        return id == null ? null : TYPES.get(id);
    }

    public static boolean isRegistered(String id) {
        return id != null && TYPES.containsKey(id);
    }

    public static boolean isComplete(ActionSpec spec) {
        if (spec == null || spec.type == null) {
            return false;
        }
        ActionType type = get(spec.type);
        return type == null || type.isComplete(spec);
    }

    public static List<ActionType> all() {
        return Collections.unmodifiableList(new ArrayList<>(TYPES.values()));
    }

    public static void clear() {
        TYPES.clear();
    }

    public static void registerDefaults() {
        register(
            new ActionType(
                KEYBIND,
                "gtnr.radialmenu.action.keybind",
                false,
                // #tr gtnr.radialmenu.action.keybind.binding
                // # Keybinding
                // # zh_CN 绑定按键
                ActionField.keybindRef(PARAM_BINDING, "gtnr.radialmenu.action.keybind.binding")
                    .required(),
                ActionField
                    .enumeration(PARAM_MODE, "gtnr.radialmenu.action.keybind.mode", "tap", "tap", "toggle", "hold"),
                ActionField.integer(PARAM_HOLD_TICKS, "gtnr.radialmenu.action.keybind.hold_ticks", 20),

                ActionField.bool(PARAM_SHIFT, "gtnr.radialmenu.action.keybind.shift", false),
                ActionField.bool(PARAM_CTRL, "gtnr.radialmenu.action.keybind.ctrl", false),
                ActionField.bool(PARAM_ALT, "gtnr.radialmenu.action.keybind.alt", false),
                ActionField.bool(PARAM_SNEAK, "gtnr.radialmenu.action.keybind.sneak", false)));

        register(
            new ActionType(
                PROFILE_SWITCH,
                "gtnr.radialmenu.action.profile_switch",
                false,
                // #tr gtnr.radialmenu.action.profile_switch.profile
                // # Profile
                // # zh_CN 配置
                ActionField.profileRef(PARAM_PROFILE, "gtnr.radialmenu.action.profile_switch.profile")));

        register(
            new ActionType(
                COMMAND,
                "gtnr.radialmenu.action.command",
                false,
                // #tr gtnr.radialmenu.action.command.text
                // # Commands
                // # zh_CN 命令内容
                ActionField.multiline(PARAM_COMMAND, "gtnr.radialmenu.action.command.text", "")
                    .required(),
                ActionField.integer(PARAM_DELAY_TICKS, "gtnr.radialmenu.action.command.delay_ticks", 0),
                ActionField.bool(PARAM_CYCLE, "gtnr.radialmenu.action.command.cycle", false)));

        register(
            new ActionType(
                SUBMENU,
                "gtnr.radialmenu.action.submenu",
                false,

                ActionField.enumeration(
                    PARAM_OPENING,
                    "gtnr.radialmenu.action.submenu.opening",
                    "replace",
                    "replace",
                    "inline"),
                ActionField
                    .enumeration(PARAM_SLOT_MODE, "gtnr.radialmenu.action.submenu.mode", "fixed", "fixed", "dynamic"),
                ActionField.range(
                    PARAM_SLOT_COUNT,
                    "gtnr.radialmenu.action.submenu.slots",
                    SlotLayout.DEFAULT_SLOTS,
                    SlotLayout.MIN_SLOTS,
                    SlotLayout.MAX_SLOTS),
                ActionField.accent(
                    PARAM_ACCENT,
                    "gtnr.radialmenu.action.submenu.accent",
                    PARAM_RING_COLOR,
                    PARAM_HIGHLIGHT_COLOR,
                    PARAM_BORDER_COLOR,
                    PARAM_HIGHLIGHT_BORDER_COLOR,
                    PARAM_BACKGROUND_COLOR),

                ActionField.color(PARAM_RING_COLOR, "gtnr.radialmenu.action.submenu.ring_color"),
                ActionField.color(PARAM_BORDER_COLOR, "gtnr.radialmenu.action.submenu.border_color"),
                ActionField.color(PARAM_HIGHLIGHT_COLOR, "gtnr.radialmenu.action.submenu.highlight_color"),
                ActionField
                    .color(PARAM_HIGHLIGHT_BORDER_COLOR, "gtnr.radialmenu.action.submenu.highlight_border_color"),
                ActionField.color(PARAM_BACKGROUND_COLOR, "gtnr.radialmenu.action.submenu.background_color")));

        register(
            new ActionType(
                SCRIPT,
                "gtnr.radialmenu.action.script",
                false,
                ActionField.code(PARAM_SCRIPT, "gtnr.radialmenu.action.script.source", "")
                    .required(),
                ActionField.integer(PARAM_TIMEOUT_TICKS, "gtnr.radialmenu.action.script.timeout_ticks", 600)));

        register(
            new ActionType(
                SEQUENCE,
                "gtnr.radialmenu.action.sequence",
                true,
                ActionField.integer(PARAM_DELAY_TICKS, "gtnr.radialmenu.action.sequence.delay_ticks", 0)));
    }
}
