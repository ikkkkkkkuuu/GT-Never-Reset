package com.xyp.gtneverreset.common.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.MCHelper;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerGhostIngredientSlot;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.LongValue;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.menu.AbstractMenuButton;
import com.cleanroommc.modularui.widgets.menu.Menu;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.ModList;
import com.xyp.gtneverreset.utils.inventory.FluidDropCompat;
import com.xyp.gtneverreset.utils.recipes.wildcard.WildcardPatternGenerator;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.IWildcardFilterComponent;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.IWildcardIOComponent;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.WildcardComponentCodec;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.WildcardExpansion;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.WildcardMaterials;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.WildcardModelState;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.WildcardRecipeTemplate;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.filter.PropertyFilterComponent;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.filter.StringFilterComponent;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.filter.SubTagFilterComponent;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.io.FluidIOComponent;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.io.SimpleIOComponent;

import gregtech.api.enums.FluidState;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.SubTag;
import gregtech.api.util.GTUtility;

public class WildcardPatternGui {

    private static final int MAX_IO_COMPONENTS = 6;
    private static final int MAX_FILTER_COMPONENTS = 64;
    private static final int PANEL_W = 192;
    private static final int PANEL_H = 292;

    private static final Theme THEME = Theme.INSTANCE;

    private static final IDrawable REFERENCE_PURPLE = THEME.button.withColorOverride(0xFF9933FF);

    private static final IDrawable REFERENCE_CYAN = THEME.button.withColorOverride(0xFF337777);

