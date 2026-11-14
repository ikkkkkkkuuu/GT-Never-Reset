package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.api.IItemVault;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.SingularityDataHubGui;
import com.xyp.gtneverreset.common.machine.hatch.VaultPortHatch;
import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipHooks;
import com.xyp.gtneverreset.common.me.vault.VaultStorageUsage;
import com.xyp.gtneverreset.loader.BlockLoader;
import com.xyp.gtneverreset.utils.enums.BlockIcons;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import lombok.Setter;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

@IMetaTileEntity.SkipGenerateDescription
public class SingularityDataHub extends MTEExtendedPowerMultiBlockBase<SingularityDataHub>
    implements ISurvivalConstructable, IItemVault, MultiblockTooltipSource {

    public static long maxDistinctItems = Long.MAX_VALUE - 1;
    public static long maxDistinctFluids = Long.MAX_VALUE - 1;
    public static final long MAX_STORAGE_BYTES = 5_764_607_523_034_234_870L;
    private static final int BYTES_PER_TYPE = 8;
    private static final int ITEMS_PER_BYTE = 8;
    private static final int FLUID_MB_PER_BYTE = 8 * 256;
    public static BigInteger maxItemCapacity = BigInteger.valueOf(MAX_STORAGE_BYTES)
        .multiply(BigInteger.valueOf(ITEMS_PER_BYTE));
    public static BigInteger maxFluidCapacity = BigInteger.valueOf(MAX_STORAGE_BYTES)
        .multiply(BigInteger.valueOf(FLUID_MB_PER_BYTE));
    private static final String STRUCTURE_NAME = "singularity_data_hub";
    private static final String STRUCTURE_PIECE = "main";
    private static final int HORIZONTAL_OFFSET = 1;
    private static final int VERTICAL_OFFSET = 1;
    private static final int DEPTH_OFFSET = 0;
    public static NumberFormat nf = NumberFormat.getNumberInstance();

    /**
     * Rebuilds AE's mutable item structureDefinition instead of sharing it through {@link IAEItemStack#copy()}.
     * Crafting flags do not describe stored inventory and are intentionally omitted.
     *
     * @param stack stored item identity and quantity
     * @return independent item structureDefinition with the original long quantity
     */
    private static IAEItemStack snapshotStoredItem(IAEItemStack stack) {
        return AEItemStack.create(stack.getItemStack())
            .setStackSize(stack.getStackSize());
    }

    public boolean wirelessMode = false;
    public boolean locked = true;
    @Setter
    public boolean doVoidExcess = false;
    private IItemList<IAEItemStack> storedItems = AEApi.instance()
        .storage()
        .createItemList();
    private IItemList<IAEFluidStack> storedFluids = AEApi.instance()
        .storage()
        .createFluidList();

    public long capacityPerItem = Long.MAX_VALUE;
    public long capacityPerFluid = Long.MAX_VALUE;
    public VaultPortHatch portHatch = null;
    public UUID ownerUUID;
    public int mCountCasing = 0;
    private IStructureDefinition<SingularityDataHub> structureDefinition;
    private final VaultStorageUsage storageUsage = new VaultStorageUsage(MAX_STORAGE_BYTES, BYTES_PER_TYPE);

    public SingularityDataHub(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public SingularityDataHub(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new SingularityDataHub(super.mName);
    }

    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        if (aBaseMetaTileEntity.isServerSide() && checkStructure(true, aBaseMetaTileEntity)) {
            this.mStartUpCheck = -1;
            this.mUpdate = 50;
        }
        this.ownerUUID = aBaseMetaTileEntity.getOwnerUuid();
        super.onFirstTick(aBaseMetaTileEntity);
    }

    @Override
    public void onBlockDestroyed() {
        locked = true;
        if (portHatch != null) {
            portHatch.updateCraftingIcon(null, getBaseMetaTileEntity());
            portHatch.unbind();
        }
        super.onBlockDestroyed();
    }

    @Override
    public void onUnload() {
        locked = true;
        if (portHatch != null) portHatch.unbind();
        super.onUnload();
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide()) {
            this.locked = !aBaseMetaTileEntity.isActive();
        }
    }

    @Override
    public IStructureDefinition<SingularityDataHub> getStructureDefinition() {
        if (structureDefinition != null) return structureDefinition;
        structureDefinition = StructureDefinition.<SingularityDataHub>builder()
            .addShape(STRUCTURE_PIECE, transpose(MultiblockStructureLoader.load(STRUCTURE_NAME)))
            .addElement(
                'A',
                ofChain(
                    StructureTooltipHooks.describeAsHatch(
                        buildHatchAdder(SingularityDataHub.class).hatchClass(VaultPortHatch.class)
                            .shouldReject(t -> t.portHatch != null)
                            .adder(SingularityDataHub::addPortBusToMachineList)
                            .casingIndex(16)
                            .hint(1)
                            .build(),
                        "gtnr.hatch.vault_port.name"),
                    onElementPass(t -> t.mCountCasing++, ofBlock(BlockLoader.eddingtonWallCasing, 0))))
            .build();
        return structureDefinition;
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {

        mCountCasing = 0;

        if (!checkPiece(STRUCTURE_PIECE, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET, errors)) {
            return;
        }

        collectTooltipStructureRequirements(errors);
        if (errors.isEmpty()) setupParameters();
    }

    public void setupParameters() {
        wirelessMode = mEnergyHatches.isEmpty() && mExoticEnergyHatches.isEmpty();
        if (portHatch != null && portHatch.controller == null) portHatch.bind(this);
    }

    @Override
    public void collectTooltipStructureRequirements(List<StructureError> errors) {
        if (!StructureTooltipHooks.onCheckCasingMin(this, 1)) checkCasingMin(errors, mCountCasing, 1);

        if (!StructureTooltipHooks.onCheckHatchKeys(this, 1, 1, "gtnr.hatch.vault_port.name") && portHatch == null) {
            // #tr gtnr.structure_error.need_vault_port_hatch
            // # Vault Port Hatch is required
            // # zh_CN 需要仓库端口仓
            errors.add(StructureErrors.of("gtnr.structure_error.need_vault_port_hatch"));
        }
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        if (StructureTooltipHooks.onBuildPiece(this, STRUCTURE_PIECE, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET))
            return;
        buildPiece(STRUCTURE_PIECE, stackSize, hintsOnly, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(
            STRUCTURE_PIECE,
            stackSize,
            HORIZONTAL_OFFSET,
            VERTICAL_OFFSET,
            DEPTH_OFFSET,
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic().enablePerfectOverclock()
            .setMaxParallelSupplier(this::getTrueParallel);
    }

    @Override
    public @NotNull CheckRecipeResult checkProcessing() {
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
        mEUt = 0;
        lEUt = 0;
        mMaxProgresstime = 20;

        if (locked) return CheckRecipeResultRegistry.SUCCESSFUL;

        ArrayList<ItemStack> inputItems = getStoredInputs();
        ArrayList<FluidStack> inputFluids = getStoredFluids();

        if (!inputItems.isEmpty()) {
            for (ItemStack aItem : inputItems) {
                ItemStack toDeplete = aItem.copy();
                toDeplete.stackSize = injectItems(aItem, false);
                if (toDeplete.stackSize > 0 && depleteInput(toDeplete)) injectItems(toDeplete, true);
            }
        }

        if (!inputFluids.isEmpty()) {
            for (FluidStack aFluid : inputFluids) {
                FluidStack toDeplete = aFluid.copy();
                toDeplete.amount = injectFluids(aFluid, false);
                if (toDeplete.amount > 0 && depleteInput(toDeplete)) injectFluids(toDeplete, true);
            }
        }

        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    public long getUsedStorageBytes() {
        return storageUsage.usedBytes();
    }

    @Override
    public long maxItemCount() {
        return maxDistinctItems;
    }

    @Override
    public long maxFluidCount() {
        return maxDistinctFluids;
    }

    @Override
    public boolean hasItem() {
        return true;
    }

    @Override
    public boolean hasFluid() {
        return true;
    }

    @Override
    public ArrayList<ItemStack> getStoredInputsForColor(Optional<Byte> color) {
        ArrayList<ItemStack> rList = new ArrayList<>();
        Map<ItemId, ItemStack> inputsFromME = new HashMap<>();
        for (MTEHatchInputBus tHatch : GTUtility.validMTEList(mInputBusses)) {
            if (tHatch instanceof MTEHatchCraftingInputME) {
                continue;
            }
            byte busColor = tHatch.getColor();
            if (color.isPresent() && busColor != -1 && busColor != color.get()) continue;
            tHatch.mRecipeMap = getRecipeMap();
            IGregTechTileEntity tileEntity = tHatch.getBaseMetaTileEntity();
            boolean isMEBus = tHatch instanceof MTEHatchInputBusME;
            for (int i = tileEntity.getSizeInventory() - 1; i >= 0; i--) {
                ItemStack itemStack = tileEntity.getStackInSlot(i);
                if (itemStack != null) {
                    if (isMEBus) {
                        // Prevent the same item from different ME buses from being recognized
                        inputsFromME.put(ItemId.createNoCopy(itemStack), itemStack);
                    } else {
                        rList.add(itemStack);
                    }
                }
            }
        }

        if (!inputsFromME.isEmpty()) {
            rList.addAll(inputsFromME.values());
        }
        return rList;

    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        wirelessMode = false;
        locked = true;
        if (portHatch != null) {
            portHatch.unbind();
            portHatch = null;
        }
    }

    private void rebuildStorageUsage() {
        storageUsage.clear();
        for (IAEItemStack item : storedItems) storageUsage.update(0, item.getStackSize(), ITEMS_PER_BYTE);
        for (IAEFluidStack fluid : storedFluids) storageUsage.update(0, fluid.getStackSize(), FLUID_MB_PER_BYTE);
    }

    @Override
    public int injectItems(ItemStack aItem, boolean doInput) {
        if (locked || aItem == null || aItem.stackSize <= 0) return 0;
        var aeItem = findStoredItem(aItem);
        long size = aeItem == null ? 0 : aeItem.getStackSize();
        long accepted = storageUsage.insertableAmount(size, aItem.stackSize, ITEMS_PER_BYTE);
        if (accepted <= 0) return doVoidExcess ? aItem.stackSize : 0;
        if (doInput) {
            if (aeItem == null) {
                storedItems.addStorage(
                    AEItemStack.create(aItem)
                        .setStackSize(accepted));
            } else {
                aeItem.setStackSize(size + accepted);
            }
            storageUsage.update(size, size + accepted, ITEMS_PER_BYTE);
            markDirty();
            if (portHatch != null) portHatch.postUpdateItem(aItem, accepted);
        }
        return doVoidExcess ? aItem.stackSize : (int) accepted;
    }

    @Override
    public long injectItems(IAEItemStack aItem, boolean doInput) {
        if (locked || aItem == null || aItem.getStackSize() <= 0) return 0;
        var aeItem = findStoredItem(aItem.getItemStack());
        long size = aeItem == null ? 0 : aeItem.getStackSize();
        long accepted = storageUsage.insertableAmount(size, aItem.getStackSize(), ITEMS_PER_BYTE);
        if (accepted <= 0) return doVoidExcess ? aItem.getStackSize() : 0;
        if (doInput) {
            if (aeItem == null) {
                storedItems.addStorage(snapshotStoredItem(aItem).setStackSize(accepted));
            } else {
                aeItem.setStackSize(size + accepted);
            }
            storageUsage.update(size, size + accepted, ITEMS_PER_BYTE);
            markDirty();
            if (portHatch != null) portHatch.postUpdateItem(aItem.getItemStack(), accepted);
        }
        return doVoidExcess ? aItem.getStackSize() : accepted;
    }

    @Override
    public int injectFluids(FluidStack aFluid, boolean doInput) {
        if (locked || aFluid == null || aFluid.amount <= 0) return 0;
        var aeFluid = findStoredFluid(aFluid);
        long size = aeFluid == null ? 0 : aeFluid.getStackSize();
        long accepted = storageUsage.insertableAmount(size, aFluid.amount, FLUID_MB_PER_BYTE);
        if (accepted <= 0) return doVoidExcess ? aFluid.amount : 0;
        if (doInput) {
            if (aeFluid == null) {
                storedFluids.addStorage(
                    AEFluidStack.create(aFluid)
                        .setStackSize(accepted));
            } else {
                aeFluid.setStackSize(size + accepted);
            }
            storageUsage.update(size, size + accepted, FLUID_MB_PER_BYTE);
            markDirty();
            if (portHatch != null) portHatch.postUpdateFluid(aFluid, accepted);
        }
        return doVoidExcess ? aFluid.amount : (int) accepted;
    }

    @Override
    public long injectFluids(IAEFluidStack aFluid, boolean doInput) {
        if (locked || aFluid == null || aFluid.getStackSize() <= 0) return 0;
        var aeFluid = findStoredFluid(aFluid.getFluidStack());
        long size = aeFluid == null ? 0 : aeFluid.getStackSize();
        long accepted = storageUsage.insertableAmount(size, aFluid.getStackSize(), FLUID_MB_PER_BYTE);
        if (accepted <= 0) return doVoidExcess ? aFluid.getStackSize() : 0;
        if (doInput) {
            if (aeFluid == null) {
                storedFluids.addStorage(
                    aFluid.copy()
                        .setStackSize(accepted));
            } else {
                aeFluid.setStackSize(size + accepted);
            }
            storageUsage.update(size, size + accepted, FLUID_MB_PER_BYTE);
            markDirty();
            if (portHatch != null) portHatch.postUpdateFluid(aFluid.getFluidStack(), accepted);
        }
        return doVoidExcess ? aFluid.getStackSize() : accepted;
    }

    @Override
    public long extractItems(IAEItemStack aItem, boolean doOutput) {
        if (locked || aItem == null || aItem.getStackSize() <= 0) return 0;
        var aeItem = findStoredItem(aItem.getItemStack());
        if (aeItem == null) return 0;
        long storedSize = aeItem.getStackSize();
        long requestSize = aItem.getStackSize();
        if (storedSize > requestSize) {
            if (doOutput) {
                aeItem.setStackSize(storedSize - requestSize);
                storageUsage.update(storedSize, storedSize - requestSize, ITEMS_PER_BYTE);
                markDirty();
                if (portHatch != null) portHatch.postUpdateItem(aItem.getItemStack(), -requestSize);
            }
            return requestSize;
        } else {
            if (doOutput) {
                aeItem.setStackSize(0);
                storageUsage.update(storedSize, 0, ITEMS_PER_BYTE);
                markDirty();
                if (portHatch != null) portHatch.postUpdateItem(aItem.getItemStack(), -storedSize);
            }
            return storedSize;
        }
    }

    @Override
    public long extractFluids(IAEFluidStack aFluid, boolean doOutput) {
        if (locked || aFluid == null || aFluid.getStackSize() <= 0) return 0;
        var aeFluid = findStoredFluid(aFluid.getFluidStack());
        if (aeFluid == null) return 0;
        long storedSize = aeFluid.getStackSize();
        long requestSize = aFluid.getStackSize();
        if (storedSize > requestSize) {
            if (doOutput) {
                aeFluid.setStackSize(storedSize - requestSize);
                storageUsage.update(storedSize, storedSize - requestSize, FLUID_MB_PER_BYTE);
                markDirty();
                if (portHatch != null) portHatch.postUpdateFluid(aFluid.getFluidStack(), -requestSize);
            }
            return requestSize;
        } else {
            if (doOutput) {
                aeFluid.setStackSize(0);
                storageUsage.update(storedSize, 0, FLUID_MB_PER_BYTE);
                markDirty();
                if (portHatch != null) portHatch.postUpdateFluid(aFluid.getFluidStack(), -storedSize);
            }
            return storedSize;
        }
    }

    @Override
    public long itemsCount() {
        long count = 0;
        for (IAEItemStack item : storedItems) if (item.getStackSize() > 0) count++;
        return count;
    }

    @Override
    public long fluidsCount() {
        long count = 0;
        for (IAEFluidStack fluid : storedFluids) if (fluid.getStackSize() > 0) count++;
        return count;
    }

    @Override
    public IAEItemStack getStoredItem(@Nullable ItemStack aItem) {
        IAEItemStack stored = findStoredItem(aItem);
        return stored == null ? null : snapshotStoredItem(stored);
    }

    private IAEItemStack findStoredItem(@Nullable ItemStack aItem) {
        if (aItem == null) return null;
        return storedItems.findPrecise(AEItemStack.create(aItem));
    }

    @Override
    public IAEFluidStack getStoredFluid(@Nullable FluidStack aFluid) {
        IAEFluidStack stored = findStoredFluid(aFluid);
        return stored == null ? null : stored.copy();
    }

    private IAEFluidStack findStoredFluid(@Nullable FluidStack aFluid) {
        if (aFluid == null) return null;
        return storedFluids.findPrecise(AEFluidStack.create(aFluid));
    }

    @Override
    public boolean containsItems(ItemStack aItem) {
        IAEItemStack stored = findStoredItem(aItem);
        return stored != null && stored.getStackSize() > 0;
    }

    @Override
    public boolean containsFluids(FluidStack aFluid) {
        IAEFluidStack stored = findStoredFluid(aFluid);
        return stored != null && stored.getStackSize() > 0;
    }

    public BigInteger getItemStoredAmount() {
        BigInteger amount = BigInteger.ZERO;
        for (IAEItemStack item : storedItems) {
            amount = amount.add(BigInteger.valueOf(item.getStackSize()));
        }
        return amount;
    }

    public BigInteger getFluidStoredAmount() {
        BigInteger amount = BigInteger.ZERO;
        for (IAEFluidStack fluid : storedFluids) {
            amount = amount.add(BigInteger.valueOf(fluid.getStackSize()));
        }
        return amount;
    }

    @Override
    public IItemList<IAEItemStack> getStoreItems() {
        IItemList<IAEItemStack> snapshot = AEApi.instance()
            .storage()
            .createItemList();
        copyItemsTo(snapshot);
        return snapshot;
    }

    @Override
    public IItemList<IAEFluidStack> getStoreFluids() {
        IItemList<IAEFluidStack> snapshot = AEApi.instance()
            .storage()
            .createFluidList();
        copyFluidsTo(snapshot);
        return snapshot;
    }

    @Override
    public void copyItemsTo(IItemList<IAEItemStack> out) {
        for (IAEItemStack item : storedItems) out.addStorage(snapshotStoredItem(item));
    }

    @Override
    public void copyFluidsTo(IItemList<IAEFluidStack> out) {
        for (IAEFluidStack fluid : storedFluids) out.addStorage(fluid.copy());
    }

    public boolean addPortBusToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity != null) {
            final IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
            if (aMetaTileEntity instanceof VaultPortHatch vaultPortHatch) {
                if (portHatch != null) return false;
                vaultPortHatch.updateTexture(aBaseCasingIndex);
                vaultPortHatch.updateCraftingIcon(getMachineCraftingIcon(), getBaseMetaTileEntity());
                portHatch = vaultPortHatch;
                return true;
            }
        }
        return false;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean aActive, boolean aRedstone) {
        if (side != facing) return new ITexture[] { BlockLoader.eddingtonWallCasing.getCasingTexture() };
        return new ITexture[] { BlockLoader.eddingtonWallCasing.getCasingTexture(), TextureFactory.builder()
            .addIcon(aActive ? BlockIcons.OverlayFrontKerrCompressorActive : BlockIcons.OverlayFrontKerrCompressor)
            .extFacing()
            .build(),
            TextureFactory.builder()
                .addIcon(
                    aActive ? BlockIcons.OverlayFrontKerrCompressorActiveGlow
                        : BlockIcons.OverlayFrontKerrCompressorGlow)
                .extFacing()
                .glow()
                .build() };
    }

    @Override
    public String getLocalNameKey() {
        return "gtnr.machine.singularity_data_hub.name";
    }

    @Override
    public String[] getStructureDescription(ItemStack stackSize) {
        return new String[0];
    }

    public MultiblockTooltipBuilder createTooltip() {
        return new AutoMultiblockTooltipBuilder().addAutoTooltip(this)
            .toolTipFinisher();
    }

    @Override
    public String getTooltipMachineType() {
        // #tr gtnr.machine.singularity_data_hub.recipe_type
        // # Vault
        // # zh_CN 保险库
        return StatCollector.translateToLocal("gtnr.machine.singularity_data_hub.recipe_type");
    }

    @Override
    public String getTooltipPath() {
        return "singularity-data-hub";
    }

    @Override
    public String[] getInfoData() {
        ArrayList<String> ll = new ArrayList<>();
        // #tr gtnr.machine.singularity_data_hub.info.stored_items
        // # Stored Items:
        // # zh_CN 已存储物品：
        ll.add(
            EnumChatFormatting.YELLOW
                + StatCollector.translateToLocal("gtnr.machine.singularity_data_hub.info.stored_items")
                + EnumChatFormatting.RESET);

        int i = 0;
        for (IAEItemStack tank : storedItems) {
            String localizedName = Objects.requireNonNull(
                tank.getItem()
                    .getItemStackDisplayName(tank.getItemStack()));
            String amount = nf.format(tank.getStackSize());
            String percentage = capacityPerItem > 0 ? BigInteger.valueOf(tank.getStackSize())
                .multiply(BigInteger.valueOf(100))
                .divide(BigInteger.valueOf(capacityPerItem))
                .toString() : "";
            ll.add(MessageFormat.format("{0} - {1}: {2} ({3}%)", i++, localizedName, amount, percentage));
            if (i >= 32) break;
        }

        // #tr gtnr.machine.singularity_data_hub.info.stored_fluids
        // # Stored Fluids:
        // # zh_CN 已存储流体：
        ll.add(
            EnumChatFormatting.YELLOW
                + StatCollector.translateToLocal("gtnr.machine.singularity_data_hub.info.stored_fluids")
                + EnumChatFormatting.RESET);

        int j = 0;
        for (IAEFluidStack tank : storedFluids) {
            String localizedName = Objects.requireNonNull(
                tank.getFluid()
                    .getLocalizedName(tank.getFluidStack()));
            String amount = nf.format(tank.getStackSize());
            String percentage = capacityPerFluid > 0 ? BigInteger.valueOf(tank.getStackSize())
                .multiply(BigInteger.valueOf(100))
                .divide(BigInteger.valueOf(capacityPerFluid))
                .toString() : "";
            ll.add(MessageFormat.format("{0} - {1}: {2} ({3}%)", j++, localizedName, amount, percentage));
            if (j >= 32) break;
        }

        // #tr gtnr.machine.singularity_data_hub.info.operational_data
        // # Operational Data
        // # zh_CN 运行数据
        ll.add(
            EnumChatFormatting.YELLOW
                + StatCollector.translateToLocal("gtnr.machine.singularity_data_hub.info.operational_data")
                + EnumChatFormatting.RESET);

        // #tr gtnr.machine.singularity_data_hub.info.item_used
        // # Item Used Capacity: %s
        // # zh_CN 已用物品容量: %s
        ll.add(
            StatCollector.translateToLocalFormatted(
                "gtnr.machine.singularity_data_hub.info.item_used",
                nf.format(getItemStoredAmount())));
        // #tr gtnr.machine.singularity_data_hub.info.item_total
        // # Item Total Capacity: %s
        // # zh_CN 物品总容量: %s
        ll.add(
            StatCollector.translateToLocalFormatted(
                "gtnr.machine.singularity_data_hub.info.item_total",
                nf.format(maxItemCapacity)));
        // #tr gtnr.machine.singularity_data_hub.info.per_item_capacity
        // # Per-Item Capacity: %s
        // # zh_CN 每种物品容量: %s
        ll.add(
            StatCollector.translateToLocalFormatted(
                "gtnr.machine.singularity_data_hub.info.per_item_capacity",
                nf.format(capacityPerItem)));
        // #tr gtnr.machine.singularity_data_hub.info.item_used_types
        // # Item Used Type: %s
        // # zh_CN 已用物品种类: %s
        ll.add(
            StatCollector.translateToLocalFormatted(
                "gtnr.machine.singularity_data_hub.info.item_used_types",
                nf.format(itemsCount())));
        // #tr gtnr.machine.singularity_data_hub.info.item_total_types
        // # Item Total Type: %s
        // # zh_CN 物品总种类: %s
        ll.add(
            StatCollector.translateToLocalFormatted(
                "gtnr.machine.singularity_data_hub.info.item_total_types",
                nf.format(maxItemCount())));

        // #tr gtnr.machine.singularity_data_hub.info.fluid_used
        // # Fluid Used Capacity: %s
        // # zh_CN 已用流体容量: %s
        ll.add(
            StatCollector.translateToLocalFormatted(
                "gtnr.machine.singularity_data_hub.info.fluid_used",
                nf.format(getFluidStoredAmount())));
        // #tr gtnr.machine.singularity_data_hub.info.fluid_total
        // # Fluid Total Capacity: %s
        // # zh_CN 流体总容量: %s
        ll.add(
            StatCollector.translateToLocalFormatted(
                "gtnr.machine.singularity_data_hub.info.fluid_total",
                nf.format(maxFluidCapacity)));
        // #tr gtnr.machine.singularity_data_hub.info.per_fluid_capacity
        // # Per-Fluid Capacity: %s
        // # zh_CN 每种流体容量: %s
        ll.add(
            StatCollector.translateToLocalFormatted(
                "gtnr.machine.singularity_data_hub.info.per_fluid_capacity",
                nf.format(capacityPerFluid)));
        // #tr gtnr.machine.singularity_data_hub.info.fluid_used_types
        // # Fluid Used Type: %s
        // # zh_CN 已用流体种类: %s
        ll.add(
            StatCollector.translateToLocalFormatted(
                "gtnr.machine.singularity_data_hub.info.fluid_used_types",
                nf.format(fluidsCount())));
        // #tr gtnr.machine.singularity_data_hub.info.fluid_total_types
        // # Fluid Total Type: %s
        // # zh_CN 流体总种类: %s
        ll.add(
            StatCollector.translateToLocalFormatted(
                "gtnr.machine.singularity_data_hub.info.fluid_total_types",
                nf.format(maxFluidCount())));

        // #tr gtnr.machine.singularity_data_hub.info.running_cost
        // # Running Cost: %dEU/t
        // # zh_CN 运行成本: %dEU/t
        ll.add(
            StatCollector.translateToLocalFormatted(
                "gtnr.machine.singularity_data_hub.info.running_cost",
                getActualEnergyUsage()));
        // #tr gtnr.machine.singularity_data_hub.info.auto_voiding
        // # Auto-voiding: %b
        // # zh_CN 自动销毁溢出: %b
        ll.add(
            StatCollector
                .translateToLocalFormatted("gtnr.machine.singularity_data_hub.info.auto_voiding", doVoidExcess));
        // #tr Waila_WirelessMode
        // # Wireless Mode
        // # zh_CN 无线模式
        if (wirelessMode)
            ll.add(EnumChatFormatting.LIGHT_PURPLE + StatCollector.translateToLocal("Waila_WirelessMode"));
        ll.add(EnumChatFormatting.STRIKETHROUGH + "---------------------------------------------");

        return ll.toArray(new String[0]);
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {}

    @Override
    protected @NotNull MTEMultiBlockBaseGui<?> getGui() {
        return new SingularityDataHubGui(this);
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        if (getBaseMetaTileEntity().isServerSide()) {
            this.setDoVoidExcess(!doVoidExcess);
            markDirty();
            // #tr gtnr.machine.singularity_data_hub.info.auto_voiding
            // # Auto-voiding: %b
            // # zh_CN 自动销毁溢出: %b
            GTUtility.sendChatToPlayer(
                aPlayer,
                StatCollector
                    .translateToLocalFormatted("gtnr.machine.singularity_data_hub.info.auto_voiding", doVoidExcess));
        }
    }

    @Override
    public void setItemNBT(NBTTagCompound tag) {
        super.setItemNBT(tag);
        writeSettings(tag);
        NBTTagCompound contents = writeContents();
        File worldDir = DimensionManager.getCurrentSaveRootDirectory();
        if (worldDir == null) {
            copyContents(contents, tag);
            return;
        }
        File dataDir = new File(worldDir, "data");
        String uuid = UUID.randomUUID()
            .toString();
        File storeFile = new File(dataDir, "ItemVault_" + uuid + ".dat");
        try {
            if (!dataDir.isDirectory() && !dataDir.mkdirs()) {
                throw new IOException("unable to create vault data directory " + dataDir);
            }
            CompressedStreamTools.safeWrite(contents, storeFile);
            tag.setString("storeUUID", uuid);
            tag.removeTag("STORE_ITEM");
            tag.removeTag("STORE_FLUID");
        } catch (IOException exception) {
            GTNeverReset.LOG.error("failed to save dropped vault contents to {}", storeFile, exception);
            copyContents(contents, tag);
            tag.removeTag("storeUUID");
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        writeSettings(tag);
        copyContents(writeContents(), tag);
        tag.removeTag("storeUUID");
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        storedItems = AEApi.instance()
            .storage()
            .createItemList();
        storedFluids = AEApi.instance()
            .storage()
            .createFluidList();
        setDoVoidExcess(tag.getBoolean("doVoidExcess"));
        locked = true;
        wirelessMode = tag.getBoolean("wirelessMode");
        if (tag.hasKey("STORE_ITEM", 9) || tag.hasKey("STORE_FLUID", 9)) {
            readContents(tag);
        } else if (tag.hasKey("storeUUID", 8)) {
            loadDroppedContents(tag.getString("storeUUID"));
        }
        rebuildStorageUsage();
    }

    private void writeSettings(NBTTagCompound tag) {
        tag.setBoolean("wirelessMode", wirelessMode);
        tag.setBoolean("doVoidExcess", doVoidExcess);
        tag.setBoolean("locked", locked);
    }

    private NBTTagCompound writeContents() {
        NBTTagCompound contents = new NBTTagCompound();
        NBTTagList items = new NBTTagList();
        for (IAEItemStack item : storedItems) {
            NBTTagCompound entry = new NBTTagCompound();
            item.writeToNBT(entry);
            items.appendTag(entry);
        }
        NBTTagList fluids = new NBTTagList();
        for (IAEFluidStack fluid : storedFluids) {
            NBTTagCompound entry = new NBTTagCompound();
            fluid.writeToNBT(entry);
            fluids.appendTag(entry);
        }
        contents.setTag("STORE_ITEM", items);
        contents.setTag("STORE_FLUID", fluids);
        return contents;
    }

    private void copyContents(NBTTagCompound contents, NBTTagCompound target) {
        target.setTag("STORE_ITEM", contents.getTag("STORE_ITEM"));
        target.setTag("STORE_FLUID", contents.getTag("STORE_FLUID"));
    }

    private void readContents(NBTTagCompound contents) {
        NBTTagList items = contents.getTagList("STORE_ITEM", 10);
        for (int index = 0; index < items.tagCount(); index++) {
            IAEItemStack item = AEItemStack.loadItemStackFromNBT(items.getCompoundTagAt(index));
            if (item != null && item.getStackSize() > 0) storedItems.addStorage(item);
        }
        NBTTagList fluids = contents.getTagList("STORE_FLUID", 10);
        for (int index = 0; index < fluids.tagCount(); index++) {
            IAEFluidStack fluid = AEFluidStack.loadFluidStackFromNBT(fluids.getCompoundTagAt(index));
            if (fluid != null && fluid.getStackSize() > 0) storedFluids.addStorage(fluid);
        }
    }

    private void loadDroppedContents(String storedUuid) {
        try {
            String uuid = UUID.fromString(storedUuid)
                .toString();
            File worldDir = DimensionManager.getCurrentSaveRootDirectory();
            if (worldDir == null) return;
            File storeFile = new File(new File(worldDir, "data"), "ItemVault_" + uuid + ".dat");
            if (!storeFile.isFile()) return;
            NBTTagCompound contents = CompressedStreamTools.read(storeFile);
            if (contents == null) throw new IOException("empty vault contents file " + storeFile);
            readContents(contents);
            if (!storeFile.delete())
                GTNeverReset.LOG.warn("failed to delete restored vault contents file {}", storeFile);
        } catch (IOException | IllegalArgumentException exception) {
            GTNeverReset.LOG.error("failed to restore dropped vault contents {}", storedUuid, exception);
        }
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {}

    @Override
    public boolean isCorrectMachinePart(ItemStack stack) {
        return true;
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsInputSeparation() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return true;
    }

    @Override
    protected boolean forceUseMui2() {
        return true;
    }

    @Override
    public boolean supportsPowerPanel() {
        return false;
    }

    public long getActualEnergyUsage() {
        return 0;
    }

    @Override
    public void checkMaintenance() {}

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public boolean shouldCheckMaintenance() {
        return false;
    }

    @Override
    public boolean isValid() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return mMachine && base != null && !base.isDead();
    }

    @Override
    public boolean getDoVoidExcess() {
        return doVoidExcess;
    }

}
