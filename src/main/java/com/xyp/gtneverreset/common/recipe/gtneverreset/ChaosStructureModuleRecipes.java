package com.xyp.gtneverreset.common.recipe.gtneverreset;

import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.common.item.items.ChaosStructureModuleItem;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;

/** Display recipes for workbench operations; these pools are never executable by ordinary GT machines. */
public final class ChaosStructureModuleRecipes {

    private ChaosStructureModuleRecipes() {}

    public static void register() {
        for (var machine : GregTechAPI.METATILEENTITIES) {
            if (!(machine instanceof gregtech.api.metatileentity.implementations.MTEBasicMachine)) continue;
            ItemStack controller = machine.getStackForm(1);
            if (ChaosStructureModuleItem.source(controller) == null) continue;
            GTValues.RA.stdBuilder()
                .itemInputs(GTNRItemList.ChaosStructureModule.get(1), controller, ItemList.Casing_SolidSteel.get(8))
                .itemOutputs(ChaosStructureModuleItem.pack(GTNRItemList.ChaosStructureModule.get(1), controller))
                .duration(1)
                .eut(0)
                .fake()
                .addTo(GTNRRecipeMaps.CHAOS_MODULE_RECIPES);
        }
    }
}
