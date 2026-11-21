package com.xyp.gtneverreset.client.projector;

import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.projector.ProjectorOrientation;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ProjectorPoseScreen extends ModularScreen {

    private static final String LANG = "gtneverreset.projector.pose.";
    private static final int WIDTH = 190, PAD = 8, ROW = 18, BUTTON = 16, TITLE = 12, SMALL = 20;

    private ProjectorPoseScreen() {
        super(GTNeverReset.MODID, panel());
    }

    public static void open() {
        ClientGUI.open(new ProjectorPoseScreen());
    }

    private static ModularPanel panel() {
        int inner = WIDTH - PAD * 2;
        ModularPanel panel = new ModularPanel("projector_pose").size(WIDTH, PAD * 2 + TITLE + ROW * 7);
        panel.child(
            IKey.lang(LANG + "title")
                .asWidget()
                .pos(PAD, PAD)
                .size(inner, TITLE));
        int top = PAD + TITLE;
        panel.child(button(inner, top, IKey.dynamic(ProjectorPoseScreen::frontLabel), ProjectorPoseScreen::nextFront));
        panel.child(
            button(
                inner,
                top + ROW,
                IKey.lang(LANG + "rotate"),
                () -> ProjectorInput
                    .turn(facing -> ProjectorOrientation.nextRotation(facing, ProjectorClient.validFacings()))));
        panel.child(
            button(
                inner,
                top + ROW * 2,
                IKey.lang(LANG + "mirror"),
                () -> ProjectorInput
                    .turn(facing -> ProjectorOrientation.mirror(facing, ProjectorClient.validFacings()))));
        panel.child(
            button(
                inner,
                top + ROW * 3,
                IKey.lang(LANG + "flip"),
                () -> ProjectorInput
                    .turn(facing -> ProjectorOrientation.flipVertical(facing, ProjectorClient.validFacings()))));
        panel.child(
            button(
                inner,
                top + ROW * 4,
                IKey.dynamic(
                    () -> StatCollector.translateToLocal(
                        // #tr gtneverreset.projector.pose.pivot_center
                        // # Turn around: structure centre
                        // # zh_CN 旋转中心：结构中心
                        LANG + (ProjectorClient.plan != null && ProjectorClient.plan.centerPivot ? "pivot_center"
                            // #tr gtneverreset.projector.pose.pivot_controller
                            // # Turn around: controller
                            // # zh_CN 旋转中心：控制器
                            : "pivot_controller"))),
                () -> ProjectorInput.edit(copy -> copy.centerPivot = !copy.centerPivot)));
        panel.child(
            button(
                inner,
                top + ROW * 5,
                IKey.dynamic(
                    () -> StatCollector.translateToLocal(
                        // #tr gtneverreset.projector.pose.ground
                        // # Placement: on the ground
                        // # zh_CN 放置：贴地
                        // #tr gtneverreset.projector.pose.free
                        // # Placement: free
                        // # zh_CN 放置：自由
                        LANG + (ProjectorClient.plan != null && !ProjectorClient.plan.groundSnap ? "free" : "ground"))),
                () -> {
                    if (ProjectorClient.plan == null) return;
                    ProjectorInput.edit(copy -> {
                        copy.groundSnap = !copy.groundSnap;
                        if (copy.groundSnap) copy.verticalOffset = Math.max(0, copy.verticalOffset);
                    });
                }));
        int offsetTop = top + ROW * 6;
        panel.child(button(SMALL, offsetTop, IKey.str("-"), () -> raise(-1)));
        panel.child(
            IKey.dynamic(ProjectorPoseScreen::offsetLabel)
                .asWidget()
                .pos(PAD + SMALL + 4, offsetTop + 4)
                .size(inner - SMALL * 2 - 8, BUTTON - 4));
        panel.child(button(SMALL, offsetTop, IKey.str("+"), () -> raise(1)).left(PAD + inner - SMALL));
        return panel;
    }

    private static ButtonWidget<?> button(int width, int top, IKey label, Runnable action) {
        return new ButtonWidget<>().overlay(label)
            .size(width, BUTTON)
            .pos(PAD, top)
            .onMousePressed(mouseButton -> {
                if (ProjectorClient.plan != null && ProjectorClient.plan.hasMachine()) action.run();
                return true;
            });
    }

    private static void raise(int step) {
        if (ProjectorClient.plan != null) ProjectorInput.raise(ProjectorClient.plan, step);
    }

    private static String frontLabel() {
        ProjectorPlan plan = ProjectorClient.plan;
        return ProjectorClient.frontLabel(plan == null ? ForgeDirection.NORTH : plan.facing.getDirection());
    }

    private static void nextFront() {
        ForgeDirection next = ProjectorOrientation.nextFront(ProjectorClient.plan.facing.getDirection());
        ProjectorInput.turn(facing -> ProjectorOrientation.withFront(facing, next, ProjectorClient.validFacings()));
    }

    private static String offsetLabel() {
        ProjectorPlan plan = ProjectorClient.plan;
        if (plan == null) return "";
        return plan.state == ProjectorPlan.State.LOCKED
            // #tr gtneverreset.projector.pose.height
            // # Height: y %s
            // # zh_CN 高度：y %s
            ? StatCollector.translateToLocalFormatted(LANG + "height", plan.y)
            // #tr gtneverreset.projector.pose.offset
            // # Raised by %s
            // # zh_CN 抬高 %s 格
            : StatCollector.translateToLocalFormatted(LANG + "offset", plan.verticalOffset);
    }
}
