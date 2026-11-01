package com.xyp.gtneverreset.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.factory.GuiFactories;
import com.xyp.gtneverreset.common.item.items.InsectStaff;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class InsectStaffInputHandler {

    // #tr gtneverreset.key.configure_insect_staff
    // # Configure Insect Staff
    // # zh_CN 配置虫之权杖
    public static final KeyBinding CONFIGURE = new KeyBinding(
        "gtneverreset.key.configure_insect_staff",
        Keyboard.KEY_G,
        "key.categories.gtneverreset");

    @SubscribeEvent
    public void onInput(InputEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null || minecraft.currentScreen != null || !CONFIGURE.isPressed()) return;
        ItemStack held = minecraft.thePlayer.getHeldItem();
        if (held != null && held.getItem() instanceof InsectStaff) {
            GuiFactories.playerInventory()
                .openFromMainHandClient();
        }
    }
}
