package com.xyp.gtneverreset;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.xyp.gtneverreset.client.event.WirelessCardInput;
import com.xyp.gtneverreset.client.gui.GuiQuickEncodingTerminal;
import com.xyp.gtneverreset.client.quantum.ICraftingCycleHost;
import com.xyp.gtneverreset.client.quantum.RecursivePatternReport;
import com.xyp.gtneverreset.client.render.VaultConnectedTexture;
import com.xyp.gtneverreset.client.render.WirelessWorldHighlight;
import com.xyp.gtneverreset.common.gui.WirelessTerminalGuiHandler;
import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.loader.ClientLoader;
import com.xyp.gtneverreset.utils.crafting.cycle.RecursivePatternFinder.Cycle;
import com.xyp.gtneverreset.utils.crafting.quantum.ae2.IQuantumCraftAmountGui;
import com.xyp.gtneverreset.utils.crafting.quantum.ae2.IQuantumCraftConfirm;
import com.xyp.gtneverreset.utils.enums.WirelessTerminalGuiType;
import com.xyp.gtneverreset.utils.inventory.DualTerminalGuiObject;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessCardBinding;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.items.contents.WirelessCraftingTerminalGuiObject;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import gregtech.api.interfaces.ITexture;

public class ClientProxy extends CommonProxy {

    @Override
    public ITexture createVaultCasingTexture(Block block) {
        return new VaultConnectedTexture(block);
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ClientLoader.preInit(event.getModConfigurationDirectory());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ClientLoader.init();
    }

    @Override
    public Object createTerminalGui(int id, EntityPlayer player, World world, int slot) {
        WirelessTerminalGuiType type = WirelessTerminalGuiType.getByOrdinal(id);
        if (type == null) return null;
        WirelessTerminalGuiObject host = WirelessTerminalGuiHandler.buildHost(player, world, slot, type);
        if (host == null) return null;
        return host instanceof DualTerminalGuiObject dual ? new GuiQuickEncodingTerminal(player.inventory, dual)
            : new GuiCraftingTerm(player.inventory, (WirelessCraftingTerminalGuiObject) host);
    }

    @Override
    public boolean preferOwnInterfaceNames() {
        return MainConfig.client.wirelessTerminal.preferOwnInterfaceNames;
    }

    @Override
    public void showWirelessHighlight(int dimension, int x, int y, int z) {
        Minecraft minecraft = Minecraft.getMinecraft();
        var connection = minecraft.getNetHandler();
        minecraft.func_152344_a(
            () -> {
                if (minecraft.getNetHandler() == connection)
                    WirelessWorldHighlight.afterTeleport(minecraft, dimension, x, y, z);
            });
    }

    @Override
    public void receiveWirelessVisualisation(NBTTagCompound bindingTag, int dimension, byte[] payload) {
        Minecraft minecraft = Minecraft.getMinecraft();
        var connection = minecraft.getNetHandler();
        minecraft.func_152344_a(() -> {
            if (minecraft.getNetHandler() == connection) WirelessCardInput
                .receiveVisualisation(minecraft, WirelessCardBinding.read(bindingTag), dimension, payload);
        });
    }

    @Override
    public void receiveQuantumAvailability(boolean available) {
        Minecraft minecraft = Minecraft.getMinecraft();
        var connection = minecraft.getNetHandler();
        minecraft.func_152344_a(() -> {
            if (minecraft.getNetHandler() == connection
                && minecraft.currentScreen instanceof IQuantumCraftAmountGui gui) {
                gui.gtnr$setQuantumAvailable(available);
            }
        });
    }

    @Override
    public void receiveQuantumConfirm(boolean quantumJob) {
        Minecraft minecraft = Minecraft.getMinecraft();
        var connection = minecraft.getNetHandler();
        minecraft.func_152344_a(() -> {
            if (minecraft.getNetHandler() == connection && minecraft.thePlayer != null
                && minecraft.thePlayer.openContainer instanceof IQuantumCraftConfirm container) {
                container.gtnr$setQuantumJob(quantumJob);
            }
        });
    }

    @Override
    public void receiveRecursivePatternResult(IAEStack<?> request, List<Cycle> cycles, boolean manual) {
        Minecraft minecraft = Minecraft.getMinecraft();
        var connection = minecraft.getNetHandler();
        minecraft.func_152344_a(() -> {
            if (minecraft.getNetHandler() != connection) return;
            if (minecraft.currentScreen instanceof ICraftingCycleHost host) {
                host.gtnr$cyclesFound(request, cycles, manual);
            } else if (manual) {
                RecursivePatternReport.show(minecraft, request, cycles);
            }
        });
    }

}
