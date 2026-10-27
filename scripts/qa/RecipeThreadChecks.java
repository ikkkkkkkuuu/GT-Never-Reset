package com.xyp.gtneverreset.qa;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.mojang.authlib.GameProfile;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.multiMachineBase.GTNRMultiBlockBase;
import com.xyp.gtneverreset.loader.MachineLoader;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.machine.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.utils.machine.RecipeThreadTask;
import com.xyp.gtneverreset.utils.structure.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.utils.structure.StructureTooltipReader;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.VoidingMode;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.objects.GTDualInputPattern;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.OverclockCalculator;
import gregtech.common.tileentities.machines.IDualInputInventoryWithPattern;

@Mod(modid = "recipethreadqa", name = "Recipe Thread QA", version = "1", dependencies = "after:" + GTNeverReset.MODID)
public final class RecipeThreadChecks {

    private static final int FIXTURE_ID = 29500;
    private static RecipeMap<?> RECIPES;
    public static NBTTagCompound wailaSnapshot;
    private boolean finished;
    private int checks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtnr.thread.qa")) return;
        RECIPES = RecipeMapBuilder.of("gtnr.thread.qa")
            .maxIO(1, 1, 1, 1)
            .minInputs(1, 0)
            .build();
        new Fixture(FIXTURE_ID);
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.iron_ingot))
            .itemOutputs(new ItemStack(Items.gold_nugget))
            .duration(200)
            .eut(8)
            .addTo(RECIPES);
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.gold_ingot))
            .itemOutputs(new ItemStack(Items.diamond))
            .duration(400)
            .eut(8)
            .addTo(RECIPES);
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.coal, 2))
            .fluidInputs(new FluidStack(FluidRegistry.WATER, 100))
            .itemOutputs(new ItemStack(Items.emerald, 2))
            .fluidOutputs(new FluidStack(FluidRegistry.LAVA, 40))
            .duration(20)
            .eut(8)
            .addTo(RECIPES);
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.blaze_powder, 3))
            .fluidInputs(new FluidStack(FluidRegistry.LAVA, 50))
            .itemOutputs(new ItemStack(Items.ender_pearl, 4))
            .fluidOutputs(new FluidStack(FluidRegistry.WATER, 30))
            .duration(40)
            .eut(16)
            .addTo(RECIPES);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (Boolean.getBoolean("gtnr.thread.client.qa")) return;
        if (finished || event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.worldServerForDimension(0) == null) return;
        finished = true;
        String result = "PASS";
        try {
            verify(server.worldServerForDimension(0));
            System.out.println("RECIPE_THREAD_QA PASS: " + checks + " assertions");
        } catch (Throwable failure) {
            failure.printStackTrace();
            result = "FAIL: " + failure;
        }
        try {
            File output = new File(System.getProperty("gtnr.thread.qa.output"));
            output.mkdirs();
            Files.write(new File(output, "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        server.initiateShutdown();
    }

    public void verify(World world) throws Exception {
        world.getChunkFromChunkCoords(0, 0);
        Fixture controller = (Fixture) place(world, 0, new ItemStack(GregTechAPI.sBlockMachines, 1, FIXTURE_ID))
            .getMetaTileEntity();
        controller.ready();
        MTEHatchInputBus first = (MTEHatchInputBus) place(world, 1, ItemList.Hatch_Input_Bus_LV.get(1))
            .getMetaTileEntity();
        MTEHatchInputBus second = (MTEHatchInputBus) place(world, 2, ItemList.Hatch_Input_Bus_LV.get(1))
            .getMetaTileEntity();
        MTEHatchOutputBus output = (MTEHatchOutputBus) place(world, 3, ItemList.Hatch_Output_Bus_LV.get(1))
            .getMetaTileEntity();
        controller.mInputBusses.add(first);
        controller.mInputBusses.add(second);
        controller.mOutputBusses.add(output);
        controller.setInputSeparation(true);
        controller.setVoidingMode(VoidingMode.VOID_NONE);
        first.setInventorySlotContents(0, new ItemStack(Items.iron_ingot));
        second.setInventorySlotContents(0, new ItemStack(Items.gold_ingot));
        controller.step(5);
        require(
            controller.getRecipeThreadTasks()
                .size() == 2,
            "two independent recipes start together");
        require(
            controller.getRunningRecipeThreads() == 2 && controller.getTotalRecipeThreads() == 2,
            "running / total thread count is 2 / 2");
        require(
            controller.getRecipeThreadTasks()
                .stream()
                .map(RecipeThreadTask::getDuration)
                .distinct()
                .count() == 2,
            "different recipes keep their own durations");
        controller.step(6);
        require(
            controller.getRecipeThreadTasks()
                .stream()
                .allMatch(task -> task.getProgress() == 1),
            "both recipes advance in one server tick");
        require(controller.spent == 16, "power is summed across threads");

        controller.getBaseMetaTileEntity()
            .setActive(true);
        NBTTagCompound waila = new NBTTagCompound();
        controller.getWailaNBTData(
            FakePlayerFactory.get(
                (WorldServer) world,
                new GameProfile(java.util.UUID.fromString("b56cf8e6-ce3c-4c10-ab0c-1825cbd5984d"), "ThreadQA")),
            (BaseMetaTileEntity) controller.getBaseMetaTileEntity(),
            waila,
            world,
            0,
            10,
            0);
        require(
            waila.getInteger("gtnrRunningThreads") == 2 && waila.getInteger("gtnrTotalThreads") == 2,
            "Waila receives authoritative current / total values");
        wailaSnapshot = (NBTTagCompound) waila.copy();
        controller.threads = 1;
        controller.step(7);
        require(
            controller.getRecipeThreadTasks()
                .get(0)
                .getProgress() == 2
                && controller.getRecipeThreadTasks()
                    .get(1)
                    .getProgress() == 1,
            "reduced capacity pauses excess work");
        require(
            controller.getRunningRecipeThreads() == 1 && controller.getTotalRecipeThreads() == 1,
            "live capacity updates Waila counters");
        controller.threads = 2;
        controller.step(8);
        require(
            controller.getRecipeThreadTasks()
                .get(1)
                .getProgress() == 2,
            "increased capacity resumes stored work");
        controller.funded = false;
        controller.step(9);
        require(
            controller.getRunningRecipeThreads() == 0 && controller.getRecipeThreadTasks()
                .size() == 2,
            "power failure pauses work without deleting inputs or outputs");
        controller.funded = true;
        controller.getBaseMetaTileEntity()
            .enableWorking();
        controller.step(10);

        NBTTagCompound saved = new NBTTagCompound();
        controller.saveNBTData(saved);
        Fixture restored = (Fixture) place(world, 10, new ItemStack(GregTechAPI.sBlockMachines, 1, FIXTURE_ID))
            .getMetaTileEntity();
        restored.loadNBTData(saved);
        require(
            restored.getRecipeThreadTasks()
                .size() == 2,
            "all tasks survive NBT reload");
        require(
            restored.getRecipeThreadTasks()
                .get(1)
                .getProgress()
                == controller.getRecipeThreadTasks()
                    .get(1)
                    .getProgress(),
            "progress survives NBT reload");

        for (int slot = 0; slot < output.getSizeInventory(); slot++) {
            output.setInventorySlotContents(slot, new ItemStack(Items.stick, 64));
        }
        for (long tick = 11; tick < 450; tick++) controller.step(tick);
        require(
            controller.getRecipeThreadTasks()
                .size() == 2 && controller.getRecipeThreadTasks()
                    .stream()
                    .allMatch(RecipeThreadTask::isFinished),
            "blocked outputs stay buffered in completed threads");
        long before = controller.spent;
        controller.step(450);
        require(
            controller.spent == before && controller.getRunningRecipeThreads() == 0,
            "completed blocked tasks consume no power and are not counted as running");
        for (int slot = 0; slot < output.getSizeInventory(); slot++) output.setInventorySlotContents(slot, null);
        controller.step(451);
        require(
            controller.getRecipeThreadTasks()
                .isEmpty(),
            "unblocked output releases threads");
        int nuggets = 0, diamonds = 0;
        for (int slot = 0; slot < output.getSizeInventory(); slot++) {
            ItemStack stack = output.getStackInSlot(slot);
            if (stack == null) continue;
            if (stack.getItem() == Items.gold_nugget) nuggets += stack.stackSize;
            if (stack.getItem() == Items.diamond) diamonds += stack.stackSize;
        }
        require(nuggets == 1 && diamonds == 1, "each recipe delivers its output once");
        controller.step(452);
        require(
            controller.getRecipeThreadTasks()
                .isEmpty(),
            "finished work is not duplicated");

        ParameterModuleHatch speed = module(world, 4, GTNRItemList.DynamicSpeedControllerT3);
        speed.setParameter(Integer.MAX_VALUE);
        require(speed.getParameter() == 8, "dynamic parameters are clamped on the server");
        NBTTagCompound moduleTag = new NBTTagCompound();
        speed.saveNBTData(moduleTag);
        ParameterModuleHatch copy = (ParameterModuleHatch) speed.newMetaEntity(null);
        copy.loadNBTData(moduleTag);
        require(copy.getParameter() == 8, "dynamic parameter NBT round trip");
        controller.addParameterModuleToMachineList(speed.getBaseMetaTileEntity(), 0);
        controller.addParameterModuleToMachineList(
            module(world, 5, GTNRItemList.StaticParallelControllerT1).getBaseMetaTileEntity(),
            0);
        controller.addParameterModuleToMachineList(
            module(world, 6, GTNRItemList.PowerConsumptionControllerT8).getBaseMetaTileEntity(),
            0);
        List<StructureError> errors = new ArrayList<>();
        controller.checkMachine(controller.getBaseMetaTileEntity(), null, errors);
        require(errors.isEmpty(), "one of each parameter type is accepted");
        require(
            controller.getSpeedMultiplier() == 8 && controller.getMaxParallelRecipes() == 9
                && controller.getEUtMultiplier() == 0.25,
            "all three modules affect processing parameters");
        controller.addParameterModuleToMachineList(
            module(world, 7, GTNRItemList.StaticSpeedControllerT1).getBaseMetaTileEntity(),
            0);
        controller.checkMachine(controller.getBaseMetaTileEntity(), null, errors);
        require(errors.size() == 1, "static plus dynamic speed is rejected as a duplicate type");
        controller.addParameterModuleToMachineList(
            module(world, 8, GTNRItemList.PowerConsumptionControllerT1).getBaseMetaTileEntity(),
            0);
        errors.clear();
        controller.checkMachine(controller.getBaseMetaTileEntity(), null, errors);
        require(errors.size() == 2, "two EU modules are rejected even at different tiers");

        int registrations = 0;
        for (GTNRItemList[] group : List.of(
            MachineLoader.STATIC_PARALLEL,
            MachineLoader.DYNAMIC_PARALLEL,
            MachineLoader.STATIC_SPEED,
            MachineLoader.DYNAMIC_SPEED,
            MachineLoader.POWER)) {
            for (GTNRItemList entry : group) {
                ItemStack item = entry.get(1);
                require(
                    GregTechAPI.METATILEENTITIES[item.getItemDamage()] instanceof ParameterModuleHatch,
                    "parameter module registered: " + entry);
                long recipes = RecipeMaps.assemblerRecipes.getAllRecipes()
                    .stream()
                    .filter(
                        recipe -> java.util.Arrays.stream(recipe.mOutputs)
                            .anyMatch(stack -> GTUtility.areStacksEqual(stack, item)))
                    .count();
                require(recipes == 1, "one survival recipe: " + entry + " count=" + recipes);
                registrations++;
            }
        }
        require(registrations == 40, "exactly the 40 EU / speed / parallel variants");

        Fixture detached = new Fixture();
        var summary = StructureTooltipReader.readStructure(detached);
        require(detached.constructCalls == 0, "tooltip probes a detached copy without mutating the caller");
        require(
            summary.size()
                .width() == 3
                && summary.size()
                    .height() == 3
                && summary.size()
                    .depth() == 3
                && summary.hollow(),
            "automatic dimensions and hollow structure");
        require(
            summary.blocks()
                .size() == 1 && summary.blocks()
                    .get(0)
                    .count()
                    .equals("20-25"),
            "deduplicated overlapping structure pieces and real casing count");
        require(
            summary.hatches()
                .stream()
                .allMatch(
                    hatch -> hatch.replaces()
                        .equals(List.of(new ItemStack(Blocks.cobblestone).getDisplayName()))),
            "hatch replacement uses the actual casing name");
        require(
            summary.hatches()
                .stream()
                .anyMatch(
                    hatch -> "1-2".equals(hatch.count()) && hatch.nameKeys()
                        .contains(Energy.getDescriptionLangKey())),
            "real hatch minimum and maximum");
        var tooltip = new AutoMultiblockTooltipBuilder().addAutoTooltip(detached)
            .toolTipFinisher();
        require(
            tooltip.getInformation().length > 1 && tooltip.getStructureInformation().length > 3,
            "automatic normal / Shift descriptions are generated");
        require(
            !StatCollector.translateToLocal("gtnr.multiblock.threads")
                .contains("gtnr.multiblock.threads"),
            "thread display translations are generated");
        verifyOverclockModules(world);
        checks += OverclockRecipeChecks.verify();
        verifyProcessingModes(world);
        verifyIndependentBatches(world);
        verifyBatchDisabled(world);
        verifyRecipeLocking(world);
    }

    private void verifyProcessingModes(World world) {
        Fixture controller = fixture(world, 40);
        require(
            controller.supportsBatchMode() && controller.supportsInputSeparation()
                && controller.supportsSingleRecipeLocking()
                && controller.supportsVoidProtection(),
            "all four processing modes remain available to players");
        requireDefaults(controller, "new controller");
        controller.loadNBTData(new NBTTagCompound());
        requireDefaults(controller, "missing saved mode keys");
        controller.setBatchMode(false);
        controller.setInputSeparation(false);
        controller.setRecipeLocking(true);
        controller.setVoidingMode(VoidingMode.VOID_ALL);
        require(
            !controller.isBatchModeEnabled() && !controller.isInputSeparationEnabled()
                && controller.isRecipeLockingEnabled()
                && controller.getVoidingMode() == VoidingMode.VOID_ALL,
            "players can reverse all four default processing modes");
        NBTTagCompound saved = new NBTTagCompound();
        controller.saveNBTData(saved);
        Fixture restored = fixture(world, 41);
        restored.loadNBTData(saved);
        require(
            !restored.isBatchModeEnabled() && !restored.isInputSeparationEnabled()
                && restored.isRecipeLockingEnabled()
                && restored.getVoidingMode() == VoidingMode.VOID_ALL,
            "explicit mode selections survive NBT reload");
        restored.setBatchMode(true);
        restored.setInputSeparation(true);
        restored.setRecipeLocking(false);
        restored.setVoidingMode(VoidingMode.VOID_NONE);
        requireDefaults(restored, "manually restored modes");
    }

    private void requireDefaults(Fixture controller, String label) {
        require(
            controller.isBatchModeEnabled() && controller.isInputSeparationEnabled()
                && !controller.isRecipeLockingEnabled()
                && controller.getVoidingMode() == VoidingMode.VOID_NONE,
            label + " use batch and separation, without locking or voiding");
    }

    private void verifyIndependentBatches(World world) {
        BatchFixture batch = batchFixture(world, 48);
        Fixture controller = batch.controller();
        controller.step(5);
        require(
            controller.getRecipeThreadTasks()
                .size() == 2,
            "two different recipes start their own batches");
        List<RecipeThreadTask> tasks = controller.getRecipeThreadTasks();
        require(
            tasks.get(0)
                .getParallels() == 3
                && tasks.get(0)
                    .getDuration() == 60
                && tasks.get(0)
                    .getEUt() == 8,
            "first thread batches three 20-tick recipes at 8 EU/t");
        require(
            tasks.get(1)
                .getParallels() == 2
                && tasks.get(1)
                    .getDuration() == 80
                && tasks.get(1)
                    .getEUt() == 16,
            "second thread independently batches two 40-tick recipes at 16 EU/t");
        require(
            itemCount(batch.first(), Items.coal) == 1 && itemCount(batch.second(), Items.blaze_powder) == 2
                && batch.water()
                    .getFluidAmount() == 25
                && batch.lava()
                    .getFluidAmount() == 25,
            "both batches commit exactly their complete item and fluid ingredients");
        require(controller.spent == 0, "starting batches does not charge a processing tick");
        controller.step(6);
        require(controller.spent == 24, "concurrent batches draw the sum of their EU/t, without a batch multiplier");
        controller.setBatchMode(false);
        NBTTagCompound saved = new NBTTagCompound();
        controller.saveNBTData(saved);
        Fixture restored = fixture(world, 57);
        restored.loadNBTData(saved);
        batch.attach(restored);
        require(
            !restored.isBatchModeEnabled() && restored.getRecipeThreadTasks()
                .size() == 2
                && restored.getRecipeThreadTasks()
                    .get(0)
                    .getProgress() == 1
                && restored.getRecipeThreadTasks()
                    .get(1)
                    .getProgress() == 1
                && restored.getRecipeThreadTasks()
                    .get(0)
                    .getParallels() == 3
                && restored.getRecipeThreadTasks()
                    .get(1)
                    .getParallels() == 2,
            "disabling batch mode and reloading preserves already committed batch tasks");
        for (int slot = 0; slot < batch.items()
            .getSizeInventory(); slot++)
            batch.items()
                .setInventorySlotContents(slot, new ItemStack(Items.stick, 64));
        batch.firstOutput().mFluid = new FluidStack(
            FluidRegistry.WATER,
            batch.firstOutput()
                .getCapacity());
        batch.secondOutput().mFluid = new FluidStack(
            FluidRegistry.WATER,
            batch.secondOutput()
                .getCapacity());
        for (long tick = 7; tick <= 85; tick++) restored.step(tick);
        require(
            restored.getRecipeThreadTasks()
                .size() == 2 && restored.getRecipeThreadTasks()
                    .stream()
                    .allMatch(RecipeThreadTask::isFinished),
            "completed item and fluid batches stay buffered while their outputs are blocked");
        require(
            controller.spent + restored.spent == 1760,
            "batch energy is exactly 8 * 60 + 16 * 80 EU, including time before reload");
        long spent = restored.spent;
        restored.step(86);
        require(restored.spent == spent, "completed blocked batches draw no more energy");
        for (int slot = 0; slot < batch.items()
            .getSizeInventory(); slot++)
            batch.items()
                .setInventorySlotContents(slot, null);
        restored.step(87);
        require(
            itemCount(batch.items(), Items.emerald) == 0 && itemCount(batch.items(), Items.ender_pearl) == 0,
            "blocked fluid outputs cannot commit only the batch item outputs");
        batch.firstOutput().mFluid = null;
        batch.secondOutput().mFluid = null;
        restored.step(88);
        require(
            restored.getRecipeThreadTasks()
                .isEmpty(),
            "unblocking both output types releases both batch threads");
        require(
            itemCount(batch.items(), Items.emerald) == 6 && itemCount(batch.items(), Items.ender_pearl) == 8
                && fluidCount(batch, FluidRegistry.LAVA) == 120
                && fluidCount(batch, FluidRegistry.WATER) == 60,
            "independent batches deliver exactly six emeralds, eight pearls, 120 lava and 60 water");
        restored.step(90);
        require(
            itemCount(batch.items(), Items.emerald) == 6 && itemCount(batch.items(), Items.ender_pearl) == 8
                && fluidCount(batch, FluidRegistry.LAVA) == 120
                && fluidCount(batch, FluidRegistry.WATER) == 60
                && restored.spent == spent,
            "completed batches neither duplicate outputs nor consume their incomplete leftover ingredients");
        System.out.println(
            "RECIPE_THREAD_BATCH_QA: batches=" + tasks.get(0)
                .getParallels()
                + "/"
                + tasks.get(1)
                    .getParallels()
                + ", duration="
                + tasks.get(0)
                    .getDuration()
                + "/"
                + tasks.get(1)
                    .getDuration()
                + ", EU/t="
                + tasks.get(0)
                    .getEUt()
                + "/"
                + tasks.get(1)
                    .getEUt()
                + ", energy="
                + (controller.spent + restored.spent)
                + ", emeralds="
                + itemCount(batch.items(), Items.emerald)
                + ", pearls="
                + itemCount(batch.items(), Items.ender_pearl)
                + ", lava="
                + fluidCount(batch, FluidRegistry.LAVA)
                + ", water="
                + fluidCount(batch, FluidRegistry.WATER));
    }

    private void verifyBatchDisabled(World world) {
        BatchFixture batch = batchFixture(world, 64);
        Fixture controller = batch.controller();
        controller.setBatchMode(false);
        // Leave only one first-recipe input so the second thread can select the other recipe.
        batch.first()
            .setInventorySlotContents(0, new ItemStack(Items.coal, 2));
        batch.water().mFluid = new FluidStack(FluidRegistry.WATER, 100);
        controller.step(5);
        require(
            controller.getRecipeThreadTasks()
                .size() == 2
                && controller.getRecipeThreadTasks()
                    .get(0)
                    .getParallels() == 1
                && controller.getRecipeThreadTasks()
                    .get(0)
                    .getDuration() == 20
                && controller.getRecipeThreadTasks()
                    .get(1)
                    .getParallels() == 1
                && controller.getRecipeThreadTasks()
                    .get(1)
                    .getDuration() == 40,
            "batch mode can be disabled without disabling cross-recipe threads");
        require(
            itemCount(batch.first(), Items.coal) == 0 && itemCount(batch.second(), Items.blaze_powder) == 5
                && batch.water()
                    .getFluidAmount() == 0
                && batch.lava()
                    .getFluidAmount() == 75,
            "disabled batches consume only one recipe's item and fluid inputs per thread");
        System.out.println(
            "RECIPE_THREAD_BATCH_DISABLED_QA: batches=1/1, duration=20/40, leftover coal="
                + itemCount(batch.first(), Items.coal)
                + ", water="
                + batch.water()
                    .getFluidAmount()
                + ", blaze powder="
                + itemCount(batch.second(), Items.blaze_powder)
                + ", lava="
                + batch.lava()
                    .getFluidAmount());
    }

    private void verifyRecipeLocking(World world) {
        Fixture controller = fixture(world, 80);
        MTEHatchInputBus first = (MTEHatchInputBus) place(world, 81, ItemList.Hatch_Input_Bus_LV.get(1))
            .getMetaTileEntity();
        MTEHatchInputBus second = (MTEHatchInputBus) place(world, 82, ItemList.Hatch_Input_Bus_LV.get(1))
            .getMetaTileEntity();
        MTEHatchOutputBus output = (MTEHatchOutputBus) place(world, 83, ItemList.Hatch_Output_Bus_LV.get(1))
            .getMetaTileEntity();
        controller.mInputBusses.add(first);
        controller.mInputBusses.add(second);
        controller.mOutputBusses.add(output);
        controller.setRecipeLocking(true);
        first.setInventorySlotContents(0, new ItemStack(Items.iron_ingot));
        second.setInventorySlotContents(0, new ItemStack(Items.gold_ingot));
        controller.rejectPreparedRecipe = true;
        controller.step(5);
        require(
            controller.getRecipeThreadTasks()
                .isEmpty() && controller.getSingleRecipeCheck() == null
                && itemCount(first, Items.iron_ingot) == 1
                && itemCount(second, Items.gold_ingot) == 1,
            "a rejected prepared recipe neither consumes ingredients nor publishes a recipe lock");
        controller.rejectPreparedRecipe = false;
        controller.step(10);
        require(
            controller.getRecipeThreadTasks()
                .size() == 1 && controller.getSingleRecipeCheck() != null
                && itemCount(second, Items.gold_ingot) == 1,
            "manually enabled recipe locking prevents a second thread from consuming a different recipe");
        verifyLockedPatternCache(controller);
        controller.setRecipeLocking(false);
        controller.step(15);
        require(
            controller.getRecipeThreadTasks()
                .size() == 2 && controller.getSingleRecipeCheck() == null
                && itemCount(second, Items.gold_ingot) == 0,
            "disabling recipe locking immediately allows the other cross-recipe thread");
    }

    private void verifyLockedPatternCache(Fixture controller) {
        RecipeThreadProcessingLogic logic = new RecipeThreadProcessingLogic();
        logic.setMachine(controller)
            .setRecipeMap(RECIPES)
            .setAvailableVoltage(8)
            .setAvailableAmperage(1)
            .setMaxParallelSupplier(() -> 1)
            .setBatchSize(1)
            .setRecipeLocking(controller, true);
        require(
            logic.tryCachePossibleRecipesFromPattern(new CachePattern(new ItemStack(Items.gold_ingot))),
            "a different recipe is available in the pattern cache before applying the existing lock");
        ItemStack iron = new ItemStack(Items.iron_ingot);
        ItemStack gold = new ItemStack(Items.gold_ingot);
        logic.setInputItems(iron, gold)
            .setInputFluids();
        logic.prepareRecipe(8);
        try {
            require(
                logic.process()
                    .wasSuccessful(),
                "the locked recipe runs with mixed pattern ingredients");
            require(
                logic.getOutputItems().length == 1 && logic.getOutputItems()[0].getItem() == Items.gold_nugget
                    && logic.getOutputItems()[0].stackSize == 1
                    && logic.getPlannedConsumption(iron) == 1
                    && logic.getPlannedConsumption(gold) == 0,
                "the recipe lock controls both consumption and outputs ahead of a different cached pattern");
            require(
                iron.stackSize == 1 && gold.stackSize == 1,
                "pattern lock validation retains deferred input commitment");
        } finally {
            logic.finishRecipe();
        }
    }

    private Fixture fixture(World world, int x) {
        world.getChunkFromChunkCoords(x >> 4, 0);
        Fixture controller = (Fixture) place(world, x, new ItemStack(GregTechAPI.sBlockMachines, 1, FIXTURE_ID))
            .getMetaTileEntity();
        controller.ready();
        return controller;
    }

    private BatchFixture batchFixture(World world, int x) {
        Fixture controller = fixture(world, x);
        controller.voltage = 32;
        MTEHatchInputBus first = (MTEHatchInputBus) place(world, x + 1, ItemList.Hatch_Input_Bus_LV.get(1))
            .getMetaTileEntity();
        MTEHatchInputBus second = (MTEHatchInputBus) place(world, x + 2, ItemList.Hatch_Input_Bus_LV.get(1))
            .getMetaTileEntity();
        MTEHatchOutputBus items = (MTEHatchOutputBus) place(world, x + 3, ItemList.Hatch_Output_Bus_LV.get(1))
            .getMetaTileEntity();
        MTEHatchInput water = (MTEHatchInput) place(world, x + 4, ItemList.Hatch_Input_LV.get(1)).getMetaTileEntity();
        MTEHatchInput lava = (MTEHatchInput) place(world, x + 5, ItemList.Hatch_Input_LV.get(1)).getMetaTileEntity();
        MTEHatchOutput firstOutput = (MTEHatchOutput) place(world, x + 6, ItemList.Hatch_Output_LV.get(1))
            .getMetaTileEntity();
        MTEHatchOutput secondOutput = (MTEHatchOutput) place(world, x + 7, ItemList.Hatch_Output_LV.get(1))
            .getMetaTileEntity();
        BatchFixture result = new BatchFixture(
            controller,
            first,
            second,
            water,
            lava,
            items,
            firstOutput,
            secondOutput);
        result.attach(controller);
        first.setInventorySlotContents(0, new ItemStack(Items.coal, 7));
        second.setInventorySlotContents(0, new ItemStack(Items.blaze_powder, 8));
        water.mFluid = new FluidStack(FluidRegistry.WATER, 325);
        lava.mFluid = new FluidStack(FluidRegistry.LAVA, 125);
        return result;
    }

    private static int itemCount(IInventory hatch, Item item) {
        int count = 0;
        for (int slot = 0; slot < hatch.getSizeInventory(); slot++) {
            ItemStack stack = hatch.getStackInSlot(slot);
            if (stack != null && stack.getItem() == item) count += stack.stackSize;
        }
        return count;
    }

    private static int fluidCount(BatchFixture batch, Fluid fluid) {
        int amount = 0;
        for (MTEHatchOutput output : List.of(batch.firstOutput(), batch.secondOutput())) {
            FluidStack stack = output.getFluid();
            if (stack != null && stack.getFluid() == fluid) amount += stack.amount;
        }
        return amount;
    }

    private void verifyOverclockModules(World world) {
        int[] timeReduction = { 4, 8, 8 };
        int[] powerIncrease = { 4, 4, 2 };
        for (int index = 0; index < MachineLoader.OVERCLOCK.length; index++) {
            int x = 16 + index * 5;
            world.getChunkFromChunkCoords(x >> 4, 0);
            Fixture controller = (Fixture) place(world, x, new ItemStack(GregTechAPI.sBlockMachines, 1, FIXTURE_ID))
                .getMetaTileEntity();
            controller.ready();
            controller.threads = 1;
            // GregTech treats ULV recipes as LV for overclocking, so MV provides one overclock.
            controller.voltage = 128;
            controller.capacity = 128;
            MTEHatchInputBus input = (MTEHatchInputBus) place(world, x + 1, ItemList.Hatch_Input_Bus_LV.get(1))
                .getMetaTileEntity();
            MTEHatchOutputBus output = (MTEHatchOutputBus) place(world, x + 2, ItemList.Hatch_Output_Bus_LV.get(1))
                .getMetaTileEntity();
            controller.mInputBusses.add(input);
            controller.mOutputBusses.add(output);
            input.setInventorySlotContents(0, new ItemStack(Items.iron_ingot));
            GTNRItemList entry = MachineLoader.OVERCLOCK[index];
            ParameterModuleHatch module = module(world, x + 3, entry);
            require(
                controller.addParameterModuleToMachineList(module.getBaseMetaTileEntity(), 0),
                "overclock module accepted: " + entry);
            require(
                module.getTimeReduction() == timeReduction[index] && module.getPowerIncrease() == powerIncrease[index],
                "GTNG overclock ratios: " + entry);
            require(
                controller.getOverclockTimeReduction() == timeReduction[index]
                    && controller.getOverclockPowerIncrease() == powerIncrease[index],
                "controller overclock selection: " + entry);
            List<StructureError> errors = new ArrayList<>();
            controller.checkMachine(controller.getBaseMetaTileEntity(), null, errors);
            require(errors.isEmpty(), "one overclock module is valid: " + entry);
            controller.step(5);
            require(
                controller.getRecipeThreadTasks()
                    .size() == 1,
                "recipe starts with overclock module: " + entry);
            RecipeThreadTask task = controller.getRecipeThreadTasks()
                .get(0);
            OverclockCalculator expected = new OverclockCalculator().setRecipeEUt(8)
                .setDuration(200)
                .setEUt(128)
                .setAmperage(1)
                .setDurationDecreasePerOC(timeReduction[index])
                .setEUtIncreasePerOC(powerIncrease[index])
                .calculate();
            require(
                task.getDuration() == expected.getDuration() && task.getEUt() == expected.getConsumption(),
                "real recipe timing and power match selected overclock: " + entry);
            require(task.getDuration() < 100, "overclock module improves on ordinary overclock: " + entry);
            NBTTagCompound saved = new NBTTagCompound();
            module.saveNBTData(saved);
            ParameterModuleHatch restored = (ParameterModuleHatch) module.newMetaEntity(null);
            restored.loadNBTData(saved);
            require(
                restored.getTimeReduction() == timeReduction[index]
                    && restored.getPowerIncrease() == powerIncrease[index],
                "overclock module round trip: " + entry);
            ParameterModuleHatch duplicate = module(
                world,
                x + 4,
                MachineLoader.OVERCLOCK[(index + 1) % MachineLoader.OVERCLOCK.length]);
            controller.addParameterModuleToMachineList(duplicate.getBaseMetaTileEntity(), 0);
            controller.checkMachine(controller.getBaseMetaTileEntity(), null, errors);
            require(errors.size() == 1, "different overclock grades still share one module limit: " + entry);
        }
        Fixture ordinary = new Fixture();
        require(
            ordinary.getOverclockTimeReduction() == 2 && ordinary.getOverclockPowerIncrease() == 4,
            "ordinary overclock is preserved without a module");
    }

    private ParameterModuleHatch module(World world, int x, GTNRItemList entry) {
        return (ParameterModuleHatch) place(world, x, entry.get(1)).getMetaTileEntity();
    }

    private static BaseMetaTileEntity place(World world, int x, ItemStack stack) {
        world.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 10, 0);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setOwnerUuid(java.util.UUID.fromString("b56cf8e6-ce3c-4c10-ab0c-1825cbd5984d"));
        tile.setOwnerName("ThreadQA");
        tile.enableWorking();
        return tile;
    }

    private void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    private record CachePattern(ItemStack item) implements IDualInputInventoryWithPattern {

        @Override
        public GTDualInputPattern getPatternInputs() {
            return new GTDualInputPattern(getItemInputs(), getFluidInputs());
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public ItemStack[] getItemInputs() {
            return new ItemStack[] { item };
        }

        @Override
        public FluidStack[] getFluidInputs() {
            return new FluidStack[0];
        }
    }

    private record BatchFixture(Fixture controller, MTEHatchInputBus first, MTEHatchInputBus second,
        MTEHatchInput water, MTEHatchInput lava, MTEHatchOutputBus items, MTEHatchOutput firstOutput,
        MTEHatchOutput secondOutput) {

        private void attach(Fixture target) {
            target.mInputBusses.add(first);
            target.mInputBusses.add(second);
            target.mInputHatches.add(water);
            target.mInputHatches.add(lava);
            target.mOutputBusses.add(items);
            target.mOutputHatches.add(firstOutput);
            target.mOutputHatches.add(secondOutput);
            target.voltage = 32;
        }
    }

    public static final class Fixture extends GTNRMultiBlockBase<Fixture> {

        private static final IStructureDefinition<Fixture> STRUCTURE = StructureDefinition.<Fixture>builder()
            .addShape(
                "main",
                new String[][] { { "A~A", "AAA", "AAA" }, { "AAA", "A-A", "AAA" }, { "AAA", "AAA", "AAA" } })
            .addElement(
                'A',
                GTStructureUtility.buildHatchAdder(Fixture.class)
                    .atLeast(Energy, InputBus)
                    .casingIndex(16)
                    .hint(1)
                    .buildAndChain(ofBlock(Blocks.cobblestone, 0)))
            .build();
        private int threads = 2;
        private long voltage = 8;
        private long capacity = 64;
        private boolean funded = true;
        private boolean rejectPreparedRecipe;
        private long spent;
        private int constructCalls;

        private Fixture(int id) {
            super(id, "gtnr_thread_qa", "Thread QA");
        }

        public Fixture() {
            super("gtnr_thread_qa");
        }

        public void ready() {
            mMachine = true;
            fixAllIssues();
        }

        public void step(long tick) {
            runMachine(getBaseMetaTileEntity(), tick);
        }

        @Override
        public int getMaxRecipeThreads() {
            return threads;
        }

        @Override
        public long getMaxInputEu() {
            return capacity;
        }

        @Override
        public long getMaxInputVoltage() {
            return voltage;
        }

        @Override
        protected CheckRecipeResult postCheckRecipe(CheckRecipeResult result, ProcessingLogic logic) {
            return rejectPreparedRecipe && result.wasSuccessful() ? CheckRecipeResultRegistry.INTERNAL_ERROR
                : super.postCheckRecipe(result, logic);
        }

        @Override
        public boolean drainEnergyInput(long eut) {
            if (funded) spent += eut;
            return funded;
        }

        @Override
        public RecipeMap<?> getRecipeMap() {
            return RECIPES;
        }

        @Override
        public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
            return new Fixture();
        }

        @Override
        public IStructureDefinition<Fixture> getStructureDefinition() {
            return STRUCTURE;
        }

        @Override
        public void construct(ItemStack stack, boolean hints) {
            constructCalls++;
            buildPiece("main", stack, hints, 1, 0, 0);
            buildPiece("main", stack, hints, 1, 0, 0);
        }

        @Override
        protected void checkMachineStructure(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {}

        @Override
        protected void checkMachineHatches(List<StructureError> errors) {
            if (getBaseMetaTileEntity() == null) {
                checkCasingMin(errors, 0, 20);
                checkHatchMin(errors, Energy, 1);
                checkHatchMax(errors, Energy, 2);
                checkHatchMin(errors, InputBus, 1);
            }
        }

        @Override
        public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
            boolean active, boolean redstone) {
            return new ITexture[0];
        }
    }
}
