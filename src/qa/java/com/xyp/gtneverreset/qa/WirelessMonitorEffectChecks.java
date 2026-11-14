package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ScreenShotHelper;

import com.xyp.gtneverreset.client.wireless.WirelessMonitorHud;
import com.xyp.gtneverreset.client.wireless.WirelessMonitorSettingsGui;
import com.xyp.gtneverreset.config.WirelessMonitorConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(modid = "wirelessmonitorqa", name = "Wireless Monitor QA", version = "1", dependencies = "after:gtneverreset")
public final class WirelessMonitorEffectChecks {

    private WirelessMonitorSettingsGui screen;
    private int frames;
    private int stage;
    private boolean finished;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.wireless.monitor.effect.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (screen == null) {
                if (!(mc.currentScreen instanceof GuiMainMenu)) return;
                Method parse = WirelessMonitorHud.class.getDeclaredMethod("parseEffect", String.class);
                parse.setAccessible(true);
                for (String valid : new String[] { "&{ba;shake=1}&l", "§{nf;fault=40}§o", "&{er;#abc;2}" })
                    if (parse.invoke(null, valid) == null) throw new AssertionError("Rejected " + valid);
                for (String invalid : new String[] { "&{missing}", "&{ba;shake=9}", "&{ba}123 UHV", "&{ba;t=20}",
                    "&{ba;roll=1}" })
                    if (parse.invoke(null, invalid) != null) throw new AssertionError("Accepted " + invalid);
                Field historyField = WirelessMonitorHud.class.getDeclaredField("history");
                historyField.setAccessible(true);
                com.xyp.gtneverreset.common.wireless.WirelessEnergyHistory history = (com.xyp.gtneverreset.common.wireless.WirelessEnergyHistory) historyField
                    .get(WirelessMonitorHud.INSTANCE);
                Method lines = WirelessMonitorHud.class.getDeclaredMethod("lines");
                lines.setAccessible(true);
                java.util.UUID owner = java.util.UUID.randomUUID();
                history.add(owner, 1, java.math.BigInteger.valueOf(1234));
                String before = java.util.Arrays.toString((String[]) lines.invoke(WirelessMonitorHud.INSTANCE));
                history.add(owner, 21, java.math.BigInteger.valueOf(5678));
                String after = java.util.Arrays.toString((String[]) lines.invoke(WirelessMonitorHud.INSTANCE));
                if (before.equals(after) || !after.contains("EU")) throw new AssertionError("Live value or unit lost");
                screen = new WirelessMonitorSettingsGui();
                mc.displayGuiScreen(screen);
                Field page = WirelessMonitorSettingsGui.class.getDeclaredField("page");
                page.setAccessible(true);
                page.setInt(screen, 1);
                screen.initGui();
            }
            if (++frames < 40) return;
            frames = 0;
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "wireless-effect-" + stage + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            if (stage == 0) {
                edit("&{nf;fault=40}&l");
                if (!WirelessMonitorConfig.effect.equals("&{nf;fault=40}&l"))
                    throw new AssertionError("Valid input not applied");
            } else if (stage == 1) {
                edit("&{ba;shake=9}");
                if (!WirelessMonitorConfig.effect.equals("&{nf;fault=40}&l"))
                    throw new AssertionError("Invalid input saved");
            } else if (stage == 2) {
                edit("&{ba;shake=1}&l");
                screen.onGuiClosed();
                screen = new WirelessMonitorSettingsGui();
                mc.displayGuiScreen(screen);
                Field page = WirelessMonitorSettingsGui.class.getDeclaredField("page");
                page.setAccessible(true);
                page.setInt(screen, 1);
                mc.gameSettings.guiScale = 3;
                screen.setWorldAndResolution(mc, 427, 240);
            } else {
                finished = true;
                Files.writeString(new File("result.txt").toPath(), "PASS");
                finishClient(mc, 0);
            }
            stage++;
        } catch (Throwable failure) {
            finished = true;
            failure.printStackTrace();
            try {
                Files.writeString(new File("result.txt").toPath(), "FAIL " + failure);
            } catch (Exception ignored) {}
            finishClient(mc, 1);
        }
    }

    private void finishClient(Minecraft mc, int status) {
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {}
            FMLCommonHandler.instance()
                .exitJava(status, true);
        }, "wireless-monitor-qa-exit");
        exit.setDaemon(true);
        exit.start();
        mc.shutdown();
    }

    private void edit(String value) throws Exception {
        Field field = WirelessMonitorSettingsGui.class.getDeclaredField("effect");
        field.setAccessible(true);
        GuiTextField input = (GuiTextField) field.get(screen);
        input.setFocused(true);
        input.setText(value + "x");
        Method key = WirelessMonitorSettingsGui.class.getDeclaredMethod("keyTyped", char.class, int.class);
        key.setAccessible(true);
        key.invoke(screen, '\0', 14);
    }
}
