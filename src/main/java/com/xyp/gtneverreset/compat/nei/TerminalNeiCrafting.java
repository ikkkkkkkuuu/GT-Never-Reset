package com.xyp.gtneverreset.compat.nei;

import java.lang.reflect.Field;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.client.gui.GuiQuickEncodingTerminal;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.me.ItemRepo;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketMonitorableAction;
import appeng.helpers.MonitorableAction;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class TerminalNeiCrafting {

    private static final Field ITEM_REPO = findField(GuiMEMonitorable.class, "repo");
    private static final Field ITEM_REPO_LIST = findField(ItemRepo.class, "list");

    private TerminalNeiCrafting() {}

    public static boolean requestCraft(GuiScreen screen, ItemStack stack) {
        if (screen instanceof GuiQuickEncodingTerminal terminal && terminal.requestFluidCraft(stack)) return true;
        if (!(screen instanceof GuiMEMonitorable gui)
            || !(gui.inventorySlots instanceof ContainerMEMonitorable container)
            || ITEM_REPO == null
            || ITEM_REPO_LIST == null) return false;
        IAEItemStack item = AEItemStack.create(stack);
        if (item == null) return false;
        try {
            ItemRepo repo = (ItemRepo) ITEM_REPO.get(gui);
            if (repo == null) return false;
            // Query the full network list so search and view filters do not hide craftable items.
            @SuppressWarnings("unchecked")
            IItemList<IAEStack<?>> items = (IItemList<IAEStack<?>>) ITEM_REPO_LIST.get(repo);
            IAEStack<?> available = items == null ? null : items.findPrecise(item);
            if (available == null || !available.isCraftable()) return false;
        } catch (IllegalAccessException ignored) {
            return false;
        }
        container.setTargetStack(item);
        NetworkHandler.instance.sendToServer(new PacketMonitorableAction(MonitorableAction.AUTO_CRAFT, -1));
        return true;
    }

    private static Field findField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
