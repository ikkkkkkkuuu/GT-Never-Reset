package com.xyp.gtneverreset.compat.nei;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.FurnaceRecipeHandler;

public final class NEIMachineConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        API.addRecipeCatalyst(GTNRItemList.LargeFurnace.get(1), new FurnaceRecipeHandler());
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
