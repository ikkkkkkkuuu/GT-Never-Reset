package com.xyp.gtneverreset.common.machine.hatch;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.me.wildcard.WildcardPatternCache;
import com.xyp.gtneverreset.common.me.wildcard.WildcardPatternGenerator;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import gregtech.api.enums.GTValues;
import gregtech.api.objects.GTDualInputPattern;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;
import lombok.Getter;

public class WildcardPatternSlotGT extends MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> {

    public static final String KEY_ACTIVE_PATTERN = "WildcardActivePattern";
    public static final String KEY_ACTIVE_PATTERN_ID = "WildcardActivePatternId";

    private ICraftingPatternDetails activePatternDetails;
    private ItemStack activePatternStack;
    @Getter
    private String activeGeneratedPatternId = "";
    private final WildcardPatternCache<List<ICraftingPatternDetails>> expansionCache = new WildcardPatternCache<>();
    private List<ICraftingPatternDetails> cachedExpandedDetails = Collections.emptyList();

    public WildcardPatternSlotGT(MTEHatchCraftingInputME parent, ItemStack pattern,
        MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> originalSlot) {
        super(pattern, parent);
        if (originalSlot != null) {
            for (ItemStack itemStack : originalSlot.getItemInputs()) {
                if (itemStack != null) {
                    this.itemInventory.add(itemStack.copy());
                }
            }
            for (FluidStack fluidStack : originalSlot.getFluidInputs()) {
                if (fluidStack != null) {
                    this.fluidInventory.add(fluidStack.copy());
                }
            }
        }
    }

    public void setActivePatternDetails(ICraftingPatternDetails activePatternDetails) {
        if (this.activePatternDetails == activePatternDetails) return;
        this.activePatternDetails = activePatternDetails;
        setActivePatternStack(activePatternDetails == null ? null : activePatternDetails.getPattern());
    }

    private void setActivePatternStack(ItemStack activePattern) {
        this.activePatternStack = activePattern == null ? null : activePattern.copy();
        this.activeGeneratedPatternId = WildcardPatternGenerator.getGeneratedPatternId(activePattern);
    }

    public List<ICraftingPatternDetails> getExpandedDetails(ItemStack patternStack, World world) {
        this.cachedExpandedDetails = expansionCache
            .get(patternStack, world, () -> WildcardPatternGenerator.generateAllDetails(patternStack, world));
        return this.cachedExpandedDetails;
    }

    public boolean canAcceptPattern(ICraftingPatternDetails patternDetails) {
        if (patternDetails == null || isEmpty()) {
            return true;
        }
        String requestedId = WildcardPatternGenerator.getGeneratedPatternId(patternDetails.getPattern());
        if (requestedId.isEmpty()) {
            return false;
        }
        if (this.activeGeneratedPatternId.isEmpty()) {
            setActivePatternDetails(recoverActiveDetails(getCachedOrGeneratedDetails()));
        }
        return requestedId.equals(this.activeGeneratedPatternId);
    }

    public void restoreActivePatternDetails(NBTTagCompound savedActivePatternTag,
        List<ICraftingPatternDetails> detailsList) {
        if (this.activePatternDetails != null || !hasStoredInputs()) {
            return;
        }
        String savedId = savedActivePatternTag == null ? "" : savedActivePatternTag.getString(KEY_ACTIVE_PATTERN_ID);
        ItemStack savedActivePattern = null;
        if (savedActivePatternTag != null && savedActivePatternTag.hasKey(KEY_ACTIVE_PATTERN, NBT.TAG_COMPOUND)) {
            savedActivePattern = ItemStack
                .loadItemStackFromNBT(savedActivePatternTag.getCompoundTag(KEY_ACTIVE_PATTERN));
            if (savedId.isEmpty()) {
                savedId = WildcardPatternGenerator.getGeneratedPatternId(savedActivePattern);
            }
        }

        ICraftingPatternDetails restored = findMatchingPatternDetails(savedId, detailsList);
        if (restored == null) {
            restored = findMatchingPatternDetails(savedActivePattern, detailsList);
        }
        if (restored == null && savedActivePattern != null) {
            ICraftingPatternDetails savedDetails = createDetailsFromPattern(savedActivePattern);
            if (savedDetails != null && inputsMatchSlot(savedDetails)) {
                restored = savedDetails;
            }
        }
        if (restored == null) {
            restored = recoverActiveDetails(detailsList);
        }
        setActivePatternDetails(restored);
    }

