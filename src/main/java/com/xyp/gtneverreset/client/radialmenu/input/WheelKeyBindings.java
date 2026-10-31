package com.xyp.gtneverreset.client.radialmenu.input;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class WheelKeyBindings {

    public static final String CATEGORY = "key.categories.gtneverreset";

    public static KeyBinding openWheel;

    public static KeyBinding nextProfile;

    public static KeyBinding previousProfile;

    public static KeyBinding openEditor;

    public static void register() {
        // #tr key.toolbelt.open
        // # Open Radial Menu
        // # zh_CN 打开轮盘菜单
        openWheel = new KeyBinding("key.toolbelt.open", Keyboard.KEY_R, CATEGORY);
        // #tr key.toolbelt.cycle.right
        // # Next Radial Menu Profile
        // # zh_CN 下一个轮盘配置档
        nextProfile = new KeyBinding("key.toolbelt.cycle.right", Keyboard.KEY_NONE, CATEGORY);
        // #tr key.toolbelt.cycle.left
        // # Previous Radial Menu Profile
        // # zh_CN 上一个轮盘配置档
        previousProfile = new KeyBinding("key.toolbelt.cycle.left", Keyboard.KEY_NONE, CATEGORY);
        // #tr key.radialmenu.openEditor
        // # Open profile editor
        // # zh_CN 打开配置编辑器
        openEditor = new KeyBinding("key.radialmenu.openEditor", Keyboard.KEY_NONE, CATEGORY);

        ClientRegistry.registerKeyBinding(openWheel);
        ClientRegistry.registerKeyBinding(nextProfile);
        ClientRegistry.registerKeyBinding(previousProfile);
        ClientRegistry.registerKeyBinding(openEditor);
    }
}
