package com.xyp.gtneverreset.common.gui.recipe;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.GTNeverReset;

import cpw.mods.fml.common.ModContainer;
import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.RecipeMapFrontend;
import gregtech.nei.RecipeDisplayInfo;

/** Use TST's short name in recipe-owner information while retaining GT's single-line layout. */
public class ChaosRecipeFrontend extends RecipeMapFrontend {

    public ChaosRecipeFrontend(BasicUIPropertiesBuilder ui, NEIRecipePropertiesBuilder nei) {
        super(ui, nei);
    }

    @Override
    protected void drawRecipeOwnerInfo(RecipeDisplayInfo recipeInfo) {
        drawOwners(recipeInfo);
    }

    static void drawOwners(RecipeDisplayInfo recipeInfo) {
        if (recipeInfo.recipe.owners == null) return;
        for (int i = 0; i < recipeInfo.recipe.owners.size(); i++) {
            String key = recipeInfo.recipe.owners.size() == 1 ? "GT5U.recipes.recipe_by"
                : i == 0 ? "GT5U.recipes.recipe_by.original" : "GT5U.recipes.recipe_by.modified";
            ModContainer owner = recipeInfo.recipe.owners.get(i);
            String name = GTNeverReset.MODID.equals(owner.getModId()) ? "GTNR" : owner.getName();
            recipeInfo.drawText(EnumChatFormatting.ITALIC + StatCollector.translateToLocalFormatted(key, name));
        }
    }
}
