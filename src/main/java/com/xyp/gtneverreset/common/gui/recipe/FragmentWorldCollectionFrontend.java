package com.xyp.gtneverreset.common.gui.recipe;

import static com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes.DRILL_CONSUMPTION_CHANCE;
import static com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes.DRILL_CONSUMPTION_SCALE;
import static com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes.DRILL_INPUT_INDEX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.ItemStack;

import com.gtnewhorizons.modularui.api.math.Pos2d;

import codechicken.nei.NEIClientUtils;
import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.RecipeMapFrontend;
import gregtech.nei.GTNEIDefaultHandler.CachedDefaultRecipe;
import gregtech.nei.GTNEIDefaultHandler.FixedPositionedStack;

public final class FragmentWorldCollectionFrontend extends RecipeMapFrontend {

    public FragmentWorldCollectionFrontend(BasicUIPropertiesBuilder ui, NEIRecipePropertiesBuilder nei) {
        super(ui, nei);
    }

    @Override
    public List<Pos2d> getItemInputPositions(int count) {
        List<Pos2d> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) positions.add(new Pos2d(8 + i % 2 * 18, 26 + i / 2 * 18));
        return positions;
    }

    @Override
    public List<Pos2d> getItemOutputPositions(int count) {
        List<Pos2d> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) positions.add(new Pos2d(106 + i % 3 * 18, 8 + i / 3 * 18));
        return positions;
    }

    @Override
    public List<Pos2d> getFluidInputPositions(int count) {
        return count == 0 ? Collections.emptyList() : Collections.singletonList(new Pos2d(8, 80));
    }

    @Override
    public List<Pos2d> getFluidOutputPositions(int count) {
        return count == 0 ? Collections.emptyList() : Collections.singletonList(new Pos2d(124, 86));
    }

    @Override
    public void prepareRecipe(CachedDefaultRecipe recipe) {
        int chance = recipe.mRecipe.getMetadataOrDefault(DRILL_CONSUMPTION_CHANCE, 0);
        if (chance == 0) return;

        ItemStack drill = recipe.mRecipe.mInputs[DRILL_INPUT_INDEX];
        String percentage = String.format(Locale.ROOT, "%.3f%%", chance * 100.0 / DRILL_CONSUMPTION_SCALE);
        for (var stack : recipe.mInputs) {
            if (stack instanceof FixedPositionedStack fixed && fixed.containsWithNBT(drill)) {
                fixed.setCustomBadge(percentage, NEIClientUtils.translate("recipe.badge.chance.consume", percentage));
                return;
            }
        }
    }
}
