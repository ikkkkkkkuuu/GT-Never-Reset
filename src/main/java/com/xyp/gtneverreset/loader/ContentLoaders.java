package com.xyp.gtneverreset.loader;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.Tags;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ContentLoaders {

    private static final List<IContentLoader> LOADERS = List.of(
        new ConfigLoader(),
        new ItemLoader.Card(),
        new BlockLoader.Card(),
        new EntityLoader(),
        new NetworkLoader(),
        new WorldLoader(),
        new GuiLoader(),
        new MachineLoader(),
        new CommandLoader(),
        new WirelessLoader(),
        new MELoader(),
        new RecipeLoader(),
        new QuantumLoader(),
        new ClientLoader.Card());

    private static Phase phase = Phase.NOT_STARTED;

    public static void preInit(FMLPreInitializationEvent event) {
        advance(Phase.NOT_STARTED, Phase.PRE_INIT);
        run("preInit", loader -> loader.preInit(event));
        GTNeverReset.LOG.info("loading {} version {}", GTNeverReset.NAME, Tags.VERSION);
    }

    public static void init(FMLInitializationEvent event) {
        advance(Phase.PRE_INIT, Phase.INIT);
        run("init", IContentLoader::init);
    }

    public static void postInit(FMLPostInitializationEvent event) {
        advance(Phase.INIT, Phase.POST_INIT);
        run("postInit", IContentLoader::postInit);
    }

    public static void loadComplete(FMLLoadCompleteEvent event) {
        advance(Phase.POST_INIT, Phase.LOAD_COMPLETE);
        run("loadComplete", IContentLoader::loadComplete);
    }

    public static void serverStarting(FMLServerStartingEvent event) {
        require(Phase.LOAD_COMPLETE);
        run("serverStarting", loader -> loader.serverStarting(event));
    }

    public static void serverStopping(FMLServerStoppingEvent event) {
        run("serverStopping", IContentLoader::serverStopping);
    }

    public static void serverStopped(FMLServerStoppedEvent event) {
        run("serverStopped", IContentLoader::serverStopped);
    }

    @SideOnly(Side.CLIENT)
    public static void clientPreInit(File configDirectory) {
        require(Phase.PRE_INIT);
        run("clientPreInit", loader -> loader.clientPreInit(configDirectory));
    }

    @SideOnly(Side.CLIENT)
    public static void clientInit() {
        require(Phase.INIT);
        run("clientInit", IContentLoader::clientInit);
    }

    private static void advance(Phase expected, Phase next) {
        require(expected);
        phase = next;
    }

    private static void require(Phase expected) {
        if (phase != expected) throw new IllegalStateException("Expected loading phase " + expected + ", got " + phase);
    }

    private static void run(String phase, Consumer<IContentLoader> action) {
        for (IContentLoader loader : LOADERS) {
            try {
                action.accept(loader);
            } catch (RuntimeException failure) {
                throw new IllegalStateException("content loader " + loader.name() + " failed in " + phase, failure);
            }
        }
    }

    private enum Phase {
        NOT_STARTED,
        PRE_INIT,
        INIT,
        POST_INIT,
        LOAD_COMPLETE
    }

}
