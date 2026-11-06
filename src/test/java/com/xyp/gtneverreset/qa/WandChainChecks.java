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

import com.xyp.gtneverreset.common.item.items.ItemUniversalWand;
import com.xyp.gtneverreset.common.packet.UniversalWandNetwork;
import com.xyp.gtneverreset.common.wand.UniversalWandMiningHandler;
import com.xyp.gtneverreset.loader.ItemLoader;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(modid = "wandqa", name = "Wand Chain Regression QA", version = "1", dependencies = "after:gtneverreset")
public final class WandChainChecks {

    private static final int LIMIT = 16;
    private static final int ORES = 3;
    private static final int CASES = 14;
    private boolean launched, finished;
    private volatile int stage, pass, bx, bz;
    private int ticks, settle, delay;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void server(TickEvent.ServerTickEvent event) {
        if (finished || stage < 0 || stage == 4) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        var player = (EntityPlayerMP) players.get(0);
        try {
            if (event.phase == TickEvent.Phase.START && stage == 0 && player.ticksExisted > 40) {
                prepare(player);
                if (pass < 4 || pass >= 12) {
                    boolean chain = pass != 12;
                    UniversalWandMiningHandler.setKeyHeld(player, chain);
                    if (!player.theItemInWorldManager.tryHarvestBlock(bx, 72, bz + 3))
                        throw new AssertionError("Origin harvest failed in case " + pass);
                    stage = 3;
                } else stage = 1;
            } else if (event.phase == TickEvent.Phase.END && stage == 3 && ++delay >= 10) {
                verify(player);
                UniversalWandMiningHandler.setKeyHeld(player, false);
                System.out.println("WAND CHAIN PASS " + pass);
                delay = 0;
                stage = ++pass == CASES ? 4 : 0;
            }
        } catch (Throwable failure) {
            stage = -1;
            Minecraft.getMinecraft()
                .func_152344_a(() -> finish(failure));
        }
    }

    private void prepare(EntityPlayerMP player) {
        var world = player.worldObj;
        if (pass == 0) {
            bx = (int) Math.floor(player.posX);
            bz = (int) Math.floor(player.posZ);
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = -1; z <= 7; z++) {
                world.setBlock(bx + x, 70, bz + z, Blocks.bedrock);
                for (int y = 71; y <= 75; y++) {
                    world.setBlock(bx + x, y, bz + z, z >= 3 ? Blocks.stone : Blocks.air);
                }
            }
        }
        if (pass != 13) {
            for (int x = 0; x < ORES; x++) world.setBlock(bx + x, 72, bz + 3, Blocks.gold_ore);
        }
        for (int slot = 0; slot < player.inventory.mainInventory.length; slot++)
            player.inventory.setInventorySlotContents(slot, null);
        ItemStack wand = new ItemStack(ItemLoader.universalWand);
        player.inventory.setInventorySlotContents(player.inventory.currentItem, wand);
        ItemUniversalWand.setProtectionMode(player, wand, false);
        ItemUniversalWand.setInventoryCollection(player, wand, true);
        ItemUniversalWand.setMaxMiningCount(player, wand, LIMIT);
        ItemUniversalWand.setEnhancedChain(player, wand, pass % 2 == 0 || pass == 13);
        ItemUniversalWand.setEnhancedChainRadius(player, wand, 4);
        ItemUniversalWand.setEnchantMode(
            player,
            wand,
            pass % 4 < 2 || pass >= 12 ? ItemUniversalWand.EnchantMode.SILK : ItemUniversalWand.EnchantMode.FORTUNE);
        ItemUniversalWand.setPlayerProtectionEnabled(player, wand, true);
        ItemUniversalWand.setNoAggroEnabled(player, wand, true);
        ItemUniversalWand.setFlightSpeedOverrideEnabled(player, wand, true);
        player.playerNetServerHandler.setPlayerLocation(bx + 0.5, 71, bz + 0.5, 0, 0);
        player.sendContainerToPlayer(player.inventoryContainer);
    }

    private void verify(EntityPlayerMP player) {
        int stone = count(player, Item.getItemFromBlock(Blocks.stone))
            + count(player, Item.getItemFromBlock(Blocks.cobblestone));
        int ore = count(player, Item.getItemFromBlock(Blocks.gold_ore));
        int expectedOre = pass == 12 ? 1 : pass == 13 ? 0 : ORES;
        int expectedStone = pass == 13 ? LIMIT : 0;
        int drops = 0;
        for (ItemStack stack : player.inventory.mainInventory)
            if (stack != null && !(stack.getItem() instanceof ItemUniversalWand)) drops += stack.stackSize;
        boolean silk = ItemUniversalWand.getEnchantMode(player.getHeldItem()) == ItemUniversalWand.EnchantMode.SILK;
        // GTNH replaces ordinary ore drops in fortune mode; silk still returns the original ore block.
        if (silk && ore != expectedOre || drops < expectedOre || stone != expectedStone) throw new AssertionError(
            "case " + pass
                + ": ore="
                + ore
                + ", stone="
                + stone
                + " (expected "
                + expectedOre
                + ", "
                + expectedStone
                + ")");
        if (pass == 13) return;
        for (int x = 0; x < ORES; x++) {
            var expected = pass == 12 && x > 0 ? Blocks.gold_ore : Blocks.stone;
            if (player.worldObj.getBlock(bx + x, 72, bz + 3) != expected)
                throw new AssertionError("Ore backfill changed at " + x + " in case " + pass);
        }
        for (int x = -4; x <= 4; x++) for (int z = 3; z <= 7; z++) for (int y = 71; y <= 75; y++) {
            if (pass == 12 && x > 0 && x < ORES && y == 72 && z == 3) continue;
            if (player.worldObj.getBlock(bx + x, y, bz + z) != Blocks.stone)
                throw new AssertionError("Surrounding stone mined in case " + pass);
        }
    }

    private static int count(EntityPlayerMP player, Item item) {
        int count = 0;
        for (ItemStack stack : player.inventory.mainInventory)
            if (stack != null && stack.getItem() == item) count += stack.stackSize;
        return count;
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var mc = Minecraft.getMinecraft();
        try {
            if (!launched && mc.theWorld == null && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "chain-" + System.currentTimeMillis(),
                    "Chain QA",
                    new WorldSettings(16, WorldSettings.GameType.SURVIVAL, false, false, WorldType.FLAT));
            }
            if (mc.thePlayer == null) return;
            if (++ticks > 1800) throw new AssertionError("Timeout stage " + stage + " case " + pass);
            if (mc.currentScreen != null) return;
            if (stage == 4) {
                ScreenShotHelper.saveScreenshot(
                    new File("."),
                    "chain-regression.png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                finish(null);
            } else if (stage == 1 && ++settle >= 12) {
                if (mc.theWorld.getBlock(bx, 72, bz + 3) != Blocks.gold_ore) return;
                settle = 0;
                mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = 0;
                mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = 0;
                // Cover both legacy clients (extra start packet) and the current key + vanilla digging path.
                UniversalWandNetwork.requestChainKey(true);
                if (pass < 8) UniversalWandNetwork.requestChainStart(bx, 72, bz + 3);
                mc.playerController.clickBlock(bx, 72, bz + 3, 2);
                mc.playerController.resetBlockRemoving();
                stage = 3;
            }
        } catch (Throwable failure) {
            finish(failure);
        }
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
        }, "wand-chain-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }
}
