package com.xyp.gtneverreset.common.machine.hatch;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import org.apache.commons.lang3.ArrayUtils;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtneverreset.api.me.RecipeMapPatternInventory;
import com.xyp.gtneverreset.common.gui.modularui.hatch.CrossRecipeMEPatternBufferGui;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;

/** Isolated pattern buffers that select one of the receiving controller's native recipe modes. */
public final class CrossRecipeMEPatternBuffer extends CircuitMEPatternBuffer {

    private static final String TERMINAL_RECIPE_MAP = "gtnrTerminalRecipeMap";
    private String terminalRecipeMap = "";

    public CrossRecipeMEPatternBuffer(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    private CrossRecipeMEPatternBuffer(String name, int tier, String[] description, ITexture[][][] textures) {
        super(name, tier, description, textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new CrossRecipeMEPatternBuffer(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    protected PatternSlot<SuperMTEHatchCraftingInputME> createPatternSlot(ItemStack pattern, NBTTagCompound saved) {
        return new RecipeMapSlot(pattern, saved, this);
    }

    public List<String> getControllerRecipeMaps() {
        for (var watcher : watchers) {
            if (watcher instanceof MTEMultiBlockBase controller && controller.isValid()
                && controller.mDualInputHatches.contains(this)) {
                return controller.getAvailableRecipeMaps()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(map -> map.unlocalizedName)
                    .distinct()
                    .toList();
            }
        }
        return Collections.emptyList();
    }

    public String getTerminalRecipeMap() {
        List<String> maps = getControllerRecipeMaps();
        if (maps.contains(terminalRecipeMap)) return terminalRecipeMap;
        String currentCategory = super.getRecipeMapNameKey();
        for (String name : maps) {
            RecipeMap<?> map = PatternRecipeMap.resolve(name);
            if (map != null && Objects.equals(currentCategory, map.getDefaultRecipeCategory().unlocalizedName))
                return name;
        }
        return maps.isEmpty() ? "" : maps.get(0);
    }

    public void setTerminalRecipeMap(String name) {
        if (getBaseMetaTileEntity() == null || !getBaseMetaTileEntity().isServerSide()
            || !getControllerRecipeMaps().contains(name)) return;
        terminalRecipeMap = name;
        setCustomName(null);
        getBaseMetaTileEntity().markDirty();
    }

    @Override
    protected String getRecipeMapNameKey() {
        RecipeMap<?> selected = PatternRecipeMap.resolve(getTerminalRecipeMap());
        return selected == null ? super.getRecipeMapNameKey() : selected.getDefaultRecipeCategory().unlocalizedName;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings uiSettings) {
        return new CrossRecipeMEPatternBufferGui(this).build(data, syncManager, uiSettings);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        if (!terminalRecipeMap.isEmpty()) tag.setString(TERMINAL_RECIPE_MAP, terminalRecipeMap);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        terminalRecipeMap = tag.getString(TERMINAL_RECIPE_MAP);
    }

    @Override
    public String[] getDescription() {
        // #tr tooltip.gtneverreset.cross_recipe_buffer.routing
        // # Uses the recipe type stored by NEI imports to select the controller's mode for each pattern
        // # zh_CN 根据 NEI 导入时保存的配方池，为每份样板自动选择控制器模式
        String routing = StatCollector.translateToLocal("tooltip.gtneverreset.cross_recipe_buffer.routing");
        // #tr tooltip.gtneverreset.cross_recipe_buffer.support
        // # Uses the controller's supported recipe maps and native mode API; machine requirements still apply
        // # zh_CN 使用控制器支持的配方池及原生模式接口，仍需满足机器加工条件
        String support = StatCollector.translateToLocal("tooltip.gtneverreset.cross_recipe_buffer.support");
        // #tr tooltip.gtneverreset.cross_recipe_buffer.fallback
        // # Unannotated patterns use the current mode; unsupported recipe types stay buffered
        // # zh_CN 未标注配方池的样板使用当前模式；不支持的配方池不会消耗缓存原料
        String fallback = StatCollector.translateToLocal("tooltip.gtneverreset.cross_recipe_buffer.fallback");
        return ArrayUtils.addAll(super.getDescription(), routing, support, fallback);
    }

    private static final class RecipeMapSlot extends CircuitSlot implements RecipeMapPatternInventory {

        private RecipeMapSlot(ItemStack pattern, NBTTagCompound saved, CrossRecipeMEPatternBuffer parent) {
            super(pattern, saved, parent);
        }

        @Override
        public String getRecipeMapName() {
            var details = getPatternDetails();
            return details == null ? null : PatternRecipeMap.name(details.getPattern());
        }
    }
}
