package com.xyp.gtneverreset.mixins.late.gregtech;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.machines.multi.MTEBrickedBlastFurnace;

@Mixin(value = MTEBrickedBlastFurnace.class, remap = false)
public abstract class BrickedBlastFurnaceAutomationMixin {

    @ModifyConstant(
        method = "getTooltip",
        constant = @Constant(stringValue = "All input/output is done manually through the controller"),
        require = 1)
    private String gtneverreset$automationTooltip(String original) {
        ConfigLoader.ensureLoaded();
        if (!MainConfig.gregTech.enableBrickedBlastFurnaceAutomation) return original;
        // #tr gtneverreset.bbf.automation
        // # Allows automated item input and output through the controller
        // # zh_CN 可通过控制器自动输入和输出物品
        return StatCollector.translateToLocal("gtneverreset.bbf.automation");
    }

    @Inject(method = "allowPutStack", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtneverreset$allowInput(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        ConfigLoader.ensureLoaded();
        if (!MainConfig.gregTech.enableBrickedBlastFurnaceAutomation) return;
        cir.setReturnValue(index >= 0 && index < MTEBrickedBlastFurnace.INPUT_SLOTS);
    }

    @Inject(method = "allowPullStack", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtneverreset$allowOutput(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        ConfigLoader.ensureLoaded();
        if (!MainConfig.gregTech.enableBrickedBlastFurnaceAutomation) return;
        cir.setReturnValue(
            index >= MTEBrickedBlastFurnace.INPUT_SLOTS
                && index < MTEBrickedBlastFurnace.INPUT_SLOTS + MTEBrickedBlastFurnace.OUTPUT_SLOTS);
    }
}
