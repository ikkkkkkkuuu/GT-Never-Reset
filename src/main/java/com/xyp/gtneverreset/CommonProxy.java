package com.xyp.gtneverreset;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.xyp.gtneverreset.loader.ContentLoaders;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import gregtech.api.interfaces.ITexture;
import gregtech.api.render.TextureFactory;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        ContentLoaders.preInit(event);
    }

    public void init(FMLInitializationEvent event) {
        ContentLoaders.init(event);
    }

    public void postInit(FMLPostInitializationEvent event) {
        ContentLoaders.postInit(event);
    }

    public void loadComplete(FMLLoadCompleteEvent event) {
        ContentLoaders.loadComplete(event);
    }

    public void serverStarting(FMLServerStartingEvent event) {
        ContentLoaders.serverStarting(event);
    }

    public void serverStopping(FMLServerStoppingEvent event) {
        ContentLoaders.serverStopping(event);
    }

    public void serverStopped(FMLServerStoppedEvent event) {
        ContentLoaders.serverStopped(event);
    }

    public ITexture createVaultCasingTexture(Block block) {
        return TextureFactory.of(block, 0);
    }

    public Object createTerminalGui(int id, EntityPlayer player, World world, int slot) {
        return null;
    }

    public boolean preferOwnInterfaceNames() {
        return false;
    }
}
