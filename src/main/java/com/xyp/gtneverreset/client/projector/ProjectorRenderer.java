package com.xyp.gtneverreset.client.projector;

import java.nio.FloatBuffer;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.common.projector.ProjectorOrientation;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtnrlib.client.fx.world.FxBlockMesh;
import com.xyp.gtnrlib.client.fx.world.FxCamera;
import com.xyp.gtnrlib.client.fx.world.FxVoxelOutline;
import com.xyp.gtnrlib.utils.Vec3d;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ProjectorRenderer {

    private static final double STRIPE = 0.5, ARROW = 1.5, INSET = 0.02;
    private static final double SECTION_CENTER = 8, SECTION_RADIUS = 14;
    private static final int YELLOW = 0xFFD200, BLACK = 0x202020, RED = 0xE02020, GREEN = 0x30E040, CYAN = 0x40D8FF;
    private static final int MAX_WRONG_BOXES = 4096, FOCUS_ALPHA_MIN = 110, FOCUS_ALPHA_RANGE = 145;
    private static final double PULSE_MILLIS = 160;
    private static final float GHOST_ALPHA = 0.45f;
    private static final long COMPILE_NANOS = 4_000_000L;
    private static final int CONSTANT_ALPHA = 0x8003, ONE_MINUS_CONSTANT_ALPHA = 0x8004;
    private static final ProjectorModelMesh GHOSTS = new ProjectorModelMesh();
    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
    private static boolean[] missing = new boolean[0];
    private static int checkRevision = -1;

    ProjectorRenderer() {}

    public static void release() {
        GHOSTS.release();
    }

    @SubscribeEvent
    public void render(RenderWorldLastEvent event) {
        ProjectorPlan plan = ProjectorClient.plan;
        if (!ProjectorClient.visible || plan == null) return;
        FxCamera.capture();
        Minecraft mc = Minecraft.getMinecraft();
        Entity camera = mc.renderViewEntity;
        if (camera == null) return;
        double cameraX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.partialTicks;
        double cameraY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.partialTicks;
        double cameraZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.partialTicks;
        ProjectorStructure structure = ProjectorClient.projection;
        ProjectorCheck check = ProjectorClient.CHECK;
        int x = ProjectorClient.x, y = ProjectorClient.y, z = ProjectorClient.z;
        int[] bounds = structure == null ? new int[6] : structure.worldBounds(ProjectorClient.facing);
        GL11.glPushMatrix();
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT
                | GL11.GL_CURRENT_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_COLOR_BUFFER_BIT);
        try {
            GL11.glTranslated(x - cameraX, y - cameraY, z - cameraZ);
            if (structure != null && structure.size > 0 && plan.display != ProjectorPlan.Display.BOX) {
                drawGhosts(
                    structure,
                    plan.display == ProjectorPlan.Display.DIFFERENCE && ProjectorClient.placed,
                    check);
            }
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            int accent = ProjectorClient.completeTicks > 0 ? GREEN
                : check.wrong > 0 || ProjectorClient.tooHigh || ProjectorClient.tooLow ? RED : YELLOW;
            for (int pass = 0; pass < 2; pass++) {
                boolean hidden = pass == 0;
                if (hidden) GL11.glDisable(GL11.GL_DEPTH_TEST);
                else GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(false);
                GL11.glLineWidth(hidden ? 2f : 3f);
                int alpha = hidden ? 90 : 255;
                drawHazardBox(
                    bounds[0] - INSET,
                    bounds[1] - INSET,
                    bounds[2] - INSET,
                    bounds[3] + 1 + INSET,
                    bounds[4] + 1 + INSET,
                    bounds[5] + 1 + INSET,
                    accent,
                    alpha);
                drawBox(0.1, 0.1, 0.1, 0.9, 0.9, 0.9, CYAN, alpha);
                drawArrow(ProjectorClient.facing.getDirection(), CYAN, alpha);
            }
            if (ProjectorClient.placed && plan.display != ProjectorPlan.Display.BOX) drawWrong(check, x, y, z);
            if (ProjectorClient.placed) drawFocus(check, x, y, z);
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private static void drawGhosts(ProjectorStructure structure, boolean difference, ProjectorCheck check) {
        if (GHOSTS.prepare(structure)) checkRevision = -1;
        if (!difference) {
            GHOSTS.show(null);
            checkRevision = -1;
        } else if (check.revision != checkRevision && check.structure == structure) {
            checkRevision = check.revision;
            if (missing.length != structure.size) missing = new boolean[structure.size];
            for (int cell = 0; cell < structure.size; cell++) {
                byte state = check.status[cell];
                missing[cell] = state == ProjectorCheck.MISSING || state == ProjectorCheck.UNKNOWN;
            }
            GHOSTS.show(missing);
        }
        GHOSTS.compile(COMPILE_NANOS);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushMatrix();
        boolean mirrored = applyFacing(structure, ProjectorClient.facing);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glFrontFace(mirrored ? GL11.GL_CW : GL11.GL_CCW);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        Predicate<FxBlockMesh.Section> inView = sectionsInView(structure);
        // Depth first, so only the nearest face of the translucent ghost is coloured.
        GL11.glColorMask(false, false, false, false);
        GL11.glDepthMask(true);
        GHOSTS.draw(inView);
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendColor(1f, 1f, 1f, GHOST_ALPHA);
        GL11.glBlendFunc(CONSTANT_ALPHA, ONE_MINUS_CONSTANT_ALPHA);
        GHOSTS.draw(inView);
        GL11.glDepthMask(true);
        GL11.glFrontFace(GL11.GL_CCW);
        GL11.glPopMatrix();
    }

    private static Predicate<FxBlockMesh.Section> sectionsInView(ProjectorStructure structure) {
        int[] m = ProjectorOrientation.matrix(structure.canonical, ProjectorClient.facing);
        double baseX = ProjectorClient.x + 0.5, baseY = ProjectorClient.y + 0.5, baseZ = ProjectorClient.z + 0.5;
        return section -> {
            double sx = section.x + SECTION_CENTER - structure.originX - 0.5,
                sy = section.y + SECTION_CENTER - structure.originY - 0.5,
                sz = section.z + SECTION_CENTER - structure.originZ - 0.5;
            return FxCamera.inView(
                new Vec3d(
                    baseX + m[0] * sx + m[3] * sy + m[6] * sz,
                    baseY + m[1] * sx + m[4] * sy + m[7] * sz,
                    baseZ + m[2] * sx + m[5] * sy + m[8] * sz),
                SECTION_RADIUS);
        };
    }

    private static boolean applyFacing(ProjectorStructure structure, ExtendedFacing facing) {
        int[] m = ProjectorOrientation.matrix(structure.canonical, facing);
        fillMatrix(MATRIX, m);
        GL11.glTranslatef(0.5f, 0.5f, 0.5f);
        GL11.glMultMatrix(MATRIX);
        GL11.glTranslatef(-structure.originX - 0.5f, -structure.originY - 0.5f, -structure.originZ - 0.5f);
        return ProjectorOrientation.mirrors(m);
    }

    public static void fillMatrix(FloatBuffer buffer, int[] rotation) {
        buffer.clear();
        for (int column = 0; column < 3; column++) {
            for (int row = 0; row < 3; row++) buffer.put(rotation[column * 3 + row]);
            buffer.put(0f);
        }
        buffer.put(0f)
            .put(0f)
            .put(0f)
            .put(1f);
        buffer.flip();
    }

    private static void drawHazardBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
        int accent, int alpha) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINES);
        double[] xs = { minX, maxX }, ys = { minY, maxY }, zs = { minZ, maxZ };
        for (double py : ys) {
            for (double pz : zs) stripe(tessellator, minX, py, pz, maxX, py, pz, accent, alpha);
            for (double px : xs) stripe(tessellator, px, py, minZ, px, py, maxZ, accent, alpha);
        }
        for (double px : xs) {
            for (double pz : zs) stripe(tessellator, px, minY, pz, px, maxY, pz, accent, alpha);
        }
        tessellator.draw();
    }

    private static void stripe(Tessellator tessellator, double x1, double y1, double z1, double x2, double y2,
        double z2, int accent, int alpha) {
        double length = Math.abs(x2 - x1) + Math.abs(y2 - y1) + Math.abs(z2 - z1);
        int segments = Math.max(1, (int) Math.ceil(length / STRIPE));
        for (int i = 0; i < segments; i++) {
            double from = (double) i / segments, to = (double) (i + 1) / segments;
            tessellator.setColorRGBA_I((i & 1) == 0 ? accent : BLACK, alpha);
            tessellator.addVertex(x1 + (x2 - x1) * from, y1 + (y2 - y1) * from, z1 + (z2 - z1) * from);
            tessellator.addVertex(x1 + (x2 - x1) * to, y1 + (y2 - y1) * to, z1 + (z2 - z1) * to);
        }
    }

    static void drawBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color,
        int alpha) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINES);
        tessellator.setColorRGBA_I(color, alpha);
        FxVoxelOutline.box(tessellator, minX, minY, minZ, maxX, maxY, maxZ);
        tessellator.draw();
    }

    private static void drawArrow(ForgeDirection front, int color, int alpha) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINES);
        tessellator.setColorRGBA_I(color, alpha);
        double startX = 0.5 + front.offsetX * 0.5, startY = 0.5 + front.offsetY * 0.5,
            startZ = 0.5 + front.offsetZ * 0.5;
        double tipX = startX + front.offsetX * ARROW, tipY = startY + front.offsetY * ARROW,
            tipZ = startZ + front.offsetZ * ARROW;
        tessellator.addVertex(startX, startY, startZ);
        tessellator.addVertex(tipX, tipY, tipZ);
        ForgeDirection side = front.offsetY == 0 ? ForgeDirection.UP : ForgeDirection.EAST;
        for (int sign = -1; sign <= 1; sign += 2) {
            tessellator.addVertex(tipX, tipY, tipZ);
            tessellator.addVertex(
                tipX - front.offsetX * 0.4 + side.offsetX * 0.3 * sign,
                tipY - front.offsetY * 0.4 + side.offsetY * 0.3 * sign,
                tipZ - front.offsetZ * 0.4 + side.offsetZ * 0.3 * sign);
        }
        tessellator.draw();
    }

    private static void drawWrong(ProjectorCheck check, int x, int y, int z) {
        if (!check.isActive() || check.wrong == 0) return;
        Tessellator tessellator = Tessellator.instance;
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(2f);
        tessellator.startDrawing(GL11.GL_LINES);
        tessellator.setColorRGBA_I(RED, 220);
        int drawn = 0;
        for (int cell = 0; cell < check.status.length && drawn < MAX_WRONG_BOXES; cell++) {
            if (check.status[cell] != ProjectorCheck.WRONG) continue;
            drawn++;
            double minX = check.worldX[cell] - x + INSET, minY = check.worldY[cell] - y + INSET,
                minZ = check.worldZ[cell] - z + INSET;
            double maxX = minX + 1 - 2 * INSET, maxY = minY + 1 - 2 * INSET, maxZ = minZ + 1 - 2 * INSET;
            FxVoxelOutline.box(tessellator, minX, minY, minZ, maxX, maxY, maxZ);
        }
        tessellator.draw();
    }

    private static void drawFocus(ProjectorCheck check, int x, int y, int z) {
        int[] cells = ProjectorClient.focus;
        if (cells.length == 0 || !check.isActive()) return;
        double pulse = (Math.sin(Minecraft.getSystemTime() / PULSE_MILLIS) + 1) / 2;
        Tessellator tessellator = Tessellator.instance;
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(3f);
        tessellator.startDrawing(GL11.GL_LINES);
        tessellator.setColorRGBA_I(ProjectorClient.focusColor, (int) (FOCUS_ALPHA_MIN + FOCUS_ALPHA_RANGE * pulse));
        int[] out = new int[3];
        for (int i = 0; i < cells.length && i < MAX_WRONG_BOXES; i++) {
            int cell = cells[i];
            if (check.structure instanceof ProjectorBatchPreview array && cell >= 0 && cell < array.template.size) {
                cell = ProjectorClient.focusPose == null ? array.atOrigin(cell)
                    : array.onCopy(cell, ProjectorClient.focusPose, ProjectorClient.focusOffset, out);

            }
            if (cell < 0 || cell >= check.worldX.length) continue;
            double minX = check.worldX[cell] - x - INSET, minY = check.worldY[cell] - y - INSET,
                minZ = check.worldZ[cell] - z - INSET;
            FxVoxelOutline
                .box(tessellator, minX, minY, minZ, minX + 1 + 2 * INSET, minY + 1 + 2 * INSET, minZ + 1 + 2 * INSET);
        }
        tessellator.draw();
    }
}
