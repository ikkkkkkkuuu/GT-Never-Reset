package com.xyp.gtneverreset.common.gui.modularui.multiblock;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.TabTexture;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.xyp.gtneverreset.GTNeverReset;

import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;

abstract class BreederGui<T extends MTEExtendedPowerMultiBlockBase<?>> extends GTNRMultiBlockBaseGui<T> {

    protected static final int BODY_WIDTH = 232;
    protected static final int ROUTE_VIEWPORT_WIDTH = BODY_WIDTH - 20;
    private static final int PANEL_PADDING = 6;
    private static final int INVENTORY_HEIGHT = 76;
    private static final int INVENTORY_WIDTH = 9 * 18;
    private static final int ROUTE_PAGE = 3;
    private static final TabTexture RIGHT_COVER_TAB = TabTexture.of(
        UITexture.builder()
            .location(GTNeverReset.MODID, "gui/tab/right_cover_tabs")
            .fullImage()
            .canApplyTheme()
            .build(),
        GuiAxis.X,
        true,
        32,
        28,
        4);

    protected final PagedWidget.Controller pages = new PagedWidget.Controller();
    private final int bodyHeight;

    protected BreederGui(T multiblock, int bodyHeight) {
        super(multiblock);
        this.bodyHeight = bodyHeight;
    }

    @Override
    protected final int getBasePanelWidth() {
        return BODY_WIDTH + 2 * PANEL_PADDING;
    }

    @Override
    protected final int getBasePanelHeight() {
        return bodyHeight + 2 * PANEL_PADDING;
    }

    @Override
    public final Flow createMainColumn(ModularPanel panel, PanelSyncManager sync) {
        PageButton[] tabs = createPageTabs();
        ParentWidget<?> tabStrip = new ParentWidget<>().name("breeder_tabs")
            .pos(getBasePanelWidth() - 3, 0)
            .size(RIGHT_COVER_TAB.getWidth(), RIGHT_COVER_TAB.getHeight() * tabs.length)
            .excludeAreaInRecipeViewer();
        for (PageButton tab : tabs) tabStrip.child(tab);
        panel.child(tabStrip);

        int inventoryTop = bodyHeight - INVENTORY_HEIGHT;
        ParentWidget<?> inventory = new ParentWidget<>().name("breeder_inventory")
            .pos(0, inventoryTop)
            .size(BODY_WIDTH, INVENTORY_HEIGHT)
            .childIf(
                multiblock.doesBindPlayerInventory(),
                () -> SlotGroupWidget.playerInventory(false)
                    .pos((BODY_WIDTH - INVENTORY_WIDTH) / 2, 0));
        Flow controls = createButtonColumn(panel, sync).name("breeder_controls")
            .top(inventoryTop)
            .height(INVENTORY_HEIGHT)
            .child(createModeSwitchButton(sync));
        PagedWidget<?> content = new PagedWidget<>().name("breeder_pages")
            .controller(pages)
            .size(BODY_WIDTH, bodyHeight);
        addPages(content, panel, sync);
        content.onPageChange(index -> inventory.setEnabled(index != ROUTE_PAGE));
        return Flow.column()
            .padding(PANEL_PADDING)
            .child(
                new ParentWidget<>().size(BODY_WIDTH, bodyHeight)
                    .child(content)
                    .child(inventory)
                    .child(controls));
    }

    protected abstract void addPages(PagedWidget<?> content, ModularPanel panel, PanelSyncManager sync);

    protected abstract PageButton[] createPageTabs();

    protected final PageButton tab(int index, IDrawable icon, String tooltipKey) {
        return new PageButton(index, pages).tab(RIGHT_COVER_TAB, -1)
            .name("breeder_tab_" + index)
            .pos(0, index * RIGHT_COVER_TAB.getHeight())
            .overlay(
                icon.asIcon()
                    .size(16, 16)
                    .center())
            .tooltipBuilder(t -> t.addLine(IKey.lang(tooltipKey)));
    }
}
