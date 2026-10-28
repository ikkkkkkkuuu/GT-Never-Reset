package com.xyp.gtneverreset.common.machine.basicMachine;

import static gregtech.api.enums.GTValues.V;
import static gregtech.api.util.GTRecipeConstants.COMPRESSION_TIER;
import static gregtech.api.util.GTRecipeConstants.EXPLODE;
import static gregtech.api.util.GTRecipeConstants.ON_FIRE;

import java.util.Arrays;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.gtnewhorizons.modularui.common.fluid.FluidStackTank;
import com.xyp.gtneverreset.common.gui.modularui.ModernFluidMachineGui;
import com.xyp.gtneverreset.utils.enums.ModernMachineTextures;
import com.xyp.gtneverreset.utils.machine.stockio.StockIORecipeBridge;

import gregtech.GTMod;
import gregtech.api.enums.SoundResource;
import gregtech.api.interfaces.ICleanroom;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachineWithRecipe;
import gregtech.api.recipe.BasicUIProperties;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

/** A standard tiered single-block machine with independent direct-fluid input and output tanks. */
public final class ModernFluidMachine extends MTEBasicMachineWithRecipe {

    private static final int AUTO_OUTPUT_INTERVAL = 20;
    private static final int AUTO_OUTPUT_AMOUNT = 1000;

    private final FluidStack[] inputFluids;
    private final FluidStack[] outputFluids;
    private final FluidStackTank[] inputTanks;
    private final FluidStackTank[] outputTanks;
    private final FluidStack[] pendingFluids;
    private boolean baseTick;

    public ModernFluidMachine(int id, String name, String regionalName, int tier, RecipeMap<?> recipes, int itemInputs,
        int itemOutputs, int fluidInputs, int fluidOutputs, ModernMachineTextures textures, SoundResource sound) {
        super(
            id,
            name,
            regionalName,
            tier,
            description(),
            recipes,
            itemInputs,
            itemOutputs,
            true,
            true,
            getCapacityForTier(tier),
            sound,
            SpecialEffects.NONE,
            textures.getOverlays(),
            null);
        inputFluids = new FluidStack[fluidInputs];
        outputFluids = new FluidStack[fluidOutputs];
        pendingFluids = new FluidStack[fluidOutputs];
        inputTanks = createTanks(inputFluids, true);
        outputTanks = createTanks(outputFluids, false);
    }

    private ModernFluidMachine(ModernFluidMachine prototype) {
        super(
            prototype.mName,
            prototype.mTier,
            prototype.mDescriptionArray,
            prototype.mRecipes,
            prototype.mInputSlotCount,
            prototype.mOutputItems.length,
            true,
            true,
            prototype.mTankCapacity,
            prototype.mAmperage,
            prototype.mTextures,
            prototype.mSoundResource,
            SpecialEffects.NONE);
        inputFluids = new FluidStack[prototype.inputTanks.length];
        outputFluids = new FluidStack[prototype.outputTanks.length];
        pendingFluids = new FluidStack[outputFluids.length];
        inputTanks = createTanks(inputFluids, true);
        outputTanks = createTanks(outputFluids, false);
        progressBarTexture = prototype.progressBarTexture;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new ModernFluidMachine(this);
    }

    @Override
    public ITexture[][][] getTextureSet(ITexture[] overlays) {
        ITexture[][][] textures = super.getTextureSet(overlays);
        for (ITexture[][] face : textures) {
            for (int color = 0; color < face.length; color++) {
                face[color][0] = ModernMachineTextures.getCasing(mTier, color - 1);
            }
        }
        return textures;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new ModernFluidMachineGui(this).build(data, syncManager, settings);
    }

    public BasicUIProperties getModernUIProperties() {
        return super.getUIProperties().toBuilder()
            .maxFluidInputs(inputTanks.length)
            .maxFluidOutputs(outputTanks.length)
            .build();
    }

    public FluidStackTank[] getInputTanks() {
        return inputTanks;
    }

    public FluidStackTank[] getOutputTanks() {
        return outputTanks;
    }

    @Override
    public int checkRecipe(boolean skipOC) {
        FluidStack[] previousPending = copyFluids(pendingFluids);
        int result = StockIORecipeBridge.checkRecipe(this, () -> checkDirectRecipe(skipOC));
        if (result != FOUND_AND_SUCCESSFULLY_USED_RECIPE) {
            System.arraycopy(previousPending, 0, pendingFluids, 0, pendingFluids.length);
        } else {
            for (int i = 0; i < inputTanks.length; i++) {
                if (inputTanks[i].getFluidAmount() <= 0) setInput(i, null);
            }
        }
        return result;
    }

