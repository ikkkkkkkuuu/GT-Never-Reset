package com.xyp.gtneverreset.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerGhostIngredientSlot;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.multiblock.LargeBeeBreeder;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.machine.bee.BeeBreedingHelper;
import com.xyp.gtneverreset.utils.machine.bee.BeeBreedingPlanner;
import com.xyp.gtneverreset.utils.machine.bee.DronePool;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;

@Mod(modid = "beebreederqa", name = "Bee Breeder QA", version = "1", dependencies = "after:gtneverreset")
public final class BeeBreederClientChecks {

    private final StringBuilder report = new StringBuilder();
    private BaseMetaTileEntity tile;
    private LargeBeeBreeder machine;
    private MTEHatchInputBus input;
    private BaseMetaTileEntity displayEnergy;
    private BaseMetaTileEntity mvEnergy;
    private String forest;
    private String meadows;
    private String common;
    private String cultivated;
    private int ticks;
    private int frames;
    private int stage;
    private int clientTicks;
    private boolean launched;
    private volatile boolean opened;
    private volatile boolean ready;
    private volatile boolean finished;
    private volatile boolean archiveCommon;
    private volatile boolean upgradeVoltage;
    private volatile int progressPreviewAction;
    private int heldProgress = -1;
    private NBTTagCompound progressBaseline;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.beeBreeder.client.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (finished) {
            mc.shutdown();
            return;
        }
        try {
            if (++clientTicks > 4000) throw new AssertionError("client timeout at stage " + stage);
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
            mc.gameSettings.renderDistanceChunks = 3;
            mc.gameSettings.limitFramerate = 60;
            if (mc.thePlayer != null) mc.ingameGUI.getChatGUI()
                .clearChatMessages();
            ready = mc.theWorld != null && mc.theWorld.getTileEntity(0, 10, 0) instanceof BaseMetaTileEntity base
                && base.getMetaTileEntity() instanceof LargeBeeBreeder;
            if (!launched && mc.theWorld == null && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "bee-breeder-" + System.currentTimeMillis(),
                    "Bee Breeder QA",
                    new WorldSettings(746L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (++ticks == 1) setup(player);
            if (upgradeVoltage) {
                upgradeVoltage = false;
                machine.mEnergyHatches.clear();
                displayEnergy = mvEnergy;
            }
            machine.addToMachineList(displayEnergy, 10);
            if (progressPreviewAction == 1) {
                progressPreviewAction = 0;
                resetRoute(progressBaseline, mvEnergy);
                require(
                    machine.checkProcessing()
                        .wasSuccessful(),
                    "progress preview starts a four-species batch");
                heldProgress = 50;
            } else if (progressPreviewAction == 2) {
                progressPreviewAction = 0;
                heldProgress = 100;
            } else if (progressPreviewAction == 3) {
                progressPreviewAction = 0;
                heldProgress = -1;
                machine.stopMachine(ShutDownReasonRegistry.NONE);
            }
            if (heldProgress >= 0) {
                // Hold deterministic native timers while the unformed fixture is being photographed.
                machine.mProgresstime = heldProgress;
                machine.mMaxProgresstime = 200;
                if (heldProgress == 100) {
                    BaseMetaTileEntity speed = (BaseMetaTileEntity) player.worldObj.getTileEntity(14, 10, 0);
                    ((ParameterModuleHatch) speed.getMetaTileEntity()).setParameter(2);
                    machine.addParameterModuleToMachineList(speed, 10);
                }
            }
            if (archiveCommon) {
                archiveCommon = false;
                if (!machine.mInputBusses.contains(input)) machine.mInputBusses.add(input);
                input.setInventorySlotContents(0, BeeBreedingHelper.createDrone(common));
                machine.checkProcessing();
            }
            if (ticks >= 70 && ready && !opened) {
                player.playerNetServerHandler.setPlayerLocation(0.5, 11, -3, 0, 10);
                require(machine.onRightclick(tile, player), "native right-click opens the MUI2 machine GUI");
                opened = true;
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void setup(EntityPlayerMP player) throws Exception {
        forest = BeeBreedingHelper.getSpeciesByName("Forest")
            .getUID();
        meadows = BeeBreedingHelper.getSpeciesByName("Meadows")
            .getUID();
        common = BeeBreedingHelper.getSpeciesByName("Common")
            .getUID();
        cultivated = BeeBreedingHelper.getSpeciesByName("Cultivated")
            .getUID();
        require(
            BeeBreedingPlanner.plan(cultivated, Set.of(forest, meadows))
                .getMissingSpecies()
                .isEmpty(),
            "Forestry multi-step route is reachable from the base parents");
        require(
            !BeeBreedingPlanner.plan(common, Set.of(forest))
                .getMissingSpecies()
                .isEmpty(),
            "missing root parent is reported");
        require(
            BeeBreedingPlanner.plan(common, Set.of(common))
                .getSteps()
                .isEmpty(),
            "archived targets need no mutations");
        List<String> reachable = BeeBreedingPlanner.findReachableSpecies(Set.of(forest, meadows));
        require(
            reachable.contains(common) && reachable.contains(cultivated),
            "reachable species include direct and multi-step descendants");
        require(!reachable.contains(forest) && !reachable.contains(meadows), "reachable species exclude the archive");
        require(
            BeeBreedingPlanner.findReachableSpecies(Set.of())
                .isEmpty(),
            "empty archive has no reachable species");
        require(
            !BeeBreedingPlanner.findReachableSpecies(Set.of(forest))
                .contains(common),
            "missing parent cannot produce a reachable target");
        require(
            !BeeBreedingPlanner.findReachableSpecies(Set.of(forest, meadows, common))
                .contains(common),
            "newly archived species leave the reachable list");
        for (String uid : reachable) {
            require(
                BeeBreedingPlanner.plan(uid, Set.of(forest, meadows))
                    .getMissingSpecies()
                    .isEmpty(),
                "reachable candidate has a complete plan: " + uid);
        }
        DronePool pool = new DronePool();
        pool.unlockSpecies(forest);
        pool.unlockSpecies(meadows);
        require(
            DronePool.fromNBT(pool.toNBT())
                .getAvailableSpecies()
                .equals(pool.getAvailableSpecies()),
            "archive NBT roundtrip");
        require(
            RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .anyMatch(recipe -> {
                    for (ItemStack stack : recipe.mOutputs)
                        if (stack != null && stack.isItemEqual(GTNRItemList.LargeBeeBreeder.get(1))) return true;
                    return false;
                }),
            "controller assembler recipe is registered");

        tile = place(player, 0, GTNRMachineID.LargeBeeBreeder.id);
        machine = (LargeBeeBreeder) tile.getMetaTileEntity();
        machine.fixAllIssues();
        displayEnergy = place(
            player,
            6,
            ItemList.Hatch_Energy_LV.get(1)
                .getItemDamage());
        mvEnergy = place(
            player,
            8,
            ItemList.Hatch_Energy_MV.get(1)
                .getItemDamage());
        machine.addToMachineList(displayEnergy, 10);
        BaseMetaTileEntity inputTile = place(
            player,
            2,
            ItemList.Hatch_Input_Bus_MV.get(1)
                .getItemDamage());
        input = (MTEHatchInputBus) inputTile.getMetaTileEntity();
        machine.mInputBusses.add(input);
        input.setInventorySlotContents(0, BeeBreedingHelper.createDrone(forest));
        input.setInventorySlotContents(1, BeeBreedingHelper.createPrincess(meadows));
        machine.setTargetBeeSpecies(common);
        require(
            machine.checkProcessing()
                .wasSuccessful(),
            "actual input bus starts breeding");
        require(
            machine.getPendingPrincessOutputs() == 1 && machine.getDronePool()
                .hasDrone(forest)
                && machine.getDronePool()
                    .hasDrone(meadows),
            "input bees consumed once into archive and pending output");
        machine.checkProcessing();
        require(machine.getPendingPrincessOutputs() == 1, "zero-size input stacks do not duplicate pending output");
        require(
            !machine.onRunningTick(null) && !machine.getDronePool()
                .hasDrone(common),
            "no energy does not settle a breeding attempt");

        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        progressBaseline = (NBTTagCompound) saved.copy();
        LargeBeeBreeder restored = (LargeBeeBreeder) place(player, 4, GTNRMachineID.LargeBeeBreeder.id)
            .getMetaTileEntity();
        restored.loadNBTData(saved);
        require(
            restored.getPendingPrincessOutputs() == 1 && restored.getDronePool()
                .hasDrone(forest)
                && restored.getTargetBeeSpecies()
                    .equals(common),
            "controller NBT restores target, archive and princess credits");
        machine.setTargetBeeSpecies("not_a_real_species");
        require(
            !machine.isTargetInputValid() && machine.getTargetBeeSpecies()
                .equals(common),
            "invalid target preserves previous selection");

        machine.getDronePool()
            .unlockSpecies(common);
        machine.setTargetBeeSpecies(common);
        require(
            machine.checkProcessing()
                .wasSuccessful() && machine.mOutputItems.length == 2,
            "archived target prepares princess and drone outputs");
        require(machine.getPendingPrincessOutputs() == 1, "princess credit remains until recipe completion");
        Method completion = LargeBeeBreeder.class.getDeclaredMethod("outputAfterRecipe");
        completion.setAccessible(true);
        completion.invoke(machine);
        require(machine.getPendingPrincessOutputs() == 0, "completed output consumes exactly one princess credit");
        completion.invoke(machine);
        require(machine.getPendingPrincessOutputs() == 0, "completion is idempotent");

        checkVoltageAndModules(player, saved, completion);

        // Restore a populated, unfinished route for visual and interaction checks.
        machine.loadNBTData(saved);
        machine.clearHatches();
        machine.addToMachineList(displayEnergy, 10);
        machine.setTargetBeeSpecies(cultivated);
        machine.mProgresstime = 0;
        machine.mMaxProgresstime = 0;
        machine.mOutputItems = null;
        machine.createTooltip();
        require(true, "automatic structure tooltip accepts the custom controller");
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(0.5, 11, -3, 0, 10);
        Files.writeString(new File(output(), "core-result.txt").toPath(), "PASS\n" + report);
    }

    private void checkVoltageAndModules(EntityPlayerMP player, NBTTagCompound saved, Method completion)
        throws Exception {
        BaseMetaTileEntity hv = place(
            player,
            10,
            ItemList.Hatch_Energy_HV.get(1)
                .getItemDamage());
        BaseMetaTileEntity secondLv = place(
            player,
            12,
            ItemList.Hatch_Energy_LV.get(1)
                .getItemDamage());
        resetRoute(saved, displayEnergy);
        require(
            machine.getMaxParallelBreedingSteps() == 2 && machine.getBreedingDurationTicks() == 400,
            "LV base is two species and 20 seconds");
        machine.addToMachineList(secondLv, 10);
        require(
            machine.getMaxParallelBreedingSteps() == 2 && machine.getBreedingDurationTicks() == 400,
            "multiple LV hatches do not increase voltage tier");
        require(
            machine.checkProcessing()
                .wasSuccessful() && machine.getActiveBreedingSpecies() == 2
                && machine.mMaxProgresstime == 400
                && machine.getEUtForDamageCalc() == -30,
            "LV starts two ready species at 30 EU/t");
        NBTTagCompound batch = new NBTTagCompound();
        machine.saveNBTData(batch);
        var attempts = batch.getTagList("beeBreederActiveBatch", 10);
        Set<String> results = new HashSet<>();
        for (int i = 0; i < attempts.tagCount(); i++) results.add(
            attempts.getCompoundTagAt(i)
                .getString("result"));
        require(
            results.size() == 2 && !results.contains(cultivated),
            "parallel slots contain distinct ready species only");
        require(
            new HashSet<>(machine.getActiveBreedingSpeciesUids()).equals(results),
            "active display exposes only the species in the running batch");
        LargeBeeBreeder loaded = (LargeBeeBreeder) place(player, 28, GTNRMachineID.LargeBeeBreeder.id)
            .getMetaTileEntity();
        loaded.loadNBTData(batch);
        require(
            new HashSet<>(loaded.getActiveBreedingSpeciesUids()).equals(results) && loaded.mMaxProgresstime == 400,
            "active species and original cycle duration survive NBT reload");
        completion.invoke(machine);
        require(
            machine.getActiveBreedingSpeciesUids()
                .isEmpty(),
            "completion clears the active species display");
        require(
            machine.getDronePool()
                .hasDrone(common)
                && !machine.getDronePool()
                    .hasDrone(cultivated),
            "dependent route species are not completed in the same batch");

        resetRoute(saved, mvEnergy);
        require(
            machine.getMaxParallelBreedingSteps() == 4 && machine.checkProcessing()
                .wasSuccessful()
                && machine.getActiveBreedingSpecies() == 4
                && machine.mMaxProgresstime == 200
                && machine.getEUtForDamageCalc() == -120,
            "MV runs four ready species in 10 seconds");
        resetRoute(saved, hv);
        require(
            machine.getMaxParallelBreedingSteps() == 8 && machine.checkProcessing()
                .wasSuccessful() && machine.getActiveBreedingSpecies() == 4 && machine.mMaxProgresstime == 100,
            "HV allows eight species in five seconds without filling unused slots with duplicates");

        BaseMetaTileEntity speed = place(player, 14, GTNRMachineID.DynamicSpeedControllerT1.id);
        BaseMetaTileEntity parallel = place(player, 16, GTNRMachineID.DynamicParallelControllerT1.id);
        BaseMetaTileEntity power = place(player, 18, GTNRMachineID.PowerConsumptionControllerT8.id);
        BaseMetaTileEntity overclock = place(player, 20, GTNRMachineID.LowSpeedPerfectOverclockController.id);
        resetRoute(saved, mvEnergy);
        require(machine.addParameterModuleToMachineList(speed, 10), "parameter module hatch is accepted");
        require(
            machine.checkProcessing()
                .wasSuccessful() && machine.mMaxProgresstime == 200,
            "dynamic speed module initially uses its selected parameter");
        ((ParameterModuleHatch) speed.getMetaTileEntity()).setParameter(2);
        require(
            machine.mMaxProgresstime == 200 && machine.getBreedingDurationTicks() == 100,
            "module changes leave the running cycle duration unchanged");
        machine.checkProcessing();
        require(machine.mMaxProgresstime == 100, "speed module applies to the next cycle");
        machine.addParameterModuleToMachineList(parallel, 10);
        ((ParameterModuleHatch) parallel.getMetaTileEntity()).setParameter(8);
        require(machine.getMaxParallelBreedingSteps() == 36, "parallel module multiplies distinct-species capacity");
        machine.addParameterModuleToMachineList(power, 10);
        require(
            machine.getEuPerBreedingAttempt() == 8,
            "power module reduces per-species consumption with safe rounding");
        machine.addParameterModuleToMachineList(overclock, 10);
        require(
            machine.getBreedingDurationTicks() == 50 && machine.checkProcessing()
                .wasSuccessful() && -machine.getEUtForDamageCalc() <= machine.getMaxInputEu(),
            "overclock module changes tier duration and batch stays within available power");
        List<StructureError> before = new ArrayList<>();
        machine.collectTooltipStructureRequirements(before);
        machine.addParameterModuleToMachineList(place(player, 22, GTNRMachineID.StaticSpeedControllerT1.id), 10);
        List<StructureError> after = new ArrayList<>();
        machine.collectTooltipStructureRequirements(after);
        require(after.size() == before.size() + 1, "duplicate module types reject structure formation");

        resetRoute(saved, hv);
        machine.addParameterModuleToMachineList(place(player, 24, GTNRMachineID.StaticSpeedControllerT8.id), 10);
        machine.addParameterModuleToMachineList(place(player, 26, GTNRMachineID.StaticParallelControllerT8.id), 10);
        require(
            machine.getBreedingDurationTicks() == 1 && machine.getMaxParallelBreedingSteps() == Integer.MAX_VALUE,
            "extreme module settings clamp duration and parallel capacity safely");
        machine.clearHatches();
        require(
            !machine.checkProcessing()
                .wasSuccessful(),
            "unpowered controller cannot start a breeding cycle");
    }

    private void resetRoute(NBTTagCompound saved, BaseMetaTileEntity energy) throws Exception {
        machine.loadNBTData(saved);
        machine.clearHatches();
        machine.addToMachineList(energy, 10);
        machine.setTargetBeeSpecies(cultivated);
        machine.mProgresstime = 0;
        machine.mMaxProgresstime = 0;
        machine.mOutputItems = null;
        // Controlled branching DAG isolates the scheduler from Forestry's actual mutation choices.
        String diligent = BeeBreedingHelper.getSpeciesByName("Diligent")
            .getUID();
        String noble = BeeBreedingHelper.getSpeciesByName("Noble")
            .getUID();
        String majestic = BeeBreedingHelper.getSpeciesByName("Majestic")
            .getUID();
        String unweary = BeeBreedingHelper.getSpeciesByName("Unweary")
            .getUID();
        String imperial = BeeBreedingHelper.getSpeciesByName("Imperial")
            .getUID();
        List<BeeBreedingHelper.BreedingStep> steps = List.of(
            new BeeBreedingHelper.BreedingStep(forest, meadows, common, 100),
            new BeeBreedingHelper.BreedingStep(forest, meadows, diligent, 100),
            new BeeBreedingHelper.BreedingStep(forest, meadows, noble, 100),
            new BeeBreedingHelper.BreedingStep(forest, meadows, majestic, 100),
            new BeeBreedingHelper.BreedingStep(common, diligent, unweary, 100),
            new BeeBreedingHelper.BreedingStep(noble, majestic, imperial, 100),
            new BeeBreedingHelper.BreedingStep(unweary, imperial, cultivated, 100));
        var constructor = BeeBreedingPlanner.Plan.class
            .getDeclaredConstructor(String.class, List.class, Set.class, double.class);
        constructor.setAccessible(true);
        setMachineField("breedingPlan", constructor.newInstance(cultivated, steps, Set.of(), 0D));
        setMachineField("breedingChain", new ArrayList<>(steps));
        setMachineField("breedingPlanDirty", false);
    }

    private void setMachineField(String name, Object value) throws Exception {
        Field field = LargeBeeBreeder.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(machine, value);
    }

    private BaseMetaTileEntity place(EntityPlayerMP player, int x, int id) {
        player.worldObj.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity base = (BaseMetaTileEntity) player.worldObj.getTileEntity(x, 10, 0);
        base.setInitialValuesAsNBT(null, (short) id);
        base.setFrontFacing(ForgeDirection.NORTH);
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        base.disableWorking();
        return base;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !opened) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper) || ++frames < 45) return;
            frames = 0;
            var panel = wrapper.getScreen()
                .getMainPanel();
            var sync = wrapper.getScreen()
                .getSyncManager()
                .getMainPSM();
            PagedWidget<?> pages = collect(panel, PagedWidget.class).get(0);
            List<PageButton> tabs = collect(panel, PageButton.class);
            require(
                tabs.size() == 4 && pages.getPages()
                    .size() == 4,
                "four MUI2 pages");
            switch (stage++) {
                case 0 -> {
                    require(panel.getArea().width == 360 && panel.getArea().height == 348, "panel bounds");
                    Files.writeString(new File(output(), "operation-layout.txt").toPath(), layout(panel, ""));
                    capture("bee-operation.png");
                    require(
                        sync.findSyncHandler("beeParallelSpecies", IntSyncValue.class)
                            .getIntValue() == 2
                            && sync.findSyncHandler("beeDuration", IntSyncValue.class)
                                .getIntValue() == 400,
                        "LV species capacity and duration are synchronized to the GUI");
                    tabs.get(1)
                        .onMousePressed(0);
                }
                case 1 -> {
                    require(pages.getCurrentPageIndex() == 1, "archive tab switches locally");
                    capture("bee-archive.png");
                    search(pages, forest);
                }
                case 2 -> {
                    capture("bee-archive-search.png");
                    collect(pages.getCurrentPage(), ButtonWidget.class).get(0)
                        .onMousePressed(0);
                }
                case 3 -> {
                    require(pages.getCurrentPageIndex() == 0, "selecting archived species returns to operation");
                    require(
                        sync.findSyncHandler("beeDraft", StringSyncValue.class)
                            .getStringValue()
                            .equals(forest),
                        "archive selection fills UID draft");
                    require(
                        machine.getTargetBeeSpecies()
                            .equals(cultivated),
                        "draft selection does not alter machine target");
                    sync.findSyncHandler("beeDraft", StringSyncValue.class)
                        .setStringValue("not_a_real_species");
                }
                case 4 -> apply(pages);
                case 5 -> {
                    require(
                        !sync.findSyncHandler("beeValid", BooleanSyncValue.class)
                            .getBoolValue(),
                        "invalid target feedback reaches client");
                    require(
                        machine.getTargetBeeSpecies()
                            .equals(cultivated),
                        "invalid GUI target rejected on server");
                    capture("bee-invalid-target.png");
                    IWidget field = collect(pages.getCurrentPage(), TextFieldWidget.class).get(0);
                    @SuppressWarnings("unchecked")
                    RecipeViewerGhostIngredientSlot<ItemStack> drop = (RecipeViewerGhostIngredientSlot<ItemStack>) field;
                    require(
                        drop.handleDragAndDrop(BeeBreedingHelper.createDrone(common), 0),
                        "NEI bee ghost drag accepted");
                }
                case 6 -> {
                    require(
                        sync.findSyncHandler("beeDraft", StringSyncValue.class)
                            .getStringValue()
                            .equals(common),
                        "NEI drag keeps species UID");
                    apply(pages);
                }
                case 7 -> {
                    require(
                        machine.getTargetBeeSpecies()
                            .equals(common),
                        "Apply confirms target on server");
                    tabs.get(3)
                        .onMousePressed(0);
                }
                case 8 -> {
                    require(pages.getCurrentPageIndex() == 3, "route tab switches");
                    capture("bee-route.png");
                    tabs.get(0)
                        .onMousePressed(0);
                    sync.findSyncHandler("beeDraft", StringSyncValue.class)
                        .setStringValue(BeeBreedingHelper.getSpeciesDisplayName(cultivated));
                }
                case 9 -> apply(pages);
                case 10 -> {
                    require(
                        machine.getTargetBeeSpecies()
                            .equals(cultivated),
                        "localized name resolves to canonical UID before server confirmation");
                    tabs.get(3)
                        .onMousePressed(0);
                }
                case 11 -> {
                    capture("bee-route-multistep.png");
                    Files.writeString(new File(output(), "route-layout.txt").toPath(), layout(panel, ""));
                    tabs.get(2)
                        .onMousePressed(0);
                }
                case 12 -> {
                    require(pages.getCurrentPageIndex() == 2, "reachable tab switches");
                    List<?> candidates = sync.findSyncHandler("beeReachable", GenericListSyncHandler.class)
                        .getValue();
                    require(
                        candidates.contains(common) && candidates.contains(cultivated)
                            && !candidates.contains(forest)
                            && !candidates.contains(meadows),
                        "server synchronizes reachable but unarchived species");
                    capture("bee-reachable.png");
                    search(pages, common);
                }
                case 13 -> {
                    capture("bee-reachable-search.png");
                    collect(pages.getCurrentPage(), ButtonWidget.class).get(0)
                        .onMousePressed(0);
                }
                case 14 -> {
                    require(
                        pages.getCurrentPageIndex() == 0 && sync.findSyncHandler("beeDraft", StringSyncValue.class)
                            .getStringValue()
                            .equals(common),
                        "reachable search and selection fill the target draft");
                    require(
                        machine.getTargetBeeSpecies()
                            .equals(cultivated),
                        "reachable selection waits for Apply");
                    tabs.get(2)
                        .onMousePressed(0);
                    search(pages, "");
                    archiveCommon = true;
                }
                case 15 -> {
                    List<?> candidates = sync.findSyncHandler("beeReachable", GenericListSyncHandler.class)
                        .getValue();
                    List<?> archived = sync.findSyncHandler("beeArchive", GenericListSyncHandler.class)
                        .getValue();
                    require(
                        archived.contains(common) && !candidates.contains(common) && candidates.contains(cultivated),
                        "input bus archive update removes only the unlocked candidate from the live GUI");
                    capture("bee-reachable-updated.png");
                    tabs.get(0)
                        .onMousePressed(0);
                    upgradeVoltage = true;
                }
                case 16 -> {
                    require(
                        sync.findSyncHandler("beeParallelSpecies", IntSyncValue.class)
                            .getIntValue() == 4
                            && sync.findSyncHandler("beeDuration", IntSyncValue.class)
                                .getIntValue() == 200,
                        "voltage upgrade refreshes displayed species capacity and duration");
                    capture("bee-operation-mv.png");
                    progressPreviewAction = 1;
                }
                case 17 -> {
                    List<?> active = sync.findSyncHandler("beeActiveBreeding", GenericListSyncHandler.class)
                        .getValue();
                    require(
                        active.size() == 4 && active.contains(common) && !active.contains(cultivated),
                        "GUI lists the four active species instead of the selected final target");
                    require(
                        sync.findSyncHandler("progressTime", IntSyncValue.class)
                            .getIntValue() == 50
                            && sync.findSyncHandler("maxProgressTime", IntSyncValue.class)
                                .getIntValue() == 200,
                        "GUI receives actual batch timing at 25 percent");
                    capture("bee-active-progress-25.png");
                    progressPreviewAction = 2;
                }
                case 18 -> {
                    require(
                        sync.findSyncHandler("progressTime", IntSyncValue.class)
                            .getIntValue() == 100
                            && sync.findSyncHandler("maxProgressTime", IntSyncValue.class)
                                .getIntValue() == 200
                            && sync.findSyncHandler("beeDuration", IntSyncValue.class)
                                .getIntValue() == 100,
                        "running progress uses original cycle duration after a speed module change");
                    capture("bee-active-progress-50.png");
                    collect(pages.getCurrentPage(), ButtonWidget.class).get(2)
                        .onMousePressed(0);
                }
                case 19 -> {
                    capture("bee-active-progress-page-2.png");
                    progressPreviewAction = 3;
                }
                case 20 -> {
                    require(
                        sync.findSyncHandler("beeActiveBreeding", GenericListSyncHandler.class)
                            .getValue()
                            .isEmpty()
                            && sync.findSyncHandler("maxProgressTime", IntSyncValue.class)
                                .getIntValue() == 0,
                        "stopping clears active species and timing on the client");
                    capture("bee-active-progress-stopped.png");
                    Files.writeString(new File(output(), "client-result.txt").toPath(), "PASS\n" + report);
                    finished = true;
                }
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    private static void apply(PagedWidget<?> pages) {
        collect(pages.getCurrentPage(), ButtonWidget.class).get(0)
            .onMousePressed(0);
    }

    private static void search(PagedWidget<?> pages, String text) throws Exception {
        TextFieldWidget search = collect(pages.getCurrentPage(), TextFieldWidget.class).get(0);
        search.setText(text);
        Method changed = TextFieldWidget.class.getDeclaredMethod("onTextChanged");
        changed.setAccessible(true);
        changed.invoke(search);
    }

    private static <T> List<T> collect(IWidget widget, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(widget)) result.add(type.cast(widget));
        for (IWidget child : widget.getChildren()) result.addAll(collect(child, type));
        return result;
    }

    private static String layout(IWidget widget, String indent) {
        StringBuilder result = new StringBuilder(indent).append(
            widget.getClass()
                .getSimpleName())
            .append(" enabled=")
            .append(widget.isEnabled())
            .append(" ")
            .append(widget.getArea())
            .append('\n');
        for (IWidget child : widget.getChildren()) result.append(layout(child, indent + "  "));
        return result.toString();
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        report.append(message)
            .append('\n');
    }

    private static File output() {
        File output = new File(System.getProperty("gtnr.beeBreeder.client.qa.output"));
        output.mkdirs();
        return output;
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void fail(Throwable error) {
        if (finished) return;
        error.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            error.printStackTrace(new PrintWriter(trace));
            Files.writeString(new File(output(), "client-result.txt").toPath(), "FAIL\n" + trace + "\n" + report);
        } catch (Exception ignored) {}
        finished = true;
    }
}
