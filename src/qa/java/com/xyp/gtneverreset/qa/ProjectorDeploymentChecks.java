package com.xyp.gtneverreset.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorSimulation;
import com.xyp.gtnrlib.animation.StructureDeployment;
import com.xyp.gtnrlib.animation.StructureDeploymentStyle;

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
        PREPARE_REVEAL,
        REVEAL,
        PREPARE_FLIGHT,
        FLIGHT,
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
    private boolean outlinePicture, solidPicture, revealPicture, flightPicture;
    private long nextBuildTick;
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
            } else if (stage == Stage.PREPARE_REVEAL || stage == Stage.PREPARE_FLIGHT) {
                if (player.worldObj.getTotalWorldTime() < nextBuildTick) return;
                z += 21;
                Stage next = stage == Stage.PREPARE_REVEAL ? Stage.REVEAL : Stage.FLIGHT;
                setup(
                    player,
                    1,
                    next == Stage.REVEAL ? StructureDeploymentStyle.REVEAL : StructureDeploymentStyle.FLIGHT);
                requested = -1;
                stage = next;
            } else if (requested >= 0) {
                long elapsed = player.worldObj.getTotalWorldTime() - requested;
                if (stage == Stage.SINGLE && controller(player, x, z)) {
                    require(elapsed >= StructureDeployment.IMPACT_TICK, "Single machine placed before landing");
                    stage = Stage.PREPARE_BATCH;
                    requested = -1;
                } else if (stage == Stage.BATCH && controller(player, x + 3, z)) {
                    require(elapsed >= StructureDeployment.IMPACT_TICK, "Batch placed before landing");
                    require(controller(player, x, z), "Batch first controller missing");
                    stage = Stage.PREPARE_SUPPLEMENT;
                    requested = -1;
                } else if (stage == Stage.SUPPLEMENT && controller(player, x + 6, z)) {
                    require(elapsed < StructureDeployment.IMPACT_TICK, "Mixed batch waited for a landing");
                    nextBuildTick = requested + 42;
                    stage = Stage.PREPARE_REVEAL;
                } else if ((stage == Stage.REVEAL || stage == Stage.FLIGHT) && controller(player, x + 3, z)) {
                    var style = stage == Stage.REVEAL ? StructureDeploymentStyle.REVEAL
                        : StructureDeploymentStyle.FLIGHT;
                    require(elapsed >= style.bandReadyTick(1), "Segmented structure placed before the last band");
                    require(controller(player, x, z), "Segmented batch first controller missing");
                    nextBuildTick = requested + 42;
                    stage = stage == Stage.REVEAL ? Stage.PREPARE_FLIGHT : Stage.DONE;
                }
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void setup(EntityPlayerMP player, int extra) {
        setup(player, extra, StructureDeploymentStyle.SUMMON);
    }

    private void setup(EntityPlayerMP player, int extra, StructureDeploymentStyle style) {
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
        plan.buildStyle = style;
        if (extra == 0) chooseFrontHatch(plan);
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
        player.rotationYaw = extra == 0 ? 15 : 25;
        player.rotationPitch = style == StructureDeploymentStyle.FLIGHT ? -14
            : style == StructureDeploymentStyle.REVEAL ? 14 : -5;
        if (style == StructureDeploymentStyle.FLIGHT) player.setPositionAndUpdate(x + 18, Y + 5, z - 23);
        else if (extra == 0) player.setPositionAndUpdate(x + 3.5, Y + 6, z - 11);
        else player.setPositionAndUpdate(x + 9.5, Y + 8, z - 17);
    }

    private static void chooseFrontHatch(ProjectorPlan plan) {
        int hatch = ProjectorMachines.idOf(ItemList.Hatch_Energy_MV.get(1));
        var shape = ProjectorSimulation.run(plan.settings());
        int chosen = -1, frontZ = Integer.MAX_VALUE, rightX = Integer.MIN_VALUE;
        int[] offset = new int[3];
        for (int i = 0; i < shape.size; i++) {
            var slot = shape.slotAt(i);
            if (slot == null || Arrays.stream(slot.candidates())
                .noneMatch(id -> id == hatch)) continue;
            plan.facing.getWorldOffset(new int[] { shape.a[i], shape.b[i], shape.c[i] }, offset);
            if (offset[2] > frontZ || offset[2] == frontZ && offset[0] <= rightX) continue;
            chosen = i;
            frontZ = offset[2];
            rightX = offset[0];
        }
        require(chosen >= 0, "No visible MV energy hatch slot for the descent fixture");
        plan.overrides.put(ProjectorPlan.pack(shape.a[chosen], shape.b[chosen], shape.c[chosen]), hatch);
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
                require(
                    outlinePicture && solidPicture && revealPicture && flightPicture,
                    "Missing segmented deployment screenshots");
                finish(null);
            }
            if (sent == current || mc.theWorld == null || ProjectorClient.plan == null) return;
            if (current != Stage.SINGLE && current != Stage.BATCH
                && current != Stage.SUPPLEMENT
                && current != Stage.REVEAL
                && current != Stage.FLIGHT) return;
            int expected = current == Stage.SINGLE ? 1 : current == Stage.SUPPLEMENT ? 3 : 2;
            var style = current == Stage.REVEAL ? StructureDeploymentStyle.REVEAL
                : current == Stage.FLIGHT ? StructureDeploymentStyle.FLIGHT : StructureDeploymentStyle.SUMMON;
            if (ProjectorClient.plan.z != z || ProjectorClient.plan.batch.machines() != expected
                || ProjectorClient.plan.buildStyle != style) return;
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
        if (stage == Stage.REVEAL || stage == Stage.FLIGHT) captureSegmented();
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
            var animation = (StructureDeployment) field(field(presentation, "effects"), "bounds");
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

    private void captureSegmented() {
        var mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.currentScreen != null) return;
        try {
            var active = ProjectorDeploymentRenderer.class.getDeclaredField("ACTIVE");
            active.setAccessible(true);
            for (Object presentation : ((Map<?, ?>) active.get(null)).values()) {
                var effects = field(presentation, "effects");
                var animation = (StructureDeployment) field(effects, "bounds");
                if (z < animation.minZ() || z >= animation.maxZ()) continue;
                var style = stage == Stage.REVEAL ? StructureDeploymentStyle.REVEAL : StructureDeploymentStyle.FLIGHT;
                require(field(effects, "style") == style, "Selected style was not sent to the client");
                var model = presentation.getClass()
                    .getDeclaredField("model");
                model.setAccessible(true);
                if (model.get(presentation) == null) continue;
                long age = mc.theWorld.getTotalWorldTime() - animation.startTick();
                if (stage == Stage.REVEAL) {
                    if (!outlinePicture && age >= 7 && age < StructureDeploymentStyle.REVEAL_OUTLINE_TICKS) {
                        save("white-outline.png");
                        outlinePicture = true;
                    }
                    if (!solidPicture && age >= 14 && age < StructureDeploymentStyle.REVEAL_SOLID_TICKS) {
                        save("white-solid.png");
                        solidPicture = true;
                    }
                    if (!revealPicture && (int) field(effects, "finished") == 1) {
                        save("white-band-reveal.png");
                        revealPicture = true;
                    }
                } else if (!flightPicture && age >= 24 && age < StructureDeploymentStyle.FLIGHT_TRAVEL_TICKS) {
                    save("segmented-flight.png");
                    flightPicture = true;
                }
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void save(String name) {
        var mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void captureImpact() {
        if (batchPresentation == null || allWaves) return;
        var mc = Minecraft.getMinecraft();
        try {
            Object summoning = field(field(batchPresentation, "effects"), "summoning");
            Object impact = field(summoning, "impact");
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

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        var field = target.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
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
