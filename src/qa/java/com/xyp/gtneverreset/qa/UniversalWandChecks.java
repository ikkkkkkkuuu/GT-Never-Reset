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
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.IMuiScreen;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.xyp.gtneverreset.common.item.items.ItemUniversalWand;
import com.xyp.gtneverreset.common.packet.UniversalWandNetwork;
import com.xyp.gtneverreset.common.wand.UniversalWandMiningHandler;
import com.xyp.gtneverreset.config.ScreenThemeConfig;
import com.xyp.gtnrlib.gui.theme.ScreenTheme;
import com.xyp.gtnrlib.gui.widget.SlideSwitch;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTECable;

@Mod(modid = "wandqa", name = "Useless Tools QA", version = "1", dependencies = "after:gtneverreset")
public final class UniversalWandChecks {

    private final ScreenTheme[] themes = ScreenTheme.values();
    private boolean launched, waiting, finished;
    private int ticks, theme, frames;
    private volatile int stage;
    private boolean oldProtection, oldBackfill, selectFortune, selectGt;
    private volatile boolean enginePassed;
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
            mc.gameSettings.guiScale = themes[theme] == ScreenTheme.DEFAULT ? 1
                : themes[theme] == ScreenTheme.AE2_DARK ? 3 : 2;
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
            if (stage == 1 && !syncPassed || stage == 6 && !enginePassed || stage >= 5 && panels.size() < stage - 3)
                return;
            var pages = (PagedWidget<?>) panel.getChildren()
                .stream()
                .filter(w -> w instanceof PagedWidget)
                .findFirst()
                .orElseThrow();
            if (find(panel, "shears") != null || find(panel, "hoe") != null)
                throw new AssertionError("Removed tools visible");
            if (find(panel, "gt_mode") != null) throw new AssertionError("GT tool mode is still visible");
            if (stage < 5 && pages.getCurrentPageIndex() != stage)
                throw new AssertionError("Wrong page at stage " + stage);
            ScreenShotHelper.saveScreenshot(
                new File("."),
                themes[theme].name() + "-" + stage + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            if (stage == 0) {
                oldProtection = ItemUniversalWand.isProtectionMode(mc.thePlayer.getHeldItem());
                oldBackfill = ItemUniversalWand.isOreBackfillEnabled(mc.thePlayer.getHeldItem());
                ((SlideSwitch) find(panel, "ore_backfill")).onMousePressed(0);
                syncPassed = false;
                selectFortune = ItemUniversalWand.getEnchantMode(mc.thePlayer.getHeldItem())
                    == ItemUniversalWand.EnchantMode.SILK;
                var enchant = (ButtonWidget<?>) find(panel, selectFortune ? "fortune" : "silk");
                enchant.onMousePressed(0);
                enchant.onMousePressed(0);
                ((SlideSwitch) find(panel, "protection")).onMousePressed(0);
            }
            if (stage < 4) {
                var nextTab = panel.getChildren()
                    .stream()
                    .filter(w -> w instanceof PageButton)
                    .skip(stage + 1)
                    .findFirst()
                    .orElseThrow();
                ((PageButton) nextTab).onMousePressed(0);
            } else if (stage == 4) {
                ((ButtonWidget<?>) find(panel, "acceleration")).onMousePressed(0);
            } else if (stage == 5) {
                selectGt = !ItemUniversalWand.isAccelerationGtMode(mc.thePlayer.getHeldItem());
                enginePassed = false;
                var engine = panels.stream()
                    .map(p -> find(p, selectGt ? "acceleration_engine_gt" : "acceleration_engine_normal"))
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElseThrow();
                ((ButtonWidget<?>) engine).onMousePressed(0);
                ((ButtonWidget<?>) engine).onMousePressed(0);
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
        if (event.phase != TickEvent.Phase.END || finished || miningChecked && stage != 1 && stage != 6) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        var player = (EntityPlayerMP) players.get(0);
        var held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemUniversalWand)) return;
        if (!miningChecked) {
            try {
                if (GameRegistry.findItem("gtneverreset", "insect_staff") != null)
                    throw new AssertionError("Removed insect staff is still registered");
                if (GameRegistry.findItem("gtneverreset", "item.VeinMiningPickaxe") == null)
                    throw new AssertionError("Vein mining pickaxe is missing");
                checkInstantMining(player, held);
                checkBackfill(player, held);
                miningChecked = true;
            } catch (Throwable failure) {
                Minecraft.getMinecraft()
                    .func_152344_a(() -> finish(failure));
            }
        } else if (stage == 1) {
            syncPassed = ItemUniversalWand.isProtectionMode(held) != oldProtection
                && ItemUniversalWand.isOreBackfillEnabled(held) != oldBackfill
                && (ItemUniversalWand.getEnchantMode(held) == ItemUniversalWand.EnchantMode.FORTUNE) == selectFortune;
        } else if (stage == 6) enginePassed = ItemUniversalWand.isAccelerationGtMode(held) == selectGt;
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
            var item = wand.getItem();
            if (!item.getToolClasses(wand)
                .containsAll(java.util.Arrays.asList("pickaxe", "axe"))
                || !net.minecraftforge.common.ForgeHooks.isToolEffective(wand, Blocks.stone, 0)
                || !net.minecraftforge.common.ForgeHooks.isToolEffective(wand, Blocks.log, 0))
                throw new AssertionError("Wand pickaxe/axe classification");
            for (String type : new String[] { "pickaxe", "axe" })
                if (item.getHarvestLevel(wand, type) != Integer.MAX_VALUE)
                    throw new AssertionError("Wand harvest level for " + type);
            for (var target : new net.minecraft.block.Block[] { Blocks.obsidian, Blocks.log }) {
                world.setBlock(x, y + 2, z, target);
                player.theItemInWorldManager.onBlockClicked(x, y + 2, z, 1);
                if (!world.isAirBlock(x, y + 2, z)) throw new AssertionError("Failed first-click harvest: " + target);
            }
            ItemUniversalWand.setProtectionMode(player, wand, true);
            if (item.getDigSpeed(wand, Blocks.log, 0) != 0F || item.getHarvestLevel(wand, "pickaxe") != 0)
                throw new AssertionError("Protection mode failed to disable mining");
            ItemUniversalWand.setProtectionMode(player, wand, false);
            ItemUniversalWand.setInventoryCollection(player, wand, true);
            ItemUniversalWand.setMaxMiningCount(player, wand, 23);
            ItemUniversalWand.setEnhancedChain(player, wand, false);
            for (int i = 0; i < 25; i++) world.setBlock(x + i % 5, y, z + i / 5, Blocks.gold_block);
            checkRemovedTools(player, wand, x, y + 8, z);
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
                || !world.isAirBlock(x + 3, y, z)
                || goldBlocks(player) - before != 26) throw new AssertionError("Instant enhanced mining gap or drops");
        } finally {
            UniversalWandMiningHandler.setKeyHeld(player, false);
            player.inventory.setInventorySlotContents(slot, original);
            player.sendContainerToPlayer(player.inventoryContainer);
        }
    }

    private static void checkBackfill(EntityPlayerMP player, ItemStack wand) {
        var world = player.worldObj;
        int x = (player.chunkCoordX << 4) + 2, z = (player.chunkCoordZ << 4) + 2, y = 100;
        ItemUniversalWand.setProtectionMode(player, wand, false);
        ItemUniversalWand.setEnhancedChain(player, wand, false);
        ItemUniversalWand.setMaxMiningCount(player, wand, 16);
        var handler = new UniversalWandMiningHandler();
        for (int mode = 0; mode < 4; mode++) {
            ItemUniversalWand.setOreBackfillEnabled(player, wand, mode != 0);
            UniversalWandMiningHandler.setKeyHeld(player, mode != 2);
            for (int dx = 0; dx < 2; dx++) world.setBlock(x + dx, y, z, Blocks.iron_ore);
            if (!player.theItemInWorldManager.tryHarvestBlock(x, y, z)) throw new AssertionError("Ore harvest failed");
            if (mode == 3) world.setBlock(x, y, z, Blocks.glass);
            handler.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
            var expected = mode == 1 ? Blocks.stone : mode == 3 ? Blocks.glass : Blocks.air;
            if (world.getBlock(x, y, z) != expected) throw new AssertionError("Backfill mode " + mode);
            if (mode != 2 && world.getBlock(x + 1, y, z) != (mode == 0 ? Blocks.air : Blocks.stone))
                throw new AssertionError("Chain target backfill mode " + mode);
            y += 4;
        }
        world.setBlock(x, y, z, Blocks.gold_block);
        player.theItemInWorldManager.tryHarvestBlock(x, y, z);
        handler.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        if (!world.isAirBlock(x, y, z)) throw new AssertionError("Non-ore backfilled");
        var legacy = wand.getTagCompound()
            .getCompoundTag("gtneverresetUniversalWand");
        legacy.setBoolean("hoeEnabled", true);
        legacy.setBoolean("shearsEnabled", true);
        world.setBlock(x, y, z, Blocks.grass);
        if (wand.getItem()
            .onItemUse(wand, player, world, x, y, z, 1, 0.5F, 1F, 0.5F) || world.getBlock(x, y, z) != Blocks.grass)
            throw new AssertionError("Legacy hoe still active");
        var sheep = new net.minecraft.entity.passive.EntitySheep(world);
        if (wand.getItem()
            .itemInteractionForEntity(wand, player, sheep) || sheep.getSheared())
            throw new AssertionError("Legacy shears still active");
        ItemUniversalWand.setOreBackfillEnabled(player, wand, false);
        UniversalWandMiningHandler.setKeyHeld(player, false);
    }

    private static void checkRemovedTools(EntityPlayerMP player, ItemStack wand, int x, int y, int z) {
        var world = player.worldObj;
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        var legacy = wand.getTagCompound()
            .getCompoundTag("gtneverresetUniversalWand");
        legacy.setInteger("gtToolMode", 1);
        var machine = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        machine.setInitialValuesAsNBT(
            null,
            (short) ItemList.Machine_HV_Bender.get(1)
                .getItemDamage());
        machine.setFrontFacing(ForgeDirection.NORTH);
        machine.setOwnerName(player.getCommandSenderName());
        wand.getItem()
            .onItemUseFirst(wand, player, world, x, y, z, 5, 1F, 0.5F, 0.5F);
        if (machine.getFrontFacing() != ForgeDirection.NORTH) throw new AssertionError("Wand rotated GT machine");
        for (var meta : GregTechAPI.METATILEENTITIES) {
            if (!(meta instanceof MTECable cable)) continue;
            for (int dx = 1; dx <= 2; dx++) {
                world.setBlock(x + dx, y, z, GregTechAPI.sBlockMachines, cable.getTileEntityBaseType(), 3);
                var pipe = (BaseMetaPipeEntity) world.getTileEntity(x + dx, y, z);
                pipe.setInitialValuesAsNBT(
                    null,
                    (short) cable.getStackForm(1)
                        .getItemDamage());
            }
            var pipe = (BaseMetaPipeEntity) world.getTileEntity(x + 1, y, z);
            legacy.setInteger("gtToolMode", 2);
            var placed = (MTECable) pipe.getMetaTileEntity();
            placed.connect(ForgeDirection.EAST);
            if (!placed.isConnectedAtSide(ForgeDirection.EAST)) throw new AssertionError("Cable fixture not connected");
            wand.getItem()
                .onItemUseFirst(wand, player, world, x + 1, y, z, 5, 1F, 0.5F, 0.5F);
            if (!placed.isConnectedAtSide(ForgeDirection.EAST) || player.getHeldItem() != wand)
                throw new AssertionError("Wand disconnected GT cable");
            return;
        }
        throw new AssertionError("No GT cable for wand test");
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
