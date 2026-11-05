package com.xyp.gtneverreset.mixins.late.gregtech;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.xyp.gtneverreset.api.me.PatternRecipeModePreflight;

import bartworks.common.tileentities.multis.MTECircuitAssemblyLine;

@Mixin(value = MTECircuitAssemblyLine.class, remap = false)
public abstract class CircuitAssemblyLinePatternModeMixin implements PatternRecipeModePreflight {

    @Shadow
    @Final
    private static int MACHINEMODE_CAL;
    @Shadow
    @Final
    private static int MINIMUM_CIRCUIT_ASSEMBLER_LENGTH;
    @Shadow
    private int length;

    @Shadow
    private boolean imprintMachine(ItemStack stack) {
        throw new AssertionError();
    }

    @Override
    public boolean gtnr$preparePatternRecipeMode() {
        var controller = (MTECircuitAssemblyLine) (Object) this;
        return controller.getMachineMode() == MACHINEMODE_CAL ? imprintMachine(controller.getControllerSlot())
            : length >= MINIMUM_CIRCUIT_ASSEMBLER_LENGTH;
    }
}
