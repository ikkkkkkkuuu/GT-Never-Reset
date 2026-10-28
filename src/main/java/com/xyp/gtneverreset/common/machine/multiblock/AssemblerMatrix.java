package com.xyp.gtneverreset.common.machine.multiblock;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableSet;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.common.gui.modularui.AssemblerMatrixGui;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.machine.AssemblerMatrixPatternInventory;
import com.xyp.gtneverreset.utils.machine.AssemblerMatrixPatternState;
import com.xyp.gtneverreset.utils.machine.DireCraftingPatternDetails;
import com.xyp.gtneverreset.utils.structure.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import appeng.api.config.Actionable;
import appeng.api.config.Upgrades;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalCoord;
import appeng.api.util.IConfigManager;
import appeng.core.localization.WailaText;
import appeng.helpers.DualityInterface;
import appeng.helpers.ICustomNameObject;
import appeng.helpers.IInterfaceHost;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.InvOperation;
import appeng.util.item.AEItemStack;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Dyes;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IMEConnectable;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechDeviceInformation;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.tileentities.machines.RecipeCheckReason;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputBusME;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

/**
 * Accepted batches, container returns and products survive interruption and NBT reload.
 *
 * @see AssemblerMatrixPatternState
 */
@IMetaTileEntity.SkipGenerateDescription
@IMetaTileEntity.SkipGenerateName
public class AssemblerMatrix extends MTEExtendedPowerMultiBlockBase<AssemblerMatrix> implements ISurvivalConstructable,
    IInterfaceHost, IGridProxyable, IAEAppEngInventory, IMEConnectable, ICustomNameObject, MultiblockTooltipSource {

    public static final int MODE_INPUT = 0;
    public static final int MODE_OUTPUT = 1;
    public static final int MODE_OPERATING = 2;
    public static final EnumSet<ForgeDirection> ALL_DIRECTIONS = EnumSet
        .complementOf(EnumSet.of(ForgeDirection.UNKNOWN));
    public static final EnumSet<ForgeDirection> NO_DIRECTIONS = EnumSet.noneOf(ForgeDirection.class);
    public static final int PATTERN_CAPACITY = 576;

    public static ItemStack resolveContainerItem(ItemStack stack) {
        final var item = stack.getItem();
        if (item == null) return null;
        if (!item.hasContainerItem(stack)) return null;
        final ItemStack containerItem = item.getContainerItem(stack.copy());
        if (containerItem != null && containerItem.isItemStackDamageable()
            && containerItem.getItemDamage() > containerItem.getMaxDamage()) {
            return null;
        }

        return containerItem;
    }

    /** Returns the localized visibility label shared by controller, GUI and overlay. */
    public static String patternVisibilityKey(boolean visible) {
        // #tr gtnr.compact.interface.show_pattern.enabled
        // # §aShow on Interface Terminal
        // # zh_CN §a在接口终端显示
        if (visible) return "gtnr.compact.interface.show_pattern.enabled";
        // #tr gtnr.compact.interface.show_pattern.disabled
        // # §cHide on Interface Terminal
        // # zh_CN §c在接口终端隐藏
        return "gtnr.compact.interface.show_pattern.disabled";
    }

    public final long mMaxParallelLong = Long.MAX_VALUE;
    public boolean wirelessMode;
    @Getter
    public boolean showPattern = true;
    public long recipesDone;
    public long usedParallel = 0;
    private String customName = "";
    @Getter
    private final AssemblerMatrixPatternInventory inventory = new AssemblerMatrixPatternInventory(this);
    private final AssemblerMatrixPatternState patternState = new AssemblerMatrixPatternState();

    public final int mMaxSlots = PATTERN_CAPACITY;
    public String costingEUText = "0";
    private AENetworkProxy gridProxy;
    private DualityInterface di;
    private final MachineSource source = new MachineSource(this);

    /** Retains pattern notifications until the server has finished formation and the AE node is active. */
    private boolean needsPatternSync = true;

    /** Last observed AE availability; recovery must wake GregTech's event-driven recipe checks as well as AE. */
    private boolean wasNetworkActive;

    public AssemblerMatrix(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public AssemblerMatrix(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new AssemblerMatrix(this.mName);
    }

    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onFirstTick(aBaseMetaTileEntity);
        if (checkStructure(true, getBaseMetaTileEntity())) {
            this.mStartUpCheck = -1;
            this.mUpdate = 200;
        }
        getProxy().onReady();
        needsPatternSync = true;
    }

    /**
     * Publishes patterns after GregTech has committed its structure state. Loading and structure checks can run
     * before AE is ready, so failed notifications remain pending and are retried without rebuilding the patterns.
     *
     * @param tile  controller tile ticking this machine
     * @param timer controller tick counter; notifications are coalesced to at most one per ten ticks
     */
    @Override
    public void onPostTick(IGregTechTileEntity tile, long timer) {
        super.onPostTick(tile, timer);
        if (!tile.isServerSide()) return;
        boolean active = getProxy().isActive();
        if (active != wasNetworkActive) {
            wasNetworkActive = active;
            needsPatternSync = true;
            if (active) scheduleRecipeCheck(RecipeCheckReason.IMMEDIATE);
        }
        // Queued products can outlive the one-shot recipe check (offline AE or full storage). Retry only while
        // actual work is waiting; never turn a manually disabled controller back on.
        if (active && timer % 20 == 0
            && mMaxProgresstime <= 0
            && !patternState.getOutputs()
                .isEmpty()) {
            scheduleRecipeCheck(RecipeCheckReason.IMMEDIATE);
        }
        if (!needsPatternSync || timer % 10 != 0 || !active) return;
        try {
            getProxy().getGrid()
                .postEvent(new MENetworkCraftingPatternChange(this, getProxy().getNode()));
            needsPatternSync = false;
        } catch (GridAccessException ignored) {
            // Keep the pending update while the network is still loading or changing topology.
        }
    }

    @Override
    public void onFacingChange() {
        super.onFacingChange();
        updateValidGridProxySides();
    }

    @Override
    public void onColorChangeServer(byte aColor) {
        updateAE2ProxyColor();
    }

    @Override
    public IStructureDefinition<AssemblerMatrix> getStructureDefinition() {
        return StructureDefinition.<AssemblerMatrix>builder()
            .addShape("main", StructureUtility.transpose(MultiblockStructureLoader.load("assembler_matrix")))
            .addElement(
                'A',
                StructureUtility.ofChain(
                    GTStructureUtility.buildHatchAdder(AssemblerMatrix.class)
                        .casingIndex(getCasingTextureID())
                        .hint(1)
                        .atLeast(HatchElement.InputBus, HatchElement.OutputBus)
                        .build(),
                    StructureUtility.ofBlock(GregTechAPI.sBlockCasings2, 0)))
            .build();
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, 1, 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece("main", stack, 1, 1, 0, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        boolean formed = checkPiece("main", 1, 1, 0, errors);
        getProxy().setValidSides(
            formed ? EnumSet.complementOf(EnumSet.of(ForgeDirection.UNKNOWN)) : EnumSet.noneOf(ForgeDirection.class));
        if (formed) upPatterns();
    }

    /**
     * Returns whether this machine can still accept new dispatch work.
     */
    @Override
    public boolean isBusy() {
        return getBaseMetaTileEntity() == null || !mMachine
            || machineMode != MODE_OPERATING
            || !getBaseMetaTileEntity().isAllowedToWork()
            || !isActive();
    }

    public void upPatterns() {
        patternState.clearPatternData();

        for (var newStack : this.inventory) {
            if (newStack.getItem() instanceof ICraftingPatternItem ic) {
                var pattern = ic.getPatternForItem(
                    newStack,
                    this.getBaseMetaTileEntity()
                        .getWorld());
                if (pattern == null) continue;
                if (pattern.isCraftable()) {
                    pattern = new DireCraftingPatternDetails(pattern);
                }
                if (pattern instanceof DireCraftingPatternDetails d) {
                    d.setMultiply(getPatternMultiply());
                    patternState.addPattern(newStack, d);
                }
            }
        }
        needsPatternSync = true;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic().enablePerfectOverclock()
            .setMaxParallelSupplier(this::getTrueParallel);
    }

    @Override
    @NotNull
    public CheckRecipeResult checkProcessing() {
        if (machineMode < 2) {
            if (machineMode == MODE_INPUT && inventory.size() < mMaxSlots) {
                List<ItemStack> storedInputs = getStoredInputs();
                boolean updated = false;

                for (ItemStack input : storedInputs) {
                    if (!(input.getItem() instanceof ICraftingPatternItem i)) continue;
                    int slot = inventory.getFirstEmptySlot();
                    if (slot == -1) continue;
                    var p = i.getPatternForItem(
                        input,
                        this.getBaseMetaTileEntity()
                            .getWorld());
                    if (p == null) continue;
                    if (p.isCraftable()) {
                        p = new DireCraftingPatternDetails(p);
                    }
                    if (!(p instanceof DireCraftingPatternDetails d)) continue;
                    ItemStack pattern = input.copy();
                    pattern.stackSize = 1;
                    inventory.setInventorySlotContents(slot, pattern);
                    d.setMultiply(getPatternMultiply());
                    patternState.addPattern(pattern, d);
                    input.stackSize--;
                    updated = true;
                    if (inventory.size() >= mMaxSlots) break;
                }
                if (updated) {
                    needsPatternSync = true;
                }
                updateSlots();
            } else if (machineMode == MODE_OUTPUT && !inventory.isEmpty()) {
                tryOutputInventory(inventory);
            } else {
                return CheckRecipeResultRegistry.NO_RECIPE;
            }
            mMaxProgresstime = 10;
            mEfficiency = 10000;
            mEfficiencyIncrease = 10000;
            lEUt = 0;
            return CheckRecipeResultRegistry.SUCCESSFUL;
        } else if (isActive() && machineMode == MODE_OPERATING) {
            if (mMaxSlots > 0 && !patternState.getOutputs()
                .isEmpty()) {
                costingEUText = "0";
                long parallel = mMaxParallelLong;

                int maximum = patternState.getOutputs()
                    .size();
                usedParallel = 0L;

                if (!patternState.getInputs()
                    .isEmpty()) {
                    var grid = getProxy().getNode()
                        .getGrid();

                    IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
                    var storage = storageGrid.getItemInventory();
                    final var s = patternState.getInputs()
                        .size();
                    for (int i = 0; i < s; i++) {
                        var in = patternState.getInputs()
                            .poll();
                        if (in == null) continue;
                        var leftover = storage.injectItems(in, Actionable.MODULATE, source);
                        if (leftover != null) patternState.getInputs()
                            .add(leftover);
                    }
                }

                List<IAEItemStack> preparedOutputs = new ObjectArrayList<>(maximum);

                IAEItemStack stack;
                while (parallel > 0 && (stack = patternState.getOutputs()
                    .poll()) != null) {
                    long stackSize = stack.getStackSize();
                    if (stackSize <= parallel) {
                        parallel -= stackSize;
                        usedParallel += stackSize;

                        preparedOutputs.add(stack);
                    } else {
                        long remain = stackSize - parallel;
                        usedParallel += parallel;
                        stack.decStackSize(parallel);
                        preparedOutputs.add(
                            stack.copy()
                                .setStackSize(parallel));

                        if (remain > 0) {
                            var remainStack = stack.copy();
                            remainStack.setStackSize(remain);
                            patternState.getOutputs()
                                .add(remainStack);
                        }

                        parallel = 0;
                    }

                    if (patternState.getOutputs()
                        .isEmpty() || --maximum == 0) break;
                }

                if (!preparedOutputs.isEmpty()) {
                    this.lEUt = 0;
                    recipesDone += usedParallel;

                    patternState.setCachedOutputItems(preparedOutputs.toArray(new IAEItemStack[0]));
                    this.mEfficiency = 10000;
                    this.mEfficiencyIncrease = 10000;
                    this.mMaxProgresstime = 1;
                    return CheckRecipeResultRegistry.SUCCESSFUL;
                }
            }
        }

        return CheckRecipeResultRegistry.NO_RECIPE;
    }

    @Override
    public void outputAfterRecipe() {
        super.outputAfterRecipe();
        if (patternState.getCachedOutputItems() == null) return;
        for (IAEItemStack stack : patternState.getCachedOutputItems()) {
            if (stack != null) patternState.getOutputs()
                .add(stack);
        }
        patternState.setCachedOutputItems(new IAEItemStack[0]);
        usedParallel = 0;
        if (!getProxy().isActive()) return;
        try {
            var storage = getProxy().getStorage()
                .getItemInventory();
            int count = patternState.getOutputs()
                .size();
            while (count-- > 0) {
                IAEItemStack stack = patternState.getOutputs()
                    .poll();
                IAEItemStack remaining = storage.injectItems(stack, Actionable.MODULATE, source);
                if (remaining != null) patternState.getOutputs()
                    .add(remaining);
            }
        } catch (GridAccessException ignored) {
            // Keep queued products until the network returns.
        }
        markDirty();
    }

    @Override
    public void stopMachine(@NotNull ShutDownReason reason) {
        if (patternState.getCachedOutputItems() != null) {
            for (IAEItemStack stack : patternState.getCachedOutputItems()) {
                if (stack != null) patternState.getOutputs()
                    .add(stack);
            }
        }
        patternState.setCachedOutputItems(new IAEItemStack[0]);
        usedParallel = 0;
        super.stopMachine(reason);
        markDirty();
    }

    /** A grid replacement must refresh both the advertised patterns and any accepted work awaiting processing. */
    @Override
    public void gridChanged() {
        needsPatternSync = true;
        scheduleRecipeCheck(RecipeCheckReason.IMMEDIATE);
    }

    @Override
    public AENetworkProxy getProxy() {
        if (gridProxy == null) {
            var bmte = getBaseMetaTileEntity();
            if (bmte instanceof IGridProxyable) {
                gridProxy = new AENetworkProxy(this, "proxy", GTNRItemList.AssemblerMatrix.get(1), true);
                gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
                gridProxy.setIdlePowerUsage(0);
                updateValidGridProxySides();
                if (bmte.getWorld() != null) {
                    gridProxy.setOwner(
                        bmte.getWorld()
                            .getPlayerEntityByName(bmte.getOwnerName()));
                }
            }
        }
        return gridProxy;
    }

    public void updateValidGridProxySides() {
        if (mMachine) {
            getProxy().setValidSides(ALL_DIRECTIONS);
        } else {
            getProxy().setValidSides(NO_DIRECTIONS);
        }
    }

    @Override
    public DualityInterface getInterfaceDuality() {
        if (di == null) {
            di = new DualityInterface(this.getProxy(), this);
        }
        return di;
    }

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkChannelsChanged c) {
        needsPatternSync = true;
        this.getInterfaceDuality()
            .notifyNeighbors();
    }

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkPowerStatusChange c) {
        needsPatternSync = true;
        this.getInterfaceDuality()
            .notifyNeighbors();
    }

    /**
     * Exposes currently available crafting patterns to the crafting network.
     */
    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        if (mMachine && this.getProxy()
            .isActive()
            && !patternState.getPatterns()
                .isEmpty()) {
            for (var value : patternState.getPatterns()
                .values()) {
                craftingTracker.addCraftingOption(this, value);
            }
        }
    }

    /**
     * Syncs pattern caches and queues a network change notification when the pattern inventory changes.
     */
    @Override
    public void onChangeInventory(IInventory inv, int slot, InvOperation operation, ItemStack removedStack,
        ItemStack newStack) {
        inventory.markDirty();
        if (patternState.onPatternInventoryChanged(this, removedStack, newStack)) {
            needsPatternSync = true;
        }
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
        if (isBusy()) return false;
        boolean accepted = patternState.pushPattern(patternDetails, table);
        if (accepted) {
            markDirty();
            scheduleRecipeCheck(RecipeCheckReason.IMMEDIATE);
        }
        return accepted;
    }

    @Override
    public EnumSet<ForgeDirection> getTargets() {
        return NO_DIRECTIONS;
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(
            getBaseMetaTileEntity().getWorld(),
            getBaseMetaTileEntity().getXCoord(),
            getBaseMetaTileEntity().getYCoord(),
            getBaseMetaTileEntity().getZCoord());
    }

    @Override
    public TileEntity getTileEntity() {
        return (TileEntity) getBaseMetaTileEntity();
    }

    @Override
    public void saveChanges() {
        this.getInterfaceDuality()
            .saveChanges();
    }

    @Override
    public ItemStack getSelfRep() {
        return GTNRItemList.AssemblerMatrix.get(1);
    }

    /**
     * Returns the pattern inventory exposed to the interface terminal.
     */
    @Override
    public IInventory getPatterns() {
        return inventory;
    }

    @Override
    public int getInstalledUpgrades(Upgrades u) {
        return u == Upgrades.PATTERN_CAPACITY ? mMaxSlots / 9 - 1 : 0;
    }

    @Override
    public TileEntity getTile() {
        return getTileEntity();
    }

    @Override
    public IInventory getInventoryByName(String name) {
        if (name.equals("patterns")) {
            return this.inventory;
        }
        return this.getInterfaceDuality()
            .getInventoryByName(name);
    }

    @Override
    public IGridNode getGridNode(ForgeDirection dir) {
        return this.getProxy()
            .getNode();
    }

    public void updateAE2ProxyColor() {
        AENetworkProxy proxy = getProxy();
        byte color = this.getColor();
        if (color == -1) {
            proxy.setColor(AEColor.Transparent);
        } else {
            proxy.setColor(AEColor.values()[Dyes.transformDyeIndex(color)]);
        }
        if (proxy.getNode() != null) {
            proxy.getNode()
                .updateState();
        }
    }

    @Override
    public void securityBreak() {}

    @Override
    public ItemStack getCrafterIcon() {
        return GTNRItemList.AssemblerMatrix.get(1);
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.getInterfaceDuality()
            .getRequestedJobs();
    }

    @Override
    public IAEStack<?> injectCraftedItems(ICraftingLink link, IAEStack<?> items, Actionable mode) {
        return this.getInterfaceDuality()
            .injectCraftedItems(link, items, mode);
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        this.getInterfaceDuality()
            .jobStateChange(link);
    }

    @Override
    public IGridNode getActionableNode() {
        AENetworkProxy gp = getProxy();
        return gp != null ? gp.getNode() : null;
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection forgeDirection) {
        return AECableType.SMART;
    }

    @Override
    public boolean connectsToAllSides() {
        return true;
    }

    @Override
    public void setConnectsToAllSides(boolean connects) {}

    @Override
    public IConfigManager getConfigManager() {
        return this.getInterfaceDuality()
            .getConfigManager();
    }

    public Set<IAEItemStack> getPossibleOutputs() {
        return patternState.getPossibleOutputs();
    }

    public void tryOutputInventory(IInventory inventory) {
        int emptySlots = 0;
        boolean ignoreEmptiness = false;

        for (MTEHatchOutputBus outputBus : mOutputBusses) {
            if (outputBus instanceof MTEHatchOutputBusME) {
                ignoreEmptiness = true;
                break;
            }
            for (int j = 0; j < outputBus.getSizeInventory(); j++) {
                if (outputBus.isValidSlot(j) && outputBus.getStackInSlot(j) == null) {
                    emptySlots++;
                }
            }
        }

        if (emptySlots == 0 && !ignoreEmptiness) return;

        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null) continue;

            if (!ignoreEmptiness && emptySlots < 1) break;

            ItemStack remaining = stack.copy();
            addOutputPartial(remaining);

            emptySlots--;

            if (remaining.stackSize <= 0) inventory.setInventorySlotContents(slot, null);
        }

        needsPatternSync = true;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        return side == facing
            ? new ITexture[] { Textures.BlockIcons.getCasingTextureForId(getCasingTextureID()),
                TextureFactory.of(
                    active ? Textures.BlockIcons.OVERLAY_ME_INPUT_HATCH_ACTIVE
                        : Textures.BlockIcons.OVERLAY_ME_INPUT_HATCH) }
            : new ITexture[] { Textures.BlockIcons.getCasingTextureForId(getCasingTextureID()) };
    }

    public int getCasingTextureID() {
        return GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings2, 0);
    }

    @Override
    public String getLocalNameKey() {
        return "gtnr.AssemblerMatrix.name";
    }

    @Override
    public MultiblockTooltipBuilder createTooltip() {
        return new AutoMultiblockTooltipBuilder().addAutoTooltip(this)
            .toolTipFinisher();
    }

    @Override
    public String getTooltipMachineType() {
        // #tr gtnr.AssemblerMatrix.name
        // # Assembler Matrix
        // # zh_CN 装配矩阵
        return StatCollector.translateToLocal("gtnr.AssemblerMatrix.name");
    }

    @Override
    public String getTooltipPath() {
        return "assembler-matrix";
    }

    @Override
    public Map<String, Object> getTooltipVars() {
        return Map.of("patternCapacity", PATTERN_CAPACITY);
    }

    @Override
    public String[] getInfoData() {
        List<String> info = new ObjectArrayList<>(super.getInfoData());
        info.add(
            IGregTechDeviceInformation.encode("kubatech.infodata.running_mode") + " "
                + EnumChatFormatting.GOLD
                + (machineMode == 0 ? IGregTechDeviceInformation.encode("kubatech.infodata.mia.running_mode.input")
                    : (machineMode == 1 ? IGregTechDeviceInformation.encode("kubatech.infodata.mia.running_mode.output")
                        : IGregTechDeviceInformation.encode("kubatech.infodata.mia.running_mode.operating.normal"))));
        info.add(
            IGregTechDeviceInformation.encode(
                // #tr gtnr.compact.machine.assembler_matrix.info.0
                // # §7Pattern storage: %s/%s
                // # zh_CN §7当前样板数量：%s/%s
                "gtnr.compact.machine.assembler_matrix.info.0",
                "" + EnumChatFormatting.GOLD + inventory.size() + EnumChatFormatting.RESET,
                (inventory.size() > mMaxSlots ? EnumChatFormatting.DARK_RED.toString()
                    : EnumChatFormatting.GOLD.toString()) + mMaxSlots + EnumChatFormatting.RESET));
        info.add(IGregTechDeviceInformation.encode(patternVisibilityKey(showPattern)));
        info.add(
            IGregTechDeviceInformation.encode("GT5U.multiblock.recipesDone") + ": "
                + EnumChatFormatting.GREEN
                + NumberFormatUtil.formatNumber(recipesDone)
                + EnumChatFormatting.RESET);
        if (wirelessMode) {
            // #tr gtnr.compact.waila.wireless.mode
            // # Wireless Mode
            // # zh_CN 无线模式
            info.add(
                EnumChatFormatting.LIGHT_PURPLE
                    + IGregTechDeviceInformation.encode("gtnr.compact.waila.wireless.mode"));
            info.add(
                // #tr gtnr.compact.waila.wireless.current_eu_cost
                // # Current EU Consumption
                // # zh_CN 当前EU消耗
                EnumChatFormatting.AQUA
                    + IGregTechDeviceInformation.encode("gtnr.compact.waila.wireless.current_eu_cost")
                    + EnumChatFormatting.RESET
                    + ": "
                    + EnumChatFormatting.GOLD
                    + costingEUText
                    + EnumChatFormatting.RESET
                    + " EU");
        }
        return info.toArray(new String[0]);
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currentTip, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();
        boolean isActive = tag.getBoolean("isAEActive");
        boolean isPowered = tag.getBoolean("isAEPowered");
        boolean showPattern = tag.getBoolean("showPattern");
        currentTip.add(WailaText.getPowerState(isActive, isPowered, false));
        if (tag.getLong("maxParallelLong") > 1) {
            currentTip.add(
                StatCollector.translateToLocal("GT5U.multiblock.parallelism") + " (Long): "
                    + EnumChatFormatting.WHITE
                    + tag.getLong("maxParallelLong"));
        }
        currentTip.add(StatCollector.translateToLocal(patternVisibilityKey(showPattern)));
        if (tag.getBoolean("wirelessMode")) {
            currentTip
                // #tr gtnr.compact.waila.wireless.mode
                // # Wireless Mode
                // # zh_CN 无线模式
                .add(
                    EnumChatFormatting.LIGHT_PURPLE
                        + StatCollector.translateToLocal("gtnr.compact.waila.wireless.mode"));
            currentTip.add(
                // #tr gtnr.compact.waila.wireless.current_eu_cost
                // # Current EU Consumption
                // # zh_CN 当前EU消耗
                EnumChatFormatting.AQUA + StatCollector.translateToLocal("gtnr.compact.waila.wireless.current_eu_cost")
                    + EnumChatFormatting.RESET
                    + ": "
                    + EnumChatFormatting.GOLD
                    + tag.getString("costingEUText")
                    + EnumChatFormatting.RESET
                    + " EU");
        }
    }

    @Override
    public String getCustomName() {
        return hasCustomName() ? customName : getMachineCraftingIcon().getDisplayName();
    }

    @Override
    public boolean hasCustomName() {
        return customName != null && !this.customName.isEmpty();
    }

    @Override
    public void setCustomName(String name) {
        customName = name;
        markDirty();
    }

    @Override
    public String getName() {
        if (hasCustomName()) {
            return customName;
        }

        return getCrafterIcon() != null ? getCrafterIcon().getDisplayName() : getLocalName();
    }

    /**
     * Supplies the terminal icon with the matrix name. AE2 also uses this stack's display name when the raw name
     * has no translation, which is normally the case for player-entered names.
     *
     * @return a display stack carrying the custom name, or the default matrix stack when unnamed
     * @see appeng.api.util.IInterfaceViewable#getDisplayRep()
     */
    @Override
    public ItemStack getDisplayRep() {
        ItemStack display = getSelfRep();
        if (display != null && hasCustomName()) {
            display.setStackDisplayName(customName);
        }
        return display;
    }

    @Override
    public boolean onWireCutterRightClick(ForgeDirection side, ForgeDirection wrenchingSide, EntityPlayer aPlayer,
        float aX, float aY, float aZ, ItemStack aTool) {
        if (getBaseMetaTileEntity().isServerSide()) {
            setShowPattern(!showPattern);
            GTUtility.sendChatTrans(aPlayer, patternVisibilityKey(showPattern));
        }
        return true;
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        if (!getBaseMetaTileEntity().isServerSide()) return;
        if (this.mMaxProgresstime > 0) {
            // #tr gtnr.compact.machine.message.mode_running
            // # Can't change mode when running !
            // # zh_CN 机器运行时无法切换模式！
            GTUtility.sendChatTrans(aPlayer, "gtnr.compact.machine.message.mode_running");
            return;
        }
        this.machineMode = (this.machineMode + 1) % 3;
        GTUtility.sendChatTrans(aPlayer, getMachineModeKey());
    }

    public void setPatternMultiply(int patternMultiply) {
        patternState.setPatternMultiply(patternMultiply);
        if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isServerSide()) {
            upPatterns();
            markDirty();
        }
    }

    public int getPatternMultiply() {
        return patternState.getPatternMultiply();
    }

    public List<IAEItemStack> getCachedPatternOutputsForGui() {
        IAEItemStack[] cachedOutputItems = patternState.getCachedOutputItems();
        if (cachedOutputItems == null || cachedOutputItems.length == 0) {
            return Collections.emptyList();
        }
        return ObjectArrayList.wrap(cachedOutputItems);
    }

    public void setCachedPatternOutputsFromGui(List<IAEItemStack> cachedOutputItems) {
        patternState.setCachedOutputItems(cachedOutputItems.toArray(new IAEItemStack[0]));
    }

    public String getGuiCustomName() {
        return hasCustomName() ? customName : "";
    }

    /**
     * Updates the visibility read by the interface terminal and persists the matrix's own setting.
     *
     * @param visible whether the matrix should appear in the interface terminal
     */
    public void setShowPattern(boolean visible) {
        showPattern = visible;
        if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isServerSide()) markDirty();
    }

    @Override
    @Nonnull
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new AssemblerMatrixGui(this);
    }

    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        super.setItemNBT(aNBT);
        saveInvData(aNBT, false);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("patternMultiply", getPatternMultiply());
        aNBT.setLong("mMaxParallelLong", mMaxParallelLong);
        aNBT.setBoolean("wirelessMode", wirelessMode);
        aNBT.setBoolean("showPattern", showPattern);
        aNBT.setLong("recipesDone", recipesDone);
        aNBT.setLong("usedParallel", usedParallel);
        if (customName != null) aNBT.setString("customName", customName);
        getProxy().writeToNBT(aNBT);
        saveInvData(aNBT, false);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void saveInvData(NBTTagCompound aNBT, boolean external) {
        NBTTagCompound storeRoot = new NBTTagCompound();

        NBTTagList cachedList = new NBTTagList();
        if (patternState.getCachedOutputItems() != null) {
            for (IAEItemStack item : patternState.getCachedOutputItems()) {
                if (item != null) {
                    NBTTagCompound tag = new NBTTagCompound();
                    item.writeToNBT(tag);
                    cachedList.appendTag(tag);
                }
            }
        }
        storeRoot.setTag("CACHED_OUTPUT_ITEMS", cachedList);

        NBTTagList outputList = new NBTTagList();
        if (!patternState.getOutputs()
            .isEmpty()) {
            for (IAEItemStack stack : patternState.getOutputs()) {
                if (stack != null) {
                    NBTTagCompound tag = new NBTTagCompound();
                    stack.writeToNBT(tag);
                    outputList.appendTag(tag);
                }
            }
        }
        storeRoot.setTag("OUTPUT_ITEMS", outputList);

        NBTTagList inputList = new NBTTagList();
        if (!patternState.getInputs()
            .isEmpty()) {
            for (IAEItemStack stack : patternState.getInputs()) {
                if (stack != null) {
                    NBTTagCompound tag = new NBTTagCompound();
                    stack.writeToNBT(tag);
                    inputList.appendTag(tag);
                }
            }
        }
        storeRoot.setTag("INPUT_ITEMS", inputList);

        NBTTagCompound invTag = new NBTTagCompound();
        inventory.saveNBTData(invTag);
        storeRoot.setTag("INVENTORY", invTag);

        aNBT.setTag("CrafterInv", storeRoot);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        needsPatternSync = true;
        patternState.clearRuntimeData();
        setPatternMultiply(aNBT.getInteger("patternMultiply"));
        usedParallel = aNBT.getLong("usedParallel");
        wirelessMode = false;
        if (aNBT.hasKey("showPattern")) setShowPattern(aNBT.getBoolean("showPattern"));
        recipesDone = aNBT.getLong("recipesDone");
        if (aNBT.hasKey("customName")) customName = aNBT.getString("customName");

        NBTTagCompound storeRoot = null;

        if (storeRoot == null && aNBT.hasKey("CrafterInv")) {
            storeRoot = aNBT.getCompoundTag("CrafterInv");
        }

        if (storeRoot != null) {
            NBTTagList cachedList = storeRoot.getTagList("CACHED_OUTPUT_ITEMS", 10);
            if (cachedList != null && cachedList.tagCount() > 0) {
                IAEItemStack[] cachedOutputItems = new IAEItemStack[cachedList.tagCount()];
                for (int i = 0; i < cachedList.tagCount(); i++) {
                    cachedOutputItems[i] = AEItemStack.loadItemStackFromNBT(cachedList.getCompoundTagAt(i));
                }
                patternState.setCachedOutputItems(cachedOutputItems);
            }

            NBTTagList outputList = storeRoot.getTagList("OUTPUT_ITEMS", 10);
            if (outputList != null && outputList.tagCount() > 0) {
                for (int i = 0; i < outputList.tagCount(); i++) {
                    IAEItemStack aeStack = AEItemStack.loadItemStackFromNBT(outputList.getCompoundTagAt(i));
                    if (aeStack != null) patternState.getOutputs()
                        .add(aeStack);
                }
            }

            NBTTagList inputList = storeRoot.getTagList("INPUT_ITEMS", 10);
            if (inputList != null && inputList.tagCount() > 0) {
                for (int i = 0; i < inputList.tagCount(); i++) {
                    IAEItemStack aeStack = AEItemStack.loadItemStackFromNBT(inputList.getCompoundTagAt(i));
                    if (aeStack != null) patternState.getInputs()
                        .add(aeStack);
                }
            }

            if (storeRoot.hasKey("INVENTORY")) {
                inventory.loadNBTData(storeRoot.getCompoundTag("INVENTORY"));
            }
        }

        getProxy().readFromNBT(aNBT);
        updateAE2ProxyColor();
        updateValidGridProxySides();
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        boolean isActive = isActive();
        boolean isPowered = isPowered();
        tag.setBoolean("isAEActive", isActive);
        tag.setBoolean("isAEPowered", isPowered);
        tag.setLong("maxParallelLong", mMaxParallelLong);
        tag.setBoolean("wirelessMode", wirelessMode);
        tag.setBoolean("showPattern", showPattern);
        if (wirelessMode) tag.setString("costingEUText", costingEUText);
    }

    @Override
    public int getMaxParallelRecipes() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public boolean supportsVoidProtection() {
        return false;
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
    public boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public int nextMachineMode() {
        if (machineMode == MODE_INPUT) return MODE_OUTPUT;
        else if (machineMode == MODE_OUTPUT) return MODE_OPERATING;
        else return MODE_INPUT;
    }

    @Override
    public String getMachineModeKey() {
        return switch (machineMode) {
            // #tr gtnr.compact.machine.assembler_matrix.mode.input
            // # Input Mode
            // # zh_CN 输入模式
            case MODE_INPUT -> "gtnr.compact.machine.assembler_matrix.mode.input";
            // #tr gtnr.compact.machine.assembler_matrix.mode.output
            // # Output Mode
            // # zh_CN 输出模式
            case MODE_OUTPUT -> "gtnr.compact.machine.assembler_matrix.mode.output";
            // #tr gtnr.compact.machine.assembler_matrix.mode.operating
            // # Operating Mode
            // # zh_CN 运行模式
            default -> "gtnr.compact.machine.assembler_matrix.mode.operating";
        };
    }

    public boolean isPowered() {
        return getProxy() != null && getProxy().isPowered();
    }

    public boolean isActive() {
        return getProxy() != null && getProxy().isActive();
    }

    /**
     * Returns whether this machine should be visible in the interface terminal.
     */
    @Override
    public boolean shouldDisplay() {
        return showPattern;
    }

    @Override
    public boolean allowsPatternOptimization() {
        return false;
    }

    @Override
    public int rows() {
        return inventory.getTerminalRows();
    }

    @Override
    public int rowSize() {
        return 9;
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public boolean shouldCheckMaintenance() {
        return false;
    }

    @Override
    public void checkMaintenance() {}

}
