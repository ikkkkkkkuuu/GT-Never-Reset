package com.xyp.gtneverreset.common.me.wildcard;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import gregtech.api.objects.GTDualInputPattern;

public class WildcardPatternSlotSuper extends SuperMTEHatchCraftingInputME.PatternSlot<SuperMTEHatchCraftingInputME> {

    private final WildcardPatternSlotState state = new WildcardPatternSlotState(
        pattern,
        () -> parentMTE.getBaseMetaTileEntity()
            .getWorld(),
        itemInventory,
        fluidInventory,
        this::isEmpty);

    public WildcardPatternSlotSuper(SuperMTEHatchCraftingInputME parent, ItemStack pattern, NBTTagCompound saved) {
        super(pattern, saved, parent);
        restoreActivePatternDetails(
            saved,
            getExpandedDetails(
                pattern,
                parent.getBaseMetaTileEntity()
                    .getWorld()));
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
