package com.xyp.gtneverreset.utils.inventory;

import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtneverreset.common.gui.WirelessTerminalGuiHandler;
import com.xyp.gtneverreset.common.item.items.ItemWirelessDualInterfaceTerminal;
import com.xyp.gtneverreset.container.ContainerQuickEncodingTerminal;
import com.xyp.gtneverreset.utils.enums.WirelessTerminalGuiType;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import baubles.api.BaublesApi;

public final class WirelessTerminalSession {

    public static final int BAUBLE_SLOT_OFFSET = 1000;
    private static final String LAST_GUI_MODE = "last_gui_mode";

    private WirelessTerminalSession() {}

    public static void open(EntityPlayer player, int slot, WirelessTerminalGuiType requested) {
        ItemStack stack = getTerminalInSlot(player, slot);
        if (stack == null || !(stack.getItem() instanceof ItemWirelessDualInterfaceTerminal item)) return;
        String key = item.getEncryptionKey(stack);
        if (key == null || key.isEmpty()) {
            player.addChatMessage(PlayerMessages.DeviceNotLinked.get());
            return;
        }
        if (!item.hasPower(player, 0.5, stack)) {
            player.addChatMessage(PlayerMessages.DeviceNotPowered.get());
            return;
        }
        if (player.openContainer instanceof ContainerQuickEncodingTerminal container) container.restoreNativePinRows();
        WirelessTerminalGuiType type = requested == null
            ? getLastGuiMode(stack, WirelessTerminalGuiType.WirelessDualInterfaceTerminal)
            : requested;
        if (requested != null) setLastGuiMode(player, slot, type);
        WirelessTerminalGuiHandler.openGui(player, slot, type);
    }

    public static ItemStack getTerminalInSlot(EntityPlayer player, int slot) {
        if (slot == -1) return player.getCurrentEquippedItem();
        if (slot >= BAUBLE_SLOT_OFFSET) {
            IInventory baubles = BaublesApi.getBaubles(player);
            int index = slot - BAUBLE_SLOT_OFFSET;
            return baubles != null && index < baubles.getSizeInventory() ? baubles.getStackInSlot(index) : null;
        }
        return slot >= 0 && slot < player.inventory.getSizeInventory() ? player.inventory.getStackInSlot(slot) : null;
    }

    public static int findDualInterfaceTerminal(EntityPlayer player) {
        return findTerminalSlot(player, null);
    }

    public static int findTerminalSlot(EntityPlayer player, ItemStack wanted) {
        for (int slot = 0; slot < player.inventory.mainInventory.length; slot++) {
            if (matches(player.inventory.mainInventory[slot], wanted)) return slot;
        }
        IInventory baubles = BaublesApi.getBaubles(player);
        if (baubles != null) {
            for (int slot = 0; slot < baubles.getSizeInventory(); slot++) {
                if (matches(baubles.getStackInSlot(slot), wanted)) return BAUBLE_SLOT_OFFSET + slot;
            }
        }
        return -1;
    }

    public static void writeBackTerminal(EntityPlayer player, int slot, ItemStack stack) {
        if (stack == null || getTerminalInSlot(player, slot) != stack) return;
        if (slot >= BAUBLE_SLOT_OFFSET) {
            IInventory baubles = BaublesApi.getBaubles(player);
            int index = slot - BAUBLE_SLOT_OFFSET;
            if (baubles != null && index < baubles.getSizeInventory()) {
                baubles.setInventorySlotContents(index, stack);
                try {
                    Method save = baubles.getClass()
                        .getMethod("saveNBT", NBTTagCompound.class);
                    save.invoke(baubles, player.getEntityData());
                } catch (ReflectiveOperationException ignored) {}
            }
        } else if (slot >= 0 && slot < player.inventory.getSizeInventory()) {
            player.inventory.setInventorySlotContents(slot, stack);
        }
    }

    public static void setLastGuiMode(EntityPlayer player, int slot, WirelessTerminalGuiType type) {
        ItemStack stack = getTerminalInSlot(player, slot);
        if (stack == null || type == null) return;
        Platform.openNbtData(stack)
            .setByte(LAST_GUI_MODE, (byte) type.ordinal());
        writeBackTerminal(player, slot, stack);
    }

