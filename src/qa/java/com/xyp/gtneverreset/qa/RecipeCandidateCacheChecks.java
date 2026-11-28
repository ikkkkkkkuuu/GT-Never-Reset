package com.xyp.gtneverreset.qa;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.multiMachineBase.CrossRecipeProcessingLogic;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeCandidateCache;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadInputPreflight;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadOutputs;
import com.xyp.gtneverreset.common.machine.multiblock.LargeOreProcessor;
import com.xyp.gtneverreset.common.machine.multiblock.OreRecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactory;
import com.xyp.gtneverreset.common.me.PatternInputScheduler;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.common.me.PatternRecipeRouting;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.api.AEApi;
import appeng.container.ContainerNull;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.VoidingMode;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.lookup.GTOreDictLookupIngredient;
import gregtech.api.recipe.lookup.GTRecipeLookup;
import gregtech.api.recipe.lookup.GTRecipeLookupIngredient;
import gregtech.api.util.GTRecipe;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;

@Mod(
    modid = "recipecacheqa",
    name = "Recipe candidate cache QA",
    version = "1",
    dependencies = "required-after:gtneverreset")
public final class RecipeCandidateCacheChecks {

    @Mod.EventHandler
    public void ready(FMLServerStartedEvent event) throws Exception {
        Path result = Path.of(System.getProperty("gtnr.recipe.cache.result"));
        try {
            checkLookup();
            checkLiveInputs();
            checkOrePowerRetry();
            checkForeignStockingBusPreflight();
            checkThreadOutputs();
            checkAssemblies();
            benchmark(result.resolveSibling("trie-timing.txt"));
            Files.writeString(result, "PASS\n");
        } catch (Throwable error) {
            Files.writeString(result, "FAIL " + error + "\n");
            throw new AssertionError(error);
        } finally {
            MinecraftServer.getServer()
                .initiateShutdown();
        }
    }

    private static void checkLookup() throws InterruptedException {
        Iterator<GTRecipe> samples = RecipeMaps.assemblerRecipes.getAllRecipes()
            .iterator();
        GTRecipe first = samples.next(), second = samples.next();
        List<GTRecipeLookupIngredient> a = List.of(new GTOreDictLookupIngredient(1));
        List<GTRecipeLookupIngredient> b = List.of(new GTOreDictLookupIngredient(2));
        GTRecipeLookup lookup = new GTRecipeLookup();
        lookup.add(first, List.of(a));
        lookup.add(second, List.of(b));
        Class<?> nativeIterator = lookup.iterator(a)
            .getClass();
        try (var scope = RecipeCandidateCache.enter()) {
            checkWarm(lookup, List.of(a.get(0), b.get(0)), List.of(first, second), nativeIterator);
            checkWarm(lookup, List.of(b.get(0), a.get(0)), List.of(second, first), nativeIterator);
            List<GTRecipeLookupIngredient> empty = List.of(new GTOreDictLookupIngredient(3));
            checkWarm(lookup, empty, List.of(), nativeIterator);
            lookup.add(second, List.of(empty));
            checkWarm(lookup, empty, List.of(second), nativeIterator);
            GTRecipeLookup replacement = new GTRecipeLookup();
            replacement.add(first, List.of(empty));
            checkWarm(replacement, empty, List.of(first), nativeIterator);
        }
        if (lookup.iterator(a)
            .getClass() != nativeIterator) throw new AssertionError("scope leaked");
    }

    private static void checkLiveInputs() {
        ItemStack input = new ItemStack(Items.firework_charge, 4);
        input.setTagCompound(new NBTTagCompound());
        input.getTagCompound()
            .setString("fixture", "recipe-cache");
        GTRecipe recipe = GTValues.RA.stdBuilder()
            .itemInputs(input.copy())
            .itemOutputs(new ItemStack(Items.emerald))
            .nbtSensitive()
            .duration(20)
            .eut(8)
            .addTo(RecipeMaps.compressorRecipes)
            .iterator()
            .next();
        try (var scope = RecipeCandidateCache.enter()) {
            for (int i = 0; i < 20; i++) {
                ItemStack probe = input.copy();
                if (i % 3 == 1) probe.stackSize = 3;
                if (i % 3 == 2) probe.getTagCompound()
                    .setString("fixture", "other");
                boolean matched = RecipeMaps.compressorRecipes.findRecipeQuery()
                    .items(probe)
                    .findAll()
                    .anyMatch(candidate -> candidate == recipe);
                if (matched != (i % 3 == 0)) throw new AssertionError("live quantity/NBT validation");
            }
        }
    }