    private int checkDirectRecipe(boolean skipOC) {
        FluidStack[] fluids = StockIORecipeBridge.fluidInputs(this, inputStacks());
        ItemStack[] items = StockIORecipeBridge.itemInputs(this, getAllInputs());
        GTRecipe recipe = mRecipes.findRecipeQuery()
            .items(items)
            .fluids(fluids)
            .specialSlot(getSpecialSlot())
            .voltage(V[mTier])
            .cachedRecipe(mLastRecipe)
            .find();
        if (recipe == null) return DID_NOT_FIND_RECIPE;
        if (recipe.getMetadataOrDefault(EXPLODE, false)) {
            getBaseMetaTileEntity().doExplosion(V[mTier] * 4);
            return DID_NOT_FIND_RECIPE;
        }
        if (recipe.getMetadataOrDefault(ON_FIRE, false)) {
            getBaseMetaTileEntity().setOnFire();
            return DID_NOT_FIND_RECIPE;
        }
        if (recipe.getMetadataOrDefault(COMPRESSION_TIER, 0) > 0
            || (GTMod.proxy.mLowGravProcessing && (recipe.mSpecialValue == -100 || recipe.mSpecialValue == -300)
                && !isValidForLowGravity(recipe, getBaseMetaTileEntity().getWorld().provider.dimensionId)))
            return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        ICleanroom cleanroom = cleanroomReference.getCleanroom();
        boolean needsCleanroom = recipe.mSpecialValue == -200 || recipe.mSpecialValue == -300;
        if (needsCleanroom && (cleanroom == null || !cleanroom.isValidCleanroom() || cleanroom.getCleanness() == 0))
            return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        if (!canOutput(recipe)) {
            mOutputBlocked++;
            return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        }
        if (!skipOC) {
            calculateCustomOverclock(recipe);
            if (mMaxProgresstime <= 0 || mMaxProgresstime >= Integer.MAX_VALUE - 1
                || mEUt < 0
                || mEUt >= Integer.MAX_VALUE - 1) {
                mMaxProgresstime = 0;
                mEUt = 0;
                return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
            }
        }
        if (!recipe.isRecipeInputEqual(true, fluids, items)) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        if (recipe.mCanBeBuffered) mLastRecipe = recipe;
        for (int i = 0; i < mOutputItems.length; i++) {
            mOutputItems[i] = recipe.rollOutput(getBaseMetaTileEntity(), i);
            if (needsCleanroom && getBaseMetaTileEntity().getRandomNumber(10000) > cleanroom.getCleanness())
                mOutputItems[i] = null;
        }
        for (int i = 0; i < pendingFluids.length; i++) {
            pendingFluids[i] = getBaseMetaTileEntity().getRandomNumber(10000) < recipe.getFluidOutputChance(i)
                ? recipe.getFluidOutput(i)
                : null;
        }
        // The base machine owns energy, progress, items and sound. Fluid results are committed in endProcess().
        mOutputFluid = null;
        markDirty();
        return FOUND_AND_SUCCESSFULLY_USED_RECIPE;
    }

    @Override
    protected boolean canOutput(GTRecipe recipe) {
        if (recipe == null || recipe.mOutputs.length > mOutputItems.length
            || recipe.mFluidOutputs.length > outputTanks.length) return false;
        for (ItemStack item : recipe.mOutputs) {
            if (item != null && item.stackSize > Math.min(64, item.getMaxStackSize())) return false;
        }
        if (recipe.mNeedsEmptyOutput && (!isOutputEmpty() || getDrainableStack() != null)) return false;
        FluidStack[] simulated = copyFluids(outputFluids);
        for (FluidStack fluid : recipe.mFluidOutputs) {
            if (!insertOutput(simulated, fluid)) return false;
        }
        return canOutput(recipe.mOutputs);
    }

