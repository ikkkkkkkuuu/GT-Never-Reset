package com.xyp.gtneverreset.client.projector;

import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.BUTTON;
import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.CONTROL;
import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.LINE;
import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.MENU;
import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.SMALL;
import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.column;
import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.inventory.InventoryTypes;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.gui.ProjectorMaterialSync;
import com.xyp.gtneverreset.common.item.items.MultiblockProjector;
import com.xyp.gtneverreset.common.packet.GenerateProjectorBatchPacket;
import com.xyp.gtneverreset.common.packet.GenerateProjectorPacket;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.projector.ProjectorBatch;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorOrientation;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorSimulation;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Choice;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Material;
import com.xyp.gtneverreset.config.ClientConfig;
import com.xyp.gtneverreset.config.ConfigRegistry;
import com.xyp.gtneverreset.config.ProjectorConfig;
import com.xyp.gtneverreset.config.ScreenThemeConfig;
import com.xyp.gtnrlib.animation.StructureDeploymentStyle;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Icon;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Draw;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Panels;
import com.xyp.gtnrlib.gui.theme.ae2.AE2SettingIcon;
import com.xyp.gtnrlib.gui.theme.ae2.IconSource;
import com.xyp.gtnrlib.gui.widget.AE2Button;
import com.xyp.gtnrlib.gui.widget.AECheckbox;
import com.xyp.gtnrlib.gui.widget.FadingNotice;
import com.xyp.gtnrlib.gui.widget.IconButton;
import com.xyp.gtnrlib.gui.widget.IconRowList;
import com.xyp.gtnrlib.gui.widget.IconRowList.Row;
import com.xyp.gtnrlib.gui.widget.ListRow;
import com.xyp.gtnrlib.gui.widget.NumberEntryWidget;
import com.xyp.gtnrlib.gui.widget.PageTabs;
import com.xyp.gtnrlib.gui.widget.VerticalButtonBar;

import appeng.util.item.AEItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

@SideOnly(Side.CLIENT)
public class ProjectorGui extends ModularPanel {

    public static final String LANG = "gtneverreset.projector.gui.", POSE = "gtneverreset.projector.pose.";
    public static final int MACHINES = 0, PARAMETERS = 1, HATCHES = 2, MATERIALS = 3, GENERATION = 4, BATCH = 5;
    private static final String NAME = "multiblock_projector";
    private static final int WIDTH = MultiblockProjector.GUI_WIDTH, HEIGHT = MultiblockProjector.GUI_HEIGHT;
    private static final int BODY_LEFT = VerticalButtonBar.WIDTH - VerticalButtonBar.OVERLAP,
        BODY_RIGHT = AE2Theme.PAGE_TAB_SIZE - AE2Theme.PAGE_TAB_INSET, PAD = 8, TOP = 6, TABS_TOP = 22,
        STATUS_INSET = 5, REFRESH_TICKS = 20, NOTICE_MILLIS = 3000, PLAIN_MUTED = 0xFF808080, PLAIN_WELL = 0xFF1C1C22;
    private static final int CONTENT_WIDTH = WIDTH - BODY_LEFT - BODY_RIGHT - 2 * PAD,
        CONTENT_HEIGHT = HEIGHT - TOP - PAD, BODY_HEIGHT = CONTENT_HEIGHT - LINE - 4;
    public static final int RIGHT_WIDTH = 176, PAGE_HEIGHT = BODY_HEIGHT - LINE - BUTTON - 8;
    private static final int PREVIEW_WIDTH = CONTENT_WIDTH - RIGHT_WIDTH - 6;
    // #tr gtneverreset.projector.gui.tab_parameters
    // # Settings
    // # zh_CN 参数
    // #tr gtneverreset.projector.gui.tab_hatches
    // # Hatches
    // # zh_CN 仓室
    // #tr gtneverreset.projector.gui.tab_materials
    // # Materials
    // # zh_CN 材料
    // #tr gtneverreset.projector.gui.tab_machines
    // # Machine
    // # zh_CN 机器
    // #tr gtneverreset.projector.gui.tab_generation
    // # Generation
    // # zh_CN 生成
    // #tr gtneverreset.projector.gui.tab_batch
    // # Batch generation
    // # zh_CN 批量生成
    private static final String[] TAB_NAMES = { "tab_machines", "tab_parameters", "tab_hatches", "tab_materials",
        "tab_generation", "tab_batch" };
    private static final IconSource[] TAB_ICONS = { Icon.BLOCK, Icon.GEAR, Icon.PROCESSOR, Icon.FILE,
        AE2SettingIcon.CRAFT_HAMMER, Icon.COPY };
    private static int lastPage = -1, lastLayerMachine, lastLayer = ProjectorPreviewWidget.ALL_LAYERS;

    public final ProjectorPlan working;
    public final ProjectorHatchPage hatches;
    public final Variant variant;
    public final int muted, wellWidth;
    public boolean configChanged;
    private final int location;
    private final ProjectorPlan stored;
    private final String themeId = ScreenThemeConfig.projector.themeId;
    private final int goodColor, badColor;
    private final ProjectorBatchPreview.Cache previewCache = new ProjectorBatchPreview.Cache();
    private final ProjectorPreviewWidget preview;
    private final ProjectorMachineTab machines;
    private final ProjectorMaterialSync materialStock;
    private ProjectorStructure shown;
    private int page, ticks;
    private final FadingNotice notice = new FadingNotice().duration(NOTICE_MILLIS);
    private boolean displayChosen;
    private final LongLinkedOpenHashSet selectedPositions = new LongLinkedOpenHashSet();
    private ProjectorStructure selectionShape;
    private int[] selectionCells = new int[0];

