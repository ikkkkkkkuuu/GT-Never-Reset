package com.xyp.gtneverreset.common.gui.modularui;

import static gregtech.api.modularui2.GTGuis.createPopUpPanel;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.DrawableStack;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.utils.machine.MoldDataManager;

import appeng.api.implementations.ICraftingPatternItem;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.util.GTUtility;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;
import gregtech.common.gui.modularui.util.PatternSlot;
import gregtech.common.modularui2.widget.SlotLikeButtonWidget;
import gregtech.common.modularui2.widget.builder.ItemSlotGridBuilder;

/**
 * Complete GUI implementation for Super ME Crafting Input Bus
 * Shares controls between the full-size and single-row pattern hatches.
 */
public class SuperMTEHatchCraftingInputMEGui extends MTEHatchBaseGui<SuperMTEHatchCraftingInputME> {

    private static final int NO_MOLD = -1;
    private static final String PATTERN_INV_NAME = "pattern_inv";
    private static final String MANUAL_ITEM_INV_NAME = "manual_item_inv";
    private static final int PATTERN_SLOT_PER_ROW = 9;
    private static final int MANUAL_SLOT_ROW = 3;
    private static final int MANUAL_SLOT_PER_ROW = 3;

    public SuperMTEHatchCraftingInputMEGui(SuperMTEHatchCraftingInputME hatch) {
        super(hatch);
    }

    @Override
    protected int getBasePanelHeight() {
        return super.getBasePanelHeight() + SLOT_SIZE - (4 - visiblePatternRows()) * SLOT_SIZE;
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createContentSection(panel, syncManager).child(createSlots(syncManager));
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        Flow flow = super.createBottomLeftCornerFlow(panel, syncManager).child(createOptimizerButton())
            .child(createShowPatternButton())
            .child(createExportButton())
            .child(createDoublePatternButton())
            .child(createManualItemsButton(syncManager));
        if (machine.hasVirtualMoldSlot()) flow.child(createMoldSlotButton(syncManager));
        return flow;
    }

    private int visiblePatternRows() {
        return Math.min(4, machine.getPatternCount() / PATTERN_SLOT_PER_ROW);
    }

    private Grid createSlots(PanelSyncManager syncManager) {
        int rows = machine.getPatternCount() / PATTERN_SLOT_PER_ROW;
        syncManager.registerSlotGroup(PATTERN_INV_NAME, rows);
        Grid grid = new Grid().minColWidth(SLOT_SIZE)
            .gridOfWidthHeight(
                PATTERN_SLOT_PER_ROW,
                rows,
                ($x, $y, index) -> new PatternSlot().slot(
                    new ModularSlot(machine.inventoryHandler, index)
                        .filter(itemStack -> itemStack.getItem() instanceof ICraftingPatternItem)
                        .changeListener((itemStack, onlyAmount, client, init) -> {
                            if (!client) {
                                machine.onPatternChange(index, itemStack);
                            }
                        })
                        .slotGroup(PATTERN_INV_NAME)))
            .size(PATTERN_SLOT_PER_ROW * SLOT_SIZE + 4, visiblePatternRows() * SLOT_SIZE);
        return rows > visiblePatternRows() ? grid.scrollable() : grid;
    }

