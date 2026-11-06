package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer.CircuitSlot;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternMirror;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.common.me.PatternRecipeRouting;
import com.xyp.gtneverreset.mixins.late.gregtech.MultiblockProcessingAccessor;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.api.AEApi;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEStack;
import appeng.container.ContainerNull;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.VoidingMode;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GTUtility;

/** Native processing-path load test; fixture recipes and structure parameters exist only in this isolated QA mod. */
@Mod(modid = "patternstressqa", name = "Pattern Recipe Stress QA", version = "1", dependencies = "after:gtneverreset")
public final class PatternRecipeStressChecks {

    private static final int SLOT_COUNT = 540;
    private static final int VALID_SLOTS = SLOT_COUNT - 3;
    private static final int ROUNDS = 6;
    private static final int CHECKS_PER_TICK = 64;
    private static final String[] TYPES = { "MTEIndustrialPackager", "MTEAmazonPackagerLegacy",
        "MTEIndustrialPlatePressLegacy", "MTEOreWashingPlant", "MTEIndustrialDehydrator", "MTEMegaDistillationTower",
        "MTEAdvDistillationTower", "MTEPreciseAssembler" };
    private static final Map<String, Fixture> RECIPES = new LinkedHashMap<>();
    private static final Map<RecipeMap<?>, Integer> MAP_IDS = new LinkedHashMap<>();
    private static Item inputItem, outputItem;
    private final List<Group> groups = new ArrayList<>();
    private final List<Long> tickTimes = new ArrayList<>();
    private volatile Throwable failure;
    private volatile List<String> summary;
    private boolean launched, finished;
    private int ticks, frames, cursor;
    private long started, peakHeap;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        inputItem = new Item().setHasSubtypes(true)
            .setUnlocalizedName("pattern_stress_input");
        outputItem = new Item().setHasSubtypes(true)
            .setUnlocalizedName("pattern_stress_output");
        GameRegistry.registerItem(inputItem, "input");
        GameRegistry.registerItem(outputItem, "output");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || failure != null || summary != null || !GregTechAPI.sFullLoadFinished)
            return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        var player = (EntityPlayerMP) players.get(0);
        try {
            if (started == 0) started = System.nanoTime();
            if (groups.size() < TYPES.length) {
                groups.add(new Group(player, TYPES[groups.size()], groups.size()));
                return;
            }
            long tickStart = System.nanoTime();
            long beforeRefills = groups.stream()
                .mapToLong(group -> group.refillNanos)
                .sum();
            long beforeRestores = groups.stream()
                .mapToLong(group -> group.restoreNanos)
                .sum();
            long beforeComparisons = groups.stream()
                .mapToLong(group -> group.comparisonNanos)
                .sum();
            long beforeGc = gcMillis();
            for (int count = 0; count < CHECKS_PER_TICK; count++) {
                Group group = groups.get(cursor++ % groups.size());
                if (group.round < ROUNDS) group.step();
            }
            long elapsed = System.nanoTime() - tickStart;
            tickTimes.add(elapsed);
            if (elapsed > 50_000_000L) System.out.println(
                String.format(
                    java.util.Locale.ROOT,
                    "[pattern-stress-qa] slow batch %.2f ms: refill %.2f, restore %.2f, comparison %.2f, GC %d ms",
                    elapsed / 1e6,
                    (groups.stream()
                        .mapToLong(group -> group.refillNanos)
                        .sum() - beforeRefills) / 1e6,
                    (groups.stream()
                        .mapToLong(group -> group.restoreNanos)
                        .sum() - beforeRestores) / 1e6,
                    (groups.stream()
                        .mapToLong(group -> group.comparisonNanos)
                        .sum() - beforeComparisons) / 1e6,
                    gcMillis() - beforeGc));
            Runtime runtime = Runtime.getRuntime();
            peakHeap = Math.max(peakHeap, runtime.totalMemory() - runtime.freeMemory());
            if (groups.stream()
                .allMatch(group -> group.round == ROUNDS)) complete();
        } catch (Throwable error) {
            failure = error;
            error.printStackTrace();
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "stress-" + System.currentTimeMillis(),
                    "Pattern Stress QA",
                    new WorldSettings(2487, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("Native stress checks failed", failure);
            if (mc.thePlayer == null) return;
            if (++ticks > 12000) throw new AssertionError("Stress QA timeout");
            if (summary == null) return;
            if (!(mc.currentScreen instanceof SummaryScreen)) mc.displayGuiScreen(new SummaryScreen(summary));
            if (++frames < 15) return;
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "pattern-stress.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            finish(null);
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void complete() throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("Pattern routing stress: PASS");
        lines.add("8 native machine types / 24 consumers / 8 full 540-slot buffers");
        long checks = 0, crafts = 0, comparisons = 0, reloads = 0;
        StringBuilder report = new StringBuilder(
            "type,maps,checks,crafts,native_comparisons,reloads,p50_ms,p95_ms,max_ms\n");
        StringBuilder operations = new StringBuilder("type,refill_max_ms,reload_max_ms,comparison_max_ms\n");
        for (Group group : groups) {
            group.verifyLongCounts();
            checks += group.samples.size();
            crafts += group.crafts;
            comparisons += group.comparisons;
            reloads += group.reloads;
            String timing = String.format(
                java.util.Locale.ROOT,
                "%.3f,%.3f,%.3f",
                percentile(group.samples, .5),
                percentile(group.samples, .95),
                percentile(group.samples, 1));
            report.append(group.type)
                .append(',')
                .append(group.maps.size())
                .append(',')
                .append(group.samples.size())
                .append(',')
                .append(group.crafts)
                .append(',')
                .append(group.comparisons)
                .append(',')
                .append(group.reloads)
                .append(',')
                .append(timing)
                .append('\n');
            operations.append(
                String.format(
                    java.util.Locale.ROOT,
                    "%s,%.3f,%.3f,%.3f%n",
                    group.type,
                    percentile(group.refillTimes, 1),
                    percentile(group.restoreTimes, 1),
                    percentile(group.comparisonTimes, 1)));
            lines.add(
                group.type + ": "
                    + group.crafts
                    + " crafts; p95 "
                    + String.format(java.util.Locale.ROOT, "%.3f", percentile(group.samples, .95))
                    + " ms");
        }
        require(checks >= 25000 && crafts >= 100000 && reloads == TYPES.length * ROUNDS, "stress volume reached");
        lines.add(checks + " checks / " + crafts + " recipe batches / " + comparisons + " native comparisons");
        lines.add(reloads + " NBT restores; exact item/fluid balances; no mode or pending-state leaks");
        lines.add("16 large-stock checks: >2^31 batches survive processing and NBT reload");
        lines.add(
            String.format(
                java.util.Locale.ROOT,
                "Harness tick p95 %.2f ms; max %.2f ms; heap peak %.0f MiB",
                percentile(tickTimes, .95),
                percentile(tickTimes, 1),
                peakHeap / 1048576D));
        lines.add("Processing-path test, not a fully built factory TPS benchmark");
        Files.writeString(Path.of("stress-metrics.csv"), report);
        Files.writeString(Path.of("stress-operations.csv"), operations);
        Files.writeString(
            Path.of("stress-summary.txt"),
            String.join("\n", lines) + "\nElapsed seconds: " + (System.nanoTime() - started) / 1e9);
        lines.forEach(line -> System.out.println("[pattern-stress-qa] " + line));
        summary = List.copyOf(lines);
    }

    private static double percentile(List<Long> values, double percentile) {
        long[] sorted = values.stream()
            .mapToLong(Long::longValue)
            .sorted()
            .toArray();
        return sorted.length == 0 ? 0
            : sorted[Math.min(sorted.length - 1, (int) Math.ceil(sorted.length * percentile) - 1)] / 1e6;
    }

    private static long gcMillis() {
        long millis = 0;
        for (var collector : ManagementFactory.getGarbageCollectorMXBeans())
            millis += Math.max(0, collector.getCollectionTime());
        return millis;
    }

    private static final class Group {

        private final EntityPlayerMP player;
        private final String type;
        private final int x, z;
        private final List<RecipeMap<?>> maps;
        private final MTEMultiBlockBase[] consumers = new MTEMultiBlockBase[3];
        private final MTEMultiBlockBase reference;
        private final CrossRecipeMEPatternMirror[] mirrors = new CrossRecipeMEPatternMirror[2];
        private final Fixture[] fixtures = new Fixture[SLOT_COUNT];
        private final int[] remaining = new int[SLOT_COUNT];
        private final List<Long> samples = new ArrayList<>();
        private final List<Long> refillTimes = new ArrayList<>(), restoreTimes = new ArrayList<>(),
            comparisonTimes = new ArrayList<>();
        private CrossRecipeMEPatternBuffer hatch;
        private List<CircuitSlot> slots;
        private int round, nextSlot, turn;
        private long crafts, comparisons, reloads;
        private long refillNanos, restoreNanos, comparisonNanos;
        private boolean restored;

        private Group(EntityPlayerMP player, String type, int index) throws Exception {
            this.player = player;
            this.type = type;
            x = (int) player.posX + 8 + index * 4;
            z = (int) player.posZ + 8;
            for (int i = 0; i < consumers.length; i++) consumers[i] = controller(player, type);
            reference = controller(player, type);
            maps = new ArrayList<>(consumers[0].getAvailableRecipeMaps());
            require(maps.size() >= 2, type + " has multiple native recipe pools");
            for (int i = 0; i < consumers.length; i++)
                consumers[i].setMachineMode(modeFor(consumers[i], maps.get(i % maps.size())));
            hatch = (CrossRecipeMEPatternBuffer) place(player, GTNRMachineID.CrossRecipeMEPatternBuffer.id, x, z)
                .getMetaTileEntity();
            for (int i = 0; i < SLOT_COUNT; i++) {
                fixtures[i] = fixture(maps.get(i % maps.size()), i / maps.size());
                ItemStack pattern = fixtures[i].pattern.copy();
                if (i == VALID_SLOTS) PatternRecipeMap.write(pattern, "gtnr.qa.removed_pool");
                if (i == VALID_SLOTS + 1) pattern.getTagCompound()
                    .setInteger(PatternRecipeMap.KEY, 5);
                if (i == VALID_SLOTS + 2) PatternRecipeMap.write(pattern, RecipeMaps.polarizerRecipes.unlocalizedName);
                hatch.onPatternChange(i, pattern);
            }
            for (int i = 0; i < mirrors.length; i++) {
                mirrors[i] = (CrossRecipeMEPatternMirror) place(
                    player,
                    GTNRMachineID.CrossRecipeMEPatternMirror.id,
                    x + i + 1,
                    z).getMetaTileEntity();
                require(mirrors[i].trySetMasterFromCoord(x, 100, z) == hatch, type + " dedicated mirror binds");
            }
            reconnect();
            for (int i = VALID_SLOTS; i < SLOT_COUNT; i++) {
                remaining[i] = 3;
                insert(slots.get(i), 3);
            }
            refill();
            System.out.println(
                "[pattern-stress-qa] prepared " + type
                    + " maps="
                    + maps.stream()
                        .map(map -> map.unlocalizedName)
                        .toList());
        }

        private void reconnect() {
            consumers[0].mDualInputHatches.clear();
            consumers[0].mDualInputHatches.add(hatch);
            hatch.addWatcher(consumers[0]);
            for (int i = 0; i < mirrors.length; i++) {
                mirrors[i].onPostTick(mirrors[i].getBaseMetaTileEntity(), 100);
                require(mirrors[i].getMaster() == hatch, type + " mirror resolves restored master");
                consumers[i + 1].mDualInputHatches.clear();
                consumers[i + 1].mDualInputHatches.add(mirrors[i]);
                mirrors[i].addWatcher(consumers[i + 1]);
            }
            slots = new ArrayList<>();
            hatch.inventories()
                .forEachRemaining(slot -> slots.add((CircuitSlot) slot));
            require(slots.size() == SLOT_COUNT, type + " retains all 540 patterns");
        }

        private void refill() {
            long start = System.nanoTime();
            for (int i = 0; i < VALID_SLOTS; i++) {
                require(
                    slots.get(i)
                        .isEmpty(),
                    type + " refill starts empty");
                remaining[i] = 1 + (round * 31 + i * 17) % 7;
                // Multiple small deliveries exercise accumulation rather than only a multiplied packet.
                for (int n = 0; n < remaining[i]; n++) insert(slots.get(i), 1);
                verifyStock(slots.get(i), fixtures[i], remaining[i]);
            }
            nextSlot = 0;
            restored = false;
            long elapsed = System.nanoTime() - start;
            refillTimes.add(elapsed);
            refillNanos += elapsed;
        }

        private void step() throws Exception {
            if (nextSlot == VALID_SLOTS) {
                for (MTEMultiBlockBase consumer : consumers) {
                    var result = process(consumer);
                    require(!result.wasSuccessful(), type + " invalid maps never fall back");
                }
                for (int i = VALID_SLOTS; i < SLOT_COUNT; i++) verifyStock(slots.get(i), fixtures[i], remaining[i]);
                require(restored, "mid-backlog reload exercised");
                System.out.println(
                    "[pattern-stress-qa] " + type
                        + " round="
                        + (++round)
                        + " crafts="
                        + crafts
                        + " checks="
                        + samples.size());
                if (round < ROUNDS) refill();
                return;
            }
            var consumer = consumers[turn++ % consumers.length];
            Fixture recipe = fixtures[nextSlot];
            int before = remaining[nextSlot];
            if (samples.size() % 128 == 0) compareNative(consumer, recipe, before);
            int originalMode = consumer.getMachineMode();
            ProcessingLogic logic = logic(consumer);
            ((MultiblockProcessingAccessor) consumer).gtnr$setupProcessingLogic(logic);
            logic.setVoidProtection(false, false);
            Object[] configuration = configuration(logic);
            long start = System.nanoTime();
            CheckRecipeResult result = check(consumer);
            samples.add(System.nanoTime() - start);
            require(
                result.wasSuccessful(),
                type + " / " + recipe.map.unlocalizedName + " failed: " + result.getDisplayString());
            int parallel = logic.getCurrentParallels();
            require(parallel > 0 && parallel <= before, type + " bounded parallel consumption");
            verifyOutput(logic, recipe, parallel);
            remaining[nextSlot] -= parallel;
            crafts += parallel;
            verifyStock(slots.get(nextSlot), recipe, remaining[nextSlot]);
            require(consumer.getMachineMode() == originalMode, type + " original mode restored");
            if (consumer.getRecipeMap() != recipe.map)
                require(Arrays.equals(configuration, configuration(logic)), type + " original settings restored");
            require(
                read(logic, "gtnr$pendingMap") == null && read(logic, "gtnr$restoreConfiguration") == null,
                type + " pending routing cleared");
            if (remaining[nextSlot] == 0) nextSlot++;
            if (!restored && nextSlot >= VALID_SLOTS / 2) restore();
        }

        private void compareNative(MTEMultiBlockBase consumer, Fixture recipe, int quantity) throws Exception {
            long start = System.nanoTime();
            CircuitMEPatternBuffer baseline = (CircuitMEPatternBuffer) tile(
                player,
                GTNRMachineID.CircuitMEPatternBuffer.id).getMetaTileEntity();
            baseline.onPatternChange(0, recipe.pattern);
            CircuitSlot slot = (CircuitSlot) baseline.inventories()
                .next();
            insert(slot, quantity);
            reference.setMachineMode(modeFor(reference, recipe.map));
            reference.mDualInputHatches.clear();
            reference.mDualInputHatches.add(baseline);
            var result = process(reference);
            require(result.wasSuccessful(), type + " native reference failed: " + result.getDisplayString());
            // Compare against an independent routed copy, leaving the shared production-test backlog untouched.
            var routed = (CrossRecipeMEPatternBuffer) tile(player, GTNRMachineID.CrossRecipeMEPatternBuffer.id)
                .getMetaTileEntity();
            routed.onPatternChange(0, recipe.pattern);
            var routedSlot = (CircuitSlot) routed.inventories()
                .next();
            insert(routedSlot, quantity);
            var saved = new ArrayList<>(consumer.mDualInputHatches);
            try {
                consumer.mDualInputHatches.clear();
                consumer.mDualInputHatches.add(routed);
                require(process(consumer).wasSuccessful(), type + " comparison route succeeds");
                var expected = logic(reference);
                var actual = logic(consumer);
                require(
                    actual.getDuration() == expected.getDuration()
                        && actual.getCalculatedEut() == expected.getCalculatedEut()
                        && actual.getCurrentParallels() == expected.getCurrentParallels(),
                    type + " matches native duration/EU/parallel");
                verifyOutput(actual, recipe, actual.getCurrentParallels());
                verifyStock(routedSlot, recipe, quantity - actual.getCurrentParallels());
                comparisons++;
            } finally {
                consumer.mDualInputHatches.clear();
                consumer.mDualInputHatches.addAll(saved);
                long elapsed = System.nanoTime() - start;
                comparisonTimes.add(elapsed);
                comparisonNanos += elapsed;
            }
        }

        private void restore() {
            long start = System.nanoTime();
            for (int i = 0; i < SLOT_COUNT; i++) verifyStock(slots.get(i), fixtures[i], remaining[i]);
            NBTTagCompound saved = new NBTTagCompound();
            hatch.saveNBTData(saved);
            var oldTile = (BaseMetaTileEntity) hatch.getBaseMetaTileEntity();
            oldTile.invalidate();
            BaseMetaTileEntity replacement = tile(player, GTNRMachineID.CrossRecipeMEPatternBuffer.id);
            player.worldObj.setTileEntity(x, 100, z, replacement);
            hatch = (CrossRecipeMEPatternBuffer) replacement.getMetaTileEntity();
            hatch.loadNBTData(saved);
            reconnect();
            for (int i = 0; i < SLOT_COUNT; i++) verifyStock(slots.get(i), fixtures[i], remaining[i]);
            restored = true;
            reloads++;
            long elapsed = System.nanoTime() - start;
            restoreTimes.add(elapsed);
            restoreNanos += elapsed;
        }

        private void verifyLongCounts() throws Exception {
            for (RecipeMap<?> map : maps) {
                Fixture recipe = fixture(map, 0);
                long quantity = (long) Integer.MAX_VALUE + 1024;
                var large = (CrossRecipeMEPatternBuffer) tile(player, GTNRMachineID.CrossRecipeMEPatternBuffer.id)
                    .getMetaTileEntity();
                large.onPatternChange(0, recipe.pattern);
                var slot = (CircuitSlot) large.inventories()
                    .next();
                insert(slot, quantity);
                verifyStock(slot, recipe, quantity);
                NBTTagCompound saved = new NBTTagCompound();
                large.saveNBTData(saved);
                var restored = (CrossRecipeMEPatternBuffer) tile(player, GTNRMachineID.CrossRecipeMEPatternBuffer.id)
                    .getMetaTileEntity();
                restored.loadNBTData(saved);
                slot = (CircuitSlot) restored.inventories()
                    .next();
                verifyStock(slot, recipe, quantity);
                var consumer = consumers[0];
                var inputs = new ArrayList<>(consumer.mDualInputHatches);
                int mode = consumer.getMachineMode();
                try {
                    consumer.mDualInputHatches.clear();
                    consumer.mDualInputHatches.add(restored);
                    require(process(consumer).wasSuccessful(), type + " large-stock processing succeeds");
                    int parallel = logic(consumer).getCurrentParallels();
                    verifyOutput(logic(consumer), recipe, parallel);
                    verifyStock(slot, recipe, quantity - parallel);
                    require(consumer.getMachineMode() == mode, type + " large-stock mode restored");
                } finally {
                    consumer.mDualInputHatches.clear();
                    consumer.mDualInputHatches.addAll(inputs);
                }
            }
            System.out.println("[pattern-stress-qa] large-stock PASS " + type);
        }
    }

    private static Fixture fixture(RecipeMap<?> map, int variant) {
        String key = map.unlocalizedName + "/" + variant;
        if (RECIPES.containsKey(key)) return RECIPES.get(key);
        int mapId = MAP_IDS.computeIfAbsent(map, ignored -> MAP_IDS.size());
        var ui = map.getFrontend()
            .getUIProperties();
        var properties = map.getBackend()
            .getProperties();
        List<ItemStack> items = new ArrayList<>();
        List<ItemStack> recipeItems = new ArrayList<>();
        boolean circuit = ui.maxItemInputs >= 2;
        boolean mold = ui.maxItemInputs >= 3;
        int tools = (circuit ? 1 : 0) + (mold ? 1 : 0);
        int count = ui.maxItemInputs == 0 ? 0 : Math.max(1, properties.minItemInputs - tools);
        for (int i = 0; i < count; i++) items.add(new ItemStack(inputItem, 2 + i, variant * 8 + i));
        recipeItems.addAll(items);
        if (circuit) recipeItems.add(GTUtility.getIntegratedCircuit(4));
        if (mold) recipeItems.add(ItemList.Shape_Mold_Plate.get(0));
        List<FluidStack> fluids = new ArrayList<>();
        for (int i = 0; i < (ui.maxFluidInputs == 0 ? 0 : Math.max(1, properties.minFluidInputs)); i++) {
            String name = "gtnr.qa.stress." + variant + "." + i;
            Fluid fluid = FluidRegistry.getFluid(name);
            if (fluid == null) {
                fluid = new Fluid(name);
                FluidRegistry.registerFluid(fluid);
            }
            fluids.add(new FluidStack(fluid, 31 + i));
        }
        ItemStack[] outputs = ui.maxItemOutputs == 0 ? new ItemStack[0]
            : new ItemStack[] { new ItemStack(outputItem, 1, mapId * 512 + variant) };
        FluidStack[] fluidOutputs = ui.maxFluidOutputs == 0 ? new FluidStack[0]
            : new FluidStack[] { new FluidStack(FluidRegistry.WATER, 11 + mapId) };
        var recipes = GTValues.RA.stdBuilder()
            .itemInputs(recipeItems.toArray(new ItemStack[0]))
            .fluidInputs(fluids.toArray(new FluidStack[0]))
            .itemOutputs(outputs)
            .fluidOutputs(fluidOutputs)
            .duration(203)
            .eut(8)
            .specialValue(0)
            .addTo(map);
        require(!recipes.isEmpty(), "fixture recipe registered: " + key);
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList(), out = new NBTTagList();
        items.forEach(
            item -> in.appendTag(
                AEItemStack.create(item)
                    .toNBTGeneric()));
        fluids.forEach(
            fluid -> in.appendTag(
                AEFluidStack.create(fluid)
                    .toNBTGeneric()));
        for (ItemStack item : outputs) out.appendTag(
            AEItemStack.create(item)
                .toNBTGeneric());
        for (FluidStack fluid : fluidOutputs) out.appendTag(
            AEFluidStack.create(fluid)
                .toNBTGeneric());
        tag.setTag("in", in);
        tag.setTag("out", out);
        if (circuit) tag.setInteger(CircuitPatternCodec.CIRCUIT, 4);
        if (mold) {
            NBTTagList molds = new NBTTagList();
            molds.appendTag(
                ItemList.Shape_Mold_Plate.get(0)
                    .writeToNBT(new NBTTagCompound()));
            tag.setTag(CircuitPatternCodec.MOLDS, molds);
        }
        pattern.setTagCompound(tag);
        PatternRecipeMap.write(pattern, map.unlocalizedName);
        Fixture fixture = new Fixture(
            map,
            pattern,
            items.toArray(new ItemStack[0]),
            fluids.toArray(new FluidStack[0]),
            outputs,
            fluidOutputs);
        RECIPES.put(key, fixture);
        return fixture;
    }

    private record Fixture(RecipeMap<?> map, ItemStack pattern, ItemStack[] items, FluidStack[] fluids,
        ItemStack[] outputs, FluidStack[] fluidOutputs) {}

    private static void insert(CircuitSlot slot, long multiplier) {
        ICraftingPatternDetails details = slot.getPatternDetails();
        IAEStack<?>[] inputs = details.getAEInputs();
        var delivery = new MEInventoryCrafting(new ContainerNull(), inputs.length, 1);
        for (int i = 0; i < inputs.length; i++) delivery.setInventorySlotContents(
            i,
            inputs[i].copy()
                .setStackSize(inputs[i].getStackSize() * multiplier));
        require(slot.insertItemsAndFluids(delivery), "AE delivery accepted");
    }

    private static void verifyStock(CircuitSlot slot, Fixture fixture, long quantity) {
        var items = slot.getStoredItems();
        var fluids = slot.getStoredFluids();
        require(items.size() == (quantity == 0 ? 0 : fixture.items.length), "item type count conserved");
        require(fluids.size() == (quantity == 0 ? 0 : fixture.fluids.length), "fluid type count conserved");
        if (quantity == 0) {
            require(slot.isEmpty(), "drained slot empty");
            return;
        }
        for (ItemStack item : fixture.items) require(
            items.stream()
                .anyMatch(
                    actual -> GTUtility.areStacksEqual(actual.getItemStack(), item)
                        && actual.getStackSize() == (long) item.stackSize * quantity),
            "exact item balance");
        for (FluidStack fluid : fixture.fluids) require(
            fluids.stream()
                .anyMatch(
                    actual -> actual.getFluid() == fluid.getFluid()
                        && actual.getStackSize() == (long) fluid.amount * quantity),
            "exact fluid balance");
    }

    private static void verifyOutput(ProcessingLogic logic, Fixture fixture, int parallel) {
        ItemStack[] items = logic.getOutputItems();
        FluidStack[] fluids = logic.getOutputFluids();
        require((items == null ? 0 : items.length) == fixture.outputs.length, "output item types match selected pool");
        require(
            (fluids == null ? 0 : fluids.length) == fixture.fluidOutputs.length,
            "output fluid types match selected pool");
        for (int i = 0; i < fixture.outputs.length; i++) require(
            GTUtility.areStacksEqual(items[i], fixture.outputs[i])
                && items[i].stackSize == fixture.outputs[i].stackSize * parallel,
            "correct pool item output and amount");
        for (int i = 0; i < fixture.fluidOutputs.length; i++) require(
            fluids[i].isFluidEqual(fixture.fluidOutputs[i])
                && fluids[i].amount == fixture.fluidOutputs[i].amount * parallel,
            "correct pool fluid output and amount");
    }

    private static MTEMultiBlockBase controller(EntityPlayerMP player, String type) throws Exception {
        for (int id = 0; id < GregTechAPI.METATILEENTITIES.length; id++) {
            var prototype = GregTechAPI.METATILEENTITIES[id];
            if (prototype == null || !prototype.getClass()
                .getSimpleName()
                .equals(type)) continue;
            var controller = (MTEMultiBlockBase) tile(player, id).getMetaTileEntity();
            var energy = tile(
                player,
                ItemList.Hatch_Energy_LV.get(1)
                    .getItemDamage());
            controller.mEnergyHatches.add((MTEHatchEnergy) energy.getMetaTileEntity());
            controller.setVoidingMode(VoidingMode.VOID_ALL);
            if (type.equals("MTEIndustrialPackager")) field(controller, "itemPipeTier").setInt(controller, 0);
            if (type.equals("MTEPreciseAssembler")) {
                field(controller, "machineTier").setInt(controller, 1);
                field(controller, "energyHatchTier").setInt(controller, 1);
                field(controller, "casingTier").setInt(controller, 0);
            }
            if (type.equals("MTEIndustrialDehydrator"))
                field(controller, "mHeatingCapacity").set(controller, HeatingCoilLevel.LV);
            if (type.equals("MTEAdvDistillationTower")) field(controller, "mHeight").setInt(controller, 4);
            if (type.equals("MTEMegaDistillationTower")) field(controller, "height").setInt(controller, 4);
            return controller;
        }
        throw new AssertionError("Missing native controller " + type);
    }

    private static int modeFor(MTEMultiBlockBase controller, RecipeMap<?> map) {
        try (var selected = PatternRecipeRouting.select(controller, map)) {
            require(selected != null, "native map reachable: " + map.unlocalizedName);
            return controller.getMachineMode();
        }
    }

    private static BaseMetaTileEntity tile(EntityPlayerMP player, int id) {
        var tile = new BaseMetaTileEntity();
        tile.setWorldObj(player.worldObj);
        tile.setInitialValuesAsNBT(null, (short) id);
        return tile;
    }

    private static BaseMetaTileEntity place(EntityPlayerMP player, int id, int x, int z) {
        player.worldObj.setBlock(x, 100, z, GregTechAPI.sBlockMachines, 0, 3);
        var tile = (BaseMetaTileEntity) player.worldObj.getTileEntity(x, 100, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        return tile;
    }

    private static Field field(Object instance, String name) throws Exception {
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }

    private static Object read(Object instance, String name) throws Exception {
        return field(instance, name).get(instance);
    }

    private static ProcessingLogic logic(MTEMultiBlockBase controller) throws Exception {
        return (ProcessingLogic) read(controller, "processingLogic");
    }

    private static Object[] configuration(ProcessingLogic logic) throws Exception {
        return new Object[] { read(logic, "speedBoost"), read(logic, "euModifier"), read(logic, "availableVoltage"),
            read(logic, "availableAmperage"), read(logic, "maxParallelSupplier"), read(logic, "batchSize") };
    }

    private static CheckRecipeResult process(MTEMultiBlockBase controller) throws Exception {
        ProcessingLogic logic = logic(controller);
        ((MultiblockProcessingAccessor) controller).gtnr$setupProcessingLogic(logic);
        logic.setVoidProtection(false, false);
        return check(controller);
    }

    private static CheckRecipeResult check(MTEMultiBlockBase controller) throws Exception {
        Method method = MTEMultiBlockBase.class.getDeclaredMethod("doCheckRecipe");
        method.setAccessible(true);
        return (CheckRecipeResult) method.invoke(controller);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void finish(Throwable error) {
        if (finished) return;
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : "FAIL " + error);
        } catch (Exception io) {
            io.printStackTrace();
        }
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(error == null ? 0 : 1, true);
        }, "pattern-stress-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }

    private static final class SummaryScreen extends GuiScreen {

        private final List<String> lines;

        private SummaryScreen(List<String> lines) {
            this.lines = lines;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            for (int i = 0; i < lines.size(); i++)
                drawString(fontRendererObj, lines.get(i), 12, 16 + i * 18, i == 0 ? 0x80FF80 : 0xFFFFFF);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }
}