    public ProjectorGui(PlayerInventoryGuiData data, ProjectorMaterialSync materialStock) {
        super(NAME);
        this.materialStock = materialStock;
        size(WIDTH, HEIGHT);
        background(AE2Panels.panelBackground(this, BODY_LEFT, BODY_RIGHT)).disableThemeBackground(true)
            .disableHoverThemeBackground(true);
        variant = AE2Theme.variant(themeId);
        muted = variant == null ? PLAIN_MUTED : variant.colors.muted();
        goodColor = variant == null ? IconRowList.GOOD : variant.colors.good();
        badColor = variant == null ? IconRowList.BAD : variant.colors.bad();
        wellWidth = variant == null ? RIGHT_WIDTH : RIGHT_WIDTH - IconRowList.AE2_SCROLL_COLUMN;
        EntityPlayer player = data.getPlayer();
        int index = data.getSlotIndex();
        location = data.getInventoryType() == InventoryTypes.BAUBLES ? -index - 1
            : index == player.inventory.currentItem ? 0 : index + 1;
        stored = ProjectorPlan.read(data.getUsedItemStack());
        working = stored.copy();
        machines = new ProjectorMachineTab(this);
        machines.refreshCarried(player);
        if (!working.hasMachine() && machines.firstCarried() > 0) selectMachine(machines.firstCarried());
        page = !working.hasMachine() ? MACHINES : lastPage >= 0 ? lastPage : PARAMETERS;
        int[] spot = ProjectorClient.takeSpot(working.machine);
        if (spot != null) {
            selectedPositions.add(ProjectorPlan.pack(spot[0], spot[1], spot[2]));
            page = HATCHES;
        }
        if (page == BATCH || page == GENERATION) working.batchProjection = page == BATCH;
        preview = new ProjectorPreviewWidget(this::previewStructure)
            .selection(this::selectedCells, cell -> select(cell, GuiScreen.isCtrlKeyDown()))
            .markers(() -> ClientConfig.projectorHatchOutlines, working)
            .facing(() -> working.hasMachine() ? working.facing : null);
        if (working.machine == lastLayerMachine) preview.restoreLayer(lastLayer);
        hatches = new ProjectorHatchPage(this::previewStructure, working, this::selectedCells, this::select);
        child(
            column().pos(BODY_LEFT + PAD, TOP)
                .size(CONTENT_WIDTH, CONTENT_HEIGHT)
                .childPadding(4)
                .child(titleRow())
                .child(
                    Flow.row()
                        .size(CONTENT_WIDTH, BODY_HEIGHT)
                        .childPadding(6)
                        .child(previewColumn())
                        .child(sideColumn())));
        buildTabs();
        child(toolbar());
    }

    public ModularScreen screen() {
        int channelMenus = ProjectorParameterTab.CHANNEL_MENUS;
        String[] menus = new String[channelMenus + 5];
        for (int i = 0; i < channelMenus; i++) menus[i] = MENU + "channel_" + i;
        // #tr gtneverreset.projector.gui.hatch_tier
        // # Hatch tier
        // # zh_CN 仓室等级
        menus[channelMenus] = MENU + "hatch_tier";
        menus[channelMenus + 1] = MENU + "kind";
        menus[channelMenus + 2] = MENU + "variant";
        // #tr gtneverreset.projector.gui.casing
        // # Casing (no hatch)
        // # zh_CN 外壳（不放仓）
        menus[channelMenus + 3] = MENU + "casing";
        // #tr gtneverreset.projector.gui.block
        // # Block
        // # zh_CN 方块
        menus[channelMenus + 4] = MENU + "block";
        AE2Theme.useMenuTheme(themeId, GTNeverReset.MODID, NAME, menus);
        return new ModularScreen(GTNeverReset.MODID, this) {

            @Override
            public void onClose() {
                super.onClose();
                closed();
            }
        }.useTheme(themeId);
    }

    public IDrawable well() {
        return AE2Panels.well(wellWidth, PLAIN_WELL);
    }

    public ListRow controlRow(String key, Widget<?> control) {
        return new ListRow(() -> StatCollector.translateToLocal(key)).width(wellWidth)
            .control(control, CONTROL, SMALL);
    }

    public ListRow header(String key) {
        return ListRow.header(() -> StatCollector.translateToLocal(key))
            .width(wellWidth);
    }

    private IWidget titleRow() {
        return Flow.row()
            .size(CONTENT_WIDTH, LINE)
            .childPadding(8)
            // #tr gtneverreset.projector.pose.title
            // # Projector pose
            // # zh_CN 投影姿态
            // #tr gtneverreset.projector.gui.title
            // # Wrist-Mounted Holographic Terminal
            // # zh_CN 腕臂式全息投影终端
            .child(text(StatCollector.translateToLocal(LANG + "title")))
            .child(
                IKey.dynamic(this::summaryLine)
                    .asWidget()
                    .color(muted)
                    .alignment(Alignment.CenterLeft)
                    .expanded()
                    .height(LINE));
    }

    private IWidget previewColumn() {
        return column().size(PREVIEW_WIDTH, BODY_HEIGHT)
            .childPadding(4)
            .child(
                new ParentWidget<>().size(PREVIEW_WIDTH, BODY_HEIGHT - BUTTON - 4)
                    .child(
                        preview.hoverText(this::hoverLine)
                            .sizeRel(1f))
                    .child(
                        notice.left(4)
                            .right(4)
                            .bottom(4)
                            .height(12))
                    .child(statusIcon()))
            .child(
                Flow.row()
                    .size(PREVIEW_WIDTH, BUTTON)
                    .childPadding(2)
                    .child(new IconButton(() -> Icon.REMOVE, () -> preview.stepLayer(-1)))
                    .child(
                        IKey.dynamic(this::layerLabel)
                            .asWidget()
                            .alignment(Alignment.Center)
                            .width(32)
                            .height(BUTTON))
                    .child(new IconButton(() -> Icon.ADD, () -> preview.stepLayer(1)))
                    .child(
                        new AE2Button(
                            () -> StatCollector
                                // #tr gtneverreset.projector.display.BOX
                                // # Display: frame only
                                // # zh_CN 显示：只有外框
                                // #tr gtneverreset.projector.display.DIFFERENCE
                                // # Display: frame and missing blocks
                                // # zh_CN 显示：外框和缺的方块
                                // #tr gtneverreset.projector.display.BLOCKS
                                // # Display: all blocks
                                // # zh_CN 显示：全部方块
                                .translateToLocal(ProjectorClient.LANG + "display." + working.display.name()),
                            this::cycleDisplay).expanded()
                                .height(BUTTON)));
    }

