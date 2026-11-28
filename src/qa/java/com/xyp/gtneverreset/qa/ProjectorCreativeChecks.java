package com.xyp.gtneverreset.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.client.projector.ProjectorClient;
import com.xyp.gtneverreset.client.projector.ProjectorGui;
import com.xyp.gtneverreset.common.packet.GenerateProjectorBatchPacket;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.projector.ProjectorBatch;
import com.xyp.gtneverreset.common.projector.ProjectorBatchCells;
import com.xyp.gtneverreset.common.projector.ProjectorEnergyHatches;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtnrlib.gui.widget.AE2Button;
import com.xyp.gtnrlib.gui.widget.NumberEntryWidget;
import com.xyp.gtnrlib.gui.widget.PageTabs;
import com.xyp.gtnrlib.gui.widget.ThemedButton;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

@Mod(
    modid = "gtnrcreativeprojectorqa",
    name = "Creative projector checks",
    version = "1",
    dependencies = "after:gtneverreset")
public class ProjectorCreativeChecks {

    private static final int SEPARATE_GAP = 4;
    private static final int BUILD_REQUEST_INTERVAL = 41;
    private boolean launched, opening, readyToClick, armed, worldPreviewChecked;
    private int worldPreviewFrames, captureFrames, smartWorldFrames;
    private boolean smartProjectionChecked;
    private volatile boolean done, shortage;
    private volatile long serverTicks;
    private long nextBuildRequest;

    private enum Stage {
        SETUP,
        SURVIVAL,
        CHECK_DENIAL,
        OBSTRUCTED,
        WAIT_OBSTRUCTION,
        REMOVE_OBSTRUCTION,
        PROTECTED,
        WAIT_PROTECTION,
        VERIFY_ROLLBACK,
        CREATIVE,
        WAIT_GENERATION,
        VERIFY_WORLD,
        INVALID_BATCH,
        BATCH,
        WAIT_BATCH,
        VERIFY_BATCH,
        SEPARATE_BATCH,
        WAIT_SEPARATE,
        VERIFY_SEPARATE,
        SMART,
        WAIT_SMART,
        VERIFY_SMART,
        CAPTURE_WORLD
    }

