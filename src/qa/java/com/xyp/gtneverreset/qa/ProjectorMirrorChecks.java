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
import com.xyp.gtneverreset.common.projector.ProjectorBatch;
import com.xyp.gtneverreset.common.projector.ProjectorBatchCells;
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
    name = "Industrial cutter translation checks",
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
                        plan.batch.setGap(direction, -1);
                    }
                } else {
                    var dir = scenario == 0 ? ForgeDirection.UP
                        : scenario == 3 ? ForgeDirection.WEST : ForgeDirection.EAST;
                    plan.batch.setAdditional(dir, 1);
                    plan.batch.setGap(dir, scenario == 0 || scenario == 2 ? -1 : 0);
                }
                plan.batchProjection = true;
                var stack = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
                plan.write(stack);
                p.inventory.setInventorySlotContents(p.inventory.currentItem, stack);
                p.inventoryContainer.detectAndSendChanges();
                stage = Stage.OPEN;
            } else if (stage == Stage.VERIFY) {
                verifyArray(p);
                if (scenario == 4) {
                    finish(null);
                    return;
                }
                scenario++;
                stage = Stage.PREPARE;
            }
        } catch (Throwable ex) {
            finish(ex);
        }
    }

    private void verifyArray(EntityPlayerMP player) throws Exception {
        var plan = ProjectorPlan.read(player.getHeldItem());
        var layout = new ProjectorBatch.Layout(plan, shape, base);
        var translated = ProjectorBatchCells.collect(shape, plan, base, layout, null);
        require(translated.compatible(), "Cutter array rejected");
        int[] offset = new int[3];
        int[][] source = shape.simulationPositions();
        int count = 0;
        for (var entry : translated.cells()
            .long2IntEntrySet()) {
            int cell = entry.getIntValue() % shape.size;
            if (shape.blocks[cell] == Blocks.air) continue;
            long pos = entry.getLongKey();
            base.getWorldOffset(
                new int[] { ProjectorPlan.unpackA(pos), ProjectorPlan.unpackB(pos), ProjectorPlan.unpackC(pos) },
                offset);
            int wx = x + offset[0], wy = y + offset[1], wz = z + offset[2];
            require(player.worldObj.getBlock(wx, wy, wz) == shape.blocks[cell], "Wrong cutter array block");
            require(
                player.worldObj.getBlockMetadata(wx, wy, wz)
                    == shape.world.getBlockMetadata(source[0][cell], source[1][cell], source[2][cell]),
                "Wrong cutter array metadata");
            if (shape.machines[cell] > 0) {
                require(
                    player.worldObj.getTileEntity(wx, wy, wz) instanceof IGregTechTileEntity gt
                        && gt.getMetaTileID() == shape.machines[cell],
                    "Wrong cutter machine");
                if (shape.a[cell] == 0 && shape.b[cell] == 0 && shape.c[cell] == 0) {
                    var tile = (IGregTechTileEntity) player.worldObj.getTileEntity(wx, wy, wz);
                    require(
                        ((IAlignment) tile.getMetaTileEntity()).getExtendedFacing() == base,
                        "Cutter controller changed facing");
                }
            }
            count++;
        }
        require(placed == count, "Unique placed array blocks");
        Files.writeString(Path.of("cutter-" + scenario + ".txt"), "Verified " + placed + " translated cutter blocks.");
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
                var plan = gui.working;
                var layout = new ProjectorBatch.Layout(plan, shape, base);
                var translated = ProjectorBatchCells.collect(shape, plan, base, layout, null);
                require(
                    projected != null && projected.size == translated.cells()
                        .size(),
                    "Three-axis translated preview size");
                require(
                    java.util.Arrays.equals(projected.worldBounds(base), layout.worldBounds()),
                    "Three-axis preview dimensions");
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
