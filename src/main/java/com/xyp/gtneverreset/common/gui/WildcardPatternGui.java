package com.xyp.gtneverreset.common.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.LongValue;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.me.wildcard.WildcardPatternGenerator;
import com.xyp.gtneverreset.common.me.wildcard.model.IWildcardFilterComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.IWildcardIOComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardComponentCodec;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardExpansion;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardMaterials;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardModelState;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardRecipeTemplate;
import com.xyp.gtneverreset.common.me.wildcard.model.filter.PropertyFilterComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.filter.StringFilterComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.filter.SubTagFilterComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.io.FluidIOComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.io.SimpleIOComponent;
import com.xyp.gtneverreset.compat.ae2fc.FluidDropCompat;
import com.xyp.gtneverreset.config.ScreenThemeConfig;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtnrlib.gui.theme.ButtonPurpose;
import com.xyp.gtnrlib.gui.widget.ItemStackSlot;
import com.xyp.gtnrlib.gui.widget.ScrollingDropdown;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.FluidState;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.SubTag;
import gregtech.api.util.GTUtility;

public class WildcardPatternGui {

    private static final int MAX_IO_COMPONENTS = 6;
    private static final int MAX_FILTER_COMPONENTS = 64;
    private static final int PANEL_W = 196;
    private static final int TAB_WIDTH = 24;
    private static final int PREVIEW_HEIGHT = 80;
    private static final int EDITOR_HEIGHT = 128;
    private static final int PANEL_H = 238;

    private static final UITexture ROW_TEXTURE = UITexture.builder()
        .location(GTNeverReset.MODID, "gui/ldlib/modern/button")
        .imageSize(200, 20)
        .adaptable(3)
        .build();
    private static final IDrawable FIXED_ROW = ROW_TEXTURE.withColorOverride(0xFF337777);
    private static final IDrawable PREFIX_ROW = ROW_TEXTURE.withColorOverride(0xFF9933FF);

    private final int slotIndex;

    private final List<IWildcardIOComponent> inputs = new ArrayList<>();
    private final List<IWildcardIOComponent> outputs = new ArrayList<>();
    private final List<IWildcardFilterComponent> filters = new ArrayList<>();

    private ItemStack backingStack;
    private StringSyncValue configSync;

    private PreviewPage previewPage;
    private ListWidget<IWidget, ?> inputCards;
    private ListWidget<IWidget, ?> outputCards;
    private ListWidget<IWidget, ?> filterCards;
    private final PagedWidget.Controller controller = new PagedWidget.Controller();

