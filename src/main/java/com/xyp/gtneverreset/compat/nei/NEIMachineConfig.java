package com.xyp.gtneverreset.compat.nei;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import gregtech.api.recipe.RecipeMaps;

public final class NEIMachineConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        com.xyp.gtneverreset.compat.nei.chaos.BottledChaosNEIHandler.register();
        com.xyp.gtneverreset.compat.nei.chaos.BottledChaosStructureNEIHandler.register();
        com.xyp.gtneverreset.compat.nei.chaos.BottledChaosPrototypePreview.register();
        API.addRecipeCatalyst(GTNRItemList.LargeFurnace.get(1), new FurnaceRecipeHandler());
        API.addRecipeCatalyst(GTNRItemList.UniversalJointFactory.get(1), RecipeMaps.cutterFakeRecipes.unlocalizedName);
    }

    @Override
    public String getName() {
        return GTNeverReset.NAME;
    }

    @Override
    public String getVersion() {
        return "1";
    }
}
