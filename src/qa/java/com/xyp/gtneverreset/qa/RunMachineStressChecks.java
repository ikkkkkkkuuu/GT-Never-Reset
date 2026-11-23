package com.xyp.gtneverreset.qa;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME.PatternSlot;
import com.xyp.gtneverreset.common.machine.multiMachineBase.GTNRMultiBlockBase;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactory;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.api.AEApi;
import appeng.container.ContainerNull;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;

@Mod(
    modid = "runmachinestressqa",
    name = "runMachine stress QA",
    version = "1",
    dependencies = "required-after:gtneverreset")
public final class RunMachineStressChecks {

    private static final int SLOTS = 16, VALID = 12, PERIOD = 20, WARMUP = 240, MEASURE = 800;
    private static final GTNRMachineID[] HATCHES = { GTNRMachineID.SuperCraftingInputME,
        GTNRMachineID.CircuitMEPatternBuffer, GTNRMachineID.CrossRecipeMEPatternBuffer };
    private final Item input = new Item().setHasSubtypes(true);
    private final Item product = new Item().setHasSubtypes(true);
    private final StringBuilder rows = new StringBuilder(
        "tick,ore_ns,factory_ns,ore_started,factory_started,ore_done,factory_done,ore_live,factory_live\n");
    private final Path output = Path.of(System.getProperty("gtnr.bench.output"));
    private MethodHandle runMachine;
    private Rig ore, factory;
    private int tick;
    private boolean ready;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerItem(input, "bench_input");
        GameRegistry.registerItem(product, "bench_product");
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @Mod.EventHandler
    public void start(FMLServerStartedEvent event) throws Exception {
        var method = GTNRMultiBlockBase.class.getDeclaredMethod("runMachine", IGregTechTileEntity.class, long.class);
        method.setAccessible(true);
        runMachine = MethodHandles.lookup()
            .unreflect(method);
        World world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        ore = new Rig(world, false);
        factory = new Rig(world, true);
        ready = true;
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) throws Throwable {
        if (!ready || event.phase != TickEvent.Phase.END) return;
        try {
            if (tick % PERIOD == 0) {
                ore.wave();
                factory.wave();
            }
            // Alternate controller order to distribute first-call and scheduling effects.
            if ((tick & 1) == 0) {
                ore.step();
                factory.step();
            } else {
                factory.step();
                ore.step();
            }
            rows.append(tick)
                .append(',')
                .append(ore.ns)
                .append(',')
                .append(factory.ns)
                .append(',')
                .append(ore.started)
                .append(',')
                .append(factory.started)
                .append(',')
                .append(ore.done)
                .append(',')
                .append(factory.done)
                .append(',')
                .append(ore.live)
                .append(',')
                .append(factory.live)
                .append('\n');
            tick++;
            if (tick % PERIOD == 0) {
                ore.verify(tick / PERIOD);
                factory.verify(tick / PERIOD);
            }
            if (tick == WARMUP + MEASURE) {
                Files.writeString(output.resolve("samples.csv"), rows);
                Files.writeString(
                    output.resolve("result.txt"),
                    "PASS\nwarmup=" + WARMUP
                        + "\nmeasured="
                        + MEASURE
                        + "\nore capacity="
                        + ore.machine.getTotalRecipeThreads()
                        + ", factory capacity="
                        + factory.machine.getTotalRecipeThreads()
                        + "\nore outputs="
                        + Arrays.toString(ore.received)
                        + "\nfactory outputs="
                        + Arrays.toString(factory.received)
                        + "\n");
                ready = false;
                MinecraftServer.getServer()
                    .initiateShutdown();
            }
        } catch (Throwable failure) {
            ready = false;
            Files.writeString(output.resolve("samples-incomplete.csv"), rows);
            Files.writeString(output.resolve("result.txt"), "FAIL at tick " + tick + ": " + failure + "\n");
            MinecraftServer.getServer()
                .initiateShutdown();
            throw failure;
        }
    }

    private final class Rig {

        final List<PatternSlot<?>> slots = new ArrayList<>();
        final BenchTile tile;
        final GTNRMultiBlockBase<?> machine;
        final List<BaseMetaTileEntity> power = new ArrayList<>();
        final MTEHatchOutputBus bus;
        final int[] perWave = new int[6], received = new int[6];
        int live, done, started;
        long ns;