    @Override
    public ICraftingPatternDetails getPatternDetails() {
        if (this.activePatternDetails != null) {
            if (!hasStoredInputs()) {
                setActivePatternDetails(null);
                return super.getPatternDetails();
            }
            return this.activePatternDetails;
        }
        if (hasStoredInputs()) {
            if (this.activePatternStack != null) {
                ICraftingPatternDetails savedDetails = createDetailsFromPattern(this.activePatternStack);
                if (savedDetails != null && inputsMatchSlot(savedDetails)) {
                    setActivePatternDetails(savedDetails);
                    return this.activePatternDetails;
                }
            }
            setActivePatternDetails(recoverActiveDetails(getCachedOrGeneratedDetails()));
            return this.activePatternDetails;
        }
        return super.getPatternDetails();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        boolean hasInputs = hasStoredInputs();
        NBTTagCompound written = super.writeToNBT(nbt);
        if (hasInputs && this.activePatternDetails == null) {
            setActivePatternDetails(recoverActiveDetails(getCachedOrGeneratedDetails()));
        }
        ItemStack activePattern = this.activePatternStack;
        if (hasInputs && activePattern != null && !this.activeGeneratedPatternId.isEmpty()) {
            NBTTagCompound activeTag = new NBTTagCompound();
            activePattern.writeToNBT(activeTag);
            written.setTag(KEY_ACTIVE_PATTERN, activeTag);
            written.setString(KEY_ACTIVE_PATTERN_ID, this.activeGeneratedPatternId);
        } else {
            written.removeTag(KEY_ACTIVE_PATTERN);
            written.removeTag(KEY_ACTIVE_PATTERN_ID);
        }
        return written;
    }

    private List<ICraftingPatternDetails> getCachedOrGeneratedDetails() {
        if (this.pattern == null) {
            return this.cachedExpandedDetails;
        }
        World world = this.parentMTE.getBaseMetaTileEntity()
            .getWorld();
        if (world == null) {
            return this.cachedExpandedDetails;
        }
        return getExpandedDetails(this.pattern, world);
    }

    private ICraftingPatternDetails recoverActiveDetails(List<ICraftingPatternDetails> detailsList) {
        if (detailsList == null || detailsList.isEmpty()) {
            return null;
        }
        ICraftingPatternDetails recovered = null;
        for (ICraftingPatternDetails details : detailsList) {
            if (!inputsMatchSlot(details)) {
                continue;
            }
            if (recovered != null && !arePatternStacksEqual(recovered.getPattern(), details.getPattern())) {
                return null;
            }
            recovered = details;
        }
        return recovered;
    }

    private ICraftingPatternDetails createDetailsFromPattern(ItemStack patternStack) {
        if (patternStack == null) {
            return null;
        }
        World world = this.parentMTE.getBaseMetaTileEntity()
            .getWorld();
        return WildcardPatternGenerator.createDetailForCurrentStack(patternStack.copy(), world);
    }

    private boolean inputsMatchSlot(ICraftingPatternDetails details) {
        if (details == null) return false;
        IAEStack<?>[] inputs = details.getCondensedAEInputs();
        boolean sawInput = false;
        for (IAEStack<?> input : inputs) {
            if (input instanceof IAEItemStack item) {
                sawInput = true;
                long count = 0;
                for (ItemStack stored : itemInventory)
                    if (stored != null && GTUtility.areStacksEqual(stored, item.getItemStack()))
                        count += Math.max(0, stored.stackSize);
                if (count < item.getStackSize()) return false;
            } else if (input instanceof IAEFluidStack fluid) {
                sawInput = true;
                long amount = 0;
                for (FluidStack stored : fluidInventory)
                    if (stored != null && GTUtility.areFluidsEqual(stored, fluid.getFluidStack()))
                        amount += Math.max(0, stored.amount);
                if (amount < fluid.getStackSize()) return false;
            }
        }
        for (ItemStack stored : itemInventory) {
            if (stored == null || stored.stackSize <= 0) continue;
            boolean matched = false;
            for (IAEStack<?> input : inputs)
                if (input instanceof IAEItemStack item && GTUtility.areStacksEqual(stored, item.getItemStack())) {
                    matched = true;
                    break;
                }
            if (!matched) return false;
        }
        for (FluidStack stored : fluidInventory) {
            if (stored == null || stored.amount <= 0) continue;
            boolean matched = false;
            for (IAEStack<?> input : inputs)
                if (input instanceof IAEFluidStack fluid && GTUtility.areFluidsEqual(stored, fluid.getFluidStack())) {
                    matched = true;
                    break;
                }
            if (!matched) return false;
        }
        return sawInput;
    }

