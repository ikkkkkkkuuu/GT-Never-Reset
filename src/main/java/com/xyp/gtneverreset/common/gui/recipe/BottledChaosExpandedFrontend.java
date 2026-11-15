package com.xyp.gtneverreset.common.gui.recipe;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.math.Size;

import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.util.GTRecipe;
import gregtech.common.gui.modularui.UIHelper;

/** Nine-column template for entries that exceed the compact four-by-four layout. */
public final class BottledChaosExpandedFrontend extends BottledChaosFrontend {

    public static final int COMPACT_SLOT_COUNT = 16;

    private final BottledChaosFrontend compactFrontend;

    public static boolean usesExpandedLayout(GTRecipe recipe) {
        return usesExpandedLayout(
            recipe.mInputs.length,
            recipe.mOutputs.length,
            recipe.mFluidInputs.length,
            recipe.mFluidOutputs.length);
    }

    public static boolean usesExpandedLayout(int itemInputs, int itemOutputs, int fluidInputs, int fluidOutputs) {
        return itemInputs > COMPACT_SLOT_COUNT || itemOutputs > COMPACT_SLOT_COUNT
            || fluidInputs > COMPACT_SLOT_COUNT
            || fluidOutputs > COMPACT_SLOT_COUNT;
    }

    public BottledChaosExpandedFrontend(BasicUIPropertiesBuilder uiPropertiesBuilder,
        NEIRecipePropertiesBuilder neiPropertiesBuilder) {
        super(uiPropertiesBuilder.progressBarPos(new Pos2d(89, 194)), neiPropertiesBuilder, new Pos2d(151, 194));
        compactFrontend = new BottledChaosFrontend(
            uiProperties.toBuilder()
                .maxItemInputs(COMPACT_SLOT_COUNT)
                .maxItemOutputs(COMPACT_SLOT_COUNT)
                .maxFluidInputs(COMPACT_SLOT_COUNT)
                .maxFluidOutputs(COMPACT_SLOT_COUNT)
                .progressBarPos(new Pos2d(78, 24))
                .clearNEITransferRects(),
            neiPropertiesBuilder);
    }

    public BottledChaosFrontend getCompactFrontend() {
        return compactFrontend;
    }

    @Override
    protected @NotNull NEIRecipePropertiesBuilder modifyNEIProperties(NEIRecipePropertiesBuilder builder) {
        return builder.recipeBackgroundSize(new Size(170, 300));
    }

    @Override
    public @NotNull List<Pos2d> getItemInputPositions(int count) {
        return UIHelper.getGridPositions(count, 6, 8, 9);
    }

    @Override
    public @NotNull List<Pos2d> getFluidInputPositions(int count) {
        return UIHelper.getGridPositions(count, 6, 152, 9);
    }

    @Override
    public @NotNull Pos2d getSpecialItemPosition() {
        return new Pos2d(61, 192);
    }

    @Override
    public @NotNull List<Pos2d> getItemOutputPositions(int count) {
        return UIHelper.getGridPositions(count, 6, 220, 9);
    }

    @Override
    public @NotNull List<Pos2d> getFluidOutputPositions(int count) {
        return UIHelper.getGridPositions(count, 6, 256, 9);
    }
}
