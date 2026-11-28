package com.xyp.gtneverreset;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.glodblock.github.client.gui.GuiMagnetFilter;
import com.glodblock.github.inventory.item.WirelessMagnetCardFilterInventory;
import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;
import com.xyp.gtneverreset.client.render.VaultConnectedTextureRender;
import com.xyp.gtneverreset.client.terminal.TerminalSearchCacheAccess;
import com.xyp.gtneverreset.client.terminal.WirelessTerminalSorting;
import com.xyp.gtneverreset.client.wand.UniversalWandTravelFallback;
import com.xyp.gtneverreset.common.gui.WirelessTerminalGuiHandler;
import com.xyp.gtneverreset.common.terminal.DualTerminalGuiObject;
import com.xyp.gtneverreset.config.ClientConfig;
import com.xyp.gtneverreset.loader.ContentLoaders;
import com.xyp.gtneverreset.mixins.late.appliedenergistics2.AccessorGuiMEMonitorable;
import com.xyp.gtneverreset.utils.enums.WirelessTerminalGuiType;

import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.me.ItemRepo;
import appeng.container.AEBaseContainer;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.items.contents.WirelessCraftingTerminalGuiObject;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import gregtech.api.interfaces.ITexture;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ContentLoaders.clientPreInit(event.getModConfigurationDirectory());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ContentLoaders.clientInit();
    }

    @Override
    public ITexture createVaultCasingTexture(Block block) {
        return new VaultConnectedTextureRender(block);
    }

    @Override
    public Object createTerminalGui(int id, EntityPlayer player, World world, int slot) {
        WirelessTerminalGuiType type = WirelessTerminalGuiType.getByOrdinal(id);
        if (type == null) return null;
        WirelessTerminalGuiObject host = WirelessTerminalGuiHandler.buildHost(player, world, slot, type);
        if (host == null) return null;
        if (type == WirelessTerminalGuiType.WirelessMagnetFilter) {
            return new GuiMagnetFilter(
                player.inventory,
                new WirelessMagnetCardFilterInventory(host.getItemStack(), host.getInventorySlot(), null, player));
        }
        WirelessTerminalSorting.initialize();
        GuiMEMonitorable screen = host instanceof DualTerminalGuiObject dual
            ? new QuickEncodingTerminalGui(player.inventory, dual)
            : new GuiCraftingTerm(player.inventory, (WirelessCraftingTerminalGuiObject) host);
        if (Minecraft.getMinecraft().currentScreen instanceof GuiMEMonitorable previous
            && (previous instanceof QuickEncodingTerminalGui && screen instanceof GuiCraftingTerm
                || previous instanceof GuiCraftingTerm && screen instanceof QuickEncodingTerminalGui)
            && previous.inventorySlots instanceof AEBaseContainer container
            && container.getTarget() instanceof WirelessTerminalGuiObject previousHost
            && previousHost.getInventorySlot() == host.getInventorySlot()
            && previousHost.getItemStack()
                .getItem()
                == host.getItemStack()
                    .getItem()) {
            ItemRepo previousRepo = ((AccessorGuiMEMonitorable) previous).gtnr$getItemRepo();
            ItemRepo nextRepo = ((AccessorGuiMEMonitorable) screen).gtnr$getItemRepo();
            ((TerminalSearchCacheAccess) nextRepo).gtnr$inheritSearchCache(previousRepo);
        }
        return screen;
    }

    @Override
    public boolean preferOwnInterfaceNames() {
        return ClientConfig.wirelessTerminal.preferOwnInterfaceNames;
    }

    @Override
    public void useWandTravelIfImmediate(ItemStack stack, EntityPlayer player) {
        UniversalWandTravelFallback.useIfImmediate(stack, player);
    }
}
