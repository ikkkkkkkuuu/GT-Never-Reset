package com.xyp.gtneverreset.loader;

import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.client.wand.UniversalWandAccelerationRenderer;
import com.xyp.gtneverreset.client.wand.UniversalWandChainHudRenderer;
import com.xyp.gtneverreset.client.wand.UniversalWandFlightSpeedHudRenderer;
import com.xyp.gtneverreset.client.wand.UniversalWandItemRenderer;
import com.xyp.gtneverreset.client.wand.UniversalWandKeyHandler;
import com.xyp.gtneverreset.client.wand.UniversalWandLavaVisionRenderer;
import com.xyp.gtneverreset.client.wand.UniversalWandStealthRenderer;
import com.xyp.gtneverreset.client.wand.UniversalWandTravelFallback;
import com.xyp.gtneverreset.common.entity.EntityUselessWandItem;
import com.xyp.gtneverreset.common.wand.UniversalWandAccelerationHandler;
import com.xyp.gtneverreset.common.wand.UniversalWandCombatHandler;
import com.xyp.gtneverreset.common.wand.UniversalWandExperienceHandler;
import com.xyp.gtneverreset.common.wand.UniversalWandFeaturePolicy;
import com.xyp.gtneverreset.common.wand.UniversalWandFlightMotionHandler;
import com.xyp.gtneverreset.common.wand.UniversalWandMiningHandler;
import com.xyp.gtneverreset.common.wand.UniversalWandProtectionHandler;
import com.xyp.gtneverreset.common.wand.UniversalWandRightClick;
import com.xyp.gtneverreset.common.wand.UniversalWandUtilityHandler;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class UniversalWandLoader implements IContentLoader {

    @Override
    public void init() {
        var events = FMLCommonHandler.instance()
            .bus();
        events.register(new UniversalWandFeaturePolicy());
        events.register(new UniversalWandRightClick());
        UniversalWandAccelerationHandler accelerationHandler = new UniversalWandAccelerationHandler();
        MinecraftForge.EVENT_BUS.register(accelerationHandler);
        events.register(accelerationHandler);
        UniversalWandMiningHandler miningHandler = new UniversalWandMiningHandler();
        MinecraftForge.EVENT_BUS.register(miningHandler);
        MinecraftForge.EVENT_BUS.register(new UniversalWandExperienceHandler());
        MinecraftForge.EVENT_BUS.register(new UniversalWandCombatHandler());
        UniversalWandProtectionHandler protectionHandler = new UniversalWandProtectionHandler();
        MinecraftForge.EVENT_BUS.register(protectionHandler);
        events.register(protectionHandler);
        events.register(miningHandler);
        events.register(new UniversalWandUtilityHandler());
        MinecraftForge.EVENT_BUS.register(new UniversalWandFlightMotionHandler());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void clientInit() {
        var events = FMLCommonHandler.instance()
            .bus();
        UniversalWandKeyHandler.init();
        RenderingRegistry.registerEntityRenderingHandler(EntityUselessWandItem.class, new RenderItem());
        MinecraftForgeClient.registerItemRenderer(ItemLoader.universalWand, new UniversalWandItemRenderer());
        var travel = new UniversalWandTravelFallback();
        var acceleration = new UniversalWandAccelerationRenderer();
        events.register(travel);
        events.register(acceleration);
        MinecraftForge.EVENT_BUS.register(travel);
        MinecraftForge.EVENT_BUS.register(acceleration);
        MinecraftForge.EVENT_BUS.register(new UniversalWandChainHudRenderer());
        MinecraftForge.EVENT_BUS.register(new UniversalWandFlightSpeedHudRenderer());
        MinecraftForge.EVENT_BUS.register(new UniversalWandStealthRenderer());
        MinecraftForge.EVENT_BUS.register(new UniversalWandLavaVisionRenderer());

    }

    @Override
    public void serverStopped() {
        UniversalWandRightClick.clearAll();
        UniversalWandMiningHandler.finishAllChains();
        UniversalWandAccelerationHandler.clearAll();
        UniversalWandUtilityHandler.clearAll();
        UniversalWandProtectionHandler.clearAll();
    }
}
