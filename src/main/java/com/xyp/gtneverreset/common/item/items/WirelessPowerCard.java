package com.xyp.gtneverreset.common.item.items;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.gtnewhorizon.gtnhlib.item.ItemStackNBT;
import com.xyp.gtneverreset.client.GTNRCreativeTabs;
import com.xyp.gtneverreset.common.gui.WirelessPowerCardGui;
import com.xyp.gtneverreset.common.wireless.IWirelessPowerReceiver;
import com.xyp.gtneverreset.common.wireless.WirelessCardInventory;
import com.xyp.gtneverreset.common.wireless.WirelessPowerSavedData;
import com.xyp.gtneverreset.common.wireless.WirelessPowerTransfer;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import baubles.api.BaubleType;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleExpanded;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.misc.spaceprojects.SpaceProjectManager;

public final class WirelessPowerCard extends Item implements IBaubleExpanded, IGuiHolder<PlayerInventoryGuiData> {

    public WirelessPowerCard() {
        // #tr item.gtneverreset.wireless_power_card.name
        // # Wireless Power Card
        // # zh_CN 无线电网卡
        setUnlocalizedName("gtneverreset.wireless_power_card");
        setTextureName("gtneverreset:wireless_power_card");
        setMaxStackSize(1);
        setCreativeTab(GTNRCreativeTabs.ITEMS);
        GameRegistry.registerItem(this, "wireless_power_card");
        GTNRItemList.WirelessPowerCard.set(this);
    }

