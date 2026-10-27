package com.xyp.gtneverreset.common.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.item.ItemWirelessDualInterfaceTerminal;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.terminal.CPacketSwitchGuis;
import com.xyp.gtneverreset.container.ContainerQuickEncodingTerminal;
import com.xyp.gtneverreset.utils.enums.WirelessTerminalGuiType;
import com.xyp.gtneverreset.utils.inventory.DualTerminalGuiObject;
import com.xyp.gtneverreset.utils.inventory.WirelessTerminalSession;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.security.ISecurityGrid;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftingTerm;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.items.contents.WirelessCraftingTerminalGuiObject;
import cpw.mods.fml.common.network.IGuiHandler;

public final class WirelessTerminalGuiHandler implements IGuiHandler {

    public static void openGui(EntityPlayer player, int slot, WirelessTerminalGuiType type) {
        if (!player.worldObj.isRemote && type != null) {
            player.openGui(GTNeverReset.INSTANCE, type.ordinal(), player.worldObj, slot, 0, 0);
        }
    }

    public static void switchGui(WirelessTerminalGuiType type) {
        NetWorkHandler.sendToServer(new CPacketSwitchGuis(type));
    }

    public static WirelessTerminalGuiObject buildHost(EntityPlayer player, World world, int slot,
        WirelessTerminalGuiType type) {
        ItemStack stack = WirelessTerminalSession.getTerminalInSlot(player, slot);
        if (stack == null || !(stack.getItem() instanceof ItemWirelessDualInterfaceTerminal handler)) return null;
        int nativeSlot = slot >= WirelessTerminalSession.BAUBLE_SLOT_OFFSET
            ? appeng.util.Platform.baublesSlotsOffset + slot - WirelessTerminalSession.BAUBLE_SLOT_OFFSET
            : slot;
        return type == WirelessTerminalGuiType.WirelessDualInterfaceTerminal
            ? new DualTerminalGuiObject(handler, stack, player, world, nativeSlot)
            : new WirelessCraftingTerminalGuiObject(handler, stack, player, world, nativeSlot, 0, 0);
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int slot, int y, int z) {
        WirelessTerminalGuiType type = WirelessTerminalGuiType.getByOrdinal(id);
        if (type == null) return null;
        WirelessTerminalGuiObject host = buildHost(player, world, slot, type);
        if (host == null) return null;
        if (host.getActionableNode() == null) {
            player.addChatMessage(PlayerMessages.OutOfRange.get());
            return null;
        }
        ISecurityGrid security = host.getGrid()
            .getCache(ISecurityGrid.class);
        if (!security.hasPermission(player, SecurityPermissions.CRAFT)
            || type == WirelessTerminalGuiType.WirelessDualInterfaceTerminal
                && !security.hasPermission(player, SecurityPermissions.BUILD)) {
            // #tr gtneverreset.message.terminal_access_denied
            // # You do not have permission to use this terminal on the ME network.
            // # zh_CN 你没有在该 ME 网络上使用此终端的权限。
            player.addChatMessage(new ChatComponentTranslation("gtneverreset.message.terminal_access_denied"));
            return null;
        }
        AEBaseContainer container = host instanceof DualTerminalGuiObject dual
            ? new ContainerQuickEncodingTerminal(player.inventory, dual)
            : new ContainerCraftingTerm(player.inventory, (WirelessCraftingTerminalGuiObject) host);
        ContainerOpenContext context = new ContainerOpenContext(host);
        context.setWorld(world);
        context.setX(slot);
        context.setSide(ForgeDirection.UNKNOWN);
        container.setOpenContext(context);
        return container;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int slot, int y, int z) {
        return GTNeverReset.proxy.createTerminalGui(id, player, world, slot);
    }
}
