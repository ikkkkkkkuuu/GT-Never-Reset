package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.*;
import static gregtech.api.enums.HatchElement.*;
import static gregtech.api.util.GTStructureUtility.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.common.crafting.chaos.ChaosBuffer;
import com.xyp.gtneverreset.common.crafting.chaos.ChaosChain;
import com.xyp.gtneverreset.common.crafting.chaos.ChaosPresets;
import com.xyp.gtneverreset.common.crafting.chaos.ChaosProcessingGroup;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.BottleInChaosGui;
import com.xyp.gtneverreset.common.item.items.ChaosCredentialItem;
import com.xyp.gtneverreset.common.item.items.ChaosStructureModuleItem;
import com.xyp.gtneverreset.common.machine.hatch.ChaosCredentialHatch;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadInputPreflight;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipHooks;
import com.xyp.gtneverreset.common.me.ChaosPatternCompiler;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import appeng.api.implementations.ICraftingPatternItem;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoidingMode;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.tileentities.machines.IDualInputHatch;

@IMetaTileEntity.SkipGenerateDescription
public final class BottleInChaos extends MTEExtendedPowerMultiBlockBase<BottleInChaos>
    implements ISurvivalConstructable, MultiblockTooltipSource {

    public static final int GROUP_COUNT = 8;
    public static final int MODULE_SLOTS = 16;
    public static final int CREDENTIAL_INPUT = 16, CREDENTIAL_OUTPUT = 17, MODULE_INPUT = 18;
    public static final int MACHINE_INPUT = 19, CASING_INPUT = 20, MODULE_OUTPUT = 21, PATTERN_START = 22;
    public static final int INVENTORY_SIZE = PATTERN_START + ChaosChain.MAX_STEPS;
    private static final int TEXTURE = 16, OFFSET_X = 5, OFFSET_Y = 8, OFFSET_Z = 0;
    private static final long BASE_CAPACITY = 1_000_000L;

    private ChaosProcessingGroup[] groups = new ChaosProcessingGroup[GROUP_COUNT];
    private int preset;
    // #tr gtnr.chaos.status.idle
    // # Idle
    // # zh_CN 空闲
    private String feedback = "gtnr.chaos.status.idle";
    private final ItemStackHandler workbench = new ItemStackHandler(INVENTORY_SIZE) {

        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack == null) return false;
            if (slot < MODULE_SLOTS) return ChaosStructureModuleItem.unpack(stack) != null;
            if (slot == CREDENTIAL_INPUT)
                return stack.getItem() instanceof ChaosCredentialItem && !ChaosCredentialItem.isBound(stack);
            if (slot == MODULE_INPUT) return stack.getItem() instanceof ChaosStructureModuleItem
                && ChaosStructureModuleItem.unpack(stack) == null;
            if (slot == MACHINE_INPUT) return ChaosStructureModuleItem.source(stack) != null;
            if (slot == CASING_INPUT) return stack.isItemEqual(new ItemStack(GregTechAPI.sBlockCasings2, 1, 0));
            return slot >= PATTERN_START && stack.getItem() instanceof ICraftingPatternItem;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < MODULE_SLOTS && hasWork() ? null : super.extractItem(slot, amount, simulate);
        }
    };

    private final ChaosCredentialHatch[] credentialHatches = new ChaosCredentialHatch[GROUP_COUNT];
    private final Map<Object, Integer> dedicatedInputs = new IdentityHashMap<>();
    private IStructureDefinition<BottleInChaos> structure;
    private int glassTier;
    private int nextGroup;

    public BottleInChaos(int id, String name, String regionalName) {
        super(id, name, regionalName);
        initializeGroups();
    }

    public BottleInChaos(String name) {
        super(name);
        initializeGroups();
    }

    private void initializeGroups() {
        for (int i = 0; i < groups.length; i++) groups[i] = new ChaosProcessingGroup();
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new BottleInChaos(mName);
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        if (credentialHatches != null) {
            for (ChaosCredentialHatch hatch : credentialHatches) if (hatch != null) hatch.unbind(this);
            Arrays.fill(credentialHatches, null);
        }
        if (dedicatedInputs != null) dedicatedInputs.clear();
    }

    @Override
    public IStructureDefinition<BottleInChaos> getStructureDefinition() {
        if (structure == null) {
            com.gtnewhorizon.structurelib.structure.IStructureElement<BottleInChaos> casing = ofBlock(
                GregTechAPI.sBlockCasings2,
                0);
            var builder = StructureDefinition.<BottleInChaos>builder()
                .addShape("main", transpose(MultiblockStructureLoader.load("bottle_in_chaos")))
                .addElement(
                    'X',
                    buildHatchAdder(BottleInChaos.class)
                        .atLeast(InputBus, InputHatch, OutputBus, OutputHatch, Energy.or(ExoticEnergy), Maintenance)
                        .casingIndex(TEXTURE)
                        .hint(1)
                        .buildAndChain(casing))
                .addElement('K', casing)
                .addElement('L', ofBlock(GregTechAPI.sBlockCasings8, 7))
                .addElement(
                    'V',
                    chainAllGlasses(-1, (machine, tier) -> machine.glassTier = tier, machine -> machine.glassTier));
            for (int i = 0; i < GROUP_COUNT; i++) {
                final int slot = i;
                builder.addElement(
                    (char) ('A' + i),
                    ofChain(
                        StructureTooltipHooks.describeAsHatch(
                            ofHatchAdder(
                                (BottleInChaos machine, IGregTechTileEntity tile, Short texture) -> machine
                                    .addCredential(slot, tile, texture),
                                TEXTURE,
                                2 + i),
                            ChaosCredentialHatch.NAME_KEY),
                        casing));
                builder.addElement(
                    (char) ('a' + i),
                    ofChain(
                        ofHatchAdder(
                            (BottleInChaos machine, IGregTechTileEntity tile, Short texture) -> machine
                                .addDedicated(slot, tile, texture),
                            TEXTURE,
                            10 + i),
                        casing));
            }
            structure = builder.build();
        }
        return structure;
    }

    private boolean addCredential(int slot, IGregTechTileEntity tile, int texture) {
        if (tile == null || !(tile.getMetaTileEntity() instanceof ChaosCredentialHatch hatch)) return false;
        if (!hatch.bind(this, slot)) return false;
        credentialHatches[slot] = hatch;
        hatch.updateTexture(texture);
        return true;
    }

    private boolean addDedicated(int slot, IGregTechTileEntity tile, int texture) {
        if (tile == null || tile.getMetaTileEntity() instanceof IDualInputHatch) return false;
        Object hatch = tile.getMetaTileEntity();
        if (!(hatch instanceof MTEHatchInputBus) && !(hatch instanceof MTEHatchInput)) return false;
        if (!super.addToMachineList(tile, texture)) return false;
        dedicatedInputs.put(hatch, slot);
        return true;
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        glassTier = -1;
        checkPiece("main", OFFSET_X, OFFSET_Y, OFFSET_Z, errors);
        collectTooltipStructureRequirements(errors);
        for (var hatch : getExoticAndNormalEnergyHatchList())
            if (hatch.mTier > glassTier) errors.add(StructureErrors.glassTierNotEnough(hatch.mTier));
    }

    @Override
    public void collectTooltipStructureRequirements(List<StructureError> errors) {
        checkHatchMin(errors, InputBus, 1);
        checkHatchMin(errors, OutputBus, 1);
        checkHatchMin(errors, OutputHatch, 1);
        if (mExoticEnergyHatches.isEmpty()) checkHatchMin(errors, Energy, 1);
        checkHatchMin(errors, Maintenance, 1);
        checkHatchMax(errors, Maintenance, 1);
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment environment) {
        return mMachine ? -1
            : survivalBuildPiece("main", stack, OFFSET_X, OFFSET_Y, OFFSET_Z, budget, environment, false, true);
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return null;
    }

    @Override
    public CheckRecipeResult checkProcessing() {
        return CheckRecipeResultRegistry.NO_RECIPE;
    }

    @Override
    protected void runMachine(IGregTechTileEntity tile, long tick) {
        long power = 0;
        for (int i = 0; i < GROUP_COUNT; i++)
            if (connected(i) && groups[i].enabled()) power = Math.addExact(power, groups[i].eut());
        boolean running = tile.isAllowedToWork() && power <= getMaxInputEu() && (power == 0 || drainEnergyInput(power));
        if (running) {
            for (int i = 0; i < GROUP_COUNT; i++)
                if (connected(i) && groups[i].enabled() && groups[i].remaining() > 0) {
                    try {
                        groups[i].advance();
                    } catch (RuntimeException failure) {
                        groups[i].status("gtnr.chaos.error.recipe");
                    }
                    markDirty();
                }
        }
        if (tick % 5 == 0) for (int offset = 0; offset < GROUP_COUNT; offset++) {
            int i = (nextGroup + offset) % GROUP_COUNT;
            if (!connected(i)) continue;
            ChaosProcessingGroup group = groups[i];
            if (group.remaining() > 0) {
                // #tr gtnr.chaos.status.running
                // # Processing
                // # zh_CN 加工中
                // #tr gtnr.chaos.status.paused
                // # Paused / power unavailable
                // # zh_CN 暂停／供电不足
                group.status(running && group.enabled() ? "gtnr.chaos.status.running" : "gtnr.chaos.status.paused");
                continue;
            }
            try {
                if (group.busy() && (group.exporting() || group.currentStep() == null)) {
                    // #tr gtnr.chaos.status.output
                    // # Exporting / output blocked
                    // # zh_CN 输出中／输出堵塞
                    group.status("gtnr.chaos.status.output");
                    group.flush(this);
                    markDirty();
                }
                if (!running || !group.enabled() || group.exporting()) continue;
                if (!group.busy()) {
                    ItemStack credential = credentialHatches[i].credential();
                    // #tr gtnr.chaos.status.credential
                    // # Insert a bound credential
                    // # zh_CN 需要已绑定凭证
                    if (!ChaosCredentialItem.isBound(credential)) {
                        group.status("gtnr.chaos.status.credential");
                        continue;
                    }
                    group.bind(
                        credential.getTagCompound()
                            .getCompoundTag(ChaosCredentialItem.CHAIN_KEY));
                    markDirty();
                }
                startStep(i);
            } catch (RuntimeException failure) {
                group.status("gtnr.chaos.error.recipe");
            }
        }
        if (tick % 5 == 0) nextGroup = (nextGroup + 1) % GROUP_COUNT;
        lEUt = running ? -power : 0;
        mEfficiency = 10000;
        mMaxProgresstime = running && power > 0 ? 20 : 0;
        mProgresstime = mMaxProgresstime == 0 ? 0 : (int) (tick % 20);
    }

    private void startStep(int index) {
        ChaosProcessingGroup group = groups[index];
        ChaosChain.Step step = group.currentStep();
        if (step == null) return;
        long voltage = 0;
        for (int i = 0; i < MODULE_SLOTS; i++)
            voltage = Math.max(voltage, ChaosStructureModuleItem.voltage(workbench.getStackInSlot(i), step.map()));
        voltage = Math.min(voltage, getMaxInputVoltage());
        // #tr gtnr.chaos.status.module
        // # Missing module or insufficient module voltage
        // # zh_CN 缺少模组或模组电压不足
        if (voltage < step.recipe().mEUt) {
            group.status("gtnr.chaos.status.module");
            return;
        }
        long available = getMaxInputEu();
        for (int i = 0; i < GROUP_COUNT; i++)
            if (connected(i) && groups[i].enabled()) available -= Math.min(available, groups[i].eut());
        // #tr gtnr.chaos.status.power
        // # Waiting for power budget
        // # zh_CN 等待可用功率
        if (available < step.recipe().mEUt) {
            group.status("gtnr.chaos.status.power");
            return;
        }
        ArrayList<MTEHatchInputBus> allBuses = mInputBusses;
        ArrayList<MTEHatchInput> allFluids = mInputHatches;
        mInputBusses = new ArrayList<>(
            allBuses.stream()
                .filter(h -> dedicatedInputs.getOrDefault(h, index) == index)
                .toList());
        mInputHatches = new ArrayList<>(
            allFluids.stream()
                .filter(h -> dedicatedInputs.getOrDefault(h, index) == index)
                .toList());
        StepLogic logic = new StepLogic(step.recipe());
        boolean ended = false;
        try {
            startRecipeProcessing();
            ChaosBuffer.View cache = group.cache()
                .view();
            List<ItemStack> items = new ArrayList<>(Arrays.asList(cache.items));
            items.addAll(unique(getStoredInputs()));
            List<FluidStack> fluids = new ArrayList<>(Arrays.asList(cache.fluids));
            fluids.addAll(unique(getStoredFluids()));
            logic.setMachine(this)
                .setRecipeMap(step.map())
                .setInputItems(items)
                .setInputFluids(fluids)
                .setAvailableVoltage(Math.min(voltage, available))
                .setAvailableAmperage(1)
                .setAmperageOC(false)
                .setMaxParallel(1)
                .setVoidProtection(false, false);
            logic.prepareRecipe(available);
            CheckRecipeResult result = logic.process();
            // #tr gtnr.chaos.status.inputs
            // # Missing inputs or input extraction failed
            // # zh_CN 缺料或输入抽取失败
            if (!result.wasSuccessful()) {
                group.status("gtnr.chaos.status.inputs");
                return;
            }
            ChaosBuffer after = cache.after(group.cache(), logic);
            ChaosBuffer outputs = new ChaosBuffer();
            outputs.add(logic.getOutputItems(), logic.getOutputFluids());
            ChaosBuffer reserved = after.copy();
            reserved.add(outputs);
            // #tr gtnr.chaos.status.capacity
            // # Group cache capacity exceeded
            // # zh_CN 加工组缓存容量不足
            if (reserved.amount() > capacityPerGroup()) {
                group.status("gtnr.chaos.status.capacity");
                return;
            }
            if (!RecipeThreadInputPreflight.canCommit(this, logic) || !logic.canCommitInputs()) {
                group.status("gtnr.chaos.status.inputs");
                return;
            }
            setCheckRecipeResult(CheckRecipeResultRegistry.SUCCESSFUL);
            if (!logic.commitInputs()) return;
            ended = true;
            endRecipeProcessing();
            // A failed external stocking transaction must never create paid-for outputs.
            if (!getCheckRecipeResult().wasSuccessful()) {
                group.status("gtnr.chaos.status.inputs");
                return;
            }
            group.begin(after, outputs, logic.getCalculatedEut(), Math.max(1, logic.getDuration()));
            updateSlots();
            markDirty();
        } finally {
            try {
                if (!ended) endRecipeProcessing();
            } finally {
                logic.finishRecipe();
                mInputBusses = allBuses;
                mInputHatches = allFluids;
            }
        }
    }

    private static <E> List<E> unique(List<E> source) {
        Set<E> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        return source.stream()
            .filter(value -> value != null && seen.add(value))
            .toList();
    }

    public long capacityPerGroup() {
        return BASE_CAPACITY << Math.max(0, Math.min(20, glassTier - 3));
    }

    public boolean connected(int i) {
        return credentialHatches[i] != null && credentialHatches[i].getBaseMetaTileEntity() != null
            && !credentialHatches[i].getBaseMetaTileEntity()
                .isDead();
    }

    public boolean hasWork() {
        return groups != null && Arrays.stream(groups)
            .anyMatch(group -> group != null && group.busy());
    }

    public boolean groupBusy(int index) {
        return index >= 0 && index < GROUP_COUNT && groups[index].busy();
    }

    public ItemStackHandler workbench() {
        return workbench;
    }

    public ChaosProcessingGroup group(int index) {
        return groups[index];
    }

    public void toggleGroup(int index) {
        if (!getBaseMetaTileEntity().isServerSide() || index < 0 || index >= GROUP_COUNT) return;
        groups[index].setEnabled(!groups[index].enabled());
        markDirty();
    }

    public void exportGroup(int index) {
        if (!getBaseMetaTileEntity().isServerSide() || index < 0 || index >= GROUP_COUNT) return;
        groups[index].requestExport();
        markDirty();
    }

    public String groupText(int index) {
        ChaosProcessingGroup group = groups[index];
        // #tr gtnr.chaos.status.disconnected
        // # Credential hatch disconnected
        // # zh_CN 未连接凭证仓
        return (index + 1) + "  "
            + StatCollector.translateToLocal(
                connected(index) ? group.enabled() ? group.status() : "gtnr.chaos.status.paused"
                    : "gtnr.chaos.status.disconnected")
            + "\n"
            + (group.stepIndex() + 1)
            + " | "
            + group.remaining()
            + " t | "
            + group.cache()
                .amount();
    }

    public String groupDetails(int index) {
        ChaosProcessingGroup group = groups[index];
        // #tr gtnr.chaos.group.capacity
        // # Cache: %s / %s units
        // # zh_CN 缓存：%s / %s 单位
        StringBuilder text = new StringBuilder(
            StatCollector.translateToLocalFormatted(
                "gtnr.chaos.group.capacity",
                group.cache()
                    .amount(),
                capacityPerGroup()));
        try {
            ChaosChain.Step step = group.currentStep();
            if (step != null) {
                text.append('\n')
                    .append(StatCollector.translateToLocal(step.map().unlocalizedName));
                // #tr gtnr.chaos.group.requirements
                // # Required by this step (including non-consumed tools):
                // # zh_CN 本工序所需（含不消耗的工具）：
                text.append('\n')
                    .append(StatCollector.translateToLocal("gtnr.chaos.group.requirements"));
                for (ItemStack item : step.recipe().mInputs) if (item != null) text.append('\n')
                    .append(item.stackSize)
                    .append(" × ")
                    .append(item.getDisplayName());
                for (FluidStack fluid : step.recipe().mFluidInputs) if (fluid != null) text.append('\n')
                    .append(fluid.amount)
                    .append(" L ")
                    .append(fluid.getLocalizedName());
            }
        } catch (RuntimeException failure) {
            text.append('\n')
                .append(StatCollector.translateToLocal("gtnr.chaos.error.recipe"));
        }
        return text.toString();
    }

    public void nextPreset() {
        if (getBaseMetaTileEntity().isServerSide()) {
            preset = (preset + 1) % Math.max(
                1,
                ChaosPresets.all()
                    .size());
            markDirty();
        }
    }

    public String presetName() {
        var presets = ChaosPresets.all();
        return presets.isEmpty() ? StatCollector.translateToLocal("gtnr.chaos.error.recipe")
            : StatCollector.translateToLocal(
                presets.get(Math.floorMod(preset, presets.size()))
                    .name());
    }

    public String feedback() {
        return StatCollector.translateToLocal(feedback);
    }

    public void bindCredential(boolean custom) {
        if (!getBaseMetaTileEntity().isServerSide()) return;
        try {
            if (workbench.getStackInSlot(CREDENTIAL_OUTPUT) != null) return;
            ChaosChain chain;
            if (custom) {
                List<ItemStack> patterns = new ArrayList<>();
                for (int i = PATTERN_START; i < INVENTORY_SIZE; i++) patterns.add(workbench.getStackInSlot(i));
                chain = ChaosPatternCompiler.compile(patterns, getBaseMetaTileEntity().getWorld());
            } else {
                var presets = ChaosPresets.all();
                if (presets.isEmpty()) throw new IllegalArgumentException("gtnr.chaos.error.recipe");
                chain = presets.get(Math.floorMod(preset, presets.size()));
            }
            ItemStack result = ChaosCredentialItem.bind(workbench.getStackInSlot(CREDENTIAL_INPUT), chain);
            workbench.extractItem(CREDENTIAL_INPUT, 1, false);
            workbench.setStackInSlot(CREDENTIAL_OUTPUT, result);
            // #tr gtnr.chaos.status.bound
            // # Completed
            // # zh_CN 已完成
            feedback = "gtnr.chaos.status.bound";
        } catch (RuntimeException failure) {
            feedback = errorKey(failure);
        }
        markDirty();
    }

    public void packageModule() {
        if (!getBaseMetaTileEntity().isServerSide() || workbench.getStackInSlot(MODULE_OUTPUT) != null) return;
        try {
            ItemStack casing = workbench.getStackInSlot(CASING_INPUT);
            if (casing == null || casing.stackSize < 8 || !workbench.isItemValid(CASING_INPUT, casing))
                // #tr gtnr.chaos.error.module
                // # Need an empty module, a supported machine and 8 solid steel casings
                // # zh_CN 需要空模组、支持的机器及 8 个坚固钢机器外壳
                throw new IllegalArgumentException("gtnr.chaos.error.module");
            ItemStack result = ChaosStructureModuleItem
                .pack(workbench.getStackInSlot(MODULE_INPUT), workbench.getStackInSlot(MACHINE_INPUT));
            workbench.extractItem(MODULE_INPUT, 1, false);
            workbench.extractItem(MACHINE_INPUT, 1, false);
            workbench.extractItem(CASING_INPUT, 8, false);
            workbench.setStackInSlot(MODULE_OUTPUT, result);
            feedback = "gtnr.chaos.status.bound";
        } catch (RuntimeException failure) {
            feedback = errorKey(failure);
        }
        markDirty();
    }

    private static String errorKey(RuntimeException failure) {
        String message = failure.getMessage();
        return message != null && message.startsWith("gtnr.chaos.") ? message : "gtnr.chaos.error.recipe";
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(TEXTURE);
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(active ? Textures.BlockIcons.OVERLAY_DTPF_ON : Textures.BlockIcons.OVERLAY_DTPF_OFF)
            .extFacing()
            .build() };
    }

    @Override
    public String getLocalNameKey() {
        return "gtnr.machine.bottle_in_chaos.name";
    }

    @Override
    public String getTooltipPath() {
        return "bottle-in-chaos";
    }

    @Override
    public Map<String, Object> getTooltipVars() {
        return Map.of("groups", GROUP_COUNT, "modules", MODULE_SLOTS, "steps", ChaosChain.MAX_STEPS);
    }

    @Override
    public int getTooltipRecipeThreads() {
        return GROUP_COUNT;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new AutoMultiblockTooltipBuilder().addAutoTooltip(this)
            .toolTipFinisher();
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new BottleInChaosGui(this);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        NBTTagList list = new NBTTagList();
        for (ChaosProcessingGroup group : groups) list.appendTag(group.save());
        tag.setTag("chaosGroups", list);
        tag.setTag("chaosWorkbench", workbench.serializeNBT());
        tag.setInteger("chaosPreset", preset);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        NBTTagList list = tag.getTagList("chaosGroups", 10);
        for (int i = 0; i < Math.min(GROUP_COUNT, list.tagCount()); i++)
            groups[i] = ChaosProcessingGroup.load(list.getCompoundTagAt(i));
        if (tag.hasKey("chaosWorkbench")) workbench.deserializeNBT(tag.getCompoundTag("chaosWorkbench"));
        preset = tag.getInteger("chaosPreset");
    }

    @Override
    public void setItemNBT(NBTTagCompound tag) {
        super.setItemNBT(tag);
        saveNBTData(tag);
    }

    @Override
    public void stopMachine(ShutDownReason reason) {
        // GT stops the controller. Group-owned inputs, work and outputs remain resumable.
        super.stopMachine(reason);
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

    private static final class StepLogic extends RecipeThreadProcessingLogic {

        private final GTRecipe recipe;

        private StepLogic(GTRecipe recipe) {
            this.recipe = recipe;
        }

        @Override
        protected Stream<GTRecipe> findRecipeMatches(RecipeMap<?> map) {
            return Stream.of(recipe);
        }
    }
}
