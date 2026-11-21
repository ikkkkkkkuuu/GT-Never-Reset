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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.inventory.InventoryTypes;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.item.items.MultiblockProjector;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorOrientation;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorSimulation;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Choice;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Material;
import com.xyp.gtneverreset.config.ClientConfig;
import com.xyp.gtneverreset.config.ConfigRegistry;
import com.xyp.gtneverreset.config.ScreenThemeConfig;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Icon;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.widget.ActionButton;
import com.xyp.gtnrlib.gui.widget.FadingNotice;
import com.xyp.gtnrlib.gui.widget.IconRowList;
import com.xyp.gtnrlib.gui.widget.IconRowList.Row;
import com.xyp.gtnrlib.gui.widget.ListRow;
import com.xyp.gtnrlib.gui.widget.VerticalButtonBar;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

@SideOnly(Side.CLIENT)
public class ProjectorGui extends ModularPanel {

    public static final String LANG = "gtneverreset.projector.gui.", POSE = "gtneverreset.projector.pose.";
    public static final int MACHINES = 0, PARAMETERS = 1, HATCHES = 2, MATERIALS = 3;
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
    private static final String[] TAB_NAMES = { "tab_machines", "tab_parameters", "tab_hatches", "tab_materials" };
    private static final Icon[] TAB_ICONS = { Icon.BLOCK, Icon.GEAR, Icon.PROCESSOR, Icon.FILE };
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
    private final ProjectorPreviewWidget preview;
    private final ProjectorMachineTab machines;
    private Int2IntOpenHashMap have = new Int2IntOpenHashMap();
    private ProjectorStructure shown;
    private int page, ticks;
    private final FadingNotice notice = new FadingNotice().duration(NOTICE_MILLIS);
    private boolean hasSelection, displayChosen;
    private int selectedA, selectedB, selectedC;