    private IWidget sideColumn() {
        ParentWidget<?> pages = new ParentWidget<>().size(RIGHT_WIDTH, PAGE_HEIGHT);
        pages.child(onPage(machines.build(), MACHINES));
        pages.child(onPage(new ProjectorParameterTab(this).build(), PARAMETERS));
        pages.child(onPage(new ProjectorHatchTab(this).build(), HATCHES));
        pages.child(onPage(materialsPage(), MATERIALS));
        pages.child(onPage(generationPage(), GENERATION));
        pages.child(onPage(batchPage(), BATCH));
        return column().size(RIGHT_WIDTH, BODY_HEIGHT)
            .childPadding(4)
            .child(
                IKey.dynamic(() -> StatCollector.translateToLocal(LANG + TAB_NAMES[page]))
                    .asWidget()
                    .alignment(Alignment.CenterLeft)
                    .widthRel(1f)
                    .height(LINE))
            .child(pages)
            .child(
                Flow.row()
                    .size(RIGHT_WIDTH, BUTTON)
                    .childPadding(2)
                    .child(
                        new AE2Button(this::projectLabel, this::project).expanded()
                            .height(BUTTON))
                    .child(
                        // #tr gtneverreset.projector.gui.clear
                        // # Clear plan
                        // # zh_CN 清除方案
                        new AE2Button(() -> StatCollector.translateToLocal(LANG + "clear"), this::clearPlan)
                            .width(RIGHT_WIDTH * 2 / 5)
                            .height(BUTTON)));
    }

    private void buildTabs() {
        PageTabs tabs = new PageTabs(() -> page, index -> {
            page = index;
            if (page == BATCH || page == GENERATION) {
                if (working.batchProjection != (page == BATCH)) clearSelection();
                working.batchProjection = page == BATCH;
            }
        });
        for (int i = 0; i < TAB_ICONS.length; i++) {
            int index = i;
            tabs.tab(() -> TAB_ICONS[index], () -> StatCollector.translateToLocal(LANG + TAB_NAMES[index]));
        }
        child(tabs.pos(WIDTH - BODY_RIGHT - AE2Theme.PAGE_TAB_INSET, TABS_TOP));
    }

    private Widget<?> materialsPage() {
        return column().size(RIGHT_WIDTH, PAGE_HEIGHT)
            .childPadding(4)
            .child(onPage(new MaterialList().size(RIGHT_WIDTH, PAGE_HEIGHT - BUTTON - 4), MATERIALS))
            .child(
                // #tr gtneverreset.projector.gui.craft_missing
                // # Craft missing materials
                // # zh_CN 一键合成
                new AE2Button(() -> StatCollector.translateToLocal(LANG + "craft_missing"), this::craftMissingMaterials)
                    .available(() -> canCraftMaterials(previewStructure()))
                    .name("craft_missing_materials")
                    .size(RIGHT_WIDTH, BUTTON));
    }

    private Widget<?> generationPage() {
        // #tr gtneverreset.projector.gui.generate
        // # Generate one machine
        // # zh_CN 生成一台
        return column().sizeRel(1f)
            .childPadding(4)
            .child(deploymentStyleButton())
            .child(
                new AE2Button(() -> StatCollector.translateToLocal(LANG + "generate"), () -> generate(false))
                    .name("generate_structure")
                    .widthRel(1f)
                    .height(BUTTON)
                    .available(this::canGenerate)
                    .tooltipBuilder(this::buildHint));
    }

    private Widget<?> deploymentStyleButton() {
        // #tr gtneverreset.projector.gui.build_style
        // # Animation: %s
        // # zh_CN 搭建动画：%s
        return new AE2Button(
            () -> StatCollector.translateToLocalFormatted(LANG + "build_style", deploymentStyleName()),
            () -> {
                var styles = StructureDeploymentStyle.values();
                working.buildStyle = styles[(working.buildStyle.ordinal() + 1) % styles.length];
            }).name("deployment_style")
                .widthRel(1f)
                .height(BUTTON);
    }

    private String deploymentStyleName() {
        return StatCollector.translateToLocal(switch (working.buildStyle) {
            // #tr gtneverreset.projector.gui.build_style_summon
            // # Summoning Circle
            // # zh_CN 法阵降临
            case SUMMON -> LANG + "build_style_summon";
            // #tr gtneverreset.projector.gui.build_style_reveal
            // # White Reveal
            // # zh_CN 白光显现
            case REVEAL -> LANG + "build_style_reveal";
            // #tr gtneverreset.projector.gui.build_style_flight
            // # Segmented Flight
            // # zh_CN 分段飞入
            case FLIGHT -> LANG + "build_style_flight";
            // #tr gtneverreset.projector.gui.build_style_none
            // # None
            // # zh_CN 无动画
            case NONE -> LANG + "build_style_none";
            // #tr gtneverreset.projector.gui.build_style_singularity
            // # Singularity Warp
            // # zh_CN 奇点跃迁
            case SINGULARITY -> LANG + "build_style_singularity";
        });
    }

    private void buildHint(RichTooltip tooltip) {
        // #tr gtneverreset.projector.gui.generate_hint
        // # Survival: all materials must be available in your inventory or linked ME storage.
        // # zh_CN 生存模式：背包和已连接的 ME 库存合计备齐材料后才会生成。
        tooltip.addLine(StatCollector.translateToLocal(LANG + "generate_hint"));
    }

