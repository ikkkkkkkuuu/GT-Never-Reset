package com.xyp.gtneverreset.common.item.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.GTNRCreativeTabs;
import com.xyp.gtneverreset.client.event.WirelessTerminalInputHandler;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.ModsItemlist;
import com.xyp.gtneverreset.utils.inventory.WirelessTerminalSession;

import appeng.api.AEApi;
import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.items.contents.WirelessCraftingTerminalGuiObject;
import appeng.items.tools.powered.ToolWirelessTerminal;
import baubles.api.expanded.BaubleExpandedSlots;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ItemWirelessDualInterfaceTerminal extends ToolWirelessTerminal implements IGuiItem {

    public static final String REGISTRY_NAME = "wireless_dual_interface_terminal";

    public ItemWirelessDualInterfaceTerminal() {
        // #tr item.wireless_dual_interface_terminal.name
        // # ME Wireless Dual Interface Terminal
        // # zh_CN ME无线二合一接口终端
        setUnlocalizedName(REGISTRY_NAME);
        setTextureName(GTNeverReset.MODID + ":" + REGISTRY_NAME);
        setCreativeTab(GTNRCreativeTabs.ITEMS);
        GameRegistry.registerItem(this, REGISTRY_NAME);
        GTNRItemList.WirelessDualInterfaceTerminal.set(this);
        AEApi.instance()
            .registries()
            .wireless()
            .registerWirelessHandler(this);
    }

    @Override
    public boolean canHandle(ItemStack stack) {
        return stack != null && stack.getItem() == this;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) WirelessTerminalSession.open(player, player.inventory.currentItem, null);
        return stack;
    }

    @Override
    public void openGui(ItemStack stack, World world, EntityPlayer player, Object mode) {
        if (world.isRemote) return;
        int slot = WirelessTerminalSession.findTerminalSlot(player, stack);
        if (slot >= 0) WirelessTerminalSession.open(player, slot, null);
    }

    @Override
    public boolean hasInfinityPower(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound()
            .getBoolean(infinityEnergyCard);
    }

    @Override
    public boolean hasInfinityRange(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound()
            .getBoolean(infinityBoosterCard);
    }

    @Override
    public boolean hasPower(EntityPlayer player, double amount, ItemStack stack) {
        return hasInfinityPower(stack) || super.hasPower(player, amount, stack);
    }

    @Override
    public boolean usePower(EntityPlayer player, double amount, ItemStack stack) {
        return hasInfinityPower(stack) || super.usePower(player, amount, stack);
    }

    @Override
    public String[] getBaubleTypes(ItemStack stack) {
        List<String> types = new ArrayList<>();
        for (String type : BaubleExpandedSlots.getCurrentlyRegisteredTypes()) {
            if (type != null && !type.equals(BaubleExpandedSlots.unknownType)
                && !type.equals(BaubleExpandedSlots.invalidType)) types.add(type);
        }
        return types.toArray(new String[0]);
    }

    @Override
    public IGuiItemObject getGuiObject(ItemStack stack, World world, EntityPlayer player, int x, int y, int z) {
        return new WirelessCraftingTerminalGuiObject(this, stack, player, world, x, y, z);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean more) {
        super.addCheckedInformation(stack, player, lines, more);
        if (GuiScreen.isShiftKeyDown()) {
            // #tr gtneverreset.tooltip.wireless_dual_interface_terminal.desc
            // # Access ME interfaces and encode patterns wirelessly
            // # zh_CN 无线访问 ME 接口并编码样板
            lines.add(I18n.format("gtneverreset.tooltip.wireless_dual_interface_terminal.desc"));
            // #tr gtneverreset.tooltip.wireless_dual_interface_terminal.key_open
            // # §7Press §b%s§7 to open the terminal
            // # zh_CN §7按 §b%s§7 打开终端
            lines.add(
                I18n.format(
                    "gtneverreset.tooltip.wireless_dual_interface_terminal.key_open",
                    keyName(WirelessTerminalInputHandler.OPEN_TERMINAL)));
            // #tr gtneverreset.tooltip.wireless_dual_interface_terminal.key_send
            // # §7Press §b%s§7 to send the held item to the ME network
            // # zh_CN §7按 §b%s§7 将手持物品存入 ME 网络
            lines.add(
                I18n.format(
                    "gtneverreset.tooltip.wireless_dual_interface_terminal.key_send",
                    keyName(WirelessTerminalInputHandler.SEND_HELD_ITEM)));
            // #tr gtneverreset.tooltip.wireless_dual_interface_terminal.nei_to_inv
            // # §7Shift/Ctrl+middle-click an NEI item to request it from the ME network
            // # zh_CN §7Shift/Ctrl+中键点击 NEI 物品，从 ME 网络提取到物品栏
            lines.add(I18n.format("gtneverreset.tooltip.wireless_dual_interface_terminal.nei_to_inv"));
        } else {
            // #tr gtneverreset.tooltip.shift_for_more
            // # §7Hold §bShift§7 for controls and functions
            // # zh_CN §7按住 §bShift§7 查看功能和操作
            lines.add(I18n.format("gtneverreset.tooltip.shift_for_more"));
        }
        if (GuiScreen.isCtrlKeyDown()) {
            // #tr gtneverreset.tooltip.wireless.installed
            // # Installed cards:
            // # zh_CN 已安装的卡：
            lines.add(I18n.format("gtneverreset.tooltip.wireless.installed"));
            if (stack.getItem() instanceof ItemWirelessDualInterfaceTerminal terminal) {
                if (terminal.hasInfinityRange(stack)) addCardName(lines, ModsItemlist.InfinityBoosterCard.get(1));
                if (terminal.hasInfinityPower(stack)) addCardName(lines, ModsItemlist.EnergyCard.get(1));
            }
        } else {
            // #tr gtneverreset.tooltip.ctrl_for_more
            // # §7Hold §bCtrl§7 to view installed cards
            // # zh_CN §7按住 §bCtrl§7 查看已安装的卡
            lines.add(I18n.format("gtneverreset.tooltip.ctrl_for_more"));
        }
    }

    @SideOnly(Side.CLIENT)
    private static void addCardName(List<String> lines, ItemStack card) {
        if (card != null) lines.add("  " + EnumChatFormatting.GOLD + card.getDisplayName());
    }

    @SideOnly(Side.CLIENT)
    private static String keyName(KeyBinding key) {
        if (key == null || key.getKeyCode() == Keyboard.CHAR_NONE) {
            // #tr gtneverreset.tooltip.key_unbound
            // # Unbound
            // # zh_CN 未绑定
            return I18n.format("gtneverreset.tooltip.key_unbound");
        }
        return GameSettings.getKeyDisplayString(key.getKeyCode());
    }
}
