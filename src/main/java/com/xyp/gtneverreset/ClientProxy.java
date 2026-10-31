package com.xyp.gtneverreset;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.glodblock.github.client.gui.GuiMagnetFilter;
import com.glodblock.github.inventory.item.WirelessMagnetCardFilterInventory;
import com.xyp.gtneverreset.client.gui.GuiQuickEncodingTerminal;
import com.xyp.gtneverreset.client.render.VaultConnectedTextureRender;
import com.xyp.gtneverreset.common.gui.WirelessTerminalGuiHandler;
import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.loader.LifecycleLoader;
import com.xyp.gtneverreset.utils.enums.WirelessTerminalGuiType;
import com.xyp.gtneverreset.utils.inventory.DualTerminalGuiObject;

import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.items.contents.WirelessCraftingTerminalGuiObject;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import gregtech.api.interfaces.ITexture;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        LifecycleLoader.clientPreInit(event.getModConfigurationDirectory());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        LifecycleLoader.clientInit();
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
        return host instanceof DualTerminalGuiObject dual ? new GuiQuickEncodingTerminal(player.inventory, dual)
            : new GuiCraftingTerm(player.inventory, (WirelessCraftingTerminalGuiObject) host);
    }

    @Override
    public boolean preferOwnInterfaceNames() {
        return MainConfig.client.wirelessTerminal.preferOwnInterfaceNames;
    }

}
