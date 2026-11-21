package com.xyp.gtneverreset.qa;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtneverreset.client.lighthunter.LightHunterSpaceStationRenderer;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.multiblock.LightHunterSpaceStation;
import com.xyp.gtneverreset.common.machine.multiblock.NexusSatelliteFactory;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.api.AEApi;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.common.misc.WirelessNetworkManager;

@Mod(modid = "lighthunterqa", name = "Light Hunter QA", version = "1", dependencies = "after:gtneverreset")
public final class LightHunterChecks {

    private final StringBuilder report = new StringBuilder();
    private boolean launched, opened, finished;
    private volatile boolean ready, galaxyCaptured;
    private volatile boolean voltageSynced;
    private boolean voltageEditSent;
    private volatile Throwable failure;
    private int ticks, frames, serverTicks;
    private BaseMetaTileEntity stationTile, satelliteTile, inputBus;
    private long singleCraftEU;
    private int singleCraftDuration;
    private boolean speedTestStarted, speedVerified;
    private CrossRecipeMEPatternBuffer patternBuffer;
    private boolean patternsLoaded, patternsVerified;
    private boolean queuedPair, pairVerified, withdrawalVerified;
    private LightHunterSpaceStation station;
    private NexusSatelliteFactory satellite;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || failure != null || !GregTechAPI.sFullLoadFinished) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) players.get(0);
            if (station == null) {
                var world = player.worldObj;
                stationTile = place(world, 0, 0, GTNRMachineID.LightHunterSpaceStation.id);
                stationTile.setOwnerUuid(player.getUniqueID());
                stationTile.setFrontFacing(ForgeDirection.NORTH);
                station = (LightHunterSpaceStation) stationTile.getMetaTileEntity();
                station.construct(new ItemStack(Items.stick), false);
                require(station.checkStructure(true, stationTile), "station forms with only its casings");
                WirelessNetworkManager.setUserEU(
                    player.getUniqueID(),
                    BigInteger.valueOf(LightHunterSpaceStation.MAINTENANCE_EUT)
                        .multiply(BigInteger.valueOf(3000)));
                stationTile.enableWorking();
                for (int offset = 0; offset < 16; offset++) {
                    var prototype = (NexusSatelliteFactory) GregTechAPI.METATILEENTITIES[GTNRMachineID.MetalFormingSatelliteMkI.id
                        + offset];
                    int grade = offset % 4;
                    require(
                        prototype.getMaxRecipeThreads() == new int[] { 32, 2048, 131072, 8388608 }[grade]
                            && prototype.getMaxParallelRecipes()
                                == new int[] { 2048, 131072, 8388608, 2147483643 }[grade]
                            && Math.abs(prototype.getEUtMultiplier() - (9 - grade) / 10D) < 1e-9,
                        "tier parameters " + prototype.getLocalName());
                    require(
                        prototype.getAvailableRecipeMaps()
                            .size() == new int[] { 10, 9, 11, 6 }[offset / 4],
                        "category maps");
                }
                satelliteTile = place(world, -9, 41, GTNRMachineID.MetalFormingSatelliteMkII.id);
                satelliteTile.setOwnerName(player.getCommandSenderName());
                satelliteTile.setOwnerUuid(player.getUniqueID());
                satelliteTile.setFrontFacing(ForgeDirection.EAST);
                satellite = (NexusSatelliteFactory) satelliteTile.getMetaTileEntity();
                satellite.construct(new ItemStack(Items.stick), false);
                String[][] shape = com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader
                    .load("light_hunter_space_station_module");
                int[] hatches = { ItemList.Hatch_Maintenance.get(1)
                    .getItemDamage(),
                    ItemList.Hatch_Input_Bus_IV.get(1)
                        .getItemDamage(),
                    ItemList.Hatch_Output_Bus_IV.get(1)
                        .getItemDamage(),
                    GTNRMachineID.CrossRecipeMEPatternBuffer.id };
                int installed = 0;
                for (int y = 0; y < shape.length && installed < hatches.length; y++)
                    for (int z = 0; z < shape[y].length && installed < hatches.length; z++) {
                        for (int x = 0; x < shape[y][z].length() && installed < hatches.length; x++) {
                            if (shape[y][z].charAt(x) != 'O') continue;
                            int[] xyz = new int[3];
                            satellite.getExtendedFacing()
                                .getWorldOffset(new int[] { x - 2, y - 10, z }, xyz);
                            int hx = -9 + xyz[0], hy = 80 + xyz[1], hz = 41 + xyz[2];
                            if (world.getBlock(hx, hy, hz) != gregtech.api.casing.Casings.HighPowerCasing.getBlock())
                                continue;
                            if (installed == 1) {
                                place(
                                    world,
                                    hx,
                                    hy,
                                    hz,
                                    ItemList.Hatch_Energy_IV.get(1)
                                        .getItemDamage());
                                require(
                                    !satellite.checkStructure(true, satelliteTile),
                                    "structure rejects energy hatch");
                                world.setBlockToAir(hx, hy, hz);
                                place(world, hx, hy, hz, GTNRMachineID.StaticSpeedControllerT1.id);
                                require(
                                    !satellite.checkStructure(true, satelliteTile),
                                    "satellite structure rejects a local parameter hatch");
                                world.setBlockToAir(hx, hy, hz);
                            }
                            var hatch = place(world, hx, hy, hz, hatches[installed]);
                            if (installed == 1) inputBus = hatch;
                            if (installed == 3) patternBuffer = (CrossRecipeMEPatternBuffer) hatch.getMetaTileEntity();
                            installed++;
                        }
                    }
                require(
                    installed == 4 && satellite.checkStructure(true, satelliteTile),
                    "satellite forms with maintenance and I/O");
                satellite.fixAllIssues();
                GTRecipeBuilder.builder()
                    .itemInputs(new ItemStack(Items.poisonous_potato))
                    .itemOutputs(new ItemStack(Items.apple))
                    .eut(1_000_000_000)
                    .duration(100)
                    .addTo(RecipeMaps.latheRecipes);
                require(satellite.getHost() == station, "satellite binds to the station");
                verifySharedModules(world, player);
                satellite.setVoltage(3221225472L);
                NBTTagCompound tag = new NBTTagCompound();
                satellite.saveNBTData(tag);
                require(tag.getLong("satelliteVoltage") == 3221225472L, "64-bit per-parallel voltage persisted");
                player.capabilities.allowFlying = true;
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                player.playerNetServerHandler.setPlayerLocation(56, 111, 92, 26, 14);
                ready = true;
            }
            if (opened && !voltageSynced && satellite.getVoltage() == 4_500_000_001L) {
                require(true, "client voltage edit reaches server without 32-bit truncation");
                voltageSynced = true;
            }
            if (!opened && stationTile.isActive() && ++serverTicks > 10 && galaxyCaptured) {
                if (!withdrawalVerified) {
                    var owner = stationTile.getOwnerUuid();
                    var before = WirelessNetworkManager.getUserEU(owner);
                    satellite.lEUt = -500;
                    require(satellite.onRunningTick(null), "satellite obtains power from station");
                    require(
                        before.subtract(WirelessNetworkManager.getUserEU(owner))
                            .equals(BigInteger.valueOf(500)),
                        "station charges the owner exactly once for satellite power");
                    stationTile.disableWorking();
                    require(!satellite.onRunningTick(null), "disabled station stops satellite power");
                    stationTile.enableWorking();
                    verifyLargeWithdrawal();
                    withdrawalVerified = true;
                }
                if (singleCraftEU == 0 && inputBus.getStackInSlot(0) == null
                    && satellite.getRecipeThreadTasks()
                        .isEmpty()) {
                    inputBus.setInventorySlotContents(0, new ItemStack(Items.poisonous_potato, 1));
                    satellite.scheduleRecipeCheckImmediate();
                    return;
                }
                if (singleCraftEU == 0 && !satellite.getRecipeThreadTasks()
                    .isEmpty()) {
                    var task = satellite.getRecipeThreadTasks()
                        .get(0);
                    singleCraftEU = task.getEUt();
                    singleCraftDuration = task.getDuration();
                    require(task.getParallels() == 1 && singleCraftEU > 0, "one craft draws positive power");
                    inputBus.setInventorySlotContents(0, new ItemStack(Items.poisonous_potato, 2));
                    satellite.scheduleRecipeCheckImmediate();
                    queuedPair = true;
                }
                if (queuedPair && !pairVerified) {
                    for (var task : satellite.getRecipeThreadTasks()) {
                        if (task.getParallels() == 2) {
                            require(
                                task.getEUt() == singleCraftEU * 2,
                                "two parallels draw twice the EU/t at the same configured voltage");
                            pairVerified = true;
                            break;
                        }
                    }
                }
                if (!pairVerified) return;
                if (!speedTestStarted) {
                    place(player.worldObj, -1, 82, 0, GTNRMachineID.StaticSpeedControllerT1.id);
                    require(
                        station.checkStructure(true, stationTile),
                        "station accepts a speed hatch during processing");
                    inputBus.setInventorySlotContents(0, new ItemStack(Items.poisonous_potato, 3));
                    satellite.scheduleRecipeCheckImmediate();
                    speedTestStarted = true;
                    return;
                }
                if (!speedVerified) {
                    for (var task : satellite.getRecipeThreadTasks()) {
                        if (task.getParallels() != 3) continue;
                        require(
                            task.getDuration() * 2 == singleCraftDuration,
                            "station speed hatch halves actual satellite recipe duration");
                        speedVerified = true;
                        break;
                    }
                    if (!speedVerified) return;
                    restoreStationCasing(player.worldObj, -1, 82);
                    require(station.checkStructure(true, stationTile), "station reforms after speed hatch removal");
                }
                if (!patternsLoaded) {
                    require(
                        satellite.mDualInputHatches.contains(patternBuffer),
                        "formed satellite registers the cross-recipe ME buffer");
                    var pools = patternBuffer.getControllerRecipeMaps();
                    require(
                        pools.contains(RecipeMaps.latheRecipes.unlocalizedName)
                            && pools.contains(RecipeMaps.compressorRecipes.unlocalizedName)
                            && !pools.contains(RecipeMaps.assemblerRecipes.unlocalizedName),
                        "buffer advertises only this satellite's recipe maps");
                    loadCrossMapPatterns(player);
                    patternsLoaded = true;
                    satellite.scheduleRecipeCheckImmediate();
                    return;
                }
                if (!patternsVerified) {
                    Set<Item> outputs = new HashSet<>();
                    for (var task : satellite.getRecipeThreadTasks()) {
                        if (task.isFinished()) continue;
                        var items = new ArrayList<ItemStack>();
                        task.getOutputs()
                            .appendDisplayItems(items);
                        for (var item : items) {
                            outputs.add(item.getItem());
                            if (item.getItem() != Items.carrot && item.getItem() != Items.reeds) continue;
                            int requested = item.getItem() == Items.reeds ? 5 : 1;
                            require(
                                task.getParallels() == requested && task.getEUt() == requested * singleCraftEU
                                    && singleCraftDuration < 100
                                    && task.getDuration() == 4 * singleCraftDuration,
                                "first pattern task starts with all " + requested
                                    + " parallels and ordinary-input overclock");
                        }
                    }
                    if (!outputs.contains(Items.carrot) || !outputs.contains(Items.reeds)) return;
                    require(
                        satellite.getMachineMode() == 0 && satellite.getRecipeMap() == RecipeMaps.latheRecipes,
                        "lathe and compressor patterns run concurrently while front panel stays on lathe");
                    var slots = patternBuffer.inventories();
                    require(
                        !slots.next()
                            .isEmpty() && slots.next()
                                .isEmpty()
                            && slots.next()
                                .isEmpty(),
                        "successful cross-map slots consumed once; unsupported assembler pattern keeps its input");
                    patternsVerified = true;
                }
                player.playerNetServerHandler.setPlayerLocation(-7, 80, 41, 90, 0);
                require(satellite.onRightclick(satelliteTile, player), "satellite GUI opened");
                opened = true;
            }
        } catch (Throwable e) {
            failure = e;
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            mc.gameSettings.pauseOnLostFocus = false;
            if (mc.theWorld != null) mc.gameSettings.hideGUI = !galaxyCaptured;
            if (!launched && mc.theWorld == null && mc.currentScreen != null && ++ticks >= 40) {
                System.out.println(
                    "[light-hunter-qa] Opening isolated world from " + mc.currentScreen.getClass()
                        .getSimpleName());
                launched = true;
                mc.launchIntegratedServer(
                    "light-hunter-" + System.currentTimeMillis(),
                    "Light Hunter QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (ready && !galaxyCaptured && ticks % 200 == 0) {
                var ct = mc.theWorld.getTileEntity(0, 80, 0);
                System.out.println(
                    "[light-hunter-qa] station client=" + ct
                        + " active="
                        + (ct instanceof BaseMetaTileEntity te && te.isActive())
                        + " serverActive="
                        + stationTile.isActive()
                        + " frames="
                        + LightHunterSpaceStationRenderer.getRenderedFrames()
                        + " beamFrames="
                        + LightHunterSpaceStationRenderer.getBeamFrames());
            }
            if (failure != null) throw new AssertionError("Server regression checks", failure);
            if (launched && ++ticks > 1800) throw new AssertionError("QA timeout");
        } catch (Throwable error) {
            finish(error);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ready || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!galaxyCaptured && LightHunterSpaceStationRenderer.getBeamFrames() > 25
            && LightHunterSpaceStationRenderer.getLastPlanetCount() == 13) {
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "station-galaxy.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            galaxyCaptured = true;
        }
        if (!(mc.currentScreen instanceof GuiContainerWrapper gui) || ++frames < 20) return;
        if (frames == 20) {
            var handle = gui.getScreen()
                .getSyncManager()
                .findPanelHandlerNullable("satelliteVoltage");
            handle.openPanel();
            return;
        }
        if (!voltageEditSent) {
            var panel = gui.getScreen()
                .getSyncManager()
                .findPanelHandlerNullable("satelliteVoltage");
            if (panel == null || !panel.isPanelOpen()) return;
            var manager = gui.getScreen()
                .getSyncManager()
                .getPanelSyncManager("satelliteVoltage");
            var value = (com.cleanroommc.modularui.value.sync.LongSyncValue) manager
                .findSyncHandlerNullable("processingVoltage", 0);
            value.setLongValue(4_500_000_001L);
            voltageEditSent = true;
        }
        if (frames < 40 || !voltageSynced) return;
        try {
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "satellite-voltage.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            finish(null);
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void verifySharedModules(World world, EntityPlayerMP player) {
        BaseMetaTileEntity otherTile = place(world, 9, 41, GTNRMachineID.FluidProcessingSatelliteMkIV.id);
        otherTile.setOwnerUuid(player.getUniqueID());
        otherTile.setFrontFacing(ForgeDirection.WEST);
        NexusSatelliteFactory other = (NexusSatelliteFactory) otherTile.getMetaTileEntity();
        other.construct(new ItemStack(Items.stick), false);
        var shape = com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader
            .load("light_hunter_space_station_module");
        outer: for (int y = 0; y < shape.length; y++) for (int z = 0; z < shape[y].length; z++) {
            int x = shape[y][z].indexOf('O');
            if (x < 0) continue;
            int[] offset = new int[3];
            other.getExtendedFacing()
                .getWorldOffset(new int[] { x - 2, y - 10, z }, offset);
            place(
                world,
                9 + offset[0],
                80 + offset[1],
                41 + offset[2],
                ItemList.Hatch_Maintenance.get(1)
                    .getItemDamage());
            break outer;
        }
        require(
            other.checkStructure(true, otherTile) && other.getHost() == station,
            "second satellite forms and links to the same station");
        int[][] modules = { { -1, 82, GTNRMachineID.StaticParallelControllerT1.id },
            { 0, 82, GTNRMachineID.DynamicSpeedControllerT2.id },
            { 1, 82, GTNRMachineID.PowerConsumptionControllerT1.id },
            { -2, 81, GTNRMachineID.SingularityPerfectOverclockController.id },
            { -1, 81, GTNRMachineID.ThreadControllerT2.id } };
        for (int[] module : modules) place(world, module[0], module[1], 0, module[2]);
        require(station.checkStructure(true, stationTile), "station accepts all five parameter module types");
        var speed = (com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch) ((BaseMetaTileEntity) world
            .getTileEntity(0, 82, 0)).getMetaTileEntity();
        speed.setParameter(4);
        require(
            satellite.getMaxParallelRecipes() == 131072 * 9 && other.getMaxParallelRecipes() == Integer.MAX_VALUE,
            "shared parallel multiplier applies independently and caps safely on Mk IV");
        require(
            satellite.getTotalRecipeThreads() == 2050 && other.getTotalRecipeThreads() == 8388610,
            "both satellites receive the full thread bonus");
        require(
            Math.abs(satellite.getEUtMultiplier() - 0.8 * 0.95) < 1e-9
                && Math.abs(other.getEUtMultiplier() - 0.6 * 0.95) < 1e-9,
            "station EU module multiplies each satellite's own grade discount");
        for (var machine : List.of(satellite, other)) require(
            machine.getSpeedMultiplier() == 4 && machine.getOverclockTimeReduction() == 8
                && machine.getOverclockPowerIncrease() == 2,
            "station speed and overclock apply to " + machine.getLocalName());
        speed.setParameter(2);
        require(
            satellite.getSpeedMultiplier() == 2 && other.getSpeedMultiplier() == 2,
            "live station parameter edit reaches both satellites");
        place(world, 0, 81, 0, GTNRMachineID.StaticSpeedControllerT1.id);
        require(!station.checkStructure(true, stationTile), "station rejects duplicate speed hatches");
        restoreStationCasing(world, 0, 81);
        for (int[] module : modules) restoreStationCasing(world, module[0], module[1]);
        require(station.checkStructure(true, stationTile), "station reforms with empty module positions");
        require(
            satellite.getMaxParallelRecipes() == 131072 && other.getMaxParallelRecipes() == 2147483643
                && satellite.getTotalRecipeThreads() == 2048
                && other.getTotalRecipeThreads() == 8388608
                && satellite.getSpeedMultiplier() == 1
                && other.getSpeedMultiplier() == 1
                && satellite.getOverclockPowerIncrease() == 4
                && other.getOverclockTimeReduction() == 2
                && Math.abs(satellite.getEUtMultiplier() - 0.8) < 1e-9
                && Math.abs(other.getEUtMultiplier() - 0.6) < 1e-9,
            "both satellites revert to base parameters after station hatch removal");
    }

    private static void restoreStationCasing(World world, int x, int y) {
        var casing = gregtech.api.casing.Casings.HighPowerCasing;
        world.setBlock(x, y, 0, casing.getBlock(), casing.getBlockMeta(), 3);
    }

    private void loadCrossMapPatterns(EntityPlayerMP player) {
        var maps = List.of(RecipeMaps.assemblerRecipes, RecipeMaps.latheRecipes, RecipeMaps.compressorRecipes);
        var input = new ItemStack(Items.brick);
        var outputs = List.of(Items.carrot, Items.carrot, Items.reeds);
        for (int index = 0; index < maps.size(); index++) {
            var map = maps.get(index);
            var output = new ItemStack(outputs.get(index));
            if (index > 0) require(
                !GTRecipeBuilder.builder()
                    .itemInputs(input.copy())
                    .itemOutputs(output.copy())
                    .eut(1_000_000_000)
                    .duration(400)
                    .addTo(map)
                    .isEmpty(),
                "fixture recipe registered in " + map.unlocalizedName);
            var encoded = AEApi.instance()
                .definitions()
                .items()
                .encodedUltimatePattern()
                .maybeStack(1)
                .get();
            NBTTagCompound nbt = new NBTTagCompound();
            var in = new NBTTagList();
            var out = new NBTTagList();
            in.appendTag(
                AEItemStack.create(input)
                    .toNBTGeneric());
            out.appendTag(
                AEItemStack.create(output)
                    .toNBTGeneric());
            nbt.setTag("in", in);
            nbt.setTag("out", out);
            encoded.setTagCompound(nbt);
            PatternRecipeMap.write(encoded, map.unlocalizedName);
            patternBuffer.onPatternChange(index, encoded);
            var slots = patternBuffer.inventories();
            for (int i = 0; i < index; i++) slots.next();
            var delivery = new MEInventoryCrafting(player.inventoryContainer, 1, 1);
            ItemStack delivered = input.copy();
            delivered.stackSize = index == 2 ? 5 : 1;
            delivery.setInventorySlotContents(0, AEItemStack.create(delivered));
            require(
                slots.next()
                    .insertItemsAndFluids(delivery),
                "cross-recipe buffer receives pattern input " + index);
        }
    }

    private void verifyLargeWithdrawal() {
        var owner = stationTile.getOwnerUuid();
        BigInteger savedBalance = WirelessNetworkManager.getUserEU(owner);
        NBTTagCompound saved = new NBTTagCompound();
        satellite.saveNBTData(saved);
        try {
            var restored = (NBTTagCompound) saved.copy();
            var threads = new NBTTagList();
            // A saved thread above the current limit is parked when a station thread hatch is removed.
            for (int slot : new int[] { 0, 1, satellite.getTotalRecipeThreads() }) {
                var task = new com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadTask(
                    slot,
                    Long.MAX_VALUE - 1,
                    100,
                    1,
                    new com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadOutputs(
                        null,
                        null));
                threads.appendTag(task.save());
            }
            restored.setTag("gtnrRecipeThreads", threads);
            satellite.loadNBTData(restored);
            satellite.lEUt = -Long.MAX_VALUE;
            BigInteger demand = BigInteger.valueOf(Long.MAX_VALUE - 1)
                .shiftLeft(1);
            WirelessNetworkManager.setUserEU(owner, demand.subtract(BigInteger.ONE));
            require(
                !satellite.onRunningTick(null) && WirelessNetworkManager.getUserEU(owner)
                    .equals(demand.subtract(BigInteger.ONE)),
                "unaffordable demand above Long.MAX_VALUE leaves balance intact");
            WirelessNetworkManager.setUserEU(owner, demand.add(BigInteger.TEN));
            require(
                satellite.onRunningTick(null) && WirelessNetworkManager.getUserEU(owner)
                    .equals(BigInteger.TEN),
                "two high-power threads pay their exact aggregate EU/t and the parked thread costs nothing");
        } finally {
            WirelessNetworkManager.setUserEU(owner, savedBalance);
            satellite.loadNBTData(saved);
        }
    }

    private static BaseMetaTileEntity place(World world, int x, int z, int id) {
        return place(world, x, 80, z, id);
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        return tile;
    }

    private void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        report.append("PASS ")
            .append(message)
            .append('\n');
        System.out.println("[light-hunter-qa] PASS " + message);
    }

    private void finish(Throwable error) {
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(Path.of("report.txt"), report);
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : "FAIL " + error);
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(error == null ? 0 : 1, true);
        }, "light-hunter-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }

}
