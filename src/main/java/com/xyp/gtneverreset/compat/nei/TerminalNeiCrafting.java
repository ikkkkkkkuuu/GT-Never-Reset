package com.xyp.gtneverreset.compat.nei;

import java.lang.reflect.Field;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.me.ItemRepo;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;
import appeng.util.item.AEItemStack;
import codechicken.nei.recipe.StackInfo;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class TerminalNeiCrafting {

    private static final Field ITEM_REPO = findField(GuiMEMonitorable.class, "repo");
    private static final Field ITEM_REPO_LIST = findField(ItemRepo.class, "list");

    private TerminalNeiCrafting() {}

    public static boolean requestCraft(GuiScreen screen, ItemStack stack) {
        if (screen instanceof QuickEncodingTerminalGui terminal && terminal.requestFluidCraft(stack)) return true;
        if (!(screen instanceof GuiMEMonitorable gui)
            || !(gui.inventorySlots instanceof ContainerMEMonitorable container)) return false;
        IAEItemStack item = AEItemStack.create(stack);
        if (!isCraftable(gui, item)) return false;
        container.setTargetStack(item);
        NetworkHandler.instance.sendToServer(
            new PacketInventoryAction(InventoryAction.AUTO_CRAFT, -1, Math.max(1, StackInfo.getAmount(stack))));
        return true;
    }

    public static boolean isCraftable(GuiMEMonitorable gui, IAEStack<?> stack) {
        if (stack == null || ITEM_REPO == null || ITEM_REPO_LIST == null) return false;
        try {
            ItemRepo repo = (ItemRepo) ITEM_REPO.get(gui);
            if (repo == null) return false;
            // Query the full network list so search and view filters do not hide craftable items.
            @SuppressWarnings("unchecked")
            IItemList<IAEStack<?>> items = (IItemList<IAEStack<?>>) ITEM_REPO_LIST.get(repo);
            IAEStack<?> available = items == null ? null : items.findPrecise(stack);
            return available != null && available.isCraftable();
        } catch (IllegalAccessException ignored) {
            return false;
        }
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
