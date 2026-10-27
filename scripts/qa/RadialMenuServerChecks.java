package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;

import com.mojang.authlib.GameProfile;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.packet.ToolBeltSyncPacket;
import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.utils.inventory.ToolBeltData;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@Mod(
    modid = "radialmenuserverqa",
    name = "Radial Menu Server QA",
    version = "1",
    dependencies = "after:" + GTNeverReset.MODID)
public final class RadialMenuServerChecks {

    private boolean finished;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.radialMenu.server.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.worldServerForDimension(0) == null) return;
        finished = true;
        String result = "PASS";
        try {
            require(server.isDedicatedServer(), "dedicated server runtime");
            require(
                MainConfig.client.radialMenu.wheel.outerRadius == 80,
                "common configuration loads without client GUI classes");
            var world = server.worldServerForDimension(0);
            var player = new EntityPlayerMP(
                server,
                world,
                new GameProfile(UUID.randomUUID(), "RadialServerQA"),
                new ItemInWorldManager(world));
            ToolBeltData data = ToolBeltData.get(player);
            require(
                data != null && data.getOwner() == player && data.getSlotCount() == 10,
                "player construction registers the persistent ten-slot belt");
            ItemStack[] snapshot = new ItemStack[24];
            snapshot[23] = new ItemStack(Items.apple, 48);
            snapshot[23].setStackDisplayName("Radial server QA");
            data.applySnapshot(snapshot);
            NBTTagCompound saved = new NBTTagCompound();
            data.saveNBTData(saved);
            ToolBeltData loaded = new ToolBeltData(player);
            loaded.loadNBTData(saved);
            require(
                loaded.getSlotCount() == 24 && ItemStack.areItemStacksEqual(snapshot[23], loaded.getStackInSlot(23)),
                "dedicated server NBT retains capacity and complete item data");
            ByteBuf buffer = Unpooled.buffer();
            try {
                new ToolBeltSyncPacket(player, loaded).toBytes(buffer);
                new ToolBeltSyncPacket().fromBytes(buffer);
                require(!buffer.isReadable(), "belt packets serialize without loading client handlers");
            } finally {
                buffer.release();
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            result = "FAIL: " + failure;
        } finally {
            try {
                File directory = new File(System.getProperty("gtnr.radialMenu.server.qa.output"));
                directory.mkdirs();
                Files.writeString(new File(directory, "result.txt").toPath(), result);
            } catch (Exception failure) {
                failure.printStackTrace();
            }
            System.out.println("RADIAL_MENU_SERVER_QA: " + result);
            server.initiateShutdown();
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        System.out.println("RADIAL_MENU_SERVER_QA: " + message);
    }
}
