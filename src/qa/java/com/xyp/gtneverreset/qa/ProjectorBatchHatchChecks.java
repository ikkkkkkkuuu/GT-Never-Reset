package com.xyp.gtneverreset.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.client.projector.ProjectorClient;
import com.xyp.gtneverreset.client.projector.ProjectorGui;
import com.xyp.gtneverreset.common.item.items.WirelessPowerCard;
import com.xyp.gtneverreset.common.projector.*;
import com.xyp.gtneverreset.common.wireless.IWirelessPowerReceiver;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.misc.WirelessNetworkManager;

@Mod(
    modid = "gtnrprojectorbatchhatchqa",
    name = "Projector batch hatch checks",
    version = "1",
    dependencies = "after:gtneverreset")
public class ProjectorBatchHatchChecks {

    private static final long POWER_BUDGET = 4_000_000L;
    private int chargeTicks;
    private int[] singleHatch;
    private volatile boolean done;
    private volatile ProjectorStructure confirmed;
    private boolean launched, seeded, opened, requested;
    private int ticks, frames, hatch, originalCasings;
    private long separate, shared;
    private ItemStack projector, casing;

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
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
        var p = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            if (!seeded) {
                var plan = new ProjectorPlan();
                plan.selectMachine(ProjectorMachines.idOf(ItemList.Machine_Multi_BlastFurnace.get(1)));
                plan.state = ProjectorPlan.State.LOCKED;
                plan.x = (int) p.posX + 24;
                plan.y = 12;
                plan.z = (int) p.posZ + 3;
                plan.facing = ExtendedFacing.of(ForgeDirection.NORTH);
                plan.batchProjection = true;
                for (var side : new ForgeDirection[] { ForgeDirection.EAST, ForgeDirection.SOUTH }) {
                    plan.batch.setAdditional(side, 1);
                    plan.batch.setShared(side, true);
                }
                projector = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
                plan.write(projector);
                p.inventory.setInventorySlotContents(0, projector);
                p.inventoryContainer.detectAndSendChanges();
                seeded = true;
            }
            if (singleHatch != null) {
                if (p.worldObj
                    .getTileEntity(singleHatch[0], singleHatch[1], singleHatch[2]) instanceof IGregTechTileEntity tile
                    && tile.getMetaTileID() == hatch
                    && ((IWirelessPowerReceiver) tile).gtnr$powerConnection() != null
                    && tile.getStoredEU() > 0) {
                    finish(null);
                } else require(++chargeTicks < 240, "single remote energy hatch did not receive EU");
                return;
            }
            var shape = confirmed;
            if (shape == null) return;
            var plan = ProjectorPlan.read(projector);
            if (plan.batchOverrides.size() != 2) return;
            require(plan.overrides.isEmpty(), "single template was changed");
            if (!requested) {
                java.util.Arrays.fill(p.inventory.mainInventory, 1, p.inventory.mainInventory.length, null);
                var card = GTNRItemList.WirelessPowerCard.get(1);
                p.inventory.setInventorySlotContents(8, card);
                var source = WirelessPowerCard.source(card, p, true);
                WirelessNetworkManager.addEUToGlobalEnergyMap(source, POWER_BUDGET);
                for (var m : shape.materials) {
                    var stack = m.stack()
                        .copy();
                    stack.stackSize = m.count();
                    require(p.inventory.addItemStackToInventory(stack) && stack.stackSize == 0, "seed survival bill");
                }
                ProjectorBuilder.request(p, 0, true);
                requested = true;
                return;
            }
            int[] offset = new int[3];
            int actual = 0, powered = 0;
            for (int i = 0; i < shape.size; i++) {
                plan.facing.getWorldOffset(new int[] { shape.a[i], shape.b[i], shape.c[i] }, offset);
                int x = plan.x + offset[0], y = plan.y + offset[1], z = plan.z + offset[2];
                if (p.worldObj.isAirBlock(x, y, z)) return;
                require(p.worldObj.getBlock(x, y, z) == shape.blocks[i], "wrong world block");
                if (p.worldObj.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile) {
                    require(tile.getMetaTileID() == shape.machines[i], "wrong world machine");
                    if (tile.getMetaTileID() == hatch) {
                        actual++;
                        require(
                            p.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) >= 64,
                            "remote placement test is inside GUI range");
                        if (((IWirelessPowerReceiver) tile).gtnr$powerConnection() != null && tile.getStoredEU() > 0)
                            powered++;
                    }
                }
            }
            require(actual == 2, "two physical hatches in four machines");
            if (powered < actual) {
                require(++chargeTicks < 60, "projected energy hatches received power: " + powered + "/" + actual);
                return;
            }
            require(
                WirelessNetworkManager.getUserEU(WirelessPowerCard.source(p.inventory.getStackInSlot(8), p, false))
                    .longValue() < POWER_BUDGET,
                "wireless network supplied EU");
            for (int i = 1; i < p.inventory.mainInventory.length; i++)
                require(i == 8 || p.inventory.mainInventory[i] == null, "leftover materials");
            // Exercise the single-build packet's server path with a second remote controller.
            p.theItemInWorldManager.setGameType(WorldSettings.GameType.CREATIVE);
            plan.batchProjection = false;
            plan.x += 16;
            plan.batchOverrides.clear();
            var template = ProjectorSimulation.run(plan.settings());
            for (int i = 0; i < template.size; i++) {
                var slot = template.slotAt(i);
                if (slot == null || !java.util.Arrays.stream(slot.candidates())
                    .anyMatch(id -> id == hatch)) continue;
                var local = new int[] { template.a[i], template.b[i], template.c[i] };
                plan.overrides.put(ProjectorPlan.pack(local[0], local[1], local[2]), hatch);
                singleHatch = new int[3];
                plan.facing.getWorldOffset(local, singleHatch);
                singleHatch[0] += plan.x;
                singleHatch[1] += plan.y;
                singleHatch[2] += plan.z;
                break;
            }
            require(singleHatch != null, "single energy hatch slot");
            chargeTicks = 0;
            plan.write(projector);
            ProjectorBuilder.request(p, 0, false);
        } catch (Throwable t) {
            finish(t);
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
            if (!launched && mc.currentScreen != null
                && (mc.currentScreen instanceof GuiMainMenu || mc.currentScreen.getClass()
                    .getSimpleName()
                    .equals("GSGuiMainMenu"))) {
                launched = true;
                mc.launchIntegratedServer(
                    "batch-hatches-" + System.currentTimeMillis(),
                    "Batch hatches",
                    new WorldSettings(4L, WorldSettings.GameType.SURVIVAL, false, false, WorldType.FLAT));
            }
            if (mc.thePlayer == null) return;
            require(++ticks < 1800, "timeout; frames=" + frames + " requested=" + requested);
            var shape = ProjectorClient.CHECK.structure;
            if (shape == null || ProjectorClient.structure == null || shape.size <= 34) return;
            if (!opened) {
                var template = ProjectorClient.structure;
                var plan = ProjectorClient.plan;
                var layout = new ProjectorBatch.Layout(plan.batch, template, plan.facing);
                require(layout.solve(), "four blast furnaces can share");
                Map<Long, Integer> owners = new HashMap<>();
                int[] w = new int[3], abc = new int[3];
                for (int n = 0; n < 4; n++) {
                    var at = layout.placement(n);
                    for (int i = 0; i < template.size; i++) {
                        at.facing()
                            .getWorldOffset(new int[] { template.a[i], template.b[i], template.c[i] }, w);
                        w[0] += at.x();
                        w[1] += at.y();
                        w[2] += at.z();
                        plan.facing.getOffsetABC(w, abc);
                        owners.merge(ProjectorPlan.pack(abc[0], abc[1], abc[2]), 1, Integer::sum);
                    }
                }
                int localCell = -1, sharedCell = -1;
                for (int i = 0; i < shape.size; i++) {
                    var slot = shape.slotAt(i);
                    if (slot == null || slot.casing() == null
                        || shape.machines[i] != 0
                        || !java.util.Arrays.stream(slot.candidates())
                            .anyMatch(id -> id == energyHatch()))
                        continue;
                    long pos = ProjectorPlan.pack(shape.a[i], shape.b[i], shape.c[i]);
                    if (template.indexOf(shape.a[i], shape.b[i], shape.c[i]) >= 0) continue;
                    if (owners.get(pos) == 1) localCell = i;
                    if (owners.get(pos) == 2) sharedCell = i;
                }
                require(localCell >= 0 && sharedCell >= 0, "unique and shared slots outside the first copy");
                separate = key(shape, localCell);
                shared = key(shape, sharedCell);
                casing = shape.items[localCell];
                originalCasings = count(shape, casing);
                ProjectorClient.openAt(shape, localCell);
                opened = true;
                return;
            }
            var screen = ModularScreen.getCurrent();
            if (screen != null && screen.getMainPanel() instanceof ProjectorGui gui) {
                if (frames == 0) {
                    require(!gui.hatches.sameKind(), "array edits must default to the selected slot");
                    gui.hatches.chooseKind(ProjectorEnergyHatches.Flow.INPUT);
                    gui.hatches.chooseVariant(energyHatch());
                    gui.hatches.chooseTier(energyHatch());
                    require(gui.hatches.held() == energyHatch(), "ordinary MV energy hatch");
                    hatch = gui.hatches.held();
                    require(
                        hatch > 0 && gui.working.batchOverrides.get(separate) == hatch
                            && gui.working.overrides.isEmpty(),
                        "selecting a unique array cell: selected=" + hatch
                            + " key="
                            + separate
                            + " kinds="
                            + gui.hatches.kinds()
                            + " array="
                            + gui.working.batchOverrides
                            + " single="
                            + gui.working.overrides);
                }
                if (frames == 4) {
                    var select = ProjectorGui.class.getDeclaredMethod("select", int.class);
                    select.setAccessible(true);
                    select.invoke(gui, index(shape, shared));
                    gui.hatches.chooseKind(ProjectorEnergyHatches.Flow.INPUT);
                    gui.hatches.chooseVariant(energyHatch());
                    gui.hatches.chooseTier(energyHatch());
                    require(gui.hatches.held() == energyHatch(), "ordinary MV energy hatch");
                    require(gui.working.batchOverrides.get(shared) == hatch, "selecting a shared cell");
                }
                if (frames > 8 && count(shape, ProjectorMachines.stackOf(hatch)) == 2) {
                    require(count(shape, casing) == originalCasings - 2, "shared boundary deducted once");
                    checkLocations(shape);
                    ScreenShotHelper.saveScreenshot(
                        new File("."),
                        "batch-two-hatches.png",
                        mc.displayWidth,
                        mc.displayHeight,
                        mc.getFramebuffer());
                    screen.close();
                }
                frames++;
            } else if (frames > 8 && confirmed == null && ProjectorClient.plan.batchOverrides.size() == 2) {
                checkLocations(shape);
                confirmed = shape;
            }
        } catch (Throwable t) {
            finish(t);
        }
    }

    private static int energyHatch() {
        return ProjectorMachines.idOf(ItemList.Hatch_Energy_MV.get(1));
    }

    private void checkLocations(ProjectorStructure shape) {
        for (int i = 0; i < shape.size; i++) require(
            (shape.machines[i] == hatch) == (key(shape, i) == shared || key(shape, i) == separate),
            "overrides applied to other furnaces");
    }

    private static int count(ProjectorStructure shape, ItemStack stack) {
        return shape.materials.stream()
            .filter(
                m -> m.stack()
                    .isItemEqual(stack))
            .mapToInt(ProjectorStructure.Material::count)
            .sum();
    }

    private static long key(ProjectorStructure shape, int i) {
        return ProjectorPlan.pack(shape.a[i], shape.b[i], shape.c[i]);
    }

    private static int index(ProjectorStructure shape, long key) {
        return shape.indexOf(ProjectorPlan.unpackA(key), ProjectorPlan.unpackB(key), ProjectorPlan.unpackC(key));
    }

    private static void require(boolean ok, String detail) {
        if (!ok) throw new AssertionError(detail);
    }

    private void finish(Throwable error) {
        try {
            var trace = new StringWriter();
            if (error != null) error.printStackTrace(new PrintWriter(trace));
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : trace.toString());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        done = true;
    }
}
