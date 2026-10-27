package com.xyp.gtneverreset.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.terminal.CPacketSendHeldItemToNetwork;
import com.xyp.gtneverreset.common.packet.terminal.CPacketSwitchGuis;
import com.xyp.gtneverreset.common.packet.terminal.TerminalItemRequestPacket;
import com.xyp.gtneverreset.utils.inventory.WirelessTerminalSession;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

public final class WirelessTerminalInputHandler {

    // #tr key.categories.gtneverreset
    // # GT-Never-Reset
    // # zh_CN GT-Never-Reset
    private static final String CATEGORY = "key.categories.gtneverreset";
    // #tr gtneverreset.key.open_dual_interface_terminal
    // # Open Dual Interface Terminal
    // # zh_CN 打开二合一接口终端
    public static final KeyBinding OPEN_TERMINAL = new KeyBinding(
        "gtneverreset.key.open_dual_interface_terminal",
        Keyboard.KEY_F,
        CATEGORY);
    // #tr gtneverreset.key.send_held_item_to_network
    // # Send Held Item to Network
    // # zh_CN 将手持物品存入 ME 网络
    public static final KeyBinding SEND_HELD_ITEM = new KeyBinding(
        "gtneverreset.key.send_held_item_to_network",
        Keyboard.KEY_C,
        CATEGORY);

    @SubscribeEvent
    public void onInput(InputEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        if (player == null || minecraft.currentScreen != null) return;
        if (OPEN_TERMINAL.isPressed()) NetWorkHandler.sendToServer(CPacketSwitchGuis.restoreLast());
        if (SEND_HELD_ITEM.isPressed() && player.inventory.getCurrentItem() != null) {
            NetWorkHandler.sendToServer(new CPacketSendHeldItemToNetwork(player.inventory.currentItem));
        }
        if (!(event instanceof InputEvent.MouseInputEvent) || !Mouse.getEventButtonState()
            || Mouse.getEventButton() != 2
            || player.capabilities.isCreativeMode
            || player.inventory.getCurrentItem() != null
            || WirelessTerminalSession.findDualInterfaceTerminal(player) < 0) return;
        MovingObjectPosition hit = minecraft.objectMouseOver;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        ItemStack block = player.worldObj.getBlock(hit.blockX, hit.blockY, hit.blockZ)
            .getPickBlock(hit, player.worldObj, hit.blockX, hit.blockY, hit.blockZ, player);
        if (block == null) return;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack != null && stack.isItemEqual(block) && ItemStack.areItemStackTagsEqual(stack, block)) return;
        }
        NetWorkHandler.sendToServer(new TerminalItemRequestPacket(block, !GuiScreen.isShiftKeyDown()));
    }

}
