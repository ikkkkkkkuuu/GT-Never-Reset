package com.xyp.gtneverreset.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.client.projector.ProjectorClient;
import com.xyp.gtneverreset.client.projector.ProjectorDeploymentRenderer;
import com.xyp.gtneverreset.client.projector.ProjectorGui;
import com.xyp.gtneverreset.common.packet.GenerateProjectorBatchPacket;
import com.xyp.gtneverreset.common.packet.GenerateProjectorPacket;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.projector.ProjectorBatch;
import com.xyp.gtneverreset.common.projector.ProjectorBuilder;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorSimulation;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtneverreset.config.ProjectorConfig;
import com.xyp.gtnrlib.animation.StructureDeployment;
import com.xyp.gtnrlib.animation.StructureDeploymentStyle;
import com.xyp.gtnrlib.client.shader.GlslProgram;
import com.xyp.gtnrlib.gui.widget.AE2Button;
import com.xyp.gtnrlib.gui.widget.PageTabs;
import com.xyp.gtnrlib.gui.widget.ThemedButton;
import com.xyp.gtnrlib.utils.Vec3d;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
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
        PREPARE_SINGULARITY,
        SINGULARITY,
        PREPARE_TALL,
        TALL,
        DONE;

        boolean singularity() {
            return this == SINGULARITY || this == TALL;
        }
    }

    private static final int Y = 10;
    private final boolean tallPreview = Boolean.getBoolean("gtnr.projector.singularityTallQa");
    private SingularityScenario singularity = new SingularityScenario(false);
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
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void placed(BlockEvent.PlaceEvent event) {
        if (stage != Stage.TALL || !(event.world instanceof WorldServer)
            || requested < 0
            || singularity.firstPlacement >= 0) return;
        try {
            singularity.noteFirstPlacement(event.world);
        } catch (Throwable failure) {
            finish(failure);
        }
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
            } else if (stage == Stage.PREPARE_SINGULARITY) {
                if (player.worldObj.getTotalWorldTime() < nextBuildTick) return;
                z += 30;
                setup(player, 1);
                singularity.prepare(player);
                requested = -1;
                stage = Stage.SINGULARITY;
            } else if (stage == Stage.PREPARE_TALL) {
                z += 85;
                machine = ProjectorMachines.supportedIdOf(ItemList.SpaceElevatorController.get(1));
                require(machine > 0, "Space Elevator is supported by the projector");
                singularity = new SingularityScenario(true);
                singularity.prepareElevator(player);
                requested = -1;
                stage = Stage.TALL;
            } else if (stage.singularity()) {
                if (requested >= 0 && singularity.checkWorld(player))
                    stage = stage == Stage.SINGULARITY && tallPreview ? Stage.PREPARE_TALL : Stage.DONE;
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
                    stage = stage == Stage.REVEAL ? Stage.PREPARE_FLIGHT : Stage.PREPARE_SINGULARITY;
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
            if (current.singularity() && singularity.complete && singularity.arrivalPicture)
                singularity.putAwayProjector(mc);
            if (sent == current || mc.theWorld == null || ProjectorClient.plan == null) return;
            if (current.singularity()) {
                singularity.startFromGui(mc, current);
                return;
            }
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
        if (done) return;
        if (event.phase == TickEvent.Phase.START) {
            if (stage.singularity()) singularity.beginRender();
            return;
        }
        captureImpact();
        if (stage == Stage.REVEAL || stage == Stage.FLIGHT) captureSegmented();
        if (stage.singularity()) singularity.capture();
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

    private final class SingularityScenario {

        private static final StructureDeploymentStyle STYLE = StructureDeploymentStyle.SINGULARITY;
        private static final int OPENING_FIRST_TICK = StructureDeploymentStyle.SINGULARITY_CHARGE_TICKS - 2;
        private static final int OPENING_LAST_TICK = StructureDeploymentStyle.SINGULARITY_GROWTH_TICKS + 6;
        private final boolean tall;
        private final String shotPrefix;
        private final List<TargetCell> targets = new ArrayList<>();
        private final List<Long> renderingTimes = new ArrayList<>();
        private final File frames;
        private int anchorY = Y;
        private long renderStarted, previousRenderStarted, previousCaptureTime;
        private long longestOpeningRender, longestOpeningRenderAge, longestOpeningFrameGap, longestOpeningFrameGapAge;
        private long longestOpeningCaptureGap, longestOpeningCaptureGapAge, largestOpeningCaptureTickGap;
        private volatile long startTick = -1, firstPlacement = -1;
        private volatile boolean complete, finalPicture;
        private Vec3d launchEye, launchLook;
        private boolean guiRequested, batchPage, styleSelected, sawStart, stowed;
        private boolean projectilePicture, mergePicture, chargePicture, arcsPicture, collapsePicture, formingPicture,
            arrivalPicture;
        private long lastFrame = Long.MIN_VALUE;
        private int frameCount, projectileFrames, openingFrames;

        private SingularityScenario(boolean tall) {
            this.tall = tall;
            shotPrefix = tall ? "singularity-tall" : "singularity";
            frames = new File(shotPrefix + "-preview");
        }

        private void prepare(EntityPlayerMP player) {
            World world = player.worldObj;
            world.getGameRules()
                .setOrCreateGameRule("doDaylightCycle", "false");
            world.getGameRules()
                .setOrCreateGameRule("doMobSpawning", "false");
            world.setWorldTime(16000);
            world.setBlock(x - 3, Y - 1, z - 2, Blocks.glowstone);
            world.setBlock(x + 6, Y - 1, z - 2, Blocks.glowstone);
            player.rotationYaw = 26.5f;
            player.rotationPitch = 17;
            player.setPositionAndUpdate(x + 9, Y + 7, z - 16);
            var plan = ProjectorPlan.read(player.getHeldItem());
            var shape = ProjectorSimulation.run(plan.settings());
            var layout = new ProjectorBatch.Layout(plan.batch, shape, plan.facing);
            require(layout.solve(), "Singularity fixture batch layout");
            for (int copy = 0; copy < plan.batch.machines(); copy++) {
                var placement = layout.placement(copy);
                prepareTargets(
                    world,
                    shape,
                    x + placement.x(),
                    Y + placement.y(),
                    z + placement.z(),
                    placement.facing());
            }
            require(targets.size() == 68, "Expected two complete 34-block machine footprints");
        }

        private void prepareElevator(EntityPlayerMP player) {
            var plan = new ProjectorPlan();
            plan.selectMachine(machine);
            plan.state = ProjectorPlan.State.LOCKED;
            plan.dimension = player.dimension;
            plan.x = x;
            plan.z = z;
            plan.facing = ExtendedFacing.of(ForgeDirection.NORTH);
            var shape = ProjectorSimulation.run(plan.settings());
            require(
                !shape.isEmpty() && shape.failure.isEmpty() && shape.limit.isEmpty(),
                "Space Elevator structure simulation is complete");
            int[] bounds = shape.worldBounds(plan.facing);
            int width = bounds[3] - bounds[0] + 1, height = bounds[4] - bounds[1] + 1;
            int depth = bounds[5] - bounds[2] + 1;
            require(
                width == 35 && height == 43 && depth == 35,
                "Expected 35x43x35 Space Elevator, got " + width + "x" + height + "x" + depth);
            anchorY = plan.y = Y - bounds[1];
            var world = player.worldObj;
            int minX = x + bounds[0] - 2, minZ = z + bounds[2] - 2;
            int maxX = x + bounds[3] + 2, maxZ = z + bounds[5] + 2;
            for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
                for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) world.getChunkFromChunkCoords(cx, cz);
            }
            for (int bx = minX; bx <= maxX; bx++) {
                for (int bz = minZ; bz <= maxZ; bz++) world.setBlock(bx, Y - 1, bz, Blocks.stone, 0, 2);
            }
            prepareTargets(world, shape, x, anchorY, z, plan.facing);
            var terminal = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
            plan.write(terminal);
            player.inventory.setInventorySlotContents(player.inventory.currentItem, terminal);
            player.inventoryContainer.detectAndSendChanges();

            ProjectorConfig.buildRange = Math.max(ProjectorConfig.buildRange, 128);
            double centerX = x + (bounds[0] + bounds[3] + 1) * .5, centerZ = z + (bounds[2] + bounds[5] + 1) * .5;
            double distance = 86, cameraY = Y + 43, centerY = Y + height * .55;
            player.rotationYaw = 25;
            double yaw = Math.toRadians(player.rotationYaw);
            player.rotationPitch = (float) Math
                .toDegrees(Math.atan2(cameraY + player.getEyeHeight() - centerY, distance));
            player
                .setPositionAndUpdate(centerX + Math.sin(yaw) * distance, cameraY, centerZ - Math.cos(yaw) * distance);
            System.out.println(
                "[projector-singularity-qa] Space Elevator " + machine
                    + " ("
                    + width
                    + "x"
                    + height
                    + "x"
                    + depth
                    + "), "
                    + targets.size()
                    + " expected physical blocks");
        }

        private void prepareTargets(World world, ProjectorStructure shape, int ox, int oy, int oz,
            ExtendedFacing facing) {
            int[] local = new int[3], offset = new int[3];
            for (int i = 0; i < shape.size; i++) {
                if (shape.blocks[i] == Blocks.air) continue;
                local[0] = shape.a[i];
                local[1] = shape.b[i];
                local[2] = shape.c[i];
                facing.getWorldOffset(local, offset);
                int tx = ox + offset[0], ty = oy + offset[1], tz = oz + offset[2];
                if (!world.isAirBlock(tx, ty, tz)) world.setBlockToAir(tx, ty, tz);
                targets.add(new TargetCell(tx, ty, tz, shape.blocks[i], shape.metas[i], shape.machines[i]));
            }
        }

        private void startFromGui(Minecraft mc, Stage current) throws Exception {
            if (!guiRequested) {
                if (ProjectorClient.plan.z != z || ProjectorClient.plan.machine != machine
                    || ProjectorClient.plan.batch.machines() != (tall ? 1 : 2)) return;
                ProjectorClient.open(0);
                guiRequested = true;
                return;
            }
            var screen = ModularScreen.getCurrent();
            if (screen == null || !(screen.getMainPanel() instanceof ProjectorGui gui)) return;
            if (!batchPage) {
                var tabs = (PageTabs) gui.getChildren()
                    .stream()
                    .filter(PageTabs.class::isInstance)
                    .findFirst()
                    .orElseThrow();
                ((ThemedButton<?>) tabs.getChildren()
                    .get(tall ? ProjectorGui.GENERATION : ProjectorGui.BATCH)).onMousePressed(0);
                batchPage = true;
                return;
            }
            if (!styleSelected) {
                var button = visibleButton(gui, "deployment_style");
                if (button == null) return;
                int remaining = StructureDeploymentStyle.values().length;
                while (gui.working.buildStyle != STYLE && remaining-- > 0) button.onMousePressed(0);
                require(gui.working.buildStyle == STYLE, "The visible batch style control did not select Singularity");
                styleSelected = true;
                return;
            }
            if (gui.structure() == null || gui.structure()
                .isEmpty()) return;
            var generate = visibleButton(gui, tall ? "generate_structure" : "generate_batch");
            if (generate == null) return;
            mc.gameSettings.hideGUI = false;
            save(shotPrefix + "-style.png");
            requested = mc.getIntegratedServer()
                .worldServerForDimension(0)
                .getTotalWorldTime();
            sent = current;
            generate.onMousePressed(0);
        }

        private void putAwayProjector(Minecraft mc) {
            if (stowed || mc.thePlayer == null) return;
            var inventory = mc.thePlayer.inventory;
            inventory.currentItem = (inventory.currentItem + 1) % 9;
            stowed = true;
        }

        private boolean checkWorld(EntityPlayerMP player) throws ReflectiveOperationException {
            if (complete) return finalPicture;
            var world = player.worldObj;
            if (startTick < 0) {
                var jobs = (Map<?, ?>) field(ProjectorBuilder.INSTANCE, "jobs");
                var job = jobs.get(player.getUniqueID());
                if (job != null && field(job, "deployment") instanceof StructureDeployment deployment) {
                    launchEye = Vec3d.eye(player);
                    launchLook = Vec3d.look(player);
                    startTick = deployment.startTick();
                }
            }
            if (tall && (!(world.getTileEntity(x, anchorY, z) instanceof IGregTechTileEntity controller)
                || controller.getMetaTileID() != machine)) return false;
            int placed = 0;
            for (TargetCell cell : targets) {
                if (world.isAirBlock(cell.x(), cell.y(), cell.z())) continue;
                placed++;
                require(cell.matches(world), "Singularity placed the wrong block: " + cell);
            }
            if (placed == 0) return false;
            if (tall) require(firstPlacement >= 0, "Space Elevator first placement event was not observed");
            else if (firstPlacement < 0) noteFirstPlacement(world);
            if (placed == targets.size()) {
                require(
                    ProjectorPlan.read(player.getHeldItem()).buildStyle == STYLE,
                    "GUI did not persist the selected build style");
                System.out.println(
                    "[projector-singularity-qa] " + shotPrefix
                        + " first real block at tick "
                        + firstPlacement
                        + "; verified all "
                        + placed
                        + " placed blocks and their machine IDs");
                complete = true;
            }
            return false;
        }

        private void noteFirstPlacement(World world) {
            require(startTick >= 0, "World placement preceded the deployment start");
            firstPlacement = world.getTotalWorldTime() - startTick;
            require(
                firstPlacement >= StructureDeploymentStyle.SINGULARITY_IMPACT_TICK,
                "First real block appeared before the singularity collapsed");
        }

        private void beginRender() {
            previousRenderStarted = renderStarted;
            renderStarted = System.nanoTime();
        }

        private void capture() {
            var mc = Minecraft.getMinecraft();
            if (finalPicture || startTick < 0 || mc.theWorld == null || mc.currentScreen != null) return;
            long age = mc.theWorld.getTotalWorldTime() - startTick;
            int flight = StructureDeploymentStyle.SINGULARITY_LAUNCH_TICKS;
            if (age >= 0 && age < StructureDeploymentStyle.SINGULARITY_IMPACT_TICK && renderStarted > 0) {
                long renderTime = System.nanoTime() - renderStarted;
                renderingTimes.add(renderTime);
                if (age >= OPENING_FIRST_TICK && age <= OPENING_LAST_TICK) {
                    openingFrames++;
                    if (renderTime > longestOpeningRender) {
                        longestOpeningRender = renderTime;
                        longestOpeningRenderAge = age;
                    }
                    long frameGap = renderStarted - previousRenderStarted;
                    if (previousRenderStarted > 0 && frameGap > longestOpeningFrameGap) {
                        longestOpeningFrameGap = frameGap;
                        longestOpeningFrameGapAge = age;
                    }
                }
            }
            boolean collapsing = age >= StructureDeploymentStyle.SINGULARITY_COLLAPSE_TICK
                && age < StructureDeploymentStyle.SINGULARITY_FORM_TICK;
            int frameDelay = tall && age > StructureDeploymentStyle.SINGULARITY_IMPACT_TICK
                + StructureDeploymentStyle.SINGULARITY_AFTERSHOCK_TICKS ? 8 : 2;
            if (collapsing) frameDelay = 1;
            if (age < -flight || lastFrame != Long.MIN_VALUE && age - lastFrame < frameDelay) return;
            try {
                if (!sawStart && age < StructureDeploymentStyle.SINGULARITY_IMPACT_TICK) {
                    var active = (Map<?, ?>) staticField(ProjectorDeploymentRenderer.class, "ACTIVE");
                    for (Object presentation : active.values()) {
                        var effects = field(presentation, "effects");
                        var bounds = (StructureDeployment) field(effects, "bounds");
                        if (bounds.startTick() != startTick || z < bounds.minZ() || z >= bounds.maxZ()) continue;
                        require(field(effects, "style") == STYLE, "Client received the wrong deployment style");
                        var singularity = field(effects, "singularity");
                        checkTwinLaunch(singularity);
                        if (!tall) checkColdShaders(singularity, age);
                        sawStart = true;
                        break;
                    }
                }
                if (!sawStart) return;
                long captureTime = System.nanoTime();
                if (age >= OPENING_FIRST_TICK && age <= OPENING_LAST_TICK && previousCaptureTime > 0) {
                    largestOpeningCaptureTickGap = Math.max(largestOpeningCaptureTickGap, age - lastFrame);
                    long captureGap = captureTime - previousCaptureTime;
                    if (captureGap > longestOpeningCaptureGap) {
                        longestOpeningCaptureGap = captureGap;
                        longestOpeningCaptureGapAge = age;
                    }
                }
                previousCaptureTime = captureTime;
                mc.gameSettings.hideGUI = true;
                frames.mkdirs();
                String name = String
                    .format(java.util.Locale.ROOT, "frame-%03d-tick-%03d.png", frameCount++, age + flight);
                ScreenShotHelper.saveScreenshot(frames, name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                Path frame = frames.toPath()
                    .resolve("screenshots")
                    .resolve(name);
                require(Files.isRegularFile(frame), "Singularity frame screenshot was not saved");
                lastFrame = age;
                if (age < 0) {
                    projectileFrames++;
                    if (!projectilePicture && age >= -flight + 3) {
                        var colors = launchColors(frame);
                        if (colors.ice() >= 35 && colors.violet() >= 35) {
                            copyFrame(frame, shotPrefix + "-projectile.png");
                            System.out.println(
                                "[projector-singularity-qa] both projectile colors visible at " + age
                                    + " (ice "
                                    + colors.ice()
                                    + ", violet "
                                    + colors.violet()
                                    + ")");
                            projectilePicture = true;
                        }
                    }
                }
                if (!mergePicture && age >= 0 && age < 5) {
                    copyFrame(frame, shotPrefix + "-merge.png");
                    mergePicture = true;
                }
                if (!chargePicture && age >= 6 && age < StructureDeploymentStyle.SINGULARITY_CHARGE_TICKS) {
                    copyFrame(frame, shotPrefix + "-charge.png");
                    chargePicture = true;
                }
                if (!arcsPicture && age >= StructureDeploymentStyle.SINGULARITY_GROWTH_TICKS + 4
                    && age < StructureDeploymentStyle.SINGULARITY_COLLAPSE_TICK
                    && (tall || showsPlasmaSurface(frame))) {
                    copyFrame(frame, shotPrefix + "-arcs.png");
                    arcsPicture = true;
                }
                if (!collapsePicture && collapsing
                    && age >= StructureDeploymentStyle.SINGULARITY_FORM_TICK - 2
                    && (tall || showsCollapseOverlap(frame, age))) {
                    copyFrame(frame, shotPrefix + "-collapse.png");
                    collapsePicture = true;
                }
                if (!formingPicture && age >= StructureDeploymentStyle.SINGULARITY_IMPACT_TICK - 10
                    && age < StructureDeploymentStyle.SINGULARITY_IMPACT_TICK) {
                    copyFrame(frame, shotPrefix + "-forming.png");
                    formingPicture = true;
                }
                if (!arrivalPicture && firstPlacement >= 0 && age >= firstPlacement + 3) {
                    copyFrame(frame, shotPrefix + "-arrival.png");
                    arrivalPicture = true;
                }
                if (!complete || age < firstPlacement + StructureDeploymentStyle.SINGULARITY_AFTERSHOCK_TICKS + 6)
                    return;
                require(
                    projectilePicture && projectileFrames >= 3
                        && mergePicture
                        && chargePicture
                        && arcsPicture
                        && collapsePicture
                        && formingPicture
                        && arrivalPicture
                        && frameCount >= 20,
                    "Singularity phase captures or animation frames are incomplete");
                require(
                    targets.stream()
                        .allMatch(cell -> cell.matches(mc.theWorld)),
                    "Final client blocks did not match the server result");
                require(openingFrames > 0, "No rendering timing samples for the singularity opening");
                System.out.printf(
                    java.util.Locale.ROOT,
                    "[projector-singularity-qa] %s opening ticks %d-%d, PID %d: max render %.2f ms at %d, "
                        + "max frame gap %.2f ms at %d, max capture gap %.2f ms at %d, max capture tick gap %d (%d frames)%n",
                    shotPrefix,
                    OPENING_FIRST_TICK,
                    OPENING_LAST_TICK,
                    ProcessHandle.current()
                        .pid(),
                    longestOpeningRender / 1_000_000.0,
                    longestOpeningRenderAge,
                    longestOpeningFrameGap / 1_000_000.0,
                    longestOpeningFrameGapAge,
                    longestOpeningCaptureGap / 1_000_000.0,
                    longestOpeningCaptureGapAge,
                    largestOpeningCaptureTickGap,
                    openingFrames);
                copyFrame(frame, shotPrefix + "-final.png");
                if (!renderingTimes.isEmpty()) {
                    renderingTimes.sort(Long::compareTo);
                    int samples = renderingTimes.size();
                    System.out.printf(
                        java.util.Locale.ROOT,
                        "[projector-singularity-qa] %s render callback p50 %.2f ms p95 %.2f ms (%d samples)%n",
                        shotPrefix,
                        renderingTimes.get(samples / 2) / 1_000_000.0,
                        renderingTimes.get((samples - 1) * 95 / 100) / 1_000_000.0,
                        samples);
                }
                finalPicture = true;
            } catch (Throwable failure) {
                finish(failure);
            }
        }

        private boolean showsPlasmaSurface(Path frame) throws java.io.IOException {
            var image = ImageIO.read(frame.toFile());
            int width = image.getWidth(), height = image.getHeight();
            int surfacePixels = 0, brightPixels = 0;
            for (int y = height * 43 / 100; y < height * 50 / 100; y++) {
                for (int x = width * 45 / 100; x < width * 49 / 100; x++) {
                    int rgb = image.getRGB(x, y);
                    int red = rgb >> 16 & 255, green = rgb >> 8 & 255, blue = rgb & 255;
                    if (isPlasmaColor(red, green, blue)) surfacePixels++;
                    if (blue >= 210 && Math.max(red, green) >= 175) brightPixels++;
                }
            }
            System.out.printf(
                java.util.Locale.ROOT,
                "[projector-singularity-qa] open plasma surface: %d saturated pixels, %d bright detail pixels%n",
                surfacePixels,
                brightPixels);
            return surfacePixels >= 350 && brightPixels >= 200;
        }

        private boolean showsCollapseOverlap(Path frame, long age) throws java.io.IOException {
            var image = ImageIO.read(frame.toFile());
            int width = image.getWidth(), height = image.getHeight();
            int plasmaPixels = 0, linedRows = 0;
            int left = width * 39 / 100, right = width * 57 / 100;
            for (int y = height * 27 / 100; y < height * 63 / 100; y++) {
                int[] brightParts = new int[4];
                for (int x = left; x < right; x++) {
                    int rgb = image.getRGB(x, y);
                    int red = rgb >> 16 & 255, green = rgb >> 8 & 255, blue = rgb & 255;
                    if (x >= width * 45 / 100 && x < width * 49 / 100
                        && y >= height * 43 / 100
                        && y < height * 50 / 100
                        && isPlasmaColor(red, green, blue)) plasmaPixels++;
                    if (y >= height * 51 / 100 && red >= 105
                        && green >= 150
                        && blue >= 177
                        && green >= red - 6
                        && blue >= green) brightParts[(x - left) * brightParts.length / (right - left)]++;
                }
                int filled = 0;
                for (int count : brightParts) if (count >= 3) filled++;
                if (filled >= 3) linedRows++;
            }
            System.out.println(
                "[projector-singularity-qa] collapse frame " + age
                    + ": "
                    + plasmaPixels
                    + " saturated plasma pixels, "
                    + linedRows
                    + " blue-white structure rows");
            // The merged voxel edges are much sparser than the tessellated mesh wireframe.
            return plasmaPixels >= 475 && linedRows >= 14;
        }

        private static boolean isPlasmaColor(int red, int green, int blue) {
            return blue >= 105 && blue - green >= 30 && blue - red >= 20;
        }

        private void checkColdShaders(Object effect, long age) throws ReflectiveOperationException {
            String uniform = System.getProperty("gtnr.projector.coldShaderUniform");
            if (uniform == null) return;
            require(age < OPENING_FIRST_TICK, "Cold shader QA did not observe the first deployment before expansion");
            var plasmaShader = staticField(effect.getClass(), "PLASMA_SURFACE");
            var refractionShader = staticField(field(effect, "refraction").getClass(), "SHADER");
            var plasmaProgram = (GlslProgram) field(plasmaShader, "program");
            var refractionProgram = (GlslProgram) field(refractionShader, "program");
            require(
                plasmaProgram != null && plasmaProgram.uniform(uniform) >= 0
                    && refractionProgram != null
                    && refractionProgram.uniform(uniform) >= 0,
                "Cold shader variants were not both linked before the first expansion");
            System.out
                .println("[projector-singularity-qa] both cold shader variants linked before opening at tick " + age);
        }

        private void checkTwinLaunch(Object effect) throws ReflectiveOperationException {
            var right = (Vec3d) field(effect, "launchOrigin");
            var left = (Vec3d) field(effect, "leftLaunchOrigin");
            require(right != null && left != null, "Singularity start did not include both launch origins");
            Vec3d rightHand = right.subtract(launchEye), leftHand = left.subtract(launchEye);
            Vec3d handAxis = Vec3d.rightOf(launchLook);
            require(
                rightHand.length() < 1.1 && leftHand.length() < 1.1
                    && rightHand.y() < 0
                    && leftHand.y() < 0
                    && rightHand.dot(launchLook) > .2
                    && leftHand.dot(launchLook) > .2
                    && rightHand.dot(handAxis) > .1
                    && leftHand.dot(handAxis) < -.1
                    && Math.abs(rightHand.dot(handAxis) + leftHand.dot(handAxis)) < .01,
                "Launch starts symmetrically in front of the player's hands");
            var flight = field(effect, "flight");
            var position = flight.getClass()
                .getDeclaredMethod("position", double.class, int.class);
            position.setAccessible(true);
            require(
                right.equals(position.invoke(flight, 0.0, 1)) && left.equals(position.invoke(flight, 0.0, -1)),
                "Projectile paths start at their hand origins");
            Vec3d rightHalfway = (Vec3d) position.invoke(flight, .46, 1);
            Vec3d leftHalfway = (Vec3d) position.invoke(flight, .46, -1);
            require(
                rightHalfway.subtract(leftHalfway)
                    .dot(handAxis) < -.1,
                "Twin projectiles orbit across their axis");
            Object center = field(effect, "center");
            require(
                center.equals(position.invoke(flight, 1.0, 1)) && center.equals(position.invoke(flight, 1.0, -1)),
                "Both projectiles reach the same portal center");
        }

        private LaunchColors launchColors(Path frame) throws java.io.IOException {
            var image = ImageIO.read(frame.toFile());
            int width = image.getWidth(), height = image.getHeight(), ice = 0, violet = 0;
            int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
            for (int rgb : pixels) {
                int red = rgb >> 16 & 255, green = rgb >> 8 & 255, blue = rgb & 255;
                if (blue >= 175 && green >= 125 && green - red >= 24 && blue - green >= 10) ice++;
                if (red >= 95 && blue >= 150 && red - green >= 18 && blue - green >= 40) violet++;
            }
            return new LaunchColors(ice, violet);
        }

        private record LaunchColors(int ice, int violet) {}

        private void copyFrame(Path frame, String name) throws java.io.IOException {
            Files.copy(frame, Path.of("screenshots", name), StandardCopyOption.REPLACE_EXISTING);
        }

        private AE2Button visibleButton(IWidget widget, String name) {
            if (widget instanceof AE2Button button && name.equals(widget.getName()) && button.areAncestorsEnabled())
                return button;
            for (IWidget child : widget.getChildren()) {
                AE2Button button = visibleButton(child, name);
                if (button != null) return button;
            }
            return null;
        }
    }

    private record TargetCell(int x, int y, int z, Block block, int meta, int machine) {

        boolean matches(World world) {
            return world.getBlock(x, y, z) == block && world.getBlockMetadata(x, y, z) == meta
                && (machine == 0 || world.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile
                    && tile.getMetaTileID() == machine);
        }
    }

    private static Object staticField(Class<?> owner, String name) throws ReflectiveOperationException {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
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
