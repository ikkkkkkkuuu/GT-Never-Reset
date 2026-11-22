package com.xyp.gtneverreset.client.projector;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.common.projector.ProjectorOrientation;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Choice;
import com.xyp.gtneverreset.config.ClientConfig;
import com.xyp.gtnrlib.client.fx.world.FxBlockMesh;
import com.xyp.gtnrlib.client.fx.world.FxVoxelOutline;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

@SideOnly(Side.CLIENT)
public class ProjectorPreviewWidget extends Widget<ProjectorPreviewWidget> implements Interactable {

    public static final int ALL_LAYERS = Integer.MIN_VALUE;
    private static final float ISOMETRIC_PITCH = 30f, MIN_ZOOM = 1f, MAX_ZOOM = 2000f, ZOOM_STEP = 1.12f,
        FRAME_MARGIN = 1.12f, EYE_MARGIN = 16f;
    private static final long COMPILE_NANOS = 6_000_000L;
    private static final int BACKGROUND = 0xFF101016, DRAG_THRESHOLD = 3, MAX_MARKED = 4096, PLAIN_TEXT = 0xFFFFFFFF;
    private static final int DIMENSION_ALPHA = 200, X_COLOR = 0xE86464, Y_COLOR = 0x64C864, Z_COLOR = 0xE8C850;
    private static final double DIMENSION_GAP = 6, DIMENSION_OVERSHOOT = 3, DIMENSION_LABEL_GAP = 12;
    private static final int SELECTED = 0xFFD040, HOVERED = 0xFFFFFF, MARKED = 0x4080FF, OVERRIDDEN = 0x40E060;

    private final Supplier<ProjectorStructure> source;
    private final ProjectorModelMesh mesh = new ProjectorModelMesh();
    private final FloatBuffer modelview = BufferUtils.createFloatBuffer(16),
        projection = BufferUtils.createFloatBuffer(16), point = BufferUtils.createFloatBuffer(3),
        labelView = BufferUtils.createFloatBuffer(16), labelLens = BufferUtils.createFloatBuffer(16);
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private ProjectorStructure structure;
    private IntSupplier selection = () -> -1;
    private IntConsumer onSelect = cell -> {};
    private BooleanSupplier showOutlines = () -> false;
    private ProjectorPlan plan = new ProjectorPlan();
    private Object markerInputs;
    private float[] slotLines = new float[0], changedLines = new float[0];
    private Supplier<ExtendedFacing> facingSource = () -> null;
    private final FloatBuffer orientation = BufferUtils.createFloatBuffer(16);
    private ExtendedFacing facing;
    private int[] rotation = { 1, 0, 0, 0, 1, 0, 0, 0, 1 };
    private Supplier<String> hoverText = () -> "";
    private float yaw = 45f, homeYaw = 45f, pitch = ISOMETRIC_PITCH, zoom = 10f, centerX, centerY, centerZ;
    private int layer = ALL_LAYERS, restoredLayer = ALL_LAYERS, hovered = -1, lastX, lastY, travel, edge;
    private final float[] dimensionX = new float[3], dimensionY = new float[3];
    private final String[] dimensionText = new String[3];
    private final int[] dimensionColors = new int[3];
    private int dimensions;

    public ProjectorPreviewWidget(Supplier<ProjectorStructure> source) {
        this.source = source;
    }

    public ProjectorPreviewWidget hoverText(Supplier<String> hoverText) {
        this.hoverText = hoverText;
        return this;
    }

    public ProjectorPreviewWidget selection(IntSupplier selection, IntConsumer onSelect) {
        this.selection = selection;
        this.onSelect = onSelect;
        return this;
    }

    public ProjectorPreviewWidget markers(BooleanSupplier showOutlines, ProjectorPlan plan) {
        this.showOutlines = showOutlines;
        this.plan = plan;
        return this;
    }

    public ProjectorPreviewWidget facing(Supplier<ExtendedFacing> facingSource) {
        this.facingSource = facingSource;
        return this;
    }

    public int hovered() {
        return structure instanceof ProjectorBatchPreview array && hovered >= 0 ? array.originals[hovered] : hovered;
    }

    public int layer() {
        return layer;
    }

    public ProjectorPreviewWidget restoreLayer(int wanted) {
        restoredLayer = wanted;
        return this;
    }

    public void release() {
        mesh.release();
        structure = null;
    }