    private Widget<?> batchPage() {
        Flow controls = column().sizeRel(1f)
            .childPadding(4);
        // #tr gtneverreset.projector.gui.batch_count
        // # Additional machines per direction
        // # zh_CN 各方向追加台数
        controls.child(text(StatCollector.translateToLocal(LANG + "batch_count")));
        List<NumberEntryWidget> inputs = new ArrayList<>();
        for (ForgeDirection direction : new ForgeDirection[] { ForgeDirection.EAST, ForgeDirection.WEST,
            ForgeDirection.SOUTH, ForgeDirection.NORTH, ForgeDirection.UP, ForgeDirection.DOWN }) {
            String label = batchDirection(direction);
            NumberEntryWidget count = new NumberEntryWidget().bounds(0, ProjectorBatch.MAX_ADDITIONAL)
                .value(working.batch.additional(direction))
                .resetValue(0)
                .wheel(true)
                .textAlignment(Alignment.Center)
                .onChange(n -> working.batch.setAdditional(direction, (int) n))
                .size(68, SMALL)
                .name("batch_count_" + direction.ordinal());
            inputs.add(count);
            controls.child(
                Flow.row()
                    .widthRel(1f)
                    .height(SMALL)
                    .childPadding(4)
                    .child(
                        text(label).width(30)
                            .height(SMALL))
                    .child(count)
                    // #tr gtneverreset.projector.gui.batch_share
                    // # Share
                    // # zh_CN 共用外壳
                    .child(
                        new AECheckbox(
                            () -> StatCollector.translateToLocal(LANG + "batch_share"),
                            new BoolValue.Dynamic(
                                () -> working.batch.shares(direction),
                                on -> working.batch.setShared(direction, on)))
                                    .name("batch_share_" + direction.ordinal())
                                    // #tr gtneverreset.projector.gui.batch_share_hint
                                    // # Share matching casings, glass, frames, sheet metal or hatch models.
                                    // # zh_CN 相同的外壳、结构玻璃、框架、金属板或同型号仓室可共用。控制器各占一格。
                                    .tooltipBuilder(
                                        tooltip -> tooltip
                                            .addLine(StatCollector.translateToLocal(LANG + "batch_share_hint")))));
        }
        controls.child(
            Flow.row()
                .widthRel(1f)
                .height(SMALL)
                .childPadding(6)
                // #tr gtneverreset.projector.gui.batch_smart
                // # Auto-layout
                // # zh_CN 智能排布
                .child(
                    new AECheckbox(
                        () -> StatCollector.translateToLocal(LANG + "batch_smart"),
                        new BoolValue.Dynamic(() -> working.batch.faceOutward, on -> working.batch.faceOutward = on))
                            .name("batch_smart")
                            // #tr gtneverreset.projector.gui.batch_smart_hint
                            // # Try rotations and left-right mirrors to match shared faces and expose controllers.
                            // # zh_CN 尝试旋转和左右镜像，匹配共用面并优先让控制器朝外。
                            .tooltipBuilder(t -> t.addLine(StatCollector.translateToLocal(LANG + "batch_smart_hint"))))
                // #tr gtneverreset.projector.gui.batch_flip
                // # Alternate vertically
                // # zh_CN 上下交替翻转
                .child(
                    new AECheckbox(
                        () -> StatCollector.translateToLocal(LANG + "batch_flip"),
                        new BoolValue.Dynamic(
                            () -> working.batch.alternateVertical,
                            on -> working.batch.alternateVertical = on)).name("batch_flip")
                                // #tr gtneverreset.projector.gui.batch_flip_hint
                                // # Keep the origin level upright and turn every other vertical level upside down.
                                // # zh_CN 保留原点层的姿态，向上或向下每隔一层倒置。
                                .tooltipBuilder(
                                    t -> t.addLine(StatCollector.translateToLocal(LANG + "batch_flip_hint")))));
        // #tr gtneverreset.projector.gui.batch_total
        // # Total: %s machines (including the origin)
        // # zh_CN 共 %s 台（含原点）
        controls.child(
            IKey.dynamic(() -> StatCollector.translateToLocalFormatted(LANG + "batch_total", working.batch.machines()))
                .asWidget()
                .alignment(Alignment.CenterLeft)
                .widthRel(1f)
                .height(LINE));
        controls.child(deploymentStyleButton());
        controls.child(
            new AE2Button(() -> StatCollector.translateToLocal(LANG + "tab_batch"), () -> generate(true))
                .name("generate_batch")
                .widthRel(1f)
                .height(BUTTON)
                .available(
                    () -> canGenerate() && working.batch.machines() > 1
                        && inputs.stream()
                            .allMatch(NumberEntryWidget::isInputValid))
                .tooltipBuilder(this::buildHint));
        controls.child(
            IKey.dynamic(this::batchWarning)
                .asWidget()
                .widthRel(1f)
                .height(LINE)
                .alignment(Alignment.CenterLeft)
                .color(badColor)
                .tooltipBuilder(t -> t.addLine(batchWarning())));
        return controls;
    }

    private String batchWarning() {
        if (page != BATCH || !(previewStructure() instanceof ProjectorBatchPreview array)) return "";
        if (array.limited) {
            // #tr gtneverreset.projector.gui.batch_search_limit
            // # Layout search limit reached
            // # zh_CN 排布搜索达到上限
            return StatCollector.translateToLocal(LANG + "batch_search_limit");
        }
        if (!array.compatible) {
            // #tr gtneverreset.projector.gui.batch_no_fit
            // # Incompatible shared faces or orientation
            // # zh_CN 共用面或朝向不兼容
            return StatCollector.translateToLocal(LANG + "batch_no_fit");
        }
        // #tr gtneverreset.projector.gui.batch_blocked_controllers
        // # Obstructed controllers: %s
        // # zh_CN 正面受挡的控制器：%s 个
        return array.blockedControllers == 0 ? ""
            : StatCollector.translateToLocalFormatted(LANG + "batch_blocked_controllers", array.blockedControllers);
    }

    private static String batchDirection(ForgeDirection direction) {
        String key = switch (direction) {
            // #tr gtneverreset.projector.gui.batch_east
            // # East
            // # zh_CN 东
            case EAST -> "batch_east";
            // #tr gtneverreset.projector.gui.batch_west
            // # West
            // # zh_CN 西
            case WEST -> "batch_west";
            // #tr gtneverreset.projector.gui.batch_south
            // # South
            // # zh_CN 南
            case SOUTH -> "batch_south";
            // #tr gtneverreset.projector.gui.batch_north
            // # North
            // # zh_CN 北
            case NORTH -> "batch_north";
            // #tr gtneverreset.projector.gui.batch_up
            // # Up
            // # zh_CN 上
            case UP -> "batch_up";
            // #tr gtneverreset.projector.gui.batch_down
            // # Down
            // # zh_CN 下
            case DOWN -> "batch_down";
            default -> throw new IllegalArgumentException(direction.name());
        };
        return StatCollector.translateToLocal(LANG + key);
    }

