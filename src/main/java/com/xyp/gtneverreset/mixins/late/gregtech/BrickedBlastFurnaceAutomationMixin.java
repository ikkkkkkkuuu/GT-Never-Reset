package com.xyp.gtneverreset.mixins.late.gregtech;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.config.GregTechConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.machines.multi.MTEBrickedBlastFurnace;

@Mixin(value = MTEBrickedBlastFurnace.class, remap = false)
public abstract class BrickedBlastFurnaceAutomationMixin {

    @ModifyArg(
        method = "getTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/util/MultiblockTooltipBuilder;addMarkdown(Lnet/minecraft/util/ResourceLocation;)Lgregtech/api/util/MultiblockTooltipBuilder;"),
        index = 0,
        require = 1)
    private ResourceLocation gtneverreset$automationTooltip(ResourceLocation original) {
        ConfigLoader.ensureLoaded();
        if (!GregTechConfig.enableBrickedBlastFurnaceAutomation) return original;
        return new ResourceLocation(GTNeverReset.MODID, "bricked-blast-furnace-automation");
    }

    @Inject(method = "allowPutStack", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtneverreset$allowInput(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        ConfigLoader.ensureLoaded();
        if (!GregTechConfig.enableBrickedBlastFurnaceAutomation) return;
        cir.setReturnValue(index >= 0 && index < MTEBrickedBlastFurnace.INPUT_SLOTS);
    }

    @Inject(method = "allowPullStack", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtneverreset$allowOutput(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        ConfigLoader.ensureLoaded();
        if (!GregTechConfig.enableBrickedBlastFurnaceAutomation) return;
        cir.setReturnValue(
            index >= MTEBrickedBlastFurnace.INPUT_SLOTS
                && index < MTEBrickedBlastFurnace.INPUT_SLOTS + MTEBrickedBlastFurnace.OUTPUT_SLOTS);
    }
}