    public WildcardPatternGui(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager syncManager, UISettings settings) {
        if (data.isClient()) settings.useTheme(ScreenThemeConfig.wildcardPattern.themeId);
        this.backingStack = data.getUsedItemStack();
        loadFromStack();

        // C2S 配置同步：客户端 getter 序列化工作副本；服务端 setter 解析并写回真实物品。
        // setter 是纯数据写入，不触碰任何 widget/面板结构。
        this.configSync = new StringSyncValue(this::serializeConfig, cfg -> applyConfigOnServer(data, cfg));
        this.configSync.allowC2S();
        syncManager.syncValue("wildcardConfig", this.configSync);
        syncManager.syncValue("wildcardImport", new RecipeImportSyncHandler(this));

        Flow tabs = Flow.column()
            .pos(0, 24)
            .coverChildren()
            // #tr gui.wildcardpattern.preview
            // # Preview
            // # zh_CN 预览
            .child(tabButton(0, new ItemDrawable(iconStack()), "gui.wildcardpattern.preview"))
            // #tr gui.wildcardpattern.inputs
            // # Inputs
            // # zh_CN 输入
            .child(tabButton(1, IKey.str("IN"), "gui.wildcardpattern.inputs"))
            // #tr gui.wildcardpattern.outputs
            // # Outputs
            // # zh_CN 输出
            .child(tabButton(2, IKey.str("OUT"), "gui.wildcardpattern.outputs"))
            // #tr gui.wildcardpattern.filters
            // # Filters
            // # zh_CN 筛选
            .child(tabButton(3, new ItemDrawable(dustOf(Materials.Aluminium)), "gui.wildcardpattern.filters"));
        PagedWidget<?> pages = new PagedWidget<>().controller(controller)
            .pos(7, 23)
            .size(158, PREVIEW_HEIGHT)
            .addPage(buildPreviewPage())
            .addPage(buildIOPage(true))
            .addPage(buildIOPage(false))
            .addPage(buildFilterPage());
        SlotGroupWidget inventory = SlotGroupWidget.playerInventory(6, true, (index, slot) -> {
            if (index == slotIndex) slot.setEnabled(false);
            return slot;
        });
        ParentWidget<?> body = new ParentWidget<>().pos(TAB_WIDTH, 0)
            .width(PANEL_W - TAB_WIDTH)
            .heightRel(1f)
            .widgetTheme(IThemeApi.PANEL)
            .child(
                new ButtonWidget<>().pos(5, 3)
                    .size(17)
                    .overlay(
                        new ItemDrawable(iconStack()).asIcon()
                            .size(12))
                    .tooltipBuilder(tooltip -> tooltip.addLine(IKey.lang("gui.wildcardpattern.preview")))
                    .onMousePressed(mouse -> {
                        controller.setPage(0);
                        return true;
                    }))
            .child(
                // #tr gui.wildcardpattern.reference_title
                // # Wildcard Pattern Configuration
                // # zh_CN 通配符样板配置
                IKey.lang("gui.wildcardpattern.reference_title")
                    .asWidget()
                    .pos(25, 5)
                    .size(142, 12)
                    .tooltipBuilder(
                        tooltip -> tooltip.addLine(
                            // #tr gui.wildcardpattern.nei_hint
                            // # Use NEI + to import a recipe, then save
                            // # zh_CN 点击 NEI 配方的＋导入，调整后保存
                            IKey.lang("gui.wildcardpattern.nei_hint"))))
            .child(pages)
            .child(inventory);
        ModularPanel panel = new ModularPanel("wildcard_pattern") {

            @Override
            @SideOnly(Side.CLIENT)
            public void postResize() {
                super.postResize();
                // NEI reads the container bounds when placing its search bar and category selector.
                getScreen().getScreenWrapper()
                    .updateGuiArea(getArea());
            }
        }.size(PANEL_W, PANEL_H - EDITOR_HEIGHT + PREVIEW_HEIGHT)
            .disableThemeBackground(true)
            .disableHoverBackground()
            .child(body)
            .child(tabs);
        pages.onPageChange(page -> {
            int height = page == 0 ? PREVIEW_HEIGHT : EDITOR_HEIGHT;
            panel.height(PANEL_H - EDITOR_HEIGHT + height);
            pages.height(height);
            panel.scheduleResize();
        });
        return panel;
    }

