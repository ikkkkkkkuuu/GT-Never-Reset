package com.xyp.gtneverreset.common.machine.multiblock;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtneverreset.common.block.blocks.BlockLightHunterCasing.Variant;

import appeng.api.AEApi;
import gregtech.api.casing.Casings;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

final class LightHunterStructure {

    private LightHunterStructure() {}

    static <T> IStructureElement<T> element(char symbol) {
        ItemStack stack = switch (symbol) {
            case '=' -> Variant.NEUTRONIUM_PIPE_CASING.stack(1);
            case 'A' -> Casings.UHVMachineCasing.toStack(1);
            case 'B' -> Variant.HYPER_MECHANICAL.stack(1);
            case 'C' -> Casings.UXVMachineCasing.toStack(1);
            case 'D' -> Variant.FUSION_GLASS.stack(1);
            case 'E' -> Casings.DimensionalInjectionCasing.toStack(1);
            case 'F' -> Casings.DimensionalBridge.toStack(1);
            case 'G' -> Casings.ContainmentFieldGenerator.toStack(1);
            case 'H' -> Variant.ATOMIC_CASING.stack(1);
            case 'I' -> Variant.EXTREME_DENSITY_CASING.stack(1);
            case 'K' -> Variant.ECHO_CASING.stack(1);
            case 'L' -> Variant.DRAGON_STRENGTH_TRITANIUM_CASING.stack(1);
            case 'M' -> Variant.HYPER_CORE.stack(1);
            case 'N' -> Casings.DimensionallyTranscendentCasing.toStack(1);
            case 'O' -> Casings.HermeticCasing12.toStack(1);
            case 'P' -> Variant.RESTRAINT_DEVICE.stack(1);
            case 'Q' -> Variant.SPACETIME_ASSEMBLY_LINE_UNIT.stack(1);
            case 'R' -> Casings.HighPowerCasing.toStack(1);
            case 'S' -> Casings.MolecularCasing.toStack(1);
            case 'U' -> Casings.FusionMachineCasingMKIV.toStack(1);
            case 'V' -> Casings.SpacetimeAlteringCasing.toStack(1);
            case 'W' -> Variant.FUSION_CASING_MK_V.stack(1);
            case 'X' -> Casings.SingularityReinforcedStellarShieldingCasing.toStack(1);
            case 'Y' -> Variant.ENHANCED_HYPER_CASING.stack(1);
            case 'Z' -> Variant.IMPROVED_SUPERCONDUCTOR.stack(1);
            case '[' -> Casings.ForceFieldGlass.toStack(1);
            case ']' -> Variant.GRAVITY_STABILIZATION_CASING.stack(1);
            case '^' -> Variant.MODULE_CONNECTOR.stack(1);
            case '_' -> Casings.AdvancedComputerCasing.toStack(1);
            case '`' -> Casings.ComputerHeatVent.toStack(1);
            case 'a' -> Variant.SUPER_COMPUTATION_COMPONENT.stack(1);
            case 'b' -> Casings.UIVMachineCasing.toStack(1);
            case 'c' -> Variant.UHV_ULTIMATE_BATTERY.stack(1);
            case 'd' -> Casings.HighPowerCasing.toStack(1);
            case 'J' -> GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Neutronium, 1);
            case 'T' -> new ItemStack(
                AEApi.instance()
                    .definitions()
                    .blocks()
                    .quartzVibrantGlass()
                    .maybeBlock()
                    .get());
            default -> throw new IllegalArgumentException("Unknown station symbol " + symbol);
        };
        return StructureUtility.ofBlock(Block.getBlockFromItem(stack.getItem()), stack.getItemDamage());
    }

    static <T> IStructureElement<T> satellite(char symbol) {
        return element(switch (symbol) {
            case 'A' -> 'K';
            case 'B' -> 'C';
            case 'C' -> 'H';
            case 'D' -> 'L';
            case 'E' -> 'M';
            case 'F' -> 'N';
            case 'G' -> 'A';
            case 'H' -> 'B';
            case 'I' -> 'O';
            case 'J' -> 'P';
            case 'K' -> 'E';
            case 'L' -> 'Q';
            case 'M' -> 'F';
            case 'N' -> '^';
            case 'O' -> 'R';
            default -> throw new IllegalArgumentException("Unknown satellite symbol " + symbol);
        });
    }
}
