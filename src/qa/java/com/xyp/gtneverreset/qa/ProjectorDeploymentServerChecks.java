package com.xyp.gtneverreset.qa;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtnrlib.animation.StructureDeployment;
import com.xyp.gtnrlib.animation.StructureDeployment.Launch;
import com.xyp.gtnrlib.animation.StructureDeploymentStyle;

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
import io.netty.buffer.ByteBufUtil;
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
    private int build;
    private boolean cancelTest;
    private long cancelDeadline = -1;

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
            request(StructureDeploymentStyle.SUMMON);
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
            if (cancelTest) {
                checkCancelledWarp();
                return;
            }
            if (!(world.getTileEntity(plan.x, Y, plan.z) instanceof IGregTechTileEntity tile)) return;
            require(tile.getMetaTileID() == plan.machine, "projector placed the wrong controller");
            int delay = switch (plan.buildStyle) {
                case SUMMON -> StructureDeployment.IMPACT_TICK;
                case REVEAL, FLIGHT -> plan.buildStyle.bandReadyTick(0);
                case SINGULARITY -> StructureDeploymentStyle.SINGULARITY_LAUNCH_TICKS
                    + plan.buildStyle.bandReadyTick(0);
                case NONE -> 0;
            };
            require(elapsed >= delay, "projector skipped the animation wait for " + plan.buildStyle);
            if (elapsed < 42) return;
            var bounds = new StructureDeployment(start, plan.x - 1, Y, plan.z, plan.x + 2, plan.z + 3, Y + 19);
            checkPacket(new ProjectorDeploymentPacket(1, 0, bounds, plan, false));
            checkPacket(new ProjectorDeploymentPacket(1, 0, Phase.IMPACT));
            if (plan.buildStyle == StructureDeploymentStyle.SINGULARITY) {
                var launch = new Launch(plan.x + .06, Y + 1.3, plan.z - 3.9, plan.x + .94, plan.z - 3.7);
                var tallBounds = new StructureDeployment(
                    start,
                    plan.x - 1,
                    Y,
                    plan.z,
                    plan.x + 2,
                    plan.z + 3,
                    Y + 19,
                    Y + 17);
                var startPacket = new ProjectorDeploymentPacket(1, 0, tallBounds, plan, false, launch);
                checkPacket(startPacket);
                checkLaunchSuffixes(startPacket, launch);
                checkPacket(new ProjectorDeploymentPacket(1, 0, world.getTotalWorldTime()));
            }
            checkPacket(new ProjectorDeploymentPacket(1, 0, Phase.COMPLETE));
            checkPacket(new ProjectorDeploymentPacket(1, 0, Phase.CANCEL));
            checkPacket(new ProjectorDeploymentPacket(1, 0, 1));
            if (++build < StructureDeploymentStyle.values().length) {
                plan.x += 8;
                request(StructureDeploymentStyle.values()[build]);
            } else {
                cancelTest = true;
                plan.x += 8;
                request(StructureDeploymentStyle.SINGULARITY);
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void checkCancelledWarp() throws Exception {
        var jobsField = ProjectorBuilder.class.getDeclaredField("jobs");
        jobsField.setAccessible(true);
        var jobs = (Map<?, ?>) jobsField.get(ProjectorBuilder.INSTANCE);
        Object job = jobs.get(player.getUniqueID());
        if (cancelDeadline < 0) {
            require(job != null, "cancel test did not start a build");
            var animationField = job.getClass()
                .getDeclaredField("deployment");
            animationField.setAccessible(true);
            var animation = (StructureDeployment) animationField.get(job);
            if (animation == null || world.getTotalWorldTime() < animation.startTick() - 4) return;
            var packetField = job.getClass()
                .getDeclaredField("deploymentStart");
            packetField.setAccessible(true);
            checkHandOrigins(readLaunch((ProjectorDeploymentPacket) packetField.get(job)));
            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            cancelDeadline = animation.startTick() + plan.buildStyle.bandReadyTick(0) + 10;
        } else if (world.getTotalWorldTime() >= cancelDeadline) {
            require(job == null, "cancelled warp remained active");
            for (int x = plan.x - 3; x <= plan.x + 3; x++) for (int z = plan.z - 3; z <= plan.z + 3; z++)
                for (int y = Y - 3; y < Y + 4; y++) require(world.isAirBlock(x, y, z), "cancelled warp placed a block");
            finish(null);
        }
    }

    private void request(StructureDeploymentStyle style) {
        plan.buildStyle = style;
        var item = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
        plan.write(item);
        require(ProjectorPlan.read(item).buildStyle == style, "deployment style did not survive NBT round trip");
        player.setPosition(plan.x + 0.5, Y, plan.z - 4);
        player.inventory.setInventorySlotContents(player.inventory.currentItem, item);
        start = world.getTotalWorldTime();
        ProjectorBuilder.request(player, 0, false);
        ProjectorBuilder.request(player, 0, false);
    }

    private void checkPacket(ProjectorDeploymentPacket packet) {
        var buffer = Unpooled.buffer();
        var encoded = Unpooled.buffer();
        try {
            packet.toBytes(buffer);
            var original = buffer.duplicate();
            var decoded = new ProjectorDeploymentPacket();
            decoded.fromBytes(buffer);
            require(!buffer.isReadable(), "deployment packet was not fully decoded");
            decoded.toBytes(encoded);
            require(ByteBufUtil.equals(original, encoded), "deployment packet changed on round trip");
            NetWorkHandler.sendToAllAround(decoded, new TargetPoint(0, plan.x, Y, plan.z, 64));
        } finally {
            encoded.release();
            buffer.release();
        }
    }

    private static void checkLaunchSuffixes(ProjectorDeploymentPacket packet, Launch launch) throws Exception {
        var buffer = Unpooled.buffer();
        try {
            packet.toBytes(buffer);
            int end = buffer.writerIndex();
            var decoded = new ProjectorDeploymentPacket();
            decoded.fromBytes(buffer.duplicate());
            require(launch.equals(readLaunch(decoded)), "both hand origins must survive the packet");

            var previous = new Launch(launch.x(), launch.y(), launch.z());
            for (int removed : new int[] { 2 * Double.BYTES, 2 * Double.BYTES + Integer.BYTES }) {
                var legacy = buffer.duplicate()
                    .writerIndex(end - removed);
                decoded.fromBytes(legacy);
                require(!legacy.isReadable(), "legacy launch packet was not fully decoded");
                require(previous.equals(readLaunch(decoded)), "legacy launch must supply its original position");
            }

            buffer.setDouble(end - Double.BYTES, Double.NaN);
            try {
                decoded.fromBytes(buffer.duplicate());
                throw new AssertionError("non-finite left launch position was accepted");
            } catch (IllegalArgumentException expected) {
                require(
                    expected.getMessage()
                        .contains("launch position"),
                    "invalid launch was rejected for another reason");
            }
        } finally {
            buffer.release();
        }
    }

    private static Launch readLaunch(ProjectorDeploymentPacket packet) throws Exception {
        var field = ProjectorDeploymentPacket.class.getDeclaredField("launch");
        field.setAccessible(true);
        return (Launch) field.get(packet);
    }

    private void checkHandOrigins(Launch launch) {
        require(launch != null, "server omitted the projectile launch");
        double yaw = Math.toRadians(player.rotationYaw);
        double stepX = launch.leftX() - launch.x(), stepZ = launch.leftZ() - launch.z();
        require(Math.abs(Math.hypot(stepX, stepZ) - .88) < .001, "hand origins must be .88 blocks apart");
        require(stepX * Math.cos(yaw) + stepZ * Math.sin(yaw) > .87, "hand origins were reversed");
        var look = player.getLookVec();
        double midpointX = (launch.x() + launch.leftX()) * .5 - player.posX;
        double midpointZ = (launch.z() + launch.leftZ()) * .5 - player.posZ;
        require(
            Math.hypot(midpointX - look.xCoord * .55, midpointZ - look.zCoord * .55) < .001,
            "hand origins must straddle the point ahead of the player");
        require(
            Math.abs(launch.y() - (player.posY + player.getEyeHeight() - .36 + look.yCoord * .55)) < .001,
            "hand origins must be near the player's chest");
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
