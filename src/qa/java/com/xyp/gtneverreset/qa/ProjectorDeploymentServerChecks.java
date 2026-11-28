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
import com.xyp.gtneverreset.common.packet.ProjectorBuildActionPacket.Action;
import com.xyp.gtneverreset.common.packet.ProjectorDeploymentPacket;
import com.xyp.gtneverreset.common.packet.ProjectorDeploymentPacket.Phase;
import com.xyp.gtneverreset.common.projector.ProjectorBuildActions;
import com.xyp.gtneverreset.common.projector.ProjectorBuilder;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorPlacementUpdates;
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
    private Position earlyBlock;

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
            if (earlyBlock == null) {
                earlyBlock = findHeldBlock();
                if (earlyBlock != null) {
                    require(plan.buildStyle != StructureDeploymentStyle.NONE, "unanimated placement delayed updates");
                    var animation = (StructureDeployment) jobField(activeBuild(), "deployment");
                    require(
                        world.getTotalWorldTime() < animation.startTick() + plan.buildStyle.bandReadyTick(0),
                        "ordinary blocks were not placed before the animation reveal");
                    require(!isAir(earlyBlock), "the held ordinary block was not placed in the server world");
                    require(world.isAirBlock(plan.x, Y, plan.z), "controller was placed before the animation reveal");
                }
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
            if (activeBuild() != null || elapsed < 42) return;
            if (plan.buildStyle != StructureDeploymentStyle.NONE) {
                require(earlyBlock != null, "no early world placement with delayed sync for " + plan.buildStyle);
                require(!isAir(earlyBlock), "ordinary placement disappeared after the reveal");
            }
            require(findHeldBlock() == null, "a completed build kept block updates on hold");
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
        Object job = activeBuild();
        if (cancelDeadline < 0) {
            require(job != null, "cancel test did not start a build");
            earlyBlock = findHeldBlock();
            if (earlyBlock == null) return;
            var animation = (StructureDeployment) jobField(job, "deployment");
            long revealTick = animation.startTick() + plan.buildStyle.bandReadyTick(0);
            require(world.getTotalWorldTime() < revealTick, "cancel test missed the hidden placement window");
            require(!isAir(earlyBlock), "cancel test did not place an ordinary block");
            checkHandOrigins(readLaunch((ProjectorDeploymentPacket) jobField(job, "deploymentStart")));
            ProjectorBuildActions.request(player, 0, Action.CANCEL, false);
            require(activeBuild() == null, "cancel did not end the build");
            require(findHeldBlock() == null, "cancel did not release delayed block updates immediately");
            cancelDeadline = revealTick + 10;
        } else if (world.getTotalWorldTime() >= cancelDeadline) {
            require(job == null, "cancelled warp remained active");
            require(!isAir(earlyBlock), "cancelled warp removed an already placed ordinary block");
            require(world.isAirBlock(plan.x, Y, plan.z), "cancelled warp placed its controller");
            finish(null);
        }
    }

    private Object activeBuild() throws Exception {
        var jobsField = ProjectorBuilder.class.getDeclaredField("jobs");
        jobsField.setAccessible(true);
        var jobs = (Map<?, ?>) jobsField.get(ProjectorBuilder.INSTANCE);
        return jobs.get(player.getUniqueID());
    }

    private static Object jobField(Object job, String name) throws Exception {
        var field = job.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        return field.get(job);
    }

    private Position findHeldBlock() {
        for (int x = plan.x - 4; x <= plan.x + 4; x++) {
            for (int z = plan.z - 4; z <= plan.z + 4; z++) {
                for (int y = Y - 4; y <= Y + 4; y++) {
                    if (ProjectorPlacementUpdates.isHeld(world, x, y, z)) return new Position(x, y, z);
                }
            }
        }
        return null;
    }

    private boolean isAir(Position position) {
        return world.isAirBlock(position.x(), position.y(), position.z());
    }

    private void request(StructureDeploymentStyle style) {
        plan.buildStyle = style;
        var item = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
        plan.write(item);
        require(ProjectorPlan.read(item).buildStyle == style, "deployment style did not survive NBT round trip");
        player.setPosition(plan.x + 0.5, Y, plan.z - 4);
        player.inventory.setInventorySlotContents(player.inventory.currentItem, item);
        start = world.getTotalWorldTime();
        earlyBlock = null;
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

    private record Position(int x, int y, int z) {}
}