    private boolean canGenerate() {
        return ProjectorConfig.buildEnabled && Minecraft.getMinecraft().thePlayer.capabilities.allowEdit
            && working.hasMachine()
            && !simulating()
            && structure() != null;
    }

    private void generate(boolean batch) {
        ProjectorStructure shape = structure();
        if (shape == null || !shape.settings.equals(working.settings())
            || !shape.failure.isEmpty()
            || !shape.limit.isEmpty()
            || shape.isEmpty()) {
            // #tr gtneverreset.projector.gui.cannot_generate
            // # A complete structure preview is required.
            // # zh_CN 请先取得完整的结构预览。
            notice("cannot_generate");
            return;
        }
        working.batchProjection = batch;
        ProjectorPlan target = working.copy();
        if (!ProjectorClient.locateBuild(target, shape, target.machine == stored.machine)) {
            // #tr gtneverreset.projector.gui.generate_target
            // # Aim at a loaded build site or lock the projection first.
            // # zh_CN 请瞄准已加载的放置位置，或先锁定投影。
            notice("generate_target");
            return;
        }
        working.readFromNBT(target.writeToNBT());
        ProjectorClient.send(location, working);
        stored.readFromNBT(working.writeToNBT());
        NetWorkHandler
            .sendToServer(batch ? new GenerateProjectorBatchPacket(location) : new GenerateProjectorPacket(location));
        getScreen().close();
    }

    private IWidget toolbar() {
        return new VerticalButtonBar()
            .button(
                () -> ClientConfig.projectorOutlinesThroughBlocks ? Icon.VISIBLE : Icon.INVISIBLE,
                // #tr gtneverreset.projector.gui.outline_through_blocks
                // # See through blocks
                // # zh_CN 透视
                () -> StatCollector.translateToLocal(LANG + "outline_through_blocks"),
                this::toggleSeeThrough)
            .button(
                () -> Icon.MAXIMIZE,
                // #tr gtneverreset.projector.gui.reset_view
                // # Reset view
                // # zh_CN 重置视角
                () -> StatCollector.translateToLocal(LANG + "reset_view"),
                preview::resetCamera)
            // #tr gtneverreset.projector.gui.raise_one
            // # Raise one block
            // # zh_CN 抬高一格
            .button(() -> Icon.ARROW_UP, () -> StatCollector.translateToLocal(LANG + "raise_one"), () -> raise(1))
            // #tr gtneverreset.projector.gui.lower_one
            // # Lower one block
            // # zh_CN 降低一格
            .button(() -> Icon.ARROW_DOWN, () -> StatCollector.translateToLocal(LANG + "lower_one"), () -> raise(-1))
            .toggle(
                () -> Icon.DOWNLOAD,
                // #tr gtneverreset.projector.gui.ground_snap
                // # Sit on the ground
                // # zh_CN 贴地放置
                () -> StatCollector.translateToLocal(LANG + "ground_snap"),
                () -> working.groundSnap,
                this::toggleGroundSnap)
            .button(() -> Icon.SHIFT_BACKWARD, this::turnTooltip, this::turnClick)
            .button(
                () -> Icon.REVERSE,
                // #tr gtneverreset.projector.pose.mirror
                // # Mirror left and right
                // # zh_CN 左右镜像
                () -> StatCollector.translateToLocal(POSE + "mirror"),
                () -> turnMachine(valid -> ProjectorOrientation.mirror(working.facing, valid)))
            .button(
                () -> Icon.FLIP_VERTICAL,
                // #tr gtneverreset.projector.pose.flip
                // # Mirror up and down
                // # zh_CN 上下镜像
                () -> StatCollector.translateToLocal(POSE + "flip"),
                () -> turnMachine(valid -> ProjectorOrientation.flipVertical(working.facing, valid)))
            .button(
                this::frontIcon,
                () -> ProjectorClient.frontLabel(working.facing.getDirection()),
                () -> turnMachine(
                    valid -> ProjectorOrientation.withFront(
                        working.facing,
                        ProjectorOrientation.nextFront(working.facing.getDirection()),
                        valid)))
            .button(
                () -> Icon.REFRESH,
                // #tr gtneverreset.projector.pose.rotate
                // # Turn around the front
                // # zh_CN 绕正面旋转
                () -> StatCollector.translateToLocal(POSE + "rotate"),
                () -> turnMachine(valid -> ProjectorOrientation.nextRotation(working.facing, valid)))
            .toggle(
                () -> Icon.MINIMIZE,
                // #tr gtneverreset.projector.gui.center_pivot
                // # Turn around the structure centre
                // # zh_CN 以结构中心旋转
                () -> StatCollector.translateToLocal(LANG + "center_pivot"),
                () -> working.centerPivot,
                () -> working.centerPivot = !working.centerPivot)
            .pos(0, 0);
    }

    private Icon frontIcon() {
        ForgeDirection front = working.facing.getDirection();
        return front == ForgeDirection.UP ? Icon.MOVE_TO_UP
            : front == ForgeDirection.DOWN ? Icon.MOVE_TO_DOWN : Icon.MOVE_TO;
    }

    private void toggleGroundSnap() {
        working.groundSnap = !working.groundSnap;
        if (working.groundSnap) working.verticalOffset = Math.max(0, working.verticalOffset);
    }

    private void turnTooltip(RichTooltip tooltip) {
        // #tr gtneverreset.projector.gui.turn
        // # Turn 90° horizontally
        // # zh_CN 水平旋转 90°
        tooltip.addLine(StatCollector.translateToLocal(LANG + "turn"));
        // #tr gtneverreset.projector.gui.turn_hint
        // # Left click: clockwise · Right click: counterclockwise
        // # zh_CN 左键顺时针，右键逆时针
        tooltip.addLine(EnumChatFormatting.DARK_GRAY + StatCollector.translateToLocal(LANG + "turn_hint"));
    }