    public boolean importRecipe(List<ItemStack> recipeInputs, List<ItemStack> recipeOutputs) {
        WildcardRecipeTemplate template;
        try {
            template = WildcardRecipeTemplate.create(recipeInputs, recipeOutputs);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
        inputs.clear();
        inputs.addAll(template.getInputs());
        outputs.clear();
        outputs.addAll(template.getOutputs());
        rebuildIOCards(inputCards, inputs);
        rebuildIOCards(outputCards, outputs);
        previewPage.refresh(WildcardExpansion.expand(inputs, outputs, filters));
        controller.setPage(1);
        return true;
    }

    private PageButton tabButton(int index, IDrawable icon, String key) {
        return new PageButton(index, controller).size(TAB_WIDTH, 26)
            .marginBottom(2)
            .overlay(
                icon.asIcon()
                    .size(icon instanceof IKey ? 22 : 16, 16))
            .tooltipBuilder(tooltip -> tooltip.addLine(IKey.lang(key)));
    }

    private ItemStack iconStack() {
        return backingStack != null ? backingStack.copy() : GTNRItemList.WildcardPattern.get(1);
    }

    private IWidget buildPreviewPage() {
        previewPage = new PreviewPage(this::excludePreviewMaterial);
        previewPage.refresh(WildcardExpansion.expand(inputs, outputs, filters));
        return previewPage;
    }

    /** Adds an exact-name blacklist entry once, saves through the existing C2S path and refreshes both views. */
    private void excludePreviewMaterial(String materialName) {
        if (materialName == null) return;
        for (IWildcardFilterComponent filter : filters) {
            if (filter instanceof StringFilterComponent) {
                StringFilterComponent name = (StringFilterComponent) filter;
                if (name.isExact() && !name.isWhitelist() && materialName.equalsIgnoreCase(name.getPattern())) return;
            }
        }
        filters.add(StringFilterComponent.exactBlacklist(materialName));
        pushConfig();
        if (filterCards != null) rebuildFilterCards(filterCards);
    }

    private static String safeName(ItemStack stack) {
        try {
            return stack.getDisplayName();
        } catch (RuntimeException ignored) {
            return String.valueOf(stack.getItem());
        }
    }

    private IWidget buildIOPage(boolean input) {
        List<IWildcardIOComponent> list = input ? inputs : outputs;
        ListWidget<IWidget, ?> cards = new ListWidget<>().pos(0, 3)
            .size(158, EDITOR_HEIGHT - 26);
        if (input) inputCards = cards;
        else outputCards = cards;
        rebuildIOCards(cards, list);
        return new ParentWidget<>().size(158, EDITOR_HEIGHT)
            .child(cards)
            .child(
                // #tr gui.wildcardpattern.single
                // # Single
                // # zh_CN 单一
                addButton("gui.wildcardpattern.single", () -> addIO(cards, list, SimpleIOComponent.empty())).width(34)
                    .pos(0, EDITOR_HEIGHT - 20))
            .child(
                // #tr gui.wildcardpattern.tag
                // # Tag
                // # zh_CN 标签
                addButton("gui.wildcardpattern.tag", () -> addIO(cards, list, PrefixIOComponent.empty())).width(45)
                    .pos(37, EDITOR_HEIGHT - 20))
            .child(
                // #tr gui.wildcardpattern.fluid
                // # Fluid
                // # zh_CN 流体
                addButton("gui.wildcardpattern.fluid", () -> addIO(cards, list, FluidIOComponent.empty()))
                    .pos(84, EDITOR_HEIGHT - 20))
            .child(saveButton().pos(126, EDITOR_HEIGHT - 20));
    }

    private void addIO(ListWidget<IWidget, ?> cards, List<IWildcardIOComponent> list, IWildcardIOComponent component) {
        if (list.size() < MAX_IO_COMPONENTS) {
            list.add(component);
            rebuildIOCards(cards, list);
        }
    }

    private void rebuildIOCards(ListWidget<IWidget, ?> cards, List<IWildcardIOComponent> list) {
        cards.removeAll();
        for (int i = 0; i < list.size(); i++) {
            cards.child(ioCard(cards, list, i));
        }
    }

    private IWidget ioCard(ListWidget<IWidget, ?> cards, List<IWildcardIOComponent> list, int index) {
        IWildcardIOComponent component = list.get(index);
        ParentWidget<?> row = new ParentWidget<>().size(152, 25)
            .background(component instanceof PrefixIOComponent ? PREFIX_ROW : FIXED_ROW);
        if (component instanceof PrefixIOComponent prefix) row.child(prefixEditor(prefix));
        else if (component instanceof SimpleIOComponent simple) row.child(simpleEditor(simple));
        else if (component instanceof FluidIOComponent fluid) row.child(fluidEditor(fluid).pos(22, 4));
        row.child(deleteButton(() -> {
            if (index >= 0 && index < list.size()) {
                list.remove(index);
                rebuildIOCards(cards, list);
            }
        }).pos(136, 5));
        return row;
    }

    /** 前缀组件编辑：拖入槽(自动识别前缀) + 前缀名文本框 + 数量数字框。 */
    private IWidget prefixEditor(PrefixIOComponent component) {
        DropWidget drop = new DropWidget(component::getDisplayStack, stack -> {
            WildcardMaterials.PrefixMaterial pm = WildcardMaterials.parseItem(stack);
            if (pm.prefix != null) component.setPrefix(pm.prefix);
        });
        TextFieldWidget name = new TextFieldWidget().pos(25, 5)
            .size(43, 15)
            .value(new StringValue.Dynamic(component::getRawText, component::setRawText))
            .setPattern(Pattern.compile("[a-zA-Z0-9]*"));
        return ioEditor(
            drop,
            name,
            component::getAmount,
            val -> component.setAmount((int) Math.max(1, Math.min(Integer.MAX_VALUE, val))));
    }

    /** 固定组件编辑：拖入槽(放固定物品或固定流体) + 名称 + 数量数字框。布局与前缀组件对齐。 */
    private IWidget simpleEditor(SimpleIOComponent component) {
        DropWidget drop = new DropWidget(component::getStack, stack -> component.setStack(normalizeFixedStack(stack)));
        IWidget name = new ScrollingTextWidget(
            IKey.dynamic(() -> component.getStack() == null ? "" : safeName(component.getStack()))).color(0xFFFFFFFF)
                .shadow(true)
                .pos(25, 7)
                .size(43, 10);
        return ioEditor(drop, name, component::getAmount, component::setAmount);
    }

    private ParentWidget<?> ioEditor(DropWidget drop, IWidget name, LongSupplier amount, LongConsumer setAmount) {
        return new ParentWidget<>().size(136, 25)
            .child(drop.pos(3, 3))
            .child(name)
            .child(
                IKey.str("x")
                    .color(0xFFFFFFFF)
                    .shadow(true)
                    .asWidget()
                    .pos(70, 7)
                    .size(8, 10))
            .child(
                new TextFieldWidget().pos(80, 5)
                    .size(50, 15)
                    .value(new LongValue.Dynamic(amount, setAmount))
                    .numbersLong(1, Integer.MAX_VALUE));
    }

    /**
     * 归一化固定组件放入的物品：若是 GT 流体显示物品，转成 AE2FC 的 ItemFluidDrop（合成 CPU 能识别的流体请求）；
     * 否则原样返回。这让"+固定"既能放固定物品也能放固定流体（如熔融橡胶）。
     */
    private static ItemStack normalizeFixedStack(ItemStack stack) {
        if (stack == null) return null;
        FluidStack fluid = GTUtility.getFluidFromDisplayStack(stack);
        if (fluid != null && fluid.getFluid() != null) {
            if (fluid.amount <= 0) fluid.amount = 1000;
            // [液滴分类] 必须留液滴：把固定流体转成 ItemFluidDrop 存入样板，合成 CPU 才能识别为流体请求
            return FluidDropCompat.newStack(fluid);
        }
        return stack;
    }

    /** 流体组件编辑：状态循环按钮 + 数量文本框。 */
    private Flow fluidEditor(FluidIOComponent component) {
        ButtonWidget<?> stateButton = new ButtonWidget<>().size(58, 16)
            .marginLeft(2)
            .overlay(
                IKey.dynamic(
                    () -> component.getState() == null ? "?"
                        : component.getState()
                            .name()))
            .onMousePressed(button -> {
                component.setState(nextFluidState(component.getState()));
                return true;
            });
        TextFieldWidget amountField = new TextFieldWidget().size(40, 16)
            .marginLeft(3)
            .value(new LongValue.Dynamic(() -> component.getAmount(), component::setAmount))
            .numbersLong(1, Integer.MAX_VALUE);
        return Flow.row()
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(stateButton)
            .child(amountField);
    }

    private static FluidState nextFluidState(FluidState current) {
        FluidState[] order = { FluidState.MOLTEN, FluidState.LIQUID, FluidState.GAS, FluidState.PLASMA };
        int idx = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i] == current) {
                idx = i;
                break;
            }
        }
        return order[(idx + 1) % order.length];
    }

    private IWidget buildFilterPage() {
        ListWidget<IWidget, ?> cards = new ListWidget<>().pos(0, 3)
            .size(158, EDITOR_HEIGHT - 26)
            .scrollDirection(new VerticalScrollData(false, 4));
        filterCards = cards;
        rebuildFilterCards(cards);
        return new ParentWidget<>().size(158, EDITOR_HEIGHT)
            .child(cards)
            .child(
                // #tr gui.wildcardpattern.filter_name
                // # Name
                // # zh_CN 名称
                addButton("gui.wildcardpattern.filter_name", () -> addFilter(cards, StringFilterComponent.empty()))
                    .pos(2, EDITOR_HEIGHT - 20))
            .child(
                // #tr gui.wildcardpattern.filter_property
                // # Property
                // # zh_CN 属性
                addButton(
                    "gui.wildcardpattern.filter_property",
                    () -> addFilter(cards, PropertyFilterComponent.empty())).width(45)
                        .pos(37, EDITOR_HEIGHT - 20))
            .child(
                // #tr gui.wildcardpattern.filter_subtag
                // # Flag
                // # zh_CN 标记
                addButton("gui.wildcardpattern.filter_subtag", () -> addFilter(cards, SubTagFilterComponent.empty()))
                    .pos(84, EDITOR_HEIGHT - 20))
            .child(saveButton().pos(126, EDITOR_HEIGHT - 20));
    }

    private void addFilter(ListWidget<IWidget, ?> cards, IWildcardFilterComponent component) {
        if (filters.size() < MAX_FILTER_COMPONENTS) {
            filters.add(component);
            rebuildFilterCards(cards);
        }
    }

    private void rebuildFilterCards(ListWidget<IWidget, ?> cards) {
        cards.removeAll();
        for (int i = 0; i < filters.size(); i++) {
            cards.child(filterCard(cards, i));
        }
    }

    private IWidget filterCard(ListWidget<IWidget, ?> cards, int index) {
        IWildcardFilterComponent component = filters.get(index);
        ParentWidget<?> row = new ParentWidget<>().size(152, 25);
        if (component instanceof PropertyFilterComponent property) {
            row.background(filterBackground(component, 0xFFFFFF33, 0xFFFF8800))
                .child(propertyEditor(property));
        } else if (component instanceof SubTagFilterComponent flag) {
            row.background(filterBackground(component, 0xFFFF33FF, 0xFF9933FF))
                .child(subTagEditor(flag));
        } else {
            row.background(filterBackground(component, 0xFF44AAFF, 0xFF4852FF));
            if (component instanceof StringFilterComponent name) row.child(stringFilterEditor(name));
            else row.child(
                new ScrollingTextWidget(IKey.dynamic(component::describe)).pos(3, 6)
                    .size(108, 13));
        }
        row.child(
            whitelistButton(component).pos(118, 5)
                .size(14));
        row.child(deleteButton(() -> {
            if (index >= 0 && index < filters.size()) {
                filters.remove(index);
                rebuildFilterCards(cards);
            }
        }).pos(136, 5));
        return row;
    }

    private static IDrawable filterBackground(IWildcardFilterComponent component, int whitelist, int blacklist) {
        IDrawable allowed = ROW_TEXTURE.withColorOverride(whitelist);
        IDrawable denied = ROW_TEXTURE.withColorOverride(blacklist);
        return new DynamicDrawable(() -> component.isWhitelist() ? allowed : denied);
    }

    private IWidget stringFilterEditor(StringFilterComponent component) {
        return new TextFieldWidget().pos(3, 5)
            .size(106, 15)
            .value(new StringValue.Dynamic(component::getPattern, component::setPattern));
    }

    /** Source selector: changing the example replaces candidates and selects the first available property. */
    private IWidget propertyEditor(PropertyFilterComponent component) {
        ScrollingDropdown selector = new ScrollingDropdown(
            "wildcard_property_" + filters.indexOf(component),
            propertyNames(component.getExample()))
                .setValue(
                    component.getProperty() == null ? ""
                        : component.getProperty()
                            .displayName())
                .setOnChanged(name -> component.setProperty(WildcardMaterials.findProperty(name)));
        DropWidget drop = new DropWidget(
            () -> component.getExample() == null ? null : dustOf(component.getExample()),
            stack -> {
                Materials material = WildcardMaterials.parseItem(stack).material;
                if (!WildcardMaterials.isRealMaterial(material)) return;
                component.setExample(material);
                List<String> names = propertyNames(material);
                selector.setCandidates(names);
                selector.setValue(names.get(0));
                component.setProperty(WildcardMaterials.findProperty(names.get(0)));
            });
        return filterSelector(drop, selector);
    }

    private static List<String> propertyNames(Materials material) {
        List<String> names = new ArrayList<>();
        if (WildcardMaterials.isRealMaterial(material)) {
            for (WildcardMaterials.Property property : WildcardMaterials.propertiesOf(material))
                names.add(property.displayName());
        }
        if (names.isEmpty()) {
            // #tr gui.wildcardpattern.no_property
            // # no property
            // # zh_CN 无属性
            names.add(
                IKey.lang("gui.wildcardpattern.no_property")
                    .get());
        }
        return names;
    }

    /** Example material icon used by both dropdown editors. */
    private static ItemStack dustOf(Materials material) {
        return WildcardMaterials.makePrefixStack(OrePrefixes.dust, material, 1);
    }

    /** Flags use 1.7.10 SubTags with the original source's candidate replacement interaction. */
    private IWidget subTagEditor(SubTagFilterComponent component) {
        ScrollingDropdown selector = new ScrollingDropdown(
            "wildcard_subtag_" + filters.indexOf(component),
            subTagNames(component.getExample()))
                .setValue(component.getSubTag() == null ? "" : component.getSubTag().mName)
                .setOnChanged(name -> component.setSubTag(WildcardMaterials.findSubTag(name)));
        DropWidget drop = new DropWidget(
            () -> component.getExample() == null ? null : dustOf(component.getExample()),
            stack -> {
                Materials material = WildcardMaterials.parseItem(stack).material;
                if (!WildcardMaterials.isRealMaterial(material)) return;
                component.setExample(material);
                List<String> names = subTagNames(material);
                selector.setCandidates(names);
                selector.setValue(names.get(0));
                component.setSubTag(WildcardMaterials.findSubTag(names.get(0)));
            });
        return filterSelector(drop, selector);
    }

    private IWidget filterSelector(DropWidget drop, ScrollingDropdown selector) {
        return new ParentWidget<>().size(110, 25)
            .child(drop.pos(3, 3))
            .child(
                selector.setMaxCount(4)
                    .pos(25, 5)
                    .width(80));
    }

    private static List<String> subTagNames(Materials material) {
        List<String> names = new ArrayList<>();
        if (WildcardMaterials.isRealMaterial(material)) {
            for (SubTag tag : WildcardMaterials.subTagsOf(material)) names.add(tag.mName);
        }
        if (names.isEmpty()) {
            // #tr gui.wildcardpattern.no_flag
            // # no flag
            // # zh_CN 无标记
            names.add(
                IKey.lang("gui.wildcardpattern.no_flag")
                    .get());
        }
        return names;
    }

    private ButtonWidget<?> whitelistButton(IWildcardFilterComponent component) {
        // #tr gui.wildcardpattern.whitelist
        // # Whitelist
        // # zh_CN 白名单
        IKey whitelist = IKey.lang("gui.wildcardpattern.whitelist");
        // #tr gui.wildcardpattern.blacklist
        // # Blacklist
        // # zh_CN 黑名单
        IKey blacklist = IKey.lang("gui.wildcardpattern.blacklist");
        return new ButtonWidget<>().overlay(IKey.dynamic(() -> component.isWhitelist() ? "W" : "B"))
            .tooltipBuilder(
                tooltip -> tooltip.setAutoUpdate(true)
                    .addLine(component.isWhitelist() ? whitelist : blacklist))
            .onMousePressed(button -> {
                component.setWhitelist(!component.isWhitelist());
                return true;
            });
    }

    private ButtonWidget<?> addButton(String langKey, Runnable action) {
        return new ButtonWidget<>().size(30, 20)
            .overlay(IKey.lang(langKey))
            .onMousePressed(button -> {
                action.run();
                return true;
            });
    }

    private ButtonWidget<?> deleteButton(Runnable action) {
        return new ButtonWidget<>().size(14)
            .overlay(GuiTextures.CLOSE)
            .onMousePressed(button -> {
                action.run();
                return true;
            });
    }

    private ButtonWidget<?> saveButton() {
        return ButtonPurpose.PRIMARY.apply(new ButtonWidget<>())
            .size(30, 20)
            // #tr gui.wildcardpattern.save
            // # Save
            // # zh_CN 保存
            .overlay(IKey.lang("gui.wildcardpattern.save"))
            .onMousePressed(button -> {
                pushConfig();
                return true;
            });
    }

    /** 把当前工作副本推送到服务端（C2S），并同步写回本地物品副本（tooltip/预览即时反映）。 */
    private void pushConfig() {
        if (configSync != null) {
            configSync.setValue(serializeConfig(), true, true);
        }
        List<WildcardExpansion.Expanded> expanded = WildcardExpansion.expand(inputs, outputs, filters);
        saveToLocalStack(expanded.size());
        if (previewPage != null) previewPage.refresh(expanded);
    }

    private void saveToLocalStack(int expandedCount) {
        if (backingStack == null) return;
        WildcardModelState.ensureInitialized(backingStack);
        WildcardModelState.setInputs(backingStack, inputs);
        WildcardModelState.setOutputs(backingStack, outputs);
        WildcardModelState.setFilters(backingStack, filters);
        WildcardModelState.setExpandedCount(backingStack, expandedCount);
    }

    private String serializeConfig() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(WildcardModelState.KEY_INPUT, WildcardComponentCodec.writeIO(inputs));
        tag.setTag(WildcardModelState.KEY_OUTPUT, WildcardComponentCodec.writeIO(outputs));
        tag.setTag(WildcardModelState.KEY_FILTER, WildcardComponentCodec.writeFilters(filters));
        return tag.toString();
    }

    /** 服务端 setter：解析配置并写回真实物品。纯数据写入，不触碰面板结构。 */
    private void applyConfigOnServer(PlayerInventoryGuiData data, String cfg) {
        if (data.getPlayer().worldObj.isRemote || cfg == null || cfg.isEmpty()) return;
        NBTTagCompound parsed;
        try {
            parsed = (NBTTagCompound) JsonToNBT.func_150315_a(cfg);
        } catch (NBTException e) {
            return;
        }
        ItemStack stack = data.getUsedItemStack();
        if (stack == null) return;
        WildcardPatternGenerator.markAsWildcard(stack);
        List<IWildcardIOComponent> in = WildcardComponentCodec.readIO(parsed, WildcardModelState.KEY_INPUT);
        List<IWildcardIOComponent> out = WildcardComponentCodec.readIO(parsed, WildcardModelState.KEY_OUTPUT);
        List<IWildcardFilterComponent> fil = WildcardComponentCodec.readFilters(parsed, WildcardModelState.KEY_FILTER);
        WildcardModelState.ensureInitialized(stack);
        WildcardModelState.setInputs(stack, in);
        WildcardModelState.setOutputs(stack, out);
        WildcardModelState.setFilters(stack, fil);
        WildcardModelState.setExpandedCount(stack, WildcardExpansion.countExpanded(in, out, fil));
        data.getPlayer().inventory.markDirty();
    }

    private void loadFromStack() {
        inputs.clear();
        outputs.clear();
        filters.clear();
        if (backingStack == null) return;
        WildcardPatternGenerator.markAsWildcard(backingStack);
        inputs.addAll(WildcardModelState.getInputs(backingStack));
        outputs.addAll(WildcardModelState.getOutputs(backingStack));
        filters.addAll(WildcardModelState.getFilters(backingStack));
    }

    public static final class RecipeImportSyncHandler extends SyncHandler<RecipeImportSyncHandler> {

        private final WildcardPatternGui gui;

        private RecipeImportSyncHandler(WildcardPatternGui gui) {
            this.gui = gui;
        }

        public boolean importRecipe(List<ItemStack> inputs, List<ItemStack> outputs) {
            return isValid() && getSyncManager().isClient() && gui.importRecipe(inputs, outputs);
        }

        @Override
        public void readOnClient(int id, PacketBuffer buf) {}

        @Override
        public void readOnServer(int id, PacketBuffer buf) {}
    }

    public static final class DropWidget extends ItemStackSlot {

        private DropWidget(Supplier<ItemStack> getter, Consumer<ItemStack> onDrop) {
            super(() -> WildcardMaterials.toDisplayStack(getter == null ? null : getter.get()));
            onDrop(onDrop);
        }
    }

    /**
     * Source Wildcard Pattern index page: a 158x80 preview with two 3x2 slot groups.
     * Advances once per 20 active client updates. Slots are display-only and never consume items.
     * Refresh receives a committed preview snapshot, so editing another tab does not mutate it before saving.
     */
    private static final class PreviewPage extends ParentWidget<PreviewPage> {

        private List<WildcardExpansion.Expanded> allPatterns = Collections.emptyList();
        private List<WildcardExpansion.Expanded> patterns = Collections.emptyList();
        private String search = "";
        private int ticks, index;

        private PreviewPage(Consumer<String> excludeMaterial) {
            size(158, PREVIEW_HEIGHT);
            child(
                // #tr gui.wildcardpattern.available_count
                // # Available patterns: %s
                // # zh_CN 可用样板：%s
                IKey.dynamic(
                    () -> IKey.lang("gui.wildcardpattern.available_count", patterns.size())
                        .get())
                    .asWidget()
                    .pos(2, 2)
                    .size(136, 15));
            child(
                new ButtonWidget<>().pos(140, 2)
                    .size(14)
                    .overlay(GuiTextures.CLOSE)
                    .onUpdateListener(button -> button.setEnabled(currentMaterialName() != null))
                    .tooltipBuilder(
                        tooltip -> tooltip.addLine(
                            // #tr gui.wildcardpattern.exclude_current
                            // # Add this material to the blacklist and save
                            // # zh_CN 将当前材料加入黑名单并保存
                            IKey.lang("gui.wildcardpattern.exclude_current")))
                    .onMousePressed(mouse -> {
                        if (mouse != 0) return false;
                        String materialName = currentMaterialName();
                        if (materialName != null) excludeMaterial.accept(materialName);
                        return true;
                    }));
            for (int i = 0; i < 6; i++) {
                final int slot = i;
                child(
                    new ItemStackSlot(() -> WildcardMaterials.toDisplayStack(stack(true, slot))).itemTooltip()
                        .pos(10 + i % 3 * 18, 18 + i / 3 * 18));
                child(
                    new ItemStackSlot(() -> WildcardMaterials.toDisplayStack(stack(false, slot))).itemTooltip()
                        .pos(94 + i % 3 * 18, 18 + i / 3 * 18));
            }
            child(
                IKey.str(">")
                    .asWidget()
                    .pos(74, 32)
                    .size(15, 10));
            child(
                // #tr gui.wildcardpattern.preview_search
                // # Search
                // # zh_CN 搜索
                IKey.lang("gui.wildcardpattern.preview_search")
                    .asWidget()
                    .pos(2, 61)
                    .size(28, 12));
            child(
                new TextFieldWidget().pos(32, 59)
                    .size(124, 16)
                    .autoUpdateOnChange(true)
                    .value(new StringValue.Dynamic(() -> search, text -> {
                        search = text == null ? "" : text;
                        filterPatterns();
                    })));
        }

        private void refresh(List<WildcardExpansion.Expanded> snapshot) {
            allPatterns = new ArrayList<>(snapshot);
            filterPatterns();
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (areAncestorsEnabled() && !patterns.isEmpty() && ++ticks >= 20) {
                ticks = 0;
                index = (index + 1) % patterns.size();
            }
        }

        /**
         * Searches internal and client-localized material names in the cached snapshot.
         * Typing never reruns recipe expansion or changes item data.
         */
        private void filterPatterns() {
            String query = search.trim()
                .toLowerCase(Locale.ROOT);
            List<WildcardExpansion.Expanded> matches = new ArrayList<>();
            for (WildcardExpansion.Expanded pattern : allPatterns) {
                String materialName = pattern.materialName;
                Materials material = WildcardMaterials.findByName(materialName);
                if (query.isEmpty() || materialName.toLowerCase(Locale.ROOT)
                    .contains(query)
                    || (WildcardMaterials.isRealMaterial(material) && material.getLocalizedName()
                        .toLowerCase(Locale.ROOT)
                        .contains(query)))
                    matches.add(pattern);
            }
            patterns = matches;
            ticks = 0;
            index = 0;
        }

        private ItemStack stack(boolean input, int slot) {
            if (patterns.isEmpty()) return null;
            WildcardExpansion.Expanded pattern = patterns.get(index);
            List<ItemStack> stacks = input ? pattern.inputs : pattern.outputs;
            return slot < stacks.size() ? stacks.get(slot) : null;
        }

        private String currentMaterialName() {
            return patterns.isEmpty() ? null : patterns.get(index).materialName;
        }

    }

}