    @Override
    public void endProcess() {
        for (FluidStack fluid : pendingFluids) {
            // Capacity was reserved before consuming inputs; output tanks cannot be filled externally.
            if (!insertOutput(outputFluids, fluid))
                throw new IllegalStateException("reserved fluid output no longer fits");
        }
        Arrays.fill(pendingFluids, null);
        markDirty();
        super.endProcess();
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        baseTick = true;
        try {
            super.onPostTick(tile, tick);
        } finally {
            baseTick = false;
        }
        if (!tile.isServerSide() || !mFluidTransfer
            || tick % AUTO_OUTPUT_INTERVAL != 0
            || tile.getFrontFacing() == mMainFacing) return;
        IFluidHandler neighbor = tile.getITankContainerAtSide(tile.getFrontFacing());
        if (neighbor == null) return;
        ForgeDirection side = tile.getBackFacing();
        for (FluidStackTank tank : outputTanks) {
            FluidStack available = tank.drain(AUTO_OUTPUT_AMOUNT, false);
            if (available == null) continue;
            if (!tile.getCoverAtSide(tile.getFrontFacing())
                .letsFluidOut(available.getFluid())) continue;
            int accepted = neighbor.fill(side, available, true);
            if (accepted > 0) {
                tank.drain(Math.min(accepted, available.amount), true);
                inventoryChanged();
            }
        }
    }

    @Override
    public boolean doesAutoOutputFluids() {
        // The base tick pumps only one tank; our loop pumps every output with the same limit.
        return !baseTick && super.doesAutoOutputFluids();
    }

    @Override
    public int fill(FluidStack fluid, boolean commit) {
        if (fluid == null || fluid.amount <= 0 || !isFluidInputAllowed(fluid)) return 0;
        // Prefer an existing fluid. A full matching tank must not occupy another fluid's slot.
        for (FluidStackTank tank : inputTanks) {
            if (tank.getFluid() != null && tank.getFluid()
                .isFluidEqual(fluid)) return fill(tank, fluid, commit);
        }
        for (FluidStackTank tank : inputTanks) {
            if (tank.getFluid() == null) return fill(tank, fluid, commit);
        }
        return 0;
    }

    @Override
    public int fill(ForgeDirection side, FluidStack fluid, boolean commit) {
        return side == ForgeDirection.UNKNOWN || isLiquidInput(side) ? fill(fluid, commit) : 0;
    }

    @Override
    public boolean canFill(ForgeDirection side, Fluid fluid) {
        return fluid != null && (side == ForgeDirection.UNKNOWN || isLiquidInput(side))
            && fill(new FluidStack(fluid, 1), false) > 0;
    }

    @Override
    public FluidStack drain(int amount, boolean commit) {
        if (amount <= 0) return null;
        for (FluidStackTank tank : outputTanks) {
            if (tank.getFluidAmount() > 0) return drain(tank, amount, commit);
        }
        return null;
    }

    @Override
    public FluidStack drain(ForgeDirection side, int amount, boolean commit) {
        return side == ForgeDirection.UNKNOWN || isLiquidOutput(side) ? drain(amount, commit) : null;
    }

    @Override
    public FluidStack drain(ForgeDirection side, FluidStack requested, boolean commit) {
        return drain(side, requested, requested == null ? 0 : requested.amount, commit);
    }

    @Override
    public FluidStack drain(ForgeDirection side, FluidStack requested, int amount, boolean commit) {
        if (requested == null || amount <= 0 || (side != ForgeDirection.UNKNOWN && !isLiquidOutput(side))) return null;
        for (FluidStackTank tank : outputTanks) {
            if (tank.getFluid() != null && tank.getFluid()
                .isFluidEqual(requested)) return drain(tank, amount, commit);
        }
        return null;
    }

    @Override
    public boolean canDrain(ForgeDirection side, Fluid fluid) {
        if (side != ForgeDirection.UNKNOWN && !isLiquidOutput(side)) return false;
        for (FluidStackTank tank : outputTanks) {
            if (tank.getFluidAmount() > 0 && (fluid == null || tank.getFluid()
                .getFluid() == fluid)) return true;
        }
        return false;
    }

    @Override
    public FluidStack getDrainableStack() {
        if (outputFluids != null) {
            for (FluidStack fluid : outputFluids) {
                if (fluid != null && fluid.amount > 0) return fluid;
            }
        }
        return null;
    }

    @Override
    public FluidStack setDrainableStack(FluidStack fluid) {
        outputFluids[0] = fluid;
        inventoryChanged();
        return fluid;
    }

