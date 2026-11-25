package com.xyp.gtneverreset.common.machine.multiMachineBase;

import java.util.List;

import net.minecraft.util.StatCollector;

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
    public List<String> getTooltipFeatureLines() {
        List<String> lines = super.getTooltipFeatureLines();
        if (supportsMachineModeSwitch() && getAvailableRecipeMaps().size() > 1) {
            // #tr gtnr.machine.tooltip.cross_recipe_pattern_buffer
            // # Cross-Recipe ME Pattern Buffer
            // # zh_CN 跨配方 ME 样板仓
            String buffer = AutoMultiblockTooltipBuilder
                .mechanicName("gtnr.machine.tooltip.cross_recipe_pattern_buffer");
            // #tr gtnr.machine.tooltip.cross_recipe_pattern_modes
            // # A %s can run patterns across maps at once. Other inputs and untyped patterns use the selected mode.
            // # zh_CN %s可同时加工不同配方池的样板。普通输入和未标注配方类型的样板按机器界面选定的模式加工。
            lines.add(
                StatCollector.translateToLocalFormatted("gtnr.machine.tooltip.cross_recipe_pattern_modes", buffer));
        }
        return lines;
    }

    @Override
    protected CrossRecipeMultiblockGui<?> getGui() {
        return new CrossRecipeMultiblockGui<>(this);
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
