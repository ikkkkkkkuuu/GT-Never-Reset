package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import com.xyp.gtneverreset.common.wand.UniversalWandMiningHandler;
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
    private volatile boolean miningChecked;

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
            if (!miningChecked) return;
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
        if (event.phase != TickEvent.Phase.END || finished || miningChecked && stage != 1) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        var player = (EntityPlayerMP) players.get(0);
        var held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemUniversalWand)) return;
        if (!miningChecked) {
            try {
                checkInstantMining(player, held);
                miningChecked = true;
            } catch (Throwable failure) {
                Minecraft.getMinecraft()
                    .func_152344_a(() -> finish(failure));
            }
        } else if (ItemUniversalWand.isProtectionMode(held) != oldProtection) syncPassed = true;
    }

    private static void checkInstantMining(EntityPlayerMP player, ItemStack original) {
        int slot = player.inventory.currentItem;
        ItemStack wand = original.copy();
        player.inventory.setInventorySlotContents(slot, wand);
        var world = player.worldObj;
        int x = (player.chunkCoordX << 4) + 2, z = (player.chunkCoordZ << 4) + 2;
        int y = 64;
        try {
            ItemUniversalWand.setProtectionMode(player, wand, false);
            ItemUniversalWand.setInventoryCollection(player, wand, true);
            ItemUniversalWand.setMaxMiningCount(player, wand, 23);
            ItemUniversalWand.setEnhancedChain(player, wand, false);
            for (int i = 0; i < 25; i++) world.setBlock(x + i % 5, y, z + i / 5, Blocks.gold_block);
            ItemUniversalWand.setGtToolMode(player, wand, ItemUniversalWand.GtToolMode.OFF);
            var legacy = wand.getTagCompound()
                .getCompoundTag("gtneverresetUniversalWand");
            legacy.setBoolean("buildingEnabled", true);
            legacy.setBoolean("buildingAeEnabled", true);
            for (int core = 0; core < 3; core++) {
                legacy.setInteger("buildingCore", core);
                if (wand.getItem()
                    .onItemUseFirst(wand, player, world, x, y, z, 1, 0.5F, 1.0F, 0.5F))
                    throw new AssertionError("Legacy core intercepted block use: " + core);
            }
            int before = goldBlocks(player);
            UniversalWandMiningHandler.setKeyHeld(player, true);
            if (!player.theItemInWorldManager.tryHarvestBlock(x, y, z))
                throw new AssertionError("Wand failed to mine origin");
            int remaining = 0;
            for (int i = 0; i < 25; i++) if (world.getBlock(x + i % 5, y, z + i / 5) == Blocks.gold_block) remaining++;
            if (remaining != 2 || goldBlocks(player) - before != 23) throw new AssertionError(
                "Instant 23-block limit: remaining=" + remaining + ", collected=" + (goldBlocks(player) - before));
            ItemUniversalWand.setEnhancedChain(player, wand, true);
            ItemUniversalWand.setEnhancedChainRadius(player, wand, 2);
            y += 4;
            world.setBlock(x, y, z, Blocks.gold_block);
            world.setBlock(x + 2, y, z, Blocks.gold_block);
            world.setBlock(x + 3, y, z, Blocks.gold_block);
            UniversalWandMiningHandler.setKeyHeld(player, true);
            if (!player.theItemInWorldManager.tryHarvestBlock(x, y, z) || !world.isAirBlock(x + 2, y, z)
                || world.getBlock(x + 3, y, z) != Blocks.gold_block
                || goldBlocks(player) - before != 25)
                throw new AssertionError("Instant enhanced mining radius or drops");
        } finally {
            UniversalWandMiningHandler.setKeyHeld(player, false);
            player.inventory.setInventorySlotContents(slot, original);
            player.sendContainerToPlayer(player.inventoryContainer);
        }
    }

    private static int goldBlocks(EntityPlayerMP player) {
        int count = 0;
        for (ItemStack stack : player.inventory.mainInventory)
            if (stack != null && stack.getItem() == Item.getItemFromBlock(Blocks.gold_block)) count += stack.stackSize;
        return count;
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
