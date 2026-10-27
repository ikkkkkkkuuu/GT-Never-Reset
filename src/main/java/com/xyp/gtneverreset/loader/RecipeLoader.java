package com.xyp.gtneverreset.loader;

import com.xyp.gtneverreset.common.recipe.ae2.AdvancedIORecipes;
import com.xyp.gtneverreset.common.recipe.ae2.CompactAEMachineRecipes;
import com.xyp.gtneverreset.common.recipe.ae2.StockIOInterfaceRecipes;
import com.xyp.gtneverreset.common.recipe.ae2.WildcardPatternRecipes;
import com.xyp.gtneverreset.common.recipe.ae2.WirelessFrequencyCardRecipes;
import com.xyp.gtneverreset.common.recipe.ae2.WirelessTerminalRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.FluxConnectorRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.LargeFurnaceRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.ModuleHatchRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.OreProcessingRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.PatternInputRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.SingularityDataHubRecipes;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

public final class RecipeLoader {

    private RecipeLoader() {}

    public static void registerRecipes() {
        WirelessTerminalRecipes.register(GTNRItemList.WirelessDualInterfaceTerminal.get(1));
        WirelessFrequencyCardRecipes.register();
        PatternInputRecipes.register();
        ModuleHatchRecipes.register();
        WildcardPatternRecipes.register();
        AdvancedIORecipes.register();
        StockIOInterfaceRecipes.register();
        OreProcessingRecipes.register();
        LargeFurnaceRecipes.register();
        FluxConnectorRecipes.register();
        CompactAEMachineRecipes.register();
        SingularityDataHubRecipes.register();
    }

    public static void registerLateRecipes() {
        OreProcessingRecipes.registerExternalRecipes();
    }
}