    public void stepLayer(int step) {
        if (structure == null || structure.isEmpty()) return;
        int[] bounds = structure.worldBounds(facing);
        if (layer == ALL_LAYERS) layer = step > 0 ? bounds[1] : bounds[4];
        else layer += step;
        if (layer < bounds[1] || layer > bounds[4]) layer = ALL_LAYERS;
        applyLayer();
    }

    public void resetCamera() {
        if (structure == null) return;
        recenter();
        yaw = homeYaw = frontYaw();
        pitch = ISOMETRIC_PITCH;
        zoom = fittedZoom();
    }

    private float frontYaw() {
        ForgeDirection front = facing.getDirection();
        return front.offsetY != 0 ? 45f
            : (float) Math.toDegrees(Math.atan2(front.offsetZ - front.offsetX, front.offsetX + front.offsetZ));
    }

    private float fittedZoom() {
        int[] bounds = structure.worldBounds(facing);
        double yawRadians = Math.toRadians(yaw), pitchRadians = Math.toRadians(pitch);
        double sinYaw = Math.sin(yawRadians), cosYaw = Math.cos(yawRadians), sinPitch = Math.sin(pitchRadians),
            cosPitch = Math.cos(pitchRadians);
        double across = 0, up = 0;
        for (int corner = 0; corner < 8; corner++) {
            double x = ((corner & 1) == 0 ? bounds[0] : bounds[3] + 1) - centerX;
            double y = ((corner & 2) == 0 ? bounds[1] : bounds[4] + 1) - centerY;
            double z = ((corner & 4) == 0 ? bounds[2] : bounds[5] + 1) - centerZ;
            across = Math.max(across, Math.abs(x * sinYaw - z * cosYaw));
            up = Math.max(up, Math.abs(-x * sinPitch * cosYaw + y * cosPitch - z * sinPitch * sinYaw));
        }
        float aspect = Math.max(1, getArea().width - 2 * edge) / (float) Math.max(1, getArea().height - 2 * edge);
        return Math.max(MIN_ZOOM, (float) Math.max(up, across / aspect) * FRAME_MARGIN);
    }

    private float eyeDistance() {
        int[] bounds = structure.worldBounds(facing);
        return bounds[3] - bounds[0] + bounds[4] - bounds[1] + bounds[5] - bounds[2] + EYE_MARGIN;
    }

    private void recenter() {
        int[] bounds = structure.worldBounds(facing);
        centerX = (bounds[0] + bounds[3] + 1) / 2f;
        centerY = (bounds[1] + bounds[4] + 1) / 2f;
        centerZ = (bounds[2] + bounds[5] + 1) / 2f;
    }

    private int relativeX(int cell) {
        return mesh.x(cell) - structure.originX;
    }

    private int relativeY(int cell) {
        return mesh.y(cell) - structure.originY;
    }

    private int relativeZ(int cell) {
        return mesh.z(cell) - structure.originZ;
    }

    private int displayY(int cell) {
        return rotation[1] * relativeX(cell) + rotation[4] * relativeY(cell) + rotation[7] * relativeZ(cell);
    }

    private void orient(ExtendedFacing next) {
        facing = next;
        rotation = ProjectorOrientation.matrix(structure.canonical, next);
        ProjectorRenderer.fillMatrix(orientation, rotation);
    }

    private void applyLayer() {
        if (structure.size > FxBlockMesh.MAX_BLOCKS) return;
        if (layer == ALL_LAYERS) {
            mesh.show(null);
            return;
        }
        boolean[] shown = new boolean[structure.size];
        for (int cell = 0; cell < structure.size; cell++) shown[cell] = displayY(cell) == layer;
        mesh.show(shown);
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        Variant variant = AE2Theme.active(context);
        edge = variant == null ? 0 : AE2Theme.INSET_EDGE;
        dimensions = 0;
        if (variant == null) GuiDraw.drawRect(0, 0, getArea().width, getArea().height, BACKGROUND);
        else variant.inset.draw(0, 0, getArea().width, getArea().height);
        drawStructure(context);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        for (int i = 0; i < dimensions; i++) {
            int textWidth = font.getStringWidth(dimensionText[i]);
            int x = Math.round(dimensionX[i]) - textWidth / 2, y = Math.round(dimensionY[i]) - 4;
            boolean inside = x >= edge && y >= edge
                && x + textWidth <= getArea().width - edge
                && y + font.FONT_HEIGHT <= getArea().height - edge;
            if (inside) AE2Theme.text(dimensionText[i], x, y, 0xFF000000 | dimensionColors[i], variant == null);
        }
        String text = hoverText.get();
        if (!text.isEmpty()) AE2Theme.text(
            Minecraft.getMinecraft().fontRenderer.trimStringToWidth(text, getArea().width - 8),
            4,
            AE2Theme.INSET_TOP + 2,
            variant == null ? PLAIN_TEXT : variant.colors.text(),
            variant == null);
    }

