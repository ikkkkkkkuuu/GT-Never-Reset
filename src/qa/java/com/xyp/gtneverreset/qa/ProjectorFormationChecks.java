package com.xyp.gtneverreset.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.*;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.client.projector.*;
import com.xyp.gtneverreset.common.projector.*;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.ItemList;

@Mod(
    modid = "projectorformationqa",
    name = "Shared hatch formation checks",
    version = "1",
    dependencies = "after:gtneverreset")
public class ProjectorFormationChecks {

    boolean launched, given, opened;
    volatile boolean done;
    int stage, ticks;
    final Map<Long, Integer> owners = new LinkedHashMap<>();
    long center, edge, corner;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (given || event.phase != TickEvent.Phase.END) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        var p = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        var plan = new ProjectorPlan();
        plan.selectMachine(ProjectorMachines.idOf(ItemList.Machine_Multi_ImplosionCompressor.get(1)));
        plan.state = ProjectorPlan.State.LOCKED;
        plan.x = (int) p.posX + 6;
        plan.y = 8;
        plan.z = (int) p.posZ + 7;
        plan.facing = ExtendedFacing.of(ForgeDirection.NORTH);
        plan.batchProjection = true;
        for (var side : new ForgeDirection[] { ForgeDirection.EAST, ForgeDirection.SOUTH }) {
            plan.batch.setAdditional(side, 1);
            plan.batch.setGap(side, -1);
        }
        var item = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
        plan.write(item);
        p.inventory.setInventorySlotContents(0, item);
        p.inventoryContainer.detectAndSendChanges();
        given = true;
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
            if (!launched && mc.currentScreen != null
                && (mc.currentScreen instanceof GuiMainMenu || mc.currentScreen.getClass()
                    .getSimpleName()
                    .equals("GSGuiMainMenu"))) {
                launched = true;
                mc.launchIntegratedServer(
                    "shared-hatch-" + System.currentTimeMillis(),
                    "Shared hatch formation",
                    new WorldSettings(4L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (mc.thePlayer == null) return;
            require(++ticks < 2400, "timeout at stage " + stage);
            var shape = ProjectorClient.CHECK.structure;
            var base = ProjectorClient.structure;
            if (shape == null || base == null || shape.size != 71) return;
            if (!opened) {
                var plan = ProjectorClient.plan;
                var layout = new ProjectorBatch.Layout(plan.batch, base, plan.facing);
                require(layout.solve(), "2x2 layout");
                for (int copy = 0; copy < 4; copy++) for (int cell = 0; cell < base.size; cell++) {
                    if (base.slotAt(cell) == null) continue;
                    var at = layout.placement(copy);
                    int[] world = new int[3], abc = new int[3];
                    at.facing()
                        .getWorldOffset(new int[] { base.a[cell], base.b[cell], base.c[cell] }, world);
                    world[0] += at.x();
                    world[1] += at.y();
                    world[2] += at.z();
                    plan.facing.getOffsetABC(world, abc);
                    owners.merge(ProjectorPlan.pack(abc[0], abc[1], abc[2]), 1 << copy, (a, b) -> a | b);
                }
                center = position(4);
                edge = position(2);
                corner = position(1);
                ProjectorClient.openAt(
                    shape,
                    shape.indexOf(
                        ProjectorPlan.unpackA(center),
                        ProjectorPlan.unpackB(center),
                        ProjectorPlan.unpackC(center)));
                opened = true;
                return;
            }
            var screen = ModularScreen.getCurrent();
            if (screen == null || !(screen.getMainPanel() instanceof ProjectorGui gui)) return;
            var lines = new ArrayList<String>();
            RichTooltip tooltip = new RichTooltip() {

                @Override
                public RichTooltip addLine(String text) {
                    lines.add(text);
                    return this;
                }
            };
            var build = ProjectorGui.class.getDeclaredMethod("statusTooltip", RichTooltip.class);
            build.setAccessible(true);
            build.invoke(gui, tooltip);
            if (lines.isEmpty() || lines.stream()
                .anyMatch(l -> l.contains("正在检查"))) return;
            String text = String.join(" | ", lines);
            Files.writeString(
                Path.of("formation-status.txt"),
                stage + " " + text + System.lineSeparator(),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
            String missing = lines.stream()
                .filter(l -> l.contains("维护仓"))
                .findFirst()
                .orElse("");
            if (stage < 4) {
                int n = new int[] { 4, 0, 2, 3 }[stage];
                require(
                    n == 0 ? missing.isEmpty() : missing.contains(n + "/4"),
                    "maintenance stage " + stage + " " + text);
                require(text.contains("能源仓") && text.contains("输入总线"), "other requirements remain " + text);
            }
            var changes = gui.working.batchOverrides;
            switch (stage++) {
                case 0 -> gui.hatches.chooseKind(HatchElement.Maintenance);
                case 1 -> {
                    require(changes.size() == 1 && changes.containsKey(center), "one physical shared hatch");
                    changes.clear();
                    changes.put(edge, ProjectorMachines.idOf(ItemList.Hatch_Maintenance.get(1)));
                }
                case 2 -> {
                    changes.clear();
                    changes.put(corner, ProjectorMachines.idOf(ItemList.Hatch_Maintenance.get(1)));
                }
                case 3 -> {
                    changes.clear();
                    changes.put(center, ProjectorMachines.idOf(ItemList.Hatch_Maintenance.get(1)));
                    for (int copy = 0; copy < 4; copy++) {
                        var positions = new ArrayList<Long>();
                        for (var e : owners.entrySet()) if (e.getValue() == (1 << copy)) positions.add(e.getKey());
                        int i = 0;
                        for (var hatch : new ItemList[] { ItemList.Hatch_Energy_MV, ItemList.Hatch_Input_Bus_LV,
                            ItemList.Hatch_Output_Bus_LV, ItemList.Hatch_Muffler_LV })
                            changes.put(positions.get(i++), ProjectorMachines.idOf(hatch.get(1)));
                    }
                }
                case 4 -> {
                    require(lines.size() == 1 && text.contains("4/4"), "all four form " + text);
                    ScreenShotHelper.saveScreenshot(
                        new File("."),
                        "formation-four.png",
                        mc.displayWidth,
                        mc.displayHeight,
                        mc.getFramebuffer());
                    finish(null);
                }
            }
        } catch (Throwable t) {
            finish(t);
        }
    }

    private long position(int count) {
        return owners.entrySet()
            .stream()
            .filter(e -> Integer.bitCount(e.getValue()) == count)
            .mapToLong(Map.Entry::getKey)
            .findFirst()
            .orElseThrow();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void finish(Throwable t) {
        try {
            var s = new StringWriter();
            if (t != null) t.printStackTrace(new PrintWriter(s));
            Files.writeString(Path.of("result.txt"), t == null ? "PASS" : s.toString());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        done = true;
    }
}
