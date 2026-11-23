package com.xyp.gtneverreset.qa;

import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.ProjectorDeploymentPacket;
import com.xyp.gtneverreset.common.packet.ProjectorDeploymentPacket.Phase;
import com.xyp.gtneverreset.common.projector.ProjectorBuilder;
import com.xyp.gtneverreset.common.projector.ProjectorDeployment;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import io.netty.buffer.Unpooled;

@Mod(
    modid = "gtnrprojectorserverqa",
    name = "Projector dedicated server QA",
    version = "1",
    dependencies = "required-after:gtneverreset")
public final class ProjectorDeploymentServerChecks {

    private static final int Y = 12;
    private MinecraftServer server;
    private WorldServer world;
    private EntityPlayerMP player;
    private ProjectorPlan plan;
    private long start;

    @Mod.EventHandler
    public void ready(FMLServerStartedEvent event) throws Exception {
        server = MinecraftServer.getServer();
        try {
            require(
                FMLCommonHandler.instance()
                    .getSide() == Side.SERVER,
                "physical dedicated server required");
            world = server.worldServerForDimension(0);
            player = FakePlayerFactory.getMinecraft(world);
            new NetHandlerPlayServer(server, new NetworkManager(false) {

                @Override
                public boolean isChannelOpen() {
                    return true;
                }
            }, player);
            player.capabilities.isCreativeMode = true;
            player.capabilities.allowEdit = true;
            var spawn = world.getSpawnPoint();
            plan = new ProjectorPlan();
            plan.selectMachine(ProjectorMachines.idOf(ItemList.Machine_Multi_BlastFurnace.get(1)));
            plan.state = ProjectorPlan.State.LOCKED;
            plan.dimension = 0;
            plan.x = spawn.posX;
            plan.y = Y;
            plan.z = spawn.posZ;
            plan.facing = ExtendedFacing.of(ForgeDirection.NORTH);
            var item = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
            plan.write(item);
            player.setPosition(plan.x + 0.5, Y, plan.z - 4);
            player.inventory.setInventorySlotContents(player.inventory.currentItem, item);
            start = world.getTotalWorldTime();
            ProjectorBuilder.request(player, 0, false);
            FMLCommonHandler.instance()
                .bus()
                .register(this);
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            long elapsed = world.getTotalWorldTime() - start;
            require(elapsed < 480, "projector did not finish on the dedicated server");
            if (!(world.getTileEntity(plan.x, Y, plan.z) instanceof IGregTechTileEntity tile)) return;
            require(elapsed >= ProjectorDeployment.IMPACT_TICK, "projector skipped the animation wait");
            require(tile.getMetaTileID() == plan.machine, "projector placed the wrong controller");
            var bounds = new ProjectorDeployment(start, plan.x - 1, Y, plan.z, plan.x + 2, plan.z + 3, Y + 19);
            checkPacket(new ProjectorDeploymentPacket(1, 0, bounds, plan, false));
            checkPacket(new ProjectorDeploymentPacket(1, 0, Phase.IMPACT));
            checkPacket(new ProjectorDeploymentPacket(1, 0, Phase.COMPLETE));
            checkPacket(new ProjectorDeploymentPacket(1, 0, Phase.CANCEL));
            finish(null);
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void checkPacket(ProjectorDeploymentPacket packet) {
        var buffer = Unpooled.buffer();
        try {
            packet.toBytes(buffer);
            var decoded = new ProjectorDeploymentPacket();
            decoded.fromBytes(buffer);
            require(!buffer.isReadable(), "deployment packet was not fully decoded");
            NetWorkHandler.sendToAllAround(decoded, new TargetPoint(0, plan.x, Y, plan.z, 64));
        } finally {
            buffer.release();
        }
    }

    private void finish(Throwable failure) throws Exception {
        try {
            Files.writeString(
                Path.of(System.getProperty("gtnr.projector.server.result")),
                failure == null ? "PASS\n" : "FAIL " + failure + "\n");
            if (failure != null) throw new AssertionError("projector dedicated server regression", failure);
        } finally {
            server.initiateShutdown();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