    private void drawStructure(ModularGuiContext context) {
        ProjectorStructure next = source.get();
        if (next != structure) {
            boolean sameMachine = structure != null && next != null
                && structure.settings.machine() == next.settings.machine();
            boolean changedArray = structure instanceof ProjectorBatchPreview || next instanceof ProjectorBatchPreview;
            structure = next;
            hovered = -1;
            if (!sameMachine) layer = ALL_LAYERS;
            if (next == null) {
                mesh.release();
            } else {
                mesh.prepare(next);
                orient(wantedFacing());
                if (sameMachine) {
                    if (changedArray) {
                        recenter();
                        zoom = fittedZoom();
                        int[] bounds = next.worldBounds(facing);
                        if (layer < bounds[1] || layer > bounds[4]) layer = ALL_LAYERS;
                    }
                    applyLayer();
                } else {
                    resetCamera();
                    int[] bounds = next.worldBounds(facing);
                    if (restoredLayer >= bounds[1] && restoredLayer <= bounds[4]) {
                        layer = restoredLayer;
                        applyLayer();
                    }
                    restoredLayer = ALL_LAYERS;
                }
            }
        }
        if (structure == null || structure.isEmpty() || structure.size > FxBlockMesh.MAX_BLOCKS) return;
        if (wantedFacing() != facing) {
            orient(wantedFacing());
            recenter();
            if (layer != ALL_LAYERS) {
                layer = ALL_LAYERS;
                applyLayer();
            }
        }
        mesh.compile(COMPILE_NANOS);
        render(context);
    }