    private boolean turnClick(int mouse) {
        if (mouse != 0 && mouse != 1) return false;
        int quarterTurns = mouse == 0 ? 1 : -1;
        turnMachine(valid -> ProjectorOrientation.rotateY(working.facing, quarterTurns, valid));
        return true;
    }

    private Widget<?> onPage(Widget<?> widget, int index) {
        return widget.setEnabledIf(w -> page == index);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (++ticks % REFRESH_TICKS == 0) machines.refreshCarried(Minecraft.getMinecraft().thePlayer);
        ProjectorStructure shape = previewStructure();
        if (shape instanceof ProjectorBatchPreview array && array.compatible && !array.outlineOnly)
            array.formation.update(array.template);
        materialStock.update(page == MATERIALS && shape != null ? shape.materials : null, ticks);
    }

    private void closed() {
        lastPage = page;
        lastLayerMachine = working.machine;
        lastLayer = preview.layer();
        preview.release();
        previewCache.clear();
        if (configChanged || hatches.configChanged()) ConfigRegistry.save(ClientConfig.class);
        if (working.isPlaced() && working.machine != stored.machine) working.state = ProjectorPlan.State.FOLLOW;
        if (!working.writeToNBT()
            .equals(stored.writeToNBT())) ProjectorClient.send(location, working);
    }

    ProjectorStructure previewStructure() {
        return previewCache.get(structure(), working, working.facing);
    }

    ProjectorPlan draftFor(int slot) {
        return slot == location ? working : null;
    }

    public ProjectorStructure structure() {
        if (!working.hasMachine()) return shown = null;
        ProjectorStructure current = ProjectorStructures.get(working.settings());
        if (current != null) shown = current;
        else if (shown != null && shown.settings.machine() != working.machine) shown = null;
        return shown;
    }

    private boolean simulating() {
        return working.hasMachine() && ProjectorStructures.get(working.settings()) == null;
    }

    public void selectMachine(int id) {
        if (id == working.machine) return;
        working.selectMachine(id);
        working.facing = ProjectorClient.initialFacing(id);
        clearSelection();
        machines.showMachine();
    }

    public void notice(String key) {
        notice.show(StatCollector.translateToLocal(LANG + key), IconRowList.BAD & 0xFFFFFF);
    }

    private void project() {
        if (!working.hasMachine()) {
            // #tr gtneverreset.projector.gui.no_machine
            // # Pick a machine first
            // # zh_CN 请先选择机器
            notice("no_machine");
            return;
        }
        if (!working.isPlaced() || working.machine != stored.machine) {
            if (working.state == ProjectorPlan.State.IDLE || working.machine != stored.machine) {
                ProjectorStructure structure = structure();
                if (!displayChosen)
                    working.display = structure != null && structure.size > ProjectorClient.BLOCK_PREVIEW_LIMIT
                        ? ProjectorPlan.Display.BOX
                        : ProjectorPlan.Display.DIFFERENCE;
            }
            working.state = ProjectorPlan.State.FOLLOW;
        }
        ProjectorClient.send(location, working);
        stored.readFromNBT(working.writeToNBT());
        getScreen().close();
    }

    private void cycleDisplay() {
        ProjectorPlan.Display[] values = ProjectorPlan.Display.values();
        working.display = values[(working.display.ordinal() + 1) % values.length];
        displayChosen = true;
    }

    private void clearPlan() {
        working.clear();
        clearSelection();
        machines.showMachine();
        ProjectorClient.send(location, working);
        stored.readFromNBT(working.writeToNBT());
    }

    private void toggleSeeThrough() {
        ClientConfig.projectorOutlinesThroughBlocks = !ClientConfig.projectorOutlinesThroughBlocks;
        configChanged = true;
    }

    private void raise(int step) {
        if (working.state == ProjectorPlan.State.LOCKED) working.y = Math.max(0, Math.min(255, working.y + step));
        else working.verticalOffset = Math.max(
            working.groundSnap ? 0 : -ProjectorPlan.MAX_OFFSET,
            Math.min(ProjectorPlan.MAX_OFFSET, working.verticalOffset + step));
    }

    private String projectLabel() {
        return StatCollector
            // #tr gtneverreset.projector.gui.project
            // # Project
            // # zh_CN 投影到世界
            // #tr gtneverreset.projector.gui.apply
            // # Apply
            // # zh_CN 应用修改
            .translateToLocal(LANG + (working.isPlaced() && working.machine == stored.machine ? "apply" : "project"));
    }

    private void turnMachine(Function<Predicate<ExtendedFacing>, ExtendedFacing> next) {
        if (working.hasMachine()) turn(next.apply(ProjectorMachines.validFacings(working.machine)));
    }

    private void turn(ExtendedFacing next) {
        if (working.state == ProjectorPlan.State.ANCHORED) {
            // #tr gtneverreset.projector.anchored
            // # An anchored plan cannot be moved or turned
            // # zh_CN 锚定的方案不能移动或旋转
            // #tr gtneverreset.projector.gui.anchored
            // # An anchored plan cannot be moved or turned
            // # zh_CN 锚定的方案不能移动或旋转
            notice("anchored");
        } else if (next == working.facing) {
            // #tr gtneverreset.projector.invalid_facing
            // # This machine cannot face that way
            // # zh_CN 这台机器不允许这个朝向
            // #tr gtneverreset.projector.gui.invalid_facing
            // # This machine cannot face that way
            // # zh_CN 这台机器不允许这个朝向
            notice("invalid_facing");
        } else {
            ProjectorClient.reorient(working, next, structure());
        }
    }

    private void craftMissingMaterials() {
        if (canCraftMaterials(previewStructure())) materialStock.craft(working);
    }

    private boolean canCraftMaterials(ProjectorStructure shape) {
        if (shape == null || shape.isEmpty()
            || !shape.settings.equals(working.settings())
            || !shape.failure.isEmpty()
            || !shape.limit.isEmpty()) return false;
        if (shape instanceof ProjectorBatchPreview array && (array.outlineOnly || !array.compatible || array.limited))
            return false;
        List<Material> materials = shape.materials;
        for (int i = 0; i < materials.size(); i++) {
            if (!materialStock.isCraftable(materials, i)) continue;
            int owned = materialStock.count(materials, i);
            if (owned >= 0 && owned < materials.get(i)
                .count()) return true;
        }
        return false;
    }

