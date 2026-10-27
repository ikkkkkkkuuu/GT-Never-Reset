package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.xyp.gtneverreset.client.gui.WirelessMonitorHud;
import com.xyp.gtneverreset.client.gui.WirelessMonitorPositionGui;
import com.xyp.gtneverreset.client.gui.WirelessMonitorSettingsGui;
import com.xyp.gtneverreset.client.text.EffectTextRenderer;
import com.xyp.gtneverreset.common.packet.WirelessMonitorSnapshotPacket;
import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.utils.text.AnimatedText;
import com.xyp.gtneverreset.utils.world.WirelessEnergyHistory;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.common.misc.WirelessNetworkManager;
import io.netty.buffer.Unpooled;

@Mod(modid = "wirelessmonitorqa", name = "Wireless Monitor QA", version = "1", dependencies = "after:gtneverreset")
public final class WirelessMonitorClientChecks {

    private boolean started, finished, initializedServer;
    private volatile boolean discharging;
    private int stage, ticks, totalTicks;
    private long disabledRequest;
    private String capture;
    private String credits;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        if (!initializedServer) {
            initializedServer = true;
            WirelessNetworkManager.strongCheckOrAddUser(player.getUniqueID());
            WirelessNetworkManager.setUserEU(player.getUniqueID(), BigInteger.valueOf(29_500_080));
            for (int i = 0; i < player.inventory.getSizeInventory(); i++)
                player.inventory.setInventorySlotContents(i, null);
            player.setPositionAndUpdate(0, 6, 0);
        } else WirelessNetworkManager.addEUToGlobalEnergyMap(player.getUniqueID(), discharging ? -4838 : 3200);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.limitFramerate = 60;
        try {
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                checkCore();
                mc.launchIntegratedServer(
                    "wireless-monitor-" + System.currentTimeMillis(),
                    "Wireless HUD QA",
                    new WorldSettings(193L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (mc.theWorld == null || mc.thePlayer == null) return;
            if (++totalTicks > 1600) throw new AssertionError("timeout stage " + stage);
            if (capture != null) return;
            ticks++;
            var c = MainConfig.wirelessMonitor;
            if (stage == 0) {
                credits = AnimatedText.GT_NEVER_RESET.get();
                mc.displayGuiScreen(null);
                WirelessMonitorHud.INSTANCE.setEnabled(false);
                toggle(WirelessMonitorHud.TOGGLE);
                require(c.enabled, "toggle enables HUD");
                c.style = 0;
                c.scale = 1;
                c.width = 240;
                c.xOffset = c.yOffset = 0;
                c.showEnergy = c.showRealtime = c.showAverage = c.showTier = true;
                c.bold = c.italic = c.customPalette = c.scientific = false;
                c.animatedColors = c.animateValues = true;
                c.refreshTicks = 20;
                next();
            } else if (stage == 1 && ticks > 80) {
                require(
                    history().energy() != null && history().realtime()
                        .signum() > 0,
                    "server charging snapshots");
                require(mc.thePlayer.getHeldItem() == null, "no item required");
                BigInteger before = history().energy();
                WirelessMonitorHud.INSTANCE.receive(
                    new WirelessMonitorSnapshotPacket(Long.MIN_VALUE, 0, new UUID(90, 91), BigInteger.ZERO),
                    mc.getNetHandler());
                require(before.equals(history().energy()), "stale response rejected");
                capture = "text-rainbow.png";
                next();
            } else if (stage == 2 && ticks > 25) {
                capture = "text-rainbow-later.png";
                next();
            } else if (stage == 3) {
                toggle(WirelessMonitorHud.TOGGLE);
                require(!c.enabled && history().energy() == null, "disable clears history");
                disabledRequest = number("requestId");
                next();
            } else if (stage == 4 && ticks > 120) {
                require(number("requestId") == disabledRequest, "disabled HUD stops polling");
                discharging = true;
                toggle(WirelessMonitorHud.TOGGLE);
                c.style = 1;
                c.animateValues = false;
                next();
            } else if (stage == 5 && ticks > 80) {
                require(
                    history().realtime()
                        .signum() < 0,
                    "discharging snapshots");
                capture = "card-discharging.png";
                c.bold = true;
                next();
            } else if (stage == 6 && ticks > 25) {
                c.style = 2;
                c.customPalette = true;
                c.animateValues = true;
                capture = "curve-custom-rainbow.png";
                require(
                    AnimatedText.GT_NEVER_RESET.get()
                        .equals(credits),
                    "machine rainbow remains unchanged");
                next();
            } else if (stage == 7 && ticks > 25) {
                c.style = 0;
                c.xOffset = c.yOffset = 0;
                toggle(WirelessMonitorHud.MOVE);
                require(mc.currentScreen instanceof WirelessMonitorPositionGui, "position key");
                GuiScreen editor = mc.currentScreen;
                int x = 12, y = editor.height - 70;
                input(editor, "mouseClicked", new Class<?>[] { int.class, int.class, int.class }, x, y, 0);
                input(
                    editor,
                    "mouseClickMove",
                    new Class<?>[] { int.class, int.class, int.class, long.class },
                    x + 60,
                    y - 75,
                    0,
                    1L);
                input(editor, "mouseMovedOrUp", new Class<?>[] { int.class, int.class, int.class }, x + 60, y - 75, 0);
                require(c.xOffset == 60 && c.yOffset == 75, "drag offset");
                captureScreen("position.png");
                input(editor, "keyTyped", new Class<?>[] { char.class, int.class }, '\0', Keyboard.KEY_ESCAPE);
                require(mc.currentScreen == null, "close editor");
                String saved = Files
                    .readString(new File(mc.mcDataDir, "config/GTNeverReset/GTNeverReset.cfg").toPath());
                require(saved.contains("xOffset=60") && saved.contains("yOffset=75"), "position persisted");
                mc.displayGuiScreen(new WirelessMonitorSettingsGui());
                next();
            } else if (stage == 8 && ticks > 30) {
                captureScreen("settings-display.png");
                input(
                    mc.currentScreen,
                    "actionPerformed",
                    new Class<?>[] { GuiButton.class },
                    new GuiButton(20, 0, 0, ""));
                next();
            } else if (stage == 9 && ticks > 30) {
                captureScreen("settings-colors.png");
                mc.displayGuiScreen(null);
                c.style = 2;
                c.width = 120;
                c.scale = 1.5F;
                c.xOffset = c.yOffset = 0;
                c.showAverage = c.showTier = false;
                capture = "curve-narrow-scaled.png";
                next();
            } else if (stage == 10 && ticks > 30) {
                var field = EffectTextRenderer.class.getDeclaredField("failed");
                field.setAccessible(true);
                require(((Set<?>) field.get(EffectTextRenderer.INSTANCE)).isEmpty(), "rainbow shader has no failures");
                require(GL11.glGetError() == GL11.GL_NO_ERROR, "no GL errors");
                finish(null);
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void next() {
        stage++;
        ticks = 0;
    }

    private static void checkCore() {
        UUID owner = new UUID(1, 2);
        var history = new WirelessEnergyHistory();
        BigInteger huge = BigInteger.TEN.pow(1000);
        history.add(owner, 0, huge);
        history.add(owner, 100, huge.add(BigInteger.valueOf(3200)));
        require(
            history.realtime()
                .compareTo(new BigDecimal("32")) == 0,
            "exact huge-balance delta");
        for (int tick = 120; tick <= 6200; tick += 20)
            history.add(owner, tick, huge.add(BigInteger.valueOf(tick * 32L)));
        require(
            history.samples()
                .size() <= 301
                && history.average()
                    .compareTo(new BigDecimal("32")) == 0,
            "five-minute window");
        history.add(new UUID(3, 4), 6300, BigInteger.ONE);
        require(history.realtime() == null, "team change clears rates");
        history.add(new UUID(3, 4), 7000, BigInteger.ONE);
        require(history.realtime() == null, "gap clears rates");
        history.add(new UUID(3, 4), 0, BigInteger.TEN);
        require(history.realtime() == null, "clock rollback clears rates");
        var buffer = Unpooled.buffer();
        try {
            new WirelessMonitorSnapshotPacket(7, 900, owner, huge).toBytes(buffer);
            var restored = new WirelessMonitorSnapshotPacket();
            restored.fromBytes(buffer);
            require(
                restored.energy.equals(huge) && restored.owner.equals(owner)
                    && restored.tick == 900
                    && restored.requestId == 7,
                "packet roundtrip");
            buffer.clear()
                .writeZero(32)
                .writeShort(4097)
                .writeZero(4097);
            boolean rejected = false;
            try {
                restored.fromBytes(buffer);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            require(rejected, "oversized packet rejected");
        } finally {
            buffer.release();
        }
    }

    private static void toggle(KeyBinding binding) {
        KeyBinding.onTick(binding.getKeyCode());
        FMLCommonHandler.instance()
            .bus()
            .post(new InputEvent.KeyInputEvent());
    }

    private static WirelessEnergyHistory history() throws Exception {
        var field = WirelessMonitorHud.class.getDeclaredField("history");
        field.setAccessible(true);
        return (WirelessEnergyHistory) field.get(WirelessMonitorHud.INSTANCE);
    }

    private static long number(String name) throws Exception {
        var field = WirelessMonitorHud.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getLong(WirelessMonitorHud.INSTANCE);
    }

    private static void input(GuiScreen screen, String name, Class<?>[] types, Object... values) throws Exception {
        Method method = screen.getClass()
            .getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(screen, values);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void render(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL && capture != null && !finished) {
            captureScreen(capture);
            capture = null;
        }
    }

    private static File output() {
        return new File(System.getProperty("gtnr.wirelessMonitor.qa.output"));
    }

    private static void captureScreen(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void finish(Throwable failure) {
        finished = true;
        if (failure != null) failure.printStackTrace();
        try {
            Files.writeString(
                new File(output(), "client-result.txt").toPath(),
                failure == null ? "PASS" : "FAIL: " + failure);
        } catch (Exception writeFailure) {
            writeFailure.printStackTrace();
        }
        FMLCommonHandler.instance()
            .exitJava(failure == null ? 0 : 1, true);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