    @Override
    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager sync, UISettings settings) {
        return WirelessPowerCardGui.build(data, sync, settings);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PlayerInventoryGuiData data, ModularPanel panel) {
        return WirelessPowerCardGui.createScreen(panel);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) return false;
        if (!(world.getTileEntity(x, y, z) instanceof BaseMetaTileEntity tile)
            || !WirelessPowerTransfer.supports(tile)) {
            // #tr power.card.unsupported
            // # Use on a GT EU machine or energy input hatch. Multiblocks receive power through their energy hatches.
            // # zh_CN 请对 GT EU 机器或能源输入仓使用；多方块通过能源仓接入。
            player.addChatMessage(new ChatComponentTranslation("power.card.unsupported"));
            return true;
        }
        UUID source = source(stack, player, true);
        var saved = WirelessPowerSavedData.get();
        var connection = saved == null ? null
            : saved.connection(((IWirelessPowerReceiver) tile).gtnr$powerConnection());
        if (connection != null && connection.owner.equals(player.getUniqueID())
            && connection.source.equals(source)
            && tile.isUseableByPlayer(player)
            && player.canPlayerEdit(x, y, z, side, stack)) {
            saved.remove(connection.id, player.getUniqueID());
            // #tr power.card.disconnected
            // # Disconnected from wireless power.
            // # zh_CN 已断开无线供电。
            player.addChatMessage(new ChatComponentTranslation("power.card.disconnected"));
        } else if (WirelessPowerTransfer.connect(tile, player, source)) {
            // #tr power.card.connected
            // # Connected to %s's wireless power network.
            // # zh_CN 已接入 %s 的无线电网。
            player.addChatMessage(new ChatComponentTranslation("power.card.connected", saved.name(source)));
        } else {
            // #tr power.card.denied
            // # Cannot connect: check machine access and the selected power network.
            // # zh_CN 无法接入：请检查机器操作权限和所选电网是否有效。
            player.addChatMessage(new ChatComponentTranslation("power.card.denied"));
        }
        return true;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) return stack;
        MovingObjectPosition hit = getMovingObjectPositionFromPlayer(world, player, false);
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) return stack;
        if (player.isSneaking()) {
            toggleAutomatic(stack, player);
            player.addChatMessage(new ChatComponentTranslation(automatic(stack) ? autoOnKey() : autoOffKey()));
        } else {
            source(stack, player, true);
            GuiFactories.playerInventory()
                .openFromMainHand(player);
        }
        return stack;
    }

    public static UUID source(ItemStack card, EntityPlayer player, boolean initialize) {
        UUID selected = WirelessPowerSavedData.uuid(card.getTagCompound(), "powerSource");
        if (selected != null) return SpaceProjectManager.getLeader(selected);
        if (initialize && !player.worldObj.isRemote) WirelessNetworkManager.strongCheckOrAddUser(player.getUniqueID());
        return SpaceProjectManager.getLeader(player.getUniqueID());
    }

    public static void selectSource(ItemStack card, EntityPlayer player, UUID source) {
        if (player.worldObj.isRemote || !WirelessPowerSavedData.sourceExists(source)) return;
        NBTTagCompound tag = ItemStackNBT.get(card);
        tag.setString("powerSource", source.toString());
        tag.setString(
            "powerSourceName",
            WirelessPowerSavedData.get()
                .name(source));
        WirelessCardInventory.sync(player, card);
    }

    public static boolean automatic(ItemStack card) {
        return card != null && card.stackSize > 0
            && card.getItem() instanceof WirelessPowerCard
            && (card.getTagCompound() == null || !card.getTagCompound()
                .getBoolean("disableAutoPower"));
    }

    public static void toggleAutomatic(ItemStack card, EntityPlayer player) {
        ItemStackNBT.get(card)
            .setBoolean("disableAutoPower", automatic(card));
        WirelessCardInventory.sync(player, card);
    }

    public static String autoOnKey() {
        // #tr power.card.auto_on
        // # Auto-connect: ON
        // # zh_CN 自动连接：开启
        return "power.card.auto_on";
    }

    public static String autoOffKey() {
        // #tr power.card.auto_off
        // # Auto-connect: OFF
        // # zh_CN 自动连接：关闭
        return "power.card.auto_off";
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.RING;
    }

    @Override
    public String[] getBaubleTypes(ItemStack stack) {
        return new String[] { BaubleExpandedSlots.universalType };
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    public void onEquipped(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    public boolean canEquip(ItemStack stack, EntityLivingBase wearer) {
        return wearer instanceof EntityPlayer;
    }

    @Override
    public boolean canUnequip(ItemStack stack, EntityLivingBase wearer) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advanced) {
        lines.add(StatCollector.translateToLocal(automatic(stack) ? autoOnKey() : autoOffKey()));
        // #tr power.card.source
        // # Power source: %s
        // # zh_CN 供电源：%s
        // #tr power.card.self
        // # Your wireless power network
        // # zh_CN 使用者自己的无线电网
        String name = stack.getTagCompound() == null ? ""
            : stack.getTagCompound()
                .getString("powerSourceName");
        lines.add(
            StatCollector.translateToLocalFormatted(
                "power.card.source",
                name.isEmpty() ? StatCollector.translateToLocal("power.card.self") : name));
        // #tr power.card.use
        // # Right-click a GT receiver to connect/disconnect; right-click air to manage networks and devices.
        // # zh_CN 右键 GT 用电设备连接／断开；右键空气选择电网、管理设备。
        lines.add(StatCollector.translateToLocal("power.card.use"));
        // #tr power.card.auto
        // # Carry in your inventory or Baubles to connect newly placed GT receivers. Sneak-right-click air to toggle.
        // # zh_CN 放在背包或饰品栏，自动接入新放置的 GT 用电设备；潜行右键空气切换自动连接。
        lines.add(StatCollector.translateToLocal("power.card.auto"));
        // #tr power.card.multiple
        // # Multiple enabled cards with different sources suspend automatic connection.
        // # zh_CN 同时携带多张已开启自动连接、但选择不同电网的卡时，自动连接暂停。
        lines.add(StatCollector.translateToLocal("power.card.multiple"));
        // #tr power.card.limits
        // # Uses real EU; respects voltage and amperage limits. Does not load chunks. All listed networks are public.
        // # zh_CN 消耗实际 EU，保留电压与安培限制，不加载区块；列表中的电网均可自由取电。
        lines.add(StatCollector.translateToLocal("power.card.limits"));
    }
}
