package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.api.ICropMutation;
import com.gtnewhorizon.cropsnh.api.IMachineBreedingRequirement;
import com.gtnewhorizon.cropsnh.farming.registries.CropRegistry;
import com.gtnewhorizon.cropsnh.farming.requirements.SubSoilRequirement;
import com.gtnewhorizon.cropsnh.init.CropsNHBlockTextures;
import com.gtnewhorizon.cropsnh.init.CropsNHFluids;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.LargeCropBreederGui;
import com.xyp.gtneverreset.common.machine.multiblock.crop.ArchivedSeedStats;
import com.xyp.gtneverreset.common.machine.multiblock.crop.CropBreedingChance;
import com.xyp.gtneverreset.common.machine.multiblock.crop.CropBreedingPlanner;
import com.xyp.gtneverreset.common.machine.multiblock.crop.CropBreedingRecipeRules;
import com.xyp.gtneverreset.common.machine.multiblock.crop.CropBreedingSubstrate;
import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipHooks;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.VoidProtectionHelper;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

@IMetaTileEntity.SkipGenerateDescription
public class LargeCropBreeder extends MTEExtendedPowerMultiBlockBase<LargeCropBreeder>
    implements ISurvivalConstructable, MultiblockTooltipSource {

    private static final String PIECE = "main";
    private static final int CASING_TEXTURE = 16;
    private static final int OFFSET_X = 2, OFFSET_Y = 3, OFFSET_Z = 0;
    private static final int MIN_CASINGS = 24;
    private static final int FERTILIZER_PER_PARALLEL = 56000;
    private static final int BASE_EUT = 512;
    private static final int WASH_UU_PER_TICK = 128;
    private static final int WASH_TICKS = 160;

    private final Map<String, ArchivedSeedStats> archive = new LinkedHashMap<>();
    private String targetCrop = "";
    private ItemStack activeSeed;
    private String activeCrop = "";
    private int activeChance;
    private int activeParallel;
    private boolean washingMode;
    private int activeWashStat;

    private IStructureDefinition<LargeCropBreeder> definition;
    private int casingCount;
    private int revision;
    private boolean planDirty = true;
    private boolean targetValid = true;
    private CropBreedingPlanner.Plan plan;

    public LargeCropBreeder(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public LargeCropBreeder(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new LargeCropBreeder(mName);
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (tile.isServerSide() && mMachine && tick % 20 == 0) {
            startRecipeProcessing();
            try {
                collectSamples();
            } finally {
                endRecipeProcessing();
            }
        }
    }

    @Override
    public IStructureDefinition<LargeCropBreeder> getStructureDefinition() {
        if (definition == null) {
            definition = StructureDefinition.<LargeCropBreeder>builder()
                .addShape(PIECE, transpose(MultiblockStructureLoader.load("large_crop_breeder")))
                .addElement(
                    'A',
                    buildHatchAdder(LargeCropBreeder.class)
                        .atLeast(InputBus, OutputBus, InputHatch, Energy.or(ExoticEnergy), Maintenance)
                        .casingIndex(CASING_TEXTURE)
                        .hint(1)
                        .buildAndChain(
                            onElementPass(machine -> ++machine.casingCount, ofBlock(GregTechAPI.sBlockCasings2, 0))))
                .addElement('G', ofBlock(Blocks.glass, 0))
                .addElement('D', ofBlock(Blocks.dirt, 0))
                .build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(PIECE, stack, hintsOnly, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(PIECE, stack, OFFSET_X, OFFSET_Y, OFFSET_Z, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        casingCount = 0;
        if (checkPiece(PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z, errors)) collectTooltipStructureRequirements(errors);
    }

    @Override
    public void collectTooltipStructureRequirements(List<StructureError> errors) {
        if (!StructureTooltipHooks.onCheckCasingMin(this, MIN_CASINGS))
            checkCasingMin(errors, casingCount, MIN_CASINGS);
        requireHatch(errors, InputBus, mInputBusses.size());
        requireHatch(errors, OutputBus, mOutputBusses.size());
        requireHatch(errors, InputHatch, mInputHatches.size());
        int energy = mEnergyHatches.size() + mExoticEnergyHatches.size();
        if (!StructureTooltipHooks.onCheckHatches(this, 1, Integer.MAX_VALUE, Energy, ExoticEnergy) && energy == 0)
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, Energy, 0, 1));
        if (shouldCheckMaintenance()) requireHatch(errors, Maintenance, mMaintenanceHatches.size());
    }

    private void requireHatch(List<StructureError> errors, HatchElement hatch, int count) {
        if (!StructureTooltipHooks.onCheckHatches(this, 1, Integer.MAX_VALUE, hatch) && count == 0)
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, hatch, 0, 1));
    }

    @Override
    public CheckRecipeResult checkProcessing() {
        activeSeed = null;
        activeCrop = "";
        activeWashStat = 0;
        collectSamples();
        if (targetCrop.isEmpty()) return CheckRecipeResultRegistry.NO_RECIPE;
        if (washingMode) return startWashing();
        List<ICropMutation> route = getPlan().steps;
        if (route.isEmpty()) {
            // #tr GT5U.gui.text.recipe_result.gtnr_crop_no_route
            // # No machine breeding route from the archived parents.
            // # zh_CN 已存档亲本无法组成机器杂交路线。
            return SimpleCheckRecipeResult.ofFailure("gtnr_crop_no_route");
        }
        ICropMutation mutation = route.get(0);
        long recipeVoltage = CropBreedingRecipeRules.requiredVoltage(mutation.getOutput());
        if (recipeVoltage > getMaxInputVoltage() || BASE_EUT > getMaxInputEu())
            return CheckRecipeResultRegistry.insufficientPower(recipeVoltage);
        ArrayList<ICropCard> parents = new ArrayList<>(mutation.getParents());
        for (ICropCard parent : parents) {
            if (!archive.containsKey(parent.getId())) return CheckRecipeResultRegistry.NO_RECIPE;
        }
        ArchivedSeedStats stats = archive.get(
            mutation.getOutput()
                .getId());
        stats = stats == null ? ArchivedSeedStats.initial() : stats;
        ItemStack output = mutation.getOutput()
            .getSeedItem(stats);
        if (output == null) return CheckRecipeResultRegistry.NO_RECIPE;
        output.stackSize = 1;
        if (!hasOutputSpace(output)) return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;

        ItemStack[] catalysts = getStoredInputs().toArray(new ItemStack[0]);
        int[] consume = breedingConsumption(mutation, parents, catalysts);
        if (!validConsumption(consume, catalysts)) {
            // #tr GT5U.gui.text.recipe_result.gtnr_crop_requirements
            // # Missing breeding catalysts / substrate, or machine breeding conditions not met.
            // # zh_CN 缺少杂交催化剂或底土，或不满足机器杂交条件。
            return SimpleCheckRecipeResult.ofFailure("gtnr_crop_requirements");
        }
        FluidStack fertilizer = new FluidStack(CropsNHFluids.enrichedFertilizer, FERTILIZER_PER_PARALLEL);
        if (!depleteInput(fertilizer, true)) {
            // #tr GT5U.gui.text.recipe_result.gtnr_crop_fertilizer
            // # Not enough enriched fertilizer.
            // # zh_CN 富集肥料不足。
            return SimpleCheckRecipeResult.ofFailure("gtnr_crop_fertilizer");
        }

        int parallels = 1;
        ItemStack[] remaining = new ItemStack[catalysts.length];
        for (int i = 0; i < catalysts.length; i++) {
            remaining[i] = catalysts[i] == null ? null : catalysts[i].copy();
            if (remaining[i] != null) remaining[i].stackSize -= consume[i];
        }
        while (parallels < getMaxParallelRecipes() && (long) BASE_EUT * (parallels + 1) <= getMaxInputEu()) {
            fertilizer.amount = FERTILIZER_PER_PARALLEL * (parallels + 1);
            output.stackSize = parallels + 1;
            if (!depleteInput(fertilizer, true) || !hasOutputSpace(output)) break;
            int[] additional = breedingConsumption(mutation, parents, remaining);
            if (!validConsumption(additional, remaining)) break;
            for (int i = 0; i < consume.length; i++) {
                consume[i] += additional[i];
                if (remaining[i] != null) remaining[i].stackSize -= additional[i];
            }
            parallels++;
        }
        fertilizer.amount = FERTILIZER_PER_PARALLEL * parallels;
        int duration = CropBreedingRecipeRules.baseDuration(mutation.getOutput());
        long eut = BASE_EUT;
        while (duration > 1 && recipeVoltage <= getMaxInputVoltage() / 4 && eut * parallels <= getMaxInputEu() / 4) {
            recipeVoltage *= 4;
            eut *= 4;
            duration = Math.max(1, duration / 2);
        }
        depleteInput(fertilizer);
        for (int i = 0; i < consume.length; i++) if (consume[i] > 0) catalysts[i].stackSize -= consume[i];
        updateSlots();
        activeCrop = mutation.getOutput()
            .getId();
        activeParallel = parallels;
        activeChance = CropBreedingChance.percent(mutation.getOutput());
        int successes = 0;
        for (int i = 0; i < parallels; i++)
            if (getBaseMetaTileEntity().getRandomNumber(100) < activeChance) successes++;
        output.stackSize = successes;
        activeSeed = successes > 0 ? output.copy() : null;
        mOutputItems = seedOutputs(activeSeed);
        mMaxProgresstime = duration;
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
        lEUt = -eut * parallels;
        markChanged(false);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private int[] breedingConsumption(ICropMutation mutation, ArrayList<ICropCard> parents, ItemStack[] catalysts) {
        int[] reserved = new int[catalysts.length];
        for (CropBreedingSubstrate substrate : CropBreedingSubstrate.forMutation(mutation)) {
            if (substrate.isAutomaticallySatisfied()) continue;
            int slot = 0;
            while (slot < catalysts.length && !substrate.matches(catalysts[slot])) slot++;
            if (slot == catalysts.length) return null;
            reserved[slot] = 1;
        }
        int[] consumed = reserved.clone();
        for (var requirement : mutation.getRequirements()) {
            if (requirement instanceof SubSoilRequirement) continue;
            if (requirement instanceof IMachineBreedingRequirement machineRequirement
                && !machineRequirement.canBreed(parents, getBaseMetaTileEntity(), catalysts, consumed)) return null;
        }
        if (!validConsumption(consumed, catalysts)) return null;
        for (int i = 0; i < consumed.length; i++) consumed[i] -= reserved[i];
        return consumed;
    }

    private int chooseWashStat(ArchivedSeedStats stats) {
        int[] available = new int[3];
        int count = 0;
        if (stats.getGrowth() < 31) available[count++] = 1;
        if (stats.getGain() < 31) available[count++] = 2;
        if (stats.getResistance() < 31) available[count++] = 3;
        return count == 0 ? 0 : available[getBaseMetaTileEntity().getRandomNumber(count)];
    }

    private CheckRecipeResult startWashing() {
        ArchivedSeedStats stats = archive.get(targetCrop);
        // #tr GT5U.gui.text.recipe_result.gtnr_crop_wash_unarchived
        // # Select an archived crop to improve.
        // # zh_CN 请先选中已入档的作物。
        if (stats == null) return SimpleCheckRecipeResult.ofFailure("gtnr_crop_wash_unarchived");
        int stat = chooseWashStat(stats);
        // #tr GT5U.gui.text.recipe_result.gtnr_crop_wash_max
        // # All three seed stats are at their maximum.
        // # zh_CN 这粒种子的三项属性均已满。
        if (stat == 0) return SimpleCheckRecipeResult.ofFailure("gtnr_crop_wash_max");
        if (getMaxInputVoltage() < BASE_EUT || getMaxInputEu() < BASE_EUT)
            return CheckRecipeResultRegistry.insufficientPower(BASE_EUT);
        FluidStack uu = Materials.UUMatter.getFluid(WASH_UU_PER_TICK);
        // #tr GT5U.gui.text.recipe_result.gtnr_crop_wash_uu
        // # Not enough UU-Matter to improve the seed.
        // # zh_CN 洗种所需的 UU 物质不足。
        if (!depleteInput(uu, true)) return SimpleCheckRecipeResult.ofFailure("gtnr_crop_wash_uu");
        activeWashStat = stat;
        activeCrop = targetCrop;
        activeParallel = 1;
        activeChance = 100;
        mOutputItems = new ItemStack[0];
        mMaxProgresstime = WASH_TICKS;
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
        lEUt = -BASE_EUT;
        markChanged(false);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    public ItemStack findSubstrate(CropBreedingSubstrate requirement) {
        for (ItemStack stack : getStoredInputs()) {
            if (!requirement.matches(stack)) continue;
            ItemStack present = stack.copy();
            present.stackSize = 1;
            return present;
        }
        return null;
    }

    private static boolean validConsumption(int[] consume, ItemStack[] catalysts) {
        if (consume == null || consume.length != catalysts.length) return false;
        for (int i = 0; i < consume.length; i++) {
            if (consume[i] < 0 || consume[i] > 0 && (catalysts[i] == null || consume[i] > catalysts[i].stackSize))
                return false;
        }
        return true;
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (activeSeed != null && mProgresstime + 1 >= mMaxProgresstime && !hasOutputSpace(activeSeed)) {
            checkRecipeResult = CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
            return false;
        }
        if (checkRecipeResult == CheckRecipeResultRegistry.ITEM_OUTPUT_FULL)
            checkRecipeResult = CheckRecipeResultRegistry.SUCCESSFUL;
        if (activeWashStat == 0) return super.onRunningTick(stack);
        FluidStack uu = Materials.UUMatter.getFluid(WASH_UU_PER_TICK);
        if (!depleteInput(uu, true)) {
            checkRecipeResult = SimpleCheckRecipeResult.ofFailure("gtnr_crop_wash_uu");
            return false;
        }
        if (!super.onRunningTick(stack)) return false;
        if (!depleteInput(uu)) {
            checkRecipeResult = SimpleCheckRecipeResult.ofFailure("gtnr_crop_wash_uu");
            return false;
        }
        checkRecipeResult = CheckRecipeResultRegistry.SUCCESSFUL;
        return true;
    }

    private static ItemStack[] seedOutputs(ItemStack seed) {
        if (seed == null) return new ItemStack[0];
        List<ItemStack> result = new ArrayList<>();
        for (int remaining = seed.stackSize; remaining > 0;) {
            ItemStack stack = seed.copy();
            stack.stackSize = Math.min(remaining, stack.getMaxStackSize());
            result.add(stack);
            remaining -= stack.stackSize;
        }
        return result.toArray(new ItemStack[0]);
    }

    private boolean hasOutputSpace(ItemStack output) {
        return !new VoidProtectionHelper().setMachine(this, true, false)
            .setItemOutputs(seedOutputs(output))
            .setMaxParallel(1)
            .build()
            .isItemFull();
    }

    @Override
    protected void outputAfterRecipe() {
        if (activeWashStat != 0) {
            ArchivedSeedStats previous = archive.get(activeCrop);
            if (previous != null) archive.put(
                activeCrop,
                new ArchivedSeedStats(
                    (byte) (previous.getGrowth() + (activeWashStat == 1 ? 1 : 0)),
                    (byte) (previous.getGain() + (activeWashStat == 2 ? 1 : 0)),
                    (byte) (previous.getResistance() + (activeWashStat == 3 ? 1 : 0)),
                    true));
        } else if (activeSeed != null) {
            ICropCard crop = CropRegistry.instance.get(activeSeed, false);
            if (crop != null) archive.putIfAbsent(crop.getId(), ArchivedSeedStats.fromTag(activeSeed.getTagCompound()));
        }
        super.outputAfterRecipe();
        activeWashStat = 0;
        activeSeed = null;
        activeCrop = "";
        markChanged(true);
    }

    private void collectSamples() {
        boolean changed = false;
        for (ItemStack stack : getStoredInputs()) {
            if (stack == null || stack.stackSize <= 0) continue;
            ICropCard crop = CropRegistry.instance.get(stack, false);
            if (crop == null || archive.containsKey(crop.getId())) continue;
            if (stack.getTagCompound() == null) continue;
            ArchivedSeedStats stats = ArchivedSeedStats.fromTag(stack.getTagCompound());
            if (!stats.isAnalyzed()) continue;
            archive.put(crop.getId(), stats);
            stack.stackSize--;
            changed = true;
        }
        if (changed) {
            updateSlots();
            markChanged(true);
        }
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(CASING_TEXTURE);
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(
                active ? CropsNHBlockTextures.OVERLAY_FRONT_CROP_BREEDER_ACTIVE
                    : CropsNHBlockTextures.OVERLAY_FRONT_CROP_BREEDER)
            .extFacing()
            .build() };
    }

    @Override
    public MultiblockTooltipBuilder createTooltip() {
        return new AutoMultiblockTooltipBuilder().addAutoTooltip(this)
            .toolTipFinisher();
    }

    @Override
    public String getTooltipMachineType() {
        // #tr gtnr.crop_breeder.type
        // # Crop Breeder
        // # zh_CN 作物杂交机
        return StatCollector.translateToLocal("gtnr.crop_breeder.type");
    }

    @Override
    public TooltipStats getTooltipStats() {
        return new TooltipStats(0, Double.NaN, Double.NaN, false);
    }

    @Override
    public String getTooltipPath() {
        return "large-crop-breeder";
    }

    @Override
    public Map<String, Object> getTooltipVars() {
        return Map.of(
            "baseEu",
            BASE_EUT,
            "fertilizer",
            FERTILIZER_PER_PARALLEL,
            "washUU",
            WASH_UU_PER_TICK,
            "washSeconds",
            WASH_TICKS / 20,
            "firstSeconds",
            CropBreedingRecipeRules.baseDuration(1) / 20);
    }

    @Override
    protected MTEMultiBlockBaseGui<LargeCropBreeder> getGui() {
        return new LargeCropBreederGui(this);
    }

    public void setTargetCrop(String value) {
        String id = value == null ? "" : value.trim();
        targetValid = id.isEmpty() || CropRegistry.instance.get(id) != null;
        if (!targetValid || targetCrop.equals(id)) return;
        targetCrop = id;
        markChanged(true);
    }

    public String getTargetCrop() {
        return targetCrop;
    }

    public String getActiveCrop() {
        return mMaxProgresstime > 0 ? activeCrop : "";
    }

    public boolean isTargetValid() {
        return targetValid;
    }

    public int getRevision() {
        return revision;
    }

    public List<String> getArchivedCrops() {
        return new ArrayList<>(archive.keySet());
    }

    public ItemStack getArchivedSeed(String id) {
        ICropCard crop = CropRegistry.instance.get(id);
        ArchivedSeedStats stats = archive.get(id);
        return crop == null || stats == null ? null : crop.getSeedItem(stats);
    }

    public CropBreedingPlanner.Plan getPlan() {
        if (planDirty || plan == null) {
            plan = CropBreedingPlanner.plan(targetCrop, archive.keySet());
            planDirty = false;
        }
        return plan;
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        writeArchive(tag);
        tag.setString("cropActive", activeCrop);
        tag.setInteger("cropActiveChance", activeChance);
        tag.setInteger("cropActiveParallel", activeParallel);
        tag.setInteger("cropActiveWashStat", activeWashStat);
        if (activeSeed != null) tag.setTag("cropActiveSeed", activeSeed.writeToNBT(new NBTTagCompound()));
        else tag.removeTag("cropActiveSeed");
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        targetCrop = tag.getString("cropTarget");
        washingMode = tag.getBoolean("cropWashingMode");
        archive.clear();
        NBTTagList samples = tag.getTagList("cropArchive", 10);
        for (int i = 0; i < samples.tagCount(); i++) {
            NBTTagCompound sample = samples.getCompoundTagAt(i);
            String id = sample.getString("id");
            if (CropRegistry.instance.get(id) != null) archive.put(id, ArchivedSeedStats.fromTag(sample));
        }
        activeCrop = tag.getString("cropActive");
        activeParallel = Math.max(1, tag.getInteger("cropActiveParallel"));
        activeWashStat = tag.getInteger("cropActiveWashStat");
        if (activeWashStat < 1 || activeWashStat > 3) activeWashStat = 0;
        activeChance = tag.hasKey("cropActiveChance") ? tag.getInteger("cropActiveChance")
            : CropBreedingChance.percent(CropRegistry.instance.get(activeCrop));
        activeSeed = tag.hasKey("cropActiveSeed") ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("cropActiveSeed"))
            : null;
        planDirty = true;
        revision++;
    }

    @Override
    public void setItemNBT(NBTTagCompound tag) {
        writeArchive(tag);
        super.setItemNBT(tag);
    }

    private void writeArchive(NBTTagCompound tag) {
        tag.setString("cropTarget", targetCrop);
        tag.setBoolean("cropWashingMode", washingMode);
        NBTTagList samples = new NBTTagList();
        archive.forEach((id, stats) -> {
            NBTTagCompound sample = stats.writeToNBT(new NBTTagCompound());
            sample.setString("id", id);
            samples.appendTag(sample);
        });
        tag.setTag("cropArchive", samples);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return null;
    }

    @Override
    public int getMaxParallelRecipes() {
        return 1 << Math.max(0, Math.min(14, (GTUtility.getTier(getMaxInputVoltage()) - VoltageIndex.HV) / 2));
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public boolean doesBindPlayerInventory() {
        return true;
    }

    public int getSuccessChance() {
        if (!getActiveCrop().isEmpty()) return activeChance;
        List<ICropMutation> steps = getPlan().steps;
        return steps.isEmpty() ? 0
            : CropBreedingChance.percent(
                steps.get(0)
                    .getOutput());
    }

    public int getCurrentParallel() {
        return mMaxProgresstime > 0 ? activeParallel : washingMode ? 1 : getMaxParallelRecipes();
    }

    public boolean isWashingMode() {
        return washingMode;
    }

    public boolean showsWashingOperation() {
        return mMaxProgresstime > 0 ? activeWashStat != 0 : washingMode;
    }

    public void toggleWashingMode() {
        washingMode = !washingMode;
        markChanged(false);
    }

    private void markChanged(boolean replan) {
        planDirty |= replan;
        revision++;
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null) tile.markDirty();
    }
}