    private static void checkOrePowerRetry() {
        ItemStack input = new ItemStack(Items.record_wait, 4);
        GTValues.RA.stdBuilder()
            .itemInputs(input.copy())
            .itemOutputs(new ItemStack(Items.quartz))
            .duration(40)
            .eut(32)
            .addTo(GTNRRecipeMaps.ORE_PROCESSING_RECIPES);
        var logic = new OreRecipeThreadProcessingLogic();
        var machine = (LargeOreProcessor) place(
            MinecraftServer.getServer()
                .worldServerForDimension(0),
            7,
            GTNRMachineID.LargeOreProcessor).getMetaTileEntity();
        logic.setMachine(machine)
            .setRecipeMapSupplier(() -> GTNRRecipeMaps.ORE_PROCESSING_RECIPES)
            .setAvailableVoltage(32)
            .setAvailableAmperage(1)
            .setMaxParallel(1)
            .setVoidProtection(false, false);
        logic.setInputItems(input)
            .setInputFluids();
        logic.prepareRecipe(8);
        var result = logic.process();
        if (result.wasSuccessful() || !logic.shouldRetryWithMorePower(result) || input.stackSize != 4)
            throw new AssertionError("ore recipe must retry a power-limited candidate");
        ItemStack shortSlot = input.copy();
        shortSlot.stackSize--;
        logic.setInputItems(shortSlot);
        result = logic.process();
        if (result.wasSuccessful() || !logic.shouldRetryWithMorePower(result))
            throw new AssertionError("a later incomplete slot must not hide the earlier power-limited candidate");
        logic.setInputItems(input);
        logic.prepareRecipe(32);
        if (!logic.process()
            .wasSuccessful() || !logic.commitInputs() || input.stackSize != 0)
            throw new AssertionError("ore retry must start and debit exactly once");
        input.stackSize = 3;
        logic.prepareRecipe(32);
        result = logic.process();
        if (result.wasSuccessful() || logic.shouldRetryWithMorePower(result) || input.stackSize != 3)
            throw new AssertionError("insufficient ore input cannot benefit from more power");
        logic.finishRecipe();
    }

    private static void checkForeignStockingBusPreflight() {
        World world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        var machine = (LargeOreProcessor) place(world, 9, GTNRMachineID.LargeOreProcessor).getMetaTileEntity();
        var tile = new BaseMetaTileEntity();
        tile.setWorldObj(world);
        var bus = new MTEHatchInputBusME("qa.foreign.stocking.bus", true, 8, new String[0], null) {

            @Override
            public AENetworkProxy getProxy() {
                throw new AssertionError("preflight accessed a foreign ME stocking bus");
            }
        };
        bus.setBaseMetaTileEntity(tile);
        machine.mInputBusses.add(bus);
        if (!bus.isValid() || !RecipeThreadInputPreflight.check(machine, new OreRecipeThreadProcessingLogic())
            .wasSuccessful())
            throw new AssertionError("foreign ME stocking bus must retain its own recipe transaction");
    }

