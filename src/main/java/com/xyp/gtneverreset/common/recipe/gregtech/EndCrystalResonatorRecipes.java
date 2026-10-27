package com.xyp.gtneverreset.common.recipe.gregtech;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;

public final class EndCrystalResonatorRecipes {

    private EndCrystalResonatorRecipes() {}

    public static void register() {
        GameRegistry.addRecipe(
            GTNRItemList.StableEndCrystal.get(1),
            "GGG",
            "GEG",
            "GTG",
            'G',
            Blocks.glass,
            'E',
            Items.ender_eye,
            'T',
            Items.ghast_tear);
        Materials[] circuits = { Materials.LV, Materials.MV, Materials.HV, Materials.EV, Materials.IV, Materials.LuV,
            Materials.ZPM, Materials.UV, Materials.UHV, Materials.UEV, Materials.UIV, Materials.UMV, Materials.UXV,
            Materials.MAX };
        for (int tier = 1; tier <= 14; tier++) {
            GTValues.RA.stdBuilder()
                .itemInputs(
                    ItemList.MACHINE_HULLS[tier].get(1),
                    ItemList.valueOf("Emitter_" + GTValues.VN[tier])
                        .get(2),
                    new Object[] { OrePrefixes.circuit.get(circuits[tier - 1]), 2 },
                    new ItemStack(Blocks.obsidian, 4),
                    new ItemStack(Items.ender_eye, 2))
                .itemOutputs(
                    GTNRItemList.valueOf("EndCrystalResonator" + GTValues.VN[tier])
                        .get(1))
                .duration(200)
                .eut(GTValues.VP[tier])
                .addTo(RecipeMaps.assemblerRecipes);
        }
    }
}
