package com.xyp.gtneverreset.utils.machine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

import com.xyp.gtneverreset.utils.recipes.wildcard.WildcardPatternGenerator;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.inv.MEInventoryCrafting;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

public final class WildcardHatchPatternState {

    private final MTEHatchCraftingInputME owner;
    private MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME>[] internalInventory;
    private Map<ICraftingPatternDetails, MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME>> patternDetailsPatternSlotMap;
    private boolean supportFluids;

    public WildcardHatchPatternState(MTEHatchCraftingInputME owner) {
        this.owner = owner;
    }

    public WildcardHatchPatternState bind(MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME>[] inventory,
        Map<ICraftingPatternDetails, MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME>> mappings,
        boolean fluids) {
        internalInventory = inventory;
        patternDetailsPatternSlotMap = mappings;
        supportFluids = fluids;
        return this;
    }

    public boolean provideCrafting(ICraftingProviderHelper craftingTracker) {
        if (!hasWildcardPattern()) {
            return false;
        }

        if (!owner.isActive()) {
            return true;
        }

        this.patternDetailsPatternSlotMap.values()
            .removeIf(s -> s instanceof WildcardPatternSlotGT);

        IInventory patterns = owner.getPatterns();
        World world = getWorld();
        for (int index = 0; index < this.internalInventory.length && index < patterns.getSizeInventory(); index++) {
            MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> slot = getOrCreateWildcardSlot(index);
            if (slot == null) {
                continue;
            }

            ItemStack stack = patterns.getStackInSlot(index);
            if (WildcardPatternGenerator.isWildcardPattern(stack)) {
                List<ICraftingPatternDetails> detailsList = getExpandedDetails(slot, stack, world);
                for (ICraftingPatternDetails details : detailsList) {
                    this.patternDetailsPatternSlotMap.put(details, slot);
                    craftingTracker.addCraftingOption(owner, details);
                }
                continue;
            }

            ICraftingPatternDetails details = slot.getPatternDetails();
            if (details != null) {
                craftingTracker.addCraftingOption(owner, details);
            }
        }
        return true;
    }

    public Boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
        if (patternDetails == null) {
            return false;
        }

        boolean hasWildcardPattern = hasWildcardPattern();
        boolean isWildcardRequest = isWildcardPatternDetails(patternDetails);
        MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> slot = this.patternDetailsPatternSlotMap
            .get(patternDetails);
        if (slot != null && !isCurrentInternalSlot(slot)) {
            MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> detachedSlot = slot;
            this.patternDetailsPatternSlotMap.values()
                .removeIf(s -> s == detachedSlot);
            slot = null;
        }

        if (slot == null && (hasWildcardPattern || isWildcardRequest)) {
            slot = findWildcardPatternSlot(patternDetails);
            if (slot != null) {
                this.patternDetailsPatternSlotMap.put(patternDetails, slot);
            }
        }
        if (slot == null) {
            return false;
        }
        if (!hasWildcardPattern && !isWildcardRequest) {
            return null;
        }
        if (slot instanceof WildcardPatternSlotGT wildcardSlot) {
            if (!wildcardSlot.canAcceptPattern(patternDetails)) {
                return false;
            }
            wildcardSlot.setActivePatternDetails(patternDetails);
        }

        MTEHatchCraftingInputME hatch = owner;
        if (!hatch.isActive() || !hatch.getBaseMetaTileEntity()
            .isAllowedToWork()) {
            return false;
        }
        if (hasUnsupportedFluidPacket(table)) {
            return false;
        }

        if (!(table instanceof MEInventoryCrafting meTable)) {
            return false;
        }

        if (!slot.insertItemsAndFluids(meTable)) {
            return false;
        }

