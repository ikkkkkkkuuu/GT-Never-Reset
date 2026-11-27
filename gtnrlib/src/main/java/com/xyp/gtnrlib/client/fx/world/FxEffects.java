package com.xyp.gtnrlib.client.fx.world;

import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FxEffects {

    private boolean deploymentWarmupPending = true;

    public static void register() {
        FxEffects effects = new FxEffects();
        MinecraftForge.EVENT_BUS.register(effects);
        FMLCommonHandler.instance()
            .bus()
            .register(effects);
        MinecraftForge.EVENT_BUS.register(new FxArcRenderer());
        MinecraftForge.EVENT_BUS.register(new FxShockwaveRenderer());
        MinecraftForge.EVENT_BUS.register(new FxMoteRenderer());
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (!event.world.isRemote) return;
        FxArcRenderer.clear();
        FxShockwaveRenderer.clear();
        FxMoteRenderer.clear();
    }

    @SubscribeEvent
    public void reload(TextureStitchEvent.Post event) {
        FxRenderHelper.clearSprites();
        if (event.map.getTextureType() == 0) deploymentWarmupPending = true;
    }

    @SubscribeEvent
    public void warmDeployment(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !deploymentWarmupPending) return;
        deploymentWarmupPending = false;
        FxDeploymentShaderWarmup.prepare();
    }
}
