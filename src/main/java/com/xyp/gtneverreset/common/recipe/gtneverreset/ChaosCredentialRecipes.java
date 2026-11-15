package com.xyp.gtneverreset.common.recipe.gtneverreset;

import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.common.crafting.chaos.ChaosPresets;
import com.xyp.gtneverreset.common.item.items.ChaosCredentialItem;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.enums.GTValues;

/** Display recipes for workbench operations; these pools are never executable by ordinary GT machines. */
public final class ChaosCredentialRecipes {

    private ChaosCredentialRecipes() {}

    public static void register() {
        for (var chain : ChaosPresets.all()) {
            ItemStack marker = chain.steps()
                .get(0)
                .recipe().mInputs[0].copy();
            marker.stackSize = 0;
            GTValues.RA.stdBuilder()
                .itemInputs(GTNRItemList.ChaosCredential.get(1), marker)
                .itemOutputs(ChaosCredentialItem.bind(GTNRItemList.ChaosCredential.get(1), chain))
                .duration(1)
                .eut(0)
                .fake()
                .addTo(GTNRRecipeMaps.CHAOS_CREDENTIAL_RECIPES);
        }
    }
}
