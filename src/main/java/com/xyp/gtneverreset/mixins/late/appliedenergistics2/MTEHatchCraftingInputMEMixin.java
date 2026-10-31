package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import java.util.Map;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.common.machine.hatch.WildcardHatchPatternState;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

@Mixin(value = MTEHatchCraftingInputME.class, remap = false)
public abstract class MTEHatchCraftingInputMEMixin {

    @Shadow
    private MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME>[] internalInventory;

    @Shadow
    private Map<ICraftingPatternDetails, MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME>> patternDetailsPatternSlotMap;

    @Shadow
    private boolean justHadNewItems;

    @Shadow
    @Final
    private boolean supportFluids;

    @Unique
    private final WildcardHatchPatternState gtneverreset$patterns = new WildcardHatchPatternState(
        (MTEHatchCraftingInputME) (Object) this);

    @Inject(method = "provideCrafting", at = @At("HEAD"), cancellable = true)
    private void wildcardpattern$provideExpandedPatterns(ICraftingProviderHelper tracker, CallbackInfo ci) {
        if (gtneverreset$state().provideCrafting(tracker)) ci.cancel();
    }

    @Inject(method = "pushPattern", at = @At("HEAD"), cancellable = true)
    private void wildcardpattern$pushExpandedPattern(ICraftingPatternDetails details, InventoryCrafting table,
        CallbackInfoReturnable<Boolean> cir) {
        Boolean result = gtneverreset$state().pushPattern(details, table);
        if (result != null) {
            if (result) {
                justHadNewItems = true;
                ((MTEHatchCraftingInputME) (Object) this).markDirty();
            }
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "onPatternChange", at = @At("RETURN"))
    private void wildcardpattern$registerExpandedPatternMap(int index, ItemStack stack, CallbackInfo ci) {
        gtneverreset$state().registerExpandedPatterns(index, stack);
    }

    @Inject(method = "loadNBTData", at = @At("RETURN"))
    private void wildcardpattern$registerLoadedExpandedPatternMap(NBTTagCompound tag, CallbackInfo ci) {
        if (gtneverreset$state().load(tag)) justHadNewItems = true;
    }

    @Unique
    private WildcardHatchPatternState gtneverreset$state() {
        return gtneverreset$patterns.bind(internalInventory, patternDetailsPatternSlotMap, supportFluids);
    }
}
