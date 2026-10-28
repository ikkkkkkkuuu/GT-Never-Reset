package com.xyp.gtneverreset.common.machine.multiMachineBase;

import static gregtech.api.util.GTUtility.validMTEList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.common.gui.modularui.GTNRMultiBlockBaseGui;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.mixins.late.gregtech.OverclockCalculatorAccessorMixin;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.machine.PatternInputScheduler;
import com.xyp.gtneverreset.utils.machine.RecipeThreadInputPreflight;
import com.xyp.gtneverreset.utils.machine.RecipeThreadOutputs;
import com.xyp.gtneverreset.utils.machine.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.utils.machine.RecipeThreadTask;
import com.xyp.gtneverreset.utils.structure.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.utils.structure.StructureTooltipHooks;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.VoidingMode;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.recipe.check.SingleRecipeCheck;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.structure.error.TranslatableText;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.shutdown.ShutDownReason;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public abstract class GTNRMultiBlockBase<T extends GTNRMultiBlockBase<T>> extends MTEExtendedPowerMultiBlockBase<T>
    implements MultiblockTooltipSource {

    private static final int RECIPE_CHECK_INTERVAL = 5;
    private static final int MAX_STARTS_PER_TICK = 8;
    private static final int HEAT_PROBE_DURATION = 400;
    private static final int HEAT_PROBE_EUT = 8;
    private static final int FIRST_HEAT_PROBE = 127;
    private static final int SECOND_HEAT_PROBE = 997;

    private static String threadCountText(int running, int total) {
        // #tr gtnr.multiblock.threads
        // # Running threads: {\\AQUA}%s{\\RESET} / {\\AQUA}%s{\\RESET}
        // # zh_CN 运行线程：{\\AQUA}%s{\\RESET} / {\\AQUA}%s{\\RESET}
        return StatCollector.translateToLocalFormatted("gtnr.multiblock.threads", running, total);
    }

    private final Map<Integer, RecipeThreadTask> threadTasks = new TreeMap<>();

    private final List<ParameterModuleHatch> parameterModules = new ArrayList<>();
    private final PatternInputScheduler patternScheduler = new PatternInputScheduler();
    private int preparingThread = -1;
    private boolean useRemainingPower;
    private boolean paused = true;
    private RecipeThreadTask pendingTask;
    private TooltipProbeState tooltipProbeState;

    protected GTNRMultiBlockBase(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    protected GTNRMultiBlockBase(String name) {
        super(name);
    }

    @Override
    public final void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        checkMachineStructure(tile, stack, errors);
        collectTooltipStructureRequirements(errors);
    }

    protected abstract void checkMachineStructure(IGregTechTileEntity tile, ItemStack stack,
        List<StructureError> errors);

    /** Pure hatch/casing counts, shared by formation checks and automatic structure tooltips. */
    protected void checkMachineHatches(List<StructureError> errors) {}

    protected final void checkHatchMin(List<StructureError> errors,
        IHatchElement<? super GTNRMultiBlockBase<T>> element, int min) {
        if (StructureTooltipHooks.onCheckHatches(this, min, StructureTooltipHooks.UNLIMITED, element)) return;
        int count = Math.toIntExact(element.count(this));
        if (count >= min) return;
        List<String> keys = element.getDescriptionLangKeys();
        TranslatableText name = TranslatableText.lang(keys.get(0));
        for (int i = 1; i < keys.size(); i++) {
            // #tr gtnr.structure.hatch.alternative
            // # %s or %s
            // # zh_CN %s或%s
            name = TranslatableText.lang("gtnr.structure.hatch.alternative", name, TranslatableText.lang(keys.get(i)));
        }
        errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, name, count, min));
    }

    @Override
    public final void collectTooltipStructureRequirements(List<StructureError> errors) {
        checkMachineHatches(errors);
        Map<MachineModuleType, Integer> counts = new EnumMap<>(MachineModuleType.class);
        for (ParameterModuleHatch hatch : parameterModules) counts.merge(hatch.getModuleType(), 1, Integer::sum);
        for (var entry : counts.entrySet()) {
            int limit = getParameterModuleLimit(entry.getKey());
            if (entry.getValue() > limit) {
                errors.add(
                    StructureErrors.hatchCount(
                        ErrorType.TOO_MANY,
                        TranslatableText.lang(
                            entry.getKey()
                                .getTranslationKey()),
                        entry.getValue(),
                        limit));
            }
        }
    }

    @Override
    protected final ProcessingLogic createProcessingLogic() {
        return createRecipeThreadProcessingLogic();
    }

    protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
        return new RecipeThreadProcessingLogic();
    }

    @Override
    protected void setupProcessingLogic(ProcessingLogic logic) {
        super.setupProcessingLogic(logic);
        long power = availableThreadPower();
        long voltage = Math.min(power, Math.max(0, getMaxInputVoltage()));
        logic.setAvailableVoltage(voltage)
            .setAvailableAmperage(voltage == 0 ? 0 : Math.max(1, power / voltage))
            .setAmperageOC(false)
            .setMaxParallelSupplier(this::getTrueParallel)
            .setSpeedBonus(1D / getSpeedMultiplier())
            .setEuModifier(getEUtMultiplier())
            .setOverclock(getOverclockTimeReduction(), getOverclockPowerIncrease());
        if (preparingThread >= 0) ((RecipeThreadProcessingLogic) logic).prepareRecipe(power);
    }

    @Override
    protected CheckRecipeResult doCheckRecipe() {
        CheckRecipeResult result = patternScheduler
            .checkNext(mDualInputHatches, (RecipeThreadProcessingLogic) processingLogic);
        return result.wasSuccessful() ? result : super.doCheckRecipe();
    }

    @Override
    @Nonnull
    public final CheckRecipeResult checkProcessing() {
        if (!(processingLogic instanceof RecipeThreadProcessingLogic logic)) {
            return CheckRecipeResultRegistry.INTERNAL_ERROR;
        }
        int slot = preparingThread < 0 ? firstFreeThread() : preparingThread;
        if (slot < 0 || slot >= getTotalRecipeThreads() || threadTasks.containsKey(slot)) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }
        preparingThread = slot;
        SingleRecipeCheck previousRecipeCheck = getSingleRecipeCheck();
        boolean committed = false;
        try {
            setupProcessingLogic(logic);
            CheckRecipeResult result = postCheckRecipe(doCheckRecipe(), logic);
            if (!result.wasSuccessful() && availableThreadPower() < remainingPower()) {
                logic.finishRecipe();
                useRemainingPower = true;
                setupProcessingLogic(logic);
                result = postCheckRecipe(doCheckRecipe(), logic);
            }
            if (!result.wasSuccessful()) return result;
            long eut = logic.getCalculatedEut();
            if (eut < 0 || !logic.canCommitInputs()) return CheckRecipeResultRegistry.INTERNAL_ERROR;
            if (!RecipeThreadInputPreflight.canCommit(this, logic)) {
                // #tr GT5U.gui.text.recipe_result.gtnr_thread_me_unavailable
                // # Insufficient ME stock or network energy
                // # zh_CN ME 库存或网络能源不足
                return SimpleCheckRecipeResult.ofFailure("gtnr_thread_me_unavailable");
            }
            if (eut > availableThreadPower()) return CheckRecipeResultRegistry.insufficientPower(eut);
            RecipeThreadTask task = new RecipeThreadTask(
                slot,
                eut,
                Math.max(1, logic.getDuration()),
                Math.max(1, logic.getCurrentParallels()),
                new RecipeThreadOutputs(logic.getOutputItems(), logic.getOutputFluids()));
            if (!logic.commitInputs()) return CheckRecipeResultRegistry.INTERNAL_ERROR;
            committed = true;
            threadTasks.put(slot, task);
            pendingTask = task;
            mEfficiency = Math.max(1000, 10000 - (getIdealStatus() - getRepairStatus()) * 1000);
            mEfficiencyIncrease = 10000;
            updateSlots();
            markDirty();
            return CheckRecipeResultRegistry.SUCCESSFUL;
        } finally {
            if (!committed) setSingleRecipeCheck(previousRecipeCheck);
            logic.finishRecipe();
            useRemainingPower = false;
            preparingThread = -1;
        }
    }

    @Override
    protected final void runMachine(IGregTechTileEntity tile, long tick) {
        paused = !tile.isAllowedToWork();
        if (paused) {
            updateThreadDisplay();
            return;
        }
        int total = getTotalRecipeThreads();
        long power = usedThreadPower();
        paused = power > Math.max(0, getMaxInputEu());
        boolean hasRunning = threadTasks.values()
            .stream()
            .anyMatch(task -> task.getThread() < total && !task.isFinished());
        if (!paused && hasRunning) {
            mEfficiency = Math.max(1000, 10000 - (getIdealStatus() - getRepairStatus()) * 1000);
            lEUt = -power;
            paused = !doRandomMaintenanceDamage() || !onRunningTick(getControllerSlot())
                || !polluteEnvironment(getPollutionPerTick(getControllerSlot()));
            if (!paused) {
                for (RecipeThreadTask task : threadTasks.values()) {
                    if (task.getThread() < total) task.advance();
                }
                markDirty();
            }
        }
        boolean completed = false;
        for (Iterator<RecipeThreadTask> iterator = threadTasks.values()
            .iterator(); iterator.hasNext();) {
            RecipeThreadTask task = iterator.next();
            if (task.isFinished() && task.getOutputs()
                .flush(this)) {
                iterator.remove();
                recipesDone += task.getParallels();
                outputAfterRecipe();
                completed = true;
                markDirty();
            }
        }
        if (!paused && tile.isAllowedToWork()
            && (completed || tick % RECIPE_CHECK_INTERVAL == 0
                || tile.hasInventoryBeenModified()
                || tile.hasWorkJustBeenEnabled()
                || shouldCheckRecipeThisTick(tick))) {
            for (int attempt = 0; attempt < MAX_STARTS_PER_TICK; attempt++) {
                int slot = firstFreeThread();
                if (slot < 0) break;
                preparingThread = slot;
                if (!checkRecipe()) break;
            }
        }
        updateThreadDisplay();
    }

    @Override
    public void startRecipeProcessing() {
        super.startRecipeProcessing();
        pendingTask = null;
    }

    @Override
    public void endRecipeProcessing() {
        try {
            super.endRecipeProcessing();
            if (pendingTask != null && !getCheckRecipeResult().wasSuccessful()) {
                threadTasks.remove(pendingTask.getThread());
                markDirty();
            }
        } finally {
            pendingTask = null;
        }
    }

    @Override
    public void stopMachine(@Nonnull ShutDownReason reason) {
        super.stopMachine(reason);
        paused = true;
        updateThreadDisplay();
    }

    private int firstFreeThread() {
        int total = getTotalRecipeThreads();
        for (int thread = 0; thread < total; thread++) {
            if (!threadTasks.containsKey(thread)) return thread;
        }
        return -1;
    }

    private long usedThreadPower() {
        int total = getTotalRecipeThreads();
        long used = 0;
        for (RecipeThreadTask task : threadTasks.values()) {
            if (task.getThread() < total && !task.isFinished()) {
                used += Math.min(Long.MAX_VALUE - used, task.getEUt());
            }
        }
        return used;
    }

    private long remainingPower() {
        return Math.max(0, getMaxInputEu() - usedThreadPower());
    }

    private long availableThreadPower() {
        long remaining = remainingPower();
        if (useRemainingPower || preparingThread < 0) return remaining;
        int total = getTotalRecipeThreads();
        long occupied = threadTasks.keySet()
            .stream()
            .filter(thread -> thread < total)
            .count();
        return remaining / Math.max(1, total - occupied);
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        for (ParameterModuleHatch hatch : parameterModules) hatch.setWorking(false);
        parameterModules.clear();
    }

    @Override
    public boolean addToMachineList(IGregTechTileEntity tile, int casingIndex) {
        return addParameterModuleToMachineList(tile, casingIndex) || super.addToMachineList(tile, casingIndex);
    }

    public final boolean addParameterModuleToMachineList(IGregTechTileEntity tile, int casingIndex) {
        if (tile == null || !(tile.getMetaTileEntity() instanceof ParameterModuleHatch hatch)) return false;
        if (getParameterModuleLimit(hatch.getModuleType()) <= 0) return false;
        if (!parameterModules.contains(hatch)) {
            hatch.updateTexture(casingIndex);
            hatch.updateCraftingIcon(getMachineCraftingIcon());
            parameterModules.add(hatch);
        }
        return true;
    }

    public final List<ParameterModuleHatch> getParameterModules() {
        return Collections.unmodifiableList(parameterModules);
    }

    @Override
    public long getMaxInputVoltage() {
        if (tooltipProbeState != null) return tooltipInputVoltage();
        long voltage = 0;
        for (var hatch : validMTEList(getExoticAndNormalEnergyHatchList())) {
            voltage = Math.max(
                voltage,
                hatch.getBaseMetaTileEntity()
                    .getInputVoltage());
        }
        return voltage;
    }

    @Override
    public long getMaxInputEu() {
        return tooltipProbeState == null ? super.getMaxInputEu() : tooltipInputVoltage();
    }

    private ParameterModuleHatch parameterModule(MachineModuleType type) {
        for (ParameterModuleHatch module : parameterModules) {
            if (module.getModuleType() == type) return module;
        }
        return null;
    }

    @Override
    public void getWailaBody(ItemStack stack, List<String> lines, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(stack, lines, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();
        if (tag.hasKey("gtnrTotalThreads")) {
            lines.add(threadCountText(tag.getInteger("gtnrRunningThreads"), tag.getInteger("gtnrTotalThreads")));
        }
    }

    @Override
    public String[] getInfoData() {
        List<String> lines = new ArrayList<>(List.of(super.getInfoData()));
        lines.add(threadCountText(getRunningRecipeThreads(), getTotalRecipeThreads()));
        return lines.toArray(String[]::new);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new AutoMultiblockTooltipBuilder().addAutoTooltip(this)
            .toolTipFinisher();
    }

    @Override
    public String getTooltipMachineType() {
        return getRecipeMap() == null ? null : StatCollector.translateToLocal(getRecipeMap().unlocalizedName);
    }

    @Override
    public TooltipStats getTooltipStats() {
        return getBaseMetaTileEntity() == null ? withBaseTooltipProbe(this::readTooltipStats) : readTooltipStats();
    }

    /** Base formulas shared with real processing; subclasses may retain their existing getTooltipStats override. */
    protected TooltipStats readTooltipStats() {
        return new TooltipStats(
            getBaseParallelRecipes(),
            getBaseSpeedMultiplier(),
            getBaseEUtMultiplier(),
            hasPerfectOverclock());
    }

    @Override
    public TooltipStats getTooltipStatsAtTier(TieredPart part, int tier) {
        if (part == null) throw new IllegalArgumentException("A tooltip probe needs a tiered part");
        new TierRange(tier, tier);
        if (part == TieredPart.VOLTAGE && tier >= GTValues.V.length) {
            throw new IllegalArgumentException("Unsupported voltage probe tier: " + tier);
        }
        return withTooltipProbe(new TooltipProbeState(part, tier), this::getTooltipStats);
    }

    /** Uses live structure state normally and the isolated formula input while a tooltip is being sampled. */
    protected final int getTooltipTier(TieredPart part, int runtimeTier) {
        if (tooltipProbeState == null) return runtimeTier;
        return tooltipProbeState.part() == part ? tooltipProbeState.tier() : 0;
    }

    /** Special energy getters should delegate to super while the detached tooltip inputs are active. */
    protected final boolean isTooltipProbeActive() {
        return tooltipProbeState != null;
    }

    @Override
    public HeatRules getTooltipHeatRules() {
        return withBaseTooltipProbe(() -> {
            OverclockCalculatorAccessorMixin first = tooltipCalculator(FIRST_HEAT_PROBE);
            OverclockCalculatorAccessorMixin second = tooltipCalculator(SECOND_HEAT_PROBE);
            if (first == null || second == null) return null;
            boolean heatOC = first.gtneverreset$heatOverclock();
            boolean discount = first.gtneverreset$heatDiscount();
            int voltageHeat = getVoltageHeatPerTier();
            if (!heatOC && !discount && voltageHeat == 0) return null;
            boolean usesRecipeHeat = first.gtneverreset$recipeHeat() != second.gtneverreset$recipeHeat();
            int freeOverclocks = heatOC && !usesRecipeHeat
                ? (int) Math
                    .max(
                        0L,
                        ((long) first.gtneverreset$machineHeat() - first.gtneverreset$recipeHeat())
                            / HEAT_OVERCLOCK_THRESHOLD)
                : 0;
            return new HeatRules(
                heatOC,
                discount,
                first.gtneverreset$heatDiscountFactor(),
                usesRecipeHeat,
                voltageHeat,
                getVoltageHeatFromTier(),
                freeOverclocks);
        });
    }

    @Override
    public OptionalInt getTooltipTierSkips() {
        return withBaseTooltipProbe(() -> {
            OverclockCalculatorAccessorMixin calculator = tooltipCalculator(FIRST_HEAT_PROBE);
            if (calculator == null || calculator.gtneverreset$maxTierSkip() == 1) return OptionalInt.empty();
            int skips = calculator.gtneverreset$maxTierSkip();
            if (skips < 0) throw new IllegalArgumentException("A recipe tier skip count cannot be negative");
            return OptionalInt.of(skips);
        });
    }

    private long tooltipInputVoltage() {
        return tooltipProbeState.part() == TieredPart.VOLTAGE ? GTValues.V[tooltipProbeState.tier()] : 0;
    }

    private <R> R withBaseTooltipProbe(Supplier<R> read) {
        return withTooltipProbe(tooltipProbeState == null ? new TooltipProbeState(null, 0) : tooltipProbeState, read);
    }

    private <R> R withTooltipProbe(TooltipProbeState state, Supplier<R> read) {
        if (getBaseMetaTileEntity() != null) {
            throw new IllegalStateException("Performance probes require a detached tooltip controller");
        }
        TooltipProbeState previous = tooltipProbeState;
        tooltipProbeState = state;
        try {
            return read.get();
        } finally {
            tooltipProbeState = previous;
        }
    }

    private OverclockCalculatorAccessorMixin tooltipCalculator(int recipeHeat) {
        if (!(createProcessingLogic() instanceof RecipeThreadProcessingLogic logic)) return null;
        GTRecipe recipe = new GTRecipe(
            false,
            new ItemStack[0],
            new ItemStack[0],
            null,
            null,
            null,
            null,
            null,
            new FluidStack[0],
            new FluidStack[0],
            HEAT_PROBE_DURATION,
            HEAT_PROBE_EUT,
            recipeHeat);
        OverclockCalculator calculator = logic.createTooltipOverclockCalculator(recipe);
        if (!(calculator instanceof OverclockCalculatorAccessorMixin accessor)) {
            throw new IllegalStateException("The configured overclock calculator is missing its tooltip accessor");
        }
        return accessor;
    }

    @Override
    public List<String> getTooltipFeatureLines() {
        // #tr gtnr.multiblock.threads.capacity
        // # Concurrent recipe threads: {\\AQUA}%s{\\RESET}
        // # zh_CN 跨配方线程数：{\\AQUA}%s{\\RESET}
        return List
            .of(StatCollector.translateToLocalFormatted("gtnr.multiblock.threads.capacity", getTotalRecipeThreads()));
    }

    private void updateThreadDisplay() {
        mOutputItems = null;
        mOutputFluids = null;
        mEUt = 0;
        lEUt = paused ? 0 : -usedThreadPower();
        mProgresstime = 0;
        mMaxProgresstime = 0;
        if (!paused) {
            int total = getTotalRecipeThreads();
            for (RecipeThreadTask task : threadTasks.values()) {
                if (task.getThread() < total && !task.isFinished()) {
                    mProgresstime = task.getProgress();
                    mMaxProgresstime = task.getDuration();
                    break;
                }
            }
        }
        for (ParameterModuleHatch module : parameterModules) {
            module.setWorking(mMaxProgresstime > 0);
        }
    }

    @Override
    protected GTNRMultiBlockBaseGui<?> getGui() {
        return new GTNRMultiBlockBaseGui<>(this);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        NBTTagList tasks = new NBTTagList();
        for (RecipeThreadTask task : threadTasks.values()) tasks.appendTag(task.save());
        tag.setTag("gtnrRecipeThreads", tasks);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        if (!tag.hasKey(INPUT_SEPARATION_NBT_KEY)) inputSeparation = getDefaultInputSeparationMode();
        threadTasks.clear();
        NBTTagList tasks = tag.getTagList("gtnrRecipeThreads", 10);
        for (int index = 0; index < tasks.tagCount(); index++) {
            RecipeThreadTask task = RecipeThreadTask.load(tasks.getCompoundTagAt(index));
            if (threadTasks.put(task.getThread(), task) != null) {
                throw new IllegalStateException("Duplicate saved recipe thread");
            }
        }
        paused = true;
        updateThreadDisplay();
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setInteger("gtnrRunningThreads", getRunningRecipeThreads());
        tag.setInteger("gtnrTotalThreads", getTotalRecipeThreads());
    }

    /** Read live; subclasses may derive this from items, modules or structure state. */
    public int getMaxRecipeThreads() {
        return 1;
    }

    public final int getTotalRecipeThreads() {
        return Math.max(1, getMaxRecipeThreads());
    }

    public final int getRunningRecipeThreads() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (paused || !mMachine || tile == null || !tile.isAllowedToWork()) return 0;
        int total = getTotalRecipeThreads();
        int running = 0;
        for (RecipeThreadTask task : threadTasks.values()) {
            if (task.getThread() < total && !task.isFinished()) running++;
        }
        return running;
    }

    public final List<RecipeThreadTask> getRecipeThreadTasks() {
        return List.copyOf(threadTasks.values());
    }

    protected int getBaseParallelRecipes() {
        return 1;
    }

    protected double getBaseSpeedMultiplier() {
        return 1;
    }

    protected double getBaseEUtMultiplier() {
        return 1;
    }

    protected boolean hasPerfectOverclock() {
        return false;
    }

    @Override
    public final int getMaxParallelRecipes() {
        ParameterModuleHatch module = parameterModule(MachineModuleType.Parallel);
        long multiplier = module == null ? 1 : 1L + module.getParameter();
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, getBaseParallelRecipes()) * multiplier);
    }

    public final double getSpeedMultiplier() {
        ParameterModuleHatch module = parameterModule(MachineModuleType.Speed);
        return getBaseSpeedMultiplier() * (module == null ? 1 : module.getParameter());
    }

    public final double getEUtMultiplier() {
        ParameterModuleHatch module = parameterModule(MachineModuleType.PowerConsumption);
        return getBaseEUtMultiplier() * (module == null ? 1 : module.getPowerMultiplier());
    }

    public final int getOverclockTimeReduction() {
        ParameterModuleHatch module = parameterModule(MachineModuleType.Overclock);
        return module == null ? (hasPerfectOverclock() ? 4 : 2) : module.getTimeReduction();
    }

    public final int getOverclockPowerIncrease() {
        ParameterModuleHatch module = parameterModule(MachineModuleType.Overclock);
        return module == null ? 4 : module.getPowerIncrease();
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public VoidingMode getDefaultVoidingMode() {
        return VoidingMode.VOID_NONE;
    }

    @Override
    public boolean supportsInputSeparation() {
        return true;
    }

    @Override
    public boolean getDefaultInputSeparationMode() {
        return true;
    }

    @Override
    public final boolean supportsSingleRecipeLocking() {
        return true;
    }

    @Override
    public boolean getDefaultRecipeLockingMode() {
        return false;
    }

    @Override
    public final boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean getDefaultBatchMode() {
        return true;
    }

    /** Override only when the same heat-per-tier value is used by the machine's real heat formula. */
    protected int getVoltageHeatPerTier() {
        return 0;
    }

    protected int getVoltageHeatFromTier() {
        return VoltageIndex.MV;
    }

    /** Shared by real machine heat and its tooltip; selecting a coil tier never changes the installed coil. */
    protected final int coilAndVoltageHeat(HeatingCoilLevel runtimeCoil) {
        HeatingCoilLevel coil = tooltipProbeState == null ? runtimeCoil
            : HeatingCoilLevel.getFromTier((byte) getTooltipTier(TieredPart.COIL, runtimeCoil.getTier()));
        long heat = coil.getHeat()
            + (long) getVoltageHeatPerTier() * (GTUtility.getTier(getMaxInputEu()) - getVoltageHeatFromTier());
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, heat));
    }

    @Override
    public int getParameterModuleLimit(MachineModuleType type) {
        return 1;
    }

    private record TooltipProbeState(TieredPart part, int tier) {}

}
