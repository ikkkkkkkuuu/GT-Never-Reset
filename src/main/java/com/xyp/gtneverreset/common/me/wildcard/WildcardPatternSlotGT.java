package com.xyp.gtneverreset.common.me.wildcard;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import gregtech.api.objects.GTDualInputPattern;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

public class WildcardPatternSlotGT extends MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> {

    private final WildcardPatternSlotState state = new WildcardPatternSlotState(
        pattern,
        () -> parentMTE.getBaseMetaTileEntity()
            .getWorld(),
        itemInventory,
        fluidInventory,
        this::isEmpty);

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

    public String getActiveGeneratedPatternId() {
        return state.getActiveGeneratedPatternId();
    }

    public void setActivePatternDetails(ICraftingPatternDetails details) {
        state.setActivePatternDetails(details);
    }

    public List<ICraftingPatternDetails> getExpandedDetails(ItemStack patternStack, World world) {
        return state.getExpandedDetails(patternStack, world);
    }

    public boolean canAcceptPattern(ICraftingPatternDetails details) {
        return state.canAcceptPattern(details);
    }

    public void restoreActivePatternDetails(NBTTagCompound saved, List<ICraftingPatternDetails> details) {
        state.restoreActivePatternDetails(saved, details);
    }

    @Override
    public ICraftingPatternDetails getPatternDetails() {
        return state == null ? super.getPatternDetails() : state.getPatternDetails(super.getPatternDetails());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        boolean hasInputs = !isEmpty();
        return state.writeToNBT(super.writeToNBT(nbt), hasInputs);
    }

    @Override
    public GTDualInputPattern getPatternInputs() {
        ICraftingPatternDetails details = getPatternDetails();
        return WildcardPatternSlotState.getPatternInputs(details, details == null ? null : parentMTE.getSharedItems());
    }
}
