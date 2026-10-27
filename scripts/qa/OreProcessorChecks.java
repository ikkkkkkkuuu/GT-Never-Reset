package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.machine.multiMachineBase.GTNRMultiBlockBase;
import com.xyp.gtneverreset.common.machine.multiblock.LargeOreProcessor;
import com.xyp.gtneverreset.loader.GTNRRecipeMaps;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.machine.OreRecipeThreadProcessingLogic;
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
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.StoneType;
import gregtech.api.enums.VoidingMode;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.common.ores.OreInfo;
import gregtech.common.ores.OreManager;

@Mod(modid = "oreprocessorqa", name = "Ore Processor QA", version = "1", dependencies = "after:" + GTNeverReset.MODID)
public final class OreProcessorChecks {

    private boolean finished;
    private int checks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.ore.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if (finished || event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.worldServerForDimension(0) == null) return;
        finished = true;
        String result = "PASS";
        try {
            verify(server.worldServerForDimension(0));
            System.out.println("ORE_PROCESSOR_QA PASS: " + checks + " assertions");
        } catch (Throwable failure) {
            failure.printStackTrace();
            result = "FAIL: " + failure;
        }
        File directory = new File(System.getProperty("gtnr.ore.qa.output"));
        directory.mkdirs();
        Files.writeString(new File(directory, "result.txt").toPath(), result);
        server.initiateShutdown();
    }

    private void verify(World world) throws Exception {
        world.getChunkFromChunkCoords(0, 0);
        BaseMetaTileEntity tile = place(world, 8, 10, GTNRItemList.LargeOreProcessor.get(1));
        tile.setFrontFacing(ForgeDirection.NORTH);
        LargeOreProcessor machine = (LargeOreProcessor) tile.getMetaTileEntity();
        require(
            machine.getMaxParallelRecipes() == 64 && machine.getTotalRecipeThreads() == 10,
            "64 parallels / 10 threads");
        require(machine.getSpeedMultiplier() == 1 && machine.getEUtMultiplier() == 1, "base speed and EU multipliers");
        require(
            machine.getTooltipStats()
                .perfectOverclock(),
            "ore controller enables perfect overclocking");
        var summary = StructureTooltipReader.readStructure(machine);
        require(
            summary.size()
                .width() == 3
                && summary.size()
                    .height() == 3
                && summary.size()
                    .depth() == 1,
            "3x3x1 structure");
        require(
            summary.hatches()
                .stream()
                .anyMatch(
                    hatch -> hatch.nameKeys()
                        .contains("gtnr.ore_processor.modules")),
            "module positions in Shift tooltip");
        var tooltip = new AutoMultiblockTooltipBuilder().addAutoTooltip(machine)
            .toolTipFinisher();
        require(
            tooltip.getInformation().length > 2 && tooltip.getStructureInformation().length > 4,
            "automatic bilingual tooltip builds");

        var recipes = GTNRRecipeMaps.ORE_PROCESSING_RECIPES.getAllRecipes();
        require(recipes.size() > 100, "material and external recipes loaded");
        require(
            recipes.stream()
                .allMatch(recipe -> recipe.mEUt == 10 && recipe.mDuration == 20),
            "all ore recipes use 10 EU/t for 20 ticks");
        for (StoneType stone : List.of(StoneType.Moon, StoneType.Deepslate)) {
            try (OreInfo<Materials> info = OreInfo.getNewInfo()) {
                info.material = Materials.Iron;
                info.stoneType = stone;
                ItemStack variant = OreManager.getStack(info, 1);
                require(variant != null, stone + " ore variant exists");
                require(
                    GTNRRecipeMaps.ORE_PROCESSING_RECIPES.findRecipeQuery()
                        .items(variant)
                        .notUnificated(true)
                        .find() != null,
                    stone + " ore variant retains the original fallback support");
            }
        }
        require(
            RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .anyMatch(
                    recipe -> java.util.Arrays.stream(recipe.mOutputs)
                        .anyMatch(stack -> GTUtility.areStacksEqual(stack, GTNRItemList.LargeOreProcessor.get(1)))),
            "controller has a survival recipe");
        System.out.println(
            "ORE_PROCESSOR_QA recipes=" + recipes.size()
                + " maxOutputs="
                + recipes.stream()
                    .mapToInt(recipe -> recipe.mOutputs.length)
                    .max()
                    .orElse(0));

        MTEHatchInputBus input = (MTEHatchInputBus) place(world, 7, 10, ItemList.Hatch_Input_Bus_LV.get(1))
            .getMetaTileEntity();
        MTEHatchOutputBus output = (MTEHatchOutputBus) place(world, 9, 10, ItemList.Hatch_Output_Bus_HV.get(1))
            .getMetaTileEntity();
        BaseMetaTileEntity energy = place(world, 7, 11, ItemList.Hatch_Energy_HV.get(1));
        place(world, 8, 11, ItemList.Hatch_Maintenance.get(1));
        world.setBlock(9, 11, 8, GregTechAPI.sBlockCasings2, 0, 3);
        place(world, 7, 12, GTNRItemList.StaticParallelControllerT1.get(1));
        place(world, 8, 12, GTNRItemList.StaticSpeedControllerT1.get(1));
        place(world, 9, 12, GTNRItemList.PowerConsumptionControllerT1.get(1));
        List<StructureError> errors = new ArrayList<>();
        machine.clearHatches();
        machine.checkMachine(tile, null, errors);
        require(errors.isEmpty(), "structure forms with all three modules: " + errors);
        require(
            machine.getParameterModules()
                .size() == 3 && machine.getMaxParallelRecipes() > 64,
            "module registration and parallel multiplier");

        place(world, 9, 12, GTNRItemList.StaticSpeedControllerT2.get(1));
        machine.clearHatches();
        machine.checkMachine(tile, null, errors);
        require(!errors.isEmpty(), "duplicate speed modules reject formation");
        for (int x = 7; x <= 9; x++) world.setBlock(x, 12, 8, GregTechAPI.sBlockCasings2, 0, 3);
        machine.clearHatches();
        errors.clear();
        machine.checkMachine(tile, null, errors);
        require(errors.isEmpty(), "machine also forms without optional modules: " + errors);

        energy.setStoredEU(Math.min(1000000, energy.getEUCapacity()));
        machine.mMachine = true;
        machine.fixAllIssues();
        machine.setVoidingMode(VoidingMode.VOID_NONE);
        ItemStack iron = GTOreDictUnificator.get(OrePrefixes.ore, Materials.Iron, 5);
        ItemStack copper = GTOreDictUnificator.get(OrePrefixes.ore, Materials.Copper, 5);
        require(iron != null && copper != null, "representative GT ores exist");
        verifyUnprocessedBatch(iron);
        RecipeThreadProcessingLogic probe = new RecipeThreadProcessingLogic();
        Method setup = GTNRMultiBlockBase.class.getDeclaredMethod("setupProcessingLogic", ProcessingLogic.class);
        setup.setAccessible(true);
        setup.invoke(machine, probe);
        probe.setAvailableVoltage(128)
            .setAvailableAmperage(1)
            .setMaxParallelSupplier(() -> 1)
            .setInputItems(GTUtility.copyAmountUnsafe(1, iron));
        require(
            probe.process()
                .wasSuccessful(),
            "overclock probe finds the real ore recipe");
        require(
            probe.getDuration() == 5 && probe.getCalculatedEut() == 40,
            "one perfect overclock: 20 ticks / 10 EU/t becomes 5 ticks / 40 EU/t");
        input.setInventorySlotContents(0, iron);
        input.setInventorySlotContents(1, copper);
        input.setInventorySlotContents(2, new ItemStack(Blocks.dirt, 3));
        Method run = GTNRMultiBlockBase.class.getDeclaredMethod("runMachine", IGregTechTileEntity.class, long.class);
        run.setAccessible(true);
        run.invoke(machine, tile, 5L);
        require(
            machine.getRecipeThreadTasks()
                .size() >= 2,
            "two ore recipes start in separate threads");
        require(
            machine.getRecipeThreadTasks()
                .stream()
                .filter(task -> task.getEUt() > 0)
                .count() >= 2,
            "ore recipe threads consume energy");
        int dirt = 0;
        for (int slot = 0; slot < input.getSizeInventory(); slot++) {
            ItemStack stack = input.getStackInSlot(slot);
            if (stack != null && stack.getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.dirt))
                dirt += stack.stackSize;
        }
        require(dirt == 0, "unprocessed items leave the input bus in one batch");
        long before = energy.getStoredEU();
        int duration = machine.getRecipeThreadTasks()
            .stream()
            .mapToInt(RecipeThreadTask::getDuration)
            .max()
            .orElseThrow();
        for (long tick = 6; tick <= duration + 10; tick++) run.invoke(machine, tile, tick);
        require(energy.getStoredEU() < before, "running threads drain the energy hatch");
        require(
            machine.getRecipeThreadTasks()
                .isEmpty(),
            "finished ore threads are cleared");
        int produced = 0;
        for (int slot = 0; slot < output.getSizeInventory(); slot++) {
            ItemStack stack = output.getStackInSlot(slot);
            if (stack != null) produced += stack.stackSize;
        }
        require(produced > 0, "ore processing delivers its products");
        require(countItem(output, new ItemStack(Blocks.dirt)) == 3, "unprocessed items arrive unchanged at the output");

        for (int slot = 0; slot < output.getSizeInventory(); slot++)
            output.setInventorySlotContents(slot, new ItemStack(Blocks.cobblestone, 64));
        input.setInventorySlotContents(0, new ItemStack(Blocks.dirt, 64));
        input.setInventorySlotContents(1, new ItemStack(Blocks.dirt, 64));
        input.setInventorySlotContents(2, new ItemStack(Blocks.dirt, 17));
        run.invoke(machine, tile, 100L);
        run.invoke(machine, tile, 101L);
        require(countItem(input, new ItemStack(Blocks.dirt)) == 0, "batch transfer exceeds the 64 parallel limit");
        require(
            machine.getRecipeThreadTasks()
                .size() == 1,
            "blocked transfer remains in one output ledger");
        require(
            machine.getRecipeThreadTasks()
                .get(0)
                .isFinished(),
            "transfer waits only for output space");
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        machine.loadNBTData(saved);
        output.setInventorySlotContents(0, null);
        run.invoke(machine, tile, 102L);
        int partial = countItem(output, new ItemStack(Blocks.dirt));
        require(partial > 0 && partial <= 64, "available output space accepts a partial batch");
        require(
            machine.getRecipeThreadTasks()
                .size() == 1,
            "remaining items survive reload and partial output");
        for (int slot = 1; slot < output.getSizeInventory(); slot++) output.setInventorySlotContents(slot, null);
        run.invoke(machine, tile, 103L);
        require(countItem(output, new ItemStack(Blocks.dirt)) == 145, "full batch arrives without loss or duplication");
        require(
            machine.getRecipeThreadTasks()
                .isEmpty(),
            "completed transfer releases its thread");
        run.invoke(machine, tile, 105L);
        require(countItem(output, new ItemStack(Blocks.dirt)) == 145, "later checks do not duplicate the batch");

        machine.clearHatches();
        world.setBlock(7, 11, 8, GregTechAPI.sBlockCasings2, 0, 3);
        errors.clear();
        machine.checkMachine(tile, null, errors);
        require(!errors.isEmpty(), "missing energy hatch rejects formation");
    }

    private void verifyUnprocessedBatch(ItemStack ore) {
        OreRecipeThreadProcessingLogic logic = new OreRecipeThreadProcessingLogic();
        ItemStack mineral = ore.copy();
        ItemStack first = new ItemStack(Blocks.dirt, 64);
        first.setStackDisplayName("Unprocessed batch");
        ItemStack second = new ItemStack(Blocks.dirt, 64);
        ItemStack circuit = GTUtility.getIntegratedCircuit(1);
        circuit.stackSize = 1;
        logic.setRecipeMap(GTNRRecipeMaps.ORE_PROCESSING_RECIPES)
            .setAvailableVoltage(0)
            .setAvailableAmperage(0)
            .setInputItems(mineral, first, first, second, circuit);
        logic.prepareRecipe(0);
        require(
            logic.process()
                .wasSuccessful(),
            "unsupported batch transfers without recipe power");
        require(first.stackSize == 64 && second.stackSize == 64, "batch does not consume before commit");
        require(logic.getOutputItems().length == 2, "aliased input stacks are not duplicated");
        require(
            logic.getOutputItems()[0].getDisplayName()
                .equals("Unprocessed batch"),
            "transfer preserves item NBT");
        require(logic.getCalculatedEut() == 0 && logic.getDuration() == 1, "transfer is one free output cycle");
        require(logic.commitInputs(), "batch input transaction commits");
        require(first.stackSize == 0 && second.stackSize == 0, "commit consumes the entire unsupported batch");
        require(
            mineral.stackSize == ore.stackSize && circuit.stackSize == 1,
            "valid ore and configuration circuit remain");
        logic.clear();
        logic.setInputItems(mineral);
        logic.prepareRecipe(0);
        require(
            !logic.process()
                .wasSuccessful(),
            "power-starved ore is not transferred as unsupported");
        logic.finishRecipe();
        require(mineral.stackSize == ore.stackSize, "failed processing preserves ore input");
    }

    private static int countItem(MTEHatch hatch, ItemStack item) {
        int count = 0;
        for (int slot = 0; slot < hatch.getSizeInventory(); slot++) {
            ItemStack stack = hatch.getStackInSlot(slot);
            if (stack != null && GTUtility.areStacksEqual(stack, item)) count += stack.stackSize;
        }
        return count;
    }

    private static BaseMetaTileEntity place(World world, int x, int y, ItemStack stack) {
        world.setBlock(x, y, 8, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, 8);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.enableWorking();
        return tile;
    }

    private void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }
}