    @Override
    public FluidStackTank getFluidOutputTank() {
        return outputTanks[0];
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection side) {
        FluidTankInfo[] info = new FluidTankInfo[inputTanks.length + outputTanks.length];
        for (int i = 0; i < inputTanks.length; i++) info[i] = inputTanks[i].getInfo();
        for (int i = 0; i < outputTanks.length; i++) info[inputTanks.length + i] = outputTanks[i].getInfo();
        return info;
    }

    @Override
    protected boolean allowPutStackValidated(IGregTechTileEntity tile, int index, ForgeDirection side,
        ItemStack stack) {
        return mRecipes.containsInput(stack);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        writeFluids(tag, "ModernInput", inputStacks());
        writeFluids(tag, "ModernOutput", outputFluids);
        writeFluids(tag, "ModernPending", pendingFluids);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        for (int i = 0; i < inputTanks.length; i++) setInput(i, readFluid(tag, "ModernInput", i));
        readFluids(tag, "ModernOutput", outputFluids);
        readFluids(tag, "ModernPending", pendingFluids);
    }

    private FluidStackTank[] createTanks(FluidStack[] fluids, boolean input) {
        FluidStackTank[] tanks = new FluidStackTank[fluids.length];
        for (int i = 0; i < tanks.length; i++) {
            final int index = i;
            tanks[i] = input && i == 0 ? fluidTank : new FluidStackTank(() -> fluids[index], value -> {
                fluids[index] = value;
                inventoryChanged();
            }, this::getCapacity);
        }
        return tanks;
    }

    private FluidStack[] inputStacks() {
        FluidStack[] fluids = inputFluids.clone();
        fluids[0] = mFluid;
        return fluids;
    }

    private void setInput(int index, FluidStack fluid) {
        if (index == 0) mFluid = fluid;
        else inputFluids[index] = fluid;
    }

    private int fill(FluidStackTank tank, FluidStack fluid, boolean commit) {
        int filled = tank.fill(fluid, commit);
        if (commit && filled > 0) inventoryChanged();
        return filled;
    }

    private FluidStack drain(FluidStackTank tank, int amount, boolean commit) {
        FluidStack drained = tank.drain(amount, commit);
        if (commit && drained != null && drained.amount > 0) inventoryChanged();
        return drained;
    }

    private boolean insertOutput(FluidStack[] slots, FluidStack fluid) {
        if (fluid == null || fluid.amount <= 0) return true;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null && slots[i].isFluidEqual(fluid)) {
                if ((long) slots[i].amount + fluid.amount > getCapacity()) return false;
                slots[i].amount += fluid.amount;
                return true;
            }
        }
        if (fluid.amount > getCapacity()) return false;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null || slots[i].amount <= 0) {
                slots[i] = fluid.copy();
                return true;
            }
        }
        return false;
    }

    private void inventoryChanged() {
        markDirty();
        if (getBaseMetaTileEntity() != null) getBaseMetaTileEntity().markInventoryBeenModified();
    }

    private static FluidStack[] copyFluids(FluidStack[] fluids) {
        FluidStack[] copy = new FluidStack[fluids.length];
        for (int i = 0; i < fluids.length; i++) copy[i] = fluids[i] == null ? null : fluids[i].copy();
        return copy;
    }

    private static void writeFluids(NBTTagCompound tag, String prefix, FluidStack[] fluids) {
        for (int i = 0; i < fluids.length; i++) {
            tag.removeTag(prefix + i);
            if (fluids[i] != null) tag.setTag(prefix + i, fluids[i].writeToNBT(new NBTTagCompound()));
        }
    }

    private static void readFluids(NBTTagCompound tag, String prefix, FluidStack[] fluids) {
        for (int i = 0; i < fluids.length; i++) fluids[i] = readFluid(tag, prefix, i);
    }

    private static FluidStack readFluid(NBTTagCompound tag, String prefix, int index) {
        return FluidStack.loadFluidStackFromNBT(tag.getCompoundTag(prefix + index));
    }

    private static String[] description() {
        // #tr gtnr.machine.modern_fluids.description
        // # GTM-style single-block processing with independent fluid tanks
        // # zh_CN GTM 风格单方块机器，具有独立的流体槽
        // #tr gtnr.machine.modern_fluids.direct_fluids
        // # Processes the corresponding GTNH recipes directly, without fluid cells
        // # zh_CN 直接处理对应的 GTNH 配方，无需流体单元
        return new String[] { StatCollector.translateToLocal("gtnr.machine.modern_fluids.description"),
            StatCollector.translateToLocal("gtnr.machine.modern_fluids.direct_fluids") };
    }
}