    public ProjectorGui(PlayerInventoryGuiData data) {
        super(NAME);
        size(WIDTH, HEIGHT);
        background(AE2Theme.panelBackground(BODY_LEFT, BODY_RIGHT)).disableThemeBackground(true)
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
        refreshInventory(player);
        if (!working.hasMachine() && machines.firstCarried() > 0) selectMachine(machines.firstCarried());
        page = !working.hasMachine() ? MACHINES : lastPage >= 0 ? lastPage : PARAMETERS;
        int[] spot = ProjectorClient.takeSpot(working.machine);
        if (spot != null) {
            hasSelection = true;
            selectedA = spot[0];
            selectedB = spot[1];
            selectedC = spot[2];
            page = HATCHES;
        }
        preview = new ProjectorPreviewWidget(this::structure).selection(this::selectedCell, this::select)
            .markers(() -> ClientConfig.projectorHatchOutlines, working)
            .facing(() -> working.hasMachine() ? working.facing : null);
        if (working.machine == lastLayerMachine) preview.restoreLayer(lastLayer);
        hatches = new ProjectorHatchPage(this::structure, working, this::selectedCell, this::select);
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
        return AE2Theme.well(wellWidth, PLAIN_WELL);
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
            // # Multiblock Projector
            // # zh_CN 多方块投影仪
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
                    .child(ActionButton.icon(() -> Icon.REMOVE, () -> preview.stepLayer(-1)))
                    .child(
                        IKey.dynamic(this::layerLabel)
                            .asWidget()
                            .alignment(Alignment.Center)
                            .width(32)
                            .height(BUTTON))
                    .child(ActionButton.icon(() -> Icon.ADD, () -> preview.stepLayer(1)))
                    .child(
                        ActionButton.text(
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
                            this::cycleDisplay)
                            .expanded()
                            .height(BUTTON)));
    }

    private IWidget sideColumn() {
        ParentWidget<?> pages = new ParentWidget<>().size(RIGHT_WIDTH, PAGE_HEIGHT);
        pages.child(onPage(machines.build(), MACHINES));
        pages.child(onPage(new ProjectorParameterTab(this).build(), PARAMETERS));
        pages.child(onPage(new ProjectorHatchTab(this).build(), HATCHES));
        pages.child(onPage(new IconRowList(this::materialRows).sizeRel(1f), MATERIALS));
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
                        ActionButton.text(this::projectLabel, this::project)
                            .expanded()
                            .height(BUTTON))
                    .child(
                        // #tr gtneverreset.projector.gui.clear
                        // # Clear plan
                        // # zh_CN 清除方案
                        ActionButton.text(() -> StatCollector.translateToLocal(LANG + "clear"), this::clearPlan)
                            .width(RIGHT_WIDTH * 2 / 5)
                            .height(BUTTON)));
    }

    private void buildTabs() {
        for (int i = 0; i < TAB_ICONS.length; i++) {
            int index = i;
            ButtonWidget<?> tab = new ButtonWidget<>();
            IDrawable look = AE2Theme.pageTab(tab, () -> TAB_ICONS[index], () -> page == index);
            child(
                tab.background(look)
                    .hoverBackground(look)
                    .disableHoverThemeBackground(true)
                    .pos(WIDTH - BODY_RIGHT - AE2Theme.PAGE_TAB_INSET, TABS_TOP + i * AE2Theme.PAGE_TAB_PITCH)
                    .size(AE2Theme.PAGE_TAB_SIZE, AE2Theme.PAGE_TAB_SIZE)
                    .tooltipBuilder(tooltip -> tooltip.addLine(StatCollector.translateToLocal(LANG + TAB_NAMES[index])))
                    .onMousePressed(mouse -> {
                        page = index;
                        return true;
                    }));
        }
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
        if (++ticks % REFRESH_TICKS == 0) refreshInventory(Minecraft.getMinecraft().thePlayer);
    }

    private void closed() {
        lastPage = page;
        lastLayerMachine = working.machine;
        lastLayer = preview.layer();
        preview.release();
        if (configChanged || hatches.configChanged()) ConfigRegistry.save(ClientConfig.class);
        if (working.isPlaced() && working.machine != stored.machine) working.state = ProjectorPlan.State.FOLLOW;
        if (!working.writeToNBT()
            .equals(stored.writeToNBT())) ProjectorClient.send(location, working);
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
        hasSelection = false;
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
        hasSelection = false;
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

    private void refreshInventory(EntityPlayer player) {
        have = ProjectorClient.inventory(player);
        machines.refreshCarried(player);
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

    private List<Row> materialRows() {
        List<Row> rows = new ArrayList<>();
        ProjectorStructure structure = structure();
        if (structure == null) return rows;
        rows.add(
            Row.header(
                StatCollector
                    // #tr gtneverreset.projector.gui.materials_total
                    // # %s kinds · %s blocks
                    // # zh_CN %s 种 · %s 格
                    .translateToLocalFormatted(LANG + "materials_total", structure.materials.size(), structure.size)));
        for (Material material : structure.materials) {
            ItemStack stack = material.stack();
            int owned = have.get(Choice.idOf(stack.getItem(), stack.getItemDamage()));
            rows.add(
                Row.colored(
                    stack,
                    stack.getDisplayName(),
                    Math.min(owned, material.count()) + "/" + material.count(),
                    owned >= material.count() ? IconRowList.GOOD : IconRowList.BAD));
        }
        return rows;
    }

    private int selectedCell() {
        ProjectorStructure structure = structure();
        return structure == null || !hasSelection ? -1 : structure.indexOf(selectedA, selectedB, selectedC);
    }

    private void select(int cell) {
        ProjectorStructure structure = structure();
        if (structure == null || cell < 0) return;
        hasSelection = true;
        selectedA = structure.a[cell];
        selectedB = structure.b[cell];
        selectedC = structure.c[cell];
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
        ProjectorStructure structure = structure();
        String state;
        if (structure == null || simulating()) {
            ProjectorSimulation running = ProjectorStructures.running(working.settings());
            // #tr gtneverreset.projector.gui.simulating
            // # Working out the structure: round %s
            // # zh_CN 正在推算结构：第 %s 轮
            state = StatCollector.translateToLocalFormatted(LANG + "simulating", running == null ? 0 : running.rounds);
        } else {
            state = StatCollector.translateToLocalFormatted(
                // #tr gtneverreset.projector.gui.summary
                // # %s blocks · %s×%s×%s
                // # zh_CN %s 格 · %s×%s×%s
                LANG + "summary",
                structure.size,
                structure.maxA - structure.minA + 1,
                structure.maxB - structure.minB + 1,
                structure.maxC - structure.minC + 1);
        }
        return StatCollector.translateToLocalFormatted(ProjectorClient.LANG + "joined", name, state);
    }

    private Widget<?> statusIcon() {
        IDrawable look = (context, x, y, width, height, theme) -> {
            ProjectorStructure structure = finishedStructure();
            if (structure == null || !hasProblem(structure) && !structure.checked) return;
            boolean good = !hasProblem(structure) && structure.formed;
            AE2Theme.drawTintedIcon(good ? Icon.CHECKMARK : Icon.EXCLAMATION, x, y, good ? goodColor : badColor);
        };
        return new Widget<>().size(AE2Theme.ICON_SIZE, AE2Theme.ICON_SIZE)
            .top(STATUS_INSET)
            .right(STATUS_INSET)
            .background(look)
            .disableThemeBackground(true)
            .disableHoverThemeBackground(true)
            .tooltipAutoUpdate(true)
            .tooltipBuilder(this::statusTooltip);
    }

    private ProjectorStructure finishedStructure() {
        return simulating() ? null : structure();
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

    private String hoverLine() {
        ProjectorStructure structure = structure();
        int cell = preview.hovered();
        if (structure == null || cell < 0 || cell >= structure.size) return "";
        return structure.items[cell] == null ? structure.blocks[cell].getLocalizedName()
            : structure.items[cell].getDisplayName();
    }
}