    private void render(ModularGuiContext context) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int scale = resolution.getScaleFactor();
        int width = getArea().width - 2 * edge, height = getArea().height - 2 * edge;
        int windowX = (context.transformX(0, 0) + edge) * scale, windowWidth = width * scale,
            windowHeight = height * scale;
        int windowY = mc.displayHeight - (context.transformY(0, 0) + edge + height) * scale;
        float aspect = width / (float) Math.max(1, height), depth = eyeDistance();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(-zoom * aspect, zoom * aspect, -zoom, zoom, 0.05, depth * 2);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        try {
            GL11.glViewport(windowX, windowY, windowWidth, windowHeight);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(windowX, windowY, windowWidth, windowHeight);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            double yawRadians = Math.toRadians(yaw), pitchRadians = Math.toRadians(pitch);
            float eyeX = centerX + (float) (depth * Math.cos(pitchRadians) * Math.cos(yawRadians));
            float eyeY = centerY + (float) (depth * Math.sin(pitchRadians));
            float eyeZ = centerZ + (float) (depth * Math.cos(pitchRadians) * Math.sin(yawRadians));
            GLU.gluLookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, 0f, 1f, 0f);
            GL11.glPushMatrix();
            GL11.glTranslatef(0.5f, 0.5f, 0.5f);
            GL11.glMultMatrix(orientation);
            GL11.glTranslatef(-0.5f, -0.5f, -0.5f);
            captureMatrices(windowX, windowY, windowWidth, windowHeight);
            mc.entityRenderer.disableLightmap(0);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_CULL_FACE);
            // A mirrored orientation reverses the winding of every face.
            GL11.glFrontFace(ProjectorOrientation.mirrors(rotation) ? GL11.GL_CW : GL11.GL_CCW);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            mc.getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
            GL11.glPushMatrix();
            GL11.glTranslatef(-structure.originX, -structure.originY, -structure.originZ);
            mesh.draw();
            GL11.glPopMatrix();
            hovered = isHovering() && structure.size > 0 ? pick(context, scale) : -1;
            drawMarkers();
            GL11.glPopMatrix();
            if (structure instanceof ProjectorBatchPreview array && array.outlineOnly) {
                int[] bounds = structure.worldBounds(facing);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                ProjectorRenderer
                    .drawBox(bounds[0], bounds[1], bounds[2], bounds[3] + 1, bounds[4] + 1, bounds[5] + 1, MARKED, 220);
            }
            drawDimensions(context, scale);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopAttrib();
        }
    }

    private void captureMatrices(int x, int y, int width, int height) {
        modelview.clear();
        projection.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        viewport.clear();
        viewport.put(x)
            .put(y)
            .put(width)
            .put(height);
        viewport.flip();
    }

    private void drawDimensions(ModularGuiContext context, int scale) {
        int[] bounds = structure.worldBounds(facing);
        double minX = bounds[0], minY = bounds[1], minZ = bounds[2], maxX = bounds[3] + 1, maxY = bounds[4] + 1,
            maxZ = bounds[5] + 1;
        double homeRadians = Math.toRadians(homeYaw);
        int towardX = Math.cos(homeRadians) >= 0 ? 1 : -1, towardZ = Math.sin(homeRadians) >= 0 ? 1 : -1;
        double nearX = towardX > 0 ? maxX : minX, nearZ = towardZ > 0 ? maxZ : minZ, farZ = towardZ > 0 ? minZ : maxZ,
            floor = minY;
        double perPixel = 2 * zoom / Math.max(1, getArea().height - 2 * edge);
        double gap = DIMENSION_GAP * perPixel, overshoot = DIMENSION_OVERSHOOT * perPixel,
            labelGap = DIMENSION_LABEL_GAP * perPixel;
        double lineX = nearX + towardX * gap, lineZ = nearZ + towardZ * gap;
        Tessellator tessellator = Tessellator.instance;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glLineWidth(1.5f);
        tessellator.startDrawing(GL11.GL_LINES);
        tessellator.setColorRGBA_I(X_COLOR, DIMENSION_ALPHA);
        line(tessellator, minX, floor, lineZ, maxX, floor, lineZ);
        for (double x : new double[] { minX, maxX })
            line(tessellator, x, floor, nearZ, x, floor, lineZ + towardZ * overshoot);
        tessellator.setColorRGBA_I(Z_COLOR, DIMENSION_ALPHA);
        line(tessellator, lineX, floor, minZ, lineX, floor, maxZ);
        for (double z : new double[] { minZ, maxZ })
            line(tessellator, nearX, floor, z, lineX + towardX * overshoot, floor, z);
        tessellator.setColorRGBA_I(Y_COLOR, DIMENSION_ALPHA);
        line(tessellator, lineX, minY, farZ, lineX, maxY, farZ);
        for (double y : new double[] { minY, maxY })
            line(tessellator, nearX, y, farZ, lineX + towardX * overshoot, y, farZ);
        tessellator.draw();
        dimensions = 0;
        label(context, scale, (minX + maxX) / 2, floor, lineZ + towardZ * labelGap, maxX - minX, X_COLOR);
        label(context, scale, lineX + towardX * labelGap, floor, (minZ + maxZ) / 2, maxZ - minZ, Z_COLOR);
        label(context, scale, lineX + towardX * labelGap, (minY + maxY) / 2, farZ, maxY - minY, Y_COLOR);
    }

    private static void line(Tessellator tessellator, double x1, double y1, double z1, double x2, double y2,
        double z2) {
        tessellator.addVertex(x1, y1, z1);
        tessellator.addVertex(x2, y2, z2);
    }

    private void label(ModularGuiContext context, int scale, double x, double y, double z, double length, int color) {
        labelView.clear();
        labelLens.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, labelView);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, labelLens);
        point.clear();
        if (!GLU.gluProject((float) x, (float) y, (float) z, labelView, labelLens, viewport, point)) return;
        Minecraft mc = Minecraft.getMinecraft();
        dimensionX[dimensions] = point.get(0) / scale - context.transformX(0, 0);
        dimensionY[dimensions] = (mc.displayHeight - point.get(1)) / scale - context.transformY(0, 0);
        dimensionColors[dimensions] = color;
        dimensionText[dimensions++] = Integer.toString((int) Math.round(length));
    }

    private void drawMarkers() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(false);
        if (ClientConfig.projectorOutlinesThroughBlocks) GL11.glDisable(GL11.GL_DEPTH_TEST);
        else GL11.glEnable(GL11.GL_DEPTH_TEST);
        updateMarkers();
        Tessellator tessellator = Tessellator.instance;
        GL11.glLineWidth(2f);
        FxVoxelOutline.draw(tessellator, slotLines, MARKED, 255);
        FxVoxelOutline.draw(tessellator, changedLines, OVERRIDDEN, 255);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        tessellator.startDrawing(GL11.GL_LINES);
        int selected = selection.getAsInt();
        if (structure instanceof ProjectorBatchPreview array) selected = array.atOrigin(selected);
        if (selected >= 0 && selected < structure.size) box(tessellator, selected, SELECTED, 0.02);
        if (hovered >= 0) box(tessellator, hovered, HOVERED, 0.0);
        tessellator.draw();
    }

    @Desugar
    private record MarkerInputs(ProjectorStructure structure, int layer, boolean shown, boolean merge, int overrides,
        int cellChoices, int choices) {}

    private void updateMarkers() {
        boolean shown = showOutlines.getAsBoolean(), merge = ClientConfig.projectorMergeOutlines;
        MarkerInputs inputs = new MarkerInputs(
            structure,
            layer,
            shown,
            merge,
            plan.overrides.hashCode(),
            plan.cellChoices.hashCode(),
            plan.choices.hashCode());
        if (inputs.equals(markerInputs)) return;
        markerInputs = inputs;
        LongOpenHashSet slotCells = new LongOpenHashSet(), changedCells = new LongOpenHashSet();
        if (shown) {
            markChanged(plan.overrides.keySet(), changedCells);
            markChanged(plan.cellChoices.keySet(), changedCells);
            for (int cell = 0; !plan.choices.isEmpty() && cell < structure.size; cell++) {
                Choice choice = structure.choiceAt(cell);
                if (choice != null && choice.channel() == null
                    && plan.choices.containsKey(choice.key())
                    && visible(cell)) changedCells.add(outlineKey(cell));
            }
        }
        for (int cell = 0; shown && cell < structure.size && slotCells.size() < MAX_MARKED; cell++) {
            if (structure.slots[cell] < 0 || !visible(cell)) continue;
            long key = outlineKey(cell);
            if (!changedCells.contains(key)) slotCells.add(key);
        }
        slotLines = merge ? FxVoxelOutline.mergedLines(slotCells) : FxVoxelOutline.boxLines(slotCells);
        changedLines = merge ? FxVoxelOutline.mergedLines(changedCells) : FxVoxelOutline.boxLines(changedCells);
    }

    private void markChanged(Iterable<Long> keys, LongOpenHashSet changedCells) {
        if (structure instanceof ProjectorBatchPreview array) {
            var originals = new LongOpenHashSet();
            for (long key : keys) originals.add(key);
            if (originals.isEmpty()) return;
            for (int i = 0; i < array.size && changedCells.size() < MAX_MARKED; i++) {
                int src = array.originals[i];
                if (visible(i) && originals
                    .contains(ProjectorPlan.pack(array.template.a[src], array.template.b[src], array.template.c[src])))
                    changedCells.add(outlineKey(i));
            }
            return;
        }
        for (long key : keys) {
            int cell = structure
                .indexOf(ProjectorPlan.unpackA(key), ProjectorPlan.unpackB(key), ProjectorPlan.unpackC(key));
            if (cell >= 0 && visible(cell)) changedCells.add(outlineKey(cell));
        }
    }

    private long outlineKey(int cell) {
        return FxVoxelOutline.pack(relativeX(cell), relativeY(cell), relativeZ(cell));
    }

    private boolean visible(int cell) {
        return layer == ALL_LAYERS || displayY(cell) == layer;
    }

    private ExtendedFacing wantedFacing() {
        ExtendedFacing wanted = facingSource.get();
        return wanted == null ? structure.canonical : wanted;
    }

    private void box(Tessellator tessellator, int cell, int color, double inset) {
        tessellator.setColorRGBA_I(color, 255);
        double minX = relativeX(cell) + inset, minY = relativeY(cell) + inset, minZ = relativeZ(cell) + inset;
        FxVoxelOutline
            .box(tessellator, minX, minY, minZ, minX + 1 - 2 * inset, minY + 1 - 2 * inset, minZ + 1 - 2 * inset);
    }

    private int pick(ModularGuiContext context, int scale) {
        Minecraft mc = Minecraft.getMinecraft();
        float mouseX = context.getAbsMouseX() * scale, mouseY = mc.displayHeight - context.getAbsMouseY() * scale;
        float[] near = unproject(mouseX, mouseY, 0f), far = unproject(mouseX, mouseY, 1f);
        if (near == null || far == null) return -1;
        double dx = far[0] - near[0], dy = far[1] - near[1], dz = far[2] - near[2];
        int[] bounds = structure.worldBounds(structure.canonical);
        double[] range = clip(near, dx, dy, dz, bounds);
        if (range == null) return -1;
        return march(near, dx, dy, dz, range[0], range[1]);
    }

    private float[] unproject(float x, float y, float depth) {
        point.clear();
        if (!GLU.gluUnProject(x, y, depth, modelview, projection, viewport, point)) return null;
        return new float[] { point.get(0), point.get(1), point.get(2) };
    }

    private static double[] clip(float[] origin, double dx, double dy, double dz, int[] bounds) {
        double enter = 0, exit = 1;
        double[] direction = { dx, dy, dz };
        for (int axis = 0; axis < 3; axis++) {
            double low = bounds[axis], high = bounds[axis + 3] + 1;
            if (Math.abs(direction[axis]) < 1e-9) {
                if (origin[axis] < low || origin[axis] > high) return null;
                continue;
            }
            double first = (low - origin[axis]) / direction[axis], second = (high - origin[axis]) / direction[axis];
            enter = Math.max(enter, Math.min(first, second));
            exit = Math.min(exit, Math.max(first, second));
        }
        return enter <= exit ? new double[] { enter, exit } : null;
    }

    private int march(float[] origin, double dx, double dy, double dz, double enter, double exit) {
        double startX = origin[0] + dx * (enter + 1e-6), startY = origin[1] + dy * (enter + 1e-6),
            startZ = origin[2] + dz * (enter + 1e-6);
        int x = (int) Math.floor(startX), y = (int) Math.floor(startY), z = (int) Math.floor(startZ);
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1, stepZ = dz > 0 ? 1 : -1;
        double deltaX = dx == 0 ? Double.MAX_VALUE : Math.abs(1 / dx),
            deltaY = dy == 0 ? Double.MAX_VALUE : Math.abs(1 / dy),
            deltaZ = dz == 0 ? Double.MAX_VALUE : Math.abs(1 / dz);
        double maxX = dx == 0 ? Double.MAX_VALUE : ((dx > 0 ? x + 1 - startX : startX - x) * deltaX) + enter,
            maxY = dy == 0 ? Double.MAX_VALUE : ((dy > 0 ? y + 1 - startY : startY - y) * deltaY) + enter,
            maxZ = dz == 0 ? Double.MAX_VALUE : ((dz > 0 ? z + 1 - startZ : startZ - z) * deltaZ) + enter;
        while (true) {
            int cell = mesh.cellAt(x + structure.originX, y + structure.originY, z + structure.originZ);
            if (cell >= 0) return cell;
            if (maxX < maxY && maxX < maxZ) {
                if (maxX > exit) return -1;
                x += stepX;
                maxX += deltaX;
            } else if (maxY < maxZ) {
                if (maxY > exit) return -1;
                y += stepY;
                maxY += deltaY;
            } else {
                if (maxZ > exit) return -1;
                z += stepZ;
                maxZ += deltaZ;
            }
        }
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        lastX = getContext().getAbsMouseX();
        lastY = getContext().getAbsMouseY();
        travel = 0;
        return Result.SUCCESS;
    }

    @Override
    public void onMouseDrag(int mouseButton, long timeSinceClick) {
        int mouseX = getContext().getAbsMouseX(), mouseY = getContext().getAbsMouseY();
        int dx = mouseX - lastX, dy = mouseY - lastY;
        travel += Math.abs(dx) + Math.abs(dy);
        if (mouseButton == 0) {
            yaw = (yaw + dx + 360f) % 360f;
            pitch = Math.max(-89f, Math.min(89f, pitch + dy));
        } else if (mouseButton == 1 || mouseButton == 2) {
            float step = 2 * zoom / Math.max(1, getArea().height - 2 * edge);
            double yawRadians = Math.toRadians(yaw), pitchRadians = Math.toRadians(pitch);
            double sinYaw = Math.sin(yawRadians), cosYaw = Math.cos(yawRadians), sinPitch = Math.sin(pitchRadians);
            centerX += (float) ((-sinYaw * dx - sinPitch * cosYaw * dy) * step);
            centerY += (float) (Math.cos(pitchRadians) * dy * step);
            centerZ += (float) ((cosYaw * dx - sinPitch * sinYaw * dy) * step);
        }
        lastX = mouseX;
        lastY = mouseY;
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        if (mouseButton == 0 && travel < DRAG_THRESHOLD && hovered >= 0) onSelect.accept(hovered());
        return true;
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        zoom = Math
            .max(MIN_ZOOM, Math.min(MAX_ZOOM, scrollDirection.modifier > 0 ? zoom / ZOOM_STEP : zoom * ZOOM_STEP));
        return true;
    }
}
