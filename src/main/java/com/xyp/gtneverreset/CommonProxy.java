package com.xyp.gtneverreset;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.xyp.gtneverreset.loader.LifecycleLoader;

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
        LifecycleLoader.preInit(event);
    }

    public void init(FMLInitializationEvent event) {
        LifecycleLoader.init(event);
    }

    public void postInit(FMLPostInitializationEvent event) {
        LifecycleLoader.postInit(event);
    }

    public void loadComplete(FMLLoadCompleteEvent event) {
        LifecycleLoader.loadComplete(event);
    }

    public void serverStarting(FMLServerStartingEvent event) {
        LifecycleLoader.serverStarting(event);
    }

    public void serverStopping(FMLServerStoppingEvent event) {
        LifecycleLoader.serverStopping(event);
    }

    public void serverStopped(FMLServerStoppedEvent event) {
        LifecycleLoader.serverStopped(event);
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
