package com.xyp.gtneverreset.mixins.late.gregtech;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtneverreset.common.machine.basicMachine.FragmentWorldCollector;
import com.xyp.gtneverreset.common.me.stockio.StockIORecipeBridge;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.util.GTRecipe;

/** Keeps the native recipe checks while committing one adjacent ME interface's simulated inputs. */
@Mixin(value = MTEBasicMachine.class, remap = false)
public abstract class BasicMachineStockIOMixin {

    @Inject(method = "onPostTick", at = @At("HEAD"))
    private void gtnr$stockIORetry(IGregTechTileEntity base, long tick, CallbackInfo ci) {
        StockIORecipeBridge.retryIdle((MTEBasicMachine) (Object) this, base, tick);
    }

    @Inject(
        method = "onPostTick",
        at = @At(
            value = "FIELD",
            target = "Lgregtech/api/metatileentity/implementations/MTEBasicMachine;mOutputItems:[Lnet/minecraft/item/ItemStack;",
            opcode = Opcodes.GETFIELD,
            ordinal = 0))
    private void gtnr$stockIOComplete(IGregTechTileEntity base, long tick, CallbackInfo ci) {
        StockIORecipeBridge.completeRecipe((MTEBasicMachine) (Object) this, base);
    }

    @WrapOperation(
        method = "onPostTick",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/metatileentity/implementations/MTEBasicMachine;checkRecipe()I"))
    private int gtnr$stockIOWaitForOutput(MTEBasicMachine machine, Operation<Integer> original) {
        return StockIORecipeBridge.hasBlockedOutput(machine.getBaseMetaTileEntity()) ? 1 : original.call(machine);
    }

    @WrapMethod(method = "checkRecipe(Z)I")
    private int gtnr$stockIORecipe(boolean skipOC, Operation<Integer> original) {
        MTEBasicMachine machine = (MTEBasicMachine) (Object) this;
        return StockIORecipeBridge.checkRecipe(machine, () -> original.call(skipOC));
    }

    @WrapOperation(
        method = "checkRecipe(Z)I",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/metatileentity/implementations/MTEBasicMachine;getAllInputs()[Lnet/minecraft/item/ItemStack;"))
    private ItemStack[] gtnr$stockIOItems(MTEBasicMachine machine, Operation<ItemStack[]> original) {
        return StockIORecipeBridge.itemInputs(machine, original.call(machine));
    }

    @ModifyArg(
        method = "checkRecipe(Z)I",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/recipe/FindRecipeQuery;fluids([Lnet/minecraftforge/fluids/FluidStack;)Lgregtech/api/recipe/FindRecipeQuery;"),
        index = 0)
    private FluidStack[] gtnr$stockIOFluidLookup(FluidStack[] local) {
        return StockIORecipeBridge.fluidInputs((MTEBasicMachine) (Object) this, local);
    }

    @WrapOperation(
        method = "checkRecipe(Z)I",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/util/GTRecipe;isRecipeInputEqual(Z[Lnet/minecraftforge/fluids/FluidStack;[Lnet/minecraft/item/ItemStack;)Z"))
    private boolean gtnr$consumeRecipeInputs(GTRecipe recipe, boolean consume, FluidStack[] localFluids,
        ItemStack[] items, Operation<Boolean> original) {
        MTEBasicMachine machine = (MTEBasicMachine) (Object) this;
        FluidStack[] fluids = StockIORecipeBridge.fluidInputs(machine, localFluids);
        boolean matched = original.call(recipe, consume, fluids, items);
        if (matched && consume && machine instanceof FragmentWorldCollector collector) {
            collector.consumeDrillHead(recipe, items);
        }
        return matched;
    }
}
