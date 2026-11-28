package com.xyp.gtneverreset.qa;

import static com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes.DRILL_CONSUMPTION_CHANCE;
import static com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes.DRILL_CONSUMPTION_SCALE;
import static com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes.DRILL_INPUT_INDEX;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.xyp.gtneverreset.common.machine.hatch.FragmentDimensionHatch;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadTask;
import com.xyp.gtneverreset.common.machine.multiblock.LargeFragmentWorldCollector;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.enums.ModsItemlist;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

@Mod(
    modid = "fragmentdimensionhatchqa",
    name = "Fragment Dimension Hatch QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class FragmentDimensionHatchChecks {

    private static final int Y = 82;
    private static final int STARTUP_TICKS = 140;
    private static final int TRIAL_TICKS = 30;
    private static final String SCREENSHOT = "fragment-dimension-hatch.png";

    private final StringBuilder report = new StringBuilder();
    private BaseMetaTileEntity controller;
    private BaseMetaTileEntity energy;
    private BaseMetaTileEntity input;
    private BaseMetaTileEntity output;
    private FragmentDimensionHatch hatch;
    private int[] dedicatedInputPosition;
    private int[] optionalPortPosition;
    private LargeFragmentWorldCollector collector;
    private NBTTagCompound idleCollector;
    private ItemStack overworld;
    private ItemStack moon;
    private ItemStack drill;
    private List<GTRecipe> steelFluidRecipes;
    private int normalWearChance;
    private int stage;
    private int trialTime;
    private int menuTicks;
    private int frames;
    private boolean launched;
    private volatile boolean ready;
    private volatile boolean finished;
    private volatile Throwable failure;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.fragment.dimension.hatch.qa")) {
            FMLCommonHandler.instance()
                .bus()
                .register(this);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ready
            || finished
            || failure != null
            || !GregTechAPI.sFullLoadFinished) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        var player = (EntityPlayerMP) players.get(0);
        try {
            if (collector == null) {
                prepare(player);
                return;
            }
            energy.setStoredEU(((MTEHatchEnergy) energy.getMetaTileEntity()).maxEUStore());
            if (++trialTime < (stage == 0 ? STARTUP_TICKS : TRIAL_TICKS)) return;

            var tasks = collector.getRecipeThreadTasks();
            report.append("Trial ")
                .append(stage)
                .append(": ")
                .append(tasks.size())
                .append(" tasks, result ")
                .append(
                    collector.getCheckRecipeResult()
                        .getID())
                .append(", ")
                .append(
                    collector.getCheckRecipeResult()
                        .getDisplayString())
                .append(", input EU/t ")
                .append(collector.getMaxInputEu())
                .append('\n');
            for (var task : tasks.stream()
                .limit(2)
                .toList()) {
                var items = new ArrayList<ItemStack>();
                var fluids = new ArrayList<FluidStack>();
                task.getOutputs()
                    .appendDisplayItems(items);
                task.getOutputs()
                    .appendDisplayFluids(fluids);
                report.append("  outputs ")
                    .append(items)
                    .append(", ")
                    .append(fluids)
                    .append('\n');
            }
            switch (stage) {
                case 0 -> {
                    require(tasks.isEmpty(), "an empty dimension hatch started no recipes");
                    setTrial(overworld, null);
                }
                case 1 -> {
                    var circuits = matchingCircuits(tasks, normalRecipes(overworld));
                    require(circuits.size() >= 2, "Overworld tasks prove different ore circuits: " + circuits);
                    require(
                        hatch.getDimensionMarker()
                            .isItemEqual(overworld),
                        "the Overworld marker remains installed");
                    setTrial(moon, null);
                }
                case 2 -> {
                    var circuits = matchingCircuits(tasks, normalRecipes(moon));
                    require(circuits.size() >= 2, "changing to Moon starts different Moon ore circuits: " + circuits);
                    setTrial(overworld, drill);
                }
                case 3 -> {
                    var fluids = new ArrayList<FluidStack>();
                    List<FluidStack> expected = steelFluidRecipes.stream()
                        .flatMap(recipe -> Arrays.stream(recipe.mFluidOutputs))
                        .toList();
                    require(!tasks.isEmpty(), "a drill supplied through the regular input starts fluid recipes");
                    for (var task : tasks) {
                        var outputs = new ArrayList<FluidStack>();
                        task.getOutputs()
                            .appendDisplayFluids(outputs);
                        require(!outputs.isEmpty(), "drill mode starts only fluid recipes", false);
                        fluids.addAll(outputs);
                    }
                    require(
                        fluids.stream()
                            .allMatch(
                                fluid -> expected.stream()
                                    .anyMatch(fluid::isFluidEqual)),
                        "all fluid outputs belong to the installed dimension and drill");
                    require(
                        fluids.stream()
                            .map(FluidStack::getFluidID)
                            .distinct()
                            .count() >= 2,
                        "drill mode starts several Overworld fluid circuits without supplying a circuit");
                    normalWearChance = steelFluidRecipes.get(0)
                        .getMetadataOrDefault(DRILL_CONSUMPTION_CHANCE, 0);
                    setWearChance(DRILL_CONSUMPTION_SCALE);
                    setTrial(overworld, drill);
                }
                case 4 -> {
                    var remaining = input.getStackInSlot(0);
                    require(
                        remaining == null || remaining.stackSize < drill.stackSize,
                        "the live collector debits physical drill heads when wear is guaranteed");
                    setWearChance(normalWearChance);
                    setTrial(overworld, null);
                }
                case 5 -> {
                    var circuits = matchingCircuits(tasks, normalRecipes(overworld));
                    require(!circuits.isEmpty(), "removing the drill restores ore collection: " + circuits);
                    setTrial(null, null);
                }
                case 6 -> {
                    require(tasks.isEmpty(), "clearing the slot stops new recipes");
                    prepareManualTrial(player.worldObj);
                }
                case 7 -> {
                    require(
                        !tasks.isEmpty(),
                        "a collector without the dimension hatch still runs an ordinary bus recipe");
                    prepareHatchAtCasing(player);
                }
                case 8 -> {
                    var hatchTile = hatch.getBaseMetaTileEntity();
                    require(hatch.onRightclick(hatchTile, player), "the real dimension hatch opens its GUI");
                    ready = true;
                }
                default -> throw new AssertionError("Unexpected trial " + stage);
            }
        } catch (Throwable error) {
            failure = error;
        }
    }

    private void prepareHatchAtCasing(EntityPlayerMP player) {
        controller.disableWorking();
        var hatchTile = place(player.worldObj, optionalPortPosition, GTNRMachineID.FragmentDimensionHatch.id);
        hatch = (FragmentDimensionHatch) hatchTile.getMetaTileEntity();
        require(collector.checkStructure(true, controller), "a dimension hatch also forms at a casing port");
        require(
            hatch.inventoryHandler.insertItem(0, overworld.copy(), false) == null,
            "the GUI inventory accepts a marker");
        var world = player.worldObj;
        int x = hatchTile.getXCoord() + 3;
        int y = hatchTile.getYCoord();
        int z = hatchTile.getZCoord();
        world.setBlock(x, y - 1, z, Blocks.stone, 0, 3);
        world.markBlockForUpdate(hatchTile.getXCoord(), y, z);
        player.playerNetServerHandler.setPlayerLocation(x + 0.5, y, z + 0.5, 90, 0);
        // Give the client the block type update before opening this tile's GUI.
        stage++;
        trialTime = 0;
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var mc = Minecraft.getMinecraft();
        try {
            if (!launched && mc.theWorld == null && mc.currentScreen != null && ++menuTicks > 60) {
                launched = true;
                mc.launchIntegratedServer(
                    "fragment-dimension-qa-" + System.currentTimeMillis(),
                    "Fragment Dimension Hatch QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw failure;
            if (ready && ++frames > 240) throw new AssertionError("The dimension hatch GUI did not open");
        } catch (Throwable error) {
            finish(error);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        var mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || !ready
            || finished
            || frames < 15
            || ModularScreen.getCurrent() == null) return;
        try {
            ScreenShotHelper
                .saveScreenshot(new File("."), SCREENSHOT, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            require(Files.isRegularFile(Path.of("screenshots", SCREENSHOT)), "the in-game hatch GUI was captured");
            finish(null);
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void prepare(EntityPlayerMP player) {
        var world = player.worldObj;
        player.playerNetServerHandler.setPlayerLocation(0.5, Y + 2, -5.5, 0, 0);
        controller = place(world, new int[] { 0, 0, 0 }, GTNRMachineID.LargeFragmentWorldCollector.id);
        controller.disableWorking();
        collector = (LargeFragmentWorldCollector) controller.getMetaTileEntity();
        collector.fixAllIssues();
        collector.construct(new ItemStack(Items.stick), false);
        var casings = ports('X');
        dedicatedInputPosition = ports('I').get(0);
        optionalPortPosition = casings.get(2);
        var dimensionTile = place(world, dedicatedInputPosition, GTNRMachineID.FragmentDimensionHatch.id);
        hatch = (FragmentDimensionHatch) dimensionTile.getMetaTileEntity();
        energy = place(
            world,
            casings.get(0),
            ItemList.Hatch_Energy_LuV.get(1)
                .getItemDamage());
        place(
            world,
            casings.get(1),
            ItemList.Hatch_Output_LV.get(1)
                .getItemDamage());
        output = place(
            world,
            ports('O').get(0),
            ItemList.Hatch_Output_Bus_HV.get(1)
                .getItemDamage());
        place(world, optionalPortPosition, GTNRMachineID.FragmentDimensionHatch.id);
        require(!collector.checkStructure(true, controller), "two dimension hatches fail formation");
        input = place(
            world,
            casings.get(2),
            ItemList.Hatch_Input_Bus_LV.get(1)
                .getItemDamage());
        require(
            collector.checkStructure(true, controller),
            "one dimension hatch forms at the dedicated input position");
        require(
            dimensionTile.getSizeInventory() == 1 && hatch.getDimensionMarker() == null,
            "the dimension hatch has one empty slot");
        require(
            dimensionTile.getInventoryStackLimit() == 1 && !hatch.isItemValidForSlot(0, new ItemStack(Items.apple)),
            "the dimension slot holds one marker and rejects unrelated items");
        overworld = ModsItemlist.DimensionOverworld.get(1);
        moon = ModsItemlist.DimensionMoon.get(1);
        drill = GTOreDictUnificator.get(OrePrefixes.toolHeadDrill, Materials.Steel, 64);
        require(overworld != null && moon != null && drill != null, "fixture inputs are registered");
        require(
            hatch.isItemValidForSlot(0, overworld) && hatch.isItemValidForSlot(0, moon),
            "the hatch accepts both registered dimension marker types");
        require(
            hatch.inventoryHandler.isItemValid(0, overworld)
                && !hatch.inventoryHandler.isItemValid(0, new ItemStack(Items.apple)),
            "the GUI inventory accepts dimension markers and filters other items");
        steelFluidRecipes = recipes(overworld).stream()
            .filter(recipe -> recipe.getMetadataOrDefault(DRILL_CONSUMPTION_CHANCE, 0) > 0)
            .filter(recipe -> recipe.mInputs[DRILL_INPUT_INDEX].isItemEqual(drill))
            .toList();
        require(!steelFluidRecipes.isEmpty(), "the Overworld has steel drill recipes");
        var backend = GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES.getBackend();
        report.append("Indexed recipes: Overworld ")
            .append(
                backend.getDimensionRecipes(overworld)
                    .size())
            .append(", Moon ")
            .append(
                backend.getDimensionRecipes(moon)
                    .size())
            .append('\n');
        idleCollector = new NBTTagCompound();
        collector.saveNBTData(idleCollector);
        controller.enableWorking();
    }

    private void prepareManualTrial(World world) {
        controller.disableWorking();
        collector.loadNBTData((NBTTagCompound) idleCollector.copy());
        for (int slot = 0; slot < output.getSizeInventory(); slot++) output.setInventorySlotContents(slot, null);
        var ordinaryInput = place(
            world,
            dedicatedInputPosition,
            ItemList.Hatch_Input_Bus_LV.get(1)
                .getItemDamage());
        require(collector.checkStructure(true, controller), "the original input bus layout still forms");
        GTRecipe recipe = normalRecipes(overworld).stream()
            .filter(candidate -> candidate.mInputs.length == 2 && candidate.mDuration >= 100)
            .findFirst()
            .orElseThrow();
        var inputBus = (MTEHatchInputBus) ordinaryInput.getMetaTileEntity();
        ItemStack circuit = Arrays.stream(recipe.mInputs)
            .filter(stack -> stack != null && GTUtility.isAnyIntegratedCircuit(stack))
            .findFirst()
            .orElseThrow();
        ordinaryInput.setInventorySlotContents(0, overworld.copy());
        ordinaryInput.setInventorySlotContents(inputBus.getCircuitSlot(), GTUtility.copyAmount(1, circuit));
        controller.enableWorking();
        stage++;
        trialTime = 0;
    }

    private void setTrial(ItemStack marker, ItemStack head) {
        controller.disableWorking();
        // Discard jobs from the previous trial so only new recipe starts are observed.
        collector.loadNBTData((NBTTagCompound) idleCollector.copy());
        hatch.inventoryHandler.extractItem(0, 1, false);
        if (marker != null) require(
            hatch.inventoryHandler.insertItem(0, marker.copy(), false) == null,
            "the GUI inventory accepted the new marker",
            false);
        input.setInventorySlotContents(0, head == null ? null : head.copy());
        var actual = hatch.getDimensionMarker();
        require(
            marker == null ? actual == null : actual != null && actual.isItemEqual(marker),
            "the dimension slot change is reflected on the server",
            false);
        controller.enableWorking();
        stage++;
        trialTime = 0;
    }

    private void setWearChance(int chance) {
        // Change the isolated game recipes only so the physical debit can be asserted without a random failure.
        for (var recipe : steelFluidRecipes) recipe.getMetadataStorage()
            .store(DRILL_CONSUMPTION_CHANCE, chance);
    }

    private List<int[]> ports(char symbol) {
        String[][] shape = MultiblockStructureLoader.load("large_fragment_world_collector");
        var ports = new ArrayList<int[]>();
        for (int row = 0; row < shape.length; row++) {
            for (int depth = 0; depth < shape[row].length; depth++) {
                String line = shape[row][depth];
                for (int col = 0; col < line.length(); col++) {
                    if (line.charAt(col) != symbol) continue;
                    var pos = new int[3];
                    collector.getExtendedFacing()
                        .getWorldOffset(new int[] { col - 1, row - 3, depth }, pos);
                    ports.add(pos);
                }
            }
        }
        return ports;
    }

    private static BaseMetaTileEntity place(World world, int[] position, int id) {
        int x = position[0], y = Y + position[1], z = position[2];
        world.setBlockToAir(x, y, z);
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        var tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    private static List<GTRecipe> recipes(ItemStack marker) {
        return GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES.getAllRecipes()
            .stream()
            .filter(
                recipe -> Arrays.stream(recipe.mInputs)
                    .anyMatch(ingredient -> ingredient != null && marker.isItemEqual(ingredient)))
            .toList();
    }

    private static List<GTRecipe> normalRecipes(ItemStack marker) {
        return recipes(marker).stream()
            .filter(recipe -> recipe.mFluidOutputs.length == 0)
            .toList();
    }

    private static List<Integer> matchingCircuits(List<RecipeThreadTask> tasks, List<GTRecipe> candidates) {
        var circuits = new ArrayList<Integer>();
        for (var task : tasks) {
            var outputs = new ArrayList<ItemStack>();
            task.getOutputs()
                .appendDisplayItems(outputs);
            var matching = candidates.stream()
                .filter(
                    recipe -> outputs.stream()
                        .allMatch(output -> contains(recipe.mOutputs, output)))
                .filter(recipe -> {
                    for (int index = 0; index < recipe.mOutputs.length; index++) {
                        ItemStack item = recipe.mOutputs[index];
                        if (item != null && recipe.getOutputChance(index) >= 10000 && !contains(outputs, item))
                            return false;
                    }
                    return true;
                })
                .flatMap(recipe -> Arrays.stream(recipe.mInputs))
                .filter(stack -> stack != null && GTUtility.isAnyIntegratedCircuit(stack))
                .map(ItemStack::getItemDamage)
                .distinct()
                .toList();
            if (matching.size() == 1 && !circuits.contains(matching.get(0))) circuits.add(matching.get(0));
        }
        return circuits;
    }

    private static boolean contains(ItemStack[] stacks, ItemStack wanted) {
        return Arrays.stream(stacks)
            .anyMatch(stack -> stack != null && wanted.isItemEqual(stack));
    }

    private static boolean contains(List<ItemStack> stacks, ItemStack wanted) {
        return stacks.stream()
            .anyMatch(stack -> stack != null && wanted.isItemEqual(stack));
    }

    private void require(boolean condition, String message) {
        require(condition, message, true);
    }

    private void require(boolean condition, String message, boolean log) {
        if (!condition) throw new AssertionError(message);
        if (log) report.append(message)
            .append('\n');
    }

    private void finish(Throwable error) {
        finished = true;
        try {
            if (error != null) {
                error.printStackTrace();
                report.append(error)
                    .append('\n');
            }
            Files.writeString(Path.of("report.txt"), report);
            Files.writeString(Path.of("result.txt"), error == null ? "PASS\n" : "FAIL\n");
        } catch (Exception writeFailure) {
            writeFailure.printStackTrace();
        }
        Minecraft.getMinecraft()
            .shutdown();
    }
}
