package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ScreenShotHelper;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.xyp.gtneverreset.compat.nei.WildcardRecipeImport;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(
    modid = "wildcardimportclientonlyqa",
    name = "Wildcard Recipe Import Client-only QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class WildcardImportClientOnlyChecks {

    private int ticks;
    private boolean finished;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.wildcard.import.client.only.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (mc.currentScreen == null || ++ticks < 40) return;
            Method target = WildcardRecipeImport.class.getDeclaredMethod("target", GuiContainer.class);
            target.setAccessible(true);
            ModularContainer container = new ModularContainer();
            require(container.isClientOnly(), "a fresh ModularContainer must be client only");
            GuiContainerWrapper gui = new GuiContainerWrapper(
                container,
                new ModularScreen("gtneverreset", new ModularPanel("wildcard_import_client_only_qa")));
            Object result;
            try {
                result = target.invoke(null, gui);
            } catch (InvocationTargetException failure) {
                throw new AssertionError(
                    "client-only ModularUI screen must not crash NEI recipe buttons",
                    failure.getCause());
            }
            require(result == null, "client-only ModularUI screen must not expose the wildcard import handler");
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "wildcard-import-client-only.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            finished = true;
            Files.writeString(new File("result.txt").toPath(), "PASS");
            finishClient(mc, 0);
        } catch (Throwable failure) {
            finished = true;
            failure.printStackTrace();
            try {
                Files.writeString(new File("result.txt").toPath(), "FAIL " + failure);
            } catch (Exception ignored) {}
            finishClient(mc, 1);
        }
    }

    private static void finishClient(Minecraft mc, int status) {
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {}
            FMLCommonHandler.instance()
                .exitJava(status, true);
        }, "wildcard-import-client-only-qa-exit");
        exit.setDaemon(true);
        exit.start();
        mc.shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