        return true;
    }

    public boolean load(NBTTagCompound tag) {
        IInventory patterns = owner.getPatterns();
        Map<Integer, NBTTagCompound> activePatternTags = readSavedActivePatternTags(tag);
        boolean hasItems = false;
        for (int index = 0; index < this.internalInventory.length && index < patterns.getSizeInventory(); index++) {
            registerExpandedPatterns(
                index,
                patterns.getStackInSlot(index),
                activePatternTags.get(Integer.valueOf(index)));
            MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> slot = this.internalInventory[index];
            if (slot != null && (slot.getItemInputs().length > 0 || slot.getFluidInputs().length > 0)) {
                hasItems = true;
            }
        }
        return hasItems;
    }

    private static boolean isWildcardPatternDetails(ICraftingPatternDetails patternDetails) {
        ItemStack pattern = patternDetails == null ? null : patternDetails.getPattern();
        return WildcardPatternGenerator.isWildcardPattern(pattern)
            || WildcardPatternGenerator.isGeneratedPattern(pattern);
    }

    private boolean hasUnsupportedFluidPacket(InventoryCrafting table) {
        if (!(table instanceof MEInventoryCrafting meTable)) return true;
        for (int index = 0; index < table.getSizeInventory(); index++) {
            IAEStack<?> input = meTable.getAEStackInSlot(index);
            if (input == null || input instanceof IAEItemStack || supportFluids && input instanceof IAEFluidStack)
                continue;
            return true;
        }
        return false;
    }

    private boolean hasWildcardPattern() {
        IInventory patterns = owner.getPatterns();
        for (int index = 0; index < this.internalInventory.length && index < patterns.getSizeInventory(); index++) {
            if (WildcardPatternGenerator.isWildcardPattern(patterns.getStackInSlot(index))) {
                return true;
            }
        }
        return false;
    }

    public void registerExpandedPatterns(int index, ItemStack stack) {
        registerExpandedPatterns(index, stack, null);
    }

    private void registerExpandedPatterns(int index, ItemStack stack, NBTTagCompound savedActivePatternTag) {
        if (index < 0 || index >= this.internalInventory.length) {
            return;
        }

        removeDetachedWildcardMappings();

        if (!WildcardPatternGenerator.isWildcardPattern(stack)) {
            return;
        }

        MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> targetSlot = this.internalInventory[index];
        if (targetSlot != null) {
            this.patternDetailsPatternSlotMap.values()
                .removeIf(s -> s == targetSlot);
        }

        MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> slot = getOrCreateWildcardSlot(index);
        if (slot == null) {
            return;
        }

        if (slot != targetSlot) {
            this.patternDetailsPatternSlotMap.values()
                .removeIf(s -> s == slot);
        }

        List<ICraftingPatternDetails> detailsList = getExpandedDetails(slot, stack, getWorld());
        if (slot instanceof WildcardPatternSlotGT wildcardSlot) {
            wildcardSlot.restoreActivePatternDetails(savedActivePatternTag, detailsList);
        }
        for (ICraftingPatternDetails details : detailsList) {
            this.patternDetailsPatternSlotMap.put(details, slot);
        }
    }

    private void removeDetachedWildcardMappings() {
        this.patternDetailsPatternSlotMap.values()
            .removeIf(s -> s instanceof WildcardPatternSlotGT && !isCurrentInternalSlot(s));
    }

    private boolean isCurrentInternalSlot(MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> slot) {
        for (MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> current : this.internalInventory) {
            if (current == slot) {
                return true;
            }
        }
        return false;
    }

    private static Map<Integer, NBTTagCompound> readSavedActivePatternTags(NBTTagCompound source) {
        Map<Integer, NBTTagCompound> result = new HashMap<>();
        if (source == null || !source.hasKey("internalInventory", NBT.TAG_LIST)) {
            return result;
        }
        NBTTagList inventory = source.getTagList("internalInventory", NBT.TAG_COMPOUND);
        for (int index = 0; index < inventory.tagCount(); index++) {
            NBTTagCompound slotWrapper = inventory.getCompoundTagAt(index);
            int patternSlot = slotWrapper.getInteger("patternSlot");
            NBTTagCompound slotTag = slotWrapper.getCompoundTag("patternSlotNBT");
            if (slotTag.hasKey(WildcardPatternSlotGT.KEY_ACTIVE_PATTERN, NBT.TAG_COMPOUND)
                || slotTag.hasKey(WildcardPatternSlotGT.KEY_ACTIVE_PATTERN_ID)) {
                result.put(Integer.valueOf(patternSlot), slotTag);
            }
        }
        return result;
    }

    private MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> findWildcardPatternSlot(
        ICraftingPatternDetails patternDetails) {
        ItemStack requestedPattern = patternDetails.getPattern();
        if (requestedPattern == null) {
            return null;
        }

        World world = getWorld();
        IInventory patterns = owner.getPatterns();
        for (int index = 0; index < this.internalInventory.length && index < patterns.getSizeInventory(); index++) {
            MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> slot = this.internalInventory[index];
            ItemStack stack = patterns.getStackInSlot(index);
            if (slot == null || !WildcardPatternGenerator.isWildcardPattern(stack)) {
                continue;
            }

            for (ICraftingPatternDetails generated : getExpandedDetails(slot, stack, world)) {
                if (arePatternDetailsEqual(generated, patternDetails)) {
                    return slot;
                }
            }
        }
        return null;
    }

    private MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> getOrCreateWildcardSlot(int index) {
        if (index < 0 || index >= this.internalInventory.length) {
            return null;
        }

        MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> slot = this.internalInventory[index];
        IInventory patterns = owner.getPatterns();
        if (patterns == null || index >= patterns.getSizeInventory()) {
            return slot;
        }

        ItemStack stack = patterns.getStackInSlot(index);
        if (!WildcardPatternGenerator.isWildcardPattern(stack) || slot instanceof WildcardPatternSlotGT) {
            return slot;
        }

        WildcardPatternSlotGT wrapped = new WildcardPatternSlotGT(owner, stack, slot);
        this.internalInventory[index] = wrapped;
        return wrapped;
    }

    private static List<ICraftingPatternDetails> getExpandedDetails(
        MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> slot, ItemStack stack, World world) {
        if (slot instanceof WildcardPatternSlotGT wildcardSlot) {
            return wildcardSlot.getExpandedDetails(stack, world);
        }
        return WildcardPatternGenerator.generateAllDetails(stack, world);
    }

    private static boolean arePatternDetailsEqual(ICraftingPatternDetails left, ICraftingPatternDetails right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        ItemStack leftPattern = left.getPattern();
        ItemStack rightPattern = right.getPattern();
        if (leftPattern == rightPattern) {
            return true;
        }
        if (leftPattern == null || rightPattern == null) {
            return false;
        }
        if (leftPattern.getItem() != rightPattern.getItem()
            || leftPattern.getItemDamage() != rightPattern.getItemDamage()) {
            return false;
        }
        String leftId = WildcardPatternGenerator.getGeneratedPatternId(leftPattern);
        String rightId = WildcardPatternGenerator.getGeneratedPatternId(rightPattern);
        if (!leftId.isEmpty() || !rightId.isEmpty()) {
            return leftId.equals(rightId);
        }
        NBTTagCompound leftTag = leftPattern.getTagCompound();
        NBTTagCompound rightTag = rightPattern.getTagCompound();
        if (leftTag == rightTag) {
            return true;
        }
        if (leftTag == null || rightTag == null) {
            return false;
        }
        return leftTag.equals(rightTag);
    }

    private World getWorld() {
        return (owner).getBaseMetaTileEntity()
            .getWorld();
    }
}
