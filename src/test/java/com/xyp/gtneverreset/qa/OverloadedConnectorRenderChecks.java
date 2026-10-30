package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.api.packaged.PackagedTarget;
import com.xyp.gtneverreset.client.render.WirelessConnectorRenderer;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityOverloadedProvider;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityPackagedProvider;
import com.xyp.gtneverreset.common.item.items.ItemWirelessConnector;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(
    modid = "overloadedrenderqa",
    name = "Overloaded connector render QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class OverloadedConnectorRenderChecks {

    private static final int[][] TARGETS = { { -4, 10, 0 }, { 4, 10, 0 }, { -2, 10, 3 }, { 2, 10, 3 }, { -2, 10, -3 },
        { 2, 10, -3 } };
    private boolean started, finished;
    private int ticks, frames;
    private volatile int stage;
    private TileEntityOverloadedProvider provider;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "overloaded-render-" + System.currentTimeMillis(),
                "Overloaded render QA",
                new WorldSettings(71, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            ticks++;
            if (ticks == 30) {
                var world = player.worldObj;
                world.setWorldTime(6000);
                world.getGameRules()
                    .setOrCreateGameRule("doDaylightCycle", "false");
                world.setBlock(
                    0,
                    10,
                    0,
                    net.minecraft.block.Block.getBlockFromItem(
                        GTNRItemList.OverloadedWirelessPatternProvider.get(1)
                            .getItem()));
                provider = (TileEntityOverloadedProvider) world.getTileEntity(0, 10, 0);
                provider.setOwnerName(player.getCommandSenderName());
                player.inventory.setInventorySlotContents(0, GTNRItemList.ItemWirelessConnector.get(1));
                player.inventory.currentItem = 0;
                ItemWirelessConnector.use(player, 0, 10, 0, 1);
                player.setSneaking(true);
                for (int face = 0; face < 6; face++) {
                    int[] pos = TARGETS[face];
                    world.setBlock(pos[0], pos[1], pos[2], Blocks.furnace);
                    ItemWirelessConnector.use(player, pos[0], pos[1], pos[2], face);
                }
                player.setSneaking(false);
                require(
                    provider.connections()
                        .size() == 6,
                    "connector binds all six face directions");
                world.setBlock(0, 10, -4, Blocks.furnace);
                world.setBlock(
                    0,
                    10,
                    6,
                    net.minecraft.block.Block.getBlockFromItem(
                        GTNRItemList.WirelessPackagedPatternProvider.get(1)
                            .getItem()));
                var packaged = (TileEntityPackagedProvider) world.getTileEntity(0, 10, 6);
                packaged.setOwnerName(player.getCommandSenderName());
                packaged.bind(new PackagedTarget(world.provider.dimensionId, -2, 10, 3, 1));
                var packet = (S35PacketUpdateTileEntity) provider.getDescriptionPacket();
                require(
                    packet.func_148857_g()
                        .func_150296_c()
                        .size() == 2 && packet.func_148857_g()
                            .hasKey("WirelessConnections"),
                    "snapshot contains only connection coordinates and custom name");
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                player.playerNetServerHandler.setPlayerLocation(5, 14, -8, 32, 27);
                player.inventoryContainer.detectAndSendChanges();
            }
            if (ticks == 80) stage = 1;
            if (stage == 2) {
                require(
                    provider.unlink(
                        provider.connections()
                            .get(1).target),
                    "unlink server target");
                stage = 3;
            } else if (stage == 4) {
                ItemStack held = player.getHeldItem();
                held.getItem()
                    .onItemRightClick(held, player.worldObj, player);
                player.inventoryContainer.detectAndSendChanges();
                stage = 5;
            } else if (stage == 6) {
                player.inventory.setInventorySlotContents(0, null);
                player.inventoryContainer.detectAndSendChanges();
                stage = 7;
            }
            if (ticks > 1000) throw new AssertionError("render QA timeout, stage " + stage);
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void aim(RenderWorldLastEvent event) {
        if (stage <= 0 || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.ingameGUI.getChatGUI()
            .clearChatMessages();
        mc.objectMouseOver = new MovingObjectPosition(0, 10, -4, 2, Vec3.createVectorHelper(.5, 10.5, -4));
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || stage <= 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null
            || !(mc.theWorld.getTileEntity(0, 10, 0) instanceof TileEntityOverloadedProvider client)) return;
        try {
            if (++frames < 90) return;
            if (stage == 1) {
                require(
                    client.visibleConnections()
                        .size() == 6,
                    "six bindings synchronize to the real client");
                for (int face = 0; face < 6; face++) require(
                    client.visibleConnections()
                        .get(face).face == face,
                    "bound face " + face + " preserved");
                Method preview = WirelessConnectorRenderer.class
                    .getDeclaredMethod("preview", Minecraft.class, TileEntity.class);
                preview.setAccessible(true);
                PackagedTarget target = (PackagedTarget) preview.invoke(null, mc, client);
                require(target != null && target.face == 2 && target.z == -4, "preview uses the hit face");
                mc.objectMouseOver = new MovingObjectPosition(4, 10, 0, 1, Vec3.createVectorHelper(4.5, 11, .5));
                require(preview.invoke(null, mc, client) == null, "already-bound machine has no duplicate preview");
                mc.objectMouseOver = new MovingObjectPosition(0, 10, 0, 1, Vec3.createVectorHelper(.5, 11, .5));
                require(preview.invoke(null, mc, client) == null, "host cannot preview itself");
                screenshot("overloaded-render-selected.png");
                stage = 2;
                frames = 0;
            } else if (stage == 3 && client.visibleConnections()
                .size() == 5) {
                    require(
                        client.visibleConnections()
                            .stream()
                            .noneMatch(target -> target.x == 4),
                        "unlink removes client face and line without reopening GUI");
                    screenshot("overloaded-render-unlinked.png");
                    stage = 4;
                    frames = 0;
                } else if (stage == 5 && ItemWirelessConnector.selection(mc.thePlayer.getHeldItem()) == null) {
                    require(
                        mc.theWorld.getTileEntity(0, 10, 6) instanceof TileEntityPackagedProvider,
                        "packaged host remains present");
                    screenshot("overloaded-render-unselected.png");
                    stage = 6;
                    frames = 0;
                } else if (stage == 7 && mc.thePlayer.getHeldItem() == null) {
                    screenshot("overloaded-render-no-tool.png");
                    finish(null);
                }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void finish(Throwable failure) {
        if (failure != null) failure.printStackTrace();
        finished = true;
        System.out.println("OVERLOADED_RENDER_QA: " + (failure == null ? "PASS" : "FAIL"));
        Minecraft.getMinecraft()
            .func_152344_a(
                () -> Minecraft.getMinecraft()
                    .shutdown());
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        System.out.println("OVERLOADED_RENDER_QA: " + message);
    }
}
