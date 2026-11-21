package com.xyp.gtneverreset.common.machine.multiMachineBase;

import java.util.List;

import com.xyp.gtneverreset.common.gui.modularui.multiblock.CrossRecipeMultiblockGui;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.MultiblockTooltipBuilder;

public abstract class CrossRecipeMultiblockBase<T extends CrossRecipeMultiblockBase<T>> extends GTNRMultiBlockBase<T> {

    private RecipeMap<?> processingRecipeMap;

    protected CrossRecipeMultiblockBase(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    protected CrossRecipeMultiblockBase(String name) {
        super(name);
    }

    @Override
    public abstract List<RecipeMap<?>> getAvailableRecipeMaps();

    @Override
    protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
        return new CrossRecipeProcessingLogic(this);
    }

    @Override
    protected boolean usesInstantThreadFill() {
        return true;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return processingRecipeMap == null
            ? getAvailableRecipeMaps().get(Math.floorMod(machineMode, getAvailableRecipeMaps().size()))
            : processingRecipeMap;
    }

    void setProcessingRecipeMap(RecipeMap<?> map) {
        processingRecipeMap = map;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new AutoMultiblockTooltipBuilder().addAutoTooltip(this)
            .addRecipeMaps(getAvailableRecipeMaps())
            .toolTipFinisher();
    }

    @Override
    protected CrossRecipeMultiblockGui getGui() {
        return new CrossRecipeMultiblockGui(this);
    }

    @Override
    public boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public int nextMachineMode() {
        return (getMachineMode() + 1) % getAvailableRecipeMaps().size();
    }

    @Override
    public void setMachineMode(int index) {
        if (index >= 0 && index < getAvailableRecipeMaps().size()) super.setMachineMode(index);
    }

    @Override
    public String getMachineModeKey() {
        return getRecipeMap().unlocalizedName;
    }

}
