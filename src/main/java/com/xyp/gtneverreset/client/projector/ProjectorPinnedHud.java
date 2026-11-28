package com.xyp.gtneverreset.client.projector;

import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.GuiScreenWrapper;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.RecipeViewerSettingsImpl;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.projector.ProjectorBatch;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorOrientation;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtnrlib.animation.StructureDeploymentStyle;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Icon;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;
import com.xyp.gtnrlib.gui.theme.ae2.AE2PanelPainter;
import com.xyp.gtnrlib.gui.widget.AE2Button;
import com.xyp.gtnrlib.gui.widget.IconButton;
import com.xyp.gtnrlib.gui.widget.NumberEntryWidget;
import com.xyp.gtnrlib.gui.widget.VerticalButtonBar;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ProjectorPinnedHud {

    private static final String LANG = ProjectorClient.LANG + "pinned.";
    private static final String NAME = "projector_pinned", CONTROL = NAME + "_";
    private static final int BODY_EDGE = VerticalButtonBar.WIDTH - VerticalButtonBar.OVERLAP;
    private static final int PAD = 5, BODY_X = BODY_EDGE + PAD, AXIS_WIDTH = 10, COLUMN_GAP = 3;
    private static final int POSITION_WIDTH = 78, COPIES_WIDTH = 25, GAP_WIDTH = 25;
    private static final int POSITION_X = BODY_X + AXIS_WIDTH;
    private static final int COPIES_X = POSITION_X + POSITION_WIDTH + COLUMN_GAP;
    private static final int GAP_X = COPIES_X + COPIES_WIDTH + COLUMN_GAP;
    private static final int WIDTH = GAP_X + GAP_WIDTH + PAD, HEIGHT = 7 * VerticalButtonBar.PITCH + 6;
    private static final int BODY_WIDTH = WIDTH - BODY_X - PAD, BUTTON_WIDTH = (BODY_WIDTH - COLUMN_GAP) / 2;
    private static final int LABEL_HEIGHT = 10, FIELD_HEIGHT = 14, ROW_Y = 34, ROW_PITCH = 16;
    private static final int WORLD_BORDER = 30_000_000;

    private static PinnedScreen pendingEditor;
    private PinnedScreen passive;

    public static void openEditor() {
        Minecraft mc = Minecraft.getMinecraft();
        if (pendingEditor != null || mc.currentScreen != null || ProjectorClient.pinnedPlan() == null) return;
        pendingEditor = new PinnedScreen(true);
        ClientGUI.open(pendingEditor, recipeViewer());
    }

    public static void closeEditor() {
        if (pendingEditor != null) pendingEditor.cancelOpening = true;
        if (ModularScreen.getCurrent() instanceof PinnedScreen editor) editor.close();
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || mc.currentScreen != null
            || mc.theWorld == null
            || mc.gameSettings.hideGUI
            || mc.gameSettings.showDebugInfo
            || ProjectorClient.pinnedPlan() == null) return;
        if (passive == null) passive = new PinnedScreen(false);
        passive.drawHud(event.resolution.getScaledWidth(), event.resolution.getScaledHeight(), event.partialTicks);
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (event.world.isRemote) passive = null;
    }

    private static RecipeViewerSettingsImpl recipeViewer() {
        RecipeViewerSettingsImpl settings = new RecipeViewerSettingsImpl();
        settings.disable();
        return settings;
    }

    private static boolean locked() {
        ProjectorPlan plan = ProjectorClient.pinnedPlan();
        return plan != null && plan.state == ProjectorPlan.State.LOCKED;
    }

    private static boolean canToggleLock() {
        ProjectorPlan plan = ProjectorClient.pinnedPlan();
        return plan != null && plan.state != ProjectorPlan.State.ANCHORED;
    }

    private static int signedCopies(Axis axis) {
        ProjectorPlan plan = ProjectorClient.pinnedPlan();
        return plan == null ? 1 : plan.batch.signedCopies(axis.positive);
    }

    private static void setCopies(Axis axis, int count) {
        ProjectorClient.editPinned(next -> next.batch.setSignedCopies(axis.positive, count));
    }

    private static boolean hasMixedCopies(Axis axis) {
        ProjectorPlan plan = ProjectorClient.pinnedPlan();
        return plan != null && plan.batch.hasBothDirections(axis.positive);
    }

    private static int gap(Axis axis) {
        ProjectorPlan plan = ProjectorClient.pinnedPlan();
        return plan == null ? 0 : plan.batch.gap(axis.positive);
    }

    private static String tr(String key) {
        return StatCollector.translateToLocal(LANG + key);
    }

    private enum Axis {

        X(ForgeDirection.EAST, -WORLD_BORDER, WORLD_BORDER),
        Y(ForgeDirection.UP, 0, 255),
        Z(ForgeDirection.SOUTH, -WORLD_BORDER, WORLD_BORDER);

        private final ForgeDirection positive;
        private final int min, max;
        private final String id = name().toLowerCase(Locale.ROOT);

        Axis(ForgeDirection positive, int min, int max) {
            this.positive = positive;
            this.min = min;
            this.max = max;
        }
    }

    private static final class PinnedScreen extends ModularScreen {

        private final boolean editable;
        private ProjectorPlan lastPlan;
        private int lastX, lastY, lastZ;
        private boolean cancelOpening;

        private PinnedScreen(boolean editable) {
            super(GTNeverReset.MODID, new PinnedPanel(editable));
            this.editable = editable;
            useTheme(AE2Theme.DARK_ID);
            drawDarkBackground(false);
            if (!editable) {
                getContext().setSettings(new UISettings(recipeViewer()));
                new GuiScreenWrapper(this);
            }
        }

        @Override
        public void onOpen() {
            if (pendingEditor == this) pendingEditor = null;
        }

        @Override
        public void onUpdate() {
            if (editable && (cancelOpening || ProjectorClient.pinnedPlan() == null)) {
                close();
                return;
            }
            super.onUpdate();
        }

        private void drawHud(int width, int height, float partialTicks) {
            ModularGuiContext context = getContext();
            if (getScreenArea().width != width || getScreenArea().height != height) onResize(width, height);
            ProjectorPlan plan = ProjectorClient.pinnedPlan();
            if (plan != lastPlan || lastX != ProjectorClient.x
                || lastY != ProjectorClient.y
                || lastZ != ProjectorClient.z) {
                lastPlan = plan;
                lastX = ProjectorClient.x;
                lastY = ProjectorClient.y;
                lastZ = ProjectorClient.z;
                onUpdate();
            }
            // Use the same native panel widget tree without attaching it to Minecraft's input or clearing world depth.
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushMatrix();
            try {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_LIGHTING);
                context.updateState(-1, -1, partialTicks);
                context.reset();
                context.pushViewport(null, getScreenArea());
                try {
                    context.updateZ(0);
                    WidgetTree.drawTree(getMainPanel(), context);
                } finally {
                    context.popViewport(null);
                }
            } finally {
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            }
        }
    }

    private static final class PinnedPanel extends ModularPanel {

        private static final int MACHINE_WIDTH = BODY_WIDTH - 42;

        private ProjectorStructure lastShape;
        private ExtendedFacing lastFacing;
        private String dimensions = "-";

        private PinnedPanel(boolean editable) {
            super(NAME);
            size(WIDTH, HEIGHT).rightRel(0.06F)
                .topRel(0.38F, 0, 0.5F);
            background(AE2PanelPainter.panelBackground(this, BODY_EDGE, 0)).disableThemeBackground(true)
                .disableHoverBackground();

            child(label(this::machineName, BODY_X, 8, MACHINE_WIDTH, Alignment.CenterLeft, true));
            child(label(PinnedPanel::stateLabel, BODY_X + MACHINE_WIDTH, 8, 42, Alignment.CenterRight, true));
            // #tr gtneverreset.projector.pinned.position
            // # Position
            // # zh_CN 位置
            child(label(() -> tr("position"), POSITION_X, 22, POSITION_WIDTH, Alignment.CenterLeft, false));
            // #tr gtneverreset.projector.pinned.copies
            // # Qty
            // # zh_CN 份数
            child(label(() -> tr("copies"), COPIES_X, 22, COPIES_WIDTH, Alignment.Center, false));
            // #tr gtneverreset.projector.pinned.gap
            // # Gap
            // # zh_CN 间隔
            child(label(() -> tr("gap"), GAP_X, 22, GAP_WIDTH, Alignment.Center, false));
            for (Axis axis : Axis.values()) addAxis(axis, editable);

            child(label(this::summary, BODY_X, 85, BODY_WIDTH, Alignment.Center, false));
            int right = WIDTH - PAD - BUTTON_WIDTH;
            child(
                button(PinnedPanel::lockAction, ProjectorClient::togglePinnedLock, "aim", BODY_X, 99)
                    .available(ProjectorPinnedHud::canToggleLock));
            // #tr gtneverreset.projector.pinned.full_gui
            // # Full GUI
            // # zh_CN 完整界面
            child(button(() -> tr("full_gui"), () -> {
                closeEditor();
                ProjectorClient.openPinnedGui();
            }, "full_gui", right, 99));
            // #tr gtneverreset.projector.pinned.build
            // # Build
            // # zh_CN 搭建
            child(
                button(() -> tr("build"), ProjectorClient::buildPinned, "build", BODY_X, 119)
                    .available(ProjectorClient::canBuildPinned));
            child(
                button(PinnedPanel::buildStyle, ProjectorClient::cyclePinnedBuildStyle, "build_style", right, 119)
                    .tooltipBuilder(
                        tooltip -> tooltip
                            // #tr gtneverreset.projector.pinned.cycle_style
                            // # Change the deployment animation
                            // # zh_CN 切换搭建动画
                            .addLine(tr("cycle_style"))));
            child(
                button(
                    PinnedPanel::copyHatchesLabel,
                    () -> ProjectorClient.editPinned(next -> next.batch.copyHatches = !next.batch.copyHatches),
                    "copy_hatches",
                    right,
                    139).tooltipBuilder(
                        tooltip -> tooltip
                            // #tr gtneverreset.projector.pinned.copy_hatches_hint
                            // # Copy hatch positions and models from the origin to its other copies.
                            // # zh_CN 将原点主机的仓室位置和型号映射到其他副本。
                            .addLine(tr("copy_hatches_hint"))));
            // #tr gtneverreset.projector.pinned.edit_hint
            // # Hold Left Alt
            // # zh_CN 按住左 Alt
            child(label(() -> tr("edit_hint"), BODY_X, 143, BUTTON_WIDTH, Alignment.Center, false));
            child(toolbar().pos(0, 0));
        }

        @Override
        public boolean shouldAnimate() {
            return false;
        }

        private void addAxis(Axis axis, boolean editable) {
            int y = ROW_Y + axis.ordinal() * ROW_PITCH;
            child(label(axis::name, BODY_X, y + 2, AXIS_WIDTH, Alignment.CenterLeft, false));
            child(
                number(axis.min, axis.max, POSITION_WIDTH, POSITION_X, y, "position_" + axis.id)
                    .follow(() -> ProjectorClient.pinnedCoordinate(axis.positive))
                    .onChange(value -> ProjectorClient.setPinnedCoordinate(axis.positive, (int) value))
                    .available(ProjectorClient::canMovePinned));
            NumberEntryWidget copies = number(
                -ProjectorBatch.MAX_COPIES,
                ProjectorBatch.MAX_COPIES,
                COPIES_WIDTH,
                COPIES_X,
                y,
                "copies_" + axis.id).compactArrows(false)
                    .resetValue(1)
                    .value(signedCopies(axis))
                    .onChange(value -> setCopies(axis, (int) value));
            if (!editable) copies.follow(() -> signedCopies(axis));
            child(copies.setEnabledIf(widget -> !hasMixedCopies(axis)));
            Widget<?> mixed = IKey.dynamic(() -> mixedLabel(axis))
                .scale(0.6F)
                .asWidget()
                .alignment(Alignment.Center)
                .pos(COPIES_X, y)
                .size(COPIES_WIDTH, FIELD_HEIGHT)
                .color(
                    AE2Look.of(AE2Theme.Variant.DARK)
                        .panelText(true))
                .setEnabledIf(widget -> hasMixedCopies(axis));
            mixed.background(AE2PanelPainter.well(mixed, COPIES_WIDTH))
                .tooltipBuilder(
                    tooltip -> tooltip
                        // #tr gtneverreset.projector.pinned.copies_both
                        // # Both directions are in use. Edit them in the full interface.
                        // # zh_CN 此轴双向排列，可在完整界面修改。
                        .addLine(tr("copies_both")));
            child(mixed);
            child(
                number(-ProjectorBatch.MAX_GAP, ProjectorBatch.MAX_GAP, GAP_WIDTH, GAP_X, y, "gap_" + axis.id)
                    .compactArrows(false)
                    .follow(() -> gap(axis))
                    .onChange(
                        value -> ProjectorClient.editPinned(next -> next.batch.setGap(axis.positive, (int) value))));
        }

        private VerticalButtonBar toolbar() {
            VerticalButtonBar bar = new VerticalButtonBar();
            bar.button(() -> Icon.SHIFT_BACKWARD, PinnedPanel::lockAction, ProjectorClient::togglePinnedLock)
                .available(ProjectorPinnedHud::canToggleLock);
            // #tr gtneverreset.projector.pinned.mirror
            // # Mirror horizontally
            // # zh_CN 水平镜像
            bar.button(() -> Icon.REVERSE, () -> tr("mirror"), () -> turn(0));
            // #tr gtneverreset.projector.pinned.flip
            // # Flip vertically
            // # zh_CN 垂直翻转
            bar.button(() -> Icon.FLIP_VERTICAL, () -> tr("flip"), () -> turn(1));
            bar.button(
                () -> Icon.MOVE_TO,
                () -> StatCollector.translateToLocal("key.gtneverreset.projector_display"),
                () -> {
                    ProjectorPlan plan = ProjectorClient.pinnedPlan();
                    if (plan != null) ProjectorInput.cycleDisplay(plan);
                });
            // #tr gtneverreset.projector.pinned.rotate
            // # Rotate clockwise
            // # zh_CN 顺时针旋转
            bar.button(() -> Icon.REFRESH, () -> tr("rotate"), () -> turn(2));
            // #tr gtneverreset.projector.pinned.unpin
            // # Unpin from the HUD
            // # zh_CN 取消固定
            bar.add(new IconButton(() -> Icon.DOWNLOAD, () -> {
                ProjectorClient.unpinControls();
                closeEditor();
            }).selected(() -> true)
                .name(CONTROL + "unpin")
                .tooltipBuilder(tooltip -> tooltip.addLine(tr("unpin"))));
            // #tr gtneverreset.projector.pinned.close
            // # Close controls
            // # zh_CN 关闭控制面板
            bar.button(() -> Icon.CLOSE, () -> tr("close"), ProjectorPinnedHud::closeEditor);
            return bar;
        }

        private Widget<?> label(Supplier<String> text, int x, int y, int width, Alignment alignment, boolean strong) {
            return IKey.dynamic(text)
                .asWidget()
                .alignment(alignment)
                .pos(x, y)
                .size(width, LABEL_HEIGHT)
                .color(
                    AE2Look.of(AE2Theme.Variant.DARK)
                        .panelText(strong));
        }

        private static AE2Button button(Supplier<String> text, Runnable action, String id, int x, int y) {
            return new AE2Button(text, action).name(CONTROL + id)
                .pos(x, y)
                .size(BUTTON_WIDTH, 17);
        }

        private static NumberEntryWidget number(int min, int max, int width, int x, int y, String id) {
            return new NumberEntryWidget().bounds(min, max)
                .wheel(true)
                .textAlignment(Alignment.Center)
                .name(CONTROL + id)
                .pos(x, y)
                .size(width, FIELD_HEIGHT);
        }

        private String machineName() {
            ProjectorPlan plan = ProjectorClient.pinnedPlan();
            ItemStack stack = plan == null ? null : ProjectorMachines.stackOf(plan.machine);
            return stack == null ? "" : ProjectorWidgets.trim(stack.getDisplayName(), MACHINE_WIDTH - 3);
        }

        private static String stateLabel() {
            ProjectorPlan plan = ProjectorClient.pinnedPlan();
            if (plan == null) return "";
            String state = StatCollector.translateToLocal("item.gtneverreset.multiblock_projector.state." + plan.state);
            return (plan.state == ProjectorPlan.State.FOLLOW ? EnumChatFormatting.YELLOW : EnumChatFormatting.GREEN)
                + ProjectorWidgets.trim(state, 42);
        }

        private static String lockAction() {
            // #tr gtneverreset.projector.pinned.return_to_aim
            // # Return to aim
            // # zh_CN 回到准星
            // #tr gtneverreset.projector.pinned.lock
            // # Lock
            // # zh_CN 锁定
            return tr(locked() ? "return_to_aim" : "lock");
        }

        private String summary() {
            ProjectorPlan plan = ProjectorClient.pinnedPlan();
            if (plan == null) return "";
            ProjectorStructure shape = ProjectorClient.structure;
            ExtendedFacing facing = ProjectorClient.facing;
            if (shape != lastShape || facing != lastFacing) {
                lastShape = shape;
                lastFacing = facing;
                if (shape == null) dimensions = "-";
                else {
                    int[] bounds = shape.worldBounds(facing);
                    dimensions = (bounds[3] - bounds[0] + 1) + "×"
                        + (bounds[4] - bounds[1] + 1)
                        + "×"
                        + (bounds[5] - bounds[2] + 1);
                }
            }
            // #tr gtneverreset.projector.pinned.summary
            // # %s total Each %s
            // # zh_CN 共 %s 份 单份 %s
            if (!plan.batch.withinLimit()) return StatCollector
                .translateToLocalFormatted(ProjectorGui.LANG + "batch_count_limit", ProjectorBatch.MAX_COPIES);
            return StatCollector.translateToLocalFormatted(LANG + "summary", plan.batch.machines(), dimensions);
        }

        private static String mixedLabel(Axis axis) {
            ProjectorPlan plan = ProjectorClient.pinnedPlan();
            if (plan == null) return "";
            return "-" + plan.batch.additional(axis.positive.getOpposite())
                + "/+"
                + plan.batch.additional(axis.positive);
        }

        private static String buildStyle() {
            ProjectorPlan plan = ProjectorClient.pinnedPlan();
            if (plan == null) return "";
            // #tr gtneverreset.projector.pinned.warp
            // # Warp
            // # zh_CN 折跃
            return plan.buildStyle == StructureDeploymentStyle.SINGULARITY ? tr("warp")
                : ProjectorGui.deploymentStyleName(plan.buildStyle);
        }

        private static String copyHatchesLabel() {
            ProjectorPlan plan = ProjectorClient.pinnedPlan();
            // #tr gtneverreset.projector.pinned.copy_hatches_on
            // # Hatches: On
            // # zh_CN 仓室映射：开
            // #tr gtneverreset.projector.pinned.copy_hatches_off
            // # Hatches: Off
            // # zh_CN 仓室映射：关
            return tr(plan != null && plan.batch.copyHatches ? "copy_hatches_on" : "copy_hatches_off");
        }

        private static void turn(int action) {
            if (ProjectorClient.pinnedPlan() == null) return;
            var allowed = ProjectorClient.validFacings();
            ProjectorInput.turn(facing -> switch (action) {
                case 0 -> ProjectorOrientation.mirror(facing, allowed);
                case 1 -> ProjectorOrientation.flipVertical(facing, allowed);
                default -> ProjectorOrientation.rotateY(facing, 1, allowed);
            });
        }
    }
}