    private ToggleButton createOptimizerButton() {
        BooleanSyncValue optimizerSync = new BooleanSyncValue(
            () -> !machine.disablePatternOptimization,
            val -> machine.disablePatternOptimization = !val).allowC2S();

        return new ToggleButton().value(optimizerSync)
            .overlay(GTGuiTextures.OVERLAY_BUTTON_PATTERN_OPTIMIZE)
            .addTooltipLine(StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.optimize_pattern"))
            .addTooltip(
                true,
                StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.optimize_pattern.enable"))
            .addTooltip(
                false,
                StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.optimize_pattern.disabled"));
    }

    private ToggleButton createShowPatternButton() {
        BooleanSyncValue showPatternSync = new BooleanSyncValue(
            () -> machine.showPattern,
            val -> machine.showPattern = val).allowC2S();

        return new ToggleButton().value(showPatternSync)
            .overlay(true, GTGuiTextures.OVERLAY_BUTTON_WHITELIST)
            .overlay(false, GTGuiTextures.OVERLAY_BUTTON_BLACKLIST)
            .addTooltip(
                true,
                StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.show_pattern.enable"))
            .addTooltip(
                false,
                StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.show_pattern.disabled"));
    }

    private ButtonWidget<?> createExportButton() {
        InteractionSyncHandler exportSyncHandler = new InteractionSyncHandler().setOnMousePressed(mouseDelta -> {
            if (!mouseDelta.isClient() && mouseDelta.mouseButton == 0) {
                machine.refundAll(false);
            }
        });

        return new ButtonWidget<>().syncHandler(exportSyncHandler)
            .overlay(GTGuiTextures.OVERLAY_BUTTON_EXPORT)
            .addTooltipLine(StatCollector.translateToLocal("GT5U.gui.tooltip.hatch.crafting_input_me.export"));
    }

    private ButtonWidget<?> createDoublePatternButton() {
        InteractionSyncHandler doubleSyncHandler = new InteractionSyncHandler().setOnMousePressed(mouseDelta -> {
            if (!mouseDelta.isClient()) {
                int val = mouseDelta.shift ? 1 : 0;
                if (mouseDelta.mouseButton == 1) val |= 0b10;
                machine.doublePatterns(val);
            }
        });

        return new ButtonWidget<>().syncHandler(doubleSyncHandler)
            .overlay(GTGuiTextures.OVERLAY_BUTTON_X2)
            .addTooltipLine(StatCollector.translateToLocal("gui.tooltips.appliedenergistics2.DoublePatterns"));
    }

    private ButtonWidget<?> createManualItemsButton(PanelSyncManager syncManager) {
        IPanelHandler popupPanel = syncManager
            .syncedPanel("manual_slots_panel", true, (manager, handler) -> createManualSlotUI(manager));

        return new ButtonWidget<>().overlay(GTGuiTextures.OVERLAY_BUTTON_PLUS_LARGE)
            .addTooltipLine(
                StatCollector.translateToLocal("GT5U.gui.tooltip.hatch.crafting_input_me.place_manual_items"))
            .onMousePressed(mouseButton -> {
                popupPanel.openPanel();
                return popupPanel.isPanelOpen();
            });
    }

    private GhostMoldSlotWidget createMoldSlotButton(PanelSyncManager syncManager) {
        GhostMoldSlotWidget widget = new GhostMoldSlotWidget(machine, syncManager);
        widget.slot(new ModularSlot(new GhostMoldItemStackHandler(machine), 0));
        widget.size(18, 18);
        return widget;
    }

    private ModularPanel createManualSlotUI(PanelSyncManager syncManager) {
        return createPopUpPanel("manual_slots_panel").size(68, 76)
            .bottomRelOffset(0.5f, 52)
            .child(
                new ItemSlotGridBuilder(machine.inventoryHandler, syncManager)
                    .size(MANUAL_SLOT_PER_ROW, MANUAL_SLOT_ROW)
                    .slotGroupKey(MANUAL_ITEM_INV_NAME)
                    .indexOffset(machine.getManualSlotStart())
                    .build()
                    .marginTop(16)
                    .horizontalCenter());
    }

    private static final class GhostMoldSlotWidget extends PhantomItemSlot {

        private static final String GUI_ID = "mold_selector";
        private static final int SELECTOR_COLS = 9;
        private static final int SELECTOR_VISIBLE_ROWS = 9;
        private static final int SELECTOR_SLOT_SIZE = 18;
        private static final int SELECTOR_GUI_WIDTH = SELECTOR_SLOT_SIZE * SELECTOR_COLS + 7 * 2;
        private static final int SELECTOR_HEADER_HEIGHT = 46;
        private static final int SELECTOR_GUI_HEIGHT = SELECTOR_HEADER_HEIGHT
            + SELECTOR_SLOT_SIZE * SELECTOR_VISIBLE_ROWS;

        private final SuperMTEHatchCraftingInputME hatch;
        private GhostMoldSyncHandler moldSyncHandler;
        private IPanelHandler selectorPanelHandler;
        private final PanelSyncManager syncManager;

        private GhostMoldSlotWidget(SuperMTEHatchCraftingInputME hatch, PanelSyncManager syncManager) {
            super();
            this.hatch = hatch;
            this.syncManager = syncManager;
            tooltipBuilder(this::getMoldSlotTooltip);
            selectorPanelHandler = buildSelectorPanel();
        }

        @Override
        public IDrawable getCurrentBackground(WidgetThemeEntry<?> widgetTheme) {
            IDrawable background = super.getCurrentBackground(widgetTheme);
            return new DrawableStack(background, GTGuiTextures.OVERLAY_SLOT_MOLD);
        }

        @Override
        public @NotNull Result onMousePressed(int mouseButton) {
            if (!isSelectorPanelOpen()) {
                if (mouseButton == 0 && Interactable.hasShiftDown()) {
                    openSelectorPanel();
                } else {
                    MouseData mouseData = MouseData.create(mouseButton);
                    getSyncHandler().syncToServer(PhantomItemSlotSH.SYNC_CLICK, mouseData::writeToPacket);
                }
            }
            return Result.SUCCESS;
        }

        @Override
        public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
            if (isSelectorPanelOpen()) return true;
            MouseData mouseData = MouseData.create(scrollDirection.modifier);
            getSyncHandler().syncToServer(PhantomItemSlotSH.SYNC_SCROLL, mouseData::writeToPacket);
            return true;
        }

        @Override
        public PhantomItemSlot slot(ModularSlot slot) {
            moldSyncHandler = new GhostMoldSyncHandler(slot, hatch);
            setSyncOrValue(moldSyncHandler);
            syncManager.syncValue("ghostMoldIndex", 0, moldSyncHandler.indexSync);
            return this;
        }

        @NotNull
        @Override
        public GhostMoldSyncHandler getSyncHandler() {
            return moldSyncHandler;
        }

        private void getMoldSlotTooltip(RichTooltip tooltip) {
            ItemStack current = hatch.inventoryHandler.getStackInSlot(hatch.getMoldSlot());
            String moldName;
            if (current == null) {
                // #tr gui.gtneverreset.select_mold.tooltip
                // # Ghost Mold Slot
                // # zh_CN 虚拟模具槽
                moldName = StatCollector.translateToLocal("gui.gtneverreset.select_mold.tooltip");
            } else {
                moldName = current.getDisplayName();
            }
            tooltip.clearText()
                .addLine(moldName)
                // #tr gui.gtneverreset.select_mold.tooltip.1
                // # Shift+left-click to select a mold
                // # zh_CN Shift+左键选择模具
                .addLine(IKey.lang("gui.gtneverreset.select_mold.tooltip.1"))
                // #tr gui.gtneverreset.select_mold.tooltip.2
                // # Scroll to cycle molds
                // # zh_CN 滚动滚轮切换模具
                .addLine(IKey.lang("gui.gtneverreset.select_mold.tooltip.2"));
        }

        private boolean isSelectorPanelOpen() {
            return getPanel().getScreen()
                .isPanelOpen(GUI_ID);
        }

        private void openSelectorPanel() {
            if (selectorPanelHandler == null) {
                selectorPanelHandler = buildSelectorPanel();
            }
            selectorPanelHandler.openPanel();
        }

        private IPanelHandler buildSelectorPanel() {
            return syncManager.syncedPanel("moldSlotPanel", true, (mainPanel, player) -> {
                ModularPanel panel = GTGuis.createPopUpPanel(GUI_ID);
                ItemStack[] molds = MoldDataManager.getMolds();
                int totalRows = (molds.length + SELECTOR_COLS - 1) / SELECTOR_COLS;

                panel.size(SELECTOR_GUI_WIDTH, SELECTOR_GUI_HEIGHT);

                // Header: item icon + title
                panel.child(
                    Flow.row()
                        .coverChildren()
                        .childPadding(4)
                        .pos(5, 5)
                        .child(
                            new ItemDrawable(hatch.getStackForm(1)).asWidget()
                                .size(16))
                        // #tr gui.gtneverreset.select_mold
                        // # Select Mold
                        // # zh_CN 选择模具
                        .child(
                            IKey.lang("gui.gtneverreset.select_mold")
                                .asWidget()));

                // "Current" label
                panel.child(
                    IKey.lang("GT5U.gui.select.current")
                        .asWidget()
                        .leftRel(0.5f, -(SELECTOR_SLOT_SIZE / 2) - 3, 1)
                        .height(SELECTOR_SLOT_SIZE)
                        .top(22));

                // Current selection widget
                panel.child(new SlotLikeButtonWidget(() -> {
                    int idx = moldSyncHandler.getSelectedIndex();
                    return idx >= 0 && idx < molds.length ? molds[idx] : null;
                }).background(GTGuiTextures.SLOT_ITEM_DARK, new DynamicDrawable(() -> GTGuiTextures.OVERLAY_SLOT_MOLD))
                    .playClickSound(false)
                    .onMousePressed(mouseButton -> true)
                    .horizontalCenter()
                    .top(22));

                // Scrollable grid of all choices - shows 9 rows, scroll for the rest
                // Use minColWidth to keep columns properly sized
                panel.child(
                    new Grid().minColWidth(SELECTOR_SLOT_SIZE)
                        .gridOfWidthHeight(SELECTOR_COLS, totalRows, (x, y, index) -> {
                            if (index >= molds.length) return null;
                            SlotLikeButtonWidget widget = new SlotLikeButtonWidget(molds[index])
                                .background(
                                    new DynamicDrawable(
                                        () -> moldSyncHandler.getSelectedIndex() == index ? GTGuiTextures.SLOT_ITEM_DARK
                                            : GTGuiTextures.SLOT_ITEM_STANDARD))
                                .size(SELECTOR_SLOT_SIZE)
                                .onMousePressed(mouseButton -> {
                                    if (mouseButton == 0) {
                                        moldSyncHandler.setSelectedIndex(index);
                                    } else {
                                        moldSyncHandler.setSelectedIndex(NO_MOLD);
                                    }
                                    MouseData mouseData = MouseData.create(mouseButton);
                                    if (mouseData.shift) {
                                        panel.closeIfOpen();
                                    }
                                    return true;
                                });
                            return widget;
                        })
                        .size(SELECTOR_COLS * SELECTOR_SLOT_SIZE + 4, SELECTOR_VISIBLE_ROWS * SELECTOR_SLOT_SIZE)
                        .scrollable()
                        .pos(7, SELECTOR_HEADER_HEIGHT));

                return panel;
            });
        }
    }

    private static final class GhostMoldSyncHandler extends PhantomItemSlotSH {

        private final SuperMTEHatchCraftingInputME hatch;
        private final IntSyncValue indexSync;

        @SuppressWarnings("UnstableApiUsage")
        private GhostMoldSyncHandler(ModularSlot slot, SuperMTEHatchCraftingInputME hatch) {
            super(slot);
            this.hatch = hatch;
            indexSync = new IntSyncValue(() -> {
                ItemStack current = hatch.inventoryHandler.getStackInSlot(hatch.getMoldSlot());
                return current != null ? hatch.findMatchingMoldIndex(current) : NO_MOLD;
            }, index -> {
                if (index >= 0 && index < MoldDataManager.getMoldCount()) {
                    hatch.setMold(MoldDataManager.getMolds()[index]);
                } else {
                    hatch.setMold(null);
                }
            }).allowC2S();
        }

        @Override
        protected void phantomClick(MouseData mouseData, ItemStack cursorStack) {
            if (indexSync == null) return;

            int itemIndex = hatch.findMatchingMoldIndex(cursorStack);
            if (cursorStack != null && itemIndex != -1) {
                setSelectedIndex(itemIndex);
            } else {
                if (mouseData.mouseButton == 0) {
                    // 左键点击：递增
                    setSelectedIndex(getNextMoldConfig(1));
                } else if (mouseData.mouseButton == 1 && mouseData.shift) {
                    // Shift+右键：清除
                    setSelectedIndex(NO_MOLD);
                } else if (mouseData.mouseButton == 1) {
                    // 右键点击：递减
                    setSelectedIndex(getNextMoldConfig(-1));
                }
            }
        }

        @Override
        protected void phantomScroll(MouseData mouseData) {
            setSelectedIndex(getNextMoldConfig(mouseData.mouseButton));
        }

        private int getNextMoldConfig(int delta) {
            int newIndex = getSelectedIndex() + delta;
            if (newIndex < NO_MOLD) newIndex = MoldDataManager.getMoldCount() - 1;
            if (newIndex >= MoldDataManager.getMoldCount()) newIndex = NO_MOLD;
            return newIndex;
        }

        public int getSelectedIndex() {
            return indexSync != null ? indexSync.getIntValue() : NO_MOLD;
        }

        public void setSelectedIndex(int index) {
            if (indexSync != null) indexSync.setIntValue(index);
        }

    }

    private static final class GhostMoldItemStackHandler implements IItemHandlerModifiable {

        private final IItemHandlerModifiable inventory;
        private final int moldSlot;

        private GhostMoldItemStackHandler(SuperMTEHatchCraftingInputME hatch) {
            this.inventory = hatch.getInventoryHandler();
            this.moldSlot = hatch.getMoldSlot();
        }

        @Override
        public void setStackInSlot(int slot, @Nullable ItemStack stack) {
            validateSlot(slot);
            if (isItemValid(slot, stack)) {
                if (stack != null) {
                    stack = stack.copy();
                    stack.stackSize = 0;
                }
                inventory.setStackInSlot(moldSlot, stack);
            }
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Nullable
        @Override
        public ItemStack getStackInSlot(int slot) {
            validateSlot(slot);
            return inventory.getStackInSlot(moldSlot);
        }

        @Nullable
        @Override
        public ItemStack insertItem(int slot, @Nullable ItemStack stack, boolean simulate) {
            validateSlot(slot);
            return stack;
        }

        @Nullable
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            validateSlot(slot);
            return null;
        }

        @Override
        public int getSlotLimit(int slot) {
            validateSlot(slot);
            return 0;
        }

        @Override
        public boolean isItemValid(int slot, @Nullable ItemStack stack) {
            validateSlot(slot);
            if (stack == null) return true;
            // 只允许模具类物品
            for (ItemStack mold : MoldDataManager.getMolds()) {
                if (GTUtility.areStacksEqual(mold, stack, true)) return true;
            }
            return false;
        }

        private void validateSlot(int slot) {
            if (slot != 0) throw new IndexOutOfBoundsException("Slot index out of bounds: " + slot);
        }
    }
}
