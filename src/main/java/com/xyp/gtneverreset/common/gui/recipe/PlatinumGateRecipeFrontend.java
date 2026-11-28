package com.xyp.gtneverreset.common.gui.recipe;

import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.StatCollector;

import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.math.Size;
import com.xyp.gtneverreset.common.recipe.gtneverreset.PlatinumProcessingRecipes;

import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.RecipeMapFrontend;
import gregtech.common.gui.modularui.UIHelper;
import gregtech.nei.RecipeDisplayInfo;
import gregtech.nei.formatter.HeatingCoilSpecialValueFormatter;

public final class PlatinumGateRecipeFrontend extends RecipeMapFrontend {

    private static final int COLUMNS = 4;
    private static final int SLOT_SIZE = 18;
    private static final int TOP = 8;

    public PlatinumGateRecipeFrontend(BasicUIPropertiesBuilder uiProperties, NEIRecipePropertiesBuilder neiProperties) {
        super(uiProperties.logoPos(new Pos2d(80, 7)), neiProperties);
    }

    @Override
    protected NEIRecipePropertiesBuilder modifyNEIProperties(NEIRecipePropertiesBuilder builder) {
        int inputRows = rows(uiProperties.maxItemInputs) + rows(uiProperties.maxFluidInputs);
        int outputRows = rows(uiProperties.maxItemOutputs) + rows(uiProperties.maxFluidOutputs);
        return builder.recipeBackgroundSize(new Size(170, TOP + SLOT_SIZE * Math.max(inputRows, outputRows) + 2));
    }

    @Override
    public List<Pos2d> getItemInputPositions(int count) {
        return UIHelper.getGridPositions(count, 6, TOP, COLUMNS);
    }

    @Override
    public List<Pos2d> getItemOutputPositions(int count) {
        return UIHelper.getGridPositions(count, 98, TOP, COLUMNS);
    }

    @Override
    public List<Pos2d> getFluidInputPositions(int count) {
        return UIHelper.getGridPositions(count, 6, TOP + SLOT_SIZE * rows(uiProperties.maxItemInputs), COLUMNS);
    }

    @Override
    public List<Pos2d> getFluidOutputPositions(int count) {
        return UIHelper.getGridPositions(count, 98, TOP + SLOT_SIZE * rows(uiProperties.maxItemOutputs), COLUMNS);
    }

    private static int rows(int count) {
        return (count + COLUMNS - 1) / COLUMNS;
    }

    public static List<String> formatRequirements(RecipeDisplayInfo info) {
        var recipe = info.recipe;
        int ring = recipe.getMetadataOrDefault(PlatinumProcessingRecipes.RING_REQUIREMENT, 1);
        List<String> lines = new ArrayList<>();
        if (recipe.getMetadataOrDefault(COIL_HEAT, 0) > 0) {
            lines.addAll(HeatingCoilSpecialValueFormatter.INSTANCE.format(info));
        }
        // #tr gtnr.recipe.platinum_gate.nei.ring
        // # Requires ring %s
        // # zh_CN 需要第 %s 圈
        lines.add(StatCollector.translateToLocalFormatted("gtnr.recipe.platinum_gate.nei.ring", ring));
        return lines;
    }
}
