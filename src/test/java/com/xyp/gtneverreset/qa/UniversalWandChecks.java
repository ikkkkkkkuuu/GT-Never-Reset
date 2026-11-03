package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.cleanroommc.modularui.api.IMuiScreen;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.xyp.gtneverreset.common.item.items.ItemUniversalWand;
import com.xyp.gtneverreset.common.packet.UniversalWandNetwork;
import com.xyp.gtneverreset.config.ScreenThemeConfig;
import com.xyp.gtnrlib.gui.theme.ScreenTheme;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(modid = "wandqa", name = "Useless Tools QA", version = "1", dependencies = "after:gtneverreset")
public final class UniversalWandChecks {

    private final ScreenTheme[] themes = ScreenTheme.values();
    private boolean launched, waiting, finished;
    private int ticks, theme, frames;
    private volatile int stage;
    private boolean oldProtection;
    private volatile boolean syncPassed;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (!launched && mc.theWorld == null && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "wand-" + System.currentTimeMillis(),
                    "Wand QA",
                    new WorldSettings(16, WorldSettings.GameType.SURVIVAL, false, false, WorldType.FLAT));
            }
            if (mc.thePlayer == null) return;
            if (++ticks > 1800) throw new AssertionError("Timed out at theme " + themes[theme] + " stage " + stage);
            if (waiting || mc.currentScreen != null) return;
            if (mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem()
                .getItem() instanceof ItemUniversalWand)) {
                for (int slot = 0; slot < 9; slot++) {
                    var stack = mc.thePlayer.inventory.getStackInSlot(slot);
                    if (stack != null && stack.getItem() instanceof ItemUniversalWand) {
                        mc.thePlayer.inventory.currentItem = slot;
                        mc.playerController.updateController();
                        break;
                    }
                }
                return;
            }
            mc.ingameGUI.getChatGUI()
                .clearChatMessages();
            mc.gameSettings.guiScale = themes[theme] == ScreenTheme.AE2_DARK ? 3 : 2;
            ScreenThemeConfig.universalWand = themes[theme];
            UniversalWandNetwork.requestOpenGui(mc.thePlayer.inventory.currentItem);
            waiting = true;
            frames = 0;
            stage = 0;
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (finished || !waiting || event.phase != TickEvent.Phase.END || !(mc.currentScreen instanceof IMuiScreen mui))
            return;
        try {
            if (++frames < 18) return;
            frames = 0;
            var screen = mui.getScreen();
            var panel = screen.getMainPanel();
            var panels = screen.getPanelManager()
                .getOpenPanels();
            if (stage == 1 && !syncPassed || stage >= 3 && panels.size() < stage - 1) return;
            var pages = (PagedWidget<?>) panel.getChildren()
                .stream()
                .filter(w -> w instanceof PagedWidget)
                .findFirst()
                .orElseThrow();
            if (stage < 3 && pages.getCurrentPageIndex() != stage)
                throw new AssertionError("Wrong page at stage " + stage);
            ScreenShotHelper.saveScreenshot(
                new File("."),
                themes[theme].name() + "-" + stage + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            if (stage == 0) {
                oldProtection = ItemUniversalWand.isProtectionMode(mc.thePlayer.getHeldItem());
                syncPassed = false;
                ((ToggleButton) find(panel, "protection")).onMousePressed(0);
            }
            if (stage < 2) {
                var nextTab = panel.getChildren()
                    .stream()
                    .filter(w -> w instanceof PageButton)
                    .skip(stage + 1)
                    .findFirst()
                    .orElseThrow();
                ((PageButton) nextTab).onMousePressed(0);
            } else if (stage == 2) {
                ((ButtonWidget<?>) find(panel, "acceleration")).onMousePressed(0);
            } else if (stage == 3) {
                var history = panels.stream()
                    .map(p -> find(p, "history"))
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElseThrow();
                ((ButtonWidget<?>) history).onMousePressed(0);
            } else {
                mc.thePlayer.closeScreen();
                waiting = false;
                if (++theme == themes.length) finish(null);
            }
            stage++;
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (stage != 1 || event.phase != TickEvent.Phase.END) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        var player = (EntityPlayerMP) players.get(0);
        var held = player.getHeldItem();
        if (held != null && held.getItem() instanceof ItemUniversalWand
            && ItemUniversalWand.isProtectionMode(held) != oldProtection) syncPassed = true;
    }

    private static IWidget find(IWidget widget, String name) {
        if (name.equals(widget.getName())) return widget;
        for (IWidget child : widget.getChildren()) {
            IWidget match = find(child, name);
            if (match != null) return match;
        }
        return null;
    }

    private void finish(Throwable failure) {
        finished = true;
        if (failure != null) failure.printStackTrace();
        try {
            Files.writeString(Path.of("result.txt"), failure == null ? "PASS" : "FAIL " + failure);
        } catch (Exception error) {
            error.printStackTrace();
        }
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(failure == null ? 0 : 1, true);
        }, "wand-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }
}