    public static WirelessTerminalGuiType getLastGuiMode(ItemStack stack, WirelessTerminalGuiType fallback) {
        if (stack == null || !stack.hasTagCompound()
            || !stack.getTagCompound()
                .hasKey(LAST_GUI_MODE))
            return fallback;
        WirelessTerminalGuiType type = WirelessTerminalGuiType.getByOrdinal(
            stack.getTagCompound()
                .getByte(LAST_GUI_MODE));
        return type == null ? fallback : type;
    }

    public static void storeHeldItem(EntityPlayerMP player, int slot) {
        if (slot < 0 || slot >= player.inventory.mainInventory.length) return;
        ItemStack held = player.inventory.getStackInSlot(slot);
        if (held == null || held.getItem() instanceof ItemWirelessDualInterfaceTerminal) return;
        WirelessTerminalGuiObject host = storageHost(player, SecurityPermissions.INJECT);
        if (host == null || host.getItemInventory() == null) return;
        ItemStack original = held.copy();
        IAEItemStack remainder = Platform.poweredInsert(
            host,
            host.getItemInventory(),
            AEItemStack.create(original.copy()),
            new PlayerSource(player, host));
        int left = remainder == null ? 0 : (int) Math.max(0, Math.min(original.stackSize, remainder.getStackSize()));
        if (left == original.stackSize) return;
        original.stackSize = left;
        player.inventory.setInventorySlotContents(slot, left == 0 ? null : original);
        syncInventory(player, host);
    }

    public static void requestItem(EntityPlayerMP player, ItemStack requested, boolean fullStack) {
        if (requested == null || requested.getItem() == null) return;
        WirelessTerminalGuiObject host = storageHost(player, SecurityPermissions.EXTRACT);
        if (host == null || host.getItemInventory() == null) return;
        ItemStack offer = requested.copy();
        offer.stackSize = fullStack ? offer.getMaxStackSize() : 1;
        int free = availableSpace(player, offer);
        if (free == 0) return;
        IAEItemStack request = AEItemStack.create(offer);
        request.setStackSize(Math.min(offer.stackSize, free));
        IAEItemStack extracted = Platform
            .poweredExtraction(host, host.getItemInventory(), request, new PlayerSource(player, host));
        if (extracted != null) {
            ItemStack result = extracted.getItemStack();
            if (!player.inventory.addItemStackToInventory(result)) player.dropPlayerItemWithRandomChoice(result, false);
            syncInventory(player, host);
        }
    }

    private static WirelessTerminalGuiObject storageHost(EntityPlayerMP player, SecurityPermissions permission) {
        int slot = findDualInterfaceTerminal(player);
        if (slot < 0) return null;
        WirelessTerminalGuiObject host = WirelessTerminalGuiHandler
            .buildHost(player, player.worldObj, slot, WirelessTerminalGuiType.WirelessCraftingTerminal);
        if (host == null || !host.rangeCheck() || host.getGrid() == null) return null;
        ISecurityGrid security = host.getGrid()
            .getCache(ISecurityGrid.class);
        return security.hasPermission(player, permission) ? host : null;
    }

    private static int availableSpace(EntityPlayer player, ItemStack incoming) {
        int space = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack == null) space += Math.min(incoming.getMaxStackSize(), player.inventory.getInventoryStackLimit());
            else if (stack.isItemEqual(incoming) && ItemStack.areItemStackTagsEqual(stack, incoming)) {
                space += Math.max(
                    0,
                    Math.min(stack.getMaxStackSize(), player.inventory.getInventoryStackLimit()) - stack.stackSize);
            }
        }
        return space;
    }

    private static void syncInventory(EntityPlayerMP player, WirelessTerminalGuiObject host) {
        int nativeSlot = host.getInventorySlot();
        int slot = nativeSlot >= Platform.baublesSlotsOffset
            ? BAUBLE_SLOT_OFFSET + nativeSlot - Platform.baublesSlotsOffset
            : nativeSlot;
        writeBackTerminal(player, slot, host.getItemStack());
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
    }

    private static boolean matches(ItemStack stack, ItemStack wanted) {
        return stack != null && stack.getItem() instanceof ItemWirelessDualInterfaceTerminal
            && (wanted == null || stack == wanted);
    }

}