    private static void checkThreadOutputs() {
        World world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        var controller = (UniversalJointFactory) place(world, 8, GTNRMachineID.UniversalJointFactory)
            .getMetaTileEntity();
        var tile = new BaseMetaTileEntity();
        tile.setWorldObj(world);
        tile.setInitialValuesAsNBT(
            null,
            (short) ItemList.Hatch_Output_Bus_HV.get(1)
                .getItemDamage());
        var bus = (MTEHatchOutputBus) tile.getMetaTileEntity();
        controller.mOutputBusses.add(bus);
        controller.setVoidingMode(VoidingMode.VOID_NONE);
        var mixed = new RecipeThreadOutputs(
            new ItemStack[] { new ItemStack(Items.diamond) },
            new FluidStack[] { new FluidStack(FluidRegistry.WATER, 1000) });
        if (mixed.flush(controller) || bus.getStackInSlot(0) != null)
            throw new AssertionError("blocked fluid must not commit the staged item");
        controller.setVoidingMode(VoidingMode.VOID_FLUID);
        if (!mixed.flush(controller) || bus.getStackInSlot(0).stackSize != 1)
            throw new AssertionError("unblocked mixed output must commit once");
        controller.setVoidingMode(VoidingMode.VOID_NONE);
        for (int i = 0; i < bus.getSizeInventory(); i++)
            bus.setInventorySlotContents(i, new ItemStack(Items.diamond, i == 0 ? 63 : 64));
        var partial = new RecipeThreadOutputs(new ItemStack[] { new ItemStack(Items.diamond, 2) }, null);
        if (partial.flush(controller) || bus.getStackInSlot(0).stackSize != 64)
            throw new AssertionError("partial output must fill exactly one available space");
        bus.setInventorySlotContents(0, null);
        if (!partial.flush(controller) || bus.getStackInSlot(0).stackSize != 1)
            throw new AssertionError("partial retry must commit only the remaining item");
    }

    private static void checkAssemblies() throws Exception {
        World world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        UniversalJointFactory factory = (UniversalJointFactory) place(world, 0, GTNRMachineID.UniversalJointFactory)
            .getMetaTileEntity();
        factory.setMachineMode(
            factory.getAvailableRecipeMaps()
                .indexOf(RecipeMaps.compressorRecipes));
        ItemStack input = new ItemStack(Items.record_13, 2), output = new ItemStack(Items.gold_nugget);
        GTValues.RA.stdBuilder()
            .itemInputs(input.copy())
            .itemOutputs(output.copy())
            .duration(20)
            .eut(8)
            .addTo(RecipeMaps.compressorRecipes);
        GTNRMachineID[] assemblies = { GTNRMachineID.SuperCraftingInputME, GTNRMachineID.CircuitMEPatternBuffer,
            GTNRMachineID.CrossRecipeMEPatternBuffer };
        CrossRecipeProcessingLogic logic = new CrossRecipeProcessingLogic(factory);
        logic.setMachine(factory)
            .setRecipeMapSupplier(factory::getRecipeMap)
            .setAvailableVoltage(32)
            .setAvailableAmperage(1)
            .setMaxParallel(1)
            .setVoidProtection(false, false);
        for (int i = 0; i < assemblies.length; i++) {
            SuperMTEHatchCraftingInputME hatch = (SuperMTEHatchCraftingInputME) place(world, i + 1, assemblies[i])
                .getMetaTileEntity();
            ItemStack encoded = AEApi.instance()
                .definitions()
                .items()
                .encodedUltimatePattern()
                .maybeStack(1)
                .get();
            NBTTagCompound tag = new NBTTagCompound();
            NBTTagList in = new NBTTagList(), out = new NBTTagList();
            in.appendTag(
                AEItemStack.create(input)
                    .toNBTGeneric());
            out.appendTag(
                AEItemStack.create(output)
                    .toNBTGeneric());
            tag.setTag("in", in);
            tag.setTag("out", out);
            encoded.setTagCompound(tag);
            if (assemblies[i] == GTNRMachineID.CrossRecipeMEPatternBuffer)
                PatternRecipeMap.write(encoded, RecipeMaps.compressorRecipes.unlocalizedName);
            var create = hatch.getClass()
                .getDeclaredMethod("createPatternSlot", ItemStack.class, NBTTagCompound.class);
            create.setAccessible(true);
            var slot = (SuperMTEHatchCraftingInputME.PatternSlot<?>) create.invoke(hatch, encoded, null);
            if (slot.getPatternDetails() == null) throw new AssertionError("failed to decode " + assemblies[i]);
            checkPatternFallback(hatch, encoded, input);
            factory.mDualInputHatches.add(hatch);
            for (int attempt = 0; attempt < 24; attempt++) {
                ItemStack material = input.copy();
                boolean sufficient = attempt % 2 == 0;
                if (!sufficient) material.stackSize--;
                int before = material.stackSize;
                logic.prepareRecipe(32);
                if (!PatternRecipeRouting.cachePattern(logic, slot))
                    throw new AssertionError("rejected " + assemblies[i]);
                logic.setInputItems(material)
                    .setInputFluids();
                boolean matched = PatternRecipeRouting.processPattern(logic)
                    .wasSuccessful();
                if (matched != sufficient || material.stackSize != before)
                    throw new AssertionError("input transaction " + assemblies[i]);
                if (matched && (!logic.getOutputItems()[0].isItemEqual(output) || !logic.commitInputs()
                    || material.stackSize != 0)) throw new AssertionError("output or debit " + assemblies[i]);
                logic.finishRecipe();
            }
            if (assemblies[i] == GTNRMachineID.CrossRecipeMEPatternBuffer) {
                factory.setMachineMode(
                    factory.getAvailableRecipeMaps()
                        .indexOf(RecipeMaps.extractorRecipes));
                logic.prepareRecipe(32);
                if (!logic.tryCachePossibleRecipesFromPattern(slot)) throw new AssertionError("cross-map routing");
                logic.setInputItems(input.copy())
                    .setInputFluids();
                if (!logic.process()
                    .wasSuccessful() || !logic.getOutputItems()[0].isItemEqual(output))
                    throw new AssertionError("explicit map in another controller mode");
                logic.finishRecipe();
            }
        }
        checkOrdinaryBusFallback(world, factory, input);
    }

