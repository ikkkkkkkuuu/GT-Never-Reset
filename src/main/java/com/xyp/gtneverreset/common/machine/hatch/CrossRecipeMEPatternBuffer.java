package com.xyp.gtneverreset.common.machine.hatch;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import com.xyp.gtneverreset.common.me.PatternMEOutput;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.common.tileentities.machines.IDualInputHatch;

/** Isolated pattern buffers that select one of the receiving controller's native recipe modes. */
public final class CrossRecipeMEPatternBuffer extends CircuitMEPatternBuffer {

    private static final String TERMINAL_RECIPE_MAP = "gtnrTerminalRecipeMap";
    private static final Comparator<MetaTileEntity> POSITION_ORDER = Comparator
        .comparingInt(
            (MetaTileEntity machine) -> machine.getBaseMetaTileEntity()
                .getWorld().provider.dimensionId)
        .thenComparingInt(
            machine -> machine.getBaseMetaTileEntity()
                .getXCoord())
        .thenComparingInt(
            machine -> machine.getBaseMetaTileEntity()
                .getYCoord())
        .thenComparingInt(
            machine -> machine.getBaseMetaTileEntity()
                .getZCoord());
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
        return getConnectedControllers().stream()
            .flatMap(
                controller -> controller.getAvailableRecipeMaps()
                    .stream())
            .filter(Objects::nonNull)
            .map(map -> map.unlocalizedName)
            .distinct()
            .toList();
    }

    private List<MTEMultiBlockBase> getConnectedControllers() {
        var controllers = new LinkedHashSet<MTEMultiBlockBase>();
        for (var watcher : watchers) {
            if (watcher instanceof MTEMultiBlockBase controller && controller.isValid()
                && controller.mDualInputHatches.contains(this)) {
                controllers.add(controller);
            }
        }
        for (var proxy : getProxyHatches()) {
            if (proxy instanceof CrossRecipeMEPatternMirror mirror && mirror.isValid() && mirror.getMaster() == this)
                controllers.addAll(mirror.getConnectedControllers());
        }
        return List.copyOf(controllers);
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

    /** Names each distinct ME provider once, even when several attached mirrors share it. */
    public int autoAssignRecipeMapNames() {
        if (!isValid() || !getBaseMetaTileEntity().isServerSide()) return 0;
        var assigned = new HashSet<CrossRecipeMEPatternBuffer>();
        for (var controller : getConnectedControllers().stream()
            .sorted(POSITION_ORDER)
            .toList()) {
            List<String> maps = controller.getAvailableRecipeMaps()
                .stream()
                .filter(Objects::nonNull)
                .map(map -> map.unlocalizedName)
                .distinct()
                .toList();
            if (maps.isEmpty()) continue;
            List<CrossRecipeMEPatternBuffer> targets = controller.mDualInputHatches.stream()
                .map(CrossRecipeMEPatternBuffer::namingTarget)
                .filter(Objects::nonNull)
                .filter(
                    target -> target.isValid() && target.getConnectedControllers()
                        .contains(controller))
                .distinct()
                .sorted(POSITION_ORDER)
                .toList();
            int[] usage = new int[maps.size()];
            for (var target : targets) {
                int index = maps.indexOf(target.getTerminalRecipeMap());
                if (assigned.contains(target) && index >= 0) usage[index]++;
            }
            for (var target : targets) {
                if (!assigned.add(target)) continue;
                int next = 0;
                for (int index = 1; index < usage.length; index++) {
                    if (usage[index] < usage[next]) next = index;
                }
                target.setTerminalRecipeMap(maps.get(next));
                usage[next]++;
            }
        }
        return assigned.size();
    }

    private static CrossRecipeMEPatternBuffer namingTarget(IDualInputHatch input) {
        if (input instanceof CrossRecipeMEPatternBuffer buffer) return buffer;
        if (input instanceof CrossRecipeMEPatternMirror mirror && mirror.isValid()
            && mirror.getMaster() instanceof CrossRecipeMEPatternBuffer buffer) return buffer;
        return null;
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
        // #tr gtnr.tooltip.gtneverreset.cross_recipe_buffer.routing
        // # Uses the recipe type saved during NEI import to switch the controller to the matching mode
        // # zh_CN 按 NEI 导入时记下的配方类型，自动把控制器切到对应模式
        String routing = StatCollector.translateToLocal("gtnr.tooltip.gtneverreset.cross_recipe_buffer.routing");
        // #tr gtnr.tooltip.gtneverreset.cross_recipe_buffer.support
        // # Only recipe types the controller supports are used; normal machine requirements still apply
        // # zh_CN 只用控制器支持的配方类型；机器原本的加工条件仍然有效
        String support = StatCollector.translateToLocal("gtnr.tooltip.gtneverreset.cross_recipe_buffer.support");
        // #tr gtnr.tooltip.gtneverreset.cross_recipe_buffer.fallback
        // # Patterns without a saved type use the current mode; unsupported types leave ingredients buffered
        // # zh_CN 没记配方类型的样板按当前模式处理；不支持的类型不会动缓存里的原料
        String fallback = StatCollector.translateToLocal("gtnr.tooltip.gtneverreset.cross_recipe_buffer.fallback");
        String[] lines = ArrayUtils.addAll(getBaseDescription(), getCircuitDescription());
        return PatternMEOutput.describe(ArrayUtils.addAll(lines, routing, support, fallback));
    }

    @Override
    protected String getMirrorDescription() {
        // #tr gtnr.tooltip.cross_recipe_buffer.mirror
        // # Only links to Super Cross-Recipe Pattern Input Mirrors (ME)
        // # zh_CN 只能连接超级跨配方类型样板输入镜像 (ME)
        return StatCollector.translateToLocal("gtnr.tooltip.cross_recipe_buffer.mirror");
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
