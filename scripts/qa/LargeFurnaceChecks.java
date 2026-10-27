package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.machine.multiMachineBase.GTNRMultiBlockBase;
import com.xyp.gtneverreset.common.machine.multiblock.LargeFurnace;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.machine.RecipeThreadTask;
import com.xyp.gtneverreset.utils.structure.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.utils.structure.LargeFurnaceStructure;
import com.xyp.gtneverreset.utils.structure.StructureTooltipReader;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.VoidingMode;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTUtility;

@Mod(modid = "largefurnaceqa", name = "Large Furnace QA", version = "1", dependencies = "after:" + GTNeverReset.MODID)
public final class LargeFurnaceChecks {

    private final List<int[]> hatchPositions = new ArrayList<>();
    private final List<int[]> lavaPositions = new ArrayList<>();
    private BaseMetaTileEntity tile;
    private LargeFurnace machine;
    private World world;
    private boolean finished;
    private int checks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.furnace.qa")) FMLCommonHandler.instance()
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
            System.out.println("LARGE_FURNACE_QA PASS: " + checks + " assertions");
        } catch (Throwable failure) {
            failure.printStackTrace();
            result = "FAIL: " + failure;
        }
        File directory = new File(System.getProperty("gtnr.furnace.qa.output"));
        directory.mkdirs();
        Files.writeString(new File(directory, "result.txt").toPath(), result);
        server.initiateShutdown();
    }

    public void verify(World testWorld) throws Exception {
        world = testWorld;
        buildStructure();
        verifyStructure();
        verifyModules();
        verifyProcessing();
    }

    public LargeFurnace getMachine() {
        return machine;
    }

    public BaseMetaTileEntity getTile() {
        return tile;
    }

    private void buildStructure() {
        tile = place(new int[] { 0, 80, 0 }, GTNRItemList.LargeFurnace.get(1));
        machine = (LargeFurnace) tile.getMetaTileEntity();
        ItemStack steel = Materials.Steel.getBlocks(1);
        int cells = 0, cokeBricks = 0, firebricks = 0;
        String[][] shape = LargeFurnaceStructure.shape();
        for (int y = 0; y < shape.length; y++) {
            for (int z = 0; z < shape[y].length; z++) {
                for (int x = 0; x < shape[y][z].length(); x++) {
                    char c = shape[y][z].charAt(x);
                    int[] p = position(x, y, z);
                    if (c == '~') continue;
                    if (c != ' ') cells++;
                    switch (c) {
                        case 'L' -> {
                            hatchPositions.add(p);
                            set(p, GregTechAPI.sBlockCasings2, 0);
                        }
                        case 'J' -> set(p, Block.getBlockFromItem(steel.getItem()), steel.getItemDamage());
                        case 'D' -> {
                            set(p, GregTechAPI.sBlockCasings12, 0);
                            cokeBricks++;
                        }
                        case 'E' -> {
                            set(p, GregTechAPI.sBlockCasings4, 15);
                            firebricks++;
                        }
                        case 'H', 'I' -> {
                            lavaPositions.add(p);
                            set(p, Blocks.lava, 0);
                        }
                        case 'B', 'C', 'G', 'K', 'M' -> set(p, Blocks.iron_bars, 0);
                        default -> set(p, Blocks.air, 0);
                    }
                }
            }
        }
        require(cells + 1 == 716, "original 716-cell geometry including controller");
        require(lavaPositions.size() == 56, "56 lava source positions");
        require(
            lavaPositions.stream()
                .allMatch(
                    p -> world.getBlock(p[0], p[1], p[2]) == Blocks.lava
                        && world.getBlockMetadata(p[0], p[1], p[2]) == 0),
            "all 56 placed lava sources retain native source block and metadata");
        require(cokeBricks == 64 && firebricks == 102, "native coke oven and firebrick counts");
        hatch(0, ItemList.Hatch_Input_Bus_UV.get(1));
        hatch(1, ItemList.Hatch_Output_Bus_UV.get(1));
        hatch(2, ItemList.Hatch_Maintenance.get(1));
        hatch(3, ItemList.Hatch_Energy_LV.get(1));
        form(true, "complete original structure forms");
    }

    private void verifyStructure() {
        require(machine.getRecipeMap() == RecipeMaps.furnaceRecipes, "native furnace recipe map");
        require(
            machine.getMaxParallelRecipes() == 64 && machine.getTotalRecipeThreads() == 6,
            "LV 64 parallels and 6 threads");
        require(
            machine.getSpeedMultiplier() == 2 && machine.getEUtMultiplier() == 0.8,
            "200 percent speed and 80 percent EU");
        require(
            !machine.getTooltipStats()
                .perfectOverclock(),
            "default normal overclocking");
        for (int tier = 1; tier <= 14; tier++) {
            require(LargeFurnace.scaleForTier(64, tier) == 64 << (tier - 1), "parallel doubling tier " + tier);
            require(LargeFurnace.scaleForTier(6, tier) == 6 << (tier - 1), "thread doubling tier " + tier);
        }
        require(
            LargeFurnace.scaleForTier(64, Integer.MAX_VALUE) == Integer.MAX_VALUE,
            "capacity saturates without overflow");
        var summary = StructureTooltipReader.readStructure(machine);
        require(
            summary.size()
                .width() == 11
                && summary.size()
                    .height() == 13
                && summary.size()
                    .depth() == 11,
            "automatic Shift occupied structure dimensions: " + summary.size());
        var tooltip = new AutoMultiblockTooltipBuilder().addAutoTooltip(machine)
            .toolTipFinisher();
        require(
            tooltip.getInformation().length > 2 && tooltip.getStructureInformation().length > 4,
            "automatic information and Shift tooltip build");
        require(
            RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .anyMatch(
                    recipe -> Arrays.stream(recipe.mOutputs)
                        .anyMatch(stack -> GTUtility.areStacksEqual(stack, GTNRItemList.LargeFurnace.get(1)))),
            "survival controller recipe");
        hatch(8, ItemList.Hatch_Energy_LV.get(1));
        form(true, "two LV energy hatches form");
        require(
            machine.getMaxParallelRecipes() == 64 && machine.getTotalRecipeThreads() == 6,
            "two LV hatches do not raise voltage tier");
        hatch(8, ItemList.Hatch_Energy_MV.get(1));
        form(true, "mixed LV and MV hatches form");
        require(
            machine.getMaxParallelRecipes() == 128 && machine.getTotalRecipeThreads() == 12,
            "MV 128 parallels and 12 threads");
        hatch(8, ItemList.Hatch_Energy_HV.get(1));
        form(true, "mixed LV and HV hatches form");
        require(
            machine.getMaxParallelRecipes() == 256 && machine.getTotalRecipeThreads() == 24,
            "HV 256 parallels and 24 threads");
        hatch(9, ItemList.Hatch_Energy_LV.get(1));
        form(false, "third energy hatch rejected");
        restore(8);
        restore(9);
        restore(3);
        form(false, "missing energy hatch rejected");
        hatch(3, ItemList.Hatch_Energy_LV.get(1));
        restore(2);
        form(false, "missing maintenance hatch rejected");
        hatch(2, ItemList.Hatch_Maintenance.get(1));
        hatch(9, ItemList.Hatch_Maintenance.get(1));
        form(false, "second maintenance hatch rejected");
        restore(9);
        restore(0);
        form(false, "missing input bus rejected");
        hatch(0, ItemList.Hatch_Input_Bus_UV.get(1));
        restore(1);
        form(false, "missing output bus rejected");
        hatch(1, ItemList.Hatch_Output_Bus_UV.get(1));
        set(lavaPositions.get(0), Blocks.flowing_lava, 1);
        form(false, "flowing lava does not replace required source block");
        set(lavaPositions.get(0), Blocks.lava, 0);
        form(true, "restored structure forms");
    }

    private void verifyModules() {
        hatch(4, GTNRItemList.StaticParallelControllerT1.get(1));
        hatch(5, GTNRItemList.StaticSpeedControllerT1.get(1));
        hatch(6, GTNRItemList.PowerConsumptionControllerT1.get(1));
        hatch(7, GTNRItemList.LowSpeedPerfectOverclockController.get(1));
        form(true, "all four parameter module types form together");
        require(
            machine.getParameterModules()
                .size() == 4,
            "all parameter modules registered");
        require(
            machine.getMaxParallelRecipes() == 576 && machine.getSpeedMultiplier() == 4,
            "parallel and speed modules multiply furnace baseline");
        require(Math.abs(machine.getEUtMultiplier() - 0.76) < 0.000001, "EU module multiplies 80 percent baseline");
        require(
            machine.getOverclockTimeReduction() == 4 && machine.getOverclockPowerIncrease() == 4,
            "overclock module changes time and power factors");
        hatch(8, GTNRItemList.DynamicSpeedControllerT1.get(1));
        form(false, "duplicate speed module type rejected");
        for (int index = 4; index <= 8; index++) restore(index);
        form(true, "optional modules may all be removed");
        require(
            machine.getMaxParallelRecipes() == 64 && machine.getSpeedMultiplier() == 2
                && machine.getEUtMultiplier() == 0.8
                && machine.getOverclockTimeReduction() == 2,
            "removing modules restores base processing parameters");
    }

    private void verifyProcessing() throws Exception {
        MTEHatchInputBus input = (MTEHatchInputBus) baseAt(0).getMetaTileEntity();
        MTEHatchOutputBus output = (MTEHatchOutputBus) baseAt(1).getMetaTileEntity();
        BaseMetaTileEntity energy = baseAt(3);
        machine.fixAllIssues();
        machine.setVoidingMode(VoidingMode.VOID_NONE);
        input.setInventorySlotContents(0, new ItemStack(Items.stick, 3));
        input.setInventorySlotContents(1, new ItemStack(Blocks.iron_ore, 3));
        input.setInventorySlotContents(2, new ItemStack(Blocks.gold_ore, 3));
        input.setInventorySlotContents(3, new ItemStack(Blocks.sand, 3));
        Method run = GTNRMultiBlockBase.class.getDeclaredMethod("runMachine", IGregTechTileEntity.class, long.class);
        run.setAccessible(true);
        energy.setStoredEU(energy.getEUCapacity());
        run.invoke(machine, tile, 5L);
        require(
            machine.getRecipeThreadTasks()
                .size() >= 2,
            "multiple furnace recipes start despite first unprocessable input");
        require(
            machine.getRecipeThreadTasks()
                .stream()
                .allMatch(task -> task.getEUt() > 0),
            "every smelting task consumes power");
        require(inputCount(input, Items.stick) == 3, "unprocessed first-slot input remains untouched");
        long before = energy.getStoredEU();
        run.invoke(machine, tile, 6L);
        require(energy.getStoredEU() < before, "real running threads drain energy hatch");
        require(
            machine.getRecipeThreadTasks()
                .stream()
                .anyMatch(task -> task.getProgress() > 0),
            "smelting tasks advance");
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        List<NBTTagCompound> taskTags = machine.getRecipeThreadTasks()
            .stream()
            .map(RecipeThreadTask::save)
            .toList();
        machine.loadNBTData(saved);
        require(
            machine.getRecipeThreadTasks()
                .stream()
                .map(RecipeThreadTask::save)
                .toList()
                .equals(taskTags),
            "task outputs, power, parallel counts and progress survive NBT reload");
        tile.disableWorking();
        run.invoke(machine, tile, 7L);
        require(
            machine.getRunningRecipeThreads() == 0 && machine.getRecipeThreadTasks()
                .stream()
                .map(RecipeThreadTask::save)
                .toList()
                .equals(taskTags),
            "switching off pauses tasks without losing work");
        tile.enableWorking();
        for (int slot = 0; slot < output.getSizeInventory(); slot++)
            output.setInventorySlotContents(slot, new ItemStack(Items.stick, 64));
        for (long tick = 8; tick < 1000; tick++) {
            energy.setStoredEU(energy.getEUCapacity());
            run.invoke(machine, tile, tick);
        }
        require(
            !machine.getRecipeThreadTasks()
                .isEmpty() && machine.getRecipeThreadTasks()
                    .stream()
                    .allMatch(RecipeThreadTask::isFinished),
            "completed outputs remain buffered while output bus is full");
        before = energy.getStoredEU();
        run.invoke(machine, tile, 1000L);
        require(energy.getStoredEU() == before, "finished blocked threads consume no additional energy");
        for (int slot = 0; slot < output.getSizeInventory(); slot++) output.setInventorySlotContents(slot, null);
        for (long tick = 1001; tick < 2000; tick++) {
            energy.setStoredEU(energy.getEUCapacity());
            run.invoke(machine, tile, tick);
        }
        require(
            machine.getRecipeThreadTasks()
                .isEmpty(),
            "all tasks complete after output is unblocked");
        require(
            outputCount(output, Items.iron_ingot) == 3 && outputCount(output, Items.gold_ingot) == 3
                && outputCount(output, Item.getItemFromBlock(Blocks.glass)) == 3,
            "each native furnace output delivered exactly once");
        require(
            inputCount(input, Items.stick) == 3 && inputCount(input, Item.getItemFromBlock(Blocks.iron_ore)) == 0
                && inputCount(input, Item.getItemFromBlock(Blocks.gold_ore)) == 0
                && inputCount(input, Item.getItemFromBlock(Blocks.sand)) == 0,
            "all smeltable inputs consumed and invalid input preserved");
    }

    private static int inputCount(MTEHatchInputBus bus, Item item) {
        int count = 0;
        for (int slot = 0; slot < bus.getSizeInventory(); slot++) {
            ItemStack stack = bus.getStackInSlot(slot);
            if (stack != null && stack.getItem() == item) count += stack.stackSize;
        }
        return count;
    }

    private static int outputCount(MTEHatchOutputBus bus, Item item) {
        int count = 0;
        for (int slot = 0; slot < bus.getSizeInventory(); slot++) {
            ItemStack stack = bus.getStackInSlot(slot);
            if (stack != null && stack.getItem() == item) count += stack.stackSize;
        }
        return count;
    }

    private BaseMetaTileEntity baseAt(int hatchIndex) {
        int[] p = hatchPositions.get(hatchIndex);
        return (BaseMetaTileEntity) world.getTileEntity(p[0], p[1], p[2]);
    }

    private void form(boolean expected, String label) {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(tile, null, errors);
        require(
            errors.isEmpty() == expected,
            label + ": "
                + errors.stream()
                    .map(StructureError::getDisplayString)
                    .toList());
        machine.mMachine = errors.isEmpty();
    }

    private BaseMetaTileEntity hatch(int index, ItemStack stack) {
        return place(hatchPositions.get(index), stack);
    }

    private void restore(int index) {
        set(hatchPositions.get(index), GregTechAPI.sBlockCasings2, 0);
    }

    private int[] position(int x, int y, int z) {
        int[] offset = new int[3];
        machine.getExtendedFacing()
            .getWorldOffset(
                new int[] { x - LargeFurnaceStructure.OFFSET_X, y - LargeFurnaceStructure.OFFSET_Y,
                    z - LargeFurnaceStructure.OFFSET_Z },
                offset);
        return new int[] { tile.getXCoord() + offset[0], tile.getYCoord() + offset[1], tile.getZCoord() + offset[2] };
    }

    private void set(int[] p, Block block, int metadata) {
        world.setBlock(p[0], p[1], p[2], block, metadata, 2);
    }

    private BaseMetaTileEntity place(int[] p, ItemStack stack) {
        world.setBlockToAir(p[0], p[1], p[2]);
        set(p, GregTechAPI.sBlockMachines, 0);
        BaseMetaTileEntity base = (BaseMetaTileEntity) world.getTileEntity(p[0], p[1], p[2]);
        base.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        base.setFrontFacing(ForgeDirection.NORTH);
        base.enableWorking();
        return base;
    }

    private void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }
}
