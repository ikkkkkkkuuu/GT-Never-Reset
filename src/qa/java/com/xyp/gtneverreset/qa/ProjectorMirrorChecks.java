package com.xyp.gtneverreset.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.client.projector.ProjectorClient;
import com.xyp.gtneverreset.client.projector.ProjectorGui;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtnrlib.gui.widget.AE2Button;
import com.xyp.gtnrlib.gui.widget.PageTabs;
import com.xyp.gtnrlib.gui.widget.ThemedButton;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.*;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.MetaTileEntityIDs;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

@Mod(
    modid = "projectormirrorqa",
    name = "Industrial cutter mirror checks",
    version = "1",
    dependencies = "after:gtneverreset")
public class ProjectorMirrorChecks {

    private enum Stage {
        PREPARE,
        OPEN,
        WAIT,
        VERIFY
    }

    private volatile Stage stage = Stage.PREPARE;
    private volatile boolean done;
    private boolean launched, opening, tabOpen;
    private int scenario, frames, ticks, x, y, z, placed;
    private ProjectorStructure shape;
    private final int id = MetaTileEntityIDs.IndustrialCuttingMachine.ID;
    private final ExtendedFacing base = ExtendedFacing.byIndex(32);

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent e) {
        if (done || e.phase != TickEvent.Phase.END) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        var p = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            if (stage == Stage.PREPARE) {
                var plan = new ProjectorPlan();
                plan.selectMachine(id);
                plan.state = ProjectorPlan.State.LOCKED;
                plan.dimension = p.dimension;
                plan.x = x = (int) p.posX - 12;
                plan.y = y = 12 + scenario * 14;
                plan.z = z = (int) p.posZ + 10;
                plan.facing = base;
                if (scenario == 4) {
                    for (ForgeDirection direction : new ForgeDirection[] { ForgeDirection.WEST, ForgeDirection.SOUTH,
                        ForgeDirection.UP }) {
                        plan.batch.setAdditional(direction, 1);
                        plan.batch.setShared(direction, true);
                    }
                    plan.batch.setShared(ForgeDirection.NORTH, true);
                } else {
                    var dir = scenario == 0 ? ForgeDirection.UP
                        : scenario == 3 ? ForgeDirection.WEST : ForgeDirection.EAST;
                    plan.batch.setAdditional(dir, 1);
                    plan.batch.setShared(dir, scenario == 0 || scenario == 2);
                }
                plan.batchProjection = true;
                var stack = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
                plan.write(stack);
                p.inventory.setInventorySlotContents(p.inventory.currentItem, stack);
                p.inventoryContainer.detectAndSendChanges();
                stage = Stage.OPEN;
            } else if (stage == Stage.VERIFY) {
                if (scenario == 4) {
                    verifyArray(p);
                    finish(null);
                    return;
                }
                var bounds = shape.worldBounds(base);
                int[] offset = new int[3], local = new int[3], transformed = new int[3];
                var cells = new HashSet<Long>();
                for (int copy = 0; copy < 2; copy++) {
                    boolean upper = copy == 1 && scenario == 0, lateral = copy == 1 && !upper;
                    int stride = bounds[3] - bounds[0] + (scenario == 2 ? 0 : 1);
                    int ox = x + (lateral ? (scenario == 3 ? -stride : stride) + bounds[0] + bounds[3] : 0);
                    int oy = y + (upper ? bounds[4] - bounds[1] + bounds[1] + bounds[4] : 0);
                    var controller = (IGregTechTileEntity) p.worldObj.getTileEntity(ox, oy, z);
                    require(
                        controller != null && controller.getMetaTileID() == id,
                        "controller position at " + ox + "," + oy + "," + z);
                    var pose = ((IAlignment) controller.getMetaTileEntity()).getExtendedFacing();
                    require(pose.getDirection() == base.getDirection(), "both controllers must face the front");
                    for (int axis = 0; axis < 3; axis++) {
                        int[] unit = new int[3];
                        unit[axis] = 1;
                        base.getOffsetABC(unit, local);
                        pose.getWorldOffset(local, transformed);
                        int sign = (axis == 0 && lateral || axis == 1 && upper) ? -1 : 1;
                        require(transformed[axis] == sign, "unexpected transform axis " + axis + " at " + pose);
                    }
                    for (int i = 0; i < shape.size; i++) {
                        base.getWorldOffset(new int[] { shape.a[i], shape.b[i], shape.c[i] }, offset);
                        int wx = ox + (lateral ? -offset[0] : offset[0]), wy = oy + (upper ? -offset[1] : offset[1]),
                            wz = z + offset[2];
                        require(
                            p.worldObj.getBlock(wx, wy, wz) == shape.blocks[i],
                            "block mismatch at " + wx + "," + wy + "," + wz);
                        if (shape.machines[i] > 0) {
                            var tile = (IGregTechTileEntity) p.worldObj.getTileEntity(wx, wy, wz);
                            require(tile.getMetaTileID() == shape.machines[i], "machine variant");
                        } else {
                            int[][] pos = shape.simulationPositions();
                            require(
                                p.worldObj.getBlockMetadata(wx, wy, wz)
                                    == shape.world.getBlockMetadata(pos[0][i], pos[1][i], pos[2][i]),
                                "material metadata");
                        }
                        if (shape.blocks[i] != net.minecraft.init.Blocks.air) cells.add(ProjectorPlan.pack(wx, wy, wz));
                    }
                }
                require(placed == cells.size(), "unique placed blocks");
                Files.writeString(
                    Path.of("cutter-" + scenario + ".txt"),
                    "Verified 2 mirrored cutters and " + placed + " unique blocks.");
                scenario++;
                stage = Stage.PREPARE;
            }
        } catch (Throwable ex) {
            finish(ex);
        }
    }

    private void verifyArray(EntityPlayerMP player) throws Exception {
        int[] bounds = shape.worldBounds(base);
        int width = bounds[3] - bounds[0] + 1, height = bounds[4] - bounds[1] + 1, depth = bounds[5] - bounds[2] + 1;
        int minX = x + bounds[0] - (width - 1), minY = y + bounds[1], minZ = z + bounds[2];
        var world = player.worldObj;
        var cells = new HashSet<Long>();
        var copies = new HashSet<Integer>();
        int[] template = { 0, 0, 0 }, offset = new int[3];
        for (int cx = minX; cx < minX + width * 2 - 1; cx++)
            for (int cy = minY; cy < minY + height * 2 - 1; cy++) for (int cz = minZ; cz < minZ + depth * 2 - 1; cz++) {
                if (!(world.getTileEntity(cx, cy, cz) instanceof IGregTechTileEntity controller)
                    || controller.getMetaTileID() != id) continue;
                var pose = ((IAlignment) controller.getMetaTileEntity()).getExtendedFacing();
                int lowX = Integer.MAX_VALUE, lowY = Integer.MAX_VALUE, lowZ = Integer.MAX_VALUE;
                for (int i = 0; i < shape.size; i++) {
                    template[0] = shape.a[i];
                    template[1] = shape.b[i];
                    template[2] = shape.c[i];
                    pose.getWorldOffset(template, offset);
                    int wx = cx + offset[0], wy = cy + offset[1], wz = cz + offset[2];
                    lowX = Math.min(lowX, wx);
                    lowY = Math.min(lowY, wy);
                    lowZ = Math.min(lowZ, wz);
                    require(
                        world.getBlock(wx, wy, wz) == shape.blocks[i],
                        "array block at " + wx + "," + wy + "," + wz);
                    if (shape.machines[i] > 0) {
                        require(
                            world.getTileEntity(wx, wy, wz) instanceof IGregTechTileEntity gt
                                && gt.getMetaTileID() == shape.machines[i],
                            "array machine variant");
                    } else {
                        int[][] pos = shape.simulationPositions();
                        require(
                            world.getBlockMetadata(wx, wy, wz)
                                == shape.world.getBlockMetadata(pos[0][i], pos[1][i], pos[2][i]),
                            "array material metadata");
                    }
                    if (shape.blocks[i] != net.minecraft.init.Blocks.air) cells.add(ProjectorPlan.pack(wx, wy, wz));
                }
                int ix = lowX - minX, iy = lowY - minY, iz = lowZ - minZ;
                require(
                    (ix == 0 || ix == width - 1) && (iy == 0 || iy == height - 1) && (iz == 0 || iz == depth - 1),
                    "array stride");
                require(copies.add((ix == 0 ? 0 : 1) + (iy == 0 ? 0 : 2) + (iz == 0 ? 0 : 4)), "duplicate array slot");
                var front = pose.getDirection();
                require(
                    world.isAirBlock(cx + front.offsetX, cy + front.offsetY, cz + front.offsetZ),
                    "controller front obstructed");
            }
        require(
            copies.size() == 8 && cells.size() == placed && placed == 458,
            "eight controllers and unique placement count");
        Files.writeString(
            Path.of("cutter-4.txt"),
            "Verified west/south/up array 17x7x5 with 8 cutters and " + placed + " unique blocks.");
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
            if (++ticks > 2400) throw new AssertionError("timeout " + scenario + " " + stage);
            if (!launched && mc.currentScreen != null
                && (mc.currentScreen instanceof GuiMainMenu || mc.currentScreen.getClass()
                    .getSimpleName()
                    .equals("GSGuiMainMenu"))) {
                launched = true;
                mc.launchIntegratedServer(
                    "offset-" + System.currentTimeMillis(),
                    "Cutting machine",
                    new WorldSettings(4L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT)
                        .enableCommands());
            }
            if (stage != Stage.OPEN || mc.thePlayer == null
                || ProjectorClient.plan == null
                || ProjectorClient.plan.y != y) return;
            if (mc.currentScreen == null && !opening) {
                ProjectorClient.open();
                opening = true;
            }
        } catch (Throwable ex) {
            finish(ex);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent e) {
        if (done || stage != Stage.OPEN || e.phase != TickEvent.Phase.END || !opening) return;
        var screen = ModularScreen.getCurrent();
        if (screen == null || !(screen.getMainPanel() instanceof ProjectorGui gui)) return;
        try {
            if (!tabOpen) {
                var tabs = (PageTabs) gui.getChildren()
                    .stream()
                    .filter(PageTabs.class::isInstance)
                    .findFirst()
                    .orElseThrow();
                ((ThemedButton<?>) tabs.getChildren()
                    .get(ProjectorGui.BATCH)).onMousePressed(0);
                tabOpen = true;
            }
            shape = gui.structure();
            if (shape == null || !shape.settings.equals(gui.working.settings()) || ++frames < 32) return;
            if (scenario == 4) {
                var projected = ProjectorClient.CHECK.structure;
                require(
                    projected != null && projected.size == 458,
                    "three-axis preview must contain 458 unique blocks");
                int[] bounds = projected.worldBounds(base);
                require(
                    bounds[3] - bounds[0] + 1 == 17 && bounds[4] - bounds[1] + 1 == 7 && bounds[5] - bounds[2] + 1 == 5,
                    "three-axis preview dimensions");
            }
            var mc = Minecraft.getMinecraft();
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "cutter-" + scenario + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            stage = Stage.WAIT;
            tabOpen = false;
            opening = false;
            frames = 0;
            WidgetTree.findFirstWithName(gui, "generate_batch", AE2Button.class)
                .onMousePressed(0);
        } catch (Throwable ex) {
            finish(ex);
        }
    }

    @SubscribeEvent
    public void chat(ClientChatReceivedEvent e) {
        if (stage != Stage.WAIT || !(e.message instanceof ChatComponentTranslation t)) return;
        if (t.getKey()
            .equals("gtneverreset.projector.build.done")) {
            placed = Integer.parseInt(String.valueOf(t.getFormatArgs()[0]));
            stage = Stage.VERIFY;
        } else if (t.getKey()
            .startsWith("gtneverreset.projector.build.")
            && !t.getKey()
                .endsWith(".started"))
            finish(new AssertionError(t.getUnformattedText()));
    }

    private static void require(boolean valid, String reason) {
        if (!valid) throw new AssertionError(reason);
    }

    private void finish(Throwable error) {
        try {
            StringWriter trace = new StringWriter();
            if (error != null) error.printStackTrace(new PrintWriter(trace));
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : "FAIL " + scenario + " " + trace);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        done = true;
    }
}