    private boolean hasStoredInputs() {
        return !isEmpty();
    }

    private static ICraftingPatternDetails findMatchingPatternDetails(String savedId,
        List<ICraftingPatternDetails> detailsList) {
        if (savedId == null || savedId.isEmpty() || detailsList == null || detailsList.isEmpty()) {
            return null;
        }
        for (ICraftingPatternDetails details : detailsList) {
            if (details != null
                && savedId.equals(WildcardPatternGenerator.getGeneratedPatternId(details.getPattern()))) {
                return details;
            }
        }
        return null;
    }

    private static ICraftingPatternDetails findMatchingPatternDetails(ItemStack savedActivePattern,
        List<ICraftingPatternDetails> detailsList) {
        if (savedActivePattern == null || detailsList == null || detailsList.isEmpty()) {
            return null;
        }
        for (ICraftingPatternDetails details : detailsList) {
            if (details != null && arePatternStacksEqual(savedActivePattern, details.getPattern())) {
                return details;
            }
        }
        return null;
    }

    private static boolean arePatternStacksEqual(ItemStack left, ItemStack right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left.getItem() != right.getItem() || left.getItemDamage() != right.getItemDamage()) {
            return false;
        }
        String leftId = WildcardPatternGenerator.getGeneratedPatternId(left);
        String rightId = WildcardPatternGenerator.getGeneratedPatternId(right);
        if (!leftId.isEmpty() || !rightId.isEmpty()) {
            return leftId.equals(rightId);
        }
        NBTTagCompound leftTag = left.getTagCompound();
        NBTTagCompound rightTag = right.getTagCompound();
        if (leftTag == rightTag) {
            return true;
        }
        return leftTag != null && leftTag.equals(rightTag);
    }

    @Override
    public GTDualInputPattern getPatternInputs() {
        ICraftingPatternDetails details = getPatternDetails();
        GTDualInputPattern dualInputs = new GTDualInputPattern();
        if (details == null) {
            dualInputs.inputItems = GTValues.emptyItemStackArray;
            dualInputs.inputFluid = GTValues.emptyFluidStackArray;
            return dualInputs;
        }
        ItemStack[] sharedItems = this.parentMTE.getSharedItems();
        IAEStack<?>[] inputs = details.getAEInputs();
        int itemCount = sharedItems == null ? 0 : sharedItems.length;
        int fluidCount = 0;
        for (IAEStack<?> input : inputs) {
            if (input instanceof IAEItemStack) itemCount++;
            else if (input instanceof IAEFluidStack) fluidCount++;
        }
        ItemStack[] inputItems = new ItemStack[itemCount];
        FluidStack[] inputFluids = new FluidStack[fluidCount];
        int itemIndex = sharedItems == null ? 0 : sharedItems.length;
        if (itemIndex > 0) System.arraycopy(sharedItems, 0, inputItems, 0, itemIndex);
        int fluidIndex = 0;
        for (IAEStack<?> singleInput : inputs) {
            if (singleInput instanceof IAEItemStack ais) {
                inputItems[itemIndex++] = ais.getItemStack();
            } else if (singleInput instanceof IAEFluidStack ifs) {
                inputFluids[fluidIndex++] = ifs.getFluidStack();
            }
        }

        dualInputs.inputItems = inputItems;
        dualInputs.inputFluid = inputFluids;
        return dualInputs;
    }

}
