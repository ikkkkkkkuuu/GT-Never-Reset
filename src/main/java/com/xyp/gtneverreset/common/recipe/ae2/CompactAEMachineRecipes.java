package com.xyp.gtneverreset.common.recipe.ae2;

import net.minecraft.init.Items;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import appeng.api.AEApi;
import cpw.mods.fml.common.registry.GameRegistry;

public final class CompactAEMachineRecipes {

    private CompactAEMachineRecipes() {}

    public static void register() {
        var definitions = AEApi.instance()
            .definitions();
        GameRegistry.addShapedRecipe(
            GTNRItemList.QuantumComputer.get(1),
            "IRI",
            "CMC",
            "IRI",
            'I',
            Items.iron_ingot,
            'R',
            Items.redstone,
            'C',
            definitions.materials()
                .fluixCrystal()
                .maybeStack(1)
                .get(),
            'M',
            definitions.blocks()
                .molecularAssembler()
                .maybeStack(1)
                .get());
        GameRegistry.addShapedRecipe(
            GTNRItemList.AssemblerMatrix.get(1),
            "IFI",
            "AMA",
            "IFI",
            'I',
            Items.iron_ingot,
            'F',
            definitions.materials()
                .fluixCrystal()
                .maybeStack(1)
                .get(),
            'A',
            definitions.blocks()
                .iface()
                .maybeStack(1)
                .get(),
            'M',
            definitions.blocks()
                .molecularAssembler()
                .maybeStack(1)
                .get());
    }
}
