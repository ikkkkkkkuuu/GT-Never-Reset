package com.xyp.gtneverreset.common.gui.modularui.hatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtnrlib.gui.widget.ScrollingDropdown;

public final class CrossRecipeMEPatternBufferGui extends SuperMTEHatchCraftingInputMEGui {

    private final CrossRecipeMEPatternBuffer hatch;

    public CrossRecipeMEPatternBufferGui(CrossRecipeMEPatternBuffer hatch) {
        super(hatch);
        this.hatch = hatch;
    }

    @Override
    protected int getBasePanelHeight() {
        return super.getBasePanelHeight() + 20;
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        InteractionSyncHandler assign = new InteractionSyncHandler().setOnMousePressed(
            mouse -> { if (!mouse.isClient() && mouse.mouseButton == 0) hatch.autoAssignRecipeMapNames(); });
        ButtonWidget<?> button = new ButtonWidget<>().name("auto_assign_recipe_names")
            .syncHandler(assign)
            .overlay(GuiTextures.EDIT);
        // #tr gui.gtneverreset.cross_recipe_buffer.auto_names
        // # Automatically assign names
        // # zh_CN 自动分配名字
        button.addTooltipLine(StatCollector.translateToLocal("gui.gtneverreset.cross_recipe_buffer.auto_names"));
        // #tr gui.gtneverreset.cross_recipe_buffer.auto_names.description
        // # Cycle recipe names across connected buffers; mirrors share a name. Only terminal names change.
        // # zh_CN 按坐标顺序为同一控制器上的跨配方池总成分配配方池名称，数量超出时循环分配。镜像共用主仓名称；只更改接口终端显示名。
        button.addTooltipLine(
            StatCollector.translateToLocal("gui.gtneverreset.cross_recipe_buffer.auto_names.description"));
        return super.createBottomLeftCornerFlow(panel, syncManager).child(button);
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        GenericListSyncHandler<String> maps = GenericListSyncHandler.<String>builder()
            .getter(hatch::getControllerRecipeMaps)
            .serializer(NetworkUtils::writeStringSafe)
            .deserializer(NetworkUtils::readStringSafe)
            .build();
        StringSyncValue selected = new StringSyncValue(
            () -> "",
            value -> {},
            hatch::getTerminalRecipeMap,
            hatch::setTerminalRecipeMap).allowC2S();
        syncManager.syncValue("terminalRecipeMaps", maps);
        syncManager.syncValue("terminalRecipeMap", selected);
        RecipeMapSelector selector = new RecipeMapSelector(maps, selected);
        // #tr gui.gtneverreset.cross_recipe_buffer.terminal_name
        // # Select the recipe type name shown in the Interface Terminal; processing modes are selected by patterns
        // # zh_CN 选择接口终端显示的配方池名称；加工模式仍由样板自动选择
        selector.addTooltipLine(StatCollector.translateToLocal("gui.gtneverreset.cross_recipe_buffer.terminal_name"));
        return getEmptyContent().child(selector.size(162, 18))
            .child(createSlots(syncManager).top(20));
    }

    private static final class RecipeMapSelector extends ScrollingDropdown {

        private final GenericListSyncHandler<String> maps;
        private final StringSyncValue selected;
        private List<String> ids = Collections.emptyList();
        private List<String> labels = Collections.emptyList();

        private RecipeMapSelector(GenericListSyncHandler<String> maps, StringSyncValue selected) {
            super("terminal_recipe_map", Collections.emptyList());
            this.maps = maps;
            this.selected = selected;
            setMaxCount(6);
            setOnChanged(label -> {
                int index = labels.indexOf(label);
                if (index >= 0) selected.setValue(ids.get(index), true, true);
            });
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            List<String> available = maps.getValue();
            List<String> names = new ArrayList<>();
            for (String id : available) {
                var map = PatternRecipeMap.resolve(id);
                String name = StatCollector
                    .translateToLocal(map == null ? id : map.getDefaultRecipeCategory().unlocalizedName);
                String unique = name;
                for (int suffix = 2; names.contains(unique); suffix++) unique = name + " (" + suffix + ")";
                names.add(unique);
            }
            if (!ids.equals(available) || !labels.equals(names)) {
                ids = new ArrayList<>(available);
                labels = names;
                setCandidates(labels);
            }
            int index = ids.indexOf(selected.getValue());
            // #tr gui.gtneverreset.cross_recipe_buffer.no_controller
            // # No controller recipe types
            // # zh_CN 未连接配方池控制器
            setValue(
                index < 0 ? StatCollector.translateToLocal("gui.gtneverreset.cross_recipe_buffer.no_controller")
                    : labels.get(index));
        }
    }
}