    private List<Row> materialRows() {
        List<Row> rows = new ArrayList<>();
        ProjectorStructure structure = previewStructure();
        if (structure == null) return rows;
        if (structure instanceof ProjectorBatchPreview array && array.outlineOnly) {
            rows.add(Row.header(StatCollector.translateToLocal(ProjectorClient.LANG + "batch_outline")));
            return rows;
        }
        rows.add(
            Row.header(
                StatCollector
                    // #tr gtneverreset.projector.gui.materials_total
                    // # %s kinds · %s blocks (available/required)
                    // # zh_CN %s 种 · %s 格（可用/用量）
                    .translateToLocalFormatted(LANG + "materials_total", structure.materials.size(), structure.size)));
        for (int i = 0; i < structure.materials.size(); i++) {
            Material material = structure.materials.get(i);
            ItemStack stack = material.stack();
            int owned = materialStock.count(structure.materials, i);
            rows.add(
                Row.colored(
                    stack,
                    stack.getDisplayName(),
                    (owned < 0 ? "…" : Integer.toString(owned)) + "/" + material.count(),
                    owned < 0 ? muted : owned >= material.count() ? IconRowList.GOOD : IconRowList.BAD)
                    .withKey(new MaterialRow(i, materialStock.isCraftable(structure.materials, i))));
        }
        return rows;
    }

    private void clearSelection() {
        selectedPositions.clear();
        selectionShape = null;
    }

    private int[] selectedCells() {
        ProjectorStructure shape = previewStructure();
        if (shape == null) return it.unimi.dsi.fastutil.ints.IntArrays.EMPTY_ARRAY;
        if (shape != selectionShape) {
            var cells = new it.unimi.dsi.fastutil.ints.IntArrayList(selectedPositions.size());
            boolean group = selectedPositions.size() > 1;
            var positions = selectedPositions.iterator();
            while (positions.hasNext()) {
                long key = positions.nextLong();
                int cell = shape
                    .indexOf(ProjectorPlan.unpackA(key), ProjectorPlan.unpackB(key), ProjectorPlan.unpackC(key));
                if (cell < 0 || group && shape.slotAt(cell) == null) positions.remove();
                else cells.add(cell);
            }
            selectionCells = cells.toIntArray();
            selectionShape = shape;
        }
        return selectionCells;
    }

    private void select(int cell) {
        select(cell, false);
    }

    private void select(int cell, boolean toggle) {
        ProjectorStructure structure = previewStructure();
        if (structure == null || cell < 0 || cell >= structure.size) return;
        long key = ProjectorPlan.pack(structure.a[cell], structure.b[cell], structure.c[cell]);
        if (toggle) {
            if (structure.slotAt(cell) == null) return;
            int[] current = selectedCells();
            if (current.length == 1 && structure.slotAt(current[0]) == null) clearSelection();
            if (!selectedPositions.remove(key)) selectedPositions.add(key);
        } else {
            selectedPositions.clear();
            selectedPositions.add(key);
        }
        selectionShape = null;
        Choice choice = structure.choiceAt(cell);
        if (structure.slotAt(cell) != null || choice != null && choice.channel() == null) page = HATCHES;
        else if (choice != null) page = PARAMETERS;
    }

    private String layerLabel() {
        int layer = preview.layer();
        // #tr gtneverreset.projector.gui.all_layers
        // # All layers
        // # zh_CN 全部层
        return layer == ProjectorPreviewWidget.ALL_LAYERS ? StatCollector.translateToLocal(LANG + "all_layers")
            // #tr gtneverreset.projector.gui.layer
            // # Layer %s
            // # zh_CN 第 %s 层
            : StatCollector.translateToLocalFormatted(LANG + "layer", layer);
    }

    private String summaryLine() {
        if (!working.hasMachine()) return "";
        String name = ProjectorMachines.stackOf(working.machine)
            .getDisplayName();
        ProjectorStructure displayed = previewStructure();
        String state;
        if (displayed == null || simulating()) {
            ProjectorSimulation running = ProjectorStructures.running(working.settings());
            // #tr gtneverreset.projector.gui.simulating
            // # Working out the structure: round %s
            // # zh_CN 正在推算结构：第 %s 轮
            state = StatCollector.translateToLocalFormatted(LANG + "simulating", running == null ? 0 : running.rounds);
        } else {
            int[] bounds = displayed.worldBounds(working.facing);
            int width = bounds[3] - bounds[0] + 1, height = bounds[4] - bounds[1] + 1,
                depth = bounds[5] - bounds[2] + 1;
            if (displayed instanceof ProjectorBatchPreview array && array.outlineOnly) {
                // #tr gtneverreset.projector.gui.array_outline
                // # Array outline · %s×%s×%s
                // # zh_CN 阵列外框 · %s×%s×%s
                state = StatCollector.translateToLocalFormatted(LANG + "array_outline", width, height, depth);
            } else {
                // #tr gtneverreset.projector.gui.summary
                // # %s blocks · %s×%s×%s
                // # zh_CN %s 格 · %s×%s×%s
                state = StatCollector.translateToLocalFormatted(LANG + "summary", displayed.size, width, height, depth);
            }
        }
        return StatCollector.translateToLocalFormatted(ProjectorClient.LANG + "joined", name, state);
    }

