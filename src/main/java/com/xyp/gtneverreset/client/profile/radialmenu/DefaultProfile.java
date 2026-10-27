package com.xyp.gtneverreset.client.profile.radialmenu;

import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.client.action.radialmenu.ToolBeltMenu;
import com.xyp.gtneverreset.utils.gui.radialmenu.ActionSpec;
import com.xyp.gtneverreset.utils.gui.radialmenu.ActionTypes;
import com.xyp.gtneverreset.utils.gui.radialmenu.IconSpec;
import com.xyp.gtneverreset.utils.gui.radialmenu.MenuNode;
import com.xyp.gtneverreset.utils.gui.radialmenu.Profile;
import com.xyp.gtneverreset.utils.gui.radialmenu.SlotLayout;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class DefaultProfile {

    private DefaultProfile() {}

    public static Profile create() {
        Profile profile = Profile.empty("default");
        profile.root.layout = SlotLayout.fixed(8);

        profile.root.setChildAt(0, ToolBeltMenu.create());
        profile.root.setChildAt(
            2,
            keybind(
                StatCollector.translateToLocal("key.inventory"),
                IconSpec.item("minecraft:chest", 0),
                "key.inventory",
                "tap",
                false));
        // #tr radialmenu.default.movement
        // # Movement
        // # zh_CN 移动
        MenuNode movement = MenuNode.category(
            StatCollector.translateToLocal("radialmenu.default.movement"),
            IconSpec.item("minecraft:golden_boots", 0),
            SlotLayout.fixed(3));
        movement.layout.opening = SlotLayout.Opening.Inline;
        movement.setChildAt(
            0,
            keybind(
                StatCollector.translateToLocal("key.sprint"),
                IconSpec.item("minecraft:golden_boots", 0),
                "key.sprint",
                "toggle",
                false));
        movement.setChildAt(
            1,
            keybind(
                StatCollector.translateToLocal("key.sneak"),
                IconSpec.item("minecraft:leather_boots", 0),
                "key.sneak",
                "toggle",
                false));
        movement.setChildAt(
            2,
            keybind(
                StatCollector.translateToLocal("key.jump"),
                IconSpec.item("minecraft:feather", 0),
                "key.jump",
                "hold",
                false));
        profile.root.setChildAt(4, movement);
        // #tr radialmenu.default.editor
        // # Edit Profiles
        // # zh_CN 编辑轮盘配置
        profile.root.setChildAt(
            6,
            keybind(
                StatCollector.translateToLocal("radialmenu.default.editor"),
                IconSpec.sprite("gear", null),
                "key.radialmenu.openEditor",
                "tap",
                false));

        profile.root.ensureSlotCapacity();
        return profile;
    }

    private static MenuNode keybind(String title, IconSpec icon, String binding, String mode, boolean keepOpen) {
        ActionSpec action = new ActionSpec(ActionTypes.KEYBIND).set(ActionTypes.PARAM_BINDING, binding)
            .set(ActionTypes.PARAM_MODE, mode)
            .set(ActionTypes.PARAM_HOLD_TICKS, "10");
        MenuNode node = MenuNode.leaf(title, icon, action);
        node.keepOpen = keepOpen;
        return node;
    }
}