        Rig(World world, boolean cross) throws Exception {
            tile = tile(world, (cross ? GTNRMachineID.UniversalJointFactory : GTNRMachineID.LargeOreProcessor).id);
            machine = (GTNRMultiBlockBase<?>) tile.getMetaTileEntity();
            machine.mMachine = true;
            machine.fixAllIssues();
            tile.enableWorking();
            if (cross) {
                var controller = (UniversalJointFactory) machine;
                controller.setMachineMode(
                    controller.getAvailableRecipeMaps()
                        .indexOf(RecipeMaps.compressorRecipes));
            }
            for (int i = 0; i < 4; i++) {
                var energy = tile(
                    world,
                    ItemList.Hatch_Energy_MV.get(1)
                        .getItemDamage());
                power.add(energy);
                machine.mEnergyHatches.add((MTEHatchEnergy) energy.getMetaTileEntity());
            }
            bus = (MTEHatchOutputBus) tile(
                world,
                ItemList.Hatch_Output_Bus_HV.get(1)
                    .getItemDamage()).getMetaTileEntity();
            machine.mOutputBusses.add(bus);
            for (int h = 0; h < HATCHES.length; h++) {
                var hatch = (SuperMTEHatchCraftingInputME) tile(world, HATCHES[h].id).getMetaTileEntity();
                machine.mDualInputHatches.add(hatch);
                Field inventory = SuperMTEHatchCraftingInputME.class.getDeclaredField("internalInventory");
                inventory.setAccessible(true);
                var backing = (PatternSlot<?>[]) inventory.get(hatch);
                var create = hatch.getClass()
                    .getDeclaredMethod("createPatternSlot", ItemStack.class, NBTTagCompound.class);
                create.setAccessible(true);
                for (int i = 0; i < SLOTS; i++) {
                    int meta = h * SLOTS + i;
                    RecipeMap<?> pool = !cross ? GTNRRecipeMaps.ORE_PROCESSING_RECIPES
                        : h == 2 && (i & 1) != 0 ? RecipeMaps.extractorRecipes : RecipeMaps.compressorRecipes;
                    int outputMeta = h + (pool == RecipeMaps.extractorRecipes ? 3 : 0);
                    var ingredient = new ItemStack(input, 2, meta);
                    var result = new ItemStack(product, 1, outputMeta);
                    if (GTValues.RA.stdBuilder()
                        .itemInputs(ingredient.copy())
                        .itemOutputs(result.copy())
                        .duration(4)
                        .eut(1)
                        .addTo(pool)
                        .isEmpty()) throw new AssertionError("fixture recipe " + meta);
                    ItemStack encoded = pattern(ingredient, result);
                    if (h == 2) PatternRecipeMap.write(encoded, pool.unlocalizedName);
                    PatternSlot<?> slot = (PatternSlot<?>) create.invoke(hatch, encoded, null);
                    backing[i] = slot;
                    slots.add(slot);
                    if (i < VALID) perWave[outputMeta]++;
                    else deliver(slot, meta, 1);
                }
            }
        }

        void wave() {
            for (int i = 0; i < slots.size(); i++) if (i % SLOTS < VALID) deliver(slots.get(i), i, 2);
        }

        void step() throws Throwable {
            for (var energy : power) energy.setStoredEU(1000000);
            // Advance the outer GT tile/multiblock clocks and reproduce the transient inventory notification
            // normally reset by BaseMetaTileEntity.updateEntity(). The controllers are detached from world ticking.
            tile.beginTick(tick, tick % PERIOD == 0);
            machine.setTotalRunTime(tick + 1L);
            long beforeDone = machine.recipesDone;
            long t0 = System.nanoTime();
            runMachine.invokeExact(machine, (IGregTechTileEntity) tile, (long) tick);
            ns = System.nanoTime() - t0;
            done = Math.toIntExact(machine.recipesDone - beforeDone);
            int nowLive = machine.getRecipeThreadTasks()
                .size();
            started = nowLive - live + done;
            live = nowLive;
            for (int i = 0; i < bus.getSizeInventory(); i++) {
                ItemStack stack = bus.getStackInSlot(i);
                if (stack == null) continue;
                if (stack.getItem() != product) throw new AssertionError("unexpected output " + stack);
                received[stack.getItemDamage()] += stack.stackSize;
                bus.setInventorySlotContents(i, null);
            }
        }

        void verify(int waves) {
            for (int i = 0; i < received.length; i++) if (received[i] != waves * perWave[i]) throw new AssertionError(
                machine.getClass()
                    .getSimpleName() + " outputs "
                    + Arrays.toString(received)
                    + ", expected per wave "
                    + Arrays.toString(perWave)
                    + ", live="
                    + live
                    + ", check="
                    + machine.getCheckRecipeResult()
                        .getID()
                    + ", EU="
                    + machine.getMaxInputEu());
            if (live != 0) throw new AssertionError("unfinished work");
        }
    }

    private void deliver(PatternSlot<?> slot, int meta, int size) {
        var delivery = new MEInventoryCrafting(new ContainerNull(), 1, 1);
        delivery.setInventorySlotContents(0, AEItemStack.create(new ItemStack(input, size, meta)));
        if (!slot.insertItemsAndFluids(delivery)) throw new AssertionError("delivery rejected");
    }

    private static ItemStack pattern(ItemStack input, ItemStack output) {
        var pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound data = new NBTTagCompound();
        NBTTagList inputs = new NBTTagList(), outputs = new NBTTagList();
        inputs.appendTag(
            AEItemStack.create(input)
                .toNBTGeneric());
        outputs.appendTag(
            AEItemStack.create(output)
                .toNBTGeneric());
        data.setTag("in", inputs);
        data.setTag("out", outputs);
        pattern.setTagCompound(data);
        return pattern;
    }

    private static final class BenchTile extends BaseMetaTileEntity {

        void beginTick(long tick, boolean newInputs) {
            mTickTimer = tick + 1;
            mInventoryChanged = newInputs;
        }
    }

    private static BenchTile tile(World world, int id) {
        BenchTile tile = new BenchTile();
        tile.setWorldObj(world);
        tile.setInitialValuesAsNBT(null, (short) id);
        return tile;
    }
}