    private static final IDrawable REFERENCE_CLOSE = UITexture.builder()
        .location(ModList.ModIds.GT_NEVER_RESET, "gui/ldlib/modern/close")
        .build();

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
            .size(24, 180)
            .child(tabButton(controller, 0, new ItemDrawable(iconStack())))
            .child(
                tabButton(
                    controller,
                    1,
                    IKey.str("IN")
                        .color(0xFFFFFFFF)
                        .shadow(true)))
            .child(
                tabButton(
                    controller,
                    2,
                    IKey.str("OUT")
                        .color(0xFFFFFFFF)
                        .shadow(true)))
            .child(tabButton(controller, 3, new ItemDrawable(dustOf(Materials.Aluminium))));
        PagedWidget<?> pages = new PagedWidget<>().controller(controller)
            .pos(27, 23)
            .size(158, 180)
            .addPage(buildPreviewPage())
            .addPage(buildIOPage(true))
            .addPage(buildIOPage(false))
            .addPage(buildFilterPage());
        IDrawable shell = (context, x, y, w, h, style) -> {
            THEME.panel.draw(context, x + 20, y + 16, 172, h - 16, style);
            THEME.panel.draw(context, x + 28, y + 3, 18, 15, style);
            THEME.panel.draw(context, x + 46, y, 138, 18, style);
        };
        SlotGroupWidget inventory = SlotGroupWidget.playerInventory((index, slot) -> {
            slot.background(THEME.slot);
            if (index == slotIndex) slot.setEnabled(false);
            return slot;
        })
            .pos(25, 110);
        ModularPanel panel = ModularPanel.defaultPanel("wildcard_pattern", PANEL_W, PANEL_H - 100)
            .background(shell)
            .disableHoverBackground()
            .child(
                THEME.button()
                    .pos(28, 5)
                    .size(18, 12)
                    .background((IDrawable) null)
                    .overlay(
                        IKey.str("<")
                            .color(0xFF000000))
                    .onMousePressed(mouse -> {
                        controller.setPage(0);
                        return true;
                    }))
            .child(
                new ItemDrawable(iconStack()).asWidget()
                    .pos(49, 3)
                    .size(12))
            .child(
                // #tr gui.wildcardpattern.reference_title
                // # Wildcard Pattern Configuration
                // # zh_CN 通配符样板配置
                IKey.lang("gui.wildcardpattern.reference_title")
                    .asWidget()
                    .pos(64, 4)
                    .size(117, 10)
                    .tooltipBuilder(
                        tooltip -> tooltip.addLine(
                            // #tr gui.wildcardpattern.nei_hint
                            // # Use NEI + to import a recipe, then save
                            // # zh_CN 点击 NEI 配方的＋导入，调整后保存
                            IKey.lang("gui.wildcardpattern.nei_hint"))))
            .child(pages)
            .child(tabs)
            .child(inventory);
        pages.onPageChange(page -> {
            int offset = page == 0 ? 100 : 0;
            panel.height(PANEL_H - offset);
            pages.height(180 - offset);
            inventory.top(210 - offset);
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

    private PageButton tabButton(PagedWidget.Controller controller, int index, IDrawable icon) {
        IDrawable padded = (context, x, y, w, h, style) -> {
            if (icon instanceof IKey) icon.draw(context, x + 2, y + 4, w - 4, h - 8, style);
            else icon.draw(context, x + 3, y + 4, w - 7, h - 8, style);
        };
        return new PageButton(index, controller).size(24, 26)
            .background(false, THEME.tab, padded)
            .background(true, THEME.selectedTab, padded)
            .marginBottom(2);
    }

    private ItemStack iconStack() {
        return backingStack != null ? backingStack.copy() : GTNRItemList.WildcardPattern.get(1);
    }

    // ============================================================
    // 主页/预览：源版双槽组与每秒轮播
    // ============================================================

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

    // ============================================================
    // 输入/输出页
    // ============================================================

    private IWidget buildIOPage(boolean input) {
        List<IWildcardIOComponent> list = input ? inputs : outputs;
        ListWidget<IWidget, ?> cards = new ListWidget<>().pos(0, 3)
            .size(156, 150);
        if (input) inputCards = cards;
        else outputCards = cards;
        rebuildIOCards(cards, list);
        return new ParentWidget<>().size(158, 180)
            .child(cards)
            .child(
                // #tr gui.wildcardpattern.single
                // # Single
                // # zh_CN 单一
                addButton("gui.wildcardpattern.single", () -> addIO(cards, list, SimpleIOComponent.empty()))
                    .pos(2, 155))
            .child(
                // #tr gui.wildcardpattern.tag
                // # Tag
                // # zh_CN 标签
                addButton("gui.wildcardpattern.tag", () -> addIO(cards, list, PrefixIOComponent.empty())).pos(37, 155))
            .child(
                // #tr gui.wildcardpattern.fluid
                // # Fluid
                // # zh_CN 流体
                addButton("gui.wildcardpattern.fluid", () -> addIO(cards, list, FluidIOComponent.empty())).pos(72, 155))
            .child(saveButton().pos(126, 155));
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
        ParentWidget<?> row = new ParentWidget<>().size(156, 25)
            .background(component instanceof PrefixIOComponent ? REFERENCE_PURPLE : REFERENCE_CYAN);
        if (component instanceof PrefixIOComponent) row.child(prefixEditor((PrefixIOComponent) component));
        else if (component instanceof SimpleIOComponent) row.child(simpleEditor((SimpleIOComponent) component));
        else if (component instanceof FluidIOComponent) row.child(fluidEditor((FluidIOComponent) component).pos(22, 4));
        row.child(deleteButton(() -> {
            if (index >= 0 && index < list.size()) {
                list.remove(index);
                rebuildIOCards(cards, list);
            }
        }).marginLeft(0)
            .pos(138, 5));
        return row;
    }

    /** 前缀组件编辑：拖入槽(自动识别前缀) + 前缀名文本框 + 数量数字框。 */
    private IWidget prefixEditor(PrefixIOComponent component) {
        DropWidget drop = new DropWidget(component::getDisplayStack, stack -> {
            WildcardMaterials.PrefixMaterial pm = WildcardMaterials.parseItem(stack);
            if (pm.prefix != null) component.setPrefix(pm.prefix);
        });
        TextFieldWidget name = THEME.textField()
            .pos(25, 5)
            .size(43, 15)
            .background((IDrawable) null)
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
                new TextFieldWidget().setTextColor(0xFFFFFFFF)
                    .pos(80, 5)
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
        ButtonWidget<?> stateButton = THEME.button()
            .size(58, 16)
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
        TextFieldWidget amountField = THEME.textField()
            .size(40, 16)
            .marginLeft(3)
            .setTextColor(0xFFFFFFFF)
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

    // ============================================================
    // 过滤页
    // ============================================================

    private IWidget buildFilterPage() {
        ListWidget<IWidget, ?> cards = new ListWidget<>().pos(0, 3)
            .size(156, 150)
            .scrollDirection(new VerticalScrollData(false, 4).texture(new Rectangle().color(0xFFAAAAAA)));
        filterCards = cards;
        rebuildFilterCards(cards);
        return new ParentWidget<>().size(158, 180)
            .child(cards)
            .child(
                // #tr gui.wildcardpattern.filter_name
                // # Name
                // # zh_CN 名称
                addButton("gui.wildcardpattern.filter_name", () -> addFilter(cards, StringFilterComponent.empty()))
                    .pos(2, 155))
            .child(
                // #tr gui.wildcardpattern.filter_property
                // # Property
                // # zh_CN 属性
                addButton(
                    "gui.wildcardpattern.filter_property",
                    () -> addFilter(cards, PropertyFilterComponent.empty())).pos(37, 155))
            .child(
                // #tr gui.wildcardpattern.filter_subtag
                // # Flag
                // # zh_CN 标记
                addButton("gui.wildcardpattern.filter_subtag", () -> addFilter(cards, SubTagFilterComponent.empty()))
                    .pos(72, 155))
            .child(saveButton().pos(126, 155));
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
        ParentWidget<?> row = new ParentWidget<>().size(152, 25)
            .background(filterBackground(component));
        if (component instanceof StringFilterComponent)
            row.child(stringFilterEditor((StringFilterComponent) component));
        else if (component instanceof PropertyFilterComponent)
            row.child(propertyEditor((PropertyFilterComponent) component));
        else if (component instanceof SubTagFilterComponent) row.child(subTagEditor((SubTagFilterComponent) component));
        row.child(
            whitelistButton(component).pos(120, 5)
                .size(14));
        row.child(deleteButton(() -> {
            if (index >= 0 && index < filters.size()) {
                filters.remove(index);
                rebuildFilterCards(cards);
            }
        }).marginLeft(0)
            .pos(138, 5));
        return row;
    }

    /** Source filter palette: type chooses the color pair, W/B selects its light or dark member. */
    private static IDrawable filterBackground(IWildcardFilterComponent component) {
        int whitelistColor = 0xFF44AAFF, blacklistColor = 0xFF4852FF;
        if (component instanceof PropertyFilterComponent) {
            whitelistColor = 0xFFFFFF33;
            blacklistColor = 0xFFFF8800;
        } else if (component instanceof SubTagFilterComponent) {
            whitelistColor = 0xFFFF33FF;
            blacklistColor = 0xFF9933FF;
        }
        IDrawable whitelist = THEME.button.withColorOverride(whitelistColor);
        IDrawable blacklist = THEME.button.withColorOverride(blacklistColor);
        return new DynamicDrawable(() -> component.isWhitelist() ? whitelist : blacklist);
    }

    private IWidget stringFilterEditor(StringFilterComponent component) {
        return THEME.textField()
            .pos(3, 5)
            .size(110, 15)
            .setTextColor(0xFFFFFFFF)
            .value(new StringValue.Dynamic(component::getPattern, component::setPattern));
    }

    /** Source selector: changing the example replaces candidates and selects the first available property. */
    private IWidget propertyEditor(PropertyFilterComponent component) {
        SelectorWidget selector = new SelectorWidget(
            "wildcard_property_" + filters.indexOf(component),
            THEME.button,
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
        return new ParentWidget<>().size(115, 25)
            .child(drop.pos(3, 3))
            .child(selector.pos(25, 5));
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
        SelectorWidget selector = new SelectorWidget(
            "wildcard_subtag_" + filters.indexOf(component),
            THEME.button,
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
        return new ParentWidget<>().size(115, 25)
            .child(drop.pos(3, 3))
            .child(selector.pos(25, 5));
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
        return THEME.button()
            .size(16)
            .overlay(IKey.dynamic(() -> component.isWhitelist() ? "W" : "B"))
            .onMousePressed(button -> {
                component.setWhitelist(!component.isWhitelist());
                return true;
            });
    }

    // ============================================================
    // 公共按钮
    // ============================================================

    private ButtonWidget<?> addButton(String langKey, Runnable action) {
        return THEME.button()
            .size(30, 20)
            .marginRight(2)
            .overlay(
                IKey.lang(langKey)
                    .color(0xFFFFFFFF)
                    .shadow(true))
            .onMousePressed(button -> {
                action.run();
                return true;
            });
    }

    private ButtonWidget<?> deleteButton(Runnable action) {
        return THEME.button()
            .size(14)
            .marginLeft(3)
            .overlay(REFERENCE_CLOSE)
            .onMousePressed(button -> {
                action.run();
                return true;
            });
    }

    // ============================================================
    // 保存、同步与状态读写
    // ============================================================

    private ButtonWidget<?> saveButton() {
        return THEME.button()
            .size(30, 20)
            // #tr gui.wildcardpattern.save
            // # Save
            // # zh_CN 保存
            .overlay(
                IKey.lang("gui.wildcardpattern.save")
                    .color(0xFFFFFFFF)
                    .shadow(true))
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

    /**
     * 一个类槽位的输入控件：渲染一个物品图标，支持
     * <ul>
     * <li>鼠标左键：把光标上拿着的物品放入（复制一份，数量置 1），空手点击则清空；</li>
     * <li>从 NEI 拖入物品（实现 {@link RecipeViewerGhostIngredientSlot}）。</li>
     * </ul>
     * 放入/拖入的物品通过 {@code onDrop} 回调交给上层解析（矿辞前缀 / 真实名 / 固定物品）。
     * 纯客户端交互，不做同步；结果由外层"保存"统一推送到服务端。
     */
    public static final class DropWidget extends Widget<DropWidget>
        implements Interactable, RecipeViewerGhostIngredientSlot<ItemStack> {

        private final Supplier<ItemStack> getter;
        private final Consumer<ItemStack> onDrop;

        private DropWidget(Supplier<ItemStack> getter, Consumer<ItemStack> onDrop) {
            this.getter = getter;
            this.onDrop = onDrop;
            size(18);
            background(Theme.INSTANCE.slot);
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            ItemStack stack = getter == null ? null : getter.get();
            stack = WildcardMaterials.toDisplayStack(stack);
            if (stack != null && stack.getItem() != null) {
                new ItemDrawable(stack).draw(context, 1, 1, 16, 16, widgetTheme.getTheme());
            }
        }

        @Override
        public @NotNull Result onMousePressed(int mouseButton) {
            EntityPlayer player = MCHelper.getPlayer();
            ItemStack held = player == null ? null : player.inventory.getItemStack();
            if (held != null && held.getItem() != null) {
                ItemStack copy = held.copy();
                copy.stackSize = 1;
                fire(copy);
            } else {
                fire(null);
            }
            Interactable.playButtonClickSound();
            return Result.SUCCESS;
        }

        @Override
        public boolean handleDragAndDrop(@NotNull ItemStack draggedStack, int button) {
            if (draggedStack.getItem() == null) return false;
            ItemStack copy = draggedStack.copy();
            copy.stackSize = 1;
            fire(copy);
            return true;
        }

        private void fire(ItemStack stack) {
            if (onDrop != null) onDrop.accept(stack);
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
            size(158, 80);
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
                Theme.INSTANCE.button()
                    .pos(140, 2)
                    .size(14)
                    .overlay(
                        UITexture.builder()
                            .location(ModList.ModIds.GT_NEVER_RESET, "gui/ldlib/modern/close")
                            .build())
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
                child(new IconWidget(() -> stack(true, slot)).pos(10 + i % 3 * 18, 18 + i / 3 * 18));
                child(new IconWidget(() -> stack(false, slot)).pos(94 + i % 3 * 18, 18 + i / 3 * 18));
            }
            child(
                UITexture.builder()
                    .location(ModList.ModIds.GT_NEVER_RESET, "gui/ldlib/modern/progress_bar_arrow")
                    .imageSize(20, 40)
                    .subAreaXYWH(0, 0, 20, 20)
                    .build()
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
                Theme.INSTANCE.textField()
                    .pos(32, 59)
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

        /**
         * 只读物品图标格：每帧从 supplier 读取要显示的物品并绘制（带槽位背景）。用于预览页的输入/输出格。
         */
        private static final class IconWidget extends Widget<IconWidget> {

            private final Supplier<ItemStack> getter;

            private IconWidget(Supplier<ItemStack> getter) {
                this.getter = getter;
                size(18);
                background(Theme.INSTANCE.slot);
                tooltip().setAutoUpdate(true)
                    .tooltipBuilder(tooltip -> {
                        ItemStack stack = displayStack();
                        if (stack != null && stack.getItem() != null) tooltip.addFromItem(stack);
                    });
            }

            @Override
            public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
                ItemStack stack = displayStack();
                if (stack != null && stack.getItem() != null) {
                    new ItemDrawable(stack).draw(context, 1, 1, 16, 16, widgetTheme.getTheme());
                }
            }

            /** Uses the same fluid display conversion for the icon and its item tooltip. */
            private ItemStack displayStack() {
                return WildcardMaterials.toDisplayStack(getter == null ? null : getter.get());
            }

            /** Supplies the current item to MUI tooltip events and rebuilds text as the preview cycles. */
            @Override
            public void drawForeground(ModularGuiContext context) {
                ItemStack stack = displayStack();
                RichTooltip tooltip = getTooltip();
                if (stack != null && stack.getItem() != null
                    && tooltip != null
                    && isHoveringFor(tooltip.getShowUpTimer())
                    && !context.hasDraggable()) {
                    tooltip.draw(context, stack);
                }
            }
        }
    }

    /**
     * Wildcard Pattern's LDLib selector interaction adapted to MUI2 floating menus.
     * The button has dark text, while the translucent popup has white 15-pixel rows, at most five visible.
     * Programmatic updates are silent; a user selection invokes the callback once and closes the menu.
     * MUI2 owns the popup layer, outside-click dismissal and scroll clipping.
     */
    private static final class SelectorWidget extends AbstractMenuButton<SelectorWidget> {

        /** MUI caches panel handlers by name for the screen lifetime, including removed filter rows. */
        private static final AtomicLong NEXT_MENU_ID = new AtomicLong();
        private static final IDrawable SELECTED = new Rectangle().color(0xFFFFFFFF)
            .hollow(1);
        private final List<String> candidates = new ArrayList<>();
        private String value = "";
        private Consumer<String> onChanged = ignored -> {};
        private int maxCount = 5;

        private SelectorWidget(String name, IDrawable button, List<String> candidates) {
            super(name + "_menu_" + NEXT_MENU_ID.getAndIncrement());
            name(name);
            openOnHover = false;
            size(80, 15);
            padding(0);
            background(button);
            disableHoverBackground();
            overlay(new ScrollingTextDrawable(() -> value, 0xFF444444, false));
            tooltip().setAutoUpdate(true)
                .tooltipBuilder(tooltip -> { if (!value.isEmpty()) tooltip.addLine(IKey.str(value)); });
            setCandidates(candidates);
        }

        /** Replaces options and invalidates the popup; never selects or writes a model value implicitly. */
        public SelectorWidget setCandidates(List<String> values) {
            List<String> copy = new ArrayList<>(Objects.requireNonNull(values));
            copy.forEach(Objects::requireNonNull);
            if (isOpen()) closeMenu(false);
            candidates.clear();
            candidates.addAll(copy);
            setMenu(null);
            return this;
        }

        public List<String> getCandidates() {
            return Collections.unmodifiableList(candidates);
        }

        public SelectorWidget setValue(String value) {
            this.value = Objects.requireNonNull(value);
            return this;
        }

        public String getSelectedValue() {
            return value;
        }

        public SelectorWidget setOnChanged(Consumer<String> callback) {
            onChanged = Objects.requireNonNull(callback);
            return this;
        }

        public SelectorWidget setMaxCount(int count) {
            if (count < 1) throw new IllegalArgumentException("Positive row count required");
            maxCount = count;
            setCandidates(new ArrayList<>(candidates));
            return this;
        }

        @Override
        public void openMenu(boolean soft) {
            if (isValid() && areAncestorsEnabled() && !candidates.isEmpty()) super.openMenu(soft);
        }

        /**
         * A menu may still receive close events while its panel is finishing its closing animation.
         * Never walk a removed source's parent chain; also retire menus whose source page is hidden.
         */
        @Override
        protected void checkClose(boolean soft, boolean requireNoHover) {
            if (!isValid() || !areAncestorsEnabled()) {
                closeMenu(false);
                return;
            }
            super.checkClose(soft, requireNoHover);
        }

        /** Closes and drops the floating menu before MUI invalidates this row's parent and context. */
        @Override
        public void dispose() {
            try {
                closeMenu(false);
                setMenu(null);
            } finally {
                super.dispose();
            }
        }

        @Override
        protected Menu<?> createMenu() {
            ListWidget<IWidget, ?> list = new ListWidget<>().widthRel(1f)
                .maxSize(maxCount * 15)
                .scrollDirection(new VerticalScrollData(false, 4).texture(new Rectangle().color(0xFFFFFFFF)));
            for (String candidate : candidates) {
                list.child(
                    new ButtonWidget<>().widthRel(1f)
                        .height(15)
                        .padding(0)
                        .background(new DynamicDrawable(() -> candidate.equals(value) ? SELECTED : null))
                        .disableHoverBackground()
                        .overlay(new ScrollingTextDrawable(() -> candidate, 0xFFFFFFFF, true))
                        .tooltipBuilder(tooltip -> tooltip.addLine(IKey.str(candidate)))
                        .onMousePressed(mouse -> {
                            if (mouse != 0) return false;
                            if (!isValid() || !areAncestorsEnabled()) {
                                closeMenu(false);
                                return true;
                            }
                            if (candidates.contains(candidate)) {
                                setValue(candidate);
                                closeMenu(false);
                                onChanged.accept(candidate);
                            }
                            return true;
                        }));
            }
            return new Menu<>().widthRel(1f)
                .coverChildrenHeight()
                .padding(0)
                .background(new Rectangle().color(0xAA000000))
                .child(list);
        }

        /**
         * Single-line selector caption: short text is centered, long text scrolls between its ends without wrapping.
         * Uses MUI's clipping implementation so floating menus and scroll-list transforms remain aligned.
         * Each caption owns its renderer and timing; changing its value resets the initial reading pause.
         */
        private static final class ScrollingTextDrawable implements IDrawable {

            private final Supplier<String> text;
            private final TextRenderer renderer = new TextRenderer();
            private String previous;
            private long started;

            private ScrollingTextDrawable(Supplier<String> text, int color, boolean shadow) {
                this.text = Objects.requireNonNull(text);
                renderer.setColor(color);
                renderer.setShadow(shadow);
                renderer.setHardWrapOnBorder(false);
            }

            @Override
            public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme theme) {
                if (width <= 4 || height <= 0) return;
                String supplied = text.get();
                String value = supplied == null ? ""
                    : supplied.replace('\n', ' ')
                        .replace('\r', ' ');
                long now = Minecraft.getSystemTime();
                if (!value.equals(previous)) {
                    previous = value;
                    started = now;
                }
                int available = width - 4;
                TextRenderer.Line line = renderer.line(value);
                renderer.setPos(x + 2, y);
                boolean overflow = line.getWidth() > available;
                renderer.setAlignment(overflow ? Alignment.CenterLeft : Alignment.Center, available, height);
                if (overflow) {
                    long travel = Math.max(1000, (long) ((line.getWidth() - available) * 50));
                    long phase = Math.max(0, now - started) % (travel * 2 + 1600);
                    float progress = phase < 800 ? 0
                        : phase < 800 + travel ? (phase - 800) / (float) travel
                            : phase < 1600 + travel ? 1 : 1 - (phase - 1600 - travel) / (float) travel;
                    renderer.drawScrolling(line, progress, null, context);
                } else {
                    renderer.drawSimple(value);
                }
            }
        }
    }

    private static final class Theme {

        private static final Theme INSTANCE = new Theme();

        private final UITexture panel = texture("background", 256, 256, 2);
        private final UITexture button = texture("button", 200, 20, 3);
        private final UITexture hover = texture("button_highlighted", 200, 20, 3);
        private final UITexture disabled = texture("button_disabled", 200, 20, 3);
        private final UITexture slot = texture("slot", 18, 18, 1);
        private final UITexture tab = sprite("tabs_left", 64, 84, 0, 28, 32, 28, 3);
        private final UITexture selectedTab = sprite("tabs_left", 64, 84, 32, 28, 32, 28, 3);
        private final UITexture input = sprite("text_field", 128, 128, 0, 0, 128, 12, 1);
        private final UITexture inputFocused = sprite("text_field", 128, 128, 0, 24, 128, 12, 1);

        private Theme() {}

        private ButtonWidget<?> button() {
            ButtonWidget<?> widget = new ButtonWidget<>();
            return widget.background(new DynamicDrawable(() -> widget.isEnabled() ? button : disabled))
                .hoverBackground(hover);
        }

        private TextFieldWidget textField() {
            TextFieldWidget field = new TextFieldWidget();
            return field.background(new DynamicDrawable(() -> field.isFocused() ? inputFocused : input))
                .padding(3, 0)
                .setTextAlignment(Alignment.CenterLeft)
                .setTextColor(0xFFFFFFFF);
        }

        private static UITexture texture(String name, int width, int height, int border) {
            return sprite(name, width, height, 0, 0, width, height, border);
        }

        private static UITexture sprite(String name, int imageWidth, int imageHeight, int x, int y, int width,
            int height, int border) {
            return UITexture.builder()
                .location(ModList.ModIds.GT_NEVER_RESET, "gui/ldlib/modern/" + name)
                .imageSize(imageWidth, imageHeight)
                .nonOpaque()
                .subAreaXYWH(x, y, width, height)
                .adaptable(border)
                .build()
                .withColorOverride(0xFFFFFFFF);
        }
    }
}
