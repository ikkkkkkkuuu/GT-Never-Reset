package com.xyp.gtneverreset.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.client.projector.ProjectorClient;
import com.xyp.gtneverreset.client.projector.ProjectorDeploymentRenderer;
import com.xyp.gtneverreset.common.packet.GenerateProjectorBatchPacket;
import com.xyp.gtneverreset.common.packet.GenerateProjectorPacket;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.projector.ProjectorDeployment;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

@Mod(
    modid = "gtnrprojectordeploymentqa",
    name = "Projector deployment checks",
    version = "1",
    dependencies = "after:gtneverreset")
public class ProjectorDeploymentChecks {

    private enum Stage {
        SETUP,
        SINGLE,
        PREPARE_BATCH,
        BATCH,
        PREPARE_SUPPLEMENT,
        SUPPLEMENT,
        DONE
    }

    private static final int Y = 10;
    private volatile Stage stage = Stage.SETUP;
    private volatile long requested = -1;
    private volatile boolean done;
    private boolean launched, singlePicture, batchPicture;
    private Stage sent;
    private Object batchPresentation;
    private boolean circlePicture, impactPicture, allWaves;
    private int ticks, x, z, machine;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        var player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            if (stage == Stage.SETUP) {
                x = (int) player.posX + 3;
                z = (int) player.posZ + 12;
                machine = ProjectorMachines.idOf(ItemList.Machine_Multi_BlastFurnace.get(1));
                setup(player, 0);
                stage = Stage.SINGLE;
            } else if (stage == Stage.PREPARE_BATCH) {
                z += 21;
                setup(player, 1);
                stage = Stage.BATCH;
            } else if (stage == Stage.PREPARE_SUPPLEMENT) {
                setup(player, 2);
                stage = Stage.SUPPLEMENT;
            } else if (requested >= 0) {
                long elapsed = player.worldObj.getTotalWorldTime() - requested;
                if (stage == Stage.SINGLE && controller(player, x, z)) {
                    require(elapsed >= ProjectorDeployment.IMPACT_TICK, "Single machine placed before landing");
                    stage = Stage.PREPARE_BATCH;
                    requested = -1;
                } else if (stage == Stage.BATCH && controller(player, x + 3, z)) {
                    require(elapsed >= ProjectorDeployment.IMPACT_TICK, "Batch placed before landing");
                    require(controller(player, x, z), "Batch first controller missing");
                    stage = Stage.PREPARE_SUPPLEMENT;
                    requested = -1;
                } else if (stage == Stage.SUPPLEMENT && controller(player, x + 6, z)) {
                    require(elapsed < ProjectorDeployment.IMPACT_TICK, "Mixed batch waited for a landing");
                    stage = Stage.DONE;
                }
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void setup(EntityPlayerMP player, int extra) {
        var plan = new ProjectorPlan();
        plan.selectMachine(machine);
        plan.state = ProjectorPlan.State.LOCKED;
        plan.dimension = player.dimension;
        plan.x = x;
        plan.y = Y;
        plan.z = z;
        plan.facing = ExtendedFacing.of(ForgeDirection.NORTH);
        plan.batch.setAdditional(ForgeDirection.EAST, extra);
        plan.batchProjection = extra > 0;
        if (extra == 1) {
            for (int dx = -4; dx <= 9; dx++)
                for (int dz = -4; dz <= 7; dz++) player.worldObj.setBlock(x + dx, Y - 1, z + dz, Blocks.stone, 0, 2);
        }
        var item = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
        plan.write(item);
        player.inventory.setInventorySlotContents(player.inventory.currentItem, item);
        player.inventoryContainer.detectAndSendChanges();
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.rotationYaw = 25;
        player.rotationPitch = -5;
        player.setPositionAndUpdate(x + (extra == 0 ? 8 : 9.5), Y + 8, z - 17);
    }

    private boolean controller(EntityPlayerMP player, int x, int z) {
        return player.worldObj.getTileEntity(x, Y, z) instanceof IGregTechTileEntity tile
            && tile.getMetaTileID() == machine;
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getMinecraft();
        if (done) {
            mc.shutdown();
            return;
        }
        try {
            if (++ticks > 2400) throw new AssertionError("timeout at " + stage);
            if (!launched && mc.currentScreen != null
                && (mc.currentScreen instanceof GuiMainMenu || mc.currentScreen.getClass()
                    .getSimpleName()
                    .equals("GSGuiMainMenu"))) {
                launched = true;
                mc.launchIntegratedServer(
                    "projector-deployment-" + System.currentTimeMillis(),
                    "Projector Deployment QA",
                    new WorldSettings(4L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT)
                        .enableCommands());
            }
            Stage current = stage;
            if (current == Stage.DONE) {
                require(singlePicture && batchPicture, "Missing descent screenshots");
                if (!allWaves) return;
                require(circlePicture && impactPicture, "Missing summoning-circle or impact screenshot");
                finish(null);
            }
            if (sent == current || mc.theWorld == null || ProjectorClient.plan == null) return;
            if (current != Stage.SINGLE && current != Stage.BATCH && current != Stage.SUPPLEMENT) return;
            int expected = current == Stage.SINGLE ? 1 : current == Stage.BATCH ? 2 : 3;
            if (ProjectorClient.plan.z != z || ProjectorClient.plan.batch.machines() != expected) return;
            var server = mc.getIntegratedServer();
            requested = server.worldServerForDimension(0)
                .getTotalWorldTime();
            sent = current;
            NetWorkHandler
                .sendToServer(expected == 1 ? new GenerateProjectorPacket(0) : new GenerateProjectorBatchPacket(0));
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void capture(TickEvent.RenderTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END) return;
        captureImpact();
        if (stage != Stage.SINGLE && stage != Stage.BATCH) return;
        boolean single = stage == Stage.SINGLE;
        if (single ? singlePicture : batchPicture) return;
        var mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.currentScreen != null) return;
        try {
            mc.gameSettings.hideGUI = true;
            var field = ProjectorDeploymentRenderer.class.getDeclaredField("ACTIVE");
            field.setAccessible(true);
            Map<?, ?> animations = (Map<?, ?>) field.get(null);
            if (animations.isEmpty()) return;
            Object presentation = animations.values()
                .iterator()
                .next();
            var animationField = presentation.getClass()
                .getDeclaredField("animation");
            animationField.setAccessible(true);
            var animation = (ProjectorDeployment) animationField.get(presentation);
            if (z < animation.minZ() || z >= animation.maxZ()) return;
            long age = mc.theWorld.getTotalWorldTime() - animation.startTick();
            if (!single && !circlePicture && age >= 40) {
                ScreenShotHelper.saveScreenshot(
                    new File("."),
                    "batch-circle.png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                circlePicture = true;
            }
            if (age < 100) return;
            require(animations.size() == 1, "Batch started several animations");
            require(mc.theWorld.isAirBlock(x, Y, z), "World contains a controller during descent");
            if (!single) require(animation.maxX() - animation.minX() == 6, "Portal bounds exclude a machine");
            ScreenShotHelper.saveScreenshot(
                new File("."),
                single ? "single-descent.png" : "batch-descent.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            if (single) singlePicture = true;
            else {
                batchPicture = true;
                batchPresentation = presentation;
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void captureImpact() {
        if (batchPresentation == null || allWaves) return;
        var mc = Minecraft.getMinecraft();
        try {
            var field = batchPresentation.getClass()
                .getDeclaredField("impact");
            field.setAccessible(true);
            Object impact = field.get(batchPresentation);
            if (impact == null) return;
            var waveField = impact.getClass()
                .getDeclaredField("nextWave");
            waveField.setAccessible(true);
            var ageMethod = impact.getClass()
                .getDeclaredMethod("age");
            ageMethod.setAccessible(true);
            long age = (long) ageMethod.invoke(impact);
            if (!impactPicture && age >= 8) {
                require(waveField.getInt(impact) >= 2, "Second impact dust wave missing");
                ScreenShotHelper.saveScreenshot(
                    new File("."),
                    "batch-impact.png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                impactPicture = true;
            }
            allWaves = waveField.getInt(impact) == 3;
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private static void require(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }

    private void finish(Throwable failure) {
        try {
            var trace = new StringWriter();
            if (failure != null) failure.printStackTrace(new PrintWriter(trace));
            Files.writeString(Path.of("result.txt"), failure == null ? "PASS" : "FAIL " + trace);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        done = true;
    }
}
