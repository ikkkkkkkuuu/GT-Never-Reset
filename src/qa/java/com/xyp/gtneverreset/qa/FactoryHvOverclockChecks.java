package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadTask;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactory;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import appeng.api.AEApi;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.container.ContainerNull;
import appeng.tile.networking.TileCreativeEnergyCell;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyMulti;

@Mod(
    modid = "factoryhvoverclockqa",
    name = "Factory HV Overclock QA",
    version = "1",
    dependencies = "required-after:gtneverreset")
public final class FactoryHvOverclockChecks {

    private static final int Y = 85;
    private static final int ORDER_TO_FINISH = 649;
    private static final int COMPLETION_LIMIT = 1150;
    private static final List<Trial> TRIALS = List.of(
        new Trial(1, 0),
        new Trial(546, 0),
        new Trial(547, 0),
        new Trial(648, 0),
        new Trial(1, 1),
        new Trial(ORDER_TO_FINISH, 0));
    private final List<BaseMetaTileEntity> energy = new ArrayList<>();
    private final List<String> observations = new ArrayList<>();
    private final StringBuilder csv = new StringBuilder(
        "offered,secondary_offered,thread,parallel,duration_ticks,eut,used_before_eut\n");
    private final StringBuilder completionCsv = new StringBuilder(
        "event,elapsed_ticks,thread,parallel,duration_ticks,progress_ticks,eut,delivered\n");
    private final long[] serverTickNanos = new long[COMPLETION_LIMIT + 1];
    private final ItemStack ingredient = new ItemStack(Items.record_13);
    private final ItemStack product = new ItemStack(Items.netherbrick);
    private final ItemStack secondaryIngredient = new ItemStack(Items.record_cat);
    private final ItemStack secondaryProduct = new ItemStack(Items.clock);
    private BaseMetaTileEntity tile;
    private BaseMetaTileEntity output;
    private UniversalJointFactory factory;
    private CrossRecipeMEPatternBuffer hatch;
    private TileCreativeEnergyCell aePower;
    private ICraftingPatternDetails details;
    private ICraftingPatternDetails secondaryDetails;
    private NBTTagCompound idleState;
    private NBTTagCompound boostedState;
    private int energyId;
    private int tick;
    private int submittedAt;
    private int releaseTick;
    private int boostTick;
    private int fallbackStartedAt;
    private int fallbackProgress;
    private long tickStartedNanos;
    private int orderIndex;
    private int frames;
    private int menuTicks;
    private boolean launched;
    private boolean finished;
    private boolean recordedStart;
    private boolean reloaded;
    private boolean accelerated;
    private volatile boolean ready;
    private volatile Throwable failure;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        registerHatch();
    }

    private void registerHatch() {
        for (int id = GregTechAPI.METATILEENTITIES.length - 2; id > 0; id--) {
            if (GregTechAPI.METATILEENTITIES[id] != null || id > Short.MAX_VALUE) continue;
            energyId = id;
            new MTEHatchEnergyMulti(id, "qa.hatch.energy.hv16", "QA 16A HV Energy Hatch", VoltageIndex.HV, 16);
            return;
        }
        throw new IllegalStateException("No free MetaTileEntity ID");
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (ready || failure != null || !GregTechAPI.sFullLoadFinished) return;
        if (event.phase == TickEvent.Phase.START) {
            tickStartedNanos = submittedAt != 0 && TRIALS.get(orderIndex)
                .primary() == ORDER_TO_FINISH ? System.nanoTime() : 0;
            return;
        }
        long tickNanos = tickStartedNanos == 0 ? 0 : System.nanoTime() - tickStartedNanos;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        try {
            tick++;
            if (tile == null) prepare((EntityPlayerMP) players.get(0));
            for (var source : energy)
                source.setStoredEU(((MTEHatchEnergyMulti) source.getMetaTileEntity()).maxEUStore());
            if (tick > 2400) throw new IllegalStateException(
                "Timeout, trial " + orderIndex
                    + ": "
                    + factory.getCheckRecipeResult()
                        .getDisplayString()
                    + ", AE active="
                    + hatch.isActive());
            if (tick < 120 || !hatch.isActive()) return;
            if (orderIndex == TRIALS.size()) {
                verifyPowerFallback();
                return;
            }
            Trial trial = TRIALS.get(orderIndex);
            if (submittedAt == 0) {
                startTrial(trial);
                return;
            }
            int elapsed = tick - submittedAt;
            if (tickNanos > 0 && elapsed <= COMPLETION_LIMIT) serverTickNanos[elapsed] = tickNanos;
            if (elapsed < 22) return;
            var tasks = factory.getRecipeThreadTasks();
            if (trial.secondary() > 0 && tasks.size() < 2 && elapsed < 80) return;
            if (!recordedStart) {
                recordStart(trial, tasks);
                recordedStart = true;
            }
            if (trial.primary() == ORDER_TO_FINISH && !observeCompletion(elapsed, tasks)) return;
            finishTrial();
        } catch (Throwable error) {
            failure = error;
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            mc.gameSettings.pauseOnLostFocus = false;
            if (!launched && mc.theWorld == null && mc.currentScreen != null && ++menuTicks > 60) {
                launched = true;
                System.out.println(
                    "FACTORY_HV_QA opening isolated world from " + mc.currentScreen.getClass()
                        .getName());
                mc.launchIntegratedServer(
                    "hv-overclock-" + System.currentTimeMillis(),
                    "HV overclock QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw failure;
            if (ready && !(mc.currentScreen instanceof ResultsScreen)) mc.displayGuiScreen(new ResultsScreen());
        } catch (Throwable error) {
            finish(error);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ready
            || finished
            || !(Minecraft.getMinecraft().currentScreen instanceof ResultsScreen)
            || ++frames < 12) return;
        try {
            var mc = Minecraft.getMinecraft();
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "factory-hv-overclock.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            require(Files.isRegularFile(Path.of("screenshots", "factory-hv-overclock.png")), "screenshot saved");
            finish(null);
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void prepare(EntityPlayerMP player) throws Exception {
        World world = player.worldObj;
        player.playerNetServerHandler.setPlayerLocation(0.5, Y + 5, -5.5, 0, 15);
        tile = place(world, new int[] { 0, 0, 0 }, GTNRMachineID.UniversalJointFactory.id);
        tile.disableWorking();
        factory = (UniversalJointFactory) tile.getMetaTileEntity();
        factory.fixAllIssues();
        factory.construct(new ItemStack(Items.stick), false);
        var ports = structurePorts();
        for (int index = 0; index < 32; index++) {
            var source = place(world, ports.get(index), energyId);
            var sourceHatch = (MTEHatchEnergyMulti) source.getMetaTileEntity();
            sourceHatch.setAmperes(16);
            require(
                sourceHatch.maxWorkingAmperesIn() == 16 && source.getInputVoltage() == 512,
                "16A HV hatch " + index);
            energy.add(source);
        }
        place(
            world,
            ports.get(32),
            ItemList.Hatch_Input_Bus_LV.get(1)
                .getItemDamage());
        output = place(
            world,
            ports.get(33),
            ItemList.Hatch_Output_Bus_HV.get(1)
                .getItemDamage());
        place(
            world,
            ports.get(34),
            ItemList.Hatch_Maintenance.get(1)
                .getItemDamage());
        var hatchTile = place(world, ports.get(35), GTNRMachineID.CrossRecipeMEPatternBuffer.id);
        hatch = (CrossRecipeMEPatternBuffer) hatchTile.getMetaTileEntity();
        hatchTile.enableWorking();
        require(factory.checkStructure(true, tile), "physical multiblock formed");
        require(
            factory.getExoticAndNormalEnergyHatchList()
                .size() == 32,
            "all 32 energy hatches recognized");
        require(
            factory.getMaxParallelRecipes() == 256 && factory.getSpeedMultiplier() == 1
                && factory.getEUtMultiplier() == 1,
            "default HV parallel and HV glass modifiers");
        factory.setMachineMode(2);
        factory.setBatchMode(false);
        factory.setInputSeparation(true);
        idleState = new NBTTagCompound();
        factory.saveNBTData(idleState);

        var extractor = GTValues.RA.stdBuilder()
            .itemInputs(ingredient.copy())
            .itemOutputs(product.copy())
            .duration(2400)
            .eut(30)
            .addTo(RecipeMaps.extractorRecipes);
        var compressor = GTValues.RA.stdBuilder()
            .itemInputs(secondaryIngredient.copy())
            .itemOutputs(secondaryProduct.copy())
            .duration(2400)
            .eut(16)
            .addTo(RecipeMaps.compressorRecipes);
        require(!extractor.isEmpty() && !compressor.isEmpty(), "both test recipes registered");
        hatch.onPatternChange(0, pattern(ingredient, product, RecipeMaps.extractorRecipes));
        hatch.onPatternChange(1, pattern(secondaryIngredient, secondaryProduct, RecipeMaps.compressorRecipes));
        var patterns = hatch.inventories();
        while (patterns.hasNext()) {
            var candidate = patterns.next()
                .getPatternDetails();
            var input = candidate.getCondensedInputs()[0].getItem();
            if (input == ingredient.getItem()) details = candidate;
            if (input == secondaryIngredient.getItem()) secondaryDetails = candidate;
        }
        require(details != null && secondaryDetails != null, "both cross-recipe patterns installed");

        hatch.getProxy()
            .onReady();
        aePower = new TileCreativeEnergyCell();
        aePower.setWorldObj(world);
        aePower.xCoord = 50;
        aePower.yCoord = Y;
        aePower.zCoord = 0;
        aePower.getProxy()
            .onReady();
        AEApi.instance()
            .createGridConnection(
                hatch.getProxy()
                    .getNode(),
                aePower.getProxy()
                    .getNode());
    }

    private static ItemStack pattern(ItemStack ingredient, ItemStack product, RecipeMap<?> map) {
        var encoded = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        var tag = new NBTTagCompound();
        var inputs = new NBTTagList();
        var outputs = new NBTTagList();
        inputs.appendTag(
            AEItemStack.create(ingredient.copy())
                .toNBTGeneric());
        outputs.appendTag(
            AEItemStack.create(product.copy())
                .toNBTGeneric());
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        encoded.setTagCompound(tag);
        PatternRecipeMap.write(encoded, map.unlocalizedName);
        return encoded;
    }

    private List<int[]> structurePorts() {
        String[][] shape = MultiblockStructureLoader.load("universal_joint_factory");
        var ports = new ArrayList<int[]>();
        for (int depth = 0; depth < shape[0].length; depth++) {
            for (int row = shape.length - 1; row >= 0; row--) {
                String line = shape[row][depth];
                for (int col = 0; col < line.length(); col++) {
                    char symbol = line.charAt(col);
                    if (symbol != 'X' && symbol != 'Q') continue;
                    var pos = new int[3];
                    factory.getExtendedFacing()
                        .getWorldOffset(new int[] { col - 24, row - 15, depth }, pos);
                    ports.add(pos);
                }
            }
        }
        require(ports.size() >= 36, "structure has enough physical ports");
        return ports;
    }

    private static BaseMetaTileEntity place(World world, int[] position, int id) {
        int x = position[0], y = Y + position[1], z = position[2];
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        var block = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        block.setInitialValuesAsNBT(null, (short) id);
        block.setFrontFacing(ForgeDirection.NORTH);
        return block;
    }

    private void startTrial(Trial trial) {
        require(
            factory.getRecipeThreadTasks()
                .isEmpty(),
            "factory starts empty");
        require(
            factory.getMaxInputVoltage() == 512 && factory.getMaxInputEu() == 262_144,
            "controller recognizes 512 V and 262144 EU/t: " + factory.getMaxInputVoltage()
                + "/"
                + factory.getMaxInputEu());
        push(details, trial.primary());
        if (trial.secondary() > 0) push(secondaryDetails, trial.secondary());
        tile.enableWorking();
        submittedAt = tick;
    }

    private void push(ICraftingPatternDetails pattern, int count) {
        var input = new MEInventoryCrafting(new ContainerNull(), 1, 1);
        input.setInventorySlotContents(
            0,
            pattern.getAEInputs()[0].copy()
                .setStackSize(count));
        require(hatch.pushPattern(pattern, input), "ME accepted order of " + count);
    }

    private void recordStart(Trial trial, List<RecipeThreadTask> tasks) {
        long used = 0;
        int total = 0;
        var line = new StringBuilder(String.format("%5s  |  ", trial.label()));
        for (var task : tasks) {
            require(
                task.getProgress() > 0,
                "live thread advanced: " + task.getThread() + " for order " + trial.label());
            csv.append(
                String.format(
                    "%d,%d,%d,%d,%d,%d,%d%n",
                    trial.primary(),
                    trial.secondary(),
                    task.getThread(),
                    task.getParallels(),
                    task.getDuration(),
                    task.getEUt(),
                    used));
            used += task.getEUt();
            total += task.getParallels();
            line.append(
                String.format("%d x %ds @ %d EU/t   ", task.getParallels(), task.getDuration() / 20, task.getEUt()));
        }
        require(
            total == trial.primary() + trial.secondary(),
            "all inputs entered live jobs, order=" + trial.label()
                + ", started="
                + total
                + ", tasks="
                + line
                + ", recipe result="
                + factory.getCheckRecipeResult()
                    .getDisplayString());
        require(used <= factory.getMaxInputEu(), "threads stay within power cap");
        if (trial.secondary() > 0) {
            require(tasks.size() == 2, "different recipe pools started both threads");
            var results = new ArrayList<ItemStack>();
            for (var task : tasks) task.getOutputs()
                .appendDisplayItems(results);
            require(
                countItems(results, product) == 1 && countItems(results, secondaryProduct) == 1,
                "each pattern produced its matching item");
        } else if (trial.primary() == 1) {
            var task = tasks.get(0);
            require(
                task.getDuration() == 600 && task.getEUt() == 480,
                "single LV craft still takes 600 ticks at 480 EU/t");
        } else if (trial.primary() == ORDER_TO_FINISH) {
            require(tasks.size() == 3, "649 produces three initial batches");
            var tail = tasks.get(2);
            require(
                tail.getParallels() == 137 && tail.getDuration() == 2400 && tail.getEUt() == 4110,
                "649 initially leaves a 137-wide tail at base speed");
            recordCompletion("start", tick - submittedAt, tail, 0);
        }
        observe(line.toString());
    }

    private boolean observeCompletion(int elapsed, List<RecipeThreadTask> tasks) throws Exception {
        int delivered = 0;
        for (int slot = 0; slot < output.getSizeInventory(); slot++) {
            var stack = output.getStackInSlot(slot);
            if (stack != null && stack.isItemEqual(product)) delivered += stack.stackSize;
        }
        require(delivered <= ORDER_TO_FINISH, "output count has no duplicates");
        require(elapsed <= COMPLETION_LIMIT, "649 completion exceeded " + COMPLETION_LIMIT + " ticks: " + delivered);
        if (releaseTick == 0 && delivered >= 512) releaseTick = elapsed;
        long used = 0;
        for (var task : tasks) {
            if (!task.isFinished()) used += task.getEUt();
        }
        require(used <= factory.getMaxInputEu(), "accelerated threads stay within power cap");

        if (!reloaded && elapsed >= 300) {
            require(tasks.size() == 3, "reload before the first outputs");
            var saved = new NBTTagCompound();
            factory.saveNBTData(saved);
            factory.loadNBTData(saved);
            var tail = factory.getRecipeThreadTasks()
                .get(2);
            require(
                tail.getProgress() == tasks.get(2)
                    .getProgress(),
                "active tail progress survived NBT reload");
            recordCompletion("reload", elapsed, tail, delivered);
            reloaded = true;
        }
        if (!accelerated && delivered >= 512
            && tasks.size() == 1
            && tasks.get(0)
                .getEUt() > 4110) {
            var tail = tasks.get(0);
            int remaining = tail.getDuration() - tail.getProgress();
            require(remaining > 0 && remaining <= 600, "tail picked up overclock after power freed");
            recordCompletion("accelerate", elapsed, tail, delivered);
            boostTick = elapsed;
            boostedState = new NBTTagCompound();
            factory.saveNBTData(boostedState);
            observe(String.format("  %d ticks | tail now @ %d EU/t, %d ticks left", elapsed, tail.getEUt(), remaining));
            accelerated = true;
        }
        if (delivered < ORDER_TO_FINISH) return false;
        require(tasks.isEmpty() && accelerated && reloaded, "all 649 outputs arrived after acceleration and reload");
        recordCompletion("complete", elapsed, null, delivered);
        observe(String.format("  %d ticks | all %d items delivered (%.2fs)", elapsed, delivered, elapsed / 20D));
        writeServerTimings(elapsed);
        return true;
    }

    private void writeServerTimings(int elapsed) throws Exception {
        var timing = new StringBuilder("elapsed_ticks,integrated_server_tick_ns,first_512_delivered,tail_boost\n");
        for (int i = 1; i <= elapsed; i++) {
            timing.append(i)
                .append(',')
                .append(serverTickNanos[i])
                .append(',')
                .append(i == releaseTick)
                .append(',')
                .append(i == boostTick)
                .append('\n');
        }
        Files.writeString(Path.of("server-tick-times.csv"), timing);
        long[] steady = Arrays.copyOfRange(serverTickNanos, 23, releaseTick);
        Arrays.sort(steady);
        System.out.printf(
            "FACTORY_HV_QA integrated server ticks: steady p50=%.3f ms, p95=%.3f ms, max=%.3f ms; release=%.3f ms, boost=%.3f ms%n",
            steady[steady.length / 2] / 1e6,
            steady[(steady.length - 1) * 95 / 100] / 1e6,
            steady[steady.length - 1] / 1e6,
            serverTickNanos[releaseTick] / 1e6,
            serverTickNanos[boostTick] / 1e6);
    }

    private void recordCompletion(String event, int elapsed, RecipeThreadTask task, int delivered) {
        completionCsv.append(
            String.format(
                "%s,%d,%d,%d,%d,%d,%d,%d%n",
                event,
                elapsed,
                task == null ? -1 : task.getThread(),
                task == null ? 0 : task.getParallels(),
                task == null ? 0 : task.getDuration(),
                task == null ? 0 : task.getProgress(),
                task == null ? 0 : task.getEUt(),
                delivered));
    }

    private void finishTrial() {
        tile.disableWorking();
        factory.loadNBTData((NBTTagCompound) idleState.copy());
        submittedAt = 0;
        recordedStart = false;
        orderIndex++;
    }

    private void verifyPowerFallback() throws Exception {
        if (fallbackStartedAt == 0) {
            require(boostedState != null, "accelerated task was saved");
            factory.loadNBTData(boostedState);
            var tail = factory.getRecipeThreadTasks()
                .get(0);
            require(tail.getEUt() == 65_760 && tail.getParallels() == 137, "accelerated rate survived NBT reload");
            for (var source : energy) ((MTEHatchEnergyMulti) source.getMetaTileEntity()).setAmperes(1);
            require(factory.getMaxInputEu() == 16_384, "rated power dropped below boosted task demand");
            tile.enableWorking();
            fallbackStartedAt = tick;
            return;
        }
        int elapsed = tick - fallbackStartedAt;
        if (elapsed < 3) return;
        var tasks = factory.getRecipeThreadTasks();
        require(tasks.size() == 1, "replayed tail remains active");
        var tail = tasks.get(0);
        require(tail.getEUt() == 4_110 && tail.getParallels() == 137, "tail restored its prior sustainable rate");
        if (elapsed == 3) {
            fallbackProgress = tail.getProgress();
            return;
        }
        if (elapsed < 6) return;
        require(tail.getProgress() > fallbackProgress, "thread keeps advancing at restored power");
        observe(String.format("  16,384 EU/t supply | reloaded tail resumes @ %,d EU/t", tail.getEUt()));
        recordCompletion("fallback_replay", elapsed, tail, 0);
        tile.disableWorking();
        factory.loadNBTData((NBTTagCompound) idleState.copy());
        for (var source : energy) ((MTEHatchEnergyMulti) source.getMetaTileEntity()).setAmperes(16);
        Files.writeString(Path.of("threads.csv"), csv);
        Files.writeString(Path.of("completion.csv"), completionCsv);
        ready = true;
    }

    private static int countItems(List<ItemStack> stacks, ItemStack wanted) {
        int count = 0;
        for (var stack : stacks) {
            if (stack.isItemEqual(wanted)) count += stack.stackSize;
        }
        return count;
    }

    private void observe(String line) {
        observations.add(line);
        System.out.println("FACTORY_HV_QA " + line);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void finish(Throwable error) {
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : "FAIL " + error);
        } catch (Exception writeFailure) {
            writeFailure.printStackTrace();
        }
        Minecraft.getMinecraft()
            .shutdown();
    }

    private record Trial(int primary, int secondary) {

        private String label() {
            return secondary == 0 ? Integer.toString(primary) : primary + "+" + secondary;
        }
    }

    private final class ResultsScreen extends GuiScreen {

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            drawString(fontRendererObj, "Universal Joint Factory | live thread readings", 15, 11, 0xFFFFFF);
            drawString(
                fontRendererObj,
                "32 x 16A HV | 512 V | 262,144 EU/t | LV recipe: 120s @ 30 EU/t",
                15,
                25,
                0xB1CDDD);
            drawString(fontRendererObj, "Input count | parallel x seconds @ thread EU/t", 15, 40, 0xFFFFFF);
            for (int i = 0; i < observations.size(); i++) {
                String line = observations.get(i);
                drawString(fontRendererObj, line, 15, 57 + 13 * i, line.contains("120s") ? 0xFFBA6B : 0xE5E5E5);
            }
        }
    }
}