    private volatile Stage stage = Stage.SETUP;
    private int deniedX, deniedY, deniedZ;
    private boolean deniedPlacement;
    private int wait, ticks, x, y, z;
    private ProjectorStructure expected;
    private int batchPlaced;
    private ExtendedFacing facing;
    private int replacement = -1, hatchCell, navigationFrames;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent e) {
        if (done || e.phase != TickEvent.Phase.END) return;
        serverTicks++;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            if (stage == Stage.SETUP) {
                player.setGameType(WorldSettings.GameType.SURVIVAL);
                var item = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
                var plan = new ProjectorPlan();
                plan.selectMachine(ProjectorMachines.idOf(ItemList.ExothermicHearth.get(1)));
                plan.state = ProjectorPlan.State.LOCKED;
                plan.dimension = player.dimension;
                plan.x = x = (int) player.posX + 48;
                plan.y = y = 16;
                plan.z = z = (int) player.posZ + 12;
                plan.facing = facing = ExtendedFacing.of(ForgeDirection.EAST);
                plan.batch.setAdditional(ForgeDirection.UP, 1);
                plan.write(item);
                player.inventory.setInventorySlotContents(player.inventory.currentItem, item);
                player.inventoryContainer.detectAndSendChanges();
                stage = Stage.SURVIVAL;
            } else if (stage == Stage.CHECK_DENIAL && shortage && ++wait >= 5) {
                require(player.worldObj.isAirBlock(x, y, z), "survival wrote to the world");
                require(!player.capabilities.isCreativeMode, "survival privilege check");
                player.setGameType(WorldSettings.GameType.CREATIVE);
                player.worldObj.setBlock(x, y, z, Blocks.bedrock);
                stage = Stage.OBSTRUCTED;
            } else if (stage == Stage.REMOVE_OBSTRUCTION) {
                require(player.worldObj.getBlock(x, y, z) == Blocks.bedrock, "unbreakable obstruction was cleared");
                require(batchPlaced > 0, "an unbreakable obstruction stopped unaffected placements");
                clearStructure(player);
                stage = Stage.PROTECTED;
            } else if (stage == Stage.VERIFY_ROLLBACK) {
                require(
                    deniedPlacement && player.worldObj.isAirBlock(deniedX, deniedY, deniedZ),
                    "protected placement did not roll back");
                require(batchPlaced > 0, "a denied placement stopped unaffected placements");
                clearStructure(player);
                stage = Stage.CREATIVE;
            } else if (stage == Stage.VERIFY_WORLD) {
                int[] offset = new int[3];
                for (int i = 0; i < expected.size; i++) {
                    facing.getWorldOffset(new int[] { expected.a[i], expected.b[i], expected.c[i] }, offset);
                    int wx = x + offset[0], wy = y + offset[1], wz = z + offset[2];
                    require(player.worldObj.getBlock(wx, wy, wz) == expected.blocks[i], "block at " + i);
                    if (expected.machines[i] > 0) {
                        var tile = (IGregTechTileEntity) player.worldObj.getTileEntity(wx, wy, wz);
                        require(tile.getMetaTileID() == expected.machines[i], "GT variant at " + i);
                    } else if (expected.metas[i] != ProjectorStructure.ANY_META) {
                        require(player.worldObj.getBlockMetadata(wx, wy, wz) == expected.metas[i], "metadata at " + i);
                    }
                }
                require(expected.machines[hatchCell] == replacement, "selected hatch");
                var gt = (IGregTechTileEntity) player.worldObj.getTileEntity(x, y, z);
                require(((IAlignment) gt.getMetaTileEntity()).getExtendedFacing() == facing, "controller orientation");
                Files.writeString(
                    Path.of("generation-report.txt"),
                    "Verified " + expected.size
                        + " cells, selected hatch "
                        + replacement
                        + " and controller "
                        + facing);
                var next = new ProjectorPlan();
                next.selectMachine(ProjectorMachines.idOf(ItemList.Machine_Multi_BlastFurnace.get(1)));
                next.state = ProjectorPlan.State.LOCKED;
                next.dimension = player.dimension;
                next.x = x = (int) player.posX + 1;
                next.y = y = 9;
                next.z = z = (int) player.posZ + 14;
                next.facing = facing = ExtendedFacing.of(ForgeDirection.NORTH);
                next.write(player.getHeldItem());
                player.inventoryContainer.detectAndSendChanges();
                stage = Stage.INVALID_BATCH;
            } else if (stage == Stage.VERIFY_BATCH || stage == Stage.VERIFY_SEPARATE) {
                boolean separate = stage == Stage.VERIFY_SEPARATE;
                verifyTranslatedWorld(player);
                if (separate) {
                    require(batchPlaced == 102, "three independent 34-block structures");
                    int eastEdge = x + expected.worldBounds(facing)[3];
                    for (int gap = 1; gap <= SEPARATE_GAP; gap++) {
                        require(player.worldObj.isAirBlock(eastEdge + gap, y, z), "gap filled during construction");
                    }
                    verifyGapLayout(expected, facing);
                    Files.writeString(
                        Path.of("separate-report.txt"),
                        "Four-block gap between copies: 3 controllers, 102 unique blocks.");
                    var layoutPlan = new ProjectorPlan();
                    layoutPlan.selectMachine(ProjectorMachines.idOf(ItemList.Machine_Multi_BlastFurnace.get(1)));
                    layoutPlan.state = ProjectorPlan.State.LOCKED;
                    layoutPlan.dimension = player.dimension;
                    layoutPlan.x = x = (int) player.posX + 2;
                    layoutPlan.y = y = 18;
                    layoutPlan.z = z = (int) player.posZ + 21;
                    layoutPlan.facing = facing = ExtendedFacing.of(ForgeDirection.NORTH);
                    for (ForgeDirection side : new ForgeDirection[] { ForgeDirection.WEST, ForgeDirection.SOUTH,
                        ForgeDirection.UP }) {
                        layoutPlan.batch.setGap(side, side == ForgeDirection.WEST ? -1 : 1);
                        layoutPlan.batch.setAdditional(side, 1);
                    }
                    layoutPlan.batchProjection = true;
                    layoutPlan.write(player.getHeldItem());
                    player.inventoryContainer.detectAndSendChanges();
                    player.capabilities.isFlying = true;
                    player.sendPlayerAbilities();
                    player.setPositionAndUpdate(x - 1, y + 1, z - 11);
                    stage = Stage.SMART;
                    return;
                }
                Files.writeString(
                    Path.of("batch-report.txt"),
                    "36 machines in 4 × 3 × 3, with X overlaps; verified " + batchPlaced + " unique placed blocks.");
                var separatePlan = ProjectorPlan.read(player.getHeldItem());
                for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                    separatePlan.batch.setAdditional(dir, dir == ForgeDirection.EAST ? 2 : 0);
                    separatePlan.batch.setGap(dir, 0);
                }
                separatePlan.batch.setGap(ForgeDirection.EAST, SEPARATE_GAP);
                separatePlan.y = y += 20;
                separatePlan.state = ProjectorPlan.State.LOCKED;
                separatePlan.write(player.getHeldItem());
                player.inventoryContainer.detectAndSendChanges();
                stage = Stage.SEPARATE_BATCH;
            } else if (stage == Stage.VERIFY_SMART) {
                verifyTranslatedWorld(player);
                Files.writeString(
                    Path.of("translated-report.txt"),
                    batchPlaced + " unique blocks built with one facing.");
                player.inventory.currentItem = 1;
                player.playerNetServerHandler
                    .sendPacket(new net.minecraft.network.play.server.S09PacketHeldItemChange(1));
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                player.setPositionAndUpdate(x - 1, y + 1, z - 11);
                stage = Stage.CAPTURE_WORLD;
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getMinecraft();
        if (done) {
            mc.shutdown();
            return;
        }
        try {
            if (++ticks > 5400) throw new AssertionError("timeout at stage " + stage);
            if (!launched && mc.currentScreen != null
                && (mc.currentScreen instanceof GuiMainMenu || mc.currentScreen.getClass()
                    .getSimpleName()
                    .equals("GSGuiMainMenu"))) {
                launched = true;
                mc.launchIntegratedServer(
                    "projector-generation-" + System.currentTimeMillis(),
                    "Projector QA",
                    new WorldSettings(4L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT)
                        .enableCommands());
            }
            if (stage == Stage.SEPARATE_BATCH && ProjectorClient.plan != null && ProjectorClient.plan.y == y) {
                if (serverTicks < nextBuildRequest) return;
                require(ProjectorClient.plan.batch.gap(ForgeDirection.WEST) == SEPARATE_GAP, "Gap did not synchronize");
                stage = Stage.WAIT_SEPARATE;
                NetWorkHandler.sendToServer(new GenerateProjectorBatchPacket(0));
                return;
            }
            if (mc.thePlayer == null || ProjectorClient.plan == null
                || (stage != Stage.SURVIVAL && stage != Stage.OBSTRUCTED
                    && stage != Stage.CREATIVE
                    && stage != Stage.PROTECTED
                    && stage != Stage.INVALID_BATCH
                    && stage != Stage.BATCH
                    && stage != Stage.SMART))
                return;
            if ((stage != Stage.SURVIVAL) != mc.thePlayer.capabilities.isCreativeMode) return;
            boolean batch = stage == Stage.INVALID_BATCH || stage == Stage.BATCH || stage == Stage.SMART;
            if (batch
                && ProjectorClient.plan.machine != ProjectorMachines.idOf(ItemList.Machine_Multi_BlastFurnace.get(1)))
                return;
            if (worldPreviewFrames > 0 || smartWorldFrames > 0) return;
            var screen = ModularScreen.getCurrent();
            if (screen == null) {
                if (!opening && mc.currentScreen == null) {
                    ProjectorClient.open();
                    opening = true;
                }
                return;
            }
            if (!(screen.getMainPanel() instanceof ProjectorGui gui)) return;
            var shape = gui.structure();
            if (shape == null || !shape.settings.equals(gui.working.settings())) return;
            require(batch ? shape.size == 34 : shape.size >= 6000, "preview size");
            if (stage == Stage.CREATIVE && !armed) {
                for (int i = 0; i < shape.size && replacement < 0; i++) {
                    var slot = shape.slotAt(i);
                    if (slot == null) continue;
                    for (int id : slot.candidates()) if (id != shape.machines[i]
                        && ProjectorEnergyHatches.flowOf(id) == ProjectorEnergyHatches.Flow.INPUT) {
                            replacement = id;
                            hatchCell = i;
                            gui.working.overrides.put(ProjectorPlan.pack(shape.a[i], shape.b[i], shape.c[i]), id);
                            break;
                        }
                }
                require(replacement > 0, "energy hatch candidate");
                armed = true;
                return;
            }
            expected = shape;
            readyToClick = true;
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent e) {
        if (done || e.phase != TickEvent.Phase.END) return;
        if (smartWorldFrames > 0) {
            try {
                var mc = Minecraft.getMinecraft();
                mc.thePlayer.rotationYaw = 0;
                mc.thePlayer.rotationPitch = -5;
                if (++smartWorldFrames < 40) return;
                require(
                    ProjectorClient.CHECK.structure.size > expected.size
                        && ProjectorClient.CHECK.structure.size < 8 * expected.size,
                    "world projection for eight overlapping machines");
                screenshot("smart-world-projection.png");
                smartWorldFrames = 0;
                smartProjectionChecked = true;
            } catch (Throwable failure) {
                finish(failure);
            }
            return;
        }
        if (stage == Stage.CAPTURE_WORLD) {
            try {
                var mc = Minecraft.getMinecraft();
                mc.thePlayer.rotationYaw = 0;
                mc.thePlayer.rotationPitch = -5;
                if (++captureFrames < 40) return;
                screenshot("smart-generated-world.png");
                finish(null);
            } catch (Throwable failure) {
                finish(failure);
            }
            return;
        }
        if (worldPreviewFrames > 0) {
            try {
                if (++worldPreviewFrames < 30) return;
                require(ModularScreen.getCurrent() == null, "world preview test still in GUI");
                verifyWorldPreview(56, 5);
                screenshot("shared-east-world.png");
                worldPreviewFrames = 0;
                worldPreviewChecked = true;
            } catch (Throwable failure) {
                finish(failure);
            }
            return;
        }
        if (!readyToClick) return;
        readyToClick = false;
        try {
            var gui = (ProjectorGui) ModularScreen.getCurrent()
                .getMainPanel();
            var tabs = (PageTabs) gui.getChildren()
                .stream()
                .filter(PageTabs.class::isInstance)
                .findFirst()
                .orElseThrow();
            require(
                tabs.getChildren()
                    .size() == 6,
                "generation tab visible");
            boolean batch = stage == Stage.INVALID_BATCH || stage == Stage.BATCH || stage == Stage.SMART;
            AE2Button button = WidgetTree
                .findFirstWithName(gui, batch ? "generate_batch" : "generate_structure", AE2Button.class);
            if (navigationFrames++ == 0) {
                ((ThemedButton<?>) tabs.getChildren()
                    .get(ProjectorGui.MATERIALS)).onMousePressed(0);
                return;
            }
            if (navigationFrames == 5) {
                require(!button.areAncestorsEnabled(), "generation action visible on the materials page");
                ((ThemedButton<?>) tabs.getChildren()
                    .get(batch ? ProjectorGui.BATCH : ProjectorGui.GENERATION)).onMousePressed(0);
                require(ModularScreen.getCurrent() != null, "tab click closed the screen");
            }
            if (stage == Stage.SURVIVAL && navigationFrames == 8) {
                ((ThemedButton<?>) tabs.getChildren()
                    .get(ProjectorGui.BATCH)).onMousePressed(0);
            }
            if (stage == Stage.SURVIVAL && navigationFrames == 12) {
                var mass = WidgetTree.findFirstWithName(gui, "generate_batch", AE2Button.class);
                require(mass.areAncestorsEnabled(), "survival batch page is visible");
                ((ThemedButton<?>) tabs.getChildren()
                    .get(ProjectorGui.GENERATION)).onMousePressed(0);
            }
            if (stage == Stage.INVALID_BATCH && !worldPreviewChecked && navigationFrames >= 10) {
                if (navigationFrames == 10) {
                    setBatchNumber(gui, "batch_count_", ForgeDirection.EAST, 2);
                } else if (navigationFrames == 26 || navigationFrames == 44) {
                    boolean shared = navigationFrames == 44;
                    int size = shared ? 56 : 68, width = shared ? 5 : 6;
                    var previewField = ProjectorGui.class.getDeclaredField("preview");
                    previewField.setAccessible(true);
                    var preview = previewField.get(gui);
                    var geometryField = preview.getClass()
                        .getDeclaredField("structure");
                    geometryField.setAccessible(true);
                    var shown = (ProjectorStructure) geometryField.get(preview);
                    require(
                        shown.size == size
                            && shown.worldBounds(gui.working.facing)[3] - shown.worldBounds(gui.working.facing)[0] + 1
                                == width,
                        "GUI batch geometry");
                    require(
                        shown.materials.stream()
                            .mapToInt(ProjectorStructure.Material::count)
                            .sum() == size,
                        "deduplicated material count");
                    verifyWorldPreview(size, width);
                    screenshot(shared ? "shared-east-gui.png" : "separate-east-gui.png");
                    if (!shared) {
                        setBatchNumber(gui, "batch_gap_", ForgeDirection.EAST, -1);
                    } else {
                        var player = Minecraft.getMinecraft().thePlayer;
                        double dx = x - player.posX, dz = z - player.posZ;
                        player.rotationYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                        player.rotationPitch = (float) -Math
                            .toDegrees(Math.atan2(y + 2 - player.posY - player.getEyeHeight(), Math.hypot(dx, dz)));
                        gui.getScreen()
                            .close();
                        navigationFrames = 0;
                        opening = false;
                        worldPreviewFrames = 1;
                    }
                }
                return;
            }
            if (stage == Stage.INVALID_BATCH && navigationFrames == 12) {
                setBatchNumber(gui, "batch_count_", ForgeDirection.EAST, ProjectorBatch.MAX_COPIES);
                setBatchNumber(gui, "batch_count_", ForgeDirection.UP, -2);
                require(!gui.working.batch.withinLimit(), "Fixture did not exceed the copy limit");
                button.onMousePressed(0);
                require(ModularScreen.getCurrent() == gui.getScreen(), "Oversized batch was allowed to generate");
                for (var direction : ForgeDirection.VALID_DIRECTIONS) gui.working.batch.setAdditional(direction, 1);
                gui.working.batch.setAdditional(ForgeDirection.EAST, 2);
                require(gui.working.batch.machines() == 36, "Mixed signs were not retained");
                stage = Stage.BATCH;
                return;
            }
            if (stage == Stage.SMART && navigationFrames == 17 && !smartProjectionChecked) {
                gui.getScreen()
                    .close();
                smartWorldFrames = 1;
                navigationFrames = 0;
                opening = false;
                return;
            }
            if (navigationFrames < 18 || serverTicks < nextBuildRequest) return;
            require(button.areAncestorsEnabled(), "generation page did not expose the action");
            navigationFrames = 0;
            var mc = Minecraft.getMinecraft();
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "generate-" + stage + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            Stage before = stage;
            stage = switch (before) {
                case SURVIVAL -> Stage.CHECK_DENIAL;
                case PROTECTED -> Stage.WAIT_PROTECTION;
                case OBSTRUCTED -> Stage.WAIT_OBSTRUCTION;
                case CREATIVE -> Stage.WAIT_GENERATION;
                case BATCH -> Stage.WAIT_BATCH;
                case SMART -> Stage.WAIT_SMART;
                default -> throw new IllegalStateException("unexpected generator click at " + before);
            };
            button.onMousePressed(0);
            opening = false;
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void place(BlockEvent.PlaceEvent event) {
        if (!(event.world instanceof net.minecraft.world.WorldServer) || !(event.player instanceof EntityPlayerMP))
            return;
        if (stage == Stage.WAIT_PROTECTION && !event.world.isRemote && !deniedPlacement) {
            deniedX = event.x;
            deniedY = event.y;
            deniedZ = event.z;
            deniedPlacement = true;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void message(ClientChatReceivedEvent event) {
        if (!(event.message instanceof ChatComponentTranslation message)) return;
        if (stage == Stage.CHECK_DENIAL && message.getKey()
            .equals("gtneverreset.projector.build.missing")) shortage = true;
        if (message.getKey()
            .equals("gtneverreset.projector.build.started")) {
            if (navigationFrames > 0) finish(new AssertionError("switching pages started a build"));
            // Leave a tick for the ordering of the fixture and builder callbacks.
            nextBuildRequest = serverTicks + BUILD_REQUEST_INTERVAL;
            return;
        }
        if (message.getKey()
            .equals("gtneverreset.projector.build.busy")) {
            finish(new AssertionError("server rejected the build request during " + stage));
            return;
        }
        if ((stage == Stage.WAIT_BATCH || stage == Stage.WAIT_SEPARATE || stage == Stage.WAIT_SMART) && message.getKey()
            .equals("gtneverreset.projector.build.done")) {
            batchPlaced = Integer.parseInt(String.valueOf(message.getFormatArgs()[0]));
            stage = stage == Stage.WAIT_SMART ? Stage.VERIFY_SMART
                : stage == Stage.WAIT_SEPARATE ? Stage.VERIFY_SEPARATE : Stage.VERIFY_BATCH;
            return;
        }
        if ((stage == Stage.WAIT_BATCH || stage == Stage.WAIT_SEPARATE || stage == Stage.WAIT_SMART) && message.getKey()
            .startsWith("gtneverreset.projector.build.")) {
            finish(new AssertionError(message.getUnformattedText()));
            return;
        }
        if ((stage == Stage.WAIT_OBSTRUCTION || stage == Stage.WAIT_PROTECTION) && message.getKey()
            .equals("gtneverreset.projector.build.done")) {
            batchPlaced = Integer.parseInt(String.valueOf(message.getFormatArgs()[0]));
            stage = stage == Stage.WAIT_PROTECTION ? Stage.VERIFY_ROLLBACK : Stage.REMOVE_OBSTRUCTION;
        } else if (stage == Stage.WAIT_GENERATION && message.getKey()
            .equals("gtneverreset.projector.build.done")) stage = Stage.VERIFY_WORLD;
        else if (stage == Stage.WAIT_GENERATION && (message.getKey()
            .equals("gtneverreset.projector.build.stopped")
            || message.getKey()
                .equals("gtneverreset.projector.build.incomplete")))
            finish(new AssertionError(message.getUnformattedText()));
    }

    private void clearStructure(EntityPlayerMP player) {
        int[] offset = new int[3];
        boolean updates = gregtech.api.threads.RunnableMachineUpdate.isCurrentThreadEnabled();
        gregtech.api.threads.RunnableMachineUpdate.setCurrentThreadEnabled(false);
        try {
            for (int i = 0; i < expected.size; i++) {
                facing.getWorldOffset(new int[] { expected.a[i], expected.b[i], expected.c[i] }, offset);
                player.worldObj.setBlockToAir(x + offset[0], y + offset[1], z + offset[2]);
            }
        } finally {
            gregtech.api.threads.RunnableMachineUpdate.setCurrentThreadEnabled(updates);
        }
    }

    private void verifyTranslatedWorld(EntityPlayerMP player) {
        var plan = ProjectorPlan.read(player.getHeldItem());
        var template = expected;
        var layout = new ProjectorBatch.Layout(plan, template, facing);
        var translated = ProjectorBatchCells.collect(template, plan, facing, layout, null);
        require(translated.compatible(), "Translated array layout rejected");
        int[] pos = new int[3];
        int[][] source = template.simulationPositions();
        int unique = 0;
        for (var entry : translated.cells()
            .long2IntEntrySet()) {
            int cell = entry.getIntValue() % template.size;
            if (template.blocks[cell] == Blocks.air) continue;
            long p = entry.getLongKey();
            facing.getWorldOffset(
                new int[] { ProjectorPlan.unpackA(p), ProjectorPlan.unpackB(p), ProjectorPlan.unpackC(p) },
                pos);
            int wx = x + pos[0], wy = y + pos[1], wz = z + pos[2];
            require(player.worldObj.getBlock(wx, wy, wz) == template.blocks[cell], "Wrong batch block");
            require(
                player.worldObj.getBlockMetadata(wx, wy, wz)
                    == template.world.getBlockMetadata(source[0][cell], source[1][cell], source[2][cell]),
                "Wrong batch metadata");
            if (template.machines[cell] != 0) {
                var tile = (IGregTechTileEntity) player.worldObj.getTileEntity(wx, wy, wz);
                require(tile.getMetaTileID() == template.machines[cell], "Wrong batch machine");
                if (template.a[cell] == 0 && template.b[cell] == 0 && template.c[cell] == 0) require(
                    ((IAlignment) tile.getMetaTileEntity()).getExtendedFacing() == facing,
                    "Batch copy changed facing");
            }
            unique++;
        }
        require(batchPlaced == unique, "Each translated solid must be placed once");
    }

    private static void setBatchNumber(ProjectorGui gui, String prefix, ForgeDirection axis, int value) {
        var input = WidgetTree.findFirstWithName(gui, prefix + axis.ordinal(), NumberEntryWidget.class);
        var field = (TextFieldWidget) input.getChildren()
            .stream()
            .filter(TextFieldWidget.class::isInstance)
            .findFirst()
            .orElseThrow();
        field.setText(Integer.toString(value));
        field.onRemoveFocus(
            gui.getScreen()
                .getContext());
    }

    private static void verifyGapLayout(ProjectorStructure shape, ExtendedFacing facing) {
        var plan = new ProjectorPlan();
        var sides = new ForgeDirection[] { ForgeDirection.EAST, ForgeDirection.UP, ForgeDirection.SOUTH };
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            plan.batch.setAdditional(side, 1);
            plan.batch.setGap(side, -1);
        }
        long legacyKey = plan.batch.arrangementKey();
        var legacy = plan.writeToNBT();
        var legacyBatch = legacy.getCompoundTag("Batch");
        legacyBatch.removeTag("Gaps");
        legacyBatch.setInteger("Shared", 63);
        var shared = new ProjectorBatch.Layout(plan.batch, shape, facing);
        int[] sharedFar = new int[3];
        shared.copyOffset(plan.batch.machines() - 1, sharedFar);

        int[] gaps = { 3, 5, ProjectorBatch.MAX_GAP };
        for (int axis = 0; axis < sides.length; axis++) {
            long before = plan.batch.arrangementKey();
            plan.batch.setGap(sides[axis], gaps[axis]);
            require(plan.batch.arrangementKey() != before, "gap omitted from preview cache key");
        }
        ProjectorPlan restored = plan.copy();
        require(restored.batch.arrangementKey() == plan.batch.arrangementKey(), "gap plan did not round trip");
        var spaced = new ProjectorBatch.Layout(restored.batch, shape, facing);
        int[] first = new int[3], last = new int[3];
        spaced.copyOffset(0, first);
        spaced.copyOffset(restored.batch.machines() - 1, last);
        for (int axis = 0; axis < sides.length; axis++) {
            ForgeDirection opposite = sides[axis].getOpposite();
            require(restored.batch.gap(opposite) == gaps[axis], "gap not shared across the axis");
            require(
                last[axis] == sharedFar[axis] - gaps[axis] - 1 && first[axis] == 0,
                "incorrect batch spacing for " + sides[axis]);
        }
        restored.readFromNBT(legacy);
        require(restored.batch.arrangementKey() == legacyKey, "Legacy shares were not converted to one-block overlaps");
    }

    private static void verifyWorldPreview(int size, int width) {
        var check = ProjectorClient.CHECK;
        require(
            check.structure != null && check.structure.size == size && check.missing == size,
            "world batch and missing-cell tally: expected " + size + ", got " + check.missing);
        int[] bounds = check.structure.worldBounds(check.facing);
        require(
            bounds[3] - bounds[0] + 1 == width && bounds[4] - bounds[1] + 1 == 4 && bounds[5] - bounds[2] + 1 == 3,
            "world bounds");
    }

    private static void screenshot(String name) {
        var mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void require(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
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