    private Widget<?> statusIcon() {
        IDrawable look = (context, x, y, width, height, theme) -> {
            ProjectorStructure structure = finishedStructure();
            if (structure instanceof ProjectorBatchPreview array && !hasProblem(structure)) {
                var status = array.formation;
                boolean unknown = array.outlineOnly
                    || array.compatible && (status.finished < status.total || status.unchecked > 0);
                boolean formed = array.compatible && status.formed == status.total;
                Icon icon = unknown ? Icon.MORE : formed ? Icon.CHECKMARK : Icon.EXCLAMATION;
                AE2Draw.drawTintedIcon(icon, x, y, unknown ? muted : formed ? goodColor : badColor);
                return;
            }
            if (structure == null || !hasProblem(structure) && !structure.checked) return;
            boolean good = !hasProblem(structure) && structure.formed;
            AE2Draw.drawTintedIcon(good ? Icon.CHECKMARK : Icon.EXCLAMATION, x, y, good ? goodColor : badColor);
        };
        return new Widget<>().size(AE2Theme.ICON_SIZE, AE2Theme.ICON_SIZE)
            .top(STATUS_INSET)
            .right(STATUS_INSET)
            // Sibling backgrounds are all drawn before the preview, which would paint over this one.
            .overlay(look)
            .disableThemeBackground(true)
            .disableHoverThemeBackground(true)
            .tooltipAutoUpdate(true)
            .tooltipBuilder(this::statusTooltip);
    }

    private ProjectorStructure finishedStructure() {
        return simulating() ? null : previewStructure();
    }

    private static boolean hasProblem(ProjectorStructure structure) {
        return !structure.failure.isEmpty() || !structure.limit.isEmpty();
    }

    private void statusTooltip(RichTooltip tooltip) {
        ProjectorStructure structure = finishedStructure();
        if (structure == null) return;
        if (!structure.failure.isEmpty()) tooltip.addLine(
            EnumChatFormatting.RED + StatCollector
                // #tr gtneverreset.projector.gui.failure
                // # Simulation failed: %s
                // # zh_CN 模拟失败：%s
                .translateToLocalFormatted(LANG + "failure", StatCollector.translateToLocal(structure.failure)));
        else if (!structure.limit.isEmpty())
            // #tr gtneverreset.projector.gui.limit.height
            // # Taller than the world (y 0–255), only part is shown
            // # zh_CN 结构高过世界（y 0–255），只显示一部分
            // #tr gtneverreset.projector.gui.limit.size
            // # Over 250,000 blocks, only part is shown
            // # zh_CN 超过 25 万格，只显示一部分
            // #tr gtneverreset.projector.gui.limit.rounds
            // # The structure took too many building rounds, only part is shown
            // # zh_CN 推算轮数超过上限，只显示一部分
            tooltip.addLine(EnumChatFormatting.RED + StatCollector.translateToLocal(LANG + "limit." + structure.limit));
        else if (structure instanceof ProjectorBatchPreview array) batchStatusTooltip(tooltip, array);
        else if (!structure.checked) return;
        else if (structure.formed)
            // #tr gtneverreset.projector.gui.formed
            // # Forms with these settings
            // # zh_CN 按当前设置可以成型
            tooltip.addLine(EnumChatFormatting.GREEN + StatCollector.translateToLocal(LANG + "formed"));
        else {
            // #tr gtneverreset.projector.gui.not_formed
            // # Does not form yet:
            // # zh_CN 还不能成型：
            tooltip.addLine(EnumChatFormatting.RED + StatCollector.translateToLocal(LANG + "not_formed"));
            for (String error : structure.errors) tooltip.addLine(EnumChatFormatting.GRAY + error);
        }
    }

    private void batchStatusTooltip(RichTooltip tooltip, ProjectorBatchPreview array) {
        if (array.outlineOnly) {
            tooltip.addLine(StatCollector.translateToLocal(ProjectorClient.LANG + "batch_outline"));
            return;
        }
        if (!array.compatible) {
            tooltip.addLine(
                EnumChatFormatting.RED
                    + StatCollector.translateToLocal(LANG + (array.limited ? "batch_search_limit" : "batch_no_fit")));
            return;
        }
        var status = array.formation;
        if (status.finished < status.total) {
            // #tr gtneverreset.projector.gui.batch_checking
            // # Checking controllers: %s/%s
            // # zh_CN 正在检查控制器：%s/%s 台
            tooltip.addLine(
                StatCollector.translateToLocalFormatted(LANG + "batch_checking", status.finished, status.total));
            return;
        }
        // #tr gtneverreset.projector.gui.batch_formed
        // # Controllers that can form: %s/%s
        // # zh_CN 可成型：%s/%s 台
        tooltip.addLine(
            (status.formed == status.total ? EnumChatFormatting.GREEN : EnumChatFormatting.YELLOW)
                + StatCollector.translateToLocalFormatted(LANG + "batch_formed", status.formed, status.total));
        status.errors.forEach((error, count) -> {
            // #tr gtneverreset.projector.gui.batch_error_count
            // # %s (%s/%s controllers)
            // # zh_CN %s（%s/%s 台）
            tooltip.addLine(
                EnumChatFormatting.GRAY
                    + StatCollector.translateToLocalFormatted(LANG + "batch_error_count", error, count, status.total));
        });
        if (status.unchecked > 0) {
            // #tr gtneverreset.projector.gui.batch_unchecked
            // # Could not check %s controllers
            // # zh_CN 有 %s 台未能完成结构检查
            tooltip.addLine(StatCollector.translateToLocalFormatted(LANG + "batch_unchecked", status.unchecked));
        }
    }

    private String hoverLine() {
        ProjectorStructure structure = previewStructure();
        int cell = preview.hovered();
        if (structure == null || cell < 0 || cell >= structure.size) return "";
        return structure.items[cell] == null ? structure.blocks[cell].getLocalizedName()
            : structure.items[cell].getDisplayName();
    }

    @Desugar
    private record MaterialRow(int index, boolean craftable) {}

    private final class MaterialList extends IconRowList {

        private MaterialList() {
            super(ProjectorGui.this::materialRows);
        }

        @Override
        protected void drawItemIcon(ModularGuiContext context, WidgetThemeEntry<?> theme, Row row, int x, int y) {
            super.drawItemIcon(context, theme, row, x, y);
            if (!(row.key() instanceof MaterialRow material) || !material.craftable()) return;
            var overlay = AEItemStack.create(row.icon());
            if (overlay == null) return;
            GL11.glPushMatrix();
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
            try {
                overlay.setStackSize(1)
                    .setCraftable(true)
                    .drawOverlayInGui(Minecraft.getMinecraft(), x, y, false, false, true, true);
            } finally {
                GL11.glPopAttrib();
                GL11.glPopMatrix();
            }
        }
    }

}