    private static BaseMetaTileEntity place(World world, int x, GTNRMachineID machine) {
        world.setBlock(x, 80, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 80, 0);
        tile.setInitialValuesAsNBT(null, (short) machine.id);
        return tile;
    }

    private static void checkPatternFallback(SuperMTEHatchCraftingInputME hatch, ItemStack encoded, ItemStack input) {
        hatch.onPatternChange(0, encoded);
        var slot = hatch.inventories()
            .next();
        var delivery = new MEInventoryCrafting(new ContainerNull(), 1, 1);
        ItemStack one = input.copy();
        one.stackSize = 1;
        delivery.setInventorySlotContents(0, AEItemStack.create(one));
        if (!slot.insertItemsAndFluids(delivery)) throw new AssertionError("initial delivery");
        var scheduler = new PatternInputScheduler();
        scheduler.beginPass(List.of(hatch), true);
        if (!scheduler.selectNext()) throw new AssertionError("missing nonempty slot");
        scheduler.finishSelection(false, CheckRecipeResultRegistry.NO_RECIPE);
        if (scheduler.selectNext() || !scheduler.alreadyChecked(hatch)) throw new AssertionError("repeated shortage");
        if (!slot.insertItemsAndFluids(delivery) || scheduler.alreadyChecked(hatch))
            throw new AssertionError("delivery within pass must invalidate the miss");
        scheduler.endPass();
        scheduler.beginPass(List.of(hatch), true);
        if (!scheduler.selectNext()) throw new AssertionError("missing restocked slot");
        scheduler.finishSelection(false, CheckRecipeResultRegistry.insufficientPower(32));
        if (scheduler.alreadyChecked(hatch)) throw new AssertionError("meaningful GT failure must survive fallback");
        scheduler.endPass();
        slot.getItemInputs()[0].stackSize--;
        if (slot.getItemInputs()[0].stackSize != 1) throw new AssertionError("reset shortage");
    }

    private static void checkOrdinaryBusFallback(World world, UniversalJointFactory factory, ItemStack input) {
        factory.setMachineMode(
            factory.getAvailableRecipeMaps()
                .indexOf(RecipeMaps.compressorRecipes));
        factory.fixAllIssues();
        factory.mMachine = true;
        factory.setVoidingMode(VoidingMode.VOID_ALL);
        var energy = new BaseMetaTileEntity();
        energy.setWorldObj(world);
        energy.setInitialValuesAsNBT(
            null,
            (short) ItemList.Hatch_Energy_MV.get(1)
                .getItemDamage());
        factory.mEnergyHatches.add((MTEHatchEnergy) energy.getMetaTileEntity());
        var tile = new BaseMetaTileEntity();
        tile.setWorldObj(world);
        tile.setInitialValuesAsNBT(
            null,
            (short) ItemList.Hatch_Input_Bus_HV.get(1)
                .getItemDamage());
        var bus = (MTEHatchInputBus) tile.getMetaTileEntity();
        factory.mInputBusses.add(bus);
        bus.setInventorySlotContents(0, input.copy());
        if (!factory.checkProcessing()
            .wasSuccessful() || (bus.getStackInSlot(0) != null && bus.getStackInSlot(0).stackSize > 0))
            throw new AssertionError("ordinary input bus must start after pattern misses");
        for (var hatch : factory.mDualInputHatches) {
            var slot = hatch.inventories()
                .next();
            if (slot.getItemInputs()[0].stackSize != 1)
                throw new AssertionError("separate short slots must not be debited");
        }
    }

    private static void benchmark(Path output) throws Exception {
        GTRecipeLookup lookup = new GTRecipeLookup();
        List<GTRecipeLookupIngredient> inputs = new ArrayList<>();
        List<GTRecipe> expected = new ArrayList<>();
        Iterator<GTRecipe> samples = RecipeMaps.assemblerRecipes.getAllRecipes()
            .iterator();
        for (int i = 0; i < 24; i++) inputs.add(new GTOreDictLookupIngredient(i));
        for (int i = 0; i < 16; i++) {
            GTRecipe recipe = samples.next();
            lookup.add(recipe, List.of(List.of(inputs.get(i)), List.of(inputs.get(i + 1))));
            expected.add(recipe);
        }
        Class<?> nativeIterator = lookup.iterator(inputs)
            .getClass();
        try (var scope = RecipeCandidateCache.enter()) {
            checkWarm(lookup, inputs, expected, nativeIterator);
        }
        long[] nativeTimes = new long[7], cachedTimes = new long[7];
        for (int i = -1; i < nativeTimes.length; i++) {
            long nativeTime = measure(lookup, inputs, expected.size());
            long cachedTime;
            try (var scope = RecipeCandidateCache.enter()) {
                cachedTime = measure(lookup, inputs, expected.size());
            }
            if (i >= 0) {
                nativeTimes[i] = nativeTime;
                cachedTimes[i] = cachedTime;
            }
        }
        Arrays.sort(nativeTimes);
        Arrays.sort(cachedTimes);
        Files.writeString(
            output,
            "Median ns per complete candidate traversal, 24 input keys / 16 recipes: native=" + nativeTimes[3]
                + " cached="
                + cachedTimes[3]
                + "\n");
    }

    private static long measure(GTRecipeLookup lookup, List<GTRecipeLookupIngredient> inputs, int expected) {
        int count = 0;
        long start = System.nanoTime();
        for (int i = 0; i < 2000; i++) {
            Iterator<GTRecipe> iterator = lookup.iterator(inputs);
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
        }
        long nanos = System.nanoTime() - start;
        if (count != 2000 * expected) throw new AssertionError("benchmark candidate count");
        return nanos / 2000;
    }

    private static void checkWarm(GTRecipeLookup lookup, List<GTRecipeLookupIngredient> inputs, List<GTRecipe> expected,
        Class<?> nativeIterator) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        boolean cold = true;
        do {
            Iterator<GTRecipe> iterator = lookup.iterator(inputs);
            boolean cached = iterator.getClass() != nativeIterator;
            if (cold && cached) throw new AssertionError("stale candidate list");
            List<GTRecipe> actual = new ArrayList<>();
            iterator.forEachRemaining(actual::add);
            if (!actual.equals(expected)) throw new AssertionError("candidate order/content");
            if (cached) return;
            cold = false;
            Thread.sleep(1);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("no asynchronous cache hit");
    }
}
